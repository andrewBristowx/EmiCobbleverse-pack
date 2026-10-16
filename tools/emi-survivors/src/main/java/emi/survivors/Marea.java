package emi.survivors;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Una marea en marcha (ver {@link Mareas}): un suceso con aviso que rompe el ritmo de la horda. Empieza con un titulo y el sonido de un cuerno; segun su tipo:
 * <ul>
 *   <li>MICHIS y TIPO: durante 30 s los enemigos salen mucho mas deprisa y todos son Michialiens / de un tipo ({@link Equipo#crearEnemigo} lo mira);</li>
 *   <li>CERCO: se marca en verde el hueco por el que salir y, a los 2,5 s, aparece de golpe un anillo de enemigos alrededor de los jugadores;</li>
 *   <li>ESTAMPIDA: se marcan 2-3 carriles rojos que cruzan la arena y, a los 2,8 s, una fila de enemigos muy rapidos los recorre sin desviarse.</li>
 * </ul>
 */
final class Marea {
    static final int AVISO_CERCO = 50, AVISO_ESTAMPIDA = 56, SEPARACION_SALIDAS = 6;

    final Equipo eq;
    final Mareas.Tipo tipo;
    final int numero;
    final Mareas.TipoPk pk;                 // el tipo de Pokemon, si es una marea «de un tipo»
    private final java.util.Random rnd;
    private int t;
    private final int dur;                  // ticks que dura (para la cuenta atras de la interfaz)
    private final List<Entity> mias = new ArrayList<>();
    // cerco
    private double cx, cz, ang0;
    private final List<double[]> pendientes = new ArrayList<>();
    private Entity carril;
    // estampida
    private final List<double[]> carriles = new ArrayList<>();         // {x0, z0, ux, uz, largo}
    private final List<Entity[]> marcas = new ArrayList<>();           // {fondo, relleno} de cada carril
    private int salidas;

    Marea(Equipo eq, Mareas.Tipo tipo, int numero, Mareas.TipoPk pk) {
        this.eq = eq; this.tipo = tipo; this.numero = numero; this.pk = pk; this.rnd = eq.rnd;
        this.dur = switch (tipo) {
            case MICHIS, TIPO -> Mareas.DURACION * 20;
            case CERCO -> AVISO_CERCO + 25;
            case ESTAMPIDA -> AVISO_ESTAMPIDA + Mareas.porCarril(eq.tick / 20.0) * SEPARACION_SALIDAS + 4;
        };
    }

    String titulo() { return Mareas.titulo(tipo, pk); }

    int segundosRestantes() { return Math.max(0, (dur - t + 19) / 20); }

    double factorRitmo() { return Mareas.factorRitmo(tipo); }

    boolean enemigosPropios() { return tipo == Mareas.Tipo.MICHIS || tipo == Mareas.Tipo.TIPO; }

    private Formatting color() {
        String c = tipo == Mareas.Tipo.TIPO && pk != null ? pk.color() : tipo == Mareas.Tipo.MICHIS ? "light_purple" : tipo == Mareas.Tipo.CERCO ? "green" : "red";
        Formatting f = Formatting.byName(c);
        return f == null ? Formatting.GOLD : f;
    }

    private Entity marca(String bloque, double x, double z, double giro, float ancho, float largo, boolean extremo, boolean relleno) {
        Entity e = Marcas.crear(eq, bloque, x, relleno ? Marcas.Y_RELLENO : Marcas.Y_FONDO, z, giro, ancho, relleno ? 1f : 0.05f, largo, extremo);
        if (e != null) mias.add(e);
        return e;
    }

    private void quitar(Entity e) { if (e == null) return; mias.remove(e); Marcas.quitar(eq, e); }

    void limpiar() {
        for (Entity e : new ArrayList<>(mias)) Marcas.quitar(eq, e);
        mias.clear();
    }

    /** Un tick de la marea; true cuando ha terminado. */
    boolean tick(List<Run> objetivos) {
        t++;
        if (t == 1) anunciar();
        switch (tipo) {
            case MICHIS, TIPO -> { return t >= dur; }
            case CERCO -> { return cerco(objetivos); }
            case ESTAMPIDA -> { return estampida(objetivos); }
        }
        return true;
    }

    private void anunciar() {
        for (Run m : eq.miembros) {
            ServerPlayerEntity p = m.jugador();
            if (p == null) continue;
            Msg.title(p, Text.literal(titulo()).formatted(color(), Formatting.BOLD), Text.literal(Mareas.subtitulo(tipo)).formatted(Formatting.YELLOW), 5, 50, 15);
            p.playSoundToPlayer(SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.MASTER, 0.45f, 1.0f);
        }
        if (Boolean.getBoolean("emi.survivors.debug")) EmiSurvivors.LOG.info("[marea] {}: {} (marea {}) a los {} s", eq.nombres(), titulo(), numero, eq.segundos());
    }

    // ------------------------------------------------------------------------------------------------ cerco

    private boolean cerco(List<Run> objetivos) {
        if (t == 1) {
            double sx = 0, sz = 0; int n = 0;
            for (Run m : objetivos) { sx += m.pp.x; sz += m.pp.z; n++; }
            if (n == 0) { sx = eq.ox; sz = eq.oz; n = 1; }
            cx = sx / n; cz = sz / n; ang0 = rnd.nextDouble() * Math.PI * 2;
            carril = marca(Marcas.CRISTAL_VERDE, cx, cz, Math.PI / 2 - ang0, 12f, 26f, true, false);
        }
        if (t < AVISO_CERCO) return false;
        if (t == AVISO_CERCO) {
            int total = Mareas.cerco(eq.tick / 20.0, eq.miembros.size());
            double medio = Math.toRadians(30);
            for (int k = 0; k < total; k++) {
                double a = ang0 + medio + (Math.PI * 2 - 2 * medio) * (k + 0.5) / total, r = 13.5 + rnd.nextDouble() * 2.5;
                double x = cx + Math.cos(a) * r, z = cz + Math.sin(a) * r;
                if (Arena.dentro(eq, x, z, 2.5)) pendientes.add(new double[]{x, z});
            }
            quitar(carril);
        }
        for (int k = 0; k < 4 && !pendientes.isEmpty(); k++) {
            double[] p = pendientes.remove(pendientes.size() - 1);
            eq.crearNormalEn(p[0], p[1]);
        }
        return pendientes.isEmpty();
    }

    // ------------------------------------------------------------------------------------------------ estampida

    private static boolean separado(List<Double> l, double d) { for (double x : l) if (Math.abs(x - d) < 9) return false; return true; }

    private boolean estampida(List<Run> objetivos) {
        if (t == 1) {
            double a = rnd.nextDouble() * Math.PI * 2, ux = Math.cos(a), uz = Math.sin(a), px = -uz, pz = ux;
            int n = Mareas.carriles(eq.miembros.size());
            List<Double> desp = new ArrayList<>();
            List<Run> base = new ArrayList<>(objetivos);
            Collections.shuffle(base, rnd);
            for (Run m : base) {                                                          // un carril pasa por donde esta cada jugador ahora
                if (desp.size() >= n) break;
                double d = Math.max(-30, Math.min(30, (m.pp.x - eq.ox) * px + (m.pp.z - eq.oz) * pz));
                if (separado(desp, d)) desp.add(d);
            }
            for (int guard = 0; desp.size() < n && guard < 60; guard++) { double d = (rnd.nextDouble() - 0.5) * 56; if (separado(desp, d)) desp.add(d); }
            for (double d : desp) {
                double h = Math.sqrt(Math.max(36, (Arena.RADIO - 1.5) * (Arena.RADIO - 1.5) - d * d));
                double x0 = eq.ox + px * d - ux * h, z0 = eq.oz + pz * d - uz * h, giro = Math.atan2(ux, uz);
                carriles.add(new double[]{x0, z0, ux, uz, 2 * h});
                marcas.add(new Entity[]{marca(Marcas.CRISTAL_ROJO, x0, z0, giro, 5f, (float) (2 * h), true, false), marca(Marcas.ALFOMBRA_ROJA, x0, z0, giro, 5f, 0.3f, true, true)});
            }
        }
        if (t == 2) for (int k = 0; k < carriles.size(); k++) {
            double[] c = carriles.get(k);
            Marcas.poner(marcas.get(k)[1], c[0], Marcas.Y_RELLENO, c[1], Math.atan2(c[2], c[3]), 5f, 1f, (float) c[4], true, AVISO_ESTAMPIDA - 2);
        }
        if (t < AVISO_ESTAMPIDA) return false;
        if (t == AVISO_ESTAMPIDA) {
            for (Entity[] m : marcas) { quitar(m[0]); quitar(m[1]); }
            marcas.clear();
            sonido();
        }
        int ultimo = Mareas.porCarril(eq.tick / 20.0);
        if ((t - AVISO_ESTAMPIDA) % SEPARACION_SALIDAS == 0 && salidas < ultimo) {
            for (double[] c : carriles) eq.crearCorredor(c[0], c[1], c[2], c[3], c[4]);
            salidas++;
        }
        return salidas >= ultimo;
    }

    private void sonido() {
        for (Run m : eq.miembros) { ServerPlayerEntity p = m.jugador(); if (p != null) p.playSoundToPlayer(SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.MASTER, 0.5f, 1.2f); }
    }
}
