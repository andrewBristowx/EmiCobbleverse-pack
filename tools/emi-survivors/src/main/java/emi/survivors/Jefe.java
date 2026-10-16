package emi.survivors;

import net.minecraft.entity.MovementType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

/**
 * El jefe de la partida (por ahora uno, a los 3:00): un Pokemon enorme (Snorlax Emi si esta Emipokemon) que persigue despacio y alterna dos ataques AVISADOS con
 * particulas rojas en el suelo, para que el jugador pueda esquivarlos: un salto aplastante (circulo rojo: cae donde estabas al empezar a avisar) y una carga en linea
 * recta (linea roja). Su vida y su dano de contacto los lleva su {@link Enemigo}; los golpes del jugador le llegan como a cualquier enemigo.
 */
final class Jefe {
    private static final int PERSIGUE = 0, AVISO_GOLPE = 1, SALTO = 2, AVISO_CARGA = 3, CARGA = 4, DESCANSO = 5;
    static final double RADIO_GOLPE = 4.6;
    static final float DANO_GOLPE = 35f, DANO_CARGA = 40f;

    final Run r;
    final Enemigo en;
    final String nombre;
    private int estado = PERSIGUE, t, vuelta;
    private double tx, tz;              // donde caera el salto
    private double sx, sz, y0;          // donde empezo el salto
    private double dx, dz;              // direccion de la carga
    private boolean cargaGolpeo;

    Jefe(Run r, Enemigo en, String nombre) { this.r = r; this.en = en; this.nombre = nombre; }

    boolean vivo() { return en.hp > 0 && !en.e.isRemoved(); }

    void tick(Vec3d pp) {
        t++;
        double bx = en.e.getX(), bz = en.e.getZ();
        switch (estado) {
            case PERSIGUE -> {
                en.mover(pp.x, pp.z);
                if (t >= 90) {
                    t = 0;
                    if (vuelta++ % 2 == 0) { estado = AVISO_GOLPE; tx = pp.x; tz = pp.z; }
                    else {
                        estado = AVISO_CARGA;
                        double l = Math.hypot(pp.x - bx, pp.z - bz);
                        dx = l < 1e-6 ? 0 : (pp.x - bx) / l; dz = l < 1e-6 ? 1 : (pp.z - bz) / l;
                        cargaGolpeo = false;
                    }
                }
            }
            case AVISO_GOLPE -> {
                en.mover(tx, tz, 0);
                if (t % 2 == 0) {
                    int pts = 16;
                    for (int k = 0; k < pts; k++) {
                        double a = k * Math.PI * 2 / pts;
                        r.w.spawnParticles(Armas.ROJO, tx + Math.cos(a) * RADIO_GOLPE, pp.y + 0.1, tz + Math.sin(a) * RADIO_GOLPE, 1, 0, 0, 0, 0);
                    }
                    if (t > 14) r.w.spawnParticles(Armas.ROJO, tx, pp.y + 0.1, tz, 10, RADIO_GOLPE * 0.5, 0.0, RADIO_GOLPE * 0.5, 0.0);
                }
                if (t >= 28) { estado = SALTO; t = 0; sx = bx; sz = bz; y0 = en.e.getY(); }
            }
            case SALTO -> {
                double f = Math.min(1.0, t / 10.0);
                en.e.refreshPositionAndAngles(sx + (tx - sx) * f, y0 + Math.sin(f * Math.PI) * 2.4, sz + (tz - sz) * f, en.e.getYaw(), 0f);
                if (t >= 10) {
                    en.e.refreshPositionAndAngles(tx, y0, tz, en.e.getYaw(), 0f);
                    r.w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, tx, y0 + 0.5, tz, 1, 0, 0, 0, 0);
                    for (int k = 0; k < 30; k++) {
                        double a = k * Math.PI * 2 / 30;
                        r.w.spawnParticles(ParticleTypes.CLOUD, tx + Math.cos(a) * RADIO_GOLPE, y0 + 0.3, tz + Math.sin(a) * RADIO_GOLPE, 2, 0.1, 0.1, 0.1, 0.05);
                    }
                    if (Math.hypot(pp.x - tx, pp.z - tz) <= RADIO_GOLPE && Math.abs(pp.y - y0) < 2.5) r.danarJugador(DANO_GOLPE);
                    estado = DESCANSO; t = 0;
                }
            }
            case AVISO_CARGA -> {
                en.e.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz))); en.e.setHeadYaw(en.e.getYaw()); en.e.setBodyYaw(en.e.getYaw());
                if (t % 2 == 0) for (double d = 1.5; d <= 15; d += 1.0) r.w.spawnParticles(Armas.ROJO, bx + dx * d, pp.y + 0.1, bz + dz * d, 1, 0.15, 0.0, 0.15, 0.0);
                if (t >= 24) { estado = CARGA; t = 0; }
            }
            case CARGA -> {
                en.e.setVelocity(Vec3d.ZERO);
                en.e.move(MovementType.SELF, new Vec3d(dx * 0.7, -0.2, dz * 0.7));
                r.w.spawnParticles(ParticleTypes.CLOUD, bx, en.e.getY() + 0.2, bz, 4, en.radio * 0.5, 0.1, en.radio * 0.5, 0.02);
                if (!cargaGolpeo && Math.hypot(pp.x - bx, pp.z - bz) <= en.radio + 0.7) { cargaGolpeo = true; r.danarJugador(DANO_CARGA); }
                if (t >= 18) { estado = DESCANSO; t = 0; }
            }
            default -> {            // DESCANSO: se queda un momento recuperando el aliento
                en.mover(pp.x, pp.z, 0.4);
                if (t >= 40) { estado = PERSIGUE; t = 0; }
            }
        }
    }
}
