package emi.survivors;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Un Pokemon enemigo: un {@link PokemonEntity} real de Cobblemon sin IA, invulnerable y silencioso, que el mod mueve cada tick hacia el jugador con la fisica
 * normal (se pone su velocidad y Minecraft resuelve las colisiones, la gravedad y la animacion de andar). Su vida es nuestra, no la de Minecraft.
 */
final class Enemigo {
    /** DEBIL y FUERTE = los normales (tramo bajo y alto de estadisticas), MICHI = un Michialien (el toque de Emi), ELITE = uno grande con mucha vida, JEFE = el jefe. */
    enum Tipo { DEBIL, FUERTE, MICHI, ELITE, JEFE }

    final PokemonEntity e;
    final Especies.E especie;
    final Tipo tipo;
    final int xp;               // experiencia que suelta al caer
    float hp, hpMax;
    final float dmg;
    final double vel;           // bloques por tick
    final double radio;         // radio de contacto (bloques)
    int cdContacto;
    double kbx, kbz;            // empuje por golpes (bloques/tick, decae)
    double sepx, sepz;          // separacion de los vecinos (la calcula la partida cada tick)

    private Enemigo(PokemonEntity e, Especies.E especie, Tipo tipo, int xp, float hp, float dmg, double velBloquesPorSegundo, double radio) {
        this.e = e; this.especie = especie; this.tipo = tipo; this.xp = xp; this.hp = this.hpMax = hp; this.dmg = dmg; this.vel = velBloquesPorSegundo / 20.0; this.radio = radio;
    }

    boolean grande() { return tipo == Tipo.ELITE || tipo == Tipo.JEFE; }

    /**
     * Crea y coloca un enemigo; null si Cobblemon no pudo crearlo. {@code hpMul}, {@code dmgMul} y {@code velMul} son los multiplicadores del momento de la partida
     * (ver {@link Curvas}); {@code hpJefe} solo lo usa el jefe.
     */
    static Enemigo crear(ServerWorld w, Especies.E sp, String extra, Vec3d pos, Tipo tipo, double hpMul, double dmgMul, double velMul, float hpJefe, java.util.Random rnd) {
        try {
            int nivel = switch (tipo) { case DEBIL -> 8; case ELITE -> 35; case JEFE -> 60; default -> 20; };
            PokemonEntity e = PokemonProperties.Companion.parse(sp.id() + " level=" + nivel + (extra == null || extra.isEmpty() ? "" : " " + extra)).createEntity(w);
            float alto = switch (tipo) { case ELITE -> 2.4f + rnd.nextFloat() * 0.5f; case JEFE -> 4.6f; default -> 1.0f + rnd.nextFloat() * 0.7f; };   // los normales se ven parecidos
            float escala = Math.max(0.3f, Math.min(8.0f, alto / sp.alto()));
            e.getPokemon().setScaleModifier(escala);
            e.setAiDisabled(true);
            e.setInvulnerable(true);
            e.setSilent(true);
            e.setPersistent();
            e.setCustomNameVisible(false);
            e.addCommandTag("emi_survivors");
            double yaw = rnd.nextDouble() * 360.0;
            e.refreshPositionAndAngles(pos.x, pos.y, pos.z, (float) yaw, 0f);
            w.spawnEntity(e);
            float hp, dmg; double vel; int xp;
            switch (tipo) {
                case FUERTE -> { hp = 42f; dmg = 8f; vel = 2.8 + rnd.nextDouble() * 0.9; xp = Curvas.xpEnemigo(true, false); }
                case MICHI -> { hp = 36f; dmg = 6f; vel = 3.0 + rnd.nextDouble() * 0.8; xp = Curvas.xpEnemigo(true, true); }
                case ELITE -> { hp = 240f; dmg = 12f; vel = 2.6; xp = 10; }
                case JEFE -> { hp = hpJefe; dmg = 25f; vel = 2.2; xp = 60; }
                default -> { hp = 24f; dmg = 5f; vel = 2.3 + rnd.nextDouble() * 0.9; xp = Curvas.xpEnemigo(false, false); }
            }
            if (tipo != Tipo.JEFE) hp *= (float) hpMul;
            dmg *= (float) dmgMul;
            vel *= velMul;
            return new Enemigo(e, sp, tipo, xp, hp, dmg, vel, 0.35 + alto * (tipo == Tipo.JEFE ? 0.33 : 0.25));
        } catch (Throwable t) {
            EmiSurvivors.LOG.error("No se pudo crear el enemigo {}", sp.id(), t);
            return null;
        }
    }

    /**
     * Un tick de movimiento hacia (ox, oz): velocidad horizontal propia mas el empuje. Se mueve con {@code Entity.move} (que resuelve las colisiones con bloques y
     * suma la distancia caminada que Cobblemon usa para la animacion de andar): con la IA apagada, la fisica normal ({@code travel}) no mueve a un Pokemon de Cobblemon.
     * La componente vertical (-0,2) lo pega al suelo.
     */
    void mover(double ox, double oz) { mover(ox, oz, 1.0); }

    /** Igual, con la velocidad multiplicada por {@code factor} (la carga del jefe, o 0 para dejarlo quieto mirando al jugador). */
    void mover(double ox, double oz, double factor) {
        double dx = ox - e.getX(), dz = oz - e.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        double vx = 0, vz = 0;
        if (d > radio + 0.2 && factor != 0) { vx = dx / d * vel * factor; vz = dz / d * vel * factor; }
        vx += kbx + sepx; vz += kbz + sepz;
        kbx *= 0.78; kbz *= 0.78;
        if (Math.abs(kbx) < 0.003) kbx = 0;
        if (Math.abs(kbz) < 0.003) kbz = 0;
        if (d > 0.05) {
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            e.setYaw(yaw); e.setHeadYaw(yaw); e.setBodyYaw(yaw);
        }
        e.setVelocity(Vec3d.ZERO);
        e.move(MovementType.SELF, new Vec3d(vx, -0.2, vz));
        e.setSprinting(factor != 0 && d > radio + 0.4);
    }

    void quitar() { if (e != null && !e.isRemoved()) e.discard(); }
}
