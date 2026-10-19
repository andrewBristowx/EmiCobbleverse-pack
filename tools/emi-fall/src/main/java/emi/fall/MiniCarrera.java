package emi.fall;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * «Carrera de obstaculos»: seis tramos hasta la meta: piedras sueltas para saltar, una barra giratoria, puertas de las que solo la mitad se abre, un abismo que se cruza
 * con un trampolin, bolas gigantes que ruedan hacia ti y dos barras giratorias seguidas. Si te caes reapareces en el ultimo punto de control; los golpes de las barras y
 * las bolas solo te empujan. Gana quien llega antes a la meta.
 */
final class MiniCarrera implements Mini {
    static final int ZC = 0;
    static final int[] CP_X = {-9, 25, 53, 80, 91, 153};          // x de cada punto de control (donde se reaparece)
    static final int META_X = 178;
    static final int LIMITE_S = 150, EXTRA_S = 45;
    static final double Y = Minijuegos.SUELO + 1;                 // altura de los pies sobre el suelo

    // tramo 1: piedras (x, z central)
    private static final int[][] PIEDRAS = {{1, 0}, {6, 3}, {11, -3}, {16, 3}, {21, 0}};
    // tramo 4: el abismo (entre el trampolin y la otra orilla)
    static final int PAD_X = 83, ABISMO_A = 84, ABISMO_B = 89, ORILLA = 90;
    // las dos paredes de puertas
    static final int[] PARED_X = {58, 66};
    private static final String[] COLORES = {"red_concrete", "blue_concrete", "lime_concrete", "yellow_concrete"};

    private static final class Barra {
        final double cx, cz, largo, omega, fase; Entity e;
        Barra(double cx, double cz, double largo, double omega, double fase) { this.cx = cx; this.cz = cz; this.largo = largo; this.omega = omega; this.fase = fase; }
        double giro(int tick) { return fase + omega * tick / 20.0; }
    }
    private static final class Bola { Entity e; double x, z; }

    private final List<Barra> barras = new ArrayList<>();
    private final List<Bola> bolas = new ArrayList<>();
    private final boolean[][] real = new boolean[2][4];
    private final boolean[][] abierta = new boolean[2][4];
    private int primero = -1;
    private int proxBola;

    @Override public String id() { return "carrera"; }
    @Override public String nombre() { return "Carrera de obstáculos"; }
    @Override public String regla() { return "Llega a la meta saltando piedras, barras giratorias, puertas falsas, un abismo y bolas rodantes. Si caes, vuelves al último punto de control."; }

