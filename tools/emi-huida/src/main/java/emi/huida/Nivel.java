package emi.huida;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nivel 1 «La huida del Tyrantrum» (prototipo): un pasillo de ruinas de selva de 7 de ancho que se recorre hacia +z.
 * Cada jugador corre en su propia copia (hueco), separada de las demas {@link #SEP} bloques en x, asi nadie se estorba.
 * Coordenadas locales: x = -3..3 (el centro es 0), z = avance, y = 0 es el suelo por el que se camina (bloque Y0).
 * Los obstaculos son bloques de verdad: chocar contra ellos te frena (y cuenta como error); el Tyrantrum los destruye al pasar.
 */
final class Nivel {
    private Nivel() {}

    static final int Y0 = 64;                 // altura de los pies del jugador
    static final int SEP = 24;                // separacion entre copias del nivel
    static final int Z_PERSEGUIDOR = -18;     // donde empieza el Tyrantrum
    static final int Z_META = 170;            // linea de meta (se escapa al pasarla)
    static final int Z_ATRAS = -24, Z_FIN = 184;
    static final int MAX_HUECOS = 12;

    /** Zonas de obstaculo en z (para contar un error si te chocas dentro de ellas, no cuando rozas la pared). */
    record Zona(int z0, int z1, String nombre) {}
    static final List<Zona> ZONAS = List.of(
            new Zona(29, 31, "tronco"), new Zona(47, 52, "raices"),
            new Zona(69, 71, "rocas"), new Zona(81, 83, "rocas"), new Zona(93, 95, "rocas"), new Zona(101, 103, "rocas"),
            new Zona(139, 141, "tronco"), new Zona(145, 147, "tronco"));
    /** Hueco en el suelo (z0..z1): caer da un error y te devuelve a {@link #Z_ANTES_HUECO}. */
    static final int HUECO_Z0 = 116, HUECO_Z1 = 117, Z_ANTES_HUECO = 112;

    static int ox(int hueco) { return hueco * SEP; }

    private static final int F = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;   // sin actualizar vecinos: mucho mas rapido

    /** Carga (y mantiene cargados) los chunks de una copia. */
    static void chunks(ServerWorld w, int hueco, boolean forzar) {
        int x0 = (ox(hueco) - 6) >> 4, x1 = (ox(hueco) + 6) >> 4, z0 = (Z_ATRAS - 4) >> 4, z1 = (Z_FIN + 4) >> 4;
        for (int cx = x0; cx <= x1; cx++) for (int cz = z0; cz <= z1; cz++) {
            if (forzar) { w.getChunk(cx, cz); w.setChunkForced(cx, cz, true); } else w.setChunkForced(cx, cz, false);
        }
    }

    private static final Block[] MURO = {Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_STONE_BRICKS};
    private static final Block[] SUELO = {Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.COARSE_DIRT, Blocks.MOSSY_STONE_BRICKS};

    private static int h(int a, int b, int c) {
        int v = a * 73856093 ^ b * 19349663 ^ c * 83492791;
        v ^= v >>> 13; v *= 0x5bd1e995; v ^= v >>> 15;
        return v & 0x7fffffff;
    }

    private static void set(ServerWorld w, int ox, int x, int y, int z, BlockState s) {
        w.setBlockState(new BlockPos(ox + x, Y0 + y, z), s, F);
    }

    /** Pone una fila de bloques de obstaculo en x0..x1 y alturas y0..y1 en el plano z, y los apunta (el Tyrantrum los rompe). */
    private static void obs(ServerWorld w, int ox, Set<BlockPos> rompibles, BlockState s, int x0, int x1, int y0, int y1, int z) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) {
            BlockPos p = new BlockPos(ox + x, Y0 + y, z);
            w.setBlockState(p, s, F);
            rompibles.add(p);
        }
    }

    /** Franja de color en el suelo antes de un obstaculo: verde = salta, naranja = agachate, amarillo = esquiva, rojo = hueco. */
    private static void senal(ServerWorld w, int ox, Block b, int z0, int z1) {
        for (int z = z0; z <= z1; z++) for (int x = -3; x <= 3; x++) set(w, ox, x, -1, z, b.getDefaultState());
    }

    /** Borra la copia y la vuelve a construir. Devuelve los bloques que destruye el perseguidor. */
    static Set<BlockPos> construir(ServerWorld w, int hueco) {
        int ox = ox(hueco);
        chunks(w, hueco, true);
        BlockState aire = Blocks.AIR.getDefaultState();
        for (int x = -6; x <= 6; x++) for (int z = Z_ATRAS - 2; z <= Z_FIN + 2; z++) for (int y = -8; y <= 14; y++) set(w, ox, x, y, z, aire);
        Set<BlockPos> rompibles = new HashSet<>();

        // suelo y muros
        for (int z = Z_ATRAS; z <= Z_FIN; z++) {
            for (int x = -3; x <= 3; x++) {
                BlockState suelo = SUELO[h(x, z, 1) % SUELO.length].getDefaultState();
                if (z >= HUECO_Z0 && z <= HUECO_Z1) continue;       // el hueco
                set(w, ox, x, -1, z, suelo);
                set(w, ox, x, -2, z, Blocks.STONE_BRICKS.getDefaultState());
            }
            for (int s : new int[]{-4, 4}) {
                for (int y = -2; y <= 7; y++) set(w, ox, s, y, z, MURO[h(s, z, y) % MURO.length].getDefaultState());
                if (h(s, z, 99) % 3 == 0) set(w, ox, s, 8, z, Blocks.JUNGLE_LEAVES.getDefaultState().with(net.minecraft.block.LeavesBlock.PERSISTENT, true));
                if (h(s, z, 7) % 5 == 0) set(w, ox, s > 0 ? 3 : -3, 3, z, Blocks.VINE.getDefaultState()
                        .with(s > 0 ? net.minecraft.block.VineBlock.EAST : net.minecraft.block.VineBlock.WEST, true));
            }
        }
        // paredes del fondo: detras de la salida y despues de la meta
        for (int x = -4; x <= 4; x++) for (int y = -2; y <= 7; y++) {
            set(w, ox, x, y, Z_ATRAS - 1, MURO[h(x, y, 5) % MURO.length].getDefaultState());
            set(w, ox, x, y, Z_FIN + 1, MURO[h(x, y, 6) % MURO.length].getDefaultState());
        }
        // la reliquia de la que despierta el Tyrantrum
        set(w, ox, 0, 0, Z_ATRAS + 2, Blocks.CHISELED_STONE_BRICKS.getDefaultState());
        set(w, ox, 0, 1, Z_ATRAS + 2, Blocks.GOLD_BLOCK.getDefaultState());
        for (int dx : new int[]{-2, 2}) for (int y = 0; y <= 3; y++) set(w, ox, dx, y, Z_ATRAS + 2, Blocks.MOSSY_STONE_BRICK_WALL.getDefaultState());

        // 1. tronco: salta
        senal(w, ox, Blocks.LIME_CONCRETE, 26, 27);
        obs(w, ox, rompibles, Blocks.JUNGLE_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X), -3, 3, 0, 0, 30);
        // 2. raices bajas: agachate (hueco de 1,5 bloques bajo una losa superior)
        senal(w, ox, Blocks.ORANGE_CONCRETE, 44, 45);
        for (int z = 48; z <= 51; z++) {
            obs(w, ox, rompibles, Blocks.MUD_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP), -3, 3, 1, 1, z);
            obs(w, ox, rompibles, Blocks.MANGROVE_ROOTS.getDefaultState(), -3, 3, 2, 6, z);
        }
        // 3. rocas: esquiva (izquierda+centro, centro+derecha, los dos lados, centro)
        BlockState roca = Blocks.MOSSY_COBBLESTONE.getDefaultState();
        senal(w, ox, Blocks.YELLOW_CONCRETE, 66, 67);
        obs(w, ox, rompibles, roca, -3, 1, 0, 1, 70);
        senal(w, ox, Blocks.YELLOW_CONCRETE, 78, 79);
        obs(w, ox, rompibles, roca, -1, 3, 0, 1, 82);
        senal(w, ox, Blocks.YELLOW_CONCRETE, 90, 91);
        obs(w, ox, rompibles, roca, -3, -2, 0, 1, 94);
        obs(w, ox, rompibles, roca, 2, 3, 0, 1, 94);
        obs(w, ox, rompibles, roca, -1, 1, 0, 1, 102);
        // 4. hueco: salta (ya esta sin suelo); borde rojo
        senal(w, ox, Blocks.RED_CONCRETE, HUECO_Z0 - 3, HUECO_Z0 - 2);
        // 5. dos troncos seguidos: salta dos veces
        senal(w, ox, Blocks.LIME_CONCRETE, 136, 137);
        BlockState tr = Blocks.JUNGLE_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X);
        obs(w, ox, rompibles, tr, -3, 3, 0, 0, 140);
        obs(w, ox, rompibles, tr, -3, 3, 0, 0, 146);
        // 6. meta: arco dorado y linea esmeralda
        for (int x = -3; x <= 3; x++) set(w, ox, x, -1, Z_META, Blocks.EMERALD_BLOCK.getDefaultState());
        for (int y = 0; y <= 3; y++) { set(w, ox, -3, y, Z_META, Blocks.GOLD_BLOCK.getDefaultState()); set(w, ox, 3, y, Z_META, Blocks.GOLD_BLOCK.getDefaultState()); }
        for (int x = -3; x <= 3; x++) set(w, ox, x, 4, Z_META, Blocks.GOLD_BLOCK.getDefaultState());
        for (int x = -2; x <= 2; x++) set(w, ox, x, 3, Z_META, Blocks.SEA_LANTERN.getDefaultState());
        return rompibles;
    }

}
