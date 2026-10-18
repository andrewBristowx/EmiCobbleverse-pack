package emi.survivors;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Un jefe de la partida (los datos de cada uno estan en {@link Jefes}): un Pokemon enorme que persigue despacio al jugador en pie mas cercano y, de vez en cuando,
 * hace un ataque de su lista. TODOS los ataques se AVISAN en el suelo con {@link Marcas} (cristal rojo = zona peligrosa, que se va rellenando de alfombra roja hasta
 * que cae el golpe; cristal verde = sitio seguro) para que se puedan esquivar: salto (cuadrado), carga (carril), zarpazo (rectangulo), lluvia (varios cuadrados),
 * anillo (una ola cuadrada con un hueco verde), haz giratorio (rayos que giran) e invocar refuerzos. Si hay varios jugadores los ataques hacen dano a TODOS los que
 * pillen. Su vida y su dano de contacto los lleva su {@link Enemigo}; los golpes de los jugadores le llegan como a cualquier enemigo. Al bajar del 50 % de vida, los
 * jefes con «furia» atacan mas deprisa y con avisos mas cortos.
 */
final class Jefe {
    private static final int PERSIGUE = 0, ATACA = 1, DESCANSA = 2;

    final Equipo eq;
    final Enemigo en;
    final Jefes.Def def;
    final int numero;                   // el jefe numero n de la partida (1 = el de los 3:00)
    final String nombre;
    final float dmg;                    // dano base de sus ataques (sube con cada vuelta de los cinco jefes)
    boolean furia;
    private int estado = PERSIGUE, t, idx;
    private Atq atq;
    private final java.util.Random rnd;
    private final List<Entity> mias = new ArrayList<>();       // las marcas de aviso que ha puesto este jefe (se quitan al acabar cada ataque o al morir)

    Jefe(Equipo eq, Enemigo en, Jefes.Def def, int numero) { this(eq, en, def, numero, Jefes.dano(numero)); }

    /** Con el dano base de sus ataques ya calculado (por la vuelta del recorrido, o el de un legendario de grieta, que usa estos mismos ataques). */
    Jefe(Equipo eq, Enemigo en, Jefes.Def def, int numero, float dmg) {
        this.eq = eq; this.en = en; this.def = def; this.numero = numero; this.nombre = def.nombre(); this.dmg = dmg; this.rnd = eq.rnd;
    }

    boolean vivo() { return en.hp > 0 && !en.e.isRemoved(); }

    private double bx() { return en.e.getX(); }
    private double bz() { return en.e.getZ(); }
    private double suelo() { return Arena.Y0 + 0.1; }

    /** Un sonido del jefe: lo oye todo el equipo a su volumen (la camara esta a 14 bloques de altura, asi que no se atenua con la distancia). */
    private void sonido(SoundEvent s, float vol, float pitch) {
        for (Run m : eq.miembros) Sonidos.sfx(m.jugador(), s, vol, pitch);
    }

    // ------------------------------------------------------------------------------------------------ marcas de aviso

    private Entity marca(String bloque, double x, double z, double giro, float ancho, float largo, boolean extremo, boolean relleno) {
        Entity e = Marcas.crear(eq, bloque, x, relleno ? Marcas.Y_RELLENO : Marcas.Y_FONDO, z, giro, ancho, relleno ? 1f : 0.05f, largo, extremo);
        if (e != null) mias.add(e);
        return e;
    }

    /** Hace que un relleno (alfombra) crezca hasta su tamano final en {@code ticks} ticks (lo anima el cliente). */
    private void llenar(Entity relleno, double x, double z, double giro, float ancho, float largo, boolean extremo, int ticks) {
        Marcas.poner(relleno, x, Marcas.Y_RELLENO, z, giro, ancho, 1f, largo, extremo, Math.max(1, ticks));
    }

    private void quitar(Entity e) { if (e == null) return; mias.remove(e); Marcas.quitar(eq, e); }

