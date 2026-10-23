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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * El cafe de un jugador: su local, sus clientes, el estado de cada estacion y lo que lleva en las manos. Todo el juego pasa aqui; {@link Cafes} solo reparte los locales.
 * Lo que se lleva en las manos es el objeto de verdad en la mano del jugador (hueco 1 de la barra, que no se puede cambiar) y no en el inventario.
 */
final class Cafe {
    /** Lo que hay en una estacion de proceso: lo que se esta haciendo, el avance y el plato terminado que espera a que lo recojan. */
    static final class Est {
        Item resultado;                       // lo que saldra (null = no esta trabajando)
        int ticks, total;
        Item listo;                           // terminado y esperando
        Entity icono, barra;
    }

    final UUID jugador;
    final MinecraftServer server;
    final ServerWorld w;
    final int ox, slot;
    final Random rnd = new Random();
    final List<Mesa> mesas = new ArrayList<>();
    final List<Cliente> clientes = new ArrayList<>();
    final Map<BlockPos, Est> ests = new HashMap<>();
    Item carga;                               // lo que lleva en las manos (null = nada)
    int t;                                    // ticks desde que entro
    int proximoCliente = 60;
    int ganado;                               // lo ganado desde que entro

    Cafe(MinecraftServer server, ServerWorld w, UUID jugador, int slot) {
        this.server = server; this.w = w; this.jugador = jugador; this.slot = slot; this.ox = slot * Local.SEPARACION;
        for (int[] m : Local.MESAS) mesas.add(new Mesa(Local.pos(ox, m[0], 1, m[1])));
    }

    ServerPlayerEntity jugador() { return server.getPlayerManager().getPlayer(jugador); }

    // ------------------------------------------------------------ lo que hace el local

    Item elegirPedido(Random r) { return CafeItems.PAPAS_FRITAS; }

