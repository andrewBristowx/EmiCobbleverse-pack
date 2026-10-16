package emi.survivors;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.sound.SoundEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La musica del minijuego (los .ogg los genera {@code tools/emi-survivors/audio/musica.py}): una cancion para la sala, una para cada bioma y una para los jefes. La manda el
 * SERVIDOR a cada jugador con {@code playSoundToPlayer} (como en las carreras y la huida) y la vuelve a lanzar cuando acaba, porque el cliente no sabe repetir un sonido que viene
 * del servidor. Por eso las duraciones de {@link #TICKS} DEBEN coincidir con las de {@code audio/DURACIONES.txt}: cada archivo dura el bucle mas una cola (reverb) que se solapa con el
 * principio de la vuelta siguiente, asi el corte no se nota. El reloj es el de la pared (no los ticks del servidor), para que un servidor con lag no deje huecos de silencio.
 * <p>La cancion de cada jugador la decide {@link #deseada}: la sala en la sala, la del bioma en su partida y la de jefe mientras hay un jefe vivo. Cuando cambia, se para la anterior
 * y se empieza la nueva (sin fundido: el servidor no puede bajar el volumen de un sonido ya lanzado). {@link #silencio} corta la musica unos segundos (para que se oigan las
 * fanfarrias del principio de la partida, del jefe derrotado o del final). La musica de Minecraft ya esta quitada en el cliente ({@code MusicTrackerMixin}).</p>
 */
final class Musica {
    private Musica() {}

    /** Duracion del bucle de cada pista en ticks (audio/DURACIONES.txt). */
    static final Map<String, Integer> TICKS = Map.of("sala", 1600, "pradera", 1200, "bosque", 1280, "cosmos", 1536, "jefe", 1024);

    private static final class Est {
        String pista;                 // la que suena ahora (null = ninguna)
        long inicioMs;                // cuando empezo la vuelta actual (reloj de pared)
        long noAntesDeMs;             // no se empieza ninguna hasta esta hora (silencio)
    }

    private static final Map<UUID, Est> ESTADO = new HashMap<>();

    /** Un efecto que tiene que sonar mas tarde (p. ej. la melodia triste al perder: se manda cuando el jugador ya esta en la sala, porque el cliente corta los sonidos al cambiar de dimension). */
    private record Pendiente(UUID id, SoundEvent sonido, long hora, float vol) {}
    private static final List<Pendiente> PENDIENTES = new ArrayList<>();

    static void sonarDespues(ServerPlayerEntity p, SoundEvent s, long ms, float vol) {
        if (p != null) PENDIENTES.add(new Pendiente(p.getUuid(), s, System.currentTimeMillis() + ms, vol));
    }
    private static final float VOLUMEN = leerVolumen();

    private static float leerVolumen() {
        try { return Math.max(0f, Math.min(1f, Float.parseFloat(System.getProperty("emi.survivors.volumen_musica", "0.55")))); }
        catch (NumberFormatException e) { return 0.55f; }
    }

    static long msDe(String pista) { return TICKS.getOrDefault(pista, 1200) * 50L; }

    /** La cancion que le toca ahora a ese jugador, o null si no debe sonar ninguna (no esta en el mundo del minijuego). */
    static String deseada(ServerPlayerEntity p) {
        if (!Mundo.dentro(p)) return null;
        if (Mundo.enSala(p)) return "sala";
        Run r = Partida.de(p.getUuid());
        if (r == null) return null;
        Equipo eq = r.eq;
        if (eq.jefe != null && eq.jefe.vivo()) return "jefe";
        return eq.bioma.musica();
    }

    static void tick(MinecraftServer s) {
        long ahora = System.currentTimeMillis();
        for (Iterator<Pendiente> it = PENDIENTES.iterator(); it.hasNext(); ) {
            Pendiente q = it.next();
            if (ahora < q.hora()) continue;
            it.remove();
            Sonidos.sfx(s.getPlayerManager().getPlayer(q.id()), q.sonido(), q.vol(), 1.0f);
        }
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            String d = deseada(p);
            Est e = ESTADO.get(p.getUuid());
            if (d == null) {
                if (e != null) {
                    if (e.pista != null) { Sonidos.pararMusica(p, e.pista); e.pista = null; }
                    if (ahora >= e.noAntesDeMs) ESTADO.remove(p.getUuid());
                }
                continue;
            }
            if (e == null) { e = new Est(); ESTADO.put(p.getUuid(), e); }
            if (e.pista != null && !e.pista.equals(d)) { Sonidos.pararMusica(p, e.pista); e.pista = null; }
            if (e.pista == null) {
                if (ahora < e.noAntesDeMs) continue;
                e.pista = d; e.inicioMs = ahora;
                Sonidos.musica(p, d, VOLUMEN);
                continue;
            }
            long dur = msDe(d);
            if (ahora >= e.inicioMs + dur - 25) {                    // medio tick antes: asi el error medio del relanzamiento es cero
                Sonidos.musica(p, d, VOLUMEN);
                e.inicioMs += dur;
                if (ahora - e.inicioMs > dur) e.inicioMs = ahora;      // si el servidor se quedo parado mucho rato, se vuelve a sincronizar
            }
        }
        // los que se han ido del servidor
        for (Iterator<Map.Entry<UUID, Est>> it = ESTADO.entrySet().iterator(); it.hasNext(); ) {
            if (s.getPlayerManager().getPlayer(it.next().getKey()) == null) it.remove();
        }
    }

    /** Corta la musica de ese jugador y no vuelve a empezar ninguna hasta dentro de {@code ms} milisegundos (para que se oiga una fanfarria). */
    static void silencio(ServerPlayerEntity p, long ms) {
        if (p == null) return;
        Est e = ESTADO.computeIfAbsent(p.getUuid(), k -> new Est());
        if (e.pista != null) { Sonidos.pararMusica(p, e.pista); e.pista = null; }
        e.noAntesDeMs = System.currentTimeMillis() + ms;
    }

    static void olvidar(UUID id) { ESTADO.remove(id); }

    static void limpiar() { ESTADO.clear(); PENDIENTES.clear(); }
}
