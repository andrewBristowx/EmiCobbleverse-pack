package emi.survivors;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Una bola que lanza un enemigo a distancia: avanza recta (sin atravesar a nadie) y dana al primer jugador en pie que toca. */
final class DisparoEnemigo {
    private Vec3d pos;
    private final Vec3d dir;           // unitario, horizontal
    private final double vel;          // bloques por tick
    private final float dmg;
    private final ParticleEffect particula;
    private int vida;                  // ticks

    DisparoEnemigo(Vec3d origen, Vec3d dir, double vel, float dmg, ParticleEffect particula, int vida) {
        this.pos = origen; this.dir = dir; this.vel = vel; this.dmg = dmg; this.particula = particula; this.vida = vida;
    }

    /** Avanza un tick; true = sigue vivo. */
    boolean tick(ServerWorld w, List<Run> objetivos) {
        for (int paso = 0; paso < 2; paso++) {
            pos = pos.add(dir.multiply(vel / 2));
            w.spawnParticles(particula, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.0);
            for (Run m : objetivos) {
                if (m.estado != Run.VIVO) continue;
                double dx = m.pp.x - pos.x, dz = m.pp.z - pos.z;
                if (dx * dx + dz * dz > 0.7 * 0.7 || Math.abs(m.pp.y + 0.6 - pos.y) > 1.6) continue;
                if (m.iframes <= 0) m.danarJugador(dmg);
                return false;
            }
        }
        return --vida > 0;
    }
}
