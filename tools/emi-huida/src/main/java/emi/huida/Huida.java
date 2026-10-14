package emi.huida;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import emi.huida.net.EstadoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * La huida: cada jugador corre por su copia del nivel mientras un Tyrantrum lo persigue. Hay que llegar a la meta sin que te alcance.
 * Choques con obstaculos y caidas cuentan como errores y te frenan (el perseguidor gana terreno). Flujo como las carreras:
 * /huida abrir -> los jugadores /huida unirse -> el organizador /huida empezar.
 */
public final class Huida {
    private Huida() {}

    public static final RegistryKey<World> MUNDO = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_huida", "nivel"));

    // ---- ajustes del prototipo (mas adelante iran a un archivo de configuracion)
    static final double VEL_JUGADOR = 0.13;            // caminar a 5,6 bloques/s (sin correr: se mantiene el hambre en 6)
    static final double VEL_PERSEGUIDOR_INI = 4.4;     // bloques/s
    static final double VEL_PERSEGUIDOR_MAX = 5.5;
    static final double ACELERACION = 0.03;            // bloques/s cada segundo
    static final double DIST_CAPTURA = 2.6;            // distancia entre el centro del Tyrantrum y el jugador a la que te atrapa
    static final int CUENTA_SEGUNDOS = 5;
    static final int ESPERA_FIN = 140;                 // ticks antes de devolver al jugador a su sitio
    static final float ALTURA_PERSEGUIDOR = 3.2f;

    static final boolean DEBUG = Boolean.getBoolean("emi.huida.debug") || Boolean.getBoolean("emi.carreras.debug");

    public enum Fase { LIBRE, COLA, CUENTA, CORRIENDO }
    enum Est { ESPERA, CORRE, ESCAPO, ATRAPADO }

    static final class J {
        final UUID id; final String nombre;
        int hueco = -1;
        Est est = Est.ESPERA;
        long inicioTick, finTick, soltarEn;
        int errores; long ultimoError;
        double zPerseguidor, vPerseguidor;
        PokemonEntity perseguidor;
        Set<BlockPos> rompibles;
        boolean desconectado;
        boolean enContacto;                 // estaba pegado a un obstaculo en el tick anterior
        J(ServerPlayerEntity p) { id = p.getUuid(); nombre = p.getGameProfile().getName(); }
    }

    private static MinecraftServer server;
    private static Fase fase = Fase.LIBRE;
    static final Map<UUID, J> jug = new LinkedHashMap<>();
    private static long cuentaFin, ultimoAviso;
    private static final List<UUID> pendientes = new ArrayList<>();      // jugadores a los que hay que devolver sus cosas al entrar
    private static final List<Long> pendientesTick = new ArrayList<>();

    public static Fase fase() { return fase; }
    static ServerWorld mundo() { return server.getWorld(MUNDO); }

    static void init(MinecraftServer s) {
        server = s;
        fase = Fase.LIBRE;
        jug.clear();
        if (mundo() == null) EmiHuida.LOG.warn("No existe la dimension emi_huida:nivel: /huida empezar no funcionara.");
    }

