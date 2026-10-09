package emi.buildbattle;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Construye y limpia parcelas repartiendo el trabajo en varios ticks para no congelar el servidor. */
public final class PlotOps {
    /** Trabajo que avanza un poco cada tick. step devuelve true cuando ha terminado. */
    public interface Job { boolean step(int budget); }

    private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
    private static final int BUDGET = 60_000;
    private static final Deque<Job> queue = new ArrayDeque<>();
    private static Runnable onIdle;

    public static boolean busy() { return !queue.isEmpty(); }

    public static void enqueue(Job j) { queue.add(j); }

    public static void tick() {
        if (queue.isEmpty()) return;
        Job j = queue.peek();
        if (j.step(BUDGET)) queue.poll();
        if (queue.isEmpty() && onIdle != null) { Runnable r = onIdle; onIdle = null; r.run(); }
    }

    /** Ejecuta r cuando no quede trabajo pendiente (o ya, si no hay). */
    public static void whenIdle(Runnable r) {
        if (queue.isEmpty()) r.run();
        else {
            Runnable prev = onIdle;
            onIdle = prev == null ? r : () -> { prev.run(); r.run(); };
        }
    }

    // ---------------------------------------------------------------- construir

    public static void build(ServerWorld w, Cfg cfg, List<Plot> plots) {
        BlockState floor = state(cfg.floorBlock, Blocks.GRASS_BLOCK.getDefaultState());
        BlockState wall = state(cfg.wallBlock, Blocks.STONE_BRICKS.getDefaultState());
        BlockState wallTop = state(cfg.wallTopBlock, Blocks.CHISELED_STONE_BRICKS.getDefaultState());
        for (Plot p : plots) {
            // un trabajo por parcela: primero las filas del suelo, luego el muro
            final int[] row = {0};
            final boolean[] ringDone = {false};
            enqueue(budget -> {
                BlockPos.Mutable m = new BlockPos.Mutable();
                int s = p.size;
                int rows = Math.max(1, budget / (s * 4));
                for (int r = 0; r < rows && row[0] < s; r++, row[0]++) {
                    int x = p.ox + row[0];
                    for (int z = p.oz; z < p.oz + s; z++) {
                        w.setBlockState(m.set(x, p.floorY, z), floor, FLAGS);
                        w.setBlockState(m.set(x, p.floorY - 1, z), Blocks.DIRT.getDefaultState(), FLAGS);
                        w.setBlockState(m.set(x, p.floorY - 2, z), Blocks.DIRT.getDefaultState(), FLAGS);
                        w.setBlockState(m.set(x, p.floorY - 3, z), Blocks.BEDROCK.getDefaultState(), FLAGS);
                    }
                }
                if (row[0] < s) return false;
                if (!ringDone[0]) {
                    for (int k = -1; k <= s; k++) {
                        for (int y = p.floorY - 3; y <= p.floorY + cfg.wallHeight; y++) {
                            BlockState st = y == p.floorY + cfg.wallHeight ? wallTop : wall;
                            w.setBlockState(m.set(p.ox + k, y, p.oz - 1), st, FLAGS);
                            w.setBlockState(m.set(p.ox + k, y, p.oz + s), st, FLAGS);
                            w.setBlockState(m.set(p.ox - 1, y, p.oz + k), st, FLAGS);
                            w.setBlockState(m.set(p.ox + s, y, p.oz + k), st, FLAGS);
                        }
                    }
                    // jaula invisible: barreras sobre el muro hasta el techo, y un techo de barreras. Asi no se puede salir volando
                    // (el servidor ademas te devuelve si lo consigues por otro medio)
                    BlockState barrier = Blocks.BARRIER.getDefaultState();
                    int top = p.floorY + p.height;
                    for (int y = p.floorY + cfg.wallHeight + 1; y <= top + 1; y++) {
                        for (int k = -1; k <= s; k++) {
                            w.setBlockState(m.set(p.ox + k, y, p.oz - 1), barrier, FLAGS);
                            w.setBlockState(m.set(p.ox + k, y, p.oz + s), barrier, FLAGS);
                            w.setBlockState(m.set(p.ox - 1, y, p.oz + k), barrier, FLAGS);
                            w.setBlockState(m.set(p.ox + s, y, p.oz + k), barrier, FLAGS);
                        }
                    }
                    for (int x = p.ox; x < p.ox + s; x++) for (int z = p.oz; z < p.oz + s; z++) w.setBlockState(m.set(x, top + 1, z), barrier, FLAGS);
                    ringDone[0] = true;
                }
                return true;
            });
        }
    }

    private static BlockState state(String id, BlockState fallback) {
        Identifier i = Identifier.tryParse(id);
        if (i == null) return fallback;
        Block b = Registries.BLOCK.get(i);
        return b == Blocks.AIR ? fallback : b.getDefaultState();
    }

    // ---------------------------------------------------------------- limpiar

    /** Deja vacia la zona de cada parcela (interior, muro y un margen de un chunk). */
    public static void clear(ServerWorld w, List<Plot> plots) {
        for (Plot p : plots) {
            int minCx = (p.ox - 17) >> 4, maxCx = (p.ox + p.size + 16) >> 4;
            int minCz = (p.oz - 17) >> 4, maxCz = (p.oz + p.size + 16) >> 4;
            List<ChunkPos> chunks = new ArrayList<>();
            for (int cx = minCx; cx <= maxCx; cx++) for (int cz = minCz; cz <= maxCz; cz++) chunks.add(new ChunkPos(cx, cz));
            final int[] ci = {0};
            enqueue(budget -> {
                BlockPos.Mutable m = new BlockPos.Mutable();
                int used = 0;
                while (ci[0] < chunks.size() && used < budget) {
                    ChunkPos cp = chunks.get(ci[0]++);
                    WorldChunk chunk = w.getChunk(cp.x, cp.z);
                    ChunkSection[] secs = chunk.getSectionArray();
                    for (int si = 0; si < secs.length; si++) {
                        ChunkSection sec = secs[si];
                        if (sec == null || sec.isEmpty()) continue;
                        int y0 = chunk.sectionIndexToCoord(si) << 4;   // y minimo de la seccion
                        for (int lx = 0; lx < 16; lx++) for (int ly = 0; ly < 16; ly++) for (int lz = 0; lz < 16; lz++) {
                            used++;
                            if (sec.getBlockState(lx, ly, lz).isAir()) continue;
                            w.setBlockState(m.set((cp.x << 4) + lx, y0 + ly, (cp.z << 4) + lz), Blocks.AIR.getDefaultState(), FLAGS);
                        }
                    }
                }
                return ci[0] >= chunks.size();
            });
        }
    }
}
