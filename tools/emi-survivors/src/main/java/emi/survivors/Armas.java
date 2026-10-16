package emi.survivors;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Los ataques del jugador (se usan solos). Cada uno es una clase con una tabla de 5 niveles (dano, tiempo entre usos, cantidad, alcance...); lo que dice cada
 * nivel al jugador esta en {@link Catalogo}. Todos se dibujan con particulas del servidor; el dano lo aplica {@link Run#golpear}, que suma las mejoras del jugador.
 * El ataque con el que empieza el Pokemon usa las particulas de su tipo (por ejemplo, llamas el de tipo Fuego).
 */
final class Armas {
    private Armas() {}

    static Arma crear(String id, Run r, int nivel) {
        return switch (id) {
            case "disparo" -> new Disparo(r, nivel);
            case "orbes" -> new Orbes(r, nivel);
            case "pulso" -> new Pulso(r, nivel);
            case "cadena" -> new Cadena(r, nivel);
            case "meteoros" -> new Meteoros(r, nivel);
            case "tajo" -> new Tajo(r, nivel);
            case "bumeran" -> new Bumeran(r, nivel);
            case "estela" -> new Estela(r, nivel);
            case "minas" -> new Minas(r, nivel);
            case "rayo" -> new Rayo(r, nivel);
            default -> Armas2.crear(id, r, nivel);
        };
    }

    static final DustParticleEffect ROJO = new DustParticleEffect(new Vector3f(1f, 0.25f, 0.2f), 1.2f);
    static final DustParticleEffect ROSA = new DustParticleEffect(new Vector3f(1f, 0.55f, 0.85f), 1.0f);

    // ------------------------------------------------------------------------------------------------ base

    abstract static class Arma {
        final Run r;
        final String id;
        int nivel;
        int cd;
        int usadoEn = -1000;                     // el tick en que se uso por ultima vez (lo apunta cdt(); la interfaz lo usa para encender la ranura)

        Arma(Run r, String id, int nivel) { this.r = r; this.id = id; this.nivel = nivel; }

        /** Un tick del ataque; {@code pp} = posicion del jugador. */
        abstract void tick(Vec3d pp);

        final int i() { return Math.max(1, Math.min(Catalogo.NIVEL_EVO, nivel)) - 1; }

        /** ¿Ha evolucionado? (nivel 6). */
        final boolean evo() { return nivel >= Catalogo.NIVEL_EVO; }

        /** Ticks hasta el siguiente uso (con la mejora de recarga del jugador; nunca menos de 4). */
        final int cdt(double base) { usadoEn = r.tick; return Math.max(4, (int) Math.round(base * r.st.recarga)); }

        final double area(double v) { return v * r.st.area; }

        final ParticleEffect part(ParticleEffect nativa) { return r.particula(id, nativa); }
    }

    // ------------------------------------------------------------------------------------------------ utilidades

    static Vec3d rotar(Vec3d d, double rad) {
        double c = Math.cos(rad), s = Math.sin(rad);
        return new Vec3d(d.x * c - d.z * s, 0, d.x * s + d.z * c);
    }

    static Vec3d unitario(double dx, double dz) {
        double l = Math.sqrt(dx * dx + dz * dz);
        return l < 1e-6 ? new Vec3d(0, 0, 1) : new Vec3d(dx / l, 0, dz / l);
    }

    /** Distancia de un punto (px, pz) al segmento (ax, az)-(bx, bz). */
    static double distSegmento(double px, double pz, double ax, double az, double bx, double bz) {
        double vx = bx - ax, vz = bz - az, l2 = vx * vx + vz * vz;
        double t = l2 < 1e-9 ? 0 : Math.max(0, Math.min(1, ((px - ax) * vx + (pz - az) * vz) / l2));
        double qx = ax + vx * t, qz = az + vz * t;
        return Math.sqrt((px - qx) * (px - qx) + (pz - qz) * (pz - qz));
    }

    /** Pinta una linea de particulas entre dos puntos (cada {@code paso} bloques, con un tope). */
    static void linea(Run r, ParticleEffect p, Vec3d a, Vec3d b, double paso, int tope) {
        double l = a.distanceTo(b);
        int n = Math.min(tope, Math.max(1, (int) (l / paso)));
        for (int k = 0; k <= n; k++) {
            double f = k / (double) n;
            r.w.spawnParticles(p, a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f, a.z + (b.z - a.z) * f, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }

    // ------------------------------------------------------------------------------------------------ 1. disparo

    static final class Disparo extends Arma {
        static final double[] DMG = {16, 20, 20, 24, 28, 30}, CD = {18, 18, 18, 15, 15, 9};
        static final int[] CNT = {1, 1, 2, 2, 3, 4}, PERF = {2, 2, 2, 3, 3, 99};

        Disparo(Run r, int nivel) { super(r, "disparo", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) { cd--; return; }
            Enemigo o = r.masCercano(pp, 15, null);
            if (o == null) return;
            Vec3d d = unitario(o.e.getX() - pp.x, o.e.getZ() - pp.z);
            int n = CNT[i()];
            for (int k = 0; k < n; k++) {
                double ang = n == 1 ? 0 : (k - (n - 1) / 2.0) * Math.toRadians(11);
                r.proyectiles.add(new Proyectil(r, pp.add(0, 0.9, 0), rotar(d, ang), DMG[i()], part(ParticleTypes.CRIT), 18, PERF[i()]));
            }
            cd = cdt(CD[i()]);
        }
    }

    // ------------------------------------------------------------------------------------------------ 2. orbes

    static final class Orbes extends Arma {
        static final double[] DMG = {6, 6, 8, 8, 10, 22}, RAD = {2.6, 2.6, 3.2, 3.2, 3.6, 4.8}, VEL = {7, 7, 9, 9, 10, 13};
        static final int[] CNT = {2, 3, 3, 4, 5, 8};
        private double ang;
        private final Map<Enemigo, Integer> espera = new HashMap<>();

        Orbes(Run r, int nivel) { super(r, "orbes", nivel); }

        @Override void tick(Vec3d pp) {
            ang += VEL[i()];
            for (Iterator<Map.Entry<Enemigo, Integer>> it = espera.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                if (e.getKey().hp <= 0 || e.getValue() <= 1) it.remove(); else e.setValue(e.getValue() - 1);
            }
            double hit = 0.9 + (nivel >= 3 ? 0.25 : 0) + (evo() ? 0.4 : 0);
            anillo(pp, CNT[i()], area(RAD[i()]), ang, hit);
            if (evo()) anillo(pp, 4, area(2.4), -ang * 1.3, hit);           // la evolucion: un segundo anillo interior que gira al reves
        }

        private void anillo(Vec3d pp, int n, double R, double angBase, double hit) {
            for (int k = 0; k < n; k++) {
                double a = Math.toRadians(angBase + 360.0 / n * k);
                double ox = pp.x + Math.cos(a) * R, oz = pp.z + Math.sin(a) * R, oy = pp.y + 0.9;
                r.w.spawnParticles(part(ParticleTypes.END_ROD), ox, oy, oz, evo() ? 3 : 2, 0.08, 0.08, 0.08, 0.0);
                if (evo()) r.w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, ox, oy, oz, 1, 0.1, 0.1, 0.1, 0.0);
                for (Enemigo en : r.enemigos) {
                    if (en.hp <= 0 || espera.containsKey(en)) continue;
                    double dx = en.e.getX() - ox, dz = en.e.getZ() - oz, rr = hit + en.radio;
                    if (dx * dx + dz * dz > rr * rr || Math.abs(en.e.getY() + 0.6 - oy) > 2.2) continue;
                    espera.put(en, 12);
                    Vec3d f = unitario(en.e.getX() - pp.x, en.e.getZ() - pp.z);
                    r.golpear(en, DMG[i()], f.x * (evo() ? 0.8 : 0.5), f.z * (evo() ? 0.8 : 0.5));
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 3. pulso

    static final class Pulso extends Arma {
        static final double[] DMG = {20, 26, 26, 34, 42, 60}, CD = {70, 64, 56, 56, 50, 44}, RAD = {4.2, 4.6, 5.6, 5.6, 7.0, 9.0};
        private static final class Anillo { double x, z, actual, max, dmg; int espera; final java.util.Set<Enemigo> golpeados = new java.util.HashSet<>(); }
        private final List<Anillo> anillos = new ArrayList<>();

        Pulso(Run r, int nivel) { super(r, "pulso", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else {
                double max = area(RAD[i()]);
                if (r.masCercano(pp, max + 1.5, null) != null) {
                    Anillo a = new Anillo(); a.x = pp.x; a.z = pp.z; a.max = max; a.dmg = DMG[i()];
                    if (evo()) a.espera = 12;                       // la evolucion primero atrae a los enemigos y luego lanza la onda
                    anillos.add(a);
                    cd = cdt(CD[i()]);
                }
            }
            for (Iterator<Anillo> it = anillos.iterator(); it.hasNext(); ) {
                Anillo a = it.next();
                if (a.espera > 0) {
                    a.espera--;
                    for (int k = 0; k < 14; k++) {
                        double t = r.rnd.nextDouble() * Math.PI * 2, rr = a.max * (0.5 + 0.5 * a.espera / 12.0);
                        r.w.spawnParticles(ParticleTypes.REVERSE_PORTAL, a.x + Math.cos(t) * rr, pp.y + 0.4, a.z + Math.sin(t) * rr, 1, 0.0, 0.1, 0.0, 0.0);
                    }
                    for (Enemigo en : r.enemigos) {
                        if (en.hp <= 0) continue;
                        double dx = a.x - en.e.getX(), dz = a.z - en.e.getZ();
                        if (dx * dx + dz * dz > a.max * a.max * 1.4 || dx * dx + dz * dz < 1.4) continue;
                        r.impulso(en, dx, dz, 0.42);
                    }
                    continue;
                }
                a.actual += a.max / 6.0;
                int pts = (int) Math.min(48, Math.max(10, a.actual * 7));
                for (int k = 0; k < pts; k++) {
                    double t = k * Math.PI * 2 / pts;
                    r.w.spawnParticles(part(ParticleTypes.CLOUD), a.x + Math.cos(t) * a.actual, pp.y + 0.2, a.z + Math.sin(t) * a.actual, 1, 0.0, 0.05, 0.0, 0.0);
                }
                for (Enemigo en : r.enemigos) {
                    if (en.hp <= 0 || a.golpeados.contains(en)) continue;
                    double dx = en.e.getX() - a.x, dz = en.e.getZ() - a.z;
                    if (Math.sqrt(dx * dx + dz * dz) > a.actual + en.radio) continue;
                    a.golpeados.add(en);
                    Vec3d f = unitario(dx, dz);
                    r.golpear(en, a.dmg, f.x * (evo() ? 1.4 : 0.9), f.z * (evo() ? 1.4 : 0.9));
                }
                if (a.actual >= a.max) it.remove();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 4. cadena

    static final class Cadena extends Arma {
        static final double[] DMG = {16, 20, 25, 25, 31, 36}, CD = {45, 42, 38, 38, 34, 28};
        static final int[] SALTOS = {2, 3, 3, 4, 5, 6};

        Cadena(Run r, int nivel) { super(r, "cadena", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) { cd--; return; }
            if (r.masCercano(pp, 13, null) == null) return;
            List<Enemigo> hit = new ArrayList<>();
            ParticleEffect p = part(ParticleTypes.ELECTRIC_SPARK);
            int cadenas = evo() ? 3 : 1;                     // la evolucion lanza 3 cadenas a la vez, cada una desde un enemigo distinto, y aturde
            for (int c = 0; c < cadenas; c++) {
                Enemigo cur = r.masCercano(pp, 13, hit);
                if (cur == null) break;
                Vec3d desde = pp.add(0, 0.9, 0);
                for (int k = 0; k < SALTOS[i()] && cur != null; k++) {
                    hit.add(cur);
                    Vec3d hasta = new Vec3d(cur.e.getX(), cur.e.getY() + 0.7, cur.e.getZ());
                    linea(r, p, desde, hasta, 0.5, 26);
                    r.golpear(cur, DMG[i()], 0, 0);
                    if (evo()) cur.aturdir(10);
                    desde = hasta;
                    cur = r.masCercano(new Vec3d(cur.e.getX(), cur.e.getY(), cur.e.getZ()), 6.5, hit);
                }
            }
            cd = cdt(CD[i()]);
        }
    }

    // ------------------------------------------------------------------------------------------------ 5. meteoros

    static final class Meteoros extends Arma {
        static final double[] DMG = {30, 36, 43, 43, 52, 70}, CD = {80, 74, 70, 66, 56, 44}, RAD = {2.2, 2.2, 2.8, 2.8, 3.2, 4.2};
        static final int[] CNT = {1, 2, 2, 3, 4, 6};
        private static final int CAIDA = 14;
        private static final class Impacto { double x, z, y; int t = CAIDA; }
        private static final class Fuego { double x, z, y, radio; int vida = 70; }
        private final List<Impacto> impactos = new ArrayList<>();
        private final List<Fuego> fuegos = new ArrayList<>();

        Meteoros(Run r, int nivel) { super(r, "meteoros", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else {
                List<Enemigo> cerca = new ArrayList<>();
                for (Enemigo en : r.enemigos) {
                    double dx = en.e.getX() - pp.x, dz = en.e.getZ() - pp.z;
                    if (en.hp > 0 && dx * dx + dz * dz < 14 * 14) cerca.add(en);
                }
                if (!cerca.isEmpty()) {
                    for (int k = 0; k < CNT[i()] && !cerca.isEmpty(); k++) {
                        Enemigo en = cerca.remove(r.rnd.nextInt(cerca.size()));
                        Impacto im = new Impacto(); im.x = en.e.getX(); im.z = en.e.getZ(); im.y = en.e.getY();
                        impactos.add(im);
                    }
                    cd = cdt(CD[i()]);
                }
            }
            double R = area(RAD[i()]);
            for (Iterator<Impacto> it = impactos.iterator(); it.hasNext(); ) {
                Impacto im = it.next();
                im.t--;
                if (im.t > 0) {
                    r.w.spawnParticles(part(ParticleTypes.FLAME), im.x, im.y + im.t * 0.7, im.z, 3, 0.15, 0.15, 0.15, 0.01);
                    if (im.t % 2 == 0) for (int k = 0; k < 10; k++) {
                        double a = k * Math.PI * 2 / 10;
                        r.w.spawnParticles(ROJO, im.x + Math.cos(a) * R, im.y + 0.1, im.z + Math.sin(a) * R, 1, 0, 0, 0, 0);
                    }
                } else {
                    r.w.spawnParticles(ParticleTypes.EXPLOSION, im.x, im.y + 0.5, im.z, 1, 0, 0, 0, 0);
                    r.w.spawnParticles(part(ParticleTypes.FLAME), im.x, im.y + 0.4, im.z, 22, R * 0.4, 0.3, R * 0.4, 0.05);
                    for (Enemigo en : r.enemigos) {
                        if (en.hp <= 0) continue;
                        double dx = en.e.getX() - im.x, dz = en.e.getZ() - im.z, rr = R + en.radio;
                        if (dx * dx + dz * dz > rr * rr) continue;
                        Vec3d f = unitario(dx, dz);
                        r.golpear(en, DMG[i()], f.x * 0.6, f.z * 0.6);
                    }
                    if (evo()) { Fuego fu = new Fuego(); fu.x = im.x; fu.z = im.z; fu.y = im.y; fu.radio = R * 0.8; fuegos.add(fu); }
                    it.remove();
                }
            }
            for (Iterator<Fuego> it = fuegos.iterator(); it.hasNext(); ) {              // la evolucion: el suelo sigue ardiendo un rato
                Fuego fu = it.next();
                if (--fu.vida <= 0) { it.remove(); continue; }
                if (fu.vida % 3 == 0) r.w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, fu.x, fu.y + 0.2, fu.z, 3, fu.radio * 0.5, 0.05, fu.radio * 0.5, 0.01);
                if (fu.vida % 10 != 0) continue;
                for (Enemigo en : r.enemigos) {
                    if (en.hp <= 0) continue;
                    double dx = en.e.getX() - fu.x, dz = en.e.getZ() - fu.z, rr = fu.radio + en.radio * 0.6;
                    if (dx * dx + dz * dz <= rr * rr) r.golpear(en, 9, 0, 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 6. tajo

    static final class Tajo extends Arma {
        static final double[] DMG = {24, 30, 30, 38, 46, 60}, CD = {44, 40, 40, 36, 30, 26}, LARGO = {3.4, 3.4, 3.4, 4.2, 4.2, 6.0};
        private int segundo = -1;          // ticks hasta el segundo tajo (nivel 5)

        Tajo(Run r, int nivel) { super(r, "tajo", nivel); }

        @Override void tick(Vec3d pp) {
            if (segundo > 0 && --segundo == 0) { if (evo()) corteTotal(pp); else { corte(pp, r.mira()); if (nivel >= 3) corte(pp, r.mira().multiply(-1)); } segundo = -1; }
            if (cd > 0) { cd--; return; }
            if (r.masCercano(pp, 8, null) == null) return;
            Vec3d f = r.mira();
            if (evo()) corteTotal(pp);
            else {
                corte(pp, f);
                if (nivel >= 3) corte(pp, f.multiply(-1));
            }
            if (nivel >= 5) segundo = 8;
            cd = cdt(CD[i()]);
        }

        /** La evolucion: un tajo que da toda la vuelta y siempre es critico. */
        private void corteTotal(Vec3d pp) {
            double L = area(LARGO[i()]);
            ParticleEffect p = part(ParticleTypes.CRIT);
            for (int k = 0; k < 24; k++) {
                double a = k * Math.PI * 2 / 24;
                r.w.spawnParticles(k % 3 == 0 ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.ENCHANTED_HIT, pp.x + Math.cos(a) * L * 0.8, pp.y + 0.9, pp.z + Math.sin(a) * L * 0.8, 1, 0.05, 0.05, 0.05, 0.0);
                r.w.spawnParticles(p, pp.x + Math.cos(a) * L * 0.55, pp.y + 0.8, pp.z + Math.sin(a) * L * 0.55, 1, 0.05, 0.05, 0.05, 0.02);
            }
            for (Enemigo en : r.enemigos) {
                if (en.hp <= 0) continue;
                double dx = en.e.getX() - pp.x, dz = en.e.getZ() - pp.z, d = Math.sqrt(dx * dx + dz * dz);
                if (d > L + en.radio) continue;
                Vec3d f = unitario(dx, dz);
                r.golpear(en, DMG[i()], f.x * 0.9, f.z * 0.9, true);
            }
        }

        private void corte(Vec3d pp, Vec3d f) {
            double L = area(LARGO[i()]);
            r.w.spawnParticles(ParticleTypes.SWEEP_ATTACK, pp.x + f.x * L * 0.5, pp.y + 0.9, pp.z + f.z * L * 0.5, 1, 0, 0, 0, 0);
            ParticleEffect p = part(ParticleTypes.CRIT);
            for (int a = -50; a <= 50; a += 25) {
                Vec3d d = rotar(f, Math.toRadians(a));
                r.w.spawnParticles(p, pp.x + d.x * L * 0.85, pp.y + 0.8, pp.z + d.z * L * 0.85, 2, 0.1, 0.1, 0.1, 0.02);
            }
            double cos = Math.cos(Math.toRadians(60));
            for (Enemigo en : r.enemigos) {
                if (en.hp <= 0) continue;
                double dx = en.e.getX() - pp.x, dz = en.e.getZ() - pp.z, d = Math.sqrt(dx * dx + dz * dz);
                if (d > L + en.radio) continue;
                if (d > 0.9 && (dx * f.x + dz * f.z) / d < cos) continue;
                r.golpear(en, DMG[i()], f.x * 0.6, f.z * 0.6);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 7. bumeran

    static final class Bumeran extends Arma {
        static final double[] DMG = {18, 22, 22, 28, 34, 44}, CD = {50, 46, 44, 40, 36, 30};
        static final int[] CNT = {1, 1, 2, 2, 3, 4}, IDA = {14, 14, 14, 18, 18, 22};
        private static final class Disco { double x, z; Vec3d dir; int t; boolean vuelta; final java.util.Set<Enemigo> golpeados = new java.util.HashSet<>(); }
        private final List<Disco> discos = new ArrayList<>();

        Bumeran(Run r, int nivel) { super(r, "bumeran", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else {
                Enemigo o = r.masCercano(pp, 14, null);
                Vec3d base = o != null ? unitario(o.e.getX() - pp.x, o.e.getZ() - pp.z) : r.mira();
                int n = CNT[i()];
                for (int k = 0; k < n; k++) {
                    Disco d = new Disco(); d.x = pp.x; d.z = pp.z;
                    d.dir = rotar(base, n == 1 ? 0 : (k - (n - 1) / 2.0) * Math.toRadians(35));
                    discos.add(d);
                }
                cd = cdt(CD[i()]);
            }
            ParticleEffect p = part(ParticleTypes.CRIT);
            for (Iterator<Disco> it = discos.iterator(); it.hasNext(); ) {
                Disco d = it.next();
                d.t++;
                if (!d.vuelta && d.t >= IDA[i()]) { d.vuelta = true; d.golpeados.clear(); }
                if (!d.vuelta) { d.x += d.dir.x * (evo() ? 0.95 : 0.75); d.z += d.dir.z * (evo() ? 0.95 : 0.75); }
                else {
                    Vec3d v = unitario(pp.x - d.x, pp.z - d.z);
                    d.x += v.x * (evo() ? 1.3 : 0.95); d.z += v.z * (evo() ? 1.3 : 0.95);
                    if (Math.hypot(pp.x - d.x, pp.z - d.z) < 1.0) { it.remove(); continue; }
                    d.dir = v;
                }
                if (d.t > 80) { it.remove(); continue; }
                double y = pp.y + 0.9;
                r.w.spawnParticles(p, d.x, y, d.z, 3, 0.12, 0.05, 0.12, 0.0);
                r.w.spawnParticles(ParticleTypes.SWEEP_ATTACK, d.x, y, d.z, 1, 0, 0, 0, 0);
                double hit = area(evo() ? 1.6 : 1.0);
                for (Enemigo en : r.enemigos) {
                    if (en.hp <= 0 || d.golpeados.contains(en)) continue;
                    double dx = en.e.getX() - d.x, dz = en.e.getZ() - d.z, rr = hit + en.radio;
                    if (dx * dx + dz * dz > rr * rr) continue;
                    d.golpeados.add(en);
                    r.golpear(en, DMG[i()], d.dir.x * 0.3, d.dir.z * 0.3);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 8. estela

    static final class Estela extends Arma {
        static final double[] DMG = {5, 5, 7, 7, 9, 12}, RAD = {1.4, 1.4, 1.4, 1.9, 2.4, 3.2};
        static final int[] VIDA = {90, 110, 110, 110, 140, 180};
        private static final class Charco { double x, z, radio; int vida; }
        private final List<Charco> charcos = new ArrayList<>();
        private double ux = Double.NaN, uz;

        Estela(Run r, int nivel) { super(r, "estela", nivel); }

        @Override void tick(Vec3d pp) {
            if (Double.isNaN(ux)) { ux = pp.x; uz = pp.z; }
            if (Math.hypot(pp.x - ux, pp.z - uz) >= 1.2 && charcos.size() < 30) {
                Charco c = new Charco(); c.x = pp.x; c.z = pp.z; c.radio = area(RAD[i()]); c.vida = VIDA[i()];
                charcos.add(c);
                ux = pp.x; uz = pp.z;
            }
            ParticleEffect p = part(ParticleTypes.WITCH);
            for (Iterator<Charco> it = charcos.iterator(); it.hasNext(); ) {
                Charco c = it.next();
                if (--c.vida <= 0) { it.remove(); continue; }
                if (c.vida % 2 == 0) r.w.spawnParticles(p, c.x, pp.y + 0.1, c.z, 2, c.radio * 0.5, 0.02, c.radio * 0.5, 0.0);
                if (c.vida % 10 != 0) continue;
                if (evo() && Math.hypot(pp.x - c.x, pp.z - c.z) <= c.radio) { r.curar(0.9); r.w.spawnParticles(ParticleTypes.HEART, pp.x, pp.y + 1.6, pp.z, 1, 0.3, 0.2, 0.3, 0.0); }   // la evolucion te cura mientras estas dentro
                for (Enemigo en : r.enemigos) {
                    if (en.hp <= 0) continue;
                    double dx = en.e.getX() - c.x, dz = en.e.getZ() - c.z, rr = c.radio + en.radio * 0.6;
                    if (dx * dx + dz * dz <= rr * rr) r.golpear(en, DMG[i()], 0, 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 9. minas

    static final class Minas extends Arma {
        static final double[] DMG = {40, 48, 60, 60, 72, 115}, CD = {70, 60, 54, 50, 44, 34}, RAD = {2.2, 2.2, 2.2, 2.8, 2.8, 3.6};
        static final int[] MAX = {1, 3, 3, 4, 5, 7};
        private static final class Mina { double x, z; int armada = 10; }
        private final List<Mina> minas = new ArrayList<>();

        Minas(Run r, int nivel) { super(r, "minas", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else if (minas.size() < MAX[i()]) {
                Mina m = new Mina(); m.x = pp.x; m.z = pp.z;
                minas.add(m);
                cd = cdt(CD[i()]);
            }
            double R = area(RAD[i()]);
            for (Iterator<Mina> it = minas.iterator(); it.hasNext(); ) {
                Mina m = it.next();
                if (m.armada > 0) m.armada--;
                if (r.tick % 5 == 0) r.w.spawnParticles(ROJO, m.x, pp.y + 0.15, m.z, 1, 0.1, 0.0, 0.1, 0.0);
                if (m.armada > 0) continue;
                boolean salta = false;
                for (Enemigo en : r.enemigos) {
                    double dx = en.e.getX() - m.x, dz = en.e.getZ() - m.z, rr = 1.2 + en.radio;
                    if (en.hp > 0 && dx * dx + dz * dz <= rr * rr) { salta = true; break; }
                }
                if (!salta) continue;
                r.w.spawnParticles(ParticleTypes.EXPLOSION, m.x, pp.y + 0.4, m.z, 1, 0, 0, 0, 0);
                r.w.spawnParticles(part(ParticleTypes.CRIT), m.x, pp.y + 0.4, m.z, 18, R * 0.4, 0.3, R * 0.4, 0.1);
                for (Enemigo en : r.enemigos) {
                    if (en.hp <= 0) continue;
                    double dx = en.e.getX() - m.x, dz = en.e.getZ() - m.z, rr = R + en.radio;
                    if (dx * dx + dz * dz > rr * rr) continue;
                    Vec3d f = unitario(dx, dz);
                    r.golpear(en, DMG[i()], f.x * 0.7, f.z * 0.7);
                }
                if (evo()) for (int k = 0; k < 6; k++) r.proyectiles.add(new Proyectil(r, new Vec3d(m.x, pp.y + 0.8, m.z), rotar(new Vec3d(1, 0, 0), k * Math.PI / 3 + r.rnd.nextDouble() * 0.5), 16, part(ParticleTypes.CRIT), 14, 3));
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 10. rayo giratorio

    static final class Rayo extends Arma {
        static final double[] DMG = {8, 10, 12, 15, 18, 40}, LARGO = {5.0, 5.8, 5.8, 6.6, 6.6, 8.5};
        static final int[] CNT = {1, 1, 2, 2, 3, 4};
        private double ang;

        Rayo(Run r, int nivel) { super(r, "rayo", nivel); }

        @Override void tick(Vec3d pp) {
            ang += evo() ? 9.0 : 5.0;
            int n = CNT[i()];
            double L = area(LARGO[i()]);
            ParticleEffect p = part(ParticleTypes.END_ROD);
            boolean daña = r.tick % 6 == 0;
            for (int k = 0; k < n; k++) {
                double a = Math.toRadians(ang + 360.0 / n * k), cx = Math.cos(a), sz = Math.sin(a);
                for (double d = 0.8; d <= L; d += 0.8) r.w.spawnParticles(p, pp.x + cx * d, pp.y + 0.9, pp.z + sz * d, 1, 0.02, 0.02, 0.02, 0.0);
                if (!daña) continue;
                for (Enemigo en : r.enemigos) {
                    if (en.hp <= 0) continue;
                    if (distSegmento(en.e.getX(), en.e.getZ(), pp.x, pp.z, pp.x + cx * L, pp.z + sz * L) <= 0.7 + en.radio)
                        r.golpear(en, DMG[i()], cx * (evo() ? 0.55 : 0.25), sz * (evo() ? 0.55 : 0.25));
                }
            }
        }
    }
}