    static void shutdown() {
        if (server == null) return;
        for (J j : new ArrayList<>(jug.values())) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(j.id);
            if (p != null && Saved.exists(server, j.id)) Saved.restore(server, p);
        }
        jug.clear(); fase = Fase.LIBRE;
    }

    public static boolean participa(net.minecraft.entity.player.PlayerEntity p) {
        J j = jug.get(p.getUuid());
        return j != null && fase != Fase.COLA && fase != Fase.LIBRE && !j.desconectado && j.hueco >= 0;
    }

    // ---------------------------------------------------------------- comandos

    public static String abrir() {
        if (fase != Fase.LIBRE) return "Ya hay una huida en marcha o una cola abierta.";
        if (mundo() == null) return "No existe la dimension emi_huida:nivel (¿esta instalado el mod en el servidor?).";
        fase = Fase.COLA; jug.clear(); ultimoAviso = server.getTicks();
        avisoCola();
        return null;
    }

    private static void avisoCola() {
        Text t = Msg.say(Text.literal("¡Se abre la huida del Tyrantrum! ").formatted(Formatting.WHITE)
                .append(Msg.button("UNIRSE", "/huida unirse", Formatting.GREEN, "Apuntarte a la huida")));
        Msg.broadcast(server, t);
    }

    public static String cerrarCola() {
        if (fase != Fase.COLA) return "No hay ninguna cola abierta.";
        fase = Fase.LIBRE; jug.clear();
        Msg.broadcast(server, Msg.say("Se ha cerrado la cola de la huida."));
        return null;
    }

    public static String unirse(ServerPlayerEntity p) {
        if (fase != Fase.COLA) return "No hay ninguna huida abierta ahora mismo.";
        if (jug.containsKey(p.getUuid())) return "Ya estas apuntado.";
        if (jug.size() >= Nivel.MAX_HUECOS) return "La huida esta llena (" + Nivel.MAX_HUECOS + " jugadores).";
        if (p.getServerWorld().getRegistryKey().equals(MUNDO)) return "Ya estas en la dimension de la huida.";
        if (Morph.transformado(p.getUuid())) return "Destransformate antes (/untransform) para jugar la huida.";
        if (p.hasVehicle()) return "Bajate de lo que montas.";
        jug.put(p.getUuid(), new J(p));
        Msg.broadcastTo(onlineJugadores(), Msg.say(p.getGameProfile().getName() + " se ha apuntado (" + jug.size() + ")."));
        p.sendMessage(Msg.say("Estas apuntado. Espera a que el organizador empiece."), false);
        return null;
    }

    public static String salir(ServerPlayerEntity p) {
        J j = jug.get(p.getUuid());
        if (j == null) return "No estas en la huida.";
        if (fase == Fase.COLA) { jug.remove(p.getUuid()); p.sendMessage(Msg.say("Te has salido de la cola."), false); return null; }
        terminar(j, p, "has abandonado");
        comprobarFin();
        return null;
    }

    public static String empezar() {
        if (fase != Fase.COLA) return "No hay cola abierta (usa /huida abrir).";
        List<J> lista = new ArrayList<>();
        for (J j : jug.values()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(j.id);
            if (p != null && !p.isDisconnected()) lista.add(j);
        }
        if (lista.isEmpty()) return "No hay nadie apuntado.";
        ServerWorld w = mundo();
        if (w == null) return "No existe la dimension emi_huida:nivel.";
        jug.keySet().retainAll(lista.stream().map(j -> j.id).toList());
        limpiarEntidades(w);
        int h = 0;
        for (J j : lista) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(j.id);
            try { Saved.save(server, p); } catch (RuntimeException e) { jug.remove(j.id); p.sendMessage(Msg.err("No se pudo guardar tu inventario: no entras."), false); continue; }
            j.hueco = h++;
            j.rompibles = Nivel.construir(w, j.hueco);
            j.zPerseguidor = Nivel.Z_PERSEGUIDOR; j.vPerseguidor = Cfg.velIni();
            j.est = Est.ESPERA; j.errores = 0; j.ultimoError = 0; j.desconectado = false;
            colocarEnSalida(p, w, j);
            prepararJugador(p);
            congelar(p, true);
            ServerPlayNetworking.send(p, new EstadoPayload(true));
            j.perseguidor = crearPerseguidor(w, j);
        }
        if (jug.isEmpty()) { fase = Fase.LIBRE; return "No se pudo preparar a nadie."; }
        fase = Fase.CUENTA;
        cuentaFin = server.getTicks() + CUENTA_SEGUNDOS * 20L;
        Msg.broadcastTo(online(), Msg.say("¡Todos a su sitio! La huida empieza en " + CUENTA_SEGUNDOS + "..."));
        return null;
    }

    public static String parar(String motivo) {
        if (fase == Fase.LIBRE) return "No hay ninguna huida.";
        Text t = Msg.say("La huida se ha cancelado" + (motivo == null ? "." : ": " + motivo));
        for (J j : new ArrayList<>(jug.values())) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(j.id);
            if (p != null) p.sendMessage(t, false);
            if (fase != Fase.COLA) restaurar(j, p);
        }
        limpiar();
        return null;
    }

    static String estado() {
        return switch (fase) {
            case LIBRE -> "No hay ninguna huida.";
            case COLA -> "Cola abierta: " + jug.size() + " jugador(es) apuntado(s).";
            case CUENTA -> "Cuenta atras en marcha.";
            case CORRIENDO -> "Huida en marcha (" + jug.values().stream().filter(j -> j.est == Est.CORRE).count() + " corriendo).";
        };
    }

    // ---------------------------------------------------------------- tick

    static void tick(MinecraftServer s) {
        if (server == null) return;
        long now = s.getTicks();
        if (!pendientes.isEmpty()) {
            for (int i = pendientes.size() - 1; i >= 0; i--) if (now >= pendientesTick.get(i)) {
                ServerPlayerEntity p = s.getPlayerManager().getPlayer(pendientes.get(i));
                if (p != null && !participa(p) && Saved.exists(s, p.getUuid())) { Saved.restore(s, p); p.sendMessage(Msg.say("Te hemos devuelto tus cosas de la huida."), false); }
                pendientes.remove(i); pendientesTick.remove(i);
            }
        }
        switch (fase) {
            case LIBRE -> {}
            case COLA -> { if (now - ultimoAviso >= 1200) { ultimoAviso = now; avisoCola(); } }
            case CUENTA -> tickCuenta(s, now);
            case CORRIENDO -> tickCorriendo(s, now);
        }
    }

    private static void tickCuenta(MinecraftServer s, long now) {
        long resta = cuentaFin - now;
        ServerWorld w = mundo();
        // durante la cuenta atras las copias estan quietas; el Tyrantrum ruge al final
        if (resta % 20 == 0 && resta > 0) {
            int seg = (int) (resta / 20);
            Text t = Text.literal(String.valueOf(seg)).formatted(seg <= 2 ? Formatting.RED : Formatting.GOLD, Formatting.BOLD);
            for (J j : jug.values()) {
                ServerPlayerEntity p = jugador(j);
                if (p == null) continue;
                Msg.title(p, t, Text.literal("¡El Tyrantrum despierta!").formatted(Formatting.YELLOW), 0, 22, 4);
                Msg.sound(p, net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), 1f, 0.6f);
            }
        }
        if (resta == 30) for (J j : jug.values()) { ServerPlayerEntity p = jugador(j); if (p != null) Msg.sound(p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.2f, 0.5f); }
        if (resta <= 0) {
            fase = Fase.CORRIENDO;
            EmiHuida.LOG.info("Huida: ¡YA! ({} jugador(es))", jug.size());
            for (J j : jug.values()) {
                ServerPlayerEntity p = jugador(j);
                if (p == null) continue;
                congelar(p, false);
                j.est = Est.CORRE; j.inicioTick = now;
                Msg.title(p, Text.literal("¡CORRE!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Llega a la puerta dorada").formatted(Formatting.WHITE), 0, 30, 10);
                Msg.sound(p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.5f, 0.4f);
            }
        }
    }

    private static void tickCorriendo(MinecraftServer s, long now) {
        ServerWorld w = mundo();
        for (J j : new ArrayList<>(jug.values())) {
            ServerPlayerEntity p = jugador(j);
            if (p == null) continue;
            if (j.est == Est.CORRE) tickJugador(w, j, p, now);
            else if (now >= j.soltarEn) { terminar(j, p, null); }
        }
        comprobarFin();
    }

    private static void tickJugador(ServerWorld w, J j, ServerPlayerEntity p, long now) {
        double ox = Nivel.ox(j.hueco);
        double lz = p.getZ(), lx = p.getX() - ox;
        if (now % 10 == 0) { p.getHungerManager().setFoodLevel(6); p.getHungerManager().setSaturationLevel(0f); }

        if (DEBUG && now % 2 == 0) EmiHuida.LOG.info("dbg t={} x={} y={} z={} col={} sneak={} pose={} chaser={}", String.format("%.1f", (now - j.inicioTick) / 20.0), String.format("%.2f", lx), String.format("%.2f", p.getY()), String.format("%.2f", lz), p.horizontalCollision, p.isSneaking(), p.getPose(), String.format("%.1f", j.zPerseguidor));

        // fuera del pasillo (no deberia pasar): vuelta a la salida
        if (Math.abs(lx) > 4.2 || lz < Nivel.Z_ATRAS || p.getWorld() != w) { reponer(w, j, p, Nivel.Z_PERSEGUIDOR + 20); }

        // caida al hueco
        if (p.getY() < Nivel.Y0 - 1.5) {
            error(j, p, now, "¡Caiste!");
            reponer(w, j, p, Nivel.Z_ANTES_HUECO);
        }
        // choque con un obstaculo
        else {
            boolean toca = tocaObstaculo(j, p);
            // solo cuenta error al EMPEZAR a tocar (si sigues pegado al obstaculo no se repite cada segundo)
            if (toca && !j.enContacto && now - j.ultimoError >= 10) {
                error(j, p, now, "¡Tropezaste!");
                p.setVelocity(0, 0.12, -0.4);
                p.velocityModified = true;
            }
            j.enContacto = toca;
        }

        // el perseguidor
        double seg = (now - j.inicioTick) / 20.0;
        j.vPerseguidor = Math.min(Cfg.velMax(), Cfg.velIni() + ACELERACION * seg);
        j.zPerseguidor += j.vPerseguidor / 20.0;
        if (j.perseguidor != null && !j.perseguidor.isRemoved()) {
            j.perseguidor.refreshPositionAndAngles(ox + 0.5, Nivel.Y0, j.zPerseguidor, 0f, 0f);
            j.perseguidor.setHeadYaw(0f); j.perseguidor.setBodyYaw(0f);
            j.perseguidor.setVelocity(0, 0, j.vPerseguidor / 20.0);
        }
        romper(w, j);
        double dist = lz - j.zPerseguidor;
        if (now % 8 == 0) {
            float vol = (float) Math.max(0.15, Math.min(1.2, 1.4 - dist / 25.0));
            Msg.sound(p, SoundEvents.ENTITY_IRON_GOLEM_STEP, vol, 0.55f);
        }

        // atrapado / meta
        if (dist <= DIST_CAPTURA) { acabar(j, p, now, false); return; }
        if (lz >= Nivel.Z_META) { acabar(j, p, now, true); return; }

        if (now % 4 == 0) Msg.actionBar(p, barra(j, dist, now));
    }

    /**
     * El servidor no marca {@code horizontalCollision} en los jugadores (su movimiento lo calcula el cliente), asi que se comprueba a mano:
     * el jugador esta pegado (a menos de 8 cm) a un bloque de obstaculo. Estar ENCIMA de un tronco no cuenta (solo se expande en horizontal).
     */
    private static boolean tocaObstaculo(J j, ServerPlayerEntity p) {
        if (j.rompibles == null) return false;
        net.minecraft.util.math.Box caja = p.getBoundingBox().expand(0.08, 0, 0.08);
        double z = p.getZ();
        for (BlockPos b : j.rompibles) {
            if (Math.abs(b.getZ() + 0.5 - z) > 2.0) continue;
            if (caja.intersects(b.getX(), b.getY(), b.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1)) {
                // una losa superior (raices) solo ocupa la mitad de arriba del bloque
                if (p.getWorld().getBlockState(b).getBlock() instanceof net.minecraft.block.SlabBlock
                        && !caja.intersects(b.getX(), b.getY() + 0.5, b.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1)) continue;
                return true;
            }
        }
        return false;
    }

    private static void error(J j, ServerPlayerEntity p, long now, String msg) {
        j.errores++; j.ultimoError = now;
        EmiHuida.LOG.info("Huida: {} error {} ({}) en z={}", j.nombre, j.errores, msg, (int) p.getZ());
        Msg.title(p, Text.literal(msg).formatted(Formatting.RED, Formatting.BOLD), Text.literal("Errores: " + j.errores).formatted(Formatting.GRAY), 0, 15, 5);
        Msg.sound(p, SoundEvents.ENTITY_PLAYER_HURT, 0.8f, 0.8f);
    }

    private static void reponer(ServerWorld w, J j, ServerPlayerEntity p, int z) {
        p.teleport(w, Nivel.ox(j.hueco) + 0.5, Nivel.Y0, z + 0.5, 0f, 0f);
        p.setVelocity(0, 0, 0); p.velocityModified = true;
        p.fallDistance = 0;
    }

    /** El Tyrantrum destruye los obstaculos que alcanza (su frente esta ~3 bloques por delante de su centro). */
    private static void romper(ServerWorld w, J j) {
        if (j.rompibles == null || j.rompibles.isEmpty()) return;
        Iterator<BlockPos> it = j.rompibles.iterator();
        while (it.hasNext()) {
            BlockPos b = it.next();
            if (b.getZ() <= j.zPerseguidor + 3.2) { w.breakBlock(b, false); it.remove(); }
        }
    }

    private static Text barra(J j, double dist, long now) {
        int n = (int) Math.max(0, Math.min(10, Math.round(dist / 2.0)));
        Formatting c = dist < 8 ? Formatting.RED : dist < 16 ? Formatting.GOLD : Formatting.GREEN;
        String bar = "■".repeat(n) + "□".repeat(10 - n);
        Text base = Text.literal("Tyrantrum a " + Math.max(0, (int) dist) + " m  " ).formatted(c, Formatting.BOLD).append(Text.literal(bar).formatted(c));
        double tiempo = (now - j.inicioTick) / 20.0;
        return base.copy().append(Text.literal("   Errores: " + j.errores + "   " + String.format(java.util.Locale.ROOT, "%.1f", tiempo) + " s").formatted(Formatting.WHITE));
    }

    private static void acabar(J j, ServerPlayerEntity p, long now, boolean escapo) {
        j.est = escapo ? Est.ESCAPO : Est.ATRAPADO;
        j.finTick = now; j.soltarEn = now + ESPERA_FIN;
        double t = (now - j.inicioTick) / 20.0;
        String tiempo = String.format(java.util.Locale.ROOT, "%.1f s", t);
        if (escapo) {
            String medalla = j.errores == 0 ? "ORO" : j.errores <= 2 ? "PLATA" : "BRONCE";
            Formatting mc = j.errores == 0 ? Formatting.GOLD : j.errores <= 2 ? Formatting.GRAY : Formatting.RED;
            Msg.title(p, Text.literal("¡ESCAPASTE!").formatted(Formatting.GREEN, Formatting.BOLD), Text.literal("Medalla de " + medalla + " · " + tiempo + " · " + j.errores + " errores").formatted(mc), 0, 100, 20);
            p.sendMessage(Msg.say("¡Escapaste del Tyrantrum en " + tiempo + " con " + j.errores + " error(es)! Medalla de " + medalla + "."), false);
            Msg.sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        } else {
            Msg.title(p, Text.literal("¡TE ATRAPÓ!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Aguantaste " + tiempo + " · " + j.errores + " errores").formatted(Formatting.GRAY), 0, 100, 20);
            p.sendMessage(Msg.say("El Tyrantrum te atrapó tras " + tiempo + "."), false);
            Msg.sound(p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.5f, 0.5f);
            Msg.sound(p, SoundEvents.ENTITY_PLAYER_HURT, 1f, 0.6f);
        }
        EmiHuida.LOG.info("Huida: {} {} en {} con {} errores", j.nombre, escapo ? "ESCAPO" : "ATRAPADO", tiempo, j.errores);
        if (!escapo) congelar(p, true);
    }

    /** Devuelve al jugador a lo que tenia y lo saca de la huida. */
    private static void terminar(J j, ServerPlayerEntity p, String motivo) {
        restaurar(j, p);
        jug.remove(j.id);
        if (p != null && motivo != null) p.sendMessage(Msg.say("Sales de la huida: " + motivo + "."), false);
    }

    private static void restaurar(J j, ServerPlayerEntity p) {
        if (j.perseguidor != null) { j.perseguidor.discard(); j.perseguidor = null; }
        if (p == null) return;
        ServerPlayNetworking.send(p, new EstadoPayload(false));
        congelar(p, false);
        if (!Saved.restore(server, p)) quitarEfectos(p);
    }

    private static void comprobarFin() {
        if (fase != Fase.CORRIENDO && fase != Fase.CUENTA) return;
        if (jug.isEmpty()) limpiar();
    }

    private static void limpiar() {
        ServerWorld w = mundo();
        if (w != null) {
            for (int h = 0; h < Nivel.MAX_HUECOS; h++) Nivel.chunks(w, h, false);
            limpiarEntidades(w);
        }
        jug.clear(); fase = Fase.LIBRE;
    }

    private static void limpiarEntidades(ServerWorld w) {
        List<Entity> borrar = new ArrayList<>();
        for (Entity e : w.iterateEntities()) if (e instanceof PokemonEntity) borrar.add(e);
        for (Entity e : borrar) e.discard();
    }

    // ---------------------------------------------------------------- entrar / salir del servidor

    static void alEntrar(ServerPlayerEntity p) {
        if (server == null) return;
        if (Saved.exists(server, p.getUuid()) && !participa(p)) { pendientes.add(p.getUuid()); pendientesTick.add(server.getTicks() + 40L); }
    }

    static void alSalir(ServerPlayerEntity p) {
        J j = jug.get(p.getUuid());
        if (j == null) return;
        if (fase == Fase.COLA) { jug.remove(p.getUuid()); return; }
        if (j.perseguidor != null) { j.perseguidor.discard(); j.perseguidor = null; }
        j.desconectado = true;
        jug.remove(p.getUuid());      // sus cosas siguen guardadas en disco: se le devuelven al volver a entrar
        comprobarFin();
    }

    // ---------------------------------------------------------------- ayudas

    private static ServerPlayerEntity jugador(J j) {
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(j.id);
        return p == null || p.isDisconnected() ? null : p;
    }

    private static List<ServerPlayerEntity> online() {
        List<ServerPlayerEntity> l = new ArrayList<>();
        for (J j : jug.values()) { ServerPlayerEntity p = jugador(j); if (p != null) l.add(p); }
        return l;
    }

    private static List<ServerPlayerEntity> onlineJugadores() { return online(); }

    private static void colocarEnSalida(ServerPlayerEntity p, ServerWorld w, J j) {
        p.teleport(w, Nivel.ox(j.hueco) + 0.5, Nivel.Y0, 0.5, 0f, 0f);
        p.setVelocity(0, 0, 0); p.velocityModified = true;
    }

    private static final Identifier ID_VEL = Identifier.of("emi_huida", "velocidad");
    private static final Identifier ID_CONG = Identifier.of("emi_huida", "congelado");
    private static final Identifier ID_SALTO = Identifier.of("emi_huida", "sin_salto");
    private static final Identifier ID_AGACHADO = Identifier.of("emi_huida", "agachado");
    private static final Identifier ID_SALTO_EXTRA = Identifier.of("emi_huida", "salto_extra");
    /** Salto un poco mas potente que el normal (0,42 -> 0,50: ~1,8 bloques de alto y ~4 de largo): con el normal el margen para saltar un tronco de 1 bloque caminando era de ~0,1 s. */
    static final double SALTO_EXTRA = 0.08;

    private static void atributo(ServerPlayerEntity p, RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> a, Identifier id, double v, EntityAttributeModifier.Operation op, boolean poner) {
        var inst = p.getAttributeInstance(a);
        if (inst == null) return;
        inst.removeModifier(id);
        if (poner) inst.addTemporaryModifier(new EntityAttributeModifier(id, v, op));
    }

    /** Modo huida: aventura, inmortal, sin inventario, velocidad fija (0,13 = 5,6 bloques/s caminando) y agacharse a la misma velocidad que caminar. */
    private static void prepararJugador(ServerPlayerEntity p) {
        p.clearStatusEffects();
        p.getInventory().clear();
        p.changeGameMode(GameMode.ADVENTURE);
        p.setInvulnerable(true);
        p.getHungerManager().setFoodLevel(6); p.getHungerManager().setSaturationLevel(0f);
        var vel = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (vel != null) { vel.removeModifier(ID_VEL); vel.addTemporaryModifier(new EntityAttributeModifier(ID_VEL, VEL_JUGADOR - vel.getBaseValue(), EntityAttributeModifier.Operation.ADD_VALUE)); }
        atributo(p, EntityAttributes.PLAYER_SNEAKING_SPEED, ID_AGACHADO, 0.7, EntityAttributeModifier.Operation.ADD_VALUE, true);   // 0,3 + 0,7 = 1,0
        atributo(p, EntityAttributes.GENERIC_JUMP_STRENGTH, ID_SALTO_EXTRA, SALTO_EXTRA, EntityAttributeModifier.Operation.ADD_VALUE, true);
    }

    private static void congelar(ServerPlayerEntity p, boolean si) {
        atributo(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, ID_CONG, -1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, si);
        atributo(p, EntityAttributes.GENERIC_JUMP_STRENGTH, ID_SALTO, -1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, si);
    }

    static void quitarEfectos(ServerPlayerEntity p) {
        p.setInvulnerable(false);
        atributo(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, ID_VEL, 0, EntityAttributeModifier.Operation.ADD_VALUE, false);
        atributo(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, ID_CONG, 0, EntityAttributeModifier.Operation.ADD_VALUE, false);
        atributo(p, EntityAttributes.GENERIC_JUMP_STRENGTH, ID_SALTO, 0, EntityAttributeModifier.Operation.ADD_VALUE, false);
        atributo(p, EntityAttributes.PLAYER_SNEAKING_SPEED, ID_AGACHADO, 0, EntityAttributeModifier.Operation.ADD_VALUE, false);
        atributo(p, EntityAttributes.GENERIC_JUMP_STRENGTH, ID_SALTO_EXTRA, 0, EntityAttributeModifier.Operation.ADD_VALUE, false);
    }

    private static PokemonEntity crearPerseguidor(ServerWorld w, J j) {
        try {
            PokemonProperties props = PokemonProperties.Companion.parse("tyrantrum level=60");
            PokemonEntity e = props.createEntity(w);
            var sp = e.getPokemon().getSpecies();
            float h = Math.max(sp.getHitbox().height(), sp.getHitbox().width() * 0.8f) * sp.getBaseScale();
            if (h > 0.01f) e.getPokemon().setScaleModifier(Math.max(0.3f, Math.min(3f, ALTURA_PERSEGUIDOR / h)));
            e.getPokemon().setNickname(Text.literal("Tyrantrum furioso").formatted(Formatting.RED));
            e.setAiDisabled(true);
            e.setInvulnerable(true);
            e.setNoGravity(true);
            e.setSilent(true);
            e.setPersistent();
            e.refreshPositionAndAngles(Nivel.ox(j.hueco) + 0.5, Nivel.Y0, Nivel.Z_PERSEGUIDOR, 0f, 0f);
            w.spawnEntity(e);
            return e;
        } catch (Throwable t) {
            EmiHuida.LOG.error("No se pudo crear al Tyrantrum", t);
            return null;
        }
    }
}
