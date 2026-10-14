package emi.huida;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import emi.huida.net.EstadoPayload;
import emi.huida.net.EstrellasPayload;
import emi.huida.net.HudPayload;
import emi.huida.net.MapaPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.particle.ParticleTypes;
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
import java.util.BitSet;
import java.util.Comparator;
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

    public enum Fase { LIBRE, COLA, LISTOS, CUENTA, CORRIENDO }
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
        boolean listo;                               // ha confirmado «listo» (fase LISTOS)
        int puntos, nEstrellas, sigEst, combo, racha;       // michiestrellas: puntos y cuantas, siguiente por mirar, combo de recogida seguida y racha de obstaculos limpios
        long ultimaEst;
        final BitSet cogidas = new BitSet();
        double[][] estPos = new double[0][];         // posicion en el mundo de cada estrella de su copia
        final BitSet malezaRota = new BitSet(); int sigMaleza;      // maleza de los desvios que el Tyrantrum ya ha roto / primera que aun puede quedar
        int sigMod, erroresAlEntrar; boolean dentro;  // obstaculo que viene, y errores que llevaba al entrar en su zona (para saber si lo paso limpio)
        long proxAmbiente;
        J(ServerPlayerEntity p) { id = p.getUuid(); nombre = p.getGameProfile().getName(); }
    }

    private static MinecraftServer server;
    private static Fase fase = Fase.LIBRE;
    static final Map<UUID, J> jug = new LinkedHashMap<>();
    private static long cuentaFin, ultimoAviso, ultimoRecordatorio;
    private static String musicaActual; private static long musicaProx;
    private static final java.util.Random RND = new java.util.Random();
    private static final Nivel.Mod[] MODS_ORD = java.util.Arrays.stream(Nivel.MODS).sorted(Comparator.comparingDouble(Nivel.Mod::s0)).toArray(Nivel.Mod[]::new);
    /** Resultado de cada jugador que ha terminado en esta partida (para la clasificacion final). */
    record Resultado(UUID id, String nombre, int puntos, int estrellas, int errores, double tiempo, boolean escapo) {}
    private static final List<Resultado> resultados = new ArrayList<>();
    private static final List<UUID> pendientes = new ArrayList<>();      // jugadores a los que hay que devolver sus cosas al entrar
    private static final List<Long> pendientesTick = new ArrayList<>();

    public static Fase fase() { return fase; }
    static ServerWorld mundo() { return server.getWorld(MUNDO); }

    static void init(MinecraftServer s) {
        server = s;
        fase = Fase.LIBRE;
        jug.clear();
        if (mundo() == null) EmiHuida.LOG.warn("No existe la dimension emi_huida:selva: /huida empezar no funcionara.");
        try {
            int pts = 0; for (Nivel.Estrella e : Nivel.ESTRELLAS) pts += e.valor();
            EmiHuida.LOG.info("Huida: nivel de {} bloques, {} michiestrellas ({} puntos), {} desvios de puente", (int) Nivel.S_TOTAL, Nivel.ESTRELLAS.length, pts, Nivel.DESVIOS.size());
        } catch (Throwable t) { EmiHuida.LOG.error("El nivel de la huida no se pudo preparar", t); }
    }

    static void shutdown() {
        if (server == null) return;
        for (J j : new ArrayList<>(jug.values())) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(j.id);
            if (p != null && Saved.exists(server, j.id)) Saved.restore(server, p);
        }
        jug.clear(); fase = Fase.LIBRE;
        try { Pausas.reanudar(); } catch (Throwable t) { }
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
        comprobarListos();
        return null;
    }

    /** @param ya true = sin pedir confirmacion de «listo»: la cuenta atras empieza en cuanto todos estan en su sitio. */
    public static String empezar(boolean ya) {
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
        resultados.clear();
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
            j.colapsos.clear(); j.sigObst = 0; j.sigPuente = 0; j.malezaRota.clear(); j.sigMaleza = 0; j.idx = Nivel.idxDe(Nivel.S_SALIDA);
            j.s = Nivel.S_SALIDA; j.sPerseguidor = Nivel.S_PERSEGUIDOR; j.vPerseguidor = VEL_BASE;
            j.est = Est.ESPERA; j.errores = 0; j.ultimoError = 0; j.desconectado = false; j.enContacto = false; j.listo = false;
            j.puntos = 0; j.nEstrellas = 0; j.sigEst = 0; j.combo = 0; j.racha = 0; j.ultimaEst = 0; j.cogidas.clear(); j.sigMod = 0; j.dentro = false; j.erroresAlEntrar = 0;
            j.estPos = new double[Nivel.ESTRELLAS.length][];
            float[] datos = new float[Nivel.ESTRELLAS.length * 4];
            for (int i = 0; i < Nivel.ESTRELLAS.length; i++) {
                double[] q = Nivel.estrellaPos(j.hueco, Nivel.ESTRELLAS[i]);
                j.estPos[i] = q;
                datos[i * 4] = (float) q[0]; datos[i * 4 + 1] = (float) q[1]; datos[i * 4 + 2] = (float) q[2]; datos[i * 4 + 3] = Nivel.ESTRELLAS[i].valor();
            }
            ServerPlayNetworking.send(p, EstrellasPayload.lista(datos));
            ServerPlayNetworking.send(p, new MapaPayload((float) Nivel.PASO_MAPA, (float) Nivel.S_TOTAL, (float) Nivel.S_META, Nivel.puntosTotal(), Nivel.mapa(j.hueco)));
            colocarEnSalida(p, w, j);
            prepararJugador(p);
            congelar(p, true);
            j.perseguidor = crearPerseguidor(w, j);
            ServerPlayNetworking.send(p, new EstadoPayload(true, j.perseguidor == null ? -1 : j.perseguidor.getId(), Nivel.rumbo(j.s), 0, !ya));
        }
        if (jug.isEmpty()) { fase = Fase.LIBRE; return "No se pudo preparar a nadie."; }
        Pausas.pausar();      // sin limpieza de objetos ni bosses de Emipokemon mientras dura la huida
        if (ya) { iniciarCuenta(null); return null; }
        fase = Fase.LISTOS;
        ultimoRecordatorio = server.getTicks();
        musicaActual = null;
        for (J j : jug.values()) {
            ServerPlayerEntity p = jugador(j);
            if (p == null) continue;
            Msg.title(p, Text.literal("¿Listo?").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Pulsa R o escribe /huida listo").formatted(Formatting.YELLOW), 0, 80, 10);
            p.sendMessage(Msg.say(Text.literal("Estás en tu pista. Mira el camino y, cuando estés preparado, confirma: ").formatted(Formatting.WHITE)
                    .append(Msg.button("LISTO", "/huida listo", Formatting.GREEN, "Confirmar que estás listo (también con la tecla R)"))), false);
        }
        return null;
    }

    /** Un jugador confirma que esta listo; cuando lo estan todos empieza la cuenta atras. */
    public static String listo(ServerPlayerEntity p) {
        if (fase != Fase.LISTOS) return jug.containsKey(p.getUuid()) ? "Ahora no hay nada que confirmar." : "No estás en ninguna huida.";
        J j = jug.get(p.getUuid());
        if (j == null) return "No estás en esta huida.";
        if (j.listo) return "Ya has confirmado; espera a los demás.";
        j.listo = true;
        Msg.broadcastTo(online(), Msg.say(j.nombre + " está listo (" + listos() + "/" + jug.size() + ")."));
        Sonidos.sfx(p, Sonidos.LISTO, 1f, 1f);
        comprobarListos();
        return null;
    }

    /** El organizador se salta la confirmacion: la cuenta atras empieza ya aunque falte gente por confirmar. */
    public static String saltar() {
        if (fase != Fase.LISTOS) return "No hay ninguna confirmación en marcha (solo se puede saltar tras /huida empezar).";
        iniciarCuenta("El organizador ha saltado la confirmación.");
        return null;
    }

    private static int listos() { int n = 0; for (J j : jug.values()) if (j.listo) n++; return n; }

    private static void comprobarListos() {
        if (fase != Fase.LISTOS || jug.isEmpty()) return;
        for (J j : jug.values()) if (!j.listo) return;
        iniciarCuenta(null);
    }

    private static void iniciarCuenta(String motivo) {
        fase = Fase.CUENTA;
        cuentaFin = server.getTicks() + CUENTA_SEGUNDOS * 20L;
        for (J j : jug.values()) {
            ServerPlayerEntity p = jugador(j);
            if (p != null) ServerPlayNetworking.send(p, new EstadoPayload(true, j.perseguidor == null ? -1 : j.perseguidor.getId(), Nivel.rumbo(j.s), 0, false));
        }
        Msg.broadcastTo(online(), Msg.say((motivo == null ? "¡Todos listos! " : motivo + " ") + "La huida empieza en " + CUENTA_SEGUNDOS + "..."));
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
            case LISTOS -> "Esperando a que todos confirmen «listo» (" + listos() + "/" + jug.size() + ").";
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
            case LISTOS -> tickListos(s, now);
            case CUENTA -> tickCuenta(s, now);
            case CORRIENDO -> tickCorriendo(s, now);
        }
    }

    /** Espera a que todos confirmen: mantiene la camara, muestra cuantos faltan y recuerda a los que no han confirmado. */
    private static void tickListos(MinecraftServer s, long now) {
        boolean recordar = now - ultimoRecordatorio >= 400;       // cada 20 s
        if (recordar) ultimoRecordatorio = now;
        int n = listos();
        musica(now, Sonidos.ESPERA);
        for (J j : new ArrayList<>(jug.values())) {
            ServerPlayerEntity p = jugador(j);
            if (p == null) continue;
            ambiente(j, p, now);
            if (now % 10 == 0) { ServerPlayNetworking.send(p, new EstadoPayload(true, j.perseguidor == null ? -1 : j.perseguidor.getId(), Nivel.rumbo(j.s), 0, true)); enviarHud(p, j, now); }
            if (now % 20 == 0) Msg.actionBar(p, j.listo
                    ? Text.literal("Listo ✔ — esperando a los demás (" + n + "/" + jug.size() + ")").formatted(Formatting.GREEN, Formatting.BOLD)
                    : Text.literal("Pulsa R o escribe /huida listo  (" + n + "/" + jug.size() + " listos)").formatted(Formatting.YELLOW, Formatting.BOLD));
            if (recordar && !j.listo) p.sendMessage(Msg.say(Text.literal("Falta tu confirmación: ").formatted(Formatting.WHITE)
                    .append(Msg.button("LISTO", "/huida listo", Formatting.GREEN, "Confirmar que estás listo (también con la tecla R)"))), false);
        }
    }

    // ---------------------------------------------------------------- interfaz (cliente)

    /** {puesto, jugadores, puntos del primero}: se ordena solo por puntos (la clasificacion final desempata por errores y tiempo). */
    private static int[] clasificacionDe(J yo) {
        Map<UUID, Integer> pts = new LinkedHashMap<>();
        for (Resultado r : resultados) pts.put(r.id(), r.puntos());
        for (J k : jug.values()) pts.put(k.id, k.puntos);
        int mejores = 0, lider = yo.puntos;
        for (var en : pts.entrySet()) { lider = Math.max(lider, en.getValue()); if (!en.getKey().equals(yo.id) && en.getValue() > yo.puntos) mejores++; }
        return new int[]{mejores + 1, Math.max(1, pts.size()), lider};
    }

    private static void enviarHud(ServerPlayerEntity p, J j, long now) {
        int[] c = clasificacionDe(j);
        int fin = j.est == Est.ESCAPO ? 1 : j.est == Est.ATRAPADO ? 2 : 0;
        long ticks = j.est == Est.CORRE ? now - j.inicioTick : j.est == Est.ESPERA ? 0 : j.finTick - j.inicioTick;
        ServerPlayNetworking.send(p, new HudPayload((float) j.s, (float) j.sPerseguidor, j.puntos, c[0], c[1], c[2], j.errores, (int) Math.max(0, ticks), fin));
    }

    // ---------------------------------------------------------------- musica y sonidos

    /** Musica de fondo en bucle: {@code id} = pista que debe sonar (null = ninguna). Se relanza cuando acaba. */
    private static void musica(long now, String id) {
        if (id == null) { pararMusica(); return; }
        if (!id.equals(musicaActual)) { pararMusica(); musicaActual = id; musicaProx = 0; }
        if (now < musicaProx) return;
        musicaProx = now + Sonidos.duracion(id) - 2;
        List<ServerPlayerEntity> ps = new ArrayList<>();
        for (J j : jug.values()) {
            ServerPlayerEntity p = jugador(j);
            if (p != null && (!Sonidos.PERSECUCION.equals(id) || j.est == Est.CORRE)) ps.add(p);
        }
        Sonidos.musica(ps, id, Cfg.volumenMusica());
    }

    private static void pararMusica() {
        if (musicaActual != null && server != null) for (J j : jug.values()) Sonidos.parar(jugador(j), musicaActual);
        musicaActual = null;
    }

    /** Sonidos de la selva y del Tyrantrum a lo lejos, de vez en cuando (todos de vanilla, bajitos). */
    private static void ambiente(J j, ServerPlayerEntity p, long now) {
        if (now < j.proxAmbiente) return;
        j.proxAmbiente = now + 70 + RND.nextInt(110);
        switch (RND.nextInt(5)) {
            case 0 -> Sonidos.sfx(p, SoundEvents.ENTITY_PARROT_AMBIENT, 0.5f, 0.8f + RND.nextFloat() * 0.5f);
            case 1 -> Sonidos.sfx(p, SoundEvents.ENTITY_PARROT_IMITATE_RAVAGER, 0.35f, 0.7f);
            case 2 -> Sonidos.sfx(p, SoundEvents.ENTITY_BAT_AMBIENT, 0.3f, 0.6f + RND.nextFloat() * 0.3f);
            case 3 -> Sonidos.sfx(p, SoundEvents.ENTITY_RAVAGER_ROAR, 0.18f, 0.4f);
            default -> Sonidos.sfx(p, SoundEvents.ENTITY_FROG_AMBIENT, 0.4f, 0.6f);
        }
    }

    /** Animacion de rugido de Cobblemon. {@code cry()} no hace nada si la entidad esta en silencio, asi que se le quita un instante. */
    private static void rugir(PokemonEntity e) {
        if (e == null) return;
        try { e.setSilent(false); e.cry(); e.setSilent(true); } catch (Throwable t) { EmiHuida.LOG.warn("cry: {}", t.toString()); }
    }

    private static void tickCuenta(MinecraftServer s, long now) {
        long resta = cuentaFin - now;
        ServerWorld w = mundo();
        // el cliente da por terminada la huida si pasan 3 s sin noticias: durante la cuenta atras se le sigue avisando (si no, volvia a la camara normal)
        if (now % 10 == 0) for (J j : jug.values()) {
            ServerPlayerEntity p = jugador(j);
            if (p != null) { ServerPlayNetworking.send(p, new EstadoPayload(true, j.perseguidor == null ? -1 : j.perseguidor.getId(), Nivel.rumbo(j.s), 0, false)); enviarHud(p, j, now); }
        }
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
            rugir(j.perseguidor);
            if (p != null) Msg.sound(p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.4f, 0.45f);
        }
        if (resta <= 0) {
            fase = Fase.CORRIENDO;
            EmiHuida.LOG.info("Huida: ¡YA! ({} jugador(es))", jug.size());
            pararMusica();
            for (J j : jug.values()) {
                ServerPlayerEntity p = jugador(j);
                if (p == null) continue;
                congelar(p, false);
                if (j.perseguidor != null) j.perseguidor.setSprinting(true);      // pose de correr de Cobblemon («ground_run»)
                j.est = Est.CORRE; j.inicioTick = now;
                Msg.title(p, Text.literal("¡CORRE!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Llega a la puerta dorada").formatted(Formatting.WHITE), 0, 30, 10);
                Msg.sound(p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.5f, 0.4f);
            }
            musica(now, Sonidos.PERSECUCION);
        }
    }

    private static void tickCorriendo(MinecraftServer s, long now) {
        ServerWorld w = mundo();
        musica(now, Sonidos.PERSECUCION);
        for (J j : new ArrayList<>(jug.values())) {
            ServerPlayerEntity p = jugador(j);
            if (p == null) continue;
            if (j.est == Est.CORRE) tickJugador(w, j, p, now);
            else if (now >= j.soltarEn) { terminar(j, p, null); }
            else if (now % 10 == 0) { ServerPlayNetworking.send(p, new EstadoPayload(true, j.perseguidor == null ? -1 : j.perseguidor.getId(), Nivel.rumbo(j.s), j.errores, false)); enviarHud(p, j, now); }   // la camara y la interfaz siguen mientras ves el final
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
            error(j, p, now, "¡Caiste!", true);
            reponer(w, j, p);
        }
        // choque con un obstaculo
        else {
            boolean toca = tocaObstaculo(j, p);
            // solo cuenta error al EMPEZAR a tocar (si sigues pegado al obstaculo no se repite cada segundo)
            if (toca && !j.enContacto && now - j.ultimoError >= 10) {
                error(j, p, now, "¡Tropezaste!", false);
                double yaw = Math.toRadians(Nivel.rumbo(j.s));
                p.setVelocity(Math.sin(yaw) * 0.4, 0.12, -Math.cos(yaw) * 0.4);      // te empuja hacia atras
                p.velocityModified = true;
            }
            j.enContacto = toca;
        }

        recogerEstrellas(w, j, p, now);
        valorarObstaculos(j, p);
        ambiente(j, p, now);

        // el perseguidor sigue el trazado y destruye lo que encuentra
        // el Tyrantrum intenta mantenerse a una distancia que se acorta con el tiempo; si te adelantas de mas acelera, si fallas no te espera
        double seg = (now - j.inicioTick) / 20.0;
        double objetivo = DIST_INI + (DIST_FIN - DIST_INI) * Math.min(1.0, seg / SEG_HASTA_FIN);
        double hueco = j.s - j.sPerseguidor;
        j.vPerseguidor = Math.min(8.5, Cfg.velBase() + CATCH_UP * Math.max(0, hueco - objetivo));
        j.sPerseguidor += j.vPerseguidor / 20.0;
        if (j.perseguidor != null && !j.perseguidor.isRemoved()) {
            double[] e = Nivel.perseguidorPos(j.hueco, j.sPerseguidor);       // en los puentes rodea el hueco por la selva
            float yaw = (float) e[3];
            j.perseguidor.refreshPositionAndAngles(e[0], e[1], e[2], yaw, 0f);
            j.perseguidor.setHeadYaw(yaw); j.perseguidor.setBodyYaw(yaw);
            // Cobblemon elige «correr» con la pose de andar (dato sincronizado MOVING, que sale de la distancia caminada, ver LivingEntityMixin) mas el sprint
            ((Corredor) (Object) j.perseguidor).emi_huida$setPaso((float) (0.6 * j.vPerseguidor / 20.0));
            j.perseguidor.setSprinting(true);
            j.perseguidor.setVelocity(Math.sin(Math.toRadians(-yaw)) * j.vPerseguidor / 20.0, 0, Math.cos(Math.toRadians(yaw)) * j.vPerseguidor / 20.0);
            romperMaleza(w, j, e[0], e[2]);
        }
        romper(w, j);
        double dist = j.s - j.sPerseguidor;
        if (now % 8 == 0) {
            float vol = (float) Math.max(0.2, Math.min(1.4, 1.5 - dist / 25.0));
            Sonidos.sfx(p, Sonidos.PISADA, vol, (now / 8) % 2 == 0 ? 0.92f : 1.05f);
            if (dist < 14) Msg.sound(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.9f);
        }

        // atrapado / meta
        if (dist <= DIST_CAPTURA) { acabar(j, p, now, false); return; }
        if (j.s >= Nivel.S_META) { acabar(j, p, now, true); return; }

        if (now % 2 == 0) {
            ServerPlayNetworking.send(p, new EstadoPayload(true, j.perseguidor == null ? -1 : j.perseguidor.getId(), Nivel.rumbo(j.s), j.errores, false));
            enviarHud(p, j, now);
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

    private static void error(J j, ServerPlayerEntity p, long now, String msg, boolean caida) {
        j.errores++; j.ultimoError = now; j.racha = 0;
        EmiHuida.LOG.info("Huida: {} error {} ({}) en s={}", j.nombre, j.errores, msg, (int) j.s);
        Msg.title(p, Text.literal(msg).formatted(Formatting.RED, Formatting.BOLD), Text.literal("Errores: " + j.errores).formatted(Formatting.GRAY), 0, 15, 5);
        Sonidos.sfx(p, caida ? Sonidos.CAIDA : Sonidos.FALLO, 1f, 1f);
    }

    /** Recoge las michiestrellas que toca el jugador (las de s cerca de la suya; las que se pasan sin coger se pierden). */
    private static void recogerEstrellas(ServerWorld w, J j, ServerPlayerEntity p, long now) {
        Nivel.Estrella[] es = Nivel.ESTRELLAS;
        for (int i = j.sigEst; i < es.length; i++) {
            Nivel.Estrella e = es[i];
            if (e.s() > j.s + 3) break;
            if (e.s() < j.s - 3.5) { if (i == j.sigEst) j.sigEst++; continue; }
            if (j.cogidas.get(i)) continue;
            double[] q = j.estPos[i];
            double dx = p.getX() - q[0], dz = p.getZ() - q[2], dy = q[1] - p.getY();
            if (dx * dx + dz * dz > 2.6 || dy < -0.5 || dy > 2.3) continue;
            j.cogidas.set(i);
            j.puntos += e.valor(); j.nEstrellas++;
            j.combo = now - j.ultimaEst <= 30 ? Math.min(j.combo + 1, 10) : 0;
            j.ultimaEst = now;
            ServerPlayNetworking.send(p, EstrellasPayload.cogida(i));
            boolean grande = e.valor() >= 5;
            Sonidos.sfx(p, grande ? Sonidos.ESTRELLA_GRANDE : Sonidos.ESTRELLA, grande ? 1f : 0.8f, 1f + j.combo * 0.055f);
            w.spawnParticles(p, grande ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.END_ROD, false, q[0], q[1], q[2], grande ? 14 : 5, 0.25, 0.25, 0.25, grande ? 0.25 : 0.04);
        }
    }

    /** Premia (con un sonido) pasar un obstaculo sin tocarlo ni caerse; cada obstaculo limpio seguido sube el tono (la racha se pierde al fallar). */
    private static void valorarObstaculos(J j, ServerPlayerEntity p) {
        while (j.sigMod < MODS_ORD.length) {
            Nivel.Mod m = MODS_ORD[j.sigMod];
            if (!j.dentro && j.s >= m.s0() - 4) { j.dentro = true; j.erroresAlEntrar = j.errores; }
            if (j.s <= m.s0() + m.filas() + 0.5) break;
            if (j.errores == j.erroresAlEntrar) {
                j.racha++;
                Sonidos.sfx(p, Sonidos.BIEN, 0.8f, 1f + Math.min(j.racha - 1, 8) * 0.06f);
            }
            j.sigMod++; j.dentro = false;
        }
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
        // se puede volver a intentar: las estrellas no cogidas de ese tramo siguen ahi y se recomponen las tablas del puente que aun no ha roto el Tyrantrum
        double sc = c == null ? Nivel.S_SALIDA : c.s();
        j.sigEst = 0; while (j.sigEst < Nivel.ESTRELLAS.length && Nivel.ESTRELLAS[j.sigEst].s() < sc - 3.5) j.sigEst++;
        j.combo = 0; j.racha = 0;
        List<Nivel.Bloque> pu = j.nivel.puente();
        for (int i = j.sigPuente; i < pu.size(); i++) {
            BlockPos pos = pu.get(i).pos();
            if (w.getBlockState(pos).isAir()) w.setBlockState(pos, net.minecraft.block.Blocks.JUNGLE_PLANKS.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS | net.minecraft.block.Block.FORCE_STATE);
            j.tablas.add(pos);
        }
        j.colapsos.clear();
    }

    /** El Tyrantrum destruye los obstaculos y las tablas que alcanza su boca. */
    private static void romper(ServerWorld w, J j) {
        double frente = j.sPerseguidor + FRENTE_PERSEGUIDOR;
        List<Nivel.Bloque> ob = j.nivel.obstaculos(), pu = j.nivel.puente();
        while (j.sigObst < ob.size() && ob.get(j.sigObst).s() <= frente) { w.breakBlock(ob.get(j.sigObst).pos(), false); j.sigObst++; }
        while (j.sigPuente < pu.size() && pu.get(j.sigPuente).s() <= frente) { w.breakBlock(pu.get(j.sigPuente).pos(), false); j.tablas.remove(pu.get(j.sigPuente).pos()); j.sigPuente++; }
    }

    /** Al salir al sendero o volver al camino el Tyrantrum destroza la maleza que cierra el muro (las hojas que quedan a menos de ~5 bloques de el). */
    private static void romperMaleza(ServerWorld w, J j, double cx, double cz) {
        List<Nivel.Bloque> ma = j.nivel.maleza();
        while (j.sigMaleza < ma.size() && ma.get(j.sigMaleza).s() < j.sPerseguidor - 10) j.sigMaleza++;
        for (int i = j.sigMaleza; i < ma.size(); i++) {
            Nivel.Bloque b = ma.get(i);
            if (b.s() > j.sPerseguidor + 10) break;
            if (j.malezaRota.get(i)) continue;
            double dx = b.pos().getX() + 0.5 - cx, dz = b.pos().getZ() + 0.5 - cz;
            if (dx * dx + dz * dz > 25.0) continue;
            j.malezaRota.set(i);
            w.breakBlock(b.pos(), false);
        }
    }

    private static void acabar(J j, ServerPlayerEntity p, long now, boolean escapo) {
        j.est = escapo ? Est.ESCAPO : Est.ATRAPADO;
        j.finTick = now; j.soltarEn = now + ESPERA_FIN;
        if (j.perseguidor != null) {      // el Tyrantrum se para (deja de correr) y, si te ha atrapado, ruge
            ((Corredor) (Object) j.perseguidor).emi_huida$setPaso(0f);
            j.perseguidor.setSprinting(false);
            if (!escapo) rugir(j.perseguidor);
        }
        double t = (now - j.inicioTick) / 20.0;
        String tiempo = String.format(java.util.Locale.ROOT, "%.1f s", t);
        resultados.add(new Resultado(j.id, j.nombre, j.puntos, j.nEstrellas, j.errores, t, escapo));
        Sonidos.parar(p, Sonidos.PERSECUCION);
        if (escapo) {
            String medalla = j.errores == 0 ? "ORO" : j.errores <= 2 ? "PLATA" : "BRONCE";
            Formatting mc = j.errores == 0 ? Formatting.GOLD : j.errores <= 2 ? Formatting.GRAY : Formatting.RED;
            Msg.title(p, Text.literal("¡ESCAPASTE!").formatted(Formatting.GREEN, Formatting.BOLD), Text.literal("★ " + j.puntos + " · Medalla de " + medalla + " · " + tiempo + " · " + j.errores + " errores").formatted(mc), 0, 100, 20);
            p.sendMessage(Msg.say("¡Escapaste del Tyrantrum en " + tiempo + " con " + j.errores + " error(es)! Medalla de " + medalla + ". Michiestrellas: " + j.nEstrellas + " (" + j.puntos + " puntos)."), false);
            Sonidos.sfx(p, Sonidos.VICTORIA, 1f, 1f);
        } else {
            Msg.title(p, Text.literal("¡TE ATRAPÓ!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("★ " + j.puntos + " · Aguantaste " + tiempo + " · " + j.errores + " errores").formatted(Formatting.GRAY), 0, 100, 20);
            p.sendMessage(Msg.say("El Tyrantrum te atrapó tras " + tiempo + ". Michiestrellas: " + j.nEstrellas + " (" + j.puntos + " puntos)."), false);
            Msg.sound(p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.5f, 0.5f);
            Sonidos.sfx(p, Sonidos.DERROTA, 1f, 1f);
        }
        EmiHuida.LOG.info("Huida: {} {} en {} con {} errores y {} puntos ({} estrellas)", j.nombre, escapo ? "ESCAPO" : "ATRAPADO", tiempo, j.errores, j.puntos, j.nEstrellas);
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
        ServerPlayNetworking.send(p, new EstadoPayload(false, -1, 0f, 0, false));
        ServerPlayNetworking.send(p, EstrellasPayload.borrar());
        Sonidos.parar(p, Sonidos.PERSECUCION); Sonidos.parar(p, Sonidos.ESPERA);
        congelar(p, false);
        if (!Saved.restore(server, p)) quitarEfectos(p);
    }

    private static void comprobarFin() {
        if (fase != Fase.CORRIENDO && fase != Fase.CUENTA && fase != Fase.LISTOS) return;
        if (jug.isEmpty()) { if (fase == Fase.CORRIENDO) anunciarClasificacion(); limpiar(); }
    }

    /** Cuando ha terminado el ultimo jugador: quien consiguio mas puntos (michiestrellas) gana; a igualdad, menos errores y luego menos tiempo. */
    private static void anunciarClasificacion() {
        if (resultados.isEmpty()) return;
        List<Resultado> orden = new ArrayList<>(resultados);
        orden.sort(Comparator.comparingInt((Resultado r) -> -r.puntos()).thenComparingInt(Resultado::errores).thenComparingDouble(Resultado::tiempo));
        List<ServerPlayerEntity> todos = new ArrayList<>();
        for (Resultado r : orden) { ServerPlayerEntity p = server.getPlayerManager().getPlayer(r.id()); if (p != null) todos.add(p); }
        List<Text> lineas = new ArrayList<>();
        lineas.add(Msg.say(Text.literal(orden.size() == 1 ? "Tu puntuación de la huida:" : "¡Clasificación de la huida! (gana quien consigue más michiestrellas)").formatted(Formatting.GOLD, Formatting.BOLD)));
        String[] medallas = {"1º", "2º", "3º"};
        for (int i = 0; i < orden.size(); i++) {
            Resultado r = orden.get(i);
            Formatting c = i == 0 ? Formatting.GOLD : i == 1 ? Formatting.GRAY : i == 2 ? Formatting.RED : Formatting.WHITE;
            lineas.add(Text.literal("  " + (i < 3 ? medallas[i] : (i + 1) + "º") + "  " + r.nombre() + "  —  ★ " + r.puntos() + " puntos (" + r.estrellas() + " estrellas) · " + r.errores() + " errores · "
                    + String.format(java.util.Locale.ROOT, "%.1f s", r.tiempo()) + (r.escapo() ? " · escapó" : " · atrapado")).formatted(c));
        }
        for (ServerPlayerEntity p : todos) for (Text t : lineas) p.sendMessage(t, false);
        if (orden.size() > 1) {
            Resultado g = orden.get(0);
            for (ServerPlayerEntity p : todos) Msg.title(p, Text.literal("¡Gana " + g.nombre() + "!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("★ " + g.puntos() + " michiestrellas").formatted(Formatting.YELLOW), 5, 80, 20);
        }
        EmiHuida.LOG.info("Huida: clasificacion: {}", orden.stream().map(r -> r.nombre() + "=" + r.puntos()).toList());
        resultados.clear();
    }

    private static void limpiar() {
        ServerWorld w = mundo();
        if (w != null) {
            for (int h = 0; h < Nivel.MAX_HUECOS; h++) Nivel.chunks(w, h, false);
            limpiarEntidades(w);
        }
        jug.clear(); fase = Fase.LIBRE;
        try { Pausas.reanudar(); } catch (Throwable t) { EmiHuida.LOG.warn("pausas: {}", t.toString()); }
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
        comprobarListos();
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
