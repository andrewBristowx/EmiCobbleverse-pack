package emi.fall;

import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Musica y efectos propios del torneo (los .ogg los generan {@code tools/emi-fall/audio/musica.py} y {@code sonidos.py}). Igual que en Survivors/Carreras/Huida, el SERVIDOR manda cada
 * sonido a cada jugador con {@code playSoundToPlayer} y relanza la cancion cuando acaba su bucle (el cliente no sabe repetir un sonido que viene del servidor), con el reloj de la pared.
 * Las duraciones de {@link #TICKS} DEBEN coincidir con {@code audio/DURACIONES.txt}. La cancion que toca la decide {@link #deseada}: en la sala y entre rondas «espera», y durante cada
 * juego la que diga el minijuego; en la cuenta atras no suena ninguna (para oir los pitidos). La musica de Minecraft esta quitada en el cliente ({@code MusicTrackerMixin}).
 */
final class Sonido {
    private Sonido() {}

    static final String NS = "emi_fall";
    /** Duracion del bucle de cada pista en ticks (audio/DURACIONES.txt). */
    static final Map<String, Integer> TICKS = Map.of("espera", 768, "carrera", 512, "tension", 640, "fiesta", 600);
    private static final float VOLUMEN = leerVolumen();

    private static float leerVolumen() {
        try { return Math.max(0f, Math.min(1f, Float.parseFloat(System.getProperty("emi.fall.volumen_musica", "0.5")))); }
        catch (NumberFormatException e) { return 0.5f; }
    }

    static SoundEvent ev(String id) { return SoundEvent.of(Identifier.of(NS, id)); }

    /** Un efecto que solo oye ese jugador ({@code nombre} = el de {@code sounds.json} sin «sfx.»). */
    static void sfx(ServerPlayerEntity p, String nombre, float vol, float tono) {
        if (p != null) p.playSoundToPlayer(ev("sfx." + nombre), SoundCategory.MASTER, vol, tono);
    }

    static void pararMusica(ServerPlayerEntity p, String pista) {
        if (p != null && pista != null) p.networkHandler.sendPacket(new StopSoundS2CPacket(Identifier.of(NS, "musica." + pista), SoundCategory.MUSIC));
    }

    private static void musica(ServerPlayerEntity p, String pista) {
        if (p != null) p.playSoundToPlayer(ev("musica." + pista), SoundCategory.MUSIC, VOLUMEN, 1f);
    }

    private static final class Est {
        String pista;
        long inicioMs;
        long noAntesDeMs;
    }

    private static final Map<UUID, Est> ESTADO = new HashMap<>();

    /** La cancion que le toca ahora a ese jugador, o null si no debe sonar ninguna. */
    static String deseada(ServerPlayerEntity p) {
        Torneo q = Torneo.actual;
        if (q != null && q.jugs.containsKey(p.getUuid()) && q.fase != Torneo.Fase.COLA) {
            return switch (q.fase) {
                case CUENTA -> null;
                case JUEGO -> q.mini != null ? q.mini.musica() : "espera";
                default -> "espera";
            };
        }
        return Sala.enSala(p) ? "espera" : null;
    }

    static void tick(MinecraftServer s) {
        long ahora = System.currentTimeMillis();
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            String d = deseada(p);
            Est e = ESTADO.get(p.getUuid());
            if (d == null) {
                if (e != null) {
                    if (e.pista != null) { pararMusica(p, e.pista); e.pista = null; }
                    if (ahora >= e.noAntesDeMs) ESTADO.remove(p.getUuid());
                }
                continue;
            }
            if (e == null) { e = new Est(); ESTADO.put(p.getUuid(), e); }
            if (e.pista != null && !e.pista.equals(d)) { pararMusica(p, e.pista); e.pista = null; }
            if (e.pista == null) {
                if (ahora < e.noAntesDeMs) continue;
                e.pista = d; e.inicioMs = ahora;
                musica(p, d);
                continue;
            }
            long dur = TICKS.getOrDefault(d, 600) * 50L;
            if (ahora >= e.inicioMs + dur - 25) {
                musica(p, d);
                e.inicioMs += dur;
                if (ahora - e.inicioMs > dur) e.inicioMs = ahora;
            }
        }
        for (Iterator<Map.Entry<UUID, Est>> it = ESTADO.entrySet().iterator(); it.hasNext(); ) if (s.getPlayerManager().getPlayer(it.next().getKey()) == null) it.remove();
    }

    /** Corta la musica de ese jugador y no vuelve a empezar ninguna hasta dentro de {@code ms} milisegundos (para que se oiga un efecto largo). */
    static void silencio(ServerPlayerEntity p, long ms) {
        if (p == null) return;
        Est e = ESTADO.computeIfAbsent(p.getUuid(), k -> new Est());
        if (e.pista != null) { pararMusica(p, e.pista); e.pista = null; }
        e.noAntesDeMs = System.currentTimeMillis() + ms;
    }
}
