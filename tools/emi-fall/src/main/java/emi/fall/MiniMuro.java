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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * «Muro fantasma»: en un pasillo de Halloween llegan muros morados con uno o dos agujeros, una vez desde un lado y otra desde el otro. Hay que colocarse delante de un agujero antes de
 * que pase el muro: quien lo toca queda eliminado (sin eliminaciones: el muro lo atraviesa y cuenta un golpe). Los muros van cada vez mas deprisa y con agujeros mas estrechos. Gana
 * quien esquiva mas muros.
 */
final class MiniMuro implements Mini {
    static final int ZC = 1800;
    static final int Z0 = ZC - 4, Z1 = ZC + 36;          // el pasillo ocupa z = Z0..Z1
    static final int ANCHO = 9;                          // x = -9..9
    static final int MUROS = 12;
    static final double GROSOR = 1.2, ALTO = 9.0, ALTO_AGUJERO = 4.5;
    static final int PAUSA = 30;

    private static final class Pieza { Entity e; double x, y, sx, sy; }

    private final List<Pieza> piezas = new ArrayList<>();
    private final List<double[]> agujeros = new ArrayList<>();          // {centro x, ancho}
    private final Set<Jug> tocados = new HashSet<>();
    private int numero;                                                  // cuantos muros han salido
    private boolean enCamino;
    private double zMuro, zAnterior, vel;
    private int dir = 1, espera;

    @Override public String id() { return "muro"; }
    @Override public String nombre() { return "Muro fantasma"; }
    @Override public String regla() { return "Llegan muros con agujeros: colócate delante de uno antes de que pase. Quien toca el muro cae. Gana quien esquive más muros."; }
    @Override public String[] pasos() {
        return new String[]{"Un muro fantasma se acerca por el pasillo (a veces de un lado, a veces del otro).",
                "Colócate delante de un agujero antes de que llegue: si lo tocas, te echa fuera.",
                "Cada vez va más rápido y los agujeros son más estrechos. Gana quien esquive más muros."};
    }
    @Override public String objetivo() { return "Pasa por el agujero del muro"; }
    @Override public String musica() { return "tension"; }
    @Override public int vivo(Ctx c, Jug j) { return j.cuenta; }
    @Override public String unidad() { return "muros esquivados"; }
    @Override public double[] zona() { return new double[]{0, ZC + 16, 40}; }

    @Override
    public void construir(ServerWorld w) {
        BlockPos marca = new BlockPos(0, -64, ZC);
        if (w.getBlockState(marca).isOf(Blocks.CRYING_OBSIDIAN)) return;
        int S = Minijuegos.SUELO;
        for (int x = -ANCHO - 1; x <= ANCHO + 1; x++) for (int z = Z0 - 2; z <= Z1 + 2; z++) {
            boolean borde = Math.abs(x) >= ANCHO + 1 || z < Z0 || z > Z1;
            Obra.poner(w, x, S, z, borde ? "blackstone" : (((x + z) & 1) == 0 ? "white_concrete" : "light_gray_concrete"));
            Obra.poner(w, x, S - 1, z, "black_concrete");
        }
        // vallas bajas y barreras invisibles para no salirse
        for (int z = Z0 - 2; z <= Z1 + 2; z++) for (int s = -1; s <= 1; s += 2) {
            Obra.poner(w, s * (ANCHO + 1), S + 1, z, "polished_blackstone_brick_wall");
            for (int y = S + 2; y <= S + 12; y++) Obra.poner(w, s * (ANCHO + 1), y, z, "barrier");
        }
        for (int x = -ANCHO - 1; x <= ANCHO + 1; x++) for (int z : new int[]{Z0 - 2, Z1 + 2}) {
            Obra.poner(w, x, S + 1, z, "polished_blackstone_brick_wall");
            for (int y = S + 2; y <= S + 12; y++) Obra.poner(w, x, y, z, "barrier");
        }
        Disp.texto(w, "{\"text\":\"MURO FANTASMA\",\"color\":\"light_purple\",\"bold\":true}", 0.5, S + 11, ZC + 16.5, 6f, false);
        Obra.poner(w, 0, -64, ZC, "crying_obsidian");
    }

    @Override public void ponerDeco(ServerWorld w) { decorar(w); }

