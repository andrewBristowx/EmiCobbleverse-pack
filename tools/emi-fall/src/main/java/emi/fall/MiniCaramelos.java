package emi.fall;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * «Cazacaramelos»: una plaza redonda de Halloween llena de dulces flotando. Pasa por encima para cogerlos: caramelos y galletas valen 1, la estrella dorada vale 5 y las
 * calaveras de la trampa te quitan 2. Cuando uno se coge, sale otro en otro sitio. Gana quien tenga mas dulces al acabar el tiempo. No elimina a nadie.
 */
final class MiniCaramelos implements Mini {
    static final int ZC = 1200;
    static final double R = 16.5;
    static final int LIMITE_S = 60;
    static final int NORMALES = 24, TRAMPAS = 5;

    static final String[] DULCES = {"cobblemon:rare_candy", "minecraft:cookie", "minecraft:pumpkin_pie", "minecraft:sweet_berries", "minecraft:honey_bottle"};

    private static final class Dulce { Entity e; double x, z; int tipo; long caduca; }   // tipo 0 = normal, 1 = estrella, 2 = trampa

    private final List<Dulce> dulces = new ArrayList<>();
    private long proxEstrella;

    @Override public String id() { return "caramelos"; }
    @Override public String nombre() { return "Cazacaramelos"; }
    @Override public String regla() { return "Pasa por encima de los dulces para cogerlos: la estrella vale 5, ¡pero las calaveras te quitan 2! Gana quien más junte en 60 segundos."; }
    @Override public String[] pasos() {
        return new String[]{"Corre por la plaza y pasa por encima de los dulces para cogerlos (valen 1 cada uno).",
                "La estrella dorada vale 5, pero salen pocas. ¡Las calaveras te quitan 2 dulces: esquívalas!",
                "Gana quien tenga más dulces cuando acaben los 60 segundos. Aquí no te elimina nadie."};
    }
    @Override public String objetivo() { return "Junta más dulces que nadie"; }
    @Override public String musica() { return "fiesta"; }
    @Override public int vivo(Ctx c, Jug j) { return j.cuenta; }
    @Override public String unidad() { return "dulces"; }
    @Override public double[] zona() { return new double[]{0, ZC, 30}; }

