package emi.survivors;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Un disparo: una bola de particulas (las del tipo del Pokemon) que avanza recta, atraviesa a algunos enemigos y los empuja. */
final class Proyectil {
    private final Run run;
    private Vec3d pos;
    private final Vec3d dir;           // unitario, horizontal
    private final double dmg;          // dano base (el dano real suma las mejoras del jugador)
    private final ParticleEffect particula;
    private int vida;                  // ticks
    private int perfora;
    private final Set<Enemigo> golpeados = new HashSet<>();
    static final double VEL = 1.0;     // bloques por tick

    Proyectil(Run run, Vec3d origen, Vec3d dir, double dmg, ParticleEffect particula, int vida, int perfora) {
        this.run = run; this.pos = origen; this.dir = dir; this.dmg = dmg; this.particula = particula; this.vida = vida; this.perfora = perfora;
    }

    /** Avanza un tick; true = sigue vivo. */
    boolean tick(List<Enemigo> enemigos) {
        for (int paso = 0; paso < 2; paso++) {
            pos = pos.add(dir.multiply(VEL / 2));
            run.w.spawnParticles(particula, pos.x, pos.y, pos.z, 2, 0.06, 0.06, 0.06, 0.0);
            for (Enemigo en : enemigos) {
                if (en.hp <= 0 || golpeados.contains(en)) continue;
                double dx = en.e.getX() - pos.x, dz = en.e.getZ() - pos.z;
                double r = en.radio + 0.55;
                if (dx * dx + dz * dz > r * r || Math.abs(en.e.getY() + 0.6 - pos.y) > 2.0) continue;
                golpeados.add(en);
                run.golpear(en, dmg, dir.x * 0.5, dir.z * 0.5);
                if (--perfora <= 0) return false;
            }
        }
        return --vida > 0;
    }
}
