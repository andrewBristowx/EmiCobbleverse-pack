package emi.survivors;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static emi.survivors.Armas.ROJO;
import static emi.survivors.Armas.ROSA;
import static emi.survivors.Armas.linea;
import static emi.survivors.Armas.rotar;
import static emi.survivors.Armas.unitario;

/**
 * Los 20 ataques nuevos de la fase 2B (lanzallamas, hidrobomba, rayo solar, hoja navaja, danza petalo, bomba lodo, gran temblor, ventisca, torbellino, trueno, granizo,
 * cometa, garra umbria, agujero negro, centinela michi, aromaterapia, pokebola saltarina, burbujas, latigo cepa y media luna). Igual que los de {@link Armas}: cada uno
 * es una clase con una tabla de 6 valores (los niveles 1 a 5 y, el sexto, la EVOLUCION); se dibujan con particulas y el dano lo aplica {@link Run#golpear}.
 * Lo que dice cada nivel al jugador esta en {@link Catalogo}: si se cambia una tabla hay que cambiar tambien su texto.
 */
final class Armas2 {
    private Armas2() {}

    static Armas.Arma crear(String id, Run r, int nivel) {
        return switch (id) {
            case "lanza" -> new Lanza(r, nivel);
            case "hidro" -> new Hidro(r, nivel);
            case "solar" -> new Solar(r, nivel);
            case "hojas" -> new Hojas(r, nivel);
            case "petalos" -> new Petalos(r, nivel);
            case "lodo" -> new Lodo(r, nivel);
            case "terremoto" -> new Terremoto(r, nivel);
            case "ventisca" -> new Ventisca(r, nivel);
            case "torbellino" -> new Torbellino(r, nivel);
            case "trueno" -> new Trueno(r, nivel);
            case "granizo" -> new Granizo(r, nivel);
            case "cometa" -> new Cometa(r, nivel);
            case "garra" -> new Garra(r, nivel);
            case "agujero" -> new Agujero(r, nivel);
            case "centinela" -> new Centinela(r, nivel);
            case "aroma" -> new Aroma(r, nivel);
            case "pokebola" -> new Pokebola(r, nivel);
            case "burbujas" -> new Burbujas(r, nivel);
            case "latigo" -> new Latigo(r, nivel);
            case "lunar" -> new Lunar(r, nivel);
            default -> throw new IllegalArgumentException("ataque desconocido: " + id);
        };
    }

    static DustParticleEffect polvo(float r, float g, float b, float tam) { return new DustParticleEffect(new Vector3f(r, g, b), tam); }

    // La camara esta muy arriba y las particulas normales se ven diminutas: el «polvo» de colores con tamano grande es lo que se ve bien.
    static final DustParticleEffect VERDE = polvo(0.35f, 0.9f, 0.35f, 1.5f);
    static final DustParticleEffect MORADO = polvo(0.55f, 0.25f, 0.85f, 1.6f);
    static final DustParticleEffect CAFE = polvo(0.45f, 0.30f, 0.12f, 1.8f);
    static final DustParticleEffect NEGRO = polvo(0.05f, 0.0f, 0.12f, 3.0f);
    static final DustParticleEffect NEGRO_FINO = polvo(0.1f, 0.1f, 0.1f, 1.8f);
    static final DustParticleEffect DORADO = polvo(1f, 0.85f, 0.25f, 1.7f);
    static final DustParticleEffect DORADO_GRANDE = polvo(1f, 0.85f, 0.25f, 2.4f);
    static final DustParticleEffect BLANCO = polvo(1f, 1f, 1f, 1.6f);
    static final DustParticleEffect AZUL = polvo(0.45f, 0.75f, 1f, 1.4f);
    static final DustParticleEffect AGUA = polvo(0.2f, 0.5f, 1f, 1.6f);
    static final DustParticleEffect NARANJA = polvo(1f, 0.5f, 0.1f, 1.6f);
    static final DustParticleEffect AZUL_FUEGO = polvo(0.25f, 0.75f, 1f, 1.6f);
    static final DustParticleEffect AMARILLO = polvo(1f, 0.95f, 0.3f, 1.8f);
    static final DustParticleEffect ROSA_GRANDE = polvo(1f, 0.55f, 0.85f, 1.8f);
    static final DustParticleEffect ROJO_GRANDE = polvo(1f, 0.15f, 0.1f, 1.8f);

    // ------------------------------------------------------------------------------------------------ utilidades

    /** Los enemigos vivos a menos de {@code radio} del punto (contando su tamano). */
    static List<Enemigo> cerca(Run r, double x, double z, double radio) {
        List<Enemigo> l = new ArrayList<>();
        for (Enemigo en : r.enemigos) {
            if (en.hp <= 0) continue;
            double dx = en.e.getX() - x, dz = en.e.getZ() - z, rr = radio + en.radio;
            if (dx * dx + dz * dz <= rr * rr) l.add(en);
        }
        return l;
    }

    /** Un enemigo vivo al azar a menos de {@code alcance} del jugador (null si no hay). */
    static Enemigo azar(Run r, Vec3d pp, double alcance) {
        List<Enemigo> l = cerca(r, pp.x, pp.z, alcance);
        return l.isEmpty() ? null : l.get(r.rnd.nextInt(l.size()));
    }

    /** El enemigo (de unos cuantos al azar) que tiene mas vecinos a menos de 2,8 bloques: donde mas rinde un ataque de area. */
    static Enemigo masDenso(Run r, Vec3d pp, double alcance) {
        List<Enemigo> cand = cerca(r, pp.x, pp.z, alcance);
        if (cand.isEmpty()) return null;
        Enemigo mejor = null; int mejorN = -1;
        int muestras = Math.min(24, cand.size());
        for (int k = 0; k < muestras; k++) {
            Enemigo c = cand.size() <= 24 ? cand.get(k) : cand.get(r.rnd.nextInt(cand.size()));
            int n = 0;
            for (Enemigo o : cand) {
                double dx = o.e.getX() - c.e.getX(), dz = o.e.getZ() - c.e.getZ();
                if (dx * dx + dz * dz < 2.8 * 2.8) n++;
            }
            if (n > mejorN) { mejorN = n; mejor = c; }
        }
        return mejor;
    }

    /** ¿Se dibuja esta particula «de adorno»? Con probabilidad {@code p}, que baja si el jugador lleva muchos ataques (ver {@link Run#densidad}). */
    static boolean ver(Run r, double p) { return r.rnd.nextDouble() < p * r.densidad(); }

    /** Un anillo de particulas (para avisos y explosiones). */
    static void anillo(Run r, ParticleEffect p, double x, double y, double z, double radio, int pts) {
        for (int k = 0; k < pts; k++) {
            double a = k * Math.PI * 2 / pts;
            r.w.spawnParticles(p, x + Math.cos(a) * radio, y, z + Math.sin(a) * radio, 1, 0, 0, 0, 0);
        }
    }

    static void sonido(Run r, double x, double y, double z, SoundEvent s, float vol, float tono) {
        r.w.playSound(null, x, y, z, s, SoundCategory.PLAYERS, vol, tono);
    }

    /** Diferencia de angulos (rad) envuelta a -PI..PI. */
    static double difAngulo(double a) {
        a = (a + Math.PI) % (Math.PI * 2);
        if (a < 0) a += Math.PI * 2;
        return a - Math.PI;
    }

    // ------------------------------------------------------------------------------------------------ 11. lanzallamas

    static final class Lanza extends Armas.Arma {
        static final double[] DMG = {10, 12, 13, 15, 18, 34}, RANGO = {5.0, 5.0, 5.8, 6.2, 6.8, 8.5}, ANG = {26, 28, 30, 34, 38, 48};
        static final int[] DUR = {20, 20, 24, 24, 28, 36}, ESPERA = {40, 38, 36, 34, 30, 18};
        private int activo;
        private Vec3d dir = new Vec3d(0, 0, 1);

        Lanza(Run r, int nivel) { super(r, "lanza", nivel); }

