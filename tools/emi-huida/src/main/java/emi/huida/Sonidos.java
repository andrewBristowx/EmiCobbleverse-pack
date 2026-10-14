package emi.huida;

import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.Collection;

/**
 * Sonidos y musica de la huida (los .ogg estan en assets/emi_huida/sounds y los genera tools/emi-huida/audio/generar_sonidos.py).
 * Las duraciones de la musica DEBEN coincidir con las de los archivos: el servidor relanza la pista cuando acaba (el cliente no sabe repetir
 * un sonido que viene del servidor).
 */
final class Sonidos {
    private Sonidos() {}

    static final String NS = "emi_huida";
    static SoundEvent ev(String id) { return SoundEvent.of(Identifier.of(NS, id)); }

    static final SoundEvent ESTRELLA = ev("sfx.estrella"), ESTRELLA_GRANDE = ev("sfx.estrella_grande"), BIEN = ev("sfx.bien"), FALLO = ev("sfx.fallo"),
            CAIDA = ev("sfx.caida"), PISADA = ev("sfx.pisada"), VICTORIA = ev("sfx.victoria"), DERROTA = ev("sfx.derrota"), LISTO = ev("sfx.listo");

    static final String PERSECUCION = "musica.persecucion", ESPERA = "musica.espera";
    /** Duracion de cada pista en ticks. */
    static int duracion(String id) { return PERSECUCION.equals(id) ? 1280 : 640; }

    static void sfx(ServerPlayerEntity p, SoundEvent e, float vol, float pitch) {
        if (p != null) p.playSoundToPlayer(e, SoundCategory.MASTER, vol, pitch);
    }

    static void musica(Collection<ServerPlayerEntity> ps, String id, float vol) {
        SoundEvent e = ev(id);
        for (ServerPlayerEntity p : ps) p.playSoundToPlayer(e, SoundCategory.MUSIC, vol, 1f);
    }

    static void parar(ServerPlayerEntity p, String id) {
        if (p != null && id != null) p.networkHandler.sendPacket(new StopSoundS2CPacket(Identifier.of(NS, id), SoundCategory.MUSIC));
    }
}
