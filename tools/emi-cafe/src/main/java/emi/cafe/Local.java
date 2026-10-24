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
    static final int SEPARACION = Ciudad.ANCHO;
    static final double SX = 7.5, SZ = 4.5;                   // donde aparece el jugador
    /** Las mesas (x, z del bloque) en el orden en que se van abriendo; el cliente se pone justo al sur. */
    static final int[][] MESAS = {{5, 9}, {9, 9}, {2, 9}, {12, 9}, {16, 9}, {20, 9}, {24, 9}, {28, 9}, {32, 9}, {36, 9}, {40, 9}, {44, 9}};
    /** Cuantas estaciones de trabajo (cajas, procesadoras y la mesa de emplatar; sin contar la basura ni el fregadero) caben en cada nivel de local (1 a 5). */
    static final int[] MAX_ESTACIONES = {5, 8, 12, 17, 22};
    static int maxEstaciones(int nivelLocal) { return MAX_ESTACIONES[Math.max(1, Math.min(MAX_ESTACIONES.length, nivelLocal)) - 1]; }
    static final int ANCHO_BASE = 14, ANCHO_POR_NIVEL = 8;
    static int ancho(int nivel) { return ANCHO_BASE + ANCHO_POR_NIVEL * nivel; }
    static final int X_REGISTRADORA = 13, Z_REGISTRADORA = 6;
    static final double PUERTA_X = 8.0, CALLE_Z = 15.5, PASILLO_Z = 11.5;       // CALLE_Z = la acera de delante de la puerta
    // la parcela (64 de ancho, ver Ciudad): muro de atras, jardin, el local (z 0-13), acera, avenida, otra acera, parque y muro de delante
    static final int Z_ATRAS = -13, Z_FRENTE = 32;
    static final int X_CARTEL = 7, Y_CARTEL = 5, Z_CARTEL = 14;                 // el letrero de la puerta: dos casillas (7 y 8) sobre el dintel
    static final int X_LIBRO = 8, Y_LIBRO = 3, Z_LIBRO = 1;                    // el libro de recetas en la pared del fondo de la cocina

    static BlockPos pos(int ox, int x, int dy, int z) { return new BlockPos(ox + x, Y + dy, z); }

    private static void ponerBloque(ServerWorld w, int ox, int x, int dy, int z, Block b) { w.setBlockState(pos(ox, x, dy, z), b.getDefaultState(), Obra.BANDERAS); }

    /** El letrero de la puerta (dos bloques-imagen sobre el dintel): verde «ABIERTO» o rojo «CERRADO». Se cambia sin reconstruir el local. */
    static void letrero(ServerWorld w, int ox, boolean abierto) {
        for (int k = 0; k < 2; k++)
            w.setBlockState(new BlockPos(ox + X_CARTEL + k, Y + Y_CARTEL, Z_CARTEL), CafeBlocks.CARTEL.getDefaultState().with(PlacaBlock.ABIERTO, abierto).with(PlacaBlock.DER, k == 1), Obra.BANDERAS);
    }

    /** Los textos del tejado: el nombre del cafe (el que le puso el dueño, o «CAFÉ MICHIALIEN») y «de <dueño>». Se puede cambiar sin reconstruir. */
    static void rotulo(ServerWorld w, int ox, String nombreCafe, String duenio) {
        net.minecraft.util.math.Box caja = new net.minecraft.util.math.Box(ox + 0, Y + 5, 13, ox + 17, Y + 10, 15);
        for (net.minecraft.entity.Entity e : w.getEntitiesByType(net.minecraft.entity.EntityType.TEXT_DISPLAY, caja, x -> x.getCommandTags().contains(Disp.TAG))) e.discard();
        String nombre = nombreCafe == null || nombreCafe.isEmpty() ? "CAFÉ MICHIALIEN" : nombreCafe;
        float escala = nombre.length() > 16 ? 1.3f : nombre.length() > 12 ? 1.6f : 1.8f;
        Disp.texto(w, "{\"text\":\"" + nombre.replace("\\", "").replace("\"", "") + "\",\"color\":\"light_purple\",\"bold\":true}", ox + 8.0, Y + 6.5, 13.6, escala, false);
        if (duenio != null && !duenio.isEmpty()) Disp.texto(w, "{\"text\":\"de " + duenio.replace("\\", "").replace("\"", "") + "\",\"color\":\"yellow\"}", ox + 8.0, Y + 7.3, 13.6, 1.0f, false);
    }

    /** El bloque del suelo exterior (a la altura Y) en la columna relativa {@code rx} y la fila {@code z}: acera, avenida, jardines y parque. */
    static String sueloExterior(int rx, int z) {
        if (z == Z_ATRAS || z == Z_FRENTE) return "stone_bricks";
        if ((z >= 14 && z <= 15) || (z >= 22 && z <= 23)) return ((rx + z) & 3) == 0 ? "stone_bricks" : "smooth_stone";
        if (z >= 16 && z <= 21) return z == 18 && Math.floorMod(rx, 8) < 4 ? "yellow_concrete" : "gray_concrete";
        return "grass_block";
    }

    private static void columnaSuelo(ServerWorld w, int x, int rx) {
        for (int z = Z_ATRAS; z <= Z_FRENTE; z++) {
            String s = sueloExterior(rx, z);
            Obra.poner(w, x, Y - 1, z, s.equals("grass_block") ? "dirt" : "stone");
            Obra.poner(w, x, Y, z, s);
            for (int y = 1; y <= 9; y++) Obra.poner(w, x, Y + y, z, "air");
            if (z == Z_ATRAS || z == Z_FRENTE) { Obra.poner(w, x, Y + 1, z, "stone_bricks"); Obra.poner(w, x, Y + 2, z, "stone_brick_slab[type=top]"); }      // los muros de atras y de delante siguen
        }
    }

    private static void muroColumna(ServerWorld w, int x) {
        for (int z = Z_ATRAS; z <= Z_FRENTE; z++) { Obra.poner(w, x, Y + 1, z, "stone_bricks"); Obra.poner(w, x, Y + 2, z, "stone_brick_slab[type=top]"); }
    }

    private static void arbol(ServerWorld w, int x, int z, int alto) {
        for (int y = 1; y <= alto; y++) Obra.poner(w, x, Y + y, z, "oak_log");
        for (int dy = alto - 1; dy <= alto + 2; dy++) {
            int r = dy >= alto + 1 ? (dy == alto + 2 ? 1 : 1) : 2;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (r == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                if (dx == 0 && dz == 0 && dy <= alto) continue;                      // el tronco
                if (dy == alto + 2 && Math.abs(dx) + Math.abs(dz) > 1) continue;
                Obra.poner(w, x + dx, Y + dy, z + dz, "oak_leaves[persistent=true]");
            }
        }
    }

    private static void farola(ServerWorld w, int x, int z) {
        for (int y = 1; y <= 3; y++) Obra.poner(w, x, Y + y, z, "stripped_oak_log");
        Obra.poner(w, x, Y + 4, z, "sea_lantern");
    }

    /** Lo de fuera que es igual para todos: la acera, la avenida, el parque con arboles, bancos, flores y fuente, el jardin de atras y los muros que cierran la parcela. */
    private static void ciudad(ServerWorld w, int ox, int ancho, boolean primera, boolean haySiguiente) {
        for (int x = ox + Ciudad.X0; x <= ox + Ciudad.X1; x++) {
            int rx = x - ox;
            for (int z = Z_ATRAS; z <= Z_FRENTE; z++) {
                String s = sueloExterior(rx, z);
                Obra.poner(w, x, Y - 1, z, s.equals("grass_block") ? "dirt" : "stone");
                Obra.poner(w, x, Y, z, s);
            }
            Obra.poner(w, x, Y + 1, Z_ATRAS, "stone_bricks"); Obra.poner(w, x, Y + 2, Z_ATRAS, "stone_brick_slab[type=top]");
            Obra.poner(w, x, Y + 1, Z_FRENTE, "stone_bricks"); Obra.poner(w, x, Y + 2, Z_FRENTE, "stone_brick_slab[type=top]");
        }
        if (primera) muroColumna(w, ox + Ciudad.X0);
        if (!haySiguiente) muroColumna(w, ox + Ciudad.X1);
        else columnaSuelo(w, ox + Ciudad.X1, Ciudad.X1);                                // (el muro del final de la calle se quita cuando llega el vecino de al lado: esto es por si acaso)
        if (!primera) columnaSuelo(w, ox + Ciudad.X0 - 1, Ciudad.X1);                   // el muro del final de la parcela de la izquierda ya no hace falta
        // el jardin de atras: seto y tres arboles
        for (int x = ox + Ciudad.X0 + 1; x < ox + Ciudad.X1; x++) Obra.poner(w, x, Y + 1, Z_ATRAS + 1, "oak_leaves[persistent=true]");
        for (int dx : new int[]{6, 24, 42}) arbol(w, ox + dx, -8, 4);
        // farolas de las dos aceras
        for (int x : new int[]{-1, ancho + 1}) farola(w, ox + x, 15);
        for (int dx : new int[]{18, 30, 42, 54}) if (dx > ancho + 2) farola(w, ox + dx, 15);
        for (int dx : new int[]{4, 20, 36, 52}) farola(w, ox + dx, 22);
        // el parque de enfrente: arboles, bancos, flores y una fuente
        for (int dx : new int[]{2, 14, 50, 58 - 2}) arbol(w, ox + dx, 28, 4 + (dx % 3));
        for (int dx : new int[]{8, 22, 42}) {
            Obra.poner(w, ox + dx, Y + 1, 25, "oak_stairs[facing=north]"); Obra.poner(w, ox + dx + 1, Y + 1, 25, "oak_stairs[facing=north]");
        }
        String[] flores = {"poppy", "dandelion", "azure_bluet", "pink_tulip", "oxeye_daisy"};
        for (int x = ox + 2; x < ox + 58; x++) if (x % 2 == 0) Obra.poner(w, x, Y + 1, 30, flores[Math.floorMod(x * 7 + 3, flores.length)]);
        int fx = ox + 32, fz = 27;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean borde = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            if (borde) Obra.poner(w, fx + dx, Y + 1, fz + dz, "stone_bricks");
            else Obra.poner(w, fx + dx, Y, fz + dz, "water");
        }
        for (int y = 1; y <= 2; y++) Obra.poner(w, fx, Y + y, fz, "stone_bricks");
        Obra.poner(w, fx, Y + 3, fz, "sea_lantern");
    }

    /**
     * Construye la parcela entera: el local con las estaciones de {@code pos} (cada una en su {x, z}; las demas casillas de la fila quedan de encimera), las mesas de la lista {@code mesas}
     * ({x, z} de cada una) y los materiales de {@code estilo} (ver {@link Estilo}), mas la calle, el parque y el jardin (ver {@link Ciudad}). {@code haySiguiente} = ya hay un vecino a la derecha.
     */
    static void construir(ServerWorld w, int ox, java.util.Map<Estacion, int[]> pos, java.util.List<int[]> mesas, int ampliar, int[] estilo, String[] pintura, String nombreCafe, String duenio, boolean haySiguiente) {
        final int W = ancho(ampliar);
        long t0 = System.nanoTime();
        Disp.barrerParcela(w, ox);
        for (int x = ox + Ciudad.X0; x <= ox + Ciudad.X1; x++) for (int z = Z_ATRAS; z <= Z_FRENTE; z++) for (int y = Y + 1; y <= Y + 9; y++) Obra.poner(w, x, y, z, "air");
        ciudad(w, ox, W, ox == 0, haySiguiente);
        // suelo del local: damero (la cocina y el comedor pueden ser de materiales distintos)
        for (int x = 0; x <= W; x++) for (int z = 0; z <= 13; z++) {
            Obra.poner(w, ox + x, Y, z, Estilo.suelo(estilo, pintura, z <= 6 ? Estilo.COCINA : Estilo.COMEDOR, x, z));
        }
        // paredes de 4 de alto (con una franja a la altura de los ojos) y el techo de cristal para que entre la luz
        final String pared = Estilo.pared(estilo, pintura), franja = Estilo.franja(estilo, pintura), ventana = Estilo.ventana(estilo, pintura);
        for (int y = 1; y <= 4; y++) {
            String mat = y == 2 ? franja : pared;
            for (int x = 0; x <= W; x++) { Obra.poner(w, ox + x, Y + y, 0, mat); Obra.poner(w, ox + x, Y + y, 13, mat); }
            for (int z = 0; z <= 13; z++) { Obra.poner(w, ox + 0, Y + y, z, mat); Obra.poner(w, ox + W, Y + y, z, mat); }
        }
        // ventanas al sur (y 2-3) y la puerta (x 7-8): un marco de roble
        for (int x : new int[]{2, 3, 4, 5, 10, 11, 12}) for (int y = 2; y <= 3; y++) Obra.poner(w, ox + x, Y + y, 13, ventana);
        for (int x = 14; x < W; x++) if ((x - 14) % 8 >= 1 && (x - 14) % 8 <= 4) for (int y = 2; y <= 3; y++) Obra.poner(w, ox + x, Y + y, 13, ventana);      // las ventanas de la parte ampliada
        for (int y = 1; y <= 3; y++) { Obra.poner(w, ox + 7, Y + y, 13, "air"); Obra.poner(w, ox + 8, Y + y, 13, "air"); }
        for (int y = 1; y <= 4; y++) { Obra.poner(w, ox + 6, Y + y, 13, "stripped_oak_log"); Obra.poner(w, ox + 9, Y + y, 13, "stripped_oak_log"); }
        for (int x = 6; x <= 9; x++) Obra.poner(w, ox + x, Y + 4, 13, "stripped_oak_log");
        final String techo = Estilo.techo(estilo, pintura), luz = Estilo.luz(estilo, pintura);
        final int per = Estilo.periodoLuz(estilo, pintura);
        for (int x = 1; x < W; x++) for (int z = 1; z <= 12; z++) Obra.poner(w, ox + x, Y + 5, z, (x % per == 1 && z % per == 1) ? luz : techo);
        for (int x = 0; x <= W; x++) { Obra.poner(w, ox + x, Y + 5, 0, pared); Obra.poner(w, ox + x, Y + 5, 13, pared); }
        for (int z = 0; z <= 13; z++) { Obra.poner(w, ox, Y + 5, z, pared); Obra.poner(w, ox + W, Y + 5, z, pared); }
        // la fila de encimeras y estaciones al fondo (z = 1)
        for (int x = 1; x < W; x++) ponerBloque(w, ox, x, 1, 1, CafeBlocks.ENCIMERA);
        // la segunda fila (z = 5) solo se monta si hace falta alguna de sus estaciones; se deja libre el paso del comedor (x = 7-8)
        boolean segunda = false;
        for (int[] q : pos.values()) if (q[1] == 5) segunda = true;
        if (segunda) for (int x = 1; x < W; x++) if (x != 7 && x != 8) ponerBloque(w, ox, x, 1, 5, CafeBlocks.ENCIMERA);
        for (java.util.Map.Entry<Estacion, int[]> en : pos.entrySet()) ponerBloque(w, ox, en.getValue()[0], 1, en.getValue()[1], en.getKey().bloque);
        // la barra que separa la cocina del comedor (con un hueco para pasar) y la caja registradora en su extremo
        for (int x = 1; x < W; x++) if (x != 7 && x != 8) ponerBloque(w, ox, x, 1, 6, CafeBlocks.ENCIMERA);
        ponerBloque(w, ox, X_REGISTRADORA, 1, Z_REGISTRADORA, CafeBlocks.REGISTRADORA);
        // las mesas
        for (int[] m : mesas) ponerBloque(w, ox, m[0], 1, m[1], CafeBlocks.MESA);
        // el libro de recetas en la pared del fondo de la cocina (clic = enseña cada plato)
        ponerBloque(w, ox, X_LIBRO, Y_LIBRO, Z_LIBRO, CafeBlocks.LIBRO_PARED);
        rotulo(w, ox, nombreCafe, duenio);
        letrero(w, ox, false);
        EmiCafe.LOG.info("Parcela del cafe construida en {} ms", (System.nanoTime() - t0) / 1_000_000);
    }
}