    static void decorar(ServerWorld w) {
        if (!Deco.pendiente(w, ZC)) return;
        int S = Minijuegos.SUELO;
        Random r = new Random(8);
        for (int z = Z0; z <= Z1; z += 6) for (int s = -1; s <= 1; s += 2) Deco.farola(w, s * (ANCHO + 3), S + 1, z, S - 8, s < 0 ? "e" : "w");
        for (int z = Z0 + 2; z <= Z1; z += 8) for (int s = -1; s <= 1; s += 2) Deco.telarañas(w, s * ANCHO, S + 2, z, 2, r);
        Deco.michi(w, -13, S - 1, Z0 - 8, 'x', 2);
        Deco.luna(w, 0, S + 26, Z1 + 14, 9, 'x');
        Deco.pedestal(w, -ANCHO - 6, S, Z1 - 2, "gigante_gatitoalien_witch_hat", "e");
        Deco.pedestal(w, ANCHO + 6, S, Z1 - 2, "gigante_gatitoalien_devil_horns", "w");
        for (int i = 0; i < 4; i++) Deco.lapida(w, (i % 2 == 0 ? -1 : 1) * (ANCHO + 5), S, Z0 + 4 + i * 8, i);
        Deco.marcar(w, ZC);
    }

    private void quitarMuro() {
        for (Pieza p : piezas) Disp.quitar(p.e);
        piezas.clear();
        agujeros.clear();
        enCamino = false;
    }

    @Override
    public void empezar(Ctx c) {
        c.forzar(-16, Z0 - 8, 16, Z1 + 8);
        Disp.barrerMoviles(c.w, 0, ZC + 16, 40);
        quitarMuro();
        tocados.clear();
        numero = 0; espera = Torneo.tt(40);
        int n = c.jugs().size(), i = 0;
        for (Jug j : c.jugs()) {
            j.cuenta = 0; j.caidas = 0;
            c.colocar(j, -7 + 14.0 * (i++ + 0.5) / Math.max(1, n) , Minijuegos.SUELO + 1, ZC + 16 + (i % 2) * 2 + 0.5, 0f);
        }
    }

    @Override public void liberar(Ctx c) { }

    /** Saca el siguiente muro por el lado que toque. */
    private void lanzar(Ctx c) {
        int S = Minijuegos.SUELO;
        numero++;
        dir = numero % 2 == 1 ? 1 : -1;
        zMuro = zAnterior = dir == 1 ? Z0 - 2 : Z1 + 2;
        vel = 0.25 + 0.02 * (numero - 1);
        int nAg = numero <= 3 ? 1 : (numero % 2 == 0 ? 2 : 1);
        double ancho = Math.max(3.4, 5.6 - 0.2 * (numero - 1)) - (nAg == 2 ? 0.8 : 0);
        agujeros.clear();
        double mitadPas = ANCHO + 0.5;
        for (int k = 0; k < nAg; k++) {
            double lo = -mitadPas + ancho / 2 + 0.2 + (nAg == 2 ? k * (2 * mitadPas) / 2 : 0), hi = nAg == 2 ? lo + (2 * mitadPas) / 2 - ancho - 0.4 : mitadPas - ancho / 2 - 0.2;
            agujeros.add(new double[]{lo + c.rnd.nextDouble() * Math.max(0, hi - lo), ancho});
        }
        agujeros.sort((a, b) -> Double.compare(a[0], b[0]));
        // las piezas: lo que hay entre agujeros (de arriba abajo del agujero hasta el techo) y una franja por encima de todo
        double borde = -mitadPas;
        List<double[]> tramos = new ArrayList<>();
        for (double[] a : agujeros) { double ini = a[0] - a[1] / 2; if (ini > borde + 0.05) tramos.add(new double[]{borde, ini}); borde = a[0] + a[1] / 2; }
        if (borde < mitadPas - 0.05) tramos.add(new double[]{borde, mitadPas});
        for (double[] t : tramos) piezas.add(pieza(c, (t[0] + t[1]) / 2, S + 1 + ALTO_AGUJERO / 2 + 0.01, t[1] - t[0], ALTO_AGUJERO));
        piezas.add(pieza(c, 0, S + 1 + ALTO_AGUJERO + (ALTO - ALTO_AGUJERO) / 2, 2 * mitadPas, ALTO - ALTO_AGUJERO));
        tocados.clear();
        enCamino = true;
        c.sfx("muro", 0.8f, 0.8f + 0.04f * numero);
        c.titulo(Text.literal(""), Text.literal("Muro " + numero + "/" + MUROS).formatted(Formatting.LIGHT_PURPLE), 0, 20, 5);
    }

