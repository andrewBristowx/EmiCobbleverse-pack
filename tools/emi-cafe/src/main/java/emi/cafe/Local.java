package emi.cafe;

import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * El local del cafe (se construye por codigo en la dimension {@code emi_cafe:cafe}, un local por jugador separados 64 bloques). Medidas en bloques desde la esquina (ox, 0):
 * paredes en x = 0 y W (14 y +8 por cada mejora «Ampliar»), z = 0 y 13; cocina al norte (z 2-5) con las estaciones pegadas a la pared de fondo (z = 1) y, cuando hacen falta, una segunda fila junto a la barra (z = 5); una barra en z = 6 con un hueco en x = 7-8;
 * comedor al sur (z 7-12) con las mesas en z = 9; la puerta en z = 13 (x = 7-8) y la calle fuera.
 */
final class Local {
    private Local() {}

    static final int Y = 63;                                  // el suelo; los pies estan en Y + 1
    static final int SEPARACION = 64;
    static final double SX = 7.5, SZ = 4.5;                   // donde aparece el jugador
    /** Las mesas (x, z del bloque) en el orden en que se van abriendo; el cliente se pone justo al sur. */
    static final int[][] MESAS = {{5, 9}, {9, 9}, {2, 9}, {12, 9}, {16, 9}, {20, 9}, {24, 9}, {28, 9}};
    static final int ANCHO_BASE = 14, ANCHO_POR_NIVEL = 8;
    static int ancho(int nivel) { return ANCHO_BASE + ANCHO_POR_NIVEL * nivel; }
    static final int X_REGISTRADORA = 13, Z_REGISTRADORA = 6;
    static final double PUERTA_X = 8.0, CALLE_Z = 16.5, PASILLO_Z = 11.5;

    static BlockPos pos(int ox, int x, int dy, int z) { return new BlockPos(ox + x, Y + dy, z); }

    private static void ponerBloque(ServerWorld w, int ox, int x, int dy, int z, Block b) { w.setBlockState(pos(ox, x, dy, z), b.getDefaultState(), Obra.BANDERAS); }

    /** El letrero de la calle (a la derecha de la puerta): verde «ABIERTO» o rojo «CERRADO». Se cambia sin reconstruir el local. */
    static void letrero(ServerWorld w, int ox, boolean abierto) {
        String panel = abierto ? "lime_concrete" : "red_concrete";
        for (int x = 10; x <= 12; x++) for (int y = 2; y <= 3; y++) Obra.poner(w, ox + x, Y + y, 14, panel);
        for (int y = 1; y <= 1; y++) { Obra.poner(w, ox + 10, Y + y, 14, "oak_fence"); Obra.poner(w, ox + 12, Y + y, 14, "oak_fence"); }
        Obra.poner(w, ox + 11, Y + 1, 14, "air");
        net.minecraft.util.math.Box caja = new net.minecraft.util.math.Box(ox + 9, Y, 14, ox + 14, Y + 6, 17);
        for (net.minecraft.entity.Entity e : w.getEntitiesByType(net.minecraft.entity.EntityType.TEXT_DISPLAY, caja, x -> x.getCommandTags().contains(Disp.TAG))) e.discard();
        Disp.texto(w, abierto ? "{\"text\":\"ABIERTO\",\"color\":\"white\",\"bold\":true}" : "{\"text\":\"CERRADO\",\"color\":\"white\",\"bold\":true}", ox + 11.5, Y + 3.0, 15.06, 1.6f, false);
    }