    /** Quita todas las marcas del jefe (al morir, o al acabar la partida). */
    void limpiar() {
        for (Entity e : new ArrayList<>(mias)) Marcas.quitar(eq, e);
        mias.clear();
    }

    // ------------------------------------------------------------------------------------------------ el jefe

    /** Un tick; {@code objetivos} = los jugadores a los que puede perseguir y golpear (en pie y sin estar eligiendo carta). */
    void tick(List<Run> objetivos) {
        if (def.furia() && !furia && en.hp < en.hpMax * 0.5f) enfurecer();
        switch (estado) {
            case PERSIGUE -> {
                perseguir(objetivos, furia ? 1.25 : 1.0);
                if (!objetivos.isEmpty() && ++t >= (furia ? (int) (def.espera() * 0.7) : def.espera())) { atq = siguiente(objetivos); estado = ATACA; t = 0; }
            }
            case ATACA -> {
                if (atq == null || atq.tick(objetivos)) { atq = null; limpiar(); estado = DESCANSA; t = 0; }
            }
            default -> {            // un momento recuperando el aliento (mas despacio)
                perseguir(objetivos, 0.4);
                if (++t >= (furia ? 24 : 40)) { estado = PERSIGUE; t = 0; }
            }
        }
    }

    private void perseguir(List<Run> objetivos, double factor) {
        Run obj = Equipo.masCercano(objetivos, bx(), bz());
        if (obj == null) en.mover(bx(), bz(), 0); else en.mover(obj.pp.x, obj.pp.z, factor);
    }

    private void enfurecer() {
        furia = true;
        EmiSurvivors.LOG.info("[jefe] {}: {} se enfurece (le quedan {} de {})", eq.nombres(), nombre, Math.round(en.hp), Math.round(en.hpMax));
        for (Run m : eq.miembros) {
            ServerPlayerEntity p = m.jugador();
            if (p != null) Msg.title(p, Text.literal("¡" + nombre.toUpperCase() + " SE ENFURECE!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Ataca más deprisa y con más fuerza").formatted(Formatting.YELLOW), 5, 40, 15);
        }
        eq.w.spawnParticles(ParticleTypes.ANGRY_VILLAGER, bx(), en.e.getY() + en.radio * 2, bz(), 24, en.radio, en.radio, en.radio, 0.0);
        sonido(Sonidos.JEFE_RUGIDO, 1.0f, 1.0f);
    }

    private Atq siguiente(List<Run> objetivos) {
        Jefes.Ataque a = def.ataques()[idx++ % def.ataques().length];
        if (Boolean.getBoolean("emi.survivors.debug")) EmiSurvivors.LOG.info("[jefe] {} ataca: {}{}", nombre, a, furia ? " (furia)" : "");
        Run objetivo = objetivos.get(rnd.nextInt(objetivos.size()));            // a quien apunta: uno cualquiera, no siempre el mas cercano
        return switch (a) {
            case SALTO -> new Salto(objetivo);
            case CARGA -> new Carga(objetivo);
            case CONO -> new Cono(objetivo);
            case LLUVIA -> new Lluvia(objetivos);
            case ANILLO -> new Anillo();
            case HAZ -> new Haz();
            case INVOCAR -> new Invocar();
        };
    }

    /** Las particulas del «impacto» de cada jefe (el aviso siempre es rojo). */
    private ParticleEffect impacto() {
        return switch (def.tema()) {
            case 0 -> ParticleTypes.CLOUD;
            case 1 -> ParticleTypes.CHERRY_LEAVES;
            case 2 -> ParticleTypes.SOUL_FIRE_FLAME;
            case 3 -> ParticleTypes.END_ROD;
            default -> ParticleTypes.TOTEM_OF_UNDYING;
        };
    }

    // ------------------------------------------------------------------------------------------------ los ataques

    private abstract class Atq {
        int t;
        /** Un tick del ataque; true cuando ha terminado. */
        abstract boolean tick(List<Run> objetivos);
    }

