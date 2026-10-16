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
    final PokemonEntity e;
    final Especies.E especie;
    float hp, hpMax;
    final float dmg;
    final double vel;           // bloques por tick
    final double radio;         // radio de contacto (bloques)
    int cdContacto;
    double kbx, kbz;            // empuje por golpes (bloques/tick, decae)
    double sepx, sepz;          // separacion de los vecinos (la calcula la partida cada tick)

    private Enemigo(PokemonEntity e, Especies.E especie, float hp, float dmg, double velBloquesPorSegundo, double radio) {
        this.e = e; this.especie = especie; this.hp = this.hpMax = hp; this.dmg = dmg; this.vel = velBloquesPorSegundo / 20.0; this.radio = radio;
    }

    /** Crea y coloca un enemigo; null si Cobblemon no pudo crearlo. {@code fuerte} = tramo de BST mas alto (mas vida y dano). */
    static Enemigo crear(ServerWorld w, Especies.E sp, String extra, Vec3d pos, boolean fuerte, java.util.Random rnd) {
        try {
            PokemonEntity e = PokemonProperties.Companion.parse(sp.id() + " level=" + (fuerte ? 20 : 8) + (extra == null || extra.isEmpty() ? "" : " " + extra)).createEntity(w);
            float alto = 1.0f + rnd.nextFloat() * 0.7f;                      // todos miden entre 1,0 y 1,7 bloques para que se vean parecidos
            float escala = Math.max(0.3f, Math.min(3.0f, alto / sp.alto()));
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
            float hp = fuerte ? 70f : 40f, dmg = fuerte ? 8f : 5f;
            double vel = (fuerte ? 2.8 : 2.3) + rnd.nextDouble() * 0.9;       // bloques/s (el jugador va a ~6)
            return new Enemigo(e, sp, hp, dmg, vel, 0.35 + alto * 0.25);
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
    void mover(double ox, double oz) {
        double dx = ox - e.getX(), dz = oz - e.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        double vx = 0, vz = 0;
        if (d > radio + 0.2) { vx = dx / d * vel; vz = dz / d * vel; }
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
        e.setSprinting(d > radio + 0.4);
    }

    void quitar() { if (e != null && !e.isRemoved()) e.discard(); }
}