    /** Construye el local con solo las estaciones {@code activas} (las demas casillas de la fila quedan de encimera) y las mesas de la lista {@code mesas} ({x, z} de cada una). */
    static void construir(ServerWorld w, int ox, java.util.Set<Estacion> activas, java.util.List<int[]> mesas, int ampliar) {
        final int W = ancho(ampliar);
        long t0 = System.nanoTime();
        Disp.barrer(w, ox + 7, 9, 30);
        for (int x = -3; x <= W + 3; x++) for (int z = -3; z <= 20; z++) for (int y = Y - 1; y <= Y + 8; y++) Obra.poner(w, ox + x, y, z, "air");
        // suelo: cocina gris claro y blanco, comedor rosa y blanco, calle de piedra
        for (int x = 0; x <= W; x++) for (int z = 0; z <= 13; z++) {
            boolean damero = ((x + z) & 1) == 0;
            String piso = z <= 6 ? (damero ? "light_gray_concrete" : "white_concrete") : (damero ? "pink_concrete" : "white_concrete");
            Obra.poner(w, ox + x, Y, z, piso);
        }
        for (int x = -2; x <= W + 2; x++) for (int z = 14; z <= 19; z++) Obra.poner(w, ox + x, Y, z, ((x + z) & 3) == 0 ? "stone_bricks" : "smooth_stone");
        // paredes de 4 de alto (blancas con una franja rosa) y el techo de cristal para que entre la luz
        for (int y = 1; y <= 4; y++) {
            String mat = y == 2 ? "pink_concrete" : "white_concrete";
            for (int x = 0; x <= W; x++) { Obra.poner(w, ox + x, Y + y, 0, mat); Obra.poner(w, ox + x, Y + y, 13, mat); }
            for (int z = 0; z <= 13; z++) { Obra.poner(w, ox + 0, Y + y, z, mat); Obra.poner(w, ox + W, Y + y, z, mat); }
        }
        // ventanas al sur (y 2-3) y la puerta (x 7-8): un marco de roble
        for (int x : new int[]{2, 3, 4, 5, 10, 11, 12}) for (int y = 2; y <= 3; y++) Obra.poner(w, ox + x, Y + y, 13, "glass_pane");
        for (int x = 14; x < W; x++) if ((x - 14) % 8 >= 1 && (x - 14) % 8 <= 4) for (int y = 2; y <= 3; y++) Obra.poner(w, ox + x, Y + y, 13, "glass_pane");      // las ventanas de la parte ampliada
        for (int y = 1; y <= 3; y++) { Obra.poner(w, ox + 7, Y + y, 13, "air"); Obra.poner(w, ox + 8, Y + y, 13, "air"); }
        for (int y = 1; y <= 4; y++) { Obra.poner(w, ox + 6, Y + y, 13, "stripped_oak_log"); Obra.poner(w, ox + 9, Y + y, 13, "stripped_oak_log"); }
        for (int x = 6; x <= 9; x++) Obra.poner(w, ox + x, Y + 4, 13, "stripped_oak_log");
        for (int x = 1; x < W; x++) for (int z = 1; z <= 12; z++) Obra.poner(w, ox + x, Y + 5, z, (x % 4 == 1 && z % 4 == 1) ? "sea_lantern" : "glass");
        for (int x = 0; x <= W; x++) { Obra.poner(w, ox + x, Y + 5, 0, "white_concrete"); Obra.poner(w, ox + x, Y + 5, 13, "white_concrete"); }
        for (int z = 0; z <= 13; z++) { Obra.poner(w, ox, Y + 5, z, "white_concrete"); Obra.poner(w, ox + W, Y + 5, z, "white_concrete"); }
        // la fila de encimeras y estaciones al fondo (z = 1)
        for (int x = 1; x < W; x++) ponerBloque(w, ox, x, 1, 1, CafeBlocks.ENCIMERA);
        // la segunda fila (z = 5) solo se monta si hace falta alguna de sus estaciones; se deja libre el paso del comedor (x = 7-8)
        boolean segunda = false;
        for (Estacion e : activas) if (e.x >= 0 && e.z == 5) segunda = true;
        if (segunda) for (int x = 1; x <= 13; x++) if (x != 7 && x != 8) ponerBloque(w, ox, x, 1, 5, CafeBlocks.ENCIMERA);
        for (Estacion e : Estacion.values()) if (e.x >= 0 && (e.siempre || activas.contains(e))) ponerBloque(w, ox, e.x, 1, e.z, e.bloque);
        // la barra que separa la cocina del comedor (con un hueco para pasar) y la caja registradora en su extremo
        for (int x = 1; x < W; x++) if (x != 7 && x != 8) ponerBloque(w, ox, x, 1, 6, CafeBlocks.ENCIMERA);
        ponerBloque(w, ox, X_REGISTRADORA, 1, Z_REGISTRADORA, CafeBlocks.REGISTRADORA);
        // las mesas
        for (int[] m : mesas) ponerBloque(w, ox, m[0], 1, m[1], CafeBlocks.MESA);
        // el letrero sobre la puerta y farolas en la calle
        Disp.texto(w, "{\"text\":\"CAFÉ MICHIALIEN\",\"color\":\"light_purple\",\"bold\":true}", ox + 8.0, Y + 6.6, 13.6, 3.0f, false);
        Disp.texto(w, "{\"text\":\"Sirve lo que piden los Pokémon\",\"color\":\"yellow\"}", ox + 8.0, Y + 5.9, 13.6, 0.9f, false);
        for (int x : new int[]{-1, W + 1}) { for (int y = 1; y <= 3; y++) Obra.poner(w, ox + x, Y + y, 15, "stripped_oak_log"); Obra.poner(w, ox + x, Y + 4, 15, "sea_lantern"); }
        letrero(w, ox, false);
        EmiCafe.LOG.info("Local del cafe construido en {} ms", (System.nanoTime() - t0) / 1_000_000);
    }
}