    private static void suelo(ServerWorld w, int x0, int x1, int z0, int z1, String a, String b) {
        int S = Minijuegos.SUELO;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) { Obra.poner(w, x, S, z, ((x + z) & 1) == 0 ? a : b); Obra.poner(w, x, S - 1, z, "white_concrete"); }
    }

    @Override
    public void construir(ServerWorld w) {
        BlockPos marca = new BlockPos(0, -64, ZC);
        if (w.getBlockState(marca).isOf(Blocks.CRYING_OBSIDIAN)) return;
        long t0 = System.nanoTime();
        int S = Minijuegos.SUELO;
        suelo(w, -14, -1, -6, 6, "light_blue_concrete", "white_concrete");                            // salida
        for (int[] p : PIEDRAS) suelo(w, p[0], p[0] + 2, p[1] - 1, p[1] + 1, "pink_concrete", "magenta_concrete");   // piedras sueltas
        suelo(w, 24, 50, -6, 6, "light_blue_concrete", "white_concrete");                              // barra 1
        Obra.poner(w, 37, S + 1, ZC, "stone_bricks"); Obra.poner(w, 37, S + 2, ZC, "stone_bricks");
        suelo(w, 51, 76, -6, 6, "yellow_concrete", "white_concrete");                                  // puertas
        suelo(w, 77, 83, -6, 6, "orange_concrete", "white_concrete");                                  // antes del abismo
        for (int z = -6; z <= 6; z++) Obra.poner(w, PAD_X, S, z, "magenta_glazed_terracotta");        // el trampolin (toda la fila)
        suelo(w, ORILLA, 150, -6, 6, "lime_concrete", "white_concrete");                               // la otra orilla y las bolas
        suelo(w, 151, 176, -4, 4, "purple_concrete", "white_concrete");                                // dos barras
        Obra.poner(w, 158, S + 1, ZC, "stone_bricks"); Obra.poner(w, 158, S + 2, ZC, "stone_bricks");
        Obra.poner(w, 168, S + 1, ZC, "stone_bricks"); Obra.poner(w, 168, S + 2, ZC, "stone_bricks");
        suelo(w, META_X, META_X + 12, -6, 6, "yellow_concrete", "lime_concrete");                      // meta
        for (int y = 1; y <= 5; y++) { Obra.poner(w, META_X + 1, S + y, -6, "gold_block"); Obra.poner(w, META_X + 1, S + y, 6, "gold_block"); }
        for (int z = -6; z <= 6; z++) Obra.poner(w, META_X + 1, S + 6, z, (z & 1) == 0 ? "gold_block" : "yellow_concrete");
        Disp.texto(w, "{\"text\":\"META\",\"color\":\"gold\",\"bold\":true}", META_X + 1.5, S + 8.2, ZC + 0.5, 6f, false);
        Disp.texto(w, "{\"text\":\"SALIDA\",\"color\":\"aqua\",\"bold\":true}", -6.5, S + 4.5, ZC + 0.5, 4f, false);
        Disp.texto(w, "{\"text\":\"¡Salta!\",\"color\":\"light_purple\",\"bold\":true}", 12.5, S + 3.5, ZC + 0.5, 3f, false);
        Disp.texto(w, "{\"text\":\"¡Usa el trampolin!\",\"color\":\"light_purple\",\"bold\":true}", PAD_X + 0.5, S + 4.5, ZC + 0.5, 3f, false);
        Obra.poner(w, 0, -64, ZC, "crying_obsidian");
        EmiFall.LOG.info("Arena «carrera» construida en {} ms", (System.nanoTime() - t0) / 1_000_000);
    }

    /** Pone las paredes de puertas (cuatro puertas cada una; la mitad se abre). */
    private void ponerPuertas(ServerWorld w, Random rnd) {
        int S = Minijuegos.SUELO;
        for (int p = 0; p < 2; p++) {
            int a = rnd.nextInt(4), b; do { b = rnd.nextInt(4); } while (b == a);
            for (int k = 0; k < 4; k++) { real[p][k] = k == a || k == b; abierta[p][k] = false; }
            int x = PARED_X[p];
            for (int y = 1; y <= 4; y++) for (int z = -6; z <= 6; z++) Obra.poner(w, x, S + y, z, "air");
            for (int y = 1; y <= 4; y++) for (int k = 0; k <= 4; k++) Obra.poner(w, x, S + y, -6 + 3 * k, "stone_bricks");   // pilares en -6, -3, 0, 3, 6
            for (int k = 0; k < 4; k++) puerta(w, p, k, COLORES[(k + p) % 4], false);
        }
    }

    private static void puerta(ServerWorld w, int pared, int k, String bloque, boolean vaciar) {
        int S = Minijuegos.SUELO, x = PARED_X[pared];
        for (int y = 1; y <= 4; y++) for (int dz = 0; dz < 2; dz++) Obra.poner(w, x, S + y, -5 + 3 * k + dz, vaciar ? "air" : bloque);
    }

    @Override
    public void empezar(Ctx c) {
        primero = -1; proxBola = 0;
        ponerPuertas(c.w, c.rnd);
        quitarEntidades();
        int S = Minijuegos.SUELO;
        double[][] datos = {{37, ZC, 12, 1.5, 0}, {158, ZC, 9, 2.1, 0}, {168, ZC, 9, -2.5, 1.5}};
        for (double[] d : datos) {
            Barra b = new Barra(d[0] + 0.5, d[1] + 0.5, d[2], d[3], d[4]);
            b.e = Disp.bloque(c.w, "minecraft:red_concrete", b.cx, S + 1.4, b.cz, (float) b.largo, 0.5f, 0.5f, b.giro(0), 0);
            barras.add(b);
        }
        int i = 0;
        for (Jug j : c.jugs()) { int n = i++; c.colocar(j, -12 + (n % 3) * 2 + 0.5, Y, -4 + (n / 3) * 2 + 0.5, -90f); }
    }

    private void quitarEntidades() {
        for (Barra b : barras) Disp.quitar(b.e);
        barras.clear();
        for (Bola b : bolas) Disp.quitar(b.e);
        bolas.clear();
    }

    @Override public void liberar(Ctx c) { }

    private int limite() { return primero < 0 ? LIMITE_S * 20 : Math.min(LIMITE_S * 20, primero + EXTRA_S * 20); }

    @Override
    public boolean tick(Ctx c) {
        int S = Minijuegos.SUELO;
        // las barras giran (el cliente anima cada cambio durante 2 ticks)
        if (c.tick % 2 == 0) for (Barra b : barras) Disp.moverBloque(b.e, b.cx, S + 1.4, b.cz, (float) b.largo, 0.5f, 0.5f, b.giro(c.tick + 2), 2);
        // bolas: salen cada ~2,2 s por un carril al azar al fondo y ruedan hacia la salida
        if (c.tick >= proxBola) {
            proxBola = c.tick + 42;
            Bola b = new Bola(); b.x = 149; b.z = -4.5 + 3 * c.rnd.nextInt(4);
            b.e = Disp.bloque(c.w, "minecraft:orange_concrete", b.x, S + 1.65, b.z, 1.3f, 1.3f, 1.3f, 0, 0);
            bolas.add(b);
        }
        for (int i = bolas.size() - 1; i >= 0; i--) {
            Bola b = bolas.get(i);
            b.x -= 0.42;
            if (b.x < ORILLA + 1) { Disp.quitar(b.e); bolas.remove(i); continue; }
            Disp.moverBloque(b.e, b.x, S + 1.65, b.z, 1.3f, 1.3f, 1.3f, (c.tick * 0.35), 1);
        }
        int llegados = 0, jugando = 0;
        for (Jug j : c.jugs()) {
            ServerPlayerEntity p = c.pl(j);
            if (p == null) continue;
            jugando++;
            if (j.llego) { llegados++; continue; }
            double x = p.getX(), y = p.getY(), z = p.getZ();
            if (y < S - 6) { caer(c, j, p); continue; }
            if (x >= META_X && y > S) {
                j.llego = true; j.tLlegada = c.tick; j.fila = META_X;
                if (primero < 0) { primero = c.tick; c.avisar(Msg.say(Text.literal(j.nombre + " ha llegado primero. Los demás tenéis " + EXTRA_S + " segundos.").formatted(Formatting.GOLD))); }
                int puesto = 1; for (Jug o : c.jugs()) if (o != j && o.llego) puesto++;
                p.sendMessage(Text.literal("¡Has llegado el " + puesto + "º!").formatted(Formatting.GREEN, Formatting.BOLD), true);
                p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1f, 1.2f);
                continue;
            }
            if (x > j.fila) j.fila = (int) x;
            for (int k = CP_X.length - 1; k > j.checkpoint; k--) if (x >= CP_X[k] + 1 && y >= S + 0.5) { j.checkpoint = k; p.sendMessage(Text.literal("Punto de control").formatted(Formatting.AQUA), true); break; }
            // trampolin
            if (p.isOnGround() && c.tick - j.tPad > 12 && c.w.getBlockState(new BlockPos((int) Math.floor(x), S, (int) Math.floor(z))).isOf(Blocks.MAGENTA_GLAZED_TERRACOTTA)) {
                j.tPad = c.tick;
                c.empujar(p, new Vec3d(0.7, 1.0, 0));
                p.playSoundToPlayer(SoundEvents.ENTITY_SLIME_JUMP, SoundCategory.MASTER, 1f, 1.2f);
                c.w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.2, z, 10, 0.4, 0.1, 0.4, 0.05);
            }
            // barras giratorias
            for (Barra b : barras) {
                if (Math.abs(x - b.cx) > b.largo / 2 + 1.5 || y > S + 2.0 || c.tick < j.inmune) continue;
                double g = b.giro(c.tick), dx = Math.cos(g), dz = -Math.sin(g), h = b.largo / 2;
                double rx = x - b.cx, rz = z - b.cz;
                double u = Math.max(-h, Math.min(h, rx * dx + rz * dz));
                double qx = rx - u * dx, qz = rz - u * dz;
                if (qx * qx + qz * qz < 0.85 * 0.85 && y < S + 1.8) {
                    j.inmune = c.tick + 14;
                    double px = u * dx, pz = u * dz;                                    // el punto de la barra mas cercano (relativo al eje)
                    double vx = b.omega * pz, vz = -b.omega * px;                      // hacia donde se mueve ese punto de la barra
                    double n = Math.max(0.2, Math.hypot(vx, vz));
                    c.empujar(p, new Vec3d(vx / n * 1.0, 0.45, vz / n * 1.0));
                    p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.MASTER, 1f, 0.9f);
                }
            }
            // bolas
            if (c.tick >= j.inmune) for (Bola b : bolas) {
                if (Math.abs(x - b.x) < 1.15 && Math.abs(z - b.z) < 1.2 && y < S + 2.0) {
                    j.inmune = c.tick + 16;
                    c.empujar(p, new Vec3d(-1.0, 0.5, (z - b.z) * 0.3));
                    p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.MASTER, 1f, 0.7f);
                    break;
                }
            }
            // puertas: la que es de verdad se abre al acercarse
            for (int pa = 0; pa < 2; pa++) {
                if (Math.abs(x - PARED_X[pa]) > 2.6) continue;
                for (int k = 0; k < 4; k++) {
                    if (!real[pa][k] || abierta[pa][k]) continue;
                    if (Math.abs(z - (-4 + 3 * k)) < 1.9) {
                        abierta[pa][k] = true; puerta(c.w, pa, k, "air", true);
                        c.w.spawnParticles(ParticleTypes.POOF, PARED_X[pa] + 0.5, S + 2, -4 + 3 * k + 0.5, 12, 0.3, 1.0, 0.8, 0.05);
                        c.w.playSound(null, PARED_X[pa] + 0.5, S + 2, -4 + 3 * k, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.4f, 0.9f);
                    }
                }
            }
        }
        c.t.barra(1f - (float) c.tick / limite());
        return (jugando > 0 && llegados >= jugando) || c.tick >= limite() || jugando == 0;
    }

    private void caer(Ctx c, Jug j, ServerPlayerEntity p) {
        j.caidas++;
        int S = Minijuegos.SUELO;
        double rz = -3 + c.rnd.nextInt(7);
        double cx = CP_X[j.checkpoint] + 0.5;
        c.reaparecer(j, cx, S + 1, rz, -90f);
        j.inmune = c.tick + 30;
        p.sendMessage(Text.literal("¡Has caído! Vuelves al último punto de control.").formatted(Formatting.RED), true);
        p.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.MASTER, 0.4f, 1.6f);
    }

    @Override
    public List<Jug> clasificacion(Ctx c) {
        List<Jug> l = c.jugs();
        l.sort((a, b) -> {
            if (a.llego != b.llego) return a.llego ? -1 : 1;
            if (a.llego) return Long.compare(a.tLlegada, b.tLlegada);
            if (a.fila != b.fila) return Integer.compare(b.fila, a.fila);
            return Integer.compare(a.caidas, b.caidas);
        });
        return l;
    }

    @Override public void limpiar(Ctx c) { quitarEntidades(); }

    @Override public void alIrse(Ctx c, Jug j) { }

    @Override
    public String hud(Ctx c, Jug j) {
        int resta = Math.max(0, (limite() - c.tick) / 20);
        int pct = Math.min(100, Math.max(0, (int) ((j.llego ? META_X : j.fila) * 100.0 / META_X)));
        return pct + " % de la carrera · caídas " + j.caidas + " · " + (resta / 60) + ":" + String.format("%02d", resta % 60) + (j.llego ? " · ¡LLEGASTE!" : "");
    }
}