    void alEnfadarse(Cliente c) {
        ServerPlayerEntity p = jugador();
        if (p != null) { Msg.actionBar(p, Text.literal("Un cliente se fue enfadado...").formatted(Formatting.RED)); }
        sonido(c.e.getX(), c.e.getY(), c.e.getZ(), SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    void sonido(double x, double y, double z, SoundEvent s, float vol, float pitch) { w.playSound(null, x, y, z, s, SoundCategory.BLOCKS, vol, pitch); }

    private Datos.Jugador datos(ServerPlayerEntity p) { return EmiCafe.datos.jugador(p.getUuid(), p.getGameProfile().getName()); }

    // ------------------------------------------------------------ cada tick

    void tick() {
        ServerPlayerEntity p = jugador();
        if (p == null) return;
        t++;
        if (p.getY() < Local.Y - 8 || p.getServerWorld() != w) p.teleport(w, ox + Local.SX, Local.Y + 1, Local.SZ, 180f, 0f);
        // clientes nuevos
        if (--proximoCliente <= 0) {
            proximoCliente = 20 * (14 + rnd.nextInt(8));
            nuevoCliente();
        }
        for (int i = clientes.size() - 1; i >= 0; i--) {
            Cliente c = clientes.get(i);
            boolean fin = c.fuera() || c.tick(this, rnd);
            if (c.fase == Cliente.Fase.SALIENDO && c.mesa.cliente == c) { c.mesa.cliente = null; Disp.quitar(c.mesa.plato); c.mesa.plato = null; }
            if (fin) { c.quitar(); if (c.mesa.cliente == c) c.mesa.cliente = null; clientes.remove(i); }
        }
        // estaciones
        for (Map.Entry<BlockPos, Est> en : ests.entrySet()) tickEstacion(en.getKey(), en.getValue());
        // las manos y el inventario
        if (t % 2 == 0) sincronizarMano(p);
        if (t % 10 == 0) {
            p.getHungerManager().setFoodLevel(20);
            p.setHealth(p.getMaxHealth());
            Msg.actionBar(p, hud());
            for (Entity e : w.getEntitiesByType(EntityType.ITEM, new Box(ox - 4, Local.Y - 2, -4, ox + 20, Local.Y + 8, 24), x -> true)) e.discard();
        }
    }

    private Text hud() {
        Datos.Jugador d = datos(jugador());
        Text mano = carga == null ? Text.literal("manos vacías").formatted(Formatting.GRAY) : Text.literal(CafeItems.nombre(carga)).formatted(Formatting.YELLOW);
        return Text.literal("Monedas: ").formatted(Formatting.GOLD).append(Text.literal(String.valueOf(d.monedas)).formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal("    Llevas: ").formatted(Formatting.GOLD)).append(mano);
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

    // ------------------------------------------------------------ las manos

    /** El objeto de la mano del jugador tiene que ser lo que dice {@code carga}; el resto del inventario, vacio, y el hueco 1 elegido. */
    void sincronizarMano(ServerPlayerEntity p) {
        var inv = p.getInventory();
        boolean cambio = false;
        ItemStack quiero = carga == null ? ItemStack.EMPTY : new ItemStack(carga);
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (i == 0) { if (!ItemStack.areItemsEqual(st, quiero) || st.getCount() != quiero.getCount()) { inv.setStack(0, quiero); cambio = true; } }
            else if (!st.isEmpty()) { inv.setStack(i, ItemStack.EMPTY); cambio = true; }
        }
        if (inv.selectedSlot != 0) { inv.selectedSlot = 0; p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket(0)); }
        if (cambio) { p.currentScreenHandler.sendContentUpdates(); p.playerScreenHandler.sendContentUpdates(); }
    }

    void msg(ServerPlayerEntity p, String s) { Msg.actionBar(p, Text.literal(s).formatted(Formatting.YELLOW)); }

    // ------------------------------------------------------------ usar bloques y clientes

    /** El jugador hace clic derecho en un bloque del local. */
    void usarBloque(ServerPlayerEntity p, BlockPos pos) {
        Estacion e = Estacion.de(w.getBlockState(pos).getBlock());
        if (e == null) return;
        switch (e) {
            case CAJA_PAPAS -> {
                if (carga != null) { msg(p, "Ya llevas algo en las manos"); return; }
                carga = CafeItems.PAPA;
                sonido(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);
            }
            case BASURA -> {
                if (carga == null) { msg(p, "No llevas nada que tirar"); return; }
                carga = null;
                sonido(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, SoundEvents.BLOCK_COMPOSTER_FILL, 0.8f, 1.0f);
            }
            case TABLA, FREIDORA -> estacion(p, e, pos);
            case MESA -> {
                Mesa m = null;
                for (Mesa x : mesas) if (x.bloque.equals(pos)) m = x;
                if (m != null) servir(p, m);
            }
        }
        sincronizarMano(p);
    }

    /** Clic derecho a un Pokemon: si es un cliente que espera, es como tocar su mesa. */
    boolean usarEntidad(ServerPlayerEntity p, Entity en) {
        for (Cliente c : clientes) if (c.e == en) { servir(p, c.mesa); sincronizarMano(p); return true; }
        return false;
    }

    private void estacion(ServerPlayerEntity p, Estacion e, BlockPos pos) {
        Est est = ests.computeIfAbsent(pos, k -> new Est());
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (est.listo != null) {
            if (carga != null) { msg(p, "Tienes las manos ocupadas"); return; }
            carga = est.listo; est.listo = null;
            sonido(x, y, z, SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.0f);
            dibujar(pos, e, est);
        } else if (est.resultado != null) {
            msg(p, e.nombre + " está trabajando");
        } else if (carga == null) {
            msg(p, "Trae algo para la " + e.nombre.toLowerCase());
        } else {
            Item sale = e.resultado(carga);
            if (sale == null) { msg(p, "Eso no va en la " + e.nombre.toLowerCase()); sonido(x, y, z, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.7f, 0.8f); return; }
            est.resultado = sale; est.ticks = 0; est.total = e.ticks;
            carga = null;
            sonido(x, y, z, e == Estacion.FREIDORA ? SoundEvents.BLOCK_BREWING_STAND_BREW : SoundEvents.BLOCK_WOOD_HIT, 0.8f, 1.0f);
            dibujar(pos, e, est);
        }
    }

    private void tickEstacion(BlockPos pos, Est est) {
        if (est.resultado == null) return;
        Estacion e = Estacion.de(w.getBlockState(pos).getBlock());
        if (e == null) return;
        est.ticks++;
        if (est.ticks >= est.total) {
            est.listo = est.resultado; est.resultado = null;
            sonido(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.0f, 1.5f);
            w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.0);
            dibujar(pos, e, est);
        } else if (est.ticks % 5 == 0) {
            dibujar(pos, e, est);
            if (e == Estacion.FREIDORA && est.ticks % 15 == 0) w.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 2, 0.15, 0.05, 0.15, 0.0);
            if (e == Estacion.TABLA && est.ticks % 10 == 0) sonido(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, SoundEvents.BLOCK_WOOD_HIT, 0.5f, 1.4f);
        }
    }

    /** Lo que se ve sobre la estacion: el objeto (el que se esta haciendo o el terminado) y una barra de avance. */
    private void dibujar(BlockPos pos, Estacion e, Est est) {
        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5, y = pos.getY() + e.alturaVisual();
        Item mostrar = est.listo != null ? est.listo : est.resultado;
        if (mostrar == null) { Disp.quitar(est.icono); Disp.quitar(est.barra); est.icono = est.barra = null; return; }
        if (est.icono == null || est.icono.isRemoved()) est.icono = Disp.objeto(w, CafeItems.id(mostrar), x, y + 0.2, z, 0.55f, true);
        else Disp.cambiarObjeto(est.icono, CafeItems.id(mostrar), x, y + 0.2, z, 0.55f, true);
        String barra;
        if (est.listo != null) barra = "{\"text\":\"¡Listo!\",\"color\":\"green\",\"bold\":true}";
        else {
            int n = Math.round(8f * est.ticks / Math.max(1, est.total));
            barra = "{\"text\":\"" + "▮".repeat(n) + "▯".repeat(8 - n) + "\",\"color\":\"gold\"}";
        }
        if (est.barra == null || est.barra.isRemoved()) est.barra = Disp.texto(w, barra, x, y + 0.75, z, 0.8f, false);
        else Disp.cambiarTexto(est.barra, barra, x, y + 0.75, z, 0.8f, false);
    }

    private void servir(ServerPlayerEntity p, Mesa m) {
        Cliente c = m.cliente;
        if (c == null || c.fase != Cliente.Fase.ESPERANDO) { msg(p, "Aquí no hay nadie esperando"); return; }
        if (carga == null) { msg(p, "Lleva un plato en las manos"); return; }
        if (carga != c.pedido) {
            msg(p, "Eso no es lo que pidió");
            sonido(c.e.getX(), c.e.getY(), c.e.getZ(), SoundEvents.ENTITY_VILLAGER_NO, 0.8f, 1.2f);
            return;
        }
        int propina = (int) Math.round(8 * c.pacienciaRestante());
        int pago = 12 + propina;
        Item plato = carga;
        carga = null;
        c.comer();
        if (m.plato != null) Disp.quitar(m.plato);
        m.plato = Disp.objeto(w, CafeItems.id(plato), m.bloque.getX() + 0.5, m.bloque.getY() + 1.35, m.bloque.getZ() + 0.5, 0.5f, true);
        Datos.Jugador d = datos(p);
        d.monedas += pago; d.clientes++; ganado += pago;
        EmiCafe.datos.guardar(p.getUuid());
        sonido(c.e.getX(), c.e.getY(), c.e.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        sonido(c.e.getX(), c.e.getY(), c.e.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.8f);
        w.spawnParticles(ParticleTypes.HEART, c.e.getX(), c.e.getY() + 1.4, c.e.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        Msg.actionBar(p, Text.literal("+" + pago + " monedas").formatted(Formatting.GOLD, Formatting.BOLD));
    }

    // ------------------------------------------------------------ cerrar

    /** Quita clientes, objetos y bloques de dibujo (el local se vuelve a construir en la siguiente visita). */
    void cerrar() {
        for (Cliente c : clientes) c.quitar();
        clientes.clear();
        for (Mesa m : mesas) { Disp.quitar(m.plato); m.plato = null; m.cliente = null; }
        for (Est e : ests.values()) { Disp.quitar(e.icono); Disp.quitar(e.barra); }
        ests.clear();
        Disp.barrer(w, ox + 7, 9, 30);
    }
}
