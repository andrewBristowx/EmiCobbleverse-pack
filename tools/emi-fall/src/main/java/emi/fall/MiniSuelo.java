package emi.fall;

import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * «Suelo embrujado» (al estilo de Hex-a-gone): tres pisos de baldosas de 3×3 de calabaza, tumba y hielo. Cada baldosa que pisas se pone roja y a los 0,4 s desaparece: tienes que
 * seguir moviendote. Con el tiempo tambien se rompen solas. Quien cae por debajo del ultimo piso queda eliminado (sin eliminaciones: reaparece en el piso mas alto que quede y
 * pierde la racha). Gana el ultimo en pie (sin eliminaciones: quien menos cae y mas tiempo aguanta).
 */
final class MiniSuelo implements Mini {
    static final int ZC = 1500;
    static final int TILE = 3, MITAD = 4;               // baldosas de 3x3; indices -4..4
    static final double RADIO_BALDOSAS = 4.35;
    static final int PISOS = 3, ALTO_PISO = 7;
    static final int ROMPE_TRAS = 8;                    // ticks entre pisar y desaparecer
    static final int LIMITE_S = 150, LIMITE_SIN_S = 90;

    private static final String[][] COLORES = {{"orange_concrete", "black_concrete"}, {"purple_concrete", "magenta_concrete"}, {"light_blue_concrete", "blue_concrete"}};
    private static final int N = 2 * MITAD + 1;

    private final boolean[][] existe = new boolean[PISOS][N * N];     // la baldosa forma parte del suelo
    private final boolean[][] viva = new boolean[PISOS][N * N];       // sigue puesta
    private final long[][] rompe = new long[PISOS][N * N];            // tick en que desaparece (0 = nadie la ha pisado)
    private long proxDecaer;
    private int limite;

    @Override public String id() { return "suelo"; }
    @Override public String nombre() { return "Suelo embrujado"; }
    @Override public String regla() { return "Las baldosas desaparecen poco después de pisarlas: no te quedes quieto. Si caes por todos los pisos, quedas fuera. ¡Aguanta más que nadie!"; }
    @Override public String[] pasos() {
        return new String[]{"Cada baldosa que pisas se pone roja y desaparece enseguida: ¡muévete sin parar!",
                "Hay tres pisos. Si caes al de abajo, sigues; si caes del último, te quedas fuera.",
                "Gana el último que quede en pie. Con el tiempo las baldosas también se rompen solas."};
    }
    @Override public String objetivo() { return "No te quedes quieto"; }
    @Override public String musica() { return "carrera"; }
    @Override public int vivo(Ctx c, Jug j) { return j.cuenta; }
    @Override public String unidad() { return "segundos a salvo"; }
    @Override public double[] zona() { return null; }

    private static int idx(int tx, int tz) { return (tx + MITAD) * N + (tz + MITAD); }

    private static boolean dentro(int tx, int tz) { return Math.hypot(tx, tz) <= RADIO_BALDOSAS; }

    private static int yPiso(int k) { return Minijuegos.SUELO - ALTO_PISO * k; }

    @Override
    public void construir(ServerWorld w) {
        BlockPos marca = new BlockPos(0, -64, ZC);
        if (w.getBlockState(marca).isOf(Blocks.CRYING_OBSIDIAN)) return;
        int S = Minijuegos.SUELO;
        Disp.texto(w, "{\"text\":\"SUELO EMBRUJADO\",\"color\":\"gold\",\"bold\":true}", 0.5, S + 10, ZC + 0.5, 6f, false);
        Obra.poner(w, 0, -64, ZC, "crying_obsidian");
    }

    @Override public void ponerDeco(ServerWorld w) { decorar(w); }