    /** Salta sobre el sitio donde estaba el jugador al empezar el aviso: un cuadrado rojo que se va rellenando; cae con una explosion. */
    private final class Salto extends Atq {
        static final double S = 4.2;                         // medio lado del cuadrado de aviso y de dano
        final double tx, tz;
        double sx, sz, y0;
        boolean saltando;
        Entity fondo, relleno;
        final int aviso = furia ? 22 : 28;

        Salto(Run obj) { tx = obj.pp.x; tz = obj.pp.z; }

        @Override boolean tick(List<Run> objetivos) {
            t++;
            if (!saltando) {
                if (t == 1) { fondo = marca(Marcas.CRISTAL_ROJO, tx, tz, 0, (float) (2 * S), (float) (2 * S), false, false); relleno = marca(Marcas.ALFOMBRA_ROJA, tx, tz, 0, 0.2f, 0.2f, false, true); }
                if (t == 2) llenar(relleno, tx, tz, 0, (float) (2 * S), (float) (2 * S), false, aviso - 2);
                en.mover(tx, tz, 0);
                if (t >= aviso) { saltando = true; t = 0; sx = bx(); sz = bz(); y0 = en.e.getY(); }
                return false;
            }
            double f = Math.min(1.0, t / 10.0);
            en.e.refreshPositionAndAngles(sx + (tx - sx) * f, y0 + Math.sin(f * Math.PI) * 2.4, sz + (tz - sz) * f, en.e.getYaw(), 0f);
            if (t < 10) return false;
            en.e.refreshPositionAndAngles(tx, y0, tz, en.e.getYaw(), 0f);
            quitar(fondo); quitar(relleno);
            eq.w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, tx, y0 + 0.5, tz, 1, 0, 0, 0, 0);
            for (int k = 0; k < 28; k++) {
                double a = k * Math.PI * 2 / 28, r = S * 1.1;
                eq.w.spawnParticles(impacto(), tx + Math.cos(a) * r, y0 + 0.3, tz + Math.sin(a) * r, 2, 0.1, 0.1, 0.1, 0.05);
            }
            sonido(Sonidos.JEFE_IMPACTO, 1.0f, 1.0f);
            for (Run m : objetivos)
                if (Marcas.dentroDeCuadrado(m.pp.x, m.pp.z, tx, tz, 2 * S, 0.3) && Math.abs(m.pp.y - y0) < 2.5) m.danarJugador(dmg * 1.4f);
            return true;
        }
    }

    /** Embiste en linea recta hacia donde estaba el jugador: un carril rojo que se rellena y luego la carrera. */
    private final class Carga extends Atq {
        final double dx, dz, giro, x0, z0;
        boolean cargando;
        Entity fondo, relleno;
        final Set<Run> golpeados = new HashSet<>();
        final int aviso = furia ? 18 : 24;
        final double v = def.tema() == 0 ? 0.7 : 0.85;
        final int dur = 18;
        final float ancho, largo;

        Carga(Run obj) {
            double l = Math.hypot(obj.pp.x - bx(), obj.pp.z - bz());
            dx = l < 1e-6 ? 0 : (obj.pp.x - bx()) / l; dz = l < 1e-6 ? 1 : (obj.pp.z - bz()) / l;
            giro = Math.atan2(dx, dz); x0 = bx(); z0 = bz();
            ancho = (float) (2 * (en.radio + 0.7)); largo = (float) (dur * v + en.radio + 1.5);
        }

        @Override boolean tick(List<Run> objetivos) {
            t++;
            double x = bx(), z = bz();
            if (!cargando) {
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                en.e.setYaw(yaw); en.e.setHeadYaw(yaw); en.e.setBodyYaw(yaw);
                if (t == 1) { fondo = marca(Marcas.CRISTAL_ROJO, x0, z0, giro, ancho, largo, true, false); relleno = marca(Marcas.ALFOMBRA_ROJA, x0, z0, giro, ancho, 0.2f, true, true); }
                if (t == 2) llenar(relleno, x0, z0, giro, ancho, largo, true, aviso - 2);
                if (t >= aviso) { cargando = true; t = 0; sonido(Sonidos.JEFE_CARGA, 1.0f, 1.0f); }
                return false;
            }
            en.e.setVelocity(Vec3d.ZERO);
            en.e.move(MovementType.SELF, new Vec3d(dx * v, -0.2, dz * v));
            eq.w.spawnParticles(ParticleTypes.CLOUD, x, en.e.getY() + 0.2, z, 4, en.radio * 0.5, 0.1, en.radio * 0.5, 0.02);
            for (Run m : objetivos)
                if (!golpeados.contains(m) && Math.hypot(m.pp.x - x, m.pp.z - z) <= en.radio + 0.7) { golpeados.add(m); m.danarJugador(dmg * 1.6f); }
            if (t < dur) return false;
            quitar(fondo); quitar(relleno);
            return true;
        }
    }

    /** Zarpazo: un rectangulo corto y ancho delante de el; sigue al objetivo al principio y fija la direccion un poco antes de golpear. */
    private final class Cono extends Atq {
        static final double ANCHO = 6.6, LARGO = 8.0;
        final Run obj;
        double giro;
        Entity fondo, relleno;
        final int aviso = furia ? 18 : 24, fija = (int) (aviso * 0.55);

        Cono(Run obj) { this.obj = obj; giro = Math.atan2(obj.pp.x - bx(), obj.pp.z - bz()); }

        @Override boolean tick(List<Run> objetivos) {
            t++;
            double x = bx(), z = bz();
            en.mover(x, z, 0);
            if (t <= fija) giro = Math.atan2(obj.pp.x - x, obj.pp.z - z);                 // sigue al objetivo; luego la direccion queda fija
            float yaw = (float) Math.toDegrees(-giro);
            en.e.setYaw(yaw); en.e.setHeadYaw(yaw); en.e.setBodyYaw(yaw);
            if (t == 1) fondo = marca(Marcas.CRISTAL_NARANJA, x, z, giro, (float) ANCHO, (float) LARGO, true, false);
            else if (t <= fija && fondo != null) Marcas.poner(fondo, x, Marcas.Y_FONDO, z, giro, (float) ANCHO, 0.05f, (float) LARGO, true, 0);
            if (t == fija) { quitar(fondo); fondo = marca(Marcas.CRISTAL_ROJO, x, z, giro, (float) ANCHO, (float) LARGO, true, false); relleno = marca(Marcas.ALFOMBRA_ROJA, x, z, giro, (float) ANCHO, 0.2f, true, true); }
            if (t == fija + 1 && relleno != null) llenar(relleno, x, z, giro, (float) ANCHO, (float) LARGO, true, aviso - fija - 1);
            if (t < aviso) return false;
            quitar(fondo); quitar(relleno);
            for (int k = -3; k <= 3; k++)
                for (double d = 2.0; d <= LARGO; d += 2.0)
                    eq.w.spawnParticles(ParticleTypes.SWEEP_ATTACK, x + Math.sin(giro) * d + Math.cos(giro) * k * 0.9, en.e.getY() + 1.0, z + Math.cos(giro) * d - Math.sin(giro) * k * 0.9, 1, 0, 0, 0, 0);
            eq.w.spawnParticles(impacto(), x + Math.sin(giro) * LARGO * 0.6, en.e.getY() + 1.0, z + Math.cos(giro) * LARGO * 0.6, 20, ANCHO * 0.3, 0.4, ANCHO * 0.3, 0.02);
            sonido(Sonidos.JEFE_ZARPAZO, 1.0f, 1.0f);
            for (Run m : objetivos)
                if (Marcas.dentroDeRayo(m.pp.x, m.pp.z, x, z, giro, ANCHO, LARGO, 0.3) || Math.hypot(m.pp.x - x, m.pp.z - z) < en.radio + 1.0) m.danarJugador(dmg * 1.5f);
            return true;
        }
    }

    /** Varios impactos a la vez, uno cerca de cada jugador y el resto repartidos a su alrededor: cada uno avisa con su cuadrado rojo unos 1,3 s y explota. */
    private final class Lluvia extends Atq {
        static final double LADO = 4.8;
        static final int AVISO = 26;
        final double[] x, z;
        final int[] salida;
        final boolean[] hecho;
        final Entity[] fondo, relleno;
        final int n;

        Lluvia(List<Run> objetivos) {
            n = Jefes.impactos(eq.miembros.size(), numero);
            x = new double[n]; z = new double[n]; salida = new int[n]; hecho = new boolean[n]; fondo = new Entity[n]; relleno = new Entity[n];
            for (int k = 0; k < n; k++) {
                Run base = objetivos.get(k % objetivos.size());
                double ang = rnd.nextDouble() * Math.PI * 2, r = k < objetivos.size() ? rnd.nextDouble() * 2.0 : 2.0 + rnd.nextDouble() * 9.0;
                double px = base.pp.x + Math.cos(ang) * r, pz = base.pp.z + Math.sin(ang) * r;
                if (!Arena.dentro(eq, px, pz, 3.0)) { px = base.pp.x; pz = base.pp.z; }
                x[k] = px; z[k] = pz; salida[k] = rnd.nextInt(18);
            }
        }

        @Override boolean tick(List<Run> objetivos) {
            t++;
            en.mover(bx(), bz(), 0);
            boolean quedan = false;
            for (int k = 0; k < n; k++) {
                if (hecho[k]) continue;
                int u = t - salida[k];
                if (u < 0) { quedan = true; continue; }
                if (u == 0) { fondo[k] = marca(Marcas.CRISTAL_ROJO, x[k], z[k], 0, (float) LADO, (float) LADO, false, false); relleno[k] = marca(Marcas.ALFOMBRA_ROJA, x[k], z[k], 0, 0.2f, 0.2f, false, true); }
                if (u == 1) llenar(relleno[k], x[k], z[k], 0, (float) LADO, (float) LADO, false, AVISO - 1);
                if (u < AVISO) { quedan = true; continue; }
                hecho[k] = true;
                quitar(fondo[k]); quitar(relleno[k]);
                eq.w.spawnParticles(ParticleTypes.EXPLOSION, x[k], suelo() + 0.6, z[k], 1, 0, 0, 0, 0);
                eq.w.spawnParticles(impacto(), x[k], suelo() + 0.5, z[k], 14, LADO * 0.25, 0.4, LADO * 0.25, 0.05);
                if (k % 3 == 0) sonido(Sonidos.JEFE_IMPACTO, 0.55f, 1.25f);
                for (Run m : objetivos) if (Marcas.dentroDeCuadrado(m.pp.x, m.pp.z, x[k], z[k], LADO, 0.3)) m.danarJugador(dmg * 0.9f);
            }
            return !quedan;
        }
    }

    /**
     * Una ola CUADRADA que se expande desde el jefe con un HUECO por el que se puede pasar: antes de salir, el hueco (un carril verde que sale del jefe) se marca en el suelo;
     * despues la ola (cuatro barras rojas) crece hasta el borde y quien esta en su camino (fuera del hueco) cuando pasa sufre dano. Si estas muy lejos del carril, corre hacia el.
     */
    private final class Anillo extends Atq {
        static final double V = 0.42, R_MAX = 30, GROSOR = 1.7, HUECO = 9.0;
        static final int[][] DIR = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        final int lado = rnd.nextInt(4);
        final double off = (rnd.nextDouble() - 0.5) * 14;                 // donde esta el centro del hueco a lo largo del lado
        final int aviso = furia ? 34 : 44;
        final double cx, cz;
        double r = 1.0;
        Entity carril;
        final List<Entity> barras = new ArrayList<>();
        final Set<Run> golpeados = new HashSet<>();

        Anillo() { cx = bx(); cz = bz(); }

        @Override boolean tick(List<Run> objetivos) {
            t++;
            en.mover(cx, cz, 0);
            int dx = DIR[lado][0], dz = DIR[lado][1], px = -dz, pz = dx;                 // adelante y de lado del lado elegido
            if (t == 1) carril = marca(Marcas.CRISTAL_VERDE, cx + px * off, cz + pz * off, Math.atan2(dx, dz), (float) HUECO, (float) R_MAX, true, false);
            if (t <= aviso) {
                if (t % 4 == 0) eq.w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, cx + dx * 4 + px * off, suelo() + 0.5, cz + dz * 4 + pz * off, 5, 1.5, 0.3, 1.5, 0.0);
                if (t == aviso) sonido(Sonidos.JEFE_AVISO, 1.0f, 1.0f);
                return false;
            }
            r += V;
            if (t % 2 == 0) pintar(false);
            for (Run m : objetivos) {
                if (golpeados.contains(m)) continue;
                double ex = m.pp.x - cx, ez = m.pp.z - cz, rp = Math.max(Math.abs(ex), Math.abs(ez));
                if (Math.abs(rp - r) > GROSOR / 2 + 0.6) continue;
                boolean mismoLado = (Math.abs(ex) >= Math.abs(ez) ? (ex >= 0 ? 0 : 2) : (ez >= 0 ? 1 : 3)) == lado;
                double lateral = ex * px + ez * pz;
                if (mismoLado && Math.abs(lateral - off) <= HUECO / 2) continue;                      // esta en el hueco: se salva
                golpeados.add(m); m.danarJugador(dmg * 1.1f);
            }
            if (r < R_MAX) return false;
            pintar(true);
            quitar(carril);
            return true;
        }

        /** Coloca las barras de la ola (4 lados; el lado del hueco se parte en dos). Con {@code borrar} las quita. */
        private void pintar(boolean borrar) {
            List<double[]> trozos = new ArrayList<>();                 // {centroX, centroZ, giro, largoLateral}
            for (int s = 0; s < 4 && !borrar; s++) {
                int dx = DIR[s][0], dz = DIR[s][1], px = -dz, pz = dx;
                double a = -(r + GROSOR / 2), b = r + GROSOR / 2;
                if (s == lado) {
                    double h0 = off - HUECO / 2, h1 = off + HUECO / 2;
                    if (h0 - a > 0.2) trozos.add(trozo(dx, dz, px, pz, a, h0));
                    if (b - h1 > 0.2) trozos.add(trozo(dx, dz, px, pz, h1, b));
                } else trozos.add(trozo(dx, dz, px, pz, a, b));
            }
            while (barras.size() < trozos.size()) { Entity e = marca(Marcas.ALFOMBRA_ROJA, cx, cz, 0, (float) GROSOR, 0.2f, false, true); if (e == null) break; barras.add(e); }
            for (int k = 0; k < barras.size(); k++) {
                if (k >= trozos.size()) { Marcas.poner(barras.get(k), cx, Marcas.Y_RELLENO, cz, 0, 0.01f, 1f, 0.01f, false, 0); continue; }
                double[] c = trozos.get(k);
                Marcas.poner(barras.get(k), c[0], Marcas.Y_RELLENO, c[1], c[2], (float) GROSOR, 1f, (float) c[3], false, 2);
            }
            if (borrar) { for (Entity e : barras) quitar(e); barras.clear(); }
        }

        /** Un trozo de barra del lado cuya direccion es (dx, dz): va del punto lateral {@code a} al {@code b}, a la distancia {@code r} del centro. */
        private double[] trozo(int dx, int dz, int px, int pz, double a, double b) {
            double mid = (a + b) / 2;
            return new double[]{cx + dx * r + px * mid, cz + dz * r + pz * mid, Math.atan2(px, pz), b - a};
        }
    }

    /** Rayos que giran alrededor del jefe (2, o 3 con la furia): primero aparecen finos y quietos un momento y luego giran unos 5 s. */
    private final class Haz extends Atq {
        static final double LARGO = 22, ANCHO = 1.7;
        final int rayos = furia ? 3 : 2;
        final double a0 = rnd.nextDouble() * Math.PI * 2, w = (rnd.nextBoolean() ? 1 : -1) * 0.03;
        final int aviso = furia ? 22 : 30, dur = 100;
        final double cx, cz;
        final List<Entity> haces = new ArrayList<>();

        Haz() { cx = bx(); cz = bz(); }

        private double angulo(int k, double giro) { return a0 + giro + k * Math.PI * 2 / rayos; }

        @Override boolean tick(List<Run> objetivos) {
            t++;
            en.mover(cx, cz, 0);
            if (t == 1) for (int k = 0; k < rayos; k++) haces.add(marca(Marcas.CRISTAL_ROJO, cx, cz, Math.PI / 2 - angulo(k, 0), 0.6f, (float) LARGO, true, false));
            if (t == aviso) {
                for (Entity e : haces) quitar(e);
                haces.clear();
                for (int k = 0; k < rayos; k++) haces.add(marca(Marcas.ALFOMBRA_ROJA, cx, cz, Math.PI / 2 - angulo(k, 0), (float) ANCHO, (float) LARGO, true, true));
                sonido(Sonidos.JEFE_RAYO, 1.0f, 1.0f);
            }
            if (t <= aviso) return false;
            double giro = (t - aviso) * w;
            if (t % 2 == 0) for (int k = 0; k < rayos; k++) if (k < haces.size()) Marcas.poner(haces.get(k), cx, Marcas.Y_RELLENO, cz, Math.PI / 2 - angulo(k, giro), (float) ANCHO, 1f, (float) LARGO, true, 2);
            for (Run m : objetivos) {
                if (m.iframes > 0) continue;
                double ddx = m.pp.x - cx, ddz = m.pp.z - cz, d = Math.hypot(ddx, ddz);
                if (d > LARGO + 1 || d < 0.5) continue;
                for (int k = 0; k < rayos; k++) {
                    double ak = angulo(k, giro);
                    if (Marcas.dentroDeRayo(m.pp.x, m.pp.z, cx, cz, Math.PI / 2 - ak, ANCHO, LARGO, 0.3)) { m.danarJugador(dmg * 0.55f); break; }
                }
            }
            return t >= aviso + dur;
        }
    }

    /** Llama refuerzos: aviso con humo de su color y, al acabar, aparecen alrededor suyo (los crea el equipo poco a poco, para no atascar el servidor). */
    private final class Invocar extends Atq {
        final int aviso = furia ? 18 : 26;

        @Override boolean tick(List<Run> objetivos) {
            t++;
            en.mover(bx(), bz(), 0);
            if (t % 2 == 0) {
                double r = en.radio + 1.0 + 3.0 * t / aviso;
                for (int k = 0; k < 14; k++) { double a = k * Math.PI * 2 / 14; eq.w.spawnParticles(impacto(), bx() + Math.cos(a) * r, suelo() + 0.3, bz() + Math.sin(a) * r, 1, 0, 0, 0, 0); }
                eq.w.spawnParticles(impacto(), bx(), en.e.getY() + en.radio * 1.5, bz(), 6, en.radio * 0.5, 0.8, en.radio * 0.5, 0.05);
            }
            if (t == 1) {
                sonido(Sonidos.JEFE_INVOCA, 1.0f, 1.0f);
                for (Run m : eq.miembros) { ServerPlayerEntity p = m.jugador(); if (p != null) Msg.actionBar(p, Text.literal("¡" + nombre + " invoca refuerzos!").formatted(Formatting.GOLD, Formatting.BOLD)); }
            }
            if (t < aviso) return false;
            eq.invocar(def, Jefes.refuerzos(eq.miembros.size(), numero, furia), bx(), bz(), en.radio);
            return true;
        }
    }
}
