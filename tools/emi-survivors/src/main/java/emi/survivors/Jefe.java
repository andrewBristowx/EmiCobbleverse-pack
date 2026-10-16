package emi.survivors;

import net.minecraft.entity.MovementType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * El jefe de la partida (por ahora uno, a los 3:00): un Pokemon enorme (Snorlax Emi si esta Emipokemon) que persigue despacio al jugador en pie mas cercano y alterna
 * dos ataques AVISADOS con particulas rojas en el suelo, para que se pueda esquivar: un salto aplastante (circulo rojo: cae donde estaba el jugador al empezar a avisar)
 * y una carga en linea recta (linea roja). Si hay varios jugadores, los dos ataques hacen dano a TODOS los que pillen. Su vida y su dano de contacto los lleva su
 * {@link Enemigo}; los golpes de los jugadores le llegan como a cualquier enemigo.
 */
final class Jefe {
    private static final int PERSIGUE = 0, AVISO_GOLPE = 1, SALTO = 2, AVISO_CARGA = 3, CARGA = 4, DESCANSO = 5;
    static final double RADIO_GOLPE = 4.6;
    static final float DANO_GOLPE = 35f, DANO_CARGA = 40f;

    final Equipo eq;
    final Enemigo en;
    final String nombre;
    private int estado = PERSIGUE, t, vuelta;
    private double tx, tz;              // donde caera el salto
    private double sx, sz, y0;          // donde empezo el salto
    private double dx, dz;              // direccion de la carga
    private final Set<Run> golpeadosCarga = new HashSet<>();

    Jefe(Equipo eq, Enemigo en, String nombre) { this.eq = eq; this.en = en; this.nombre = nombre; }

    boolean vivo() { return en.hp > 0 && !en.e.isRemoved(); }

    /** Un tick; {@code objetivos} = los jugadores a los que puede perseguir y golpear (en pie y sin estar eligiendo carta). */
    void tick(List<Run> objetivos) {
        t++;
        double bx = en.e.getX(), bz = en.e.getZ();
        Run obj = Equipo.masCercano(objetivos, bx, bz);
        double suelo = Arena.Y0 + 0.1;
        switch (estado) {
            case PERSIGUE -> {
                if (obj == null) { en.mover(bx, bz, 0); return; }
                en.mover(obj.pp.x, obj.pp.z);
                if (t >= 90) {
                    t = 0;
                    if (vuelta++ % 2 == 0) { estado = AVISO_GOLPE; tx = obj.pp.x; tz = obj.pp.z; }
                    else {
                        estado = AVISO_CARGA;
                        double l = Math.hypot(obj.pp.x - bx, obj.pp.z - bz);
                        dx = l < 1e-6 ? 0 : (obj.pp.x - bx) / l; dz = l < 1e-6 ? 1 : (obj.pp.z - bz) / l;
                        golpeadosCarga.clear();
                    }
                }
            }
            case AVISO_GOLPE -> {
                en.mover(tx, tz, 0);
                if (t % 2 == 0) {
                    int pts = 16;
                    for (int k = 0; k < pts; k++) {
                        double a = k * Math.PI * 2 / pts;
                        eq.w.spawnParticles(Armas.ROJO, tx + Math.cos(a) * RADIO_GOLPE, suelo, tz + Math.sin(a) * RADIO_GOLPE, 1, 0, 0, 0, 0);
                    }
                    if (t > 14) eq.w.spawnParticles(Armas.ROJO, tx, suelo, tz, 10, RADIO_GOLPE * 0.5, 0.0, RADIO_GOLPE * 0.5, 0.0);
                }
                if (t >= 28) { estado = SALTO; t = 0; sx = bx; sz = bz; y0 = en.e.getY(); }
            }
            case SALTO -> {
                double f = Math.min(1.0, t / 10.0);
                en.e.refreshPositionAndAngles(sx + (tx - sx) * f, y0 + Math.sin(f * Math.PI) * 2.4, sz + (tz - sz) * f, en.e.getYaw(), 0f);
                if (t >= 10) {
                    en.e.refreshPositionAndAngles(tx, y0, tz, en.e.getYaw(), 0f);
                    eq.w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, tx, y0 + 0.5, tz, 1, 0, 0, 0, 0);
                    for (int k = 0; k < 30; k++) {
                        double a = k * Math.PI * 2 / 30;
                        eq.w.spawnParticles(ParticleTypes.CLOUD, tx + Math.cos(a) * RADIO_GOLPE, y0 + 0.3, tz + Math.sin(a) * RADIO_GOLPE, 2, 0.1, 0.1, 0.1, 0.05);
                    }
                    for (Run m : objetivos)
                        if (Math.hypot(m.pp.x - tx, m.pp.z - tz) <= RADIO_GOLPE && Math.abs(m.pp.y - y0) < 2.5) m.danarJugador(DANO_GOLPE);
                    estado = DESCANSO; t = 0;
                }
            }
            case AVISO_CARGA -> {
                en.e.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz))); en.e.setHeadYaw(en.e.getYaw()); en.e.setBodyYaw(en.e.getYaw());
                if (t % 2 == 0) for (double d = 1.5; d <= 15; d += 1.0) eq.w.spawnParticles(Armas.ROJO, bx + dx * d, suelo, bz + dz * d, 1, 0.15, 0.0, 0.15, 0.0);
                if (t >= 24) { estado = CARGA; t = 0; }
            }
            case CARGA -> {
                en.e.setVelocity(Vec3d.ZERO);
                en.e.move(MovementType.SELF, new Vec3d(dx * 0.7, -0.2, dz * 0.7));
                eq.w.spawnParticles(ParticleTypes.CLOUD, bx, en.e.getY() + 0.2, bz, 4, en.radio * 0.5, 0.1, en.radio * 0.5, 0.02);
                for (Run m : objetivos)
                    if (!golpeadosCarga.contains(m) && Math.hypot(m.pp.x - bx, m.pp.z - bz) <= en.radio + 0.7) { golpeadosCarga.add(m); m.danarJugador(DANO_CARGA); }
                if (t >= 18) { estado = DESCANSO; t = 0; }
            }
            default -> {            // DESCANSO: se queda un momento recuperando el aliento
                if (obj == null) en.mover(bx, bz, 0); else en.mover(obj.pp.x, obj.pp.z, 0.4);
                if (t >= 40) { estado = PERSIGUE; t = 0; }
            }
        }
    }
}
