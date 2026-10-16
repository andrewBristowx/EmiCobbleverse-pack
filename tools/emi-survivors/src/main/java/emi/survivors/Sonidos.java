package emi.survivors;

import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Los efectos de sonido del minijuego (los .ogg estan en {@code assets/emi_survivors/sounds}, los registra {@code sounds.json} y los genera
 * {@code tools/emi-survivors/audio/sonidos.py}; la musica, {@code musica.py}, la manda {@link Musica}). Son propios: ya no se usan los sonidos de Minecraft para esto.
 * Los sonidos van por su nombre (no hace falta registrarlos en el servidor): el cliente los busca en su {@code sounds.json}.
 */
final class Sonidos {
    private Sonidos() {}

    static final String NS = "emi_survivors";

    static SoundEvent ev(String id) { return SoundEvent.of(Identifier.of(NS, id)); }

    // lo que oye solo el jugador (suena en su sitio, sin bajar con la distancia)
    static final SoundEvent GEMA = ev("sfx.gema"), NIVEL = ev("sfx.nivel"), CARTA = ev("sfx.carta"), EVOLUCION = ev("sfx.evolucion"), EMPIEZA = ev("sfx.empieza"), GOLPE = ev("sfx.golpe"),
            CAER = ev("sfx.caer"), REVIVIR = ev("sfx.revivir"), DERROTA = ev("sfx.derrota"), JEFE_DERROTADO = ev("sfx.jefe_derrotado"), BAYA = ev("sfx.baya"), IMAN = ev("sfx.iman"),
            BOMBA = ev("sfx.bomba"), CARAMELO = ev("sfx.caramelo"), FOCUS = ev("sfx.focus"), MENU_COMPRAR = ev("sfx.menu_comprar");

    // lo que suena en un sitio de la arena (baja con la distancia)
    static final SoundEvent JEFE_APARECE = ev("sfx.jefe_aparece"), JEFE_RUGIDO = ev("sfx.jefe_rugido"), JEFE_IMPACTO = ev("sfx.jefe_impacto"), JEFE_CARGA = ev("sfx.jefe_carga"),
            JEFE_ZARPAZO = ev("sfx.jefe_zarpazo"), JEFE_AVISO = ev("sfx.jefe_aviso"), JEFE_RAYO = ev("sfx.jefe_rayo"), JEFE_INVOCA = ev("sfx.jefe_invoca"), MAREA_CUERNO = ev("sfx.marea_cuerno"),
            MAREA_ESTAMPIDA = ev("sfx.marea_estampida"), TRUENO = ev("sfx.trueno"), BRILLO = ev("sfx.brillo"), TEMBLOR = ev("sfx.temblor"), EXPLOSION_SUAVE = ev("sfx.explosion_suave");

    /** Un efecto que solo oye ese jugador. */
    static void sfx(ServerPlayerEntity p, SoundEvent e, float vol, float tono) {
        if (p != null) p.playSoundToPlayer(e, SoundCategory.MASTER, vol, tono);
    }

    /** Un efecto en un punto de la arena: lo oye quien este cerca (mas bajo cuanto mas lejos). */
    static void en(ServerWorld w, double x, double y, double z, SoundEvent e, SoundCategory categoria, float vol, float tono) {
        w.playSound(null, x, y, z, e, categoria, vol, tono);
    }

    /** Para una pista de musica: manda parar esa musica a ese jugador (el cliente corta todas las que se llamen asi). */
    static void pararMusica(ServerPlayerEntity p, String pista) {
        if (p != null && pista != null) p.networkHandler.sendPacket(new StopSoundS2CPacket(Identifier.of(NS, "musica." + pista), SoundCategory.MUSIC));
    }

    static void musica(ServerPlayerEntity p, String pista, float vol) {
        if (p != null) p.playSoundToPlayer(ev("musica." + pista), SoundCategory.MUSIC, vol, 1f);
    }

    /** El tono (1 = el original) del sonido de la gema numero {@code cadena} seguida: sube por una escala mayor, como al coger monedas seguidas. */
    static float tonoGema(int cadena) {
        final int[] escala = {0, 2, 4, 5, 7, 9, 11, 12, 14, 15};
        int s = escala[Math.max(0, Math.min(cadena, escala.length - 1))];
        return (float) (0.8 * Math.pow(2.0, s / 12.0));
    }
}
