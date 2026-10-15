package emi.huida;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * La horda de michialiens del nivel 3: unos 18 GatitoAlien de distintos aspectos (los accesorios y el dorado de emi-plush) que corren en formacion por
 * el camino, detras del jugador, como un solo perseguidor. La posicion de la horda es la de su primera fila ({@code J.sPerseguidor}); cada michi va unos
 * metros detras de ella ({@code ds}) y algo a un lado ({@code v}). El michi del centro de la primera fila es el «lider» ({@code J.perseguidor}): con el
 * funcionan igual que con el Tyrantrum las partes del juego que solo conocen a un perseguidor (el rugido, la camara...).
 */
final class Horda {
    /** Un michi de la horda: donde va respecto a la primera fila, como se ve y su fase para los saltitos. {@code encima}: va montado en otro (altura extra). */
    static final class Michi {
        PokemonEntity e; final double ds, v, fase; final float escala; final String forma; final double encima;
        Michi(double ds, double v, double fase, float escala, String forma, double encima) { this.ds = ds; this.v = v; this.fase = fase; this.escala = escala; this.forma = forma; this.encima = encima; }
    }

    /** Los aspectos de la horda: accesorio y si es dorado (las 25 formas del GatitoAlien: se reparten 16 de ellas entre los michis). */
    static final String[] FORMAS = {"", "gold=true", "accessory=crown", "accessory=bow", "accessory=halo", "accessory=devil_horns", "accessory=witch_hat", "accessory=santa_hat",
            "accessory=headphones", "accessory=sunglasses", "accessory=pirate", "accessory=flower_crown", "accessory=scarf", "accessory=beach_hat", "accessory=crown gold=true", "accessory=halo gold=true"};

    final List<Michi> michis = new ArrayList<>();

    /** El michi de delante en el centro. */
    Michi lider() { return michis.get(0); }

    /** Las filas de la formacion: {ds, v, escala, encima}. Cinco filas de tres (con el lider delante en el centro) y tres michis montados encima de otros. */
    static final double[][] FORMACION = {
            {0.0, 0.0, 3.2, 0}, {0.6, -2.0, 2.8, 0}, {0.4, 2.0, 2.6, 0},
            {3.0, -1.1, 3.0, 0}, {3.4, 1.2, 2.5, 0}, {3.2, -3.0, 2.3, 0},
            {5.8, -2.1, 2.7, 0}, {6.0, 0.0, 3.3, 0}, {5.6, 2.2, 2.9, 0},
            {8.6, -1.0, 2.4, 0}, {8.2, 1.1, 3.1, 0}, {8.8, -3.0, 2.6, 0},
            {11.2, 0.0, 2.8, 0}, {11.6, 2.2, 2.4, 0}, {11.0, -2.2, 3.0, 0},
            {3.0, -1.1, 1.6, 2.7}, {6.0, 0.0, 1.8, 3.2}, {8.2, 1.1, 1.5, 2.9}};

    static Horda crear(ServerWorld w, Huida.J j, double sFrente) {
        Horda h = new Horda();
        for (int i = 0; i < FORMACION.length; i++) {
            double[] f = FORMACION[i];
            h.michis.add(new Michi(f[0], f[1], i * 1.7, (float) f[2], FORMAS[i % FORMAS.length], f[3]));
        }
        for (Michi m : h.michis) {
            Vec3d pos = posicion(j.hueco, sFrente, m, 0);
            m.e = michiDeTierra(w, m.forma, pos, (float) Nivel.rumbo(sFrente - m.ds), m.escala);
        }
        if (h.michis.get(0).e == null) return null;           // sin emi-plush no hay michis
        return h;
    }

    /** Un GatitoAlien en el suelo (sin IA, ni gravedad, ni sonido, sin empujones), con la forma dada («accessory=halo gold=true»...). */
    static PokemonEntity michiDeTierra(ServerWorld w, String forma, Vec3d pos, float yaw, float escala) {
        try {
            PokemonEntity e;
            try { e = PokemonProperties.Companion.parse("gatitoalien level=30 " + forma).createEntity(w); }
            catch (Throwable t) { e = PokemonProperties.Companion.parse("gatitoalien level=30").createEntity(w); }
            e.getPokemon().setScaleModifier(escala);
            e.setAiDisabled(true); e.setInvulnerable(true); e.setNoGravity(true); e.setSilent(true); e.setPersistent();
            e.setCustomNameVisible(false);
            e.noClip = true;
            e.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0f);
            e.setHeadYaw(yaw); e.setBodyYaw(yaw);
            w.spawnEntity(e);
            return e;
        } catch (Throwable t) {
            EmiHuida.LOG.error("No se pudo crear un michi de la horda ({})", forma, t);
            return null;
        }
    }

    /** Donde esta un michi cuando la primera fila va por {@code sFrente}; {@code t} = tick (para los pasitos al correr) y {@code salto} = altura de los brincos de emocion (escenas). */
    static Vec3d posicion(int hueco, double sFrente, Michi m, long t, double salto) {
        double s = sFrente - m.ds;
        double v = m.v + 0.25 * Math.sin(t * 0.17 + m.fase);                                 // se balancean un poco de lado a lado
        double y = m.encima + (t > 0 ? 0.12 * Math.abs(Math.sin(t * 0.55 + m.fase)) : 0) + salto * Math.abs(Math.sin(t * 0.42 + m.fase)) + Nivel.saltoHueco(s);            // pasitos, brincos y, sobre un hueco, un salto
        return Escenas.P(hueco, Math.max(0, s), v, y);
    }

    static Vec3d posicion(int hueco, double sFrente, Michi m, long t) { return posicion(hueco, sFrente, m, t, 0); }

    /** Coloca un michi (lo teletransporta) mirando a {@code yaw}; {@code corre} = patas de correr a la velocidad {@code vel} (bloques/s). */
    static void poner(Michi m, Vec3d pos, float yaw, boolean corre, double vel) {
        PokemonEntity e = m.e;
        if (e == null || e.isRemoved()) return;
        e.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0f);
        e.setHeadYaw(yaw); e.setBodyYaw(yaw);
        ((Corredor) (Object) e).emi_huida$setPaso(corre ? (float) (0.6 * vel / 20.0) : 0f);
        e.setSprinting(corre);
        e.setVelocity(Vec3d.ZERO);
        if (e.getWorld() instanceof ServerWorld sw) sw.getChunkManager().sendToNearbyPlayers(e, new net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket(e));
    }

    /**
     * Mueve la horda un tick: la primera fila va por {@code sFrente}; si {@code corriendo} los michis corren (pose de correr de Cobblemon) y si no se quedan quietos.
     * {@code vel} = velocidad (bloques/s) para el ritmo de las patas, {@code salto} = brincos de emocion (0 = ninguno) y {@code yawExtra} = grados que giran respecto al camino (180 = miran hacia atras).
     */
    void mover(Huida.J j, double sFrente, long fase, boolean corriendo, double vel, double salto, float yawExtra) {
        for (Michi m : michis) {
            float yaw = (float) Nivel.rumbo(Math.max(0, sFrente - m.ds)) + yawExtra;
            poner(m, posicion(j.hueco, sFrente, m, corriendo || salto > 0 ? fase : 0, salto), yaw, corriendo, vel);
        }
    }

    void mover(Huida.J j, double sFrente, long fase, boolean corriendo, double vel) { mover(j, sFrente, fase, corriendo, vel, 0, 0f); }

    void quitar() {
        for (Michi m : michis) if (m.e != null && !m.e.isRemoved()) m.e.discard();
        michis.clear();
    }
}