        @Override void tick(Vec3d pp) {
            if (activo > 0) {
                activo--;
                double L = area(RANGO[i()]);
                if (r.tick % 4 == 0) { Enemigo o = r.masCercano(pp, L + 2.5, null); if (o != null) dir = unitario(o.e.getX() - pp.x, o.e.getZ() - pp.z); }
                llamas(pp, L);
                if (activo % 4 == 0) dano(pp, L);
                if (activo == 0) cd = cdt(ESPERA[i()]);
                return;
            }
            if (cd > 0) { cd--; return; }
            Enemigo o = r.masCercano(pp, area(RANGO[i()]) + 2.5, null);
            if (o == null) return;
            dir = unitario(o.e.getX() - pp.x, o.e.getZ() - pp.z);
            activo = DUR[i()];
        }

        private void llamas(Vec3d pp, double L) {
            double ang = Math.toRadians(ANG[i()]);
            ParticleEffect fuego = part(evo() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME);
            ParticleEffect polvo = evo() ? AZUL_FUEGO : NARANJA;
            int n = evo() ? 14 : 10;
            for (int k = 0; k < n; k++) {
                double t = 0.8 + r.rnd.nextDouble() * (L - 0.8), off = (r.rnd.nextDouble() * 2 - 1) * ang * Math.min(1.0, 0.3 + t / L);
                Vec3d d = rotar(dir, off);
                double x = pp.x + d.x * t, y = pp.y + 0.7 + r.rnd.nextDouble() * 0.5, z = pp.z + d.z * t;
                r.w.spawnParticles(k % 2 == 0 ? fuego : polvo, x, y, z, 1, 0.08, 0.08, 0.08, 0.01);
                if (k == 0) r.w.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.4, z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }

        private void dano(Vec3d pp, double L) {
            double cos = Math.cos(Math.toRadians(ANG[i()]));
            for (Enemigo en : r.enemigos) {
                if (en.hp <= 0) continue;
                double dx = en.e.getX() - pp.x, dz = en.e.getZ() - pp.z, d = Math.sqrt(dx * dx + dz * dz);
                if (d > L + en.radio) continue;
                if (d > 1.0 && (dx * dir.x + dz * dir.z) / d < cos) continue;
                r.golpear(en, DMG[i()], dir.x * 0.1, dir.z * 0.1);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 12. hidrobomba

    static final class Hidro extends Armas.Arma {
        static final double[] DMG = {24, 29, 34, 34, 41, 55}, ANCHO = {1.3, 1.3, 1.7, 1.7, 2.1, 3.0}, LARGO = {10, 10, 11, 12, 13, 16};
        static final int[] CD = {72, 68, 64, 60, 54, 42}, DIRS = {1, 1, 1, 2, 2, 4};
        private static final class Ola { double x, z, dx, dz, recorrido, largo, ancho, dmg; final Set<Enemigo> golpeados = new HashSet<>(); }
        private final List<Ola> olas = new ArrayList<>();

        Hidro(Run r, int nivel) { super(r, "hidro", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else {
                Enemigo o = r.masCercano(pp, 12, null);
                if (o != null) {
                    Vec3d base = unitario(o.e.getX() - pp.x, o.e.getZ() - pp.z);
                    int n = DIRS[i()];
                    for (int k = 0; k < n; k++) {
                        Vec3d d = rotar(base, Math.PI * 2 * k / n);
                        Ola ola = new Ola(); ola.x = pp.x; ola.z = pp.z; ola.dx = d.x; ola.dz = d.z; ola.largo = LARGO[i()]; ola.ancho = area(ANCHO[i()]); ola.dmg = DMG[i()];
                        olas.add(ola);
                    }
                    cd = cdt(CD[i()]);
                }
            }
            ParticleEffect agua = part(ParticleTypes.SPLASH);
            for (Iterator<Ola> it = olas.iterator(); it.hasNext(); ) {
                Ola o = it.next();
                o.x += o.dx * 0.6; o.z += o.dz * 0.6; o.recorrido += 0.6;
                double px = -o.dz, pz = o.dx;
                for (double j = -o.ancho; j <= o.ancho + 0.01; j += Math.max(0.7, o.ancho / 3.5)) {            // el frente de la ola: una pared de agua (polvo azul) con espuma
                    double fx = o.x + px * j, fz = o.z + pz * j;
                    r.w.spawnParticles(AGUA, fx, pp.y + 0.6, fz, 2, 0.25, 0.25, 0.25, 0.0);
                    if (ver(r, 0.8)) r.w.spawnParticles(agua, fx, pp.y + 0.5, fz, 1, 0.3, 0.35, 0.3, 0.0);
                }
                for (Enemigo en : r.enemigos) {
                    if (en.hp <= 0 || o.golpeados.contains(en)) continue;
                    double ex = en.e.getX() - o.x, ez = en.e.getZ() - o.z;
                    double along = ex * o.dx + ez * o.dz, across = ex * px + ez * pz;
                    if (Math.abs(along) > 0.9 + en.radio || Math.abs(across) > o.ancho + en.radio) continue;
                    o.golpeados.add(en);
                    r.golpear(en, o.dmg, o.dx * 0.7, o.dz * 0.7);
                    en.ralentizar(30, 0.6);
                }
                if (o.recorrido >= o.largo) it.remove();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 13. rayo solar

    static final class Solar extends Armas.Arma {
        static final double[] DMG = {50, 60, 70, 70, 88, 130}, ANCHO = {0.9, 1.0, 1.1, 1.3, 1.5, 2.2}, LARGO = {11, 12, 13, 14, 15, 18};
        static final int[] CD = {120, 110, 100, 92, 84, 66}, RAYOS = {1, 1, 2, 2, 3, 4};
        private static final int CARGA = 16;
        private static final class Haz { Vec3d dir; int t = CARGA, brillo; }
        private final List<Haz> haces = new ArrayList<>();

        Solar(Run r, int nivel) { super(r, "solar", nivel); }

        @Override void tick(Vec3d pp) {
            double L = area(LARGO[i()]), W = area(ANCHO[i()]);
            if (cd > 0) cd--;
            else if (haces.isEmpty()) {
                Enemigo o = r.masCercano(pp, L, null);
                if (o != null) {
                    Vec3d base = unitario(o.e.getX() - pp.x, o.e.getZ() - pp.z);
                    int n = RAYOS[i()];
                    for (int k = 0; k < n; k++) { Haz h = new Haz(); h.dir = rotar(base, Math.PI * 2 * k / n); haces.add(h); }
                    cd = cdt(CD[i()]);
                }
            }
            for (Iterator<Haz> it = haces.iterator(); it.hasNext(); ) {
                Haz h = it.next();
                if (h.t > 0) {
                    h.t--;
                    if (h.t % 2 == 0) linea(r, ROJO_GRANDE, pp.add(0, 0.9, 0), new Vec3d(pp.x + h.dir.x * L, pp.y + 0.9, pp.z + h.dir.z * L), 1.0, 24);
                    if (h.t == 0) {
                        disparar(pp, h, L, W);
                        h.brillo = 6;
                    }
                } else {
                    h.brillo--;
                    brillar(pp, h, L, W);
                    if (h.brillo <= 0) it.remove();
                }
            }
        }

        private void disparar(Vec3d pp, Haz h, double L, double W) {
            double px = -h.dir.z, pz = h.dir.x;
            for (Enemigo en : r.enemigos) {
                if (en.hp <= 0) continue;
                double ex = en.e.getX() - pp.x, ez = en.e.getZ() - pp.z;
                double along = ex * h.dir.x + ez * h.dir.z, across = ex * px + ez * pz;
                if (along < -0.5 || along > L + en.radio || Math.abs(across) > W + en.radio) continue;
                r.golpear(en, DMG[i()], h.dir.x * 0.5, h.dir.z * 0.5);
            }
            sonido(r, pp.x, pp.y, pp.z, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.5f, 1.6f);
        }

        private void brillar(Vec3d pp, Haz h, double L, double W) {
            double px = -h.dir.z, pz = h.dir.x;
            ParticleEffect luz = part(ParticleTypes.END_ROD);
            for (double d = 0.5; d <= L; d += 0.9) {                                   // tres hilos de luz (el del centro blanco y dos dorados a los lados)
                double cx = pp.x + h.dir.x * d, cz = pp.z + h.dir.z * d;
                r.w.spawnParticles(luz, cx, pp.y + 0.9, cz, 2, 0.15, 0.1, 0.15, 0.0);
                r.w.spawnParticles(DORADO_GRANDE, cx + px * W * 0.6, pp.y + 0.9, cz + pz * W * 0.6, 1, 0.12, 0.1, 0.12, 0.0);
                r.w.spawnParticles(DORADO_GRANDE, cx - px * W * 0.6, pp.y + 0.9, cz - pz * W * 0.6, 1, 0.12, 0.1, 0.12, 0.0);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 14. hoja navaja

    static final class Hojas extends Armas.Arma {
        static final double[] DMG = {14, 14, 14, 16, 18, 22};
        static final int[] CD = {34, 34, 32, 30, 26, 20}, CNT = {3, 4, 4, 5, 6, 8}, PERF = {1, 1, 2, 2, 3, 4};
        private static final class Hoja { double x, y, z, dx, dz; Enemigo obj; int vida = 40, perf; final Set<Enemigo> golpeados = new HashSet<>(); }
        private final List<Hoja> hojas = new ArrayList<>();

        Hojas(Run r, int nivel) { super(r, "hojas", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else if (r.masCercano(pp, 13, null) != null) {
                for (int k = 0; k < CNT[i()]; k++) {
                    Hoja h = new Hoja(); h.x = pp.x; h.y = pp.y + 0.9; h.z = pp.z; h.obj = azar(r, pp, 13); h.perf = PERF[i()];
                    double a = r.rnd.nextDouble() * Math.PI * 2; h.dx = Math.cos(a); h.dz = Math.sin(a);
                    hojas.add(h);
                }
                cd = cdt(CD[i()]);
            }
            for (Iterator<Hoja> it = hojas.iterator(); it.hasNext(); ) {
                Hoja h = it.next();
                if (--h.vida <= 0) { it.remove(); continue; }
                if (h.obj == null || h.obj.hp <= 0 || h.golpeados.contains(h.obj)) h.obj = r.masCercano(new Vec3d(h.x, h.y, h.z), 11, h.golpeados);
                if (h.obj != null) {                                                  // gira hacia su objetivo hasta ~23 grados por tick
                    double ang = Math.atan2(h.dz, h.dx), quiere = Math.atan2(h.obj.e.getZ() - h.z, h.obj.e.getX() - h.x);
                    ang += Math.max(-0.4, Math.min(0.4, difAngulo(quiere - ang)));
                    h.dx = Math.cos(ang); h.dz = Math.sin(ang);
                }
                h.x += h.dx * 0.75; h.z += h.dz * 0.75;
                for (int k = 0; k < 3; k++) r.w.spawnParticles(VERDE, h.x - h.dx * k * 0.3, h.y, h.z - h.dz * k * 0.3, 1, 0.02, 0.02, 0.02, 0.0);     // la hoja: una cuchilla verde con cola
                if (h.vida % 2 == 0) r.w.spawnParticles(ParticleTypes.CRIT, h.x, h.y, h.z, 1, 0.05, 0.05, 0.05, 0.0);
                boolean fin = false;
                for (Enemigo en : r.enemigos) {
                    if (en.hp <= 0 || h.golpeados.contains(en)) continue;
                    double dx = en.e.getX() - h.x, dz = en.e.getZ() - h.z, rr = 0.55 + en.radio;
                    if (dx * dx + dz * dz > rr * rr) continue;
                    h.golpeados.add(en);
                    r.golpear(en, DMG[i()], h.dx * 0.3, h.dz * 0.3);
                    if (--h.perf <= 0) { fin = true; break; }
                }
                if (fin) it.remove();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 15. danza petalo

    static final class Petalos extends Armas.Arma {
        static final double[] DMG = {11, 13, 13, 15, 17, 22};
        static final int[] CD = {44, 40, 38, 34, 30, 22}, BRAZOS = {7, 8, 8, 10, 12, 18}, PERF = {1, 1, 2, 2, 2, 3};
        private static final class Petalo { double x, z, ang, giro; int vida = 26, perf; final Set<Enemigo> golpeados = new HashSet<>(); }
        private final List<Petalo> petalos = new ArrayList<>();
        private double rot;

        Petalos(Run r, int nivel) { super(r, "petalos", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else if (r.masCercano(pp, 13, null) != null) {
                rot += 17;
                int n = BRAZOS[i()];
                for (int k = 0; k < n; k++) {
                    Petalo p = new Petalo(); p.x = pp.x; p.z = pp.z; p.ang = Math.toRadians(rot + 360.0 / n * k);
                    p.giro = Math.toRadians(2.6) * (evo() && k % 2 == 1 ? -1 : 1);       // la evolucion: espiral doble (la mitad gira al reves)
                    p.perf = PERF[i()];
                    petalos.add(p);
                }
                cd = cdt(CD[i()]);
            }
            for (Iterator<Petalo> it = petalos.iterator(); it.hasNext(); ) {
                Petalo p = it.next();
                if (--p.vida <= 0) { it.remove(); continue; }
                p.ang += p.giro;
                double cx = Math.cos(p.ang), cz = Math.sin(p.ang);
                p.x += cx * 0.62; p.z += cz * 0.62;
                if (ver(r, 1.0)) r.w.spawnParticles(ParticleTypes.CHERRY_LEAVES, p.x, pp.y + 0.9, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                if (ver(r, 0.8)) r.w.spawnParticles(ROSA_GRANDE, p.x, pp.y + 0.9, p.z, 1, 0.03, 0.03, 0.03, 0.0);
                boolean fin = false;
                for (Enemigo en : r.enemigos) {
                    if (en.hp <= 0 || p.golpeados.contains(en)) continue;
                    double dx = en.e.getX() - p.x, dz = en.e.getZ() - p.z, rr = 0.6 + en.radio;
                    if (dx * dx + dz * dz > rr * rr) continue;
                    p.golpeados.add(en);
                    r.golpear(en, DMG[i()], cx * 0.3, cz * 0.3);
                    if (--p.perf <= 0) { fin = true; break; }
                }
                if (fin) it.remove();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 16. bomba lodo

    static final class Lodo extends Armas.Arma {
        static final double[] DMG = {24, 28, 32, 32, 38, 56}, RAD = {2.0, 2.2, 2.4, 2.6, 2.8, 3.8};
        static final int[] CD = {88, 82, 76, 70, 62, 48}, VIDA = {80, 100, 100, 120, 140, 200}, CNT = {1, 1, 2, 2, 3, 4};
        private static final int VUELO = 18;
        private static final class Bomba { double x0, z0, x1, z1; int t; }
        private static final class Charco { double x, z, radio; int vida; }
        private final List<Bomba> bombas = new ArrayList<>();
        private final List<Charco> charcos = new ArrayList<>();

        Lodo(Run r, int nivel) { super(r, "lodo", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else if (r.masCercano(pp, 13, null) != null) {
                for (int k = 0; k < CNT[i()]; k++) {
                    Enemigo o = k == 0 ? masDenso(r, pp, 13) : azar(r, pp, 13);
                    if (o == null) break;
                    Bomba b = new Bomba(); b.x0 = pp.x; b.z0 = pp.z; b.x1 = o.e.getX() + (r.rnd.nextDouble() - 0.5); b.z1 = o.e.getZ() + (r.rnd.nextDouble() - 0.5);
                    bombas.add(b);
                }
                cd = cdt(CD[i()]);
            }
            double R = area(RAD[i()]);
            for (Iterator<Bomba> it = bombas.iterator(); it.hasNext(); ) {
                Bomba b = it.next();
                b.t++;
                double f = Math.min(1.0, b.t / (double) VUELO);
                double x = b.x0 + (b.x1 - b.x0) * f, z = b.z0 + (b.z1 - b.z0) * f, y = pp.y + 0.9 + Math.sin(Math.PI * f) * 4.5;
                r.w.spawnParticles(CAFE, x, y, z, 4, 0.18, 0.18, 0.18, 0.0);
                r.w.spawnParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
                if (b.t < VUELO) continue;
                r.w.spawnParticles(ParticleTypes.EXPLOSION, b.x1, pp.y + 0.4, b.z1, 1, 0, 0, 0, 0);
                r.w.spawnParticles(CAFE, b.x1, pp.y + 0.5, b.z1, 30, R * 0.45, 0.3, R * 0.45, 0.02);
                for (Enemigo en : cerca(r, b.x1, b.z1, R)) {
                    Vec3d d = unitario(en.e.getX() - b.x1, en.e.getZ() - b.z1);
                    r.golpear(en, DMG[i()], d.x * 0.6, d.z * 0.6);
                    en.ralentizar(40, 0.5);
                }
                Charco c = new Charco(); c.x = b.x1; c.z = b.z1; c.radio = R * 1.1; c.vida = VIDA[i()];
                charcos.add(c);
                it.remove();
            }
            for (Iterator<Charco> it = charcos.iterator(); it.hasNext(); ) {
                Charco c = it.next();
                if (--c.vida <= 0) { it.remove(); continue; }
                if (c.vida % 3 == 0) r.w.spawnParticles(CAFE, c.x, pp.y + 0.1, c.z, 6, c.radio * 0.55, 0.02, c.radio * 0.55, 0.0);
                if (c.vida % 20 == 0) anillo(r, CAFE, c.x, pp.y + 0.12, c.z, c.radio, 14);
                if (c.vida % 5 != 0) continue;
                for (Enemigo en : cerca(r, c.x, c.z, c.radio)) {
                    en.ralentizar(12, 0.5);
                    if (c.vida % 10 == 0) r.golpear(en, DMG[i()] * 0.12, 0, 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 17. gran temblor

    static final class Terremoto extends Armas.Arma {
        static final double[] DMG = {40, 48, 58, 58, 72, 110}, R = {5.0, 5.5, 6.0, 6.5, 7.0, 9.5};
        static final int[] CD = {170, 158, 146, 136, 124, 96}, ATURDE = {30, 36, 40, 46, 52, 76};
        private static final int AVISO = 20;
        private int aviso;
        private static final BlockStateParticleEffect TIERRA = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DIRT.getDefaultState());

        Terremoto(Run r, int nivel) { super(r, "terremoto", nivel); }

        @Override void tick(Vec3d pp) {
            double Rr = area(R[i()]);
            if (aviso > 0) {
                aviso--;
                if (aviso > 0) {
                    if (aviso % 2 == 0) anillo(r, ROJO_GRANDE, pp.x, pp.y + 0.15, pp.z, Rr, (int) (Rr * 7));
                    if (aviso % 4 == 0) anillo(r, ROJO_GRANDE, pp.x, pp.y + 0.15, pp.z, Rr * (aviso / (double) AVISO), (int) (Rr * 4));
                    r.w.spawnParticles(ParticleTypes.SMOKE, pp.x, pp.y + 0.3, pp.z, 2, Rr * 0.3, 0.05, Rr * 0.3, 0.0);
                } else sacudir(pp, Rr);
                return;
            }
            if (cd > 0) { cd--; return; }
            if (r.masCercano(pp, Rr + 1.0, null) == null) return;
            aviso = AVISO;
            cd = cdt(CD[i()]);
        }

        private void sacudir(Vec3d pp, double Rr) {
            r.w.spawnParticles(ParticleTypes.EXPLOSION, pp.x, pp.y + 0.4, pp.z, 1, 0, 0, 0, 0);
            for (int k = 0; k < 18; k++) {
                double a = k * Math.PI * 2 / 18;
                r.w.spawnParticles(ParticleTypes.EXPLOSION, pp.x + Math.cos(a) * Rr * 0.5, pp.y + 0.3, pp.z + Math.sin(a) * Rr * 0.5, 1, 0, 0, 0, 0);
                r.w.spawnParticles(ParticleTypes.EXPLOSION, pp.x + Math.cos(a) * Rr * 0.9, pp.y + 0.3, pp.z + Math.sin(a) * Rr * 0.9, 1, 0, 0, 0, 0);
            }
            r.w.spawnParticles(TIERRA, pp.x, pp.y + 0.3, pp.z, (int) (90 * r.densidad()), Rr * 0.5, 0.2, Rr * 0.5, 0.2);
            sonido(r, pp.x, pp.y, pp.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.6f, 0.6f);
            for (Enemigo en : cerca(r, pp.x, pp.z, Rr)) {
                Vec3d f = unitario(en.e.getX() - pp.x, en.e.getZ() - pp.z);
                r.golpear(en, DMG[i()], f.x * 1.0, f.z * 1.0);
                en.aturdir(ATURDE[i()]);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 18. ventisca

    static final class Ventisca extends Armas.Arma {
        static final double[] DMG = {6, 7, 8, 10, 12, 30}, R = {3.2, 3.5, 3.9, 4.3, 4.8, 6.5}, LENTO = {0.7, 0.65, 0.6, 0.55, 0.5, 0.35};
        private double ang;

        Ventisca(Run r, int nivel) { super(r, "ventisca", nivel); }

        @Override void tick(Vec3d pp) {
            ang += 14;
            double Rr = area(R[i()]);
            for (int k = 0; k < 3; k++) {                                              // el borde: copos de polvo azul que giran
                double a = Math.toRadians(ang + k * 120);
                r.w.spawnParticles(AZUL, pp.x + Math.cos(a) * Rr, pp.y + 0.6, pp.z + Math.sin(a) * Rr, 2, 0.1, 0.2, 0.1, 0.0);
                r.w.spawnParticles(ParticleTypes.SNOWFLAKE, pp.x + Math.cos(a + 0.2) * Rr, pp.y + 0.6, pp.z + Math.sin(a + 0.2) * Rr, 1, 0.1, 0.2, 0.1, 0.0);
            }
            for (int k = 0; k < (evo() ? 7 : 5); k++) {                                // el interior: copos y niebla al azar
                if (!ver(r, 1.0)) continue;
                double rr = 0.4 + r.rnd.nextDouble() * (Rr - 0.4), a = r.rnd.nextDouble() * Math.PI * 2;
                ParticleEffect e = k % 4 == 0 ? ParticleTypes.CLOUD : k % 4 == 1 ? BLANCO : k % 4 == 2 ? AZUL : ParticleTypes.SNOWFLAKE;
                r.w.spawnParticles(e, pp.x + Math.cos(a) * rr, pp.y + 0.3 + r.rnd.nextDouble() * 1.4, pp.z + Math.sin(a) * rr, 1, 0.05, 0.05, 0.05, 0.0);
            }
            if (r.tick % 5 != 0) return;
            boolean dana = r.tick % 10 == 0;
            for (Enemigo en : cerca(r, pp.x, pp.z, Rr)) {
                en.ralentizar(14, LENTO[i()]);
                if (dana) r.golpear(en, DMG[i()], 0, 0);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 19. torbellino

    static final class Torbellino extends Armas.Arma {
        static final double[] DMG = {5, 6, 7, 8, 10, 15}, PR = {3.0, 3.3, 3.6, 4.0, 4.4, 6.0};
        static final int[] VIDA = {100, 110, 120, 130, 140, 200}, CD = {150, 140, 130, 120, 110, 90}, CNT = {1, 1, 1, 2, 2, 3};
        private static final class Tornado { double x, z, ang; int vida; Vec3d meta; }
        private final List<Tornado> tornados = new ArrayList<>();

        Torbellino(Run r, int nivel) { super(r, "torbellino", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else if (r.masCercano(pp, 14, null) != null) {
                for (int k = 0; k < CNT[i()]; k++) {
                    Tornado t = new Tornado(); t.x = pp.x + (r.rnd.nextDouble() - 0.5) * 2; t.z = pp.z + (r.rnd.nextDouble() - 0.5) * 2; t.vida = VIDA[i()];
                    tornados.add(t);
                }
                cd = cdt(CD[i()]);
            }
            double R = area(PR[i()]);
            for (Iterator<Tornado> it = tornados.iterator(); it.hasNext(); ) {
                Tornado t = it.next();
                if (--t.vida <= 0) { it.remove(); continue; }
                t.ang += 0.55;
                if (t.vida % 10 == 0 || t.meta == null) {                                // va hacia donde mas enemigos hay
                    Enemigo o = masDenso(r, new Vec3d(t.x, pp.y, t.z), 16);
                    t.meta = o == null ? null : new Vec3d(o.e.getX(), pp.y, o.e.getZ());
                }
                if (t.meta != null) {
                    Vec3d d = unitario(t.meta.x - t.x, t.meta.z - t.z);
                    t.x += d.x * 0.14; t.z += d.z * 0.14;
                }
                double esc = R / 3.5;
                for (int h = 0; h < 5; h++) {
                    double a = t.ang + h * 1.5, rad = (0.4 + h * 0.5) * esc;
                    r.w.spawnParticles(ParticleTypes.CLOUD, t.x + Math.cos(a) * rad, pp.y + 0.2 + h * 0.7, t.z + Math.sin(a) * rad, 1, 0.03, 0.03, 0.03, 0.0);
                    if (ver(r, 0.8)) r.w.spawnParticles(BLANCO, t.x + Math.cos(a + 3.1) * rad, pp.y + 0.2 + h * 0.7, t.z + Math.sin(a + 3.1) * rad, 1, 0.03, 0.03, 0.03, 0.0);
                }
                if (t.vida % 5 != 0) continue;
                boolean dana = t.vida % 10 == 0;
                for (Enemigo en : cerca(r, t.x, t.z, R)) {
                    double dx = t.x - en.e.getX(), dz = t.z - en.e.getZ();
                    if (dx * dx + dz * dz > 0.8 * 0.8) r.impulso(en, dx, dz, 0.22);
                    if (dana) r.golpear(en, DMG[i()], 0, 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 20. trueno

    static final class Trueno extends Armas.Arma {
        static final double[] DMG = {26, 30, 34, 34, 40, 56}, R = {1.6, 1.6, 1.9, 1.9, 2.2, 2.9};
        static final int[] CD = {54, 50, 46, 42, 38, 28}, CNT = {1, 2, 2, 3, 4, 6};

        Trueno(Run r, int nivel) { super(r, "trueno", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) { cd--; return; }
            List<Enemigo> l = cerca(r, pp.x, pp.z, 12);
            if (l.isEmpty()) return;
            double Rr = area(R[i()]);
            int n = Math.min(CNT[i()], l.size());
            boolean sonar = true;
            for (int k = 0; k < n; k++) {
                Enemigo o = l.remove(r.rnd.nextInt(l.size()));
                double x = o.e.getX(), y = o.e.getY(), z = o.e.getZ();
                for (double h = 0; h <= 9; h += 0.8) {
                    double sw = 0.45 * (h / 9.0) * (r.rnd.nextDouble() - 0.5);
                    r.w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x + sw, y + h, z + sw, 2, 0.08, 0.08, 0.08, 0.0);
                    r.w.spawnParticles(AMARILLO, x + sw, y + h, z + sw, 1, 0.06, 0.06, 0.06, 0.0);
                }
                r.w.spawnParticles(ParticleTypes.EXPLOSION, x, y + 0.4, z, 1, 0, 0, 0, 0);
                r.w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.5, z, 24, Rr * 0.5, 0.3, Rr * 0.5, 0.25);
                anillo(r, AMARILLO, x, y + 0.2, z, Rr, 10);
                for (Enemigo en : cerca(r, x, z, Rr)) {
                    r.golpear(en, DMG[i()], 0, 0);
                    en.aturdir(evo() ? 30 : 14);
                }
                if (sonar) { sonido(r, x, y, z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.25f, 1.4f); sonar = false; }
            }
            cd = cdt(CD[i()]);
        }
    }

    // ------------------------------------------------------------------------------------------------ 21. granizo

    static final class Granizo extends Armas.Arma {
        static final double[] DMG = {7, 8, 10, 10, 12, 18}, R = {2.6, 2.8, 3.0, 3.4, 3.8, 5.0};
        static final int[] VIDA = {50, 60, 60, 70, 80, 100}, CD = {72, 66, 62, 58, 52, 40}, CNT = {1, 1, 2, 2, 3, 4};
        private static final class Zona { double x, z, radio; int vida; }
        private final List<Zona> zonas = new ArrayList<>();
        private static final ItemStackParticleEffect HIELO = new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.ICE));

        Granizo(Run r, int nivel) { super(r, "granizo", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else if (r.masCercano(pp, 14, null) != null) {
                for (int k = 0; k < CNT[i()]; k++) {
                    Enemigo o = k == 0 ? masDenso(r, pp, 14) : azar(r, pp, 14);
                    if (o == null) break;
                    Zona z = new Zona(); z.x = o.e.getX(); z.z = o.e.getZ(); z.radio = area(R[i()]); z.vida = VIDA[i()];
                    zonas.add(z);
                }
                cd = cdt(CD[i()]);
            }
            for (Iterator<Zona> it = zonas.iterator(); it.hasNext(); ) {
                Zona z = it.next();
                if (--z.vida <= 0) { it.remove(); continue; }
                for (int k = 0; k < 3; k++) {                                        // trozos de hielo que caen desde arriba
                    double a = r.rnd.nextDouble() * Math.PI * 2, rr = Math.sqrt(r.rnd.nextDouble()) * z.radio;
                    r.w.spawnParticles(HIELO, z.x + Math.cos(a) * rr, pp.y + 4.5, z.z + Math.sin(a) * rr, 1, 0.05, 0.1, 0.05, 0.0);
                }
                if (z.vida % 3 == 0) r.w.spawnParticles(ParticleTypes.SNOWFLAKE, z.x, pp.y + 3.0, z.z, 2, z.radio * 0.5, 0.3, z.radio * 0.5, 0.0);
                if (z.vida % 12 == 0) anillo(r, AZUL, z.x, pp.y + 0.15, z.z, z.radio, 16);
                if (z.vida % 8 != 0) continue;
                for (Enemigo en : cerca(r, z.x, z.z, z.radio)) {
                    r.golpear(en, DMG[i()], 0, 0);
                    en.ralentizar(20, 0.75);
                }
                r.w.spawnParticles(HIELO, z.x, pp.y + 0.4, z.z, 8, z.radio * 0.4, 0.2, z.radio * 0.4, 0.1);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 22. cometa

    static final class Cometa extends Armas.Arma {
        static final double[] DMG = {16, 19, 22, 22, 26, 36}, TAM = {1.5, 1.6, 1.8, 1.9, 2.1, 3.0};
        static final int[] CD = {100, 92, 86, 80, 72, 56}, CNT = {1, 1, 1, 2, 2, 3};
        private static final int VIDA = 36;
        private static final class Astro { double x, z; Vec3d dir; int t; }
        private final List<Astro> astros = new ArrayList<>();

        Cometa(Run r, int nivel) { super(r, "cometa", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else {
                Enemigo o = r.masCercano(pp, 14, null);
                if (o != null) {
                    Vec3d base = unitario(o.e.getX() - pp.x, o.e.getZ() - pp.z);
                    int n = CNT[i()];
                    for (int k = 0; k < n; k++) {
                        Astro a = new Astro(); a.x = pp.x; a.z = pp.z; a.dir = rotar(base, n == 1 ? 0 : (k - (n - 1) / 2.0) * Math.toRadians(32));
                        astros.add(a);
                    }
                    cd = cdt(CD[i()]);
                }
            }
            double T = area(TAM[i()]);
            for (Iterator<Astro> it = astros.iterator(); it.hasNext(); ) {
                Astro a = it.next();
                a.t++;
                a.x += a.dir.x * 0.55; a.z += a.dir.z * 0.55;
                double y = pp.y + 0.9;
                r.w.spawnParticles(part(ParticleTypes.END_ROD), a.x, y, a.z, 6, T * 0.35, T * 0.3, T * 0.35, 0.0);
                r.w.spawnParticles(DORADO_GRANDE, a.x, y, a.z, 5, T * 0.3, T * 0.2, T * 0.3, 0.0);
                r.w.spawnParticles(ParticleTypes.FIREWORK, a.x - a.dir.x * 0.8, y, a.z - a.dir.z * 0.8, 3, 0.25, 0.2, 0.25, 0.03);
                r.w.spawnParticles(DORADO, a.x - a.dir.x * 1.8, y, a.z - a.dir.z * 1.8, 4, 0.3, 0.2, 0.3, 0.0);
                if (a.t % 2 == 0) anillo(r, DORADO, a.x, y, a.z, T, 8);
                if (a.t % 4 == 0) for (Enemigo en : cerca(r, a.x, a.z, T)) r.golpear(en, DMG[i()], a.dir.x * 0.25, a.dir.z * 0.25);
                if (a.t >= VIDA) it.remove();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 23. garra umbria

    static final class Garra extends Armas.Arma {
        static final double[] DMG = {16, 19, 19, 22, 26, 46}, L = {2.8, 2.8, 3.2, 3.4, 3.6, 4.8};
        static final int[] CD = {54, 50, 46, 42, 38, 30}, N = {3, 4, 5, 5, 6, 9};
        private int pendientes, espera;

        Garra(Run r, int nivel) { super(r, "garra", nivel); }

        @Override void tick(Vec3d pp) {
            if (pendientes > 0) {
                if (--espera <= 0) { zarpazo(pp); pendientes--; espera = 3; }
                return;
            }
            if (cd > 0) { cd--; return; }
            if (r.masCercano(pp, area(L[i()]) + 2.5, null) == null) return;
            pendientes = N[i()]; espera = 0;
            cd = cdt(CD[i()]);
        }

        private void zarpazo(Vec3d pp) {
            double Ll = area(L[i()]);
            Enemigo o = azar(r, pp, Ll + 1.0);
            double ang = o != null ? Math.atan2(o.e.getZ() - pp.z, o.e.getX() - pp.x) : r.rnd.nextDouble() * Math.PI * 2;
            Vec3d f = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
            for (int k = 0; k < 3; k++) r.w.spawnParticles(ParticleTypes.SWEEP_ATTACK, pp.x + f.x * Ll * (0.45 + 0.2 * k), pp.y + 0.9, pp.z + f.z * Ll * (0.45 + 0.2 * k), 1, 0, 0, 0, 0);
            for (int a = -50; a <= 50; a += 10) {                                       // las tres marcas de la garra: tres lineas de polvo morado
                Vec3d d = rotar(f, Math.toRadians(a));
                for (double e = 0.5; e <= 0.95; e += 0.15) r.w.spawnParticles(MORADO, pp.x + d.x * Ll * e, pp.y + 0.8, pp.z + d.z * Ll * e, 1, 0.04, 0.04, 0.04, 0.0);
                r.w.spawnParticles(ParticleTypes.SQUID_INK, pp.x + d.x * Ll * 0.6, pp.y + 0.8, pp.z + d.z * Ll * 0.6, 1, 0.05, 0.05, 0.05, 0.0);
            }
            double cos = Math.cos(Math.toRadians(55));
            for (Enemigo en : r.enemigos) {
                if (en.hp <= 0) continue;
                double dx = en.e.getX() - pp.x, dz = en.e.getZ() - pp.z, d = Math.sqrt(dx * dx + dz * dz);
                if (d > Ll + en.radio) continue;
                if (d > 0.9 && (dx * f.x + dz * f.z) / d < cos) continue;
                r.golpear(en, DMG[i()], f.x * 0.5, f.z * 0.5);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 24. agujero negro

    static final class Agujero extends Armas.Arma {
        static final double[] DMG = {50, 60, 70, 70, 88, 140}, R = {4.5, 4.9, 5.4, 5.8, 6.4, 9.0};
        static final int[] CD = {200, 186, 172, 160, 146, 110}, VIDA = {40, 44, 48, 52, 56, 70};
        private static final class Hoyo { double x, z; int t; }
        private final List<Hoyo> hoyos = new ArrayList<>();

        Agujero(Run r, int nivel) { super(r, "agujero", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else {
                Enemigo o = masDenso(r, pp, 12);
                if (o != null) {
                    Hoyo h = new Hoyo(); h.x = o.e.getX(); h.z = o.e.getZ();
                    hoyos.add(h);
                    cd = cdt(CD[i()]);
                }
            }
            double Rr = area(R[i()]);
            for (Iterator<Hoyo> it = hoyos.iterator(); it.hasNext(); ) {
                Hoyo h = it.next();
                h.t++;
                double y = pp.y + 0.9, giro = h.t * 0.4;
                r.w.spawnParticles(NEGRO, h.x, y, h.z, 8, 0.35, 0.35, 0.35, 0.0);
                for (int k = 0; k < 4; k++) {
                    double a = giro + k * Math.PI / 2, rr = Rr * 0.8 * (1.0 - (h.t % 20) / 20.0) + 0.5;
                    r.w.spawnParticles(ParticleTypes.PORTAL, h.x + Math.cos(a) * rr, y, h.z + Math.sin(a) * rr, 2, 0.05, 0.05, 0.05, 0.0);
                    r.w.spawnParticles(MORADO, h.x + Math.cos(a) * rr * 0.7, y, h.z + Math.sin(a) * rr * 0.7, 1, 0.05, 0.05, 0.05, 0.0);
                }
                if (h.t % 8 == 0) anillo(r, MORADO, h.x, y, h.z, Rr, (int) (Rr * 3));
                for (Enemigo en : cerca(r, h.x, h.z, Rr * 1.3)) {
                    double dx = h.x - en.e.getX(), dz = h.z - en.e.getZ();
                    if (dx * dx + dz * dz > 0.7 * 0.7) r.impulso(en, dx, dz, 0.06);
                }
                if (h.t < VIDA[i()]) continue;
                r.w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, h.x, y, h.z, 1, 0, 0, 0, 0);
                r.w.spawnParticles(ParticleTypes.END_ROD, h.x, y, h.z, 40, Rr * 0.3, 0.5, Rr * 0.3, 0.3);
                sonido(r, h.x, y, h.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.5f, 0.7f);
                for (Enemigo en : cerca(r, h.x, h.z, Rr)) {
                    Vec3d f = unitario(en.e.getX() - h.x, en.e.getZ() - h.z);
                    r.golpear(en, DMG[i()], f.x * 1.2, f.z * 1.2);
                }
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 25. centinela michi

    static final class Centinela extends Armas.Arma {
        static final double[] DMG = {12, 13, 16, 16, 19, 20};
        static final int[] DISP = {12, 11, 10, 9, 8, 7}, VIDA = {160, 180, 200, 220, 240, 360}, CD = {130, 120, 110, 100, 90, 66}, MAX = {1, 2, 2, 3, 3, 5};
        private static final class Torre { double x, z; int vida, cd; }
        private final List<Torre> torres = new ArrayList<>();

        Centinela(Run r, int nivel) { super(r, "centinela", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else if (torres.size() < MAX[i()]) {
                Torre t = new Torre(); t.x = pp.x + (r.rnd.nextDouble() - 0.5) * 2; t.z = pp.z + (r.rnd.nextDouble() - 0.5) * 2; t.vida = VIDA[i()]; t.cd = 10;
                torres.add(t);
                cd = cdt(CD[i()]);
            }
            for (Iterator<Torre> it = torres.iterator(); it.hasNext(); ) {
                Torre t = it.next();
                if (--t.vida <= 0) { r.w.spawnParticles(ParticleTypes.POOF, t.x, pp.y + 0.8, t.z, 8, 0.2, 0.3, 0.2, 0.02); it.remove(); continue; }
                if (t.vida % 3 == 0) {
                    for (double h = 0.2; h < 1.8; h += 0.5) r.w.spawnParticles(part(ParticleTypes.END_ROD), t.x, pp.y + h, t.z, 1, 0.04, 0.0, 0.04, 0.0);
                    r.w.spawnParticles(ROSA_GRANDE, t.x, pp.y + 1.9, t.z, 3, 0.15, 0.1, 0.15, 0.0);
                }
                if (t.vida % 12 == 0) anillo(r, ROSA_GRANDE, t.x, pp.y + 0.15, t.z, 0.9, 6);
                if (t.vida % 10 == 0) r.w.spawnParticles(ParticleTypes.HEART, t.x, pp.y + 2.3, t.z, 1, 0.1, 0.1, 0.1, 0.0);
                if (--t.cd > 0) continue;
                Enemigo o = r.masCercano(new Vec3d(t.x, pp.y, t.z), 10, null);
                if (o == null) { t.cd = 4; continue; }
                Vec3d hasta = new Vec3d(o.e.getX(), o.e.getY() + 0.7, o.e.getZ());
                linea(r, ROSA_GRANDE, new Vec3d(t.x, pp.y + 1.7, t.z), hasta, 0.5, 20);
                Vec3d f = unitario(o.e.getX() - t.x, o.e.getZ() - t.z);
                r.golpear(o, DMG[i()], f.x * 0.25, f.z * 0.25);
                t.cd = DISP[i()];
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 26. aromaterapia

    static final class Aroma extends Armas.Arma {
        static final double[] PCT = {8, 10, 12, 14, 16, 24}, R = {7.0, 7.5, 8.0, 8.5, 9.0, 13.0}, DMG = {6, 8, 10, 12, 14, 40};
        static final int[] CD = {200, 180, 170, 160, 140, 100};
        private double ang;

        Aroma(Run r, int nivel) { super(r, "aroma", nivel); }

        @Override void tick(Vec3d pp) {
            ang += 9;
            if (cd > 0) { cd--; return; }
            double Rr = area(R[i()]);
            boolean hayHeridos = false;
            for (Run m : r.eq.miembros) if (m.estado == Run.VIVO && m.hp < m.st.hpMax * 0.97 && Math.hypot(m.pp.x - pp.x, m.pp.z - pp.z) <= Rr) hayHeridos = true;
            if (!hayHeridos && r.masCercano(pp, 3.5, null) == null) return;            // sin nadie a quien curar ni enemigos cerca, espera
            for (Run m : r.eq.miembros) {
                if (m.estado != Run.VIVO || Math.hypot(m.pp.x - pp.x, m.pp.z - pp.z) > Rr) continue;
                m.curar(m.st.hpMax * PCT[i()] / 100.0);
                r.w.spawnParticles(ParticleTypes.HEART, m.pp.x, m.pp.y + 1.8, m.pp.z, 5, 0.4, 0.3, 0.4, 0.0);
                r.w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, m.pp.x, m.pp.y + 1.0, m.pp.z, 10, 0.5, 0.6, 0.5, 0.0);
            }
            for (int k = 0; k < 36; k++) {
                double a = Math.toRadians(ang + k * 10), rr = 0.6 + k * 0.1;
                r.w.spawnParticles(ParticleTypes.CHERRY_LEAVES, pp.x + Math.cos(a) * rr, pp.y + 0.3 + k * 0.05, pp.z + Math.sin(a) * rr, 2, 0.05, 0.05, 0.05, 0.0);
                r.w.spawnParticles(ROSA_GRANDE, pp.x + Math.cos(a) * rr, pp.y + 0.3 + k * 0.05, pp.z + Math.sin(a) * rr, 1, 0.05, 0.05, 0.05, 0.0);
            }
            r.w.spawnParticles(ParticleTypes.HEART, pp.x, pp.y + 2.0, pp.z, 8, 1.2, 0.3, 1.2, 0.0);
            anillo(r, ROSA_GRANDE, pp.x, pp.y + 0.2, pp.z, Math.min(Rr, 7.0), 48);
            for (Enemigo en : cerca(r, pp.x, pp.z, 3.5)) {                                // los petalos apartan a los enemigos que tienes encima
                Vec3d f = unitario(en.e.getX() - pp.x, en.e.getZ() - pp.z);
                r.golpear(en, DMG[i()], f.x * 0.9, f.z * 0.9);
            }
            cd = cdt(CD[i()]);
        }
    }

    // ------------------------------------------------------------------------------------------------ 27. pokebola saltarina

    static final class Pokebola extends Armas.Arma {
        static final double[] DMG = {14, 16, 18, 20, 23, 32}, TAM = {0.9, 0.9, 1.0, 1.1, 1.2, 1.8};
        static final int[] CNT = {1, 2, 2, 3, 3, 5}, VIDA = {100, 110, 120, 130, 140, 200}, CD = {92, 86, 80, 74, 68, 50};
        private static final class Bola { double x, z, dx, dz; int vida; final Map<Enemigo, Integer> espera = new HashMap<>(); }
        private final List<Bola> bolas = new ArrayList<>();

        Pokebola(Run r, int nivel) { super(r, "pokebola", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else if (r.masCercano(pp, 14, null) != null) {
                double base = r.rnd.nextDouble() * Math.PI * 2;
                int n = CNT[i()];
                for (int k = 0; k < n; k++) {
                    Bola b = new Bola(); b.x = pp.x; b.z = pp.z; b.vida = VIDA[i()];
                    double a = base + Math.PI * 2 * k / n; b.dx = Math.cos(a); b.dz = Math.sin(a);
                    bolas.add(b);
                }
                cd = cdt(CD[i()]);
            }
            double T = area(TAM[i()]);
            for (Iterator<Bola> it = bolas.iterator(); it.hasNext(); ) {
                Bola b = it.next();
                if (--b.vida <= 0) { r.w.spawnParticles(ParticleTypes.POOF, b.x, pp.y + 0.8, b.z, 8, 0.2, 0.2, 0.2, 0.02); it.remove(); continue; }
                b.x += b.dx * 0.65; b.z += b.dz * 0.65;
                double cx = r.eq.ox, cz = r.eq.oz, lim = Arena.RADIO - 1.8, vx = b.x - cx, vz = b.z - cz, dist = Math.hypot(vx, vz);
                if (dist > lim) {                                                      // rebota en el muro de la arena
                    double nx = vx / dist, nz = vz / dist, punto = b.dx * nx + b.dz * nz;
                    if (punto > 0) { b.dx -= 2 * punto * nx; b.dz -= 2 * punto * nz; }
                    b.x = cx + nx * lim; b.z = cz + nz * lim;
                }
                double y = pp.y + 0.9;
                r.w.spawnParticles(ROJO_GRANDE, b.x, y + 0.25, b.z, 3, T * 0.3, T * 0.12, T * 0.3, 0.0);          // arriba rojo, abajo blanco: una Pokebola
                r.w.spawnParticles(BLANCO, b.x, y - 0.25, b.z, 3, T * 0.3, T * 0.12, T * 0.3, 0.0);
                if (ver(r, 0.7)) r.w.spawnParticles(NEGRO_FINO, b.x, y, b.z, 2, T * 0.35, 0.03, T * 0.35, 0.0);
                if (b.vida % 4 == 0) r.w.spawnParticles(ParticleTypes.ENCHANTED_HIT, b.x, y, b.z, 1, 0.1, 0.1, 0.1, 0.0);
                for (Iterator<Map.Entry<Enemigo, Integer>> e = b.espera.entrySet().iterator(); e.hasNext(); ) {
                    var x = e.next();
                    if (x.getKey().hp <= 0 || x.getValue() <= 1) e.remove(); else x.setValue(x.getValue() - 1);
                }
                for (Enemigo en : cerca(r, b.x, b.z, T)) {
                    if (b.espera.containsKey(en)) continue;
                    b.espera.put(en, 10);
                    r.golpear(en, DMG[i()], b.dx * 0.5, b.dz * 0.5);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 28. burbujas

    static final class Burbujas extends Armas.Arma {
        static final double[] DMG = {20, 24, 28, 28, 34, 48}, R = {1.8, 1.9, 2.0, 2.2, 2.4, 3.2};
        static final int[] CNT = {2, 3, 3, 4, 5, 8}, CD = {84, 78, 72, 66, 60, 42};
        private static final class Burbuja { double x, z, dx, dz; int vida = 120; }
        private final List<Burbuja> burbujas = new ArrayList<>();

        Burbujas(Run r, int nivel) { super(r, "burbujas", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else if (r.masCercano(pp, 14, null) != null) {
                for (int k = 0; k < CNT[i()]; k++) {
                    Burbuja b = new Burbuja(); b.x = pp.x; b.z = pp.z;
                    double a = r.rnd.nextDouble() * Math.PI * 2; b.dx = Math.cos(a) * 0.2; b.dz = Math.sin(a) * 0.2;
                    burbujas.add(b);
                }
                cd = cdt(CD[i()]);
            }
            double Rr = area(R[i()]);
            for (Iterator<Burbuja> it = burbujas.iterator(); it.hasNext(); ) {
                Burbuja b = it.next();
                b.vida--;
                Enemigo o = r.masCercano(new Vec3d(b.x, pp.y, b.z), 13, null);
                if (o != null) {                                                       // se acerca despacio al enemigo mas cercano
                    Vec3d d = unitario(o.e.getX() - b.x, o.e.getZ() - b.z);
                    b.dx += (d.x * 0.14 - b.dx) * 0.12; b.dz += (d.z * 0.14 - b.dz) * 0.12;
                }
                b.x += b.dx; b.z += b.dz;
                double y = pp.y + 0.9 + Math.sin((120 - b.vida) * 0.2) * 0.2;
                r.w.spawnParticles(ParticleTypes.BUBBLE, b.x, y, b.z, 2, 0.15, 0.15, 0.15, 0.0);
                if (b.vida % 2 == 0) { anillo(r, AZUL, b.x, y, b.z, 0.6, 6); r.w.spawnParticles(BLANCO, b.x - 0.25, y + 0.25, b.z - 0.25, 1, 0.02, 0.02, 0.02, 0.0); }
                boolean revienta = b.vida <= 0;
                if (!revienta) for (Enemigo en : cerca(r, b.x, b.z, 0.5)) { revienta = true; break; }
                if (!revienta) continue;
                r.w.spawnParticles(ParticleTypes.BUBBLE_POP, b.x, y, b.z, 24, Rr * 0.4, 0.3, Rr * 0.4, 0.05);
                anillo(r, ParticleTypes.SPLASH, b.x, y - 0.4, b.z, Rr, 18);
                for (Enemigo en : cerca(r, b.x, b.z, Rr)) {
                    Vec3d f = unitario(en.e.getX() - b.x, en.e.getZ() - b.z);
                    r.golpear(en, DMG[i()], f.x * 0.5, f.z * 0.5);
                    en.ralentizar(40, 0.7);
                }
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 29. latigo cepa

    static final class Latigo extends Armas.Arma {
        static final double[] DMG = {18, 21, 24, 28, 32, 46}, L = {6.0, 6.5, 7.0, 8.0, 9.0, 12.0};
        static final int[] CD = {28, 26, 24, 22, 20, 14}, LADOS = {1, 1, 2, 2, 2, 4};
        private static final double[] GIROS = {0, Math.PI, Math.PI / 2, -Math.PI / 2};
        private boolean alterna;

        Latigo(Run r, int nivel) { super(r, "latigo", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) { cd--; return; }
            double Ll = area(L[i()]);
            Enemigo o = r.masCercano(pp, Ll, null);
            if (o == null) return;
            Vec3d base = unitario(o.e.getX() - pp.x, o.e.getZ() - pp.z);
            alterna = !alterna;
            for (int k = 0; k < LADOS[i()]; k++) azotar(pp, rotar(base, GIROS[k] + (alterna ? 0.2 : -0.2)), Ll);
            cd = cdt(CD[i()]);
        }

        private void azotar(Vec3d pp, Vec3d dir, double Ll) {
            double px = -dir.z, pz = dir.x;
            for (double d = 0.4; d <= Ll; d += 0.7) {
                double off = Math.sin(d * 1.4 + r.tick * 0.5) * 0.3;
                r.w.spawnParticles(VERDE, pp.x + dir.x * d + px * off, pp.y + 0.8, pp.z + dir.z * d + pz * off, 2, 0.05, 0.05, 0.05, 0.0);
            }
            for (Enemigo en : r.enemigos) {
                if (en.hp <= 0) continue;
                double ex = en.e.getX() - pp.x, ez = en.e.getZ() - pp.z;
                double along = ex * dir.x + ez * dir.z, across = ex * px + ez * pz;
                if (along < -0.3 || along > Ll + en.radio || Math.abs(across) > 0.9 + en.radio) continue;
                r.golpear(en, DMG[i()], dir.x * 0.5, dir.z * 0.5);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ 30. media luna

    static final class Lunar extends Armas.Arma {
        static final double[] DMG = {26, 30, 30, 34, 38, 52}, ESQ = {12, 14, 16, 18, 20, 28};
        static final int[] CD = {64, 60, 56, 52, 46, 34}, NESQ = {3, 4, 5, 5, 6, 10};
        private static final class Luna { double x, z; Vec3d dir; int vida = 30; }
        private final List<Luna> lunas = new ArrayList<>();

        Lunar(Run r, int nivel) { super(r, "lunar", nivel); }

        @Override void tick(Vec3d pp) {
            if (cd > 0) cd--;
            else {
                Enemigo o = r.masCercano(pp, 13, null);
                if (o != null) {
                    Luna l = new Luna(); l.x = pp.x; l.z = pp.z; l.dir = unitario(o.e.getX() - pp.x, o.e.getZ() - pp.z);
                    lunas.add(l);
                    cd = cdt(CD[i()]);
                }
            }
            for (Iterator<Luna> it = lunas.iterator(); it.hasNext(); ) {
                Luna l = it.next();
                l.x += l.dir.x * 0.8; l.z += l.dir.z * 0.8;
                double px = -l.dir.z, pz = l.dir.x, y = pp.y + 0.9;
                for (int j = -5; j <= 5; j++) {                                        // la forma de una media luna: un arco que abraza hacia atras
                    double atras = j * j * 0.045 * area(1.0), lado = j * 0.2 * area(1.0);
                    double qx = l.x - l.dir.x * atras + px * lado, qz = l.z - l.dir.z * atras + pz * lado;
                    r.w.spawnParticles(Math.abs(j) >= 4 ? ROSA_GRANDE : part(ParticleTypes.END_ROD), qx, y, qz, 1, 0.02, 0.02, 0.02, 0.0);
                    r.w.spawnParticles(BLANCO, qx, y, qz, 1, 0.02, 0.02, 0.02, 0.0);
                }
                boolean golpe = --l.vida <= 0;
                Enemigo tocado = null;
                for (Enemigo en : cerca(r, l.x, l.z, 0.9 * area(1.0))) { tocado = en; break; }
                if (tocado != null) {
                    golpe = true;
                    r.golpear(tocado, DMG[i()], l.dir.x * 0.4, l.dir.z * 0.4);
                }
                if (!golpe) continue;
                if (tocado != null) {
                    r.w.spawnParticles(ParticleTypes.ENCHANTED_HIT, l.x, y, l.z, 12, 0.3, 0.3, 0.3, 0.1);
                    int n = NESQ[i()];
                    double a0 = r.rnd.nextDouble() * Math.PI * 2;
                    for (int k = 0; k < n; k++) {
                        double a = a0 + Math.PI * 2 * k / n;
                        r.proyectiles.add(new Proyectil(r, new Vec3d(l.x, pp.y + 0.9, l.z), new Vec3d(Math.cos(a), 0, Math.sin(a)), ESQ[i()], part(ParticleTypes.END_ROD), 12, 2));
                    }
                }
                it.remove();
            }
        }
    }
}