    private Pieza pieza(Ctx c, double x, double y, double sx, double sy) {
        Pieza p = new Pieza(); p.x = x; p.y = y; p.sx = sx; p.sy = sy;
        p.e = Disp.bloque(c.w, "minecraft:purple_concrete", x, y, zMuro, (float) sx, (float) sy, (float) GROSOR, 0, 0);
        return p;
    }

    @Override
    public boolean tick(Ctx c) {
        int S = Minijuegos.SUELO;
        if (!enCamino) {
            if (espera > 0) espera--;
            else if (numero >= MUROS) { c.t.barra(0f); return true; }
            else lanzar(c);
        } else {
            zAnterior = zMuro;
            zMuro += dir * vel;
            for (Pieza p : piezas) Disp.moverBloque(p.e, p.x, p.y, zMuro, (float) p.sx, (float) p.sy, (float) GROSOR, 0, 0);
            if (c.tick % 3 == 0) c.w.spawnParticles(ParticleTypes.SOUL, 0, S + 6, zMuro, 4, ANCHO * 0.8, 2.5, 0.2, 0.01);
            double lo = Math.min(zAnterior, zMuro) - GROSOR / 2 - 0.3, hi = Math.max(zAnterior, zMuro) + GROSOR / 2 + 0.3;
            for (Jug j : c.jugs()) {
                ServerPlayerEntity p = c.pl(j);
                if (p == null || j.fuera || tocados.contains(j) || j.inmune > c.tick) continue;
                if (p.getZ() < lo || p.getZ() > hi || p.getY() > S + 3 + ALTO_AGUJERO) continue;
                boolean libre = false;
                for (double[] a : agujeros) if (Math.abs(p.getX() - a[0]) <= a[1] / 2 - 0.35) libre = true;
                if (libre) continue;
                // ¡golpe!
                tocados.add(j);
                j.caidas++;
                Sonido.sfx(p, "golpe", 1f, 0.8f);
                Sonido.sfx(p, "boo", 0.7f, 1.2f);
                c.w.spawnParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
                if (c.sinElim()) {
                    c.reaparecer(j, p.getX(), p.getY(), zMuro - dir * (GROSOR / 2 + 1.5), p.getYaw());
                    j.inmune = c.tick + 20;
                    c.avisar(Text.literal("¡" + j.nombre + " atravesó el muro! (golpe " + j.caidas + ")").formatted(Formatting.LIGHT_PURPLE));
                } else {
                    j.cuenta = Math.max(0, numero - 1) ;
                    c.eliminar(j);
                    c.avisar(Text.literal("☠ " + j.nombre + " ha chocado con el muro").formatted(Formatting.RED));
                    Msg.title(p, Text.literal("¡ELIMINADO!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Esquivaste " + j.cuenta + " muros").formatted(Formatting.GRAY), 3, 40, 10);
                    p.teleport(c.w, 0.5, S + 9, ZC + 16.5, p.getYaw(), 0f);
                }
            }
            if ((dir == 1 && zMuro > Z1 + 3) || (dir == -1 && zMuro < Z0 - 3)) {
                // el muro termina: los que no lo han tocado suman uno
                for (Jug j : c.jugs()) if (!j.fuera && !tocados.contains(j) && c.pl(j) != null) j.cuenta++;
                quitarMuro();
                espera = Torneo.tt(PAUSA);
                c.sfx("puntos", 0.4f, 1.3f);
            }
        }
        c.t.barra(1f - (float) (numero - (enCamino ? 1 : 0)) / MUROS);
        if (!c.sinElim()) {
            int vivos = c.vivos().size();
            if (vivos == 0 || (c.jugs().size() > 1 && vivos <= 1)) return true;
        }
        return false;
    }

    @Override
    public List<Jug> clasificacion(Ctx c) {
        List<Jug> l = c.jugs();
        l.sort((a, b) -> {
            if (c.sinElim()) return a.caidas != b.caidas ? Integer.compare(a.caidas, b.caidas) : Integer.compare(b.cuenta, a.cuenta);
            if (a.fuera != b.fuera) return a.fuera ? 1 : -1;
            return Integer.compare(b.ordenFuera, a.ordenFuera);
        });
        return l;
    }

    @Override public void limpiar(Ctx c) { quitarMuro(); }

    @Override
    public String hud(Ctx c, Jug j) {
        return "Muro " + Math.max(1, numero) + "/" + MUROS + " · esquivados " + j.cuenta + (c.sinElim() ? " · golpes " + j.caidas : j.fuera ? " · eliminado" : "");
    }
}
