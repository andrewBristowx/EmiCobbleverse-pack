package emi.cafe;

import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Musica y efectos propios del cafe (los .ogg los generan {@code tools/emi-cafe/audio/musica.py} y {@code sonidos.py}). Como en los otros minijuegos, el SERVIDOR manda cada cancion a cada
 * jugador y la relanza cuando acaba su bucle (el cliente no sabe repetir un sonido que viene del servidor), con el reloj de la pared. Las duraciones de {@link #TICKS} DEBEN coincidir
 * con {@code audio/DURACIONES.txt}. «cerrado» suena con el cafe cerrado o en obras y «abierto» con clientes. La musica de Minecraft esta quitada en el cliente ({@code MusicTrackerMixin}).
 */
final class Sonidos {
    private Sonidos() {}

    static final String NS = "emi_cafe";
    static final Map<String, Integer> TICKS = Map.of("cerrado", 800, "abierto", 640);
    private static final float VOLUMEN = leerVolumen();

    private static float leerVolumen() {
        try { return Math.max(0f, Math.min(1f, Float.parseFloat(System.getProperty("emi.cafe.volumen_musica", "0.4")))); }
        catch (NumberFormatException e) { return 0.4f; }
    }

    static SoundEvent ev(String id) { return SoundEvent.of(Identifier.of(NS, id)); }

    /** Un efecto que suena en un sitio del mundo (lo oyen los que estan cerca; los .ogg de estos son mono para que se atenuen con la distancia). */
    static void mundo(ServerWorld w, double x, double y, double z, String nombre, float vol, float tono) {
        w.playSound(null, x, y, z, ev("sfx." + nombre), SoundCategory.BLOCKS, vol, tono);
    }

    /** Un efecto que solo oye ese jugador. */
    static void jugador(ServerPlayerEntity p, String nombre, float vol, float tono) {
        if (p != null) p.playSoundToPlayer(ev("sfx." + nombre), SoundCategory.MASTER, vol, tono);
    }

    private static void parar(ServerPlayerEntity p, String pista) {
        if (p != null && pista != null) p.networkHandler.sendPacket(new StopSoundS2CPacket(Identifier.of(NS, "musica." + pista), SoundCategory.MUSIC));
    }

    private static final class Est {
        String pista;
        long inicioMs;
    }

    private static final Map<UUID, Est> ESTADO = new HashMap<>();

    /** La cancion que le toca a ese jugador (null = ninguna: no esta en un cafe). */
    static String deseada(ServerPlayerEntity p) {
        Cafe c = Cafes.de(p.getUuid());
        if (c == null) return null;
        return c.estado == Cafe.Estado.ABIERTO ? "abierto" : "cerrado";
    }

    static void tick(MinecraftServer s) {
        long ahora = System.currentTimeMillis();
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            String d = deseada(p);
            Est e = ESTADO.get(p.getUuid());
            if (d == null) {
                if (e != null) { parar(p, e.pista); ESTADO.remove(p.getUuid()); }
                continue;
            }
            if (e == null) { e = new Est(); ESTADO.put(p.getUuid(), e); }
            if (e.pista != null && !e.pista.equals(d)) { parar(p, e.pista); e.pista = null; }
            if (e.pista == null) {
                e.pista = d; e.inicioMs = ahora;
                p.playSoundToPlayer(ev("musica." + d), SoundCategory.MUSIC, VOLUMEN, 1f);
                continue;
            }
            long dur = TICKS.getOrDefault(d, 600) * 50L;
            if (ahora >= e.inicioMs + dur - 25) {
                p.playSoundToPlayer(ev("musica." + d), SoundCategory.MUSIC, VOLUMEN, 1f);
                e.inicioMs += dur;
                if (ahora - e.inicioMs > dur) e.inicioMs = ahora;
            }
        }
        for (Iterator<Map.Entry<UUID, Est>> it = ESTADO.entrySet().iterator(); it.hasNext(); ) if (s.getPlayerManager().getPlayer(it.next().getKey()) == null) it.remove();
    }
}
