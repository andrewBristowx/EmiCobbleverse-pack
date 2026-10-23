package emi.cafe;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * El cafe de un jugador (su «dueño»): el local, los clientes, las estaciones, el turno de 4 minutos y quienes estan dentro (el dueño y los amigos que invite).
 * Lo que cada uno lleva en las manos es el objeto de verdad en la mano (hueco 1 de la barra, que no se puede cambiar), no en el inventario.
 * El dinero ganado va a quien sirve; las mejoras y los platos que se conocen son los del dueño.
 */
final class Cafe {
    enum Estado { PREPARANDO, ABIERTO, CERRANDO }

    /** Lo que hay en una estacion: lo que se esta haciendo, el avance y el plato terminado que espera a que lo recojan. */
    static final class Est {
        Item resultado;                       // lo que saldra (null = no esta trabajando)
        int ticks, total;
        Item listo;                           // terminado y esperando
        int listoTicks;                       // desde cuando espera (la freidora quema)
        final List<Item> dentro = new ArrayList<>();      // en la mesa de emplatar: lo que se ha puesto
        int cola;                             // en el fregadero: platos sucios esperando
        final List<UUID> quien = new ArrayList<>();       // y quien puso cada uno (para las misiones)
        Entity icono, barra;
    }

    static final int TURNO = 20 * 60 * 4;                 // 4 minutos
    static final int QUEMA_AVISO = 20 * 6, QUEMA = 20 * 10;

    final UUID duenio;
    final MinecraftServer server;
    final ServerWorld w;
    final int ox, slot;
    final Random rnd = new Random();
    final Set<UUID> jugadores = new LinkedHashSet<>();
    final Map<UUID, Item> manos = new HashMap<>();
    final List<Mesa> mesas = new ArrayList<>();
    final List<Cliente> clientes = new ArrayList<>();
    final Map<BlockPos, Est> ests = new HashMap<>();
    Estado estado = Estado.PREPARANDO;
    int t;
    int restante, cierre;
    int proximoCliente;
    int platos;                                           // platos limpios en la pila
    // lo de este turno (para el resumen)
    int servidos, enfadados, propinas, ganado;
    final Map<UUID, Integer> ganadoPor = new HashMap<>();
    final Map<UUID, Integer> servidosPor = new HashMap<>();
    final Map<UUID, Integer> estrellasTurno = new HashMap<>();

    Cafe(MinecraftServer server, ServerWorld w, UUID duenio, int slot) {
        this.server = server; this.w = w; this.duenio = duenio; this.slot = slot; this.ox = slot * Local.SEPARACION;
        jugadores.add(duenio);
    }