    @Override
    public void construir(ServerWorld w) {
        BlockPos marca = new BlockPos(0, -64, ZC);
        if (w.getBlockState(marca).isOf(Blocks.CRYING_OBSIDIAN)) return;
        int S = Minijuegos.SUELO;
        int n = (int) Math.ceil(R) + 1;
        for (int dx = -n; dx <= n; dx++) for (int dz = -n; dz <= n; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > R) continue;
            String b = d < 3.2 ? "purple_concrete" : d > R - 1.2 ? "blackstone" : (((dx + dz) & 1) == 0 ? "orange_concrete" : "black_concrete");
            Obra.poner(w, dx, S, ZC + dz, b);
            Obra.poner(w, dx, S - 1, ZC + dz, "black_concrete");
        }
        Disp.texto(w, "{\"text\":\"CAZACARAMELOS\",\"color\":\"gold\",\"bold\":true}", 0.5, S + 9, ZC + 0.5, 6f, false);
        Obra.poner(w, 0, -64, ZC, "crying_obsidian");
    }

    @Override public void ponerDeco(ServerWorld w) { decorar(w); }

    static void decorar(ServerWorld w) {
        if (!Deco.pendiente(w, ZC)) return;
        int S = Minijuegos.SUELO;
        Random r = new Random(12);
        Deco.anilloBarrera(w, 0, ZC, R + 0.5, S + 1, S + 4);
        Deco.anilloFarolas(w, 0, ZC, R + 1.5, 12, S + 1, S - 6);
        for (int i = 0; i < 5; i++) { double a = 2 * Math.PI * i / 5 + 0.3; Deco.arbolSeco(w, (int) Math.round(Math.cos(a) * (R + 5)), S - 5, ZC + (int) Math.round(Math.sin(a) * (R + 5)), 8, r); }
        for (int i = 0; i < 14; i++) { double a = r.nextDouble() * 2 * Math.PI; Deco.telarañas(w, (int) Math.round(Math.cos(a) * (R - 0.5)), S + 1, ZC + (int) Math.round(Math.sin(a) * (R - 0.5)), 2, r); }
        Deco.pedestal(w, 0, S, ZC, "gigante_gatitoalien_witch_hat", "s");
        Deco.michi(w, -13, S - 2, ZC - 34, 'x', 2);
        Deco.luna(w, 26, S + 30, ZC - 20, 9, 'z');
        Deco.marcar(w, ZC);
    }

    @Override
    public void empezar(Ctx c) {
        c.forzar(-24, ZC - 24, 24, ZC + 24);
        Disp.barrerMoviles(c.w, 0, ZC, 30);
        quitarEntidades();
        proxEstrella = Torneo.tt(8 * 20);
        int n = c.jugs().size(), i = 0;
        for (Jug j : c.jugs()) {
            j.cuenta = 0;
            double a = 2 * Math.PI * (i++) / Math.max(1, n);
            double x = Math.cos(a) * 12, z = Math.sin(a) * 12;
            c.colocar(j, x + 0.5, Minijuegos.SUELO + 1, ZC + z + 0.5, (float) Math.toDegrees(Math.atan2(-x, z)) );
        }
        for (int k = 0; k < NORMALES; k++) nuevo(c, 0, 0);
        for (int k = 0; k < TRAMPAS; k++) nuevo(c, 2, c.rnd.nextInt(Torneo.tt(8 * 20)));
    }

    private void quitarEntidades() {
        for (Dulce d : dulces) Disp.quitar(d.e);
        dulces.clear();
    }

    /** Crea un dulce de ese tipo en un sitio libre. {@code vida} = ticks que dura (0 = hasta que lo cojan). */
    private void nuevo(Ctx c, int tipo, int vidaExtra) {
        int S = Minijuegos.SUELO;
        Dulce d = new Dulce();
        d.tipo = tipo;
        for (int intento = 0; intento < 20; intento++) {
            double a = c.rnd.nextDouble() * 2 * Math.PI, r = Math.sqrt(c.rnd.nextDouble()) * (R - 2.5);
            d.x = Math.cos(a) * r + 0.5; d.z = ZC + Math.sin(a) * r + 0.5;
            boolean libre = true;
            for (Dulce o : dulces) if ((o.x - d.x) * (o.x - d.x) + (o.z - d.z) * (o.z - d.z) < 2.5) { libre = false; break; }
            if (libre) break;
        }
        String item = tipo == 0 ? DULCES[c.rnd.nextInt(DULCES.length)] : tipo == 1 ? "minecraft:nether_star" : "minecraft:skeleton_skull";
        float esc = tipo == 1 ? 1.6f : 1.0f;
        d.e = Disp.objeto(c.w, item, d.x, S + 1.5, d.z, esc, true);
        d.caduca = tipo == 0 ? 0 : c.tick + (tipo == 1 ? Torneo.tt(9 * 20) : Torneo.tt(7 * 20) + vidaExtra);
        dulces.add(d);
    }

    @Override public void liberar(Ctx c) { }

    @Override
    public boolean tick(Ctx c) {
        int S = Minijuegos.SUELO;
        int normales = 0, trampas = 0;
        boolean hayEstrella = false;
        for (int i = dulces.size() - 1; i >= 0; i--) {
            Dulce d = dulces.get(i);
            if (d.e == null || d.e.isRemoved()) { dulces.remove(i); continue; }
            if (d.tipo != 0 && c.tick >= d.caduca) { Disp.quitar(d.e); dulces.remove(i); continue; }
            if (d.tipo == 0) normales++; else if (d.tipo == 1) hayEstrella = true; else trampas++;
            if (c.tick % 8 == (i & 7) && d.tipo != 0) c.w.spawnParticles(d.tipo == 1 ? ParticleTypes.END_ROD : ParticleTypes.SOUL_FIRE_FLAME, d.x, S + 1.5, d.z, 2, 0.25, 0.3, 0.25, 0.01);
        }
        // jugadores que cogen dulces
        for (Jug j : c.jugs()) {
            ServerPlayerEntity p = c.pl(j);
            if (p == null) continue;
            if (p.getY() < S - 3) c.reaparecer(j, j.sx, j.sy, j.sz, j.syaw);              // por si alguien se sale
            for (int i = dulces.size() - 1; i >= 0; i--) {
                Dulce d = dulces.get(i);
                double dx = p.getX() - d.x, dz = p.getZ() - d.z;
                if (dx * dx + dz * dz > 1.7 || Math.abs(p.getY() - (S + 1)) > 2.5) continue;
                Disp.quitar(d.e);
                dulces.remove(i);
                if (d.tipo == 0) { j.cuenta++; normales--; Sonido.sfx(p, "recoger", 0.7f, 0.9f + c.rnd.nextFloat() * 0.3f); }
                else if (d.tipo == 1) { j.cuenta += 5; hayEstrella = false; Sonido.sfx(p, "oro", 0.9f, 1f); c.w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, d.x, S + 1.8, d.z, 18, 0.4, 0.5, 0.4, 0.3); }
                else { j.cuenta = Math.max(0, j.cuenta - 2); trampas--; Sonido.sfx(p, "trampa", 0.9f, 1f); c.w.spawnParticles(ParticleTypes.SMOKE, d.x, S + 1.5, d.z, 12, 0.3, 0.3, 0.3, 0.03); }
            }
        }
        // reponer: siempre hay el mismo numero de dulces y de trampas; la estrella sale de vez en cuando
        for (int k = normales; k < NORMALES; k++) nuevo(c, 0, 0);
        for (int k = trampas; k < TRAMPAS; k++) nuevo(c, 2, c.rnd.nextInt(40));
        if (!hayEstrella && c.tick >= proxEstrella) { nuevo(c, 1, 0); proxEstrella = c.tick + Torneo.tt(11 * 20); }
        c.t.barra(1f - (float) c.tick / Torneo.tt(LIMITE_S * 20));
        return c.tick >= Torneo.tt(LIMITE_S * 20);
    }

    @Override
    public List<Jug> clasificacion(Ctx c) {
        List<Jug> l = c.jugs();
        l.sort((a, b) -> Integer.compare(b.cuenta, a.cuenta));
        return l;
    }

    @Override public void limpiar(Ctx c) { quitarEntidades(); }

    @Override
    public String hud(Ctx c, Jug j) {
        int resta = Math.max(0, Torneo.tt(LIMITE_S * 20) / 20 - c.tick / 20);
        return "Dulces: " + j.cuenta + " · " + resta + " s";
    }
}
