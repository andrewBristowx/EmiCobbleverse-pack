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

    public static final RegistryKey<World> MUNDO = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_huida", "selva"));

    // ---- ajustes del prototipo (mas adelante iran a un archivo de configuracion)
    // el jugador corre siempre (el cliente fuerza el sprint): velocidad normal de Minecraft, 5,6 bloques/s corriendo y un salto largo de ~4 bloques
    static final double VEL_BASE = 5.3;                // velocidad del Tyrantrum si va a la distancia que toca (un poco menos que el jugador)
    static final double DIST_INI = 12.0, DIST_FIN = 8.5;   // distancia que intenta mantener: empieza lejos y se va acercando con el tiempo
    static final double SEG_HASTA_FIN = 150.0;
    static final double CATCH_UP = 0.8;                // blocs/s extra por cada bloque que le saques de mas (para que no te escapes del todo)
    static final double DIST_CAPTURA = 6.0;            // distancia entre el centro del Tyrantrum (muy grande) y el jugador a la que te atrapa
    static final double FRENTE_PERSEGUIDOR = 6.5;      // hasta donde alcanza su boca por delante de su centro: destruye lo que haya ahi
    static final int CUENTA_SEGUNDOS = 5;
    static final int ESPERA_FIN = 140;                 // ticks antes de devolver al jugador a su sitio
    static final float ALTURA_PERSEGUIDOR = 9.0f;

    static final boolean DEBUG = Boolean.getBoolean("emi.huida.debug") || Boolean.getBoolean("emi.carreras.debug");

    public enum Fase { LIBRE, COLA, CUENTA, CORRIENDO }
    enum Est { ESPERA, CORRE, ESCAPO, ATRAPADO }

    static final class J {
        final UUID id; final String nombre;
        int hueco = -1;
        Est est = Est.ESPERA;
        long inicioTick, finTick, soltarEn;
        int errores; long ultimoError;
        int idx;                                     // muestra del camino en la que va (para proyectar su posicion)
        double s, sPerseguidor, vPerseguidor;        // distancia recorrida por el jugador y por el Tyrantrum a lo largo del trazado
        PokemonEntity perseguidor;
        Nivel.Construido nivel;
        int sigObst, sigPuente;                      // primer obstaculo / tabla que el Tyrantrum aun no ha destruido
        java.util.Set<BlockPos> tablas = new java.util.HashSet<>();
        final Map<BlockPos, Long> colapsos = new LinkedHashMap<>();   // tablas del puente que se van a romper (cuando)
        boolean desconectado;
        boolean enContacto;                          // estaba pegado a un obstaculo en el tick anterior
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
        if (mundo() == null) EmiHuida.LOG.warn("No existe la dimension emi_huida:selva: /huida empezar no funcionara.");
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
        if (mundo() == null) return "No existe la dimension emi_huida:selva (¿esta instalado el mod en el servidor?).";
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
        if (w == null) return "No existe la dimension emi_huida:selva.";
        jug.keySet().retainAll(lista.stream().map(j -> j.id).toList());
        limpiarEntidades(w);
        // primero se construye todo (si falla, nadie ha sido tocado) y despues se guarda y se mueve a cada jugador
        int h = 0;
        try {
            for (J j : lista) { j.hueco = h++; j.nivel = Nivel.construir(w, j.hueco); }
        } catch (Throwable t) {
            EmiHuida.LOG.error("No se pudo construir el nivel", t);
            for (J j : lista) j.nivel = null;
            fase = Fase.LIBRE; jug.clear();
            return "No se pudo construir el nivel: " + t;
        }
        for (J j : new ArrayList<>(lista)) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(j.id);
            try { Saved.save(server, p); } catch (RuntimeException e) { jug.remove(j.id); p.sendMessage(Msg.err("No se pudo guardar tu inventario: no entras."), false); continue; }
            j.tablas.clear(); for (Nivel.Bloque b : j.nivel.puente()) j.tablas.add(b.pos());
            j.colapsos.clear(); j.sigObst = 0; j.sigPuente = 0; j.idx = Nivel.idxDe(Nivel.S_SALIDA);
            j.s = Nivel.S_SALIDA; j.sPerseguidor = Nivel.S_PERSEGUIDOR; j.vPerseguidor = VEL_BASE;
            j.est = Est.ESPERA; j.errores = 0; j.ultimoError = 0; j.desconectado = false; j.enContacto = false;
            colocarEnSalida(p, w, j);
            prepararJugador(p);
            congelar(p, true);
            j.perseguidor = crearPerseguidor(w, j);
            ServerPlayNetworking.send(p, new EstadoPayload(true, j.perseguidor == null ? -1 : j.perseguidor.getId(), Nivel.rumbo(j.s), 0));
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
        if (resta == 46) for (J j : jug.values()) {                    // el Tyrantrum ruge (animacion y grito de Cobblemon) antes de salir
            ServerPlayerEntity p = jugador(j);
            if (j.perseguidor != null) { try { j.perseguidor.cry(); } catch (Throwable t) { EmiHuida.LOG.warn("cry: {}", t.toString()); } }
            if (p != null) Msg.sound(p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.4f, 0.45f);
        }
        if (resta <= 0) {
            fase = Fase.CORRIENDO;
            EmiHuida.LOG.info("Huida: ¡YA! ({} jugador(es))", jug.size());
            for (J j : jug.values()) {
                ServerPlayerEntity p = jugador(j);
                if (p == null) continue;
                congelar(p, false);
                if (j.perseguidor != null) j.perseguidor.setSprinting(true);      // pose de correr de Cobblemon («ground_run»)
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
            else if (now % 10 == 0) ServerPlayNetworking.send(p, new EstadoPayload(true, j.perseguidor == null ? -1 : j.perseguidor.getId(), Nivel.rumbo(j.s), j.errores));   // la camara sigue mientras ves el final
        }
        comprobarFin();
    }

    private static void tickJugador(ServerWorld w, J j, ServerPlayerEntity p, long now) {
        if (now % 10 == 0) { p.getHungerManager().setFoodLevel(20); p.getHungerManager().setSaturationLevel(5f); }      // hambre llena: asi se puede correr

        // donde va el jugador en el trazado
        Nivel.Prog pr = Nivel.proyectar(j.hueco, p.getX(), p.getZ(), j.idx);
        j.idx = pr.idx(); j.s = pr.s();
        if (DEBUG && now % 2 == 0) EmiHuida.LOG.info("dbg t={} idx={} v={} y={} s={} chaser={} sneak={} pose={}", String.format("%.1f", (now - j.inicioTick) / 20.0), pr.idx(), String.format("%.2f", pr.v()), String.format("%.2f", p.getY()), String.format("%.1f", j.s), String.format("%.1f", j.sPerseguidor), p.isSneaking(), p.getPose());

        // tablas del puente: la que pisas se rompe al poco
        if (p.isOnGround()) {
            double[] dx = {-0.3, 0.3};
            for (double ax : dx) for (double az : dx) {
                BlockPos bajo = BlockPos.ofFloored(p.getX() + ax, Nivel.Y0 - 1, p.getZ() + az);
                if (j.tablas.contains(bajo) && !j.colapsos.containsKey(bajo)) j.colapsos.put(bajo, now + 9);
            }
        }
        for (Iterator<Map.Entry<BlockPos, Long>> it = j.colapsos.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            if (now >= en.getValue()) { w.breakBlock(en.getKey(), false); j.tablas.remove(en.getKey()); it.remove(); }
        }

        // fuera del camino (no deberia poder): de vuelta al ultimo punto seguro
        if (pr.dist() > Nivel.anchoMax(pr.s()) + 0.5 && p.getY() >= Nivel.Y0 - 1.5) { reponer(w, j, p); }
        // caida a la lava
        else if (p.getY() < Nivel.Y0 - 1.5) {
            error(j, p, now, "¡Caiste!");
            reponer(w, j, p);
        }
        // choque con un obstaculo
        else {
            boolean toca = tocaObstaculo(j, p);
            // solo cuenta error al EMPEZAR a tocar (si sigues pegado al obstaculo no se repite cada segundo)
            if (toca && !j.enContacto && now - j.ultimoError >= 10) {
                error(j, p, now, "¡Tropezaste!");
                double yaw = Math.toRadians(Nivel.rumbo(j.s));
                p.setVelocity(Math.sin(yaw) * 0.4, 0.12, -Math.cos(yaw) * 0.4);      // te empuja hacia atras
                p.velocityModified = true;
            }
            j.enContacto = toca;
        }

        // el perseguidor sigue el trazado y destruye lo que encuentra
        // el Tyrantrum intenta mantenerse a una distancia que se acorta con el tiempo; si te adelantas de mas acelera, si fallas no te espera
        double seg = (now - j.inicioTick) / 20.0;
        double objetivo = DIST_INI + (DIST_FIN - DIST_INI) * Math.min(1.0, seg / SEG_HASTA_FIN);
        double hueco = j.s - j.sPerseguidor;
        j.vPerseguidor = Math.min(8.5, Cfg.velBase() + CATCH_UP * Math.max(0, hueco - objetivo));
        j.sPerseguidor += j.vPerseguidor / 20.0;
        if (j.perseguidor != null && !j.perseguidor.isRemoved()) {
            double[] e = Nivel.ejePos(j.hueco, j.sPerseguidor);
            float yaw = Nivel.rumbo(j.sPerseguidor);
            j.perseguidor.refreshPositionAndAngles(e[0], Nivel.Y0, e[1], yaw, 0f);
            j.perseguidor.setHeadYaw(yaw); j.perseguidor.setBodyYaw(yaw);
            j.perseguidor.setVelocity(Math.sin(Math.toRadians(-yaw)) * j.vPerseguidor / 20.0, 0, Math.cos(Math.toRadians(yaw)) * j.vPerseguidor / 20.0);
        }
        romper(w, j);
        double dist = j.s - j.sPerseguidor;
        if (now % 8 == 0) {
            float vol = (float) Math.max(0.2, Math.min(1.4, 1.5 - dist / 25.0));
            Msg.sound(p, SoundEvents.ENTITY_IRON_GOLEM_STEP, vol, 0.45f);
            if (dist < 14) Msg.sound(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.9f);
        }

        // atrapado / meta
        if (dist <= DIST_CAPTURA) { acabar(j, p, now, false); return; }
        if (j.s >= Nivel.S_META) { acabar(j, p, now, true); return; }

        if (now % 2 == 0) {
            ServerPlayNetworking.send(p, new EstadoPayload(true, j.perseguidor == null ? -1 : j.perseguidor.getId(), Nivel.rumbo(j.s), j.errores));
            if (now % 4 == 0) Msg.actionBar(p, barra(j, dist, now));
        }
    }

    /**
     * El servidor no marca {@code horizontalCollision} en los jugadores (su movimiento lo calcula el cliente), asi que se comprueba a mano:
     * el jugador esta pegado (a menos de 8 cm) a un bloque de obstaculo. Estar ENCIMA de un tronco no cuenta (solo se expande en horizontal).
     */
    private static boolean tocaObstaculo(J j, ServerPlayerEntity p) {
        if (j.nivel == null) return false;
        net.minecraft.util.math.Box caja = p.getBoundingBox().expand(0.08, 0, 0.08);
        List<Nivel.Bloque> lista = j.nivel.obstaculos();
        for (int i = j.sigObst; i < lista.size(); i++) {
            Nivel.Bloque bl = lista.get(i);
            if (bl.s() > j.s + 4) break;
            if (bl.s() < j.s - 4) continue;
            BlockPos b = bl.pos();
            if (caja.intersects(b.getX(), b.getY(), b.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1)) {
                // una losa superior (barra baja) solo ocupa la mitad de arriba del bloque
                if (p.getWorld().getBlockState(b).getBlock() instanceof net.minecraft.block.SlabBlock
                        && !caja.intersects(b.getX(), b.getY() + 0.5, b.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1)) continue;
                var estado = p.getWorld().getBlockState(b);
                if (estado.isAir() || estado.getCollisionShape(p.getWorld(), b).isEmpty()) continue;       // aire o decoracion sin colision (raices colgando)
                return true;
            }
        }
        return false;
    }

    private static void error(J j, ServerPlayerEntity p, long now, String msg) {
        j.errores++; j.ultimoError = now;
        EmiHuida.LOG.info("Huida: {} error {} ({}) en s={}", j.nombre, j.errores, msg, (int) j.s);
        Msg.title(p, Text.literal(msg).formatted(Formatting.RED, Formatting.BOLD), Text.literal("Errores: " + j.errores).formatted(Formatting.GRAY), 0, 15, 5);
        Msg.sound(p, SoundEvents.ENTITY_PLAYER_HURT, 0.8f, 0.8f);
    }

    /** Tras una caida: de vuelta al ultimo punto seguro (antes del hueco o del puente en el que caiste). */
    private static void reponer(ServerWorld w, J j, ServerPlayerEntity p) {
        Nivel.Check c = null;
        for (Nivel.Check k : j.nivel.checks()) if (k.s() <= j.s) c = k;
        if (c == null) { double[] e = Nivel.ejePos(j.hueco, Nivel.S_SALIDA); p.teleport(w, e[0], Nivel.Y0, e[1], (float) e[2], 0f); }
        else p.teleport(w, Nivel.ox(j.hueco) + c.x(), Nivel.Y0, c.z(), c.yaw(), 0f);
        p.setVelocity(0, 0, 0); p.velocityModified = true;
        p.fallDistance = 0;
        j.enContacto = false;
        j.idx = Nivel.idxDe(c == null ? Nivel.S_SALIDA : c.s());
        // tras una caida el Tyrantrum no esta encima: se queda al menos a 9 bloques del punto de regreso
        j.sPerseguidor = Math.min(j.sPerseguidor, (c == null ? Nivel.S_SALIDA : c.s()) - 9);
    }

    /** El Tyrantrum destruye los obstaculos y las tablas que alcanza su boca. */
    private static void romper(ServerWorld w, J j) {
        double frente = j.sPerseguidor + FRENTE_PERSEGUIDOR;
        List<Nivel.Bloque> ob = j.nivel.obstaculos(), pu = j.nivel.puente();
        while (j.sigObst < ob.size() && ob.get(j.sigObst).s() <= frente) { w.breakBlock(ob.get(j.sigObst).pos(), false); j.sigObst++; }
        while (j.sigPuente < pu.size() && pu.get(j.sigPuente).s() <= frente) { w.breakBlock(pu.get(j.sigPuente).pos(), false); j.tablas.remove(pu.get(j.sigPuente).pos()); j.sigPuente++; }
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
        ServerPlayNetworking.send(p, new EstadoPayload(false, -1, 0f, 0));
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
        double[] e = Nivel.ejePos(j.hueco, Nivel.S_SALIDA);
        p.teleport(w, e[0], Nivel.Y0, e[1], (float) e[2], 0f);
        p.setVelocity(0, 0, 0); p.velocityModified = true;
    }

    private static final Identifier ID_VEL = Identifier.of("emi_huida", "velocidad");
    private static final Identifier ID_CONG = Identifier.of("emi_huida", "congelado");
    private static final Identifier ID_SALTO = Identifier.of("emi_huida", "sin_salto");
    private static final Identifier ID_AGACHADO = Identifier.of("emi_huida", "agachado");
    private static final Identifier ID_SALTO_EXTRA = Identifier.of("emi_huida", "salto_extra");
    /** Salto mas potente que el de vanilla (0,42 -> 0,60: ~2,4 bloques de alto y ~4 de largo corriendo). Con el salto normal el margen para pasar un tronco de 1 bloque era de ~0,06 s (injugable); asi son ~0,2 s. */
    static final double SALTO_EXTRA = 0.18;

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
        p.getHungerManager().setFoodLevel(20); p.getHungerManager().setSaturationLevel(5f);
        atributo(p, EntityAttributes.PLAYER_SNEAKING_SPEED, ID_AGACHADO, 0.7, EntityAttributeModifier.Operation.ADD_VALUE, true);   // 0,3 + 0,7 = 1,0: agachado se camina a 4,3 bloques/s
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
            if (h > 0.01f) e.getPokemon().setScaleModifier(Math.max(0.3f, Math.min(8f, ALTURA_PERSEGUIDOR / h)));
            e.getPokemon().setNickname(Text.literal("Tyrantrum furioso").formatted(Formatting.RED));
            e.setAiDisabled(true);
            e.setInvulnerable(true);
            e.setNoGravity(true);
            e.setSilent(true);
            e.setPersistent();
            double[] ep = Nivel.ejePos(j.hueco, Nivel.S_PERSEGUIDOR);
            e.refreshPositionAndAngles(ep[0], Nivel.Y0, ep[1], (float) ep[2], 0f);
            w.spawnEntity(e);
            return e;
        } catch (Throwable t) {
            EmiHuida.LOG.error("No se pudo crear al Tyrantrum", t);
            return null;
        }
    }
}