    Datos.Jugador dj() { ServerPlayerEntity p = jugador(duenio); return EmiCafe.datos.jugador(duenio, p == null ? null : p.getGameProfile().getName()); }
    Datos.Jugador dj(ServerPlayerEntity p) { return EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName()); }
    ServerPlayerEntity jugador(UUID id) { return server.getPlayerManager().getPlayer(id); }
    List<ServerPlayerEntity> presentes() { List<ServerPlayerEntity> l = new ArrayList<>(); for (UUID id : jugadores) { ServerPlayerEntity p = jugador(id); if (p != null) l.add(p); } return l; }
    boolean esDuenio(ServerPlayerEntity p) { return p.getUuid().equals(duenio); }

    // ------------------------------------------------------------ lo que dicen las mejoras

    int nivel(Recetas.Mejora m) { return dj().nivel(m.id); }
    int numMesas() { return Math.min(Local.MESAS.length, 2 + nivel(Recetas.Mejora.MESAS)); }
    double velocidad() { return 1.0 - 0.2 * nivel(Recetas.Mejora.VELOCIDAD); }
    int pacienciaTicks() { return (int) (20 * 90 * (1.0 + 0.25 * nivel(Recetas.Mejora.PACIENCIA))); }
    int platosMax() { return 4 + 2 * nivel(Recetas.Mejora.VAJILLA); }
    /** Ticks entre un cliente y el siguiente (menos con mejor publicidad y con mas gente cocinando). */
    int intervalo() { return (int) (20 * (16 - 3 * nivel(Recetas.Mejora.PUBLICIDAD)) / (1.0 + 0.5 * (jugadores.size() - 1))) + rnd.nextInt(60); }

    Set<Estacion> activas() {
        Set<Estacion> s = EnumSet.noneOf(Estacion.class);
        for (String id : dj().platos) { Recetas.Plato pl = Recetas.Plato.de(id); if (pl != null) s.addAll(pl.estaciones); }
        return s;
    }

    /** Construye (o reconstruye) el local segun lo que ha desbloqueado el dueño; borra lo que hubiera dentro. */
    void reconstruir() {
        for (Cliente c : clientes) c.quitar();
        clientes.clear();
        mesas.clear();
        for (Est e : ests.values()) { Disp.quitar(e.icono); Disp.quitar(e.barra); }
        ests.clear();
        Local.construir(w, ox, activas(), numMesas());
        for (int i = 0; i < numMesas(); i++) mesas.add(new Mesa(Local.pos(ox, Local.MESAS[i][0], 1, Local.MESAS[i][1])));
    }

    // ------------------------------------------------------------ mensajes a todos

    void aTodos(java.util.function.Consumer<ServerPlayerEntity> f) { for (ServerPlayerEntity p : presentes()) f.accept(p); }

    void sonido(double x, double y, double z, SoundEvent s, float vol, float pitch) { w.playSound(null, x, y, z, s, SoundCategory.BLOCKS, vol, pitch); }

    // ------------------------------------------------------------ cosas que piden los clientes

    Item elegirPedido(Random r) {
        List<String> ids = dj().platos;
        if (ids.isEmpty()) return CafeItems.PAPAS_FRITAS;
        Recetas.Plato pl = Recetas.Plato.de(ids.get(r.nextInt(ids.size())));
        return pl == null ? CafeItems.PAPAS_FRITAS : pl.item;
    }

    void alEnfadarse(Cliente c) {
        enfadados++;
        aTodos(p -> Msg.actionBar(p, Text.literal("Un cliente se fue enfadado...").formatted(Formatting.RED)));
        sonido(c.e.getX(), c.e.getY(), c.e.getZ(), SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    // ------------------------------------------------------------ el turno

    /** Abre el turno de 4 minutos (solo el dueño, y solo si ya eligio cocinero). */
    String abrirTurno() {
        if (estado != Estado.PREPARANDO) return "El café ya está abierto.";
        if (dj().inicial == null) return "Primero elige a tu Pokémon cocinero.";
        estado = Estado.ABIERTO;
        restante = TURNO;
        proximoCliente = 40;
        platos = platosMax();
        servidos = enfadados = propinas = ganado = 0;
        ganadoPor.clear(); servidosPor.clear(); estrellasTurno.clear();
        aTodos(p -> {
            Msg.title(p, Text.literal("¡ABRIMOS!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Turno de 4 minutos").formatted(Formatting.YELLOW), 5, 40, 10);
            Msg.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.2f);
        });
        return null;
    }

    /** Termina el turno antes de tiempo o cuando se acaba el reloj: no entran mas clientes y los que esperan se van sin pagar. */
    void cerrarTurno() {
        if (estado != Estado.ABIERTO) return;
        estado = Estado.CERRANDO;
        cierre = 0;
        for (Cliente c : clientes) c.despedir();
        aTodos(p -> {
            Msg.title(p, Text.literal("¡CERRAMOS!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Se acabó el turno").formatted(Formatting.YELLOW), 5, 40, 10);
            Msg.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 0.7f);
        });
    }

    private void terminarTurno() {
        for (ServerPlayerEntity p : presentes()) {
            Datos.Jugador d = dj(p);
            int gan = ganadoPor.getOrDefault(p.getUuid(), 0), serv = servidosPor.getOrDefault(p.getUuid(), 0);
            if (serv > 0) {
                d.turnos++;
                Progreso.dar(this, d, p.getUuid(), 5);                                    // por terminar el turno sirviendo
                Progreso.evento(this, p, d, "turnos", 1);
                if (enfadados == 0 && serv >= 3) { d.sinEnfados++; Progreso.dar(this, d, p.getUuid(), 5); Progreso.evento(this, p, d, "sin_enfado", 1); }
                if (gan > d.mejorTurno) { if (d.mejorTurno > 0) Progreso.dar(this, d, p.getUuid(), 5); d.mejorTurno = gan; }      // el primer turno no cuenta como record
                Progreso.evento(this, p, d, "turno_max", gan);
            }
            Progreso.revisarLogros(this, p, d);
            EmiCafe.datos.guardar(p.getUuid());
        }
        estado = Estado.PREPARANDO;
        for (UUID id : jugadores) manos.put(id, null);
        reconstruir();
        aTodos(p -> { sincronizarMano(p); Menus.resumen(this, p); });
    }

    // ------------------------------------------------------------ cada tick

    void tick() {
        List<ServerPlayerEntity> ps = presentes();
        if (ps.isEmpty()) return;
        t++;
        for (ServerPlayerEntity p : ps) {
            if (p.getServerWorld() != w) { Cafes.sinRestaurar(p); continue; }          // se fue por otro medio (otro minijuego): ya no es de este cafe; Retornos le devuelve lo suyo si vuelve aqui
            if (p.getY() < Local.Y - 8) p.teleport(w, ox + Local.SX, Local.Y + 1, Local.SZ, 180f, 0f);
        }
        ps = presentes();
        if (ps.isEmpty()) return;
        if (estado == Estado.ABIERTO) {
            if (--restante <= 0) cerrarTurno();
            else if (--proximoCliente <= 0) { proximoCliente = intervalo(); nuevoCliente(); }
        }
        for (int i = clientes.size() - 1; i >= 0; i--) {
            Cliente c = clientes.get(i);
            boolean fin = c.fuera() || c.tick(this, rnd);
            if (c.fase == Cliente.Fase.SALIENDO && c.mesa.cliente == c) {
                c.mesa.cliente = null; Disp.quitar(c.mesa.plato); c.mesa.plato = null;
                if (!c.enfadado && c.sirvio) ponerSucio(c.mesa);
            }
            if (fin) { c.quitar(); if (c.mesa.cliente == c) c.mesa.cliente = null; clientes.remove(i); }
        }
        if (estado == Estado.CERRANDO) {
            cierre++;
            if (clientes.isEmpty() || cierre > 20 * 12) terminarTurno();
        }
        for (Map.Entry<BlockPos, Est> en : ests.entrySet()) tickEstacion(en.getKey(), en.getValue());
        if (t % 2 == 0) for (ServerPlayerEntity p : ps) sincronizarMano(p);
        if (t % 10 == 0) {
            for (ServerPlayerEntity p : ps) {
                p.getHungerManager().setFoodLevel(20);
                p.setHealth(p.getMaxHealth());
                Msg.actionBar(p, hud(p));
            }
            for (Entity e : w.getEntitiesByType(EntityType.ITEM, new Box(ox - 4, Local.Y - 2, -4, ox + 20, Local.Y + 8, 24), x -> true)) e.discard();
        }
    }

    private Text hud(ServerPlayerEntity p) {
        Item c = manos.get(p.getUuid());
        Text mano = c == null ? Text.literal("manos vacías").formatted(Formatting.GRAY) : Text.literal(CafeItems.nombre(c)).formatted(Formatting.YELLOW);
        Text r = Text.literal("");
        if (estado == Estado.ABIERTO) {
            int s = restante / 20;
            r = Text.literal(String.format("⏱ %d:%02d   ", s / 60, s % 60)).formatted(s < 30 ? Formatting.RED : Formatting.WHITE, Formatting.BOLD);
        } else if (estado == Estado.PREPARANDO) r = Text.literal("Café cerrado — usa la caja registradora   ").formatted(Formatting.AQUA);
        return Text.empty().append(r).append(Text.literal("Monedas: ").formatted(Formatting.GOLD)).append(Text.literal(String.valueOf(dj(p).monedas)).formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal("   Platos limpios: ").formatted(Formatting.GOLD)).append(Text.literal(String.valueOf(platos)).formatted(platos == 0 ? Formatting.RED : Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal("   Llevas: ").formatted(Formatting.GOLD)).append(mano);
    }

    private void nuevoCliente() {
        List<Mesa> libres = new ArrayList<>();
        for (Mesa m : mesas) if (m.libre()) libres.add(m);
        if (libres.isEmpty()) return;
        forzarCliente(libres.get(rnd.nextInt(libres.size())));
    }

    /** Crea un cliente ya (tambien lo usa el comando de pruebas). false si no hay mesa libre. */
    boolean forzarCliente(Mesa m) {
        Cliente c = Cliente.crear(w, ox, m, rnd);
        if (c == null) return false;
        m.cliente = c;
        clientes.add(c);
        return true;
    }

    boolean forzarCliente() {
        for (Mesa m : mesas) if (m.libre()) return forzarCliente(m);
        return false;
    }

    private void ponerSucio(Mesa m) {
        Disp.quitar(m.sucio);
        m.sucio = Disp.objeto(w, CafeItems.id(CafeItems.PLATO_SUCIO), m.bloque.getX() + 0.5, m.bloque.getY() + 1.3, m.bloque.getZ() + 0.5, 0.5f, true);
    }

    // ------------------------------------------------------------ las manos

    /** El objeto de la mano del jugador tiene que ser lo que dice {@code manos}; el resto del inventario, vacio, y el hueco 1 elegido. */
    void sincronizarMano(ServerPlayerEntity p) {
        var inv = p.getInventory();
        boolean cambio = false;
        Item c = manos.get(p.getUuid());
        ItemStack quiero = c == null ? ItemStack.EMPTY : new ItemStack(c);
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (i == 0) { if (!ItemStack.areItemsEqual(st, quiero) || st.getCount() != quiero.getCount()) { inv.setStack(0, quiero); cambio = true; } }
            else if (!st.isEmpty()) { inv.setStack(i, ItemStack.EMPTY); cambio = true; }
        }
        if (inv.selectedSlot != 0) { inv.selectedSlot = 0; p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket(0)); }
        if (cambio && p.currentScreenHandler == p.playerScreenHandler) { p.playerScreenHandler.sendContentUpdates(); }
    }

    void msg(ServerPlayerEntity p, String s) { Msg.actionBar(p, Text.literal(s).formatted(Formatting.YELLOW)); }

    // ------------------------------------------------------------ usar bloques y clientes

    /** El jugador hace clic derecho en un bloque del local. */
    void usarBloque(ServerPlayerEntity p, BlockPos pos) {
        Estacion e = Estacion.de(w.getBlockState(pos).getBlock());
        if (e == null) return;
        Item carga = manos.get(p.getUuid());
        double x = pos.getX() + 0.5, y = pos.getY() + 1, z = pos.getZ() + 0.5;
        switch (e.tipo) {
            case CAJA -> {
                if (carga != null) { msg(p, "Ya llevas algo en las manos"); return; }
                manos.put(p.getUuid(), e.ingrediente);
                sonido(x, y, z, SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);
            }
            case BASURA -> {
                if (carga == null) { msg(p, "No llevas nada que tirar"); return; }
                if (carga == CafeItems.PLATO_SUCIO) { msg(p, "Los platos sucios se lavan en el fregadero"); return; }
                manos.put(p.getUuid(), null);
                sonido(x, y, z, SoundEvents.BLOCK_COMPOSTER_FILL, 0.8f, 1.0f);
            }
            case PROCESO -> proceso(p, e, pos, carga);
            case EMPLATAR -> emplatar(p, e, pos, carga);
            case FREGADERO -> fregadero(p, e, pos, carga);
            case MESA -> {
                Mesa m = null;
                for (Mesa k : mesas) if (k.bloque.equals(pos)) m = k;
                if (m == null) return;
                if (m.sucio != null) {
                    if (carga != null) { msg(p, "Tienes las manos ocupadas"); return; }
                    Disp.quitar(m.sucio); m.sucio = null;
                    manos.put(p.getUuid(), CafeItems.PLATO_SUCIO);
                    sonido(x, y, z, SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 0.9f);
                } else servir(p, m, carga);
            }
            case REGISTRADORA -> Menus.principal(this, p);
        }
        sincronizarMano(p);
    }

    /** Clic derecho a un Pokemon: si es un cliente, es como tocar su mesa. */
    boolean usarEntidad(ServerPlayerEntity p, Entity en) {
        for (Cliente c : clientes) if (c.e == en) { servir(p, c.mesa, manos.get(p.getUuid())); sincronizarMano(p); return true; }
        return false;
    }

    private Est est(BlockPos pos) { return ests.computeIfAbsent(pos, k -> new Est()); }

    private void proceso(ServerPlayerEntity p, Estacion e, BlockPos pos, Item carga) {
        Est est = est(pos);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (est.listo != null) {
            if (carga != null) { msg(p, "Tienes las manos ocupadas"); return; }
            manos.put(p.getUuid(), est.listo); est.listo = null; est.listoTicks = 0;
            sonido(x, y, z, SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.0f);
        } else if (est.resultado != null) {
            msg(p, e.nombre + " está trabajando");
        } else if (carga == null) {
            msg(p, "Trae algo para: " + e.nombre.toLowerCase());
        } else {
            Item sale = Recetas.procesa(e, carga);
            if (sale == null) { msg(p, "Eso no va aquí: " + e.nombre.toLowerCase()); sonido(x, y, z, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.7f, 0.8f); return; }
            est.resultado = sale; est.ticks = 0; est.total = Math.max(10, (int) Math.round(e.ticks * velocidad()));
            manos.put(p.getUuid(), null);
            sonido(x, y, z, e == Estacion.FREIDORA ? SoundEvents.BLOCK_BREWING_STAND_BREW : e == Estacion.EXPRIMIDOR ? SoundEvents.ITEM_BUCKET_FILL : SoundEvents.BLOCK_WOOD_HIT, 0.8f, 1.0f);
        }
        dibujar(pos, e, est);
    }

    private void emplatar(ServerPlayerEntity p, Estacion e, BlockPos pos, Item carga) {
        Est est = est(pos);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (est.listo != null) {
            if (carga != null) { msg(p, "Tienes las manos ocupadas"); return; }
            manos.put(p.getUuid(), est.listo); est.listo = null;
            sonido(x, y, z, SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.0f);
        } else if (est.resultado != null) {
            msg(p, e.nombre + " está trabajando");
        } else if (carga == null) {
            if (est.dentro.isEmpty()) { msg(p, "Aquí se juntan los ingredientes de un plato"); return; }
            manos.put(p.getUuid(), est.dentro.remove(est.dentro.size() - 1));           // se puede recoger lo que se puso por error
            sonido(x, y, z, SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.0f);
        } else {
            if (!Recetas.sirveParaEmplatar(carga) || est.dentro.contains(carga) || est.dentro.size() >= 2) { msg(p, "Eso no va aquí: " + e.nombre.toLowerCase()); sonido(x, y, z, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.7f, 0.8f); return; }
            est.dentro.add(carga);
            manos.put(p.getUuid(), null);
            sonido(x, y, z, SoundEvents.BLOCK_WOOD_PLACE, 0.8f, 1.2f);
            if (est.dentro.size() == 2) {
                Item sale = Recetas.combina(est.dentro.get(0), est.dentro.get(1));
                if (sale == null) { msg(p, "Esos ingredientes no combinan"); manos.put(p.getUuid(), est.dentro.remove(1)); }
                else { est.resultado = sale; est.ticks = 0; est.total = Math.max(10, (int) Math.round(e.ticks * velocidad())); est.dentro.clear(); }
            }
        }
        dibujar(pos, e, est);
    }

    private void fregadero(ServerPlayerEntity p, Estacion e, BlockPos pos, Item carga) {
        Est est = est(pos);
        if (carga == CafeItems.PLATO_SUCIO) {
            if (est.cola >= 4) { msg(p, "El fregadero está lleno de platos"); return; }
            est.cola++; est.quien.add(p.getUuid());
            manos.put(p.getUuid(), null);
            sonido(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, SoundEvents.BLOCK_WOOD_PLACE, 0.8f, 0.8f);
            if (est.resultado == null) { est.resultado = CafeItems.PLATO_LIMPIO; est.ticks = 0; est.total = Math.max(10, (int) Math.round(e.ticks * velocidad())); }
        } else msg(p, carga == null ? "Aquí se lavan los platos sucios" : "Aquí solo se lavan platos sucios");
        dibujar(pos, e, est);
    }

    private void tickEstacion(BlockPos pos, Est est) {
        Estacion e = Estacion.de(w.getBlockState(pos).getBlock());
        if (e == null) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 1, z = pos.getZ() + 0.5;
        if (est.resultado != null) {
            est.ticks++;
            if (est.ticks >= est.total) {
                if (e.tipo == Estacion.Tipo.FREGADERO) {
                    est.cola--;
                    platos++;
                    if (!est.quien.isEmpty()) { ServerPlayerEntity lav = jugador(est.quien.remove(0)); if (lav != null) { Datos.Jugador dl = dj(lav); dl.lavados++; Progreso.evento(this, lav, dl, "lavados", 1); } }
                    sonido(x, y, z, SoundEvents.ENTITY_PLAYER_SPLASH, 0.5f, 1.6f);
                    w.spawnParticles(ParticleTypes.SPLASH, x, y + 0.3, z, 6, 0.2, 0.1, 0.2, 0.0);
                    if (est.cola > 0) { est.ticks = 0; } else est.resultado = null;
                } else {
                    est.listo = est.resultado; est.resultado = null; est.listoTicks = 0;
                    sonido(x, y, z, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.0f, 1.5f);
                    w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.2, z, 4, 0.2, 0.1, 0.2, 0.0);
                }
                dibujar(pos, e, est);
            } else if (est.ticks % 5 == 0) {
                dibujar(pos, e, est);
                if (e == Estacion.FREIDORA && est.ticks % 15 == 0) w.spawnParticles(ParticleTypes.SMOKE, x, y + 0.1, z, 2, 0.15, 0.05, 0.15, 0.0);
                if (e == Estacion.TABLA && est.ticks % 10 == 0) sonido(x, y, z, SoundEvents.BLOCK_WOOD_HIT, 0.5f, 1.4f);
                if (e == Estacion.FREGADERO && est.ticks % 12 == 0) sonido(x, y, z, SoundEvents.BLOCK_WATER_AMBIENT, 0.4f, 1.3f);
            }
        } else if (est.listo != null && e == Estacion.FREIDORA) {
            est.listoTicks++;
            if (est.listoTicks == QUEMA_AVISO) { sonido(x, y, z, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 0.6f); w.spawnParticles(ParticleTypes.SMOKE, x, y + 0.2, z, 8, 0.2, 0.1, 0.2, 0.01); dibujar(pos, e, est); }
            else if (est.listoTicks > QUEMA_AVISO && est.listoTicks % 8 == 0) w.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.3, z, 2, 0.15, 0.1, 0.15, 0.01);
            if (est.listoTicks >= QUEMA) {
                est.listo = CafeItems.PAPAS_QUEMADAS;
                sonido(x, y, z, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 1.0f);
                w.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.3, z, 12, 0.25, 0.15, 0.25, 0.02);
                aTodos(pl -> Msg.actionBar(pl, Text.literal("¡Se quemaron unas papas! Tíralas a la basura").formatted(Formatting.RED)));
                dibujar(pos, e, est);
            }
        }
    }

    /** Lo que se ve sobre la estacion: el objeto (el que se esta haciendo o el terminado) y una barra de avance. */
    private void dibujar(BlockPos pos, Estacion e, Est est) {
        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5, y = pos.getY() + e.alturaVisual();
        Item mostrar = est.listo != null ? est.listo : est.resultado != null && e.tipo != Estacion.Tipo.FREGADERO ? est.resultado : e.tipo == Estacion.Tipo.FREGADERO && est.cola > 0 ? CafeItems.PLATO_SUCIO : !est.dentro.isEmpty() ? est.dentro.get(0) : null;
        if (mostrar == null) { Disp.quitar(est.icono); Disp.quitar(est.barra); est.icono = est.barra = null; return; }
        if (est.icono == null || est.icono.isRemoved()) est.icono = Disp.objeto(w, CafeItems.id(mostrar), x, y + 0.2, z, 0.55f, true);
        else Disp.cambiarObjeto(est.icono, CafeItems.id(mostrar), x, y + 0.2, z, 0.55f, true);
        String barra;
        if (est.listo == CafeItems.PAPAS_QUEMADAS) barra = "{\"text\":\"¡Quemado!\",\"color\":\"red\",\"bold\":true}";
        else if (est.listo != null) barra = est.listoTicks >= QUEMA_AVISO ? "{\"text\":\"¡Se quema!\",\"color\":\"gold\",\"bold\":true}" : "{\"text\":\"¡Listo!\",\"color\":\"green\",\"bold\":true}";
        else if (est.resultado != null) {
            int n = Math.round(8f * est.ticks / Math.max(1, est.total));
            String extra = e.tipo == Estacion.Tipo.FREGADERO ? " x" + est.cola : "";
            barra = "{\"text\":\"" + "▮".repeat(n) + "▯".repeat(8 - n) + extra + "\",\"color\":\"gold\"}";
        } else barra = "{\"text\":\"" + est.dentro.size() + "/2\",\"color\":\"aqua\"}";
        if (est.barra == null || est.barra.isRemoved()) est.barra = Disp.texto(w, barra, x, y + 0.75, z, 0.9f, false);
        else Disp.cambiarTexto(est.barra, barra, x, y + 0.75, z, 0.9f, false);
    }

    private void servir(ServerPlayerEntity p, Mesa m, Item carga) {
        Cliente c = m.cliente;
        if (c == null || c.fase != Cliente.Fase.ESPERANDO) { msg(p, "Aquí no hay nadie esperando"); return; }
        if (carga == null) { msg(p, "Lleva un plato en las manos"); return; }
        if (carga != c.pedido) {
            msg(p, "Eso no es lo que pidió");
            sonido(c.e.getX(), c.e.getY(), c.e.getZ(), SoundEvents.ENTITY_VILLAGER_NO, 0.8f, 1.2f);
            return;
        }
        if (platos <= 0) { msg(p, "No quedan platos limpios: lava los sucios en el fregadero"); sonido(c.e.getX(), c.e.getY(), c.e.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.8f, 0.7f); return; }
        Recetas.Plato pl = Recetas.Plato.de(carga);
        int base = pl == null ? 10 : pl.precio;
        int propina = (int) Math.round(base * 0.6 * c.pacienciaRestante());
        int pago = base + propina;
        platos--;
        manos.put(p.getUuid(), null);
        c.comer();
        if (m.plato != null) Disp.quitar(m.plato);
        m.plato = Disp.objeto(w, CafeItems.id(carga), m.bloque.getX() + 0.5, m.bloque.getY() + 1.35, m.bloque.getZ() + 0.5, 0.5f, true);
        Datos.Jugador d = dj(p);
        d.monedas += pago; d.clientes++; d.monedasTotal += pago; d.propinasTotal += propina;
        servidos++; propinas += propina; ganado += pago;
        ganadoPor.merge(p.getUuid(), pago, Integer::sum);
        servidosPor.merge(p.getUuid(), 1, Integer::sum);
        Progreso.dar(this, d, p.getUuid(), 2);
        Progreso.evento(this, p, d, "servidos", 1);
        Progreso.evento(this, p, d, "ganado", pago);
        if (propina > 0) Progreso.evento(this, p, d, "propinas", propina);
        Progreso.revisarLogros(this, p, d);
        EmiCafe.datos.guardar(p.getUuid());
        sonido(c.e.getX(), c.e.getY(), c.e.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        sonido(c.e.getX(), c.e.getY(), c.e.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.8f);
        w.spawnParticles(ParticleTypes.HEART, c.e.getX(), c.e.getY() + 1.4, c.e.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        Msg.actionBar(p, Text.literal("+" + pago + " monedas" + (propina > 0 ? " (propina " + propina + ")" : "")).formatted(Formatting.GOLD, Formatting.BOLD));
    }

    // ------------------------------------------------------------ cerrar

    /** Quita clientes, objetos y bloques de dibujo. */
    void cerrar() {
        for (Cliente c : clientes) c.quitar();
        clientes.clear();
        for (Mesa m : mesas) { Disp.quitar(m.plato); Disp.quitar(m.sucio); m.plato = null; m.sucio = null; m.cliente = null; }
        for (Est e : ests.values()) { Disp.quitar(e.icono); Disp.quitar(e.barra); }
        ests.clear();
        Disp.barrer(w, ox + 7, 9, 30);
    }
}
