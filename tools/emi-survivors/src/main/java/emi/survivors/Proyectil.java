package emi.survivors;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** El disparo del ataque automatico: una bola de particulas del color del tipo del Pokemon que avanza recto, atraviesa a algunos enemigos y los empuja. */
final class Proyectil {
    private final ServerWorld w;
    private Vec3d pos;
    private final Vec3d dir;           // unitario, horizontal
    private final float dmg;
    private final ParticleEffect particula;
    private int vida;                  // ticks
    private int perfora;
    private final Set<Enemigo> golpeados = new HashSet<>();
    static final double VEL = 1.0;     // bloques por tick

    Proyectil(ServerWorld w, Vec3d origen, Vec3d dir, float dmg, ParticleEffect particula, int vida, int perfora) {
        this.w = w; this.pos = origen; this.dir = dir; this.dmg = dmg; this.particula = particula; this.vida = vida; this.perfora = perfora;
    }

    /** Avanza un tick; true = sigue vivo. */
    boolean tick(List<Enemigo> enemigos) {
        for (int paso = 0; paso < 2; paso++) {
            pos = pos.add(dir.multiply(VEL / 2));
            w.spawnParticles(particula, pos.x, pos.y, pos.z, 2, 0.06, 0.06, 0.06, 0.0);
            for (Enemigo en : enemigos) {
                if (en.hp <= 0 || golpeados.contains(en)) continue;
                double dx = en.e.getX() - pos.x, dz = en.e.getZ() - pos.z;
                double r = en.radio + 0.55;
                if (dx * dx + dz * dz > r * r || Math.abs(en.e.getY() + 0.6 - pos.y) > 2.0) continue;
                golpeados.add(en);
                en.hp -= dmg;
                en.kbx += dir.x * 0.5; en.kbz += dir.z * 0.5;
                w.sendEntityStatus(en.e, (byte) 2);                        // destello rojo de golpe
                w.spawnParticles(ParticleTypes.CRIT, en.e.getX(), en.e.getY() + 0.7, en.e.getZ(), 4, 0.25, 0.25, 0.25, 0.1);
                if (--perfora <= 0) return false;
            }
        }
        return --vida > 0;
    }
}