    static void decorar(ServerWorld w) {
        if (!Deco.pendiente(w, ZC)) return;
        int S = Minijuegos.SUELO;
        Random r = new Random(5);
        Deco.anilloFarolas(w, 0, ZC, 20.5, 14, S + 1, S - 26);
        Deco.michi(w, -13, S - 8, ZC - 40, 'x', 2);
        Deco.luna(w, -30, S + 34, ZC + 10, 10, 'z');
        for (int i = 0; i < 6; i++) { double a = 2 * Math.PI * i / 6 + 0.4; Deco.arbolSeco(w, (int) Math.round(Math.cos(a) * 27), S - 14, ZC + (int) Math.round(Math.sin(a) * 27), 12, r); }
        Deco.pedestal(w, 0, S + 12, ZC - 24, "gigante_gatitoalien_devil_horns", "s");
        Deco.marcar(w, ZC);
    }

    private void ponerBaldosa(ServerWorld w, int k, int tx, int tz, String bloque) {
        int y = yPiso(k);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) Obra.poner(w, tx * TILE + dx, y, ZC + tz * TILE + dz, bloque);
    }

    private void ponerTodo(ServerWorld w) {
        for (int k = 0; k < PISOS; k++) for (int tx = -MITAD; tx <= MITAD; tx++) for (int tz = -MITAD; tz <= MITAD; tz++) {
            int i = idx(tx, tz);
            existe[k][i] = dentro(tx, tz);
            viva[k][i] = existe[k][i];
            rompe[k][i] = 0;
            ponerBaldosa(w, k, tx, tz, existe[k][i] ? COLORES[k][(tx + tz) & 1] : "air");
        }
    }

    @Override
    public void empezar(Ctx c) {
        c.forzar(-18, ZC - 18, 18, ZC + 18);
        limite = Torneo.tt((c.sinElim() ? LIMITE_SIN_S : LIMITE_S) * 20);
        ponerTodo(c.w);
        proxDecaer = Torneo.tt(35 * 20);
        List<int[]> sitios = new ArrayList<>();
        for (int tx = -3; tx <= 3; tx++) for (int tz = -3; tz <= 3; tz++) if (dentro(tx, tz)) sitios.add(new int[]{tx, tz});
        Collections.shuffle(sitios, c.rnd);
        int i = 0;
        for (Jug j : c.jugs()) {
            int[] s = sitios.get(i++ % sitios.size());
            j.cuenta = 0; j.caidas = 0;
            c.colocar(j, s[0] * TILE + 0.5, yPiso(0) + 1, ZC + s[1] * TILE + 0.5, c.rnd.nextInt(360));
        }
    }

    @Override public void liberar(Ctx c) { }

    /** Marca una baldosa para que desaparezca dentro de {@code en} ticks (y la pone roja). */
    private void marcar(Ctx c, int k, int tx, int tz, int en) {
        int i = idx(tx, tz);
        if (!existe[k][i] || !viva[k][i] || rompe[k][i] != 0) return;
        rompe[k][i] = c.tick + en;
        ponerBaldosa(c.w, k, tx, tz, "red_concrete");
        c.w.playSound(null, tx * TILE + 0.5, yPiso(k) + 1, ZC + tz * TILE + 0.5, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.6f, 1.5f);
    }

    /** El piso mas alto que aun tiene baldosas vivas (o -1). */
    private int pisoAlto() {
        for (int k = 0; k < PISOS; k++) for (int i = 0; i < N * N; i++) if (viva[k][i]) return k;
        return -1;
    }

    private int[] baldosaViva(Ctx c, int k) {
        List<int[]> l = new ArrayList<>();
        for (int tx = -MITAD; tx <= MITAD; tx++) for (int tz = -MITAD; tz <= MITAD; tz++) if (viva[k][idx(tx, tz)] && rompe[k][idx(tx, tz)] == 0) l.add(new int[]{tx, tz});
        return l.isEmpty() ? null : l.get(c.rnd.nextInt(l.size()));
    }

    @Override
    public boolean tick(Ctx c) {
        int S = Minijuegos.SUELO;
        // baldosas que desaparecen
        for (int k = 0; k < PISOS; k++) for (int tx = -MITAD; tx <= MITAD; tx++) for (int tz = -MITAD; tz <= MITAD; tz++) {
            int i = idx(tx, tz);
            if (rompe[k][i] != 0 && viva[k][i] && c.tick >= rompe[k][i]) {
                viva[k][i] = false;
                ponerBaldosa(c.w, k, tx, tz, "air");
                c.w.spawnParticles(ParticleTypes.CLOUD, tx * TILE + 0.5, yPiso(k) + 0.6, ZC + tz * TILE + 0.5, 6, 0.9, 0.1, 0.9, 0.02);
                c.w.playSound(null, tx * TILE + 0.5, yPiso(k), ZC + tz * TILE + 0.5, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 0.8f, 0.8f);
            }
        }
        // las baldosas se rompen solas cuando pasa un rato (asi la partida siempre acaba)
        if (c.tick >= proxDecaer) {
            int k = pisoAlto();
            if (k >= 0) {
                int[] b = baldosaViva(c, k);
                if (b != null) marcar(c, k, b[0], b[1], 14);
            }
            proxDecaer = c.tick + Math.max(10, 40 - (c.tick - Torneo.tt(35 * 20)) / 25);
        }
        int vivosN = 0;
        for (Jug j : c.jugs()) {
            ServerPlayerEntity p = c.pl(j);
            if (p == null || j.fuera) continue;
            // pisar: la baldosa que tienes debajo
            if (p.isOnGround()) {
                int by = (int) Math.floor(p.getY() - 0.05);
                int k = (S - by) % ALTO_PISO == 0 ? (S - by) / ALTO_PISO : -1;
                if (k >= 0 && k < PISOS) {
                    int tx = Math.floorDiv((int) Math.floor(p.getX()) + 1, TILE), tz = Math.floorDiv((int) Math.floor(p.getZ()) - ZC + 1, TILE);
                    if (Math.abs(tx) <= MITAD && Math.abs(tz) <= MITAD) marcar(c, k, tx, tz, ROMPE_TRAS);
                }
            }
            // se cae por el ultimo piso
            if (p.getY() < yPiso(PISOS - 1) - 4) {
                Sonido.sfx(p, "caida", 0.9f, 1f);
                if (c.sinElim()) {
                    j.caidas++; j.cuenta = 0;
                    int k = pisoAlto();
                    int[] b = k < 0 ? null : baldosaViva(c, k);
                    if (b != null) c.recuperar(j, b[0] * TILE + 0.5, yPiso(k) + 1, ZC + b[1] * TILE + 0.5, p.getYaw());
                    else c.recuperar(j, 0.5, yPiso(0) + 1, ZC + 0.5, p.getYaw());
                    c.avisar(Text.literal("↓ " + j.nombre + " ha caído (vuelve al juego)").formatted(Formatting.RED));
                    Msg.title(p, Text.literal("¡CAÍSTE!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Vuelves al piso más alto que queda").formatted(Formatting.GRAY), 3, 30, 10);
                } else {
                    j.cuenta = c.segundos();
                    c.eliminar(j);
                    c.avisar(Text.literal("☠ " + j.nombre + " ha caído").formatted(Formatting.RED));
                    Msg.title(p, Text.literal("¡ELIMINADO!").formatted(Formatting.RED, Formatting.BOLD), Text.literal("Aguantaste " + j.cuenta + " s").formatted(Formatting.GRAY), 3, 40, 10);
                    p.teleport(c.w, p.getX() * 0.3, S + 14, ZC + (p.getZ() - ZC) * 0.3, p.getYaw(), 0f);
                }
                continue;
            }
            vivosN++;
        }
        if (c.tick % 20 == 0) for (Jug j : c.jugs()) if (!j.fuera) j.cuenta = c.sinElim() ? j.cuenta + 1 : c.segundos();
        c.t.barra(1f - (float) c.tick / limite);
        if (c.tick >= limite) return true;
        if (!c.sinElim() && c.jugs().size() > 1 && vivosN <= 1) return true;
        return !c.sinElim() && vivosN == 0;
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

    @Override public void limpiar(Ctx c) { }

    @Override
    public String hud(Ctx c, Jug j) {
        if (c.sinElim()) return "A salvo: " + j.cuenta + " s · caídas " + j.caidas;
        return j.fuera ? "Eliminado · aguantaste " + j.cuenta + " s" : "Quedan " + c.vivos().size() + " · " + c.segundos() + " s";
    }
}
