package emi.huida;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nivel 1 «La huida del Tyrantrum»: un camino de ruinas de selva con CURVAS (arcos y curvas en S), ancho variable, muros irregulares y selva alrededor.
 *
 * El camino se define como una linea central: rectas (siempre alineadas con los ejes) unidas por arcos de 90 grados. De ella salen unas muestras cada
 * 0,5 bloques (posicion, direccion, distancia recorrida s). Cada celda del terreno se asigna a la muestra mas cercana y de ahi salen su distancia al
 * camino y su posicion (s, v): v lateral (positivo = derecha del jugador). Segun eso es suelo, muro, terreno de selva, hueco, puente u obstaculo.
 * Los obstaculos solo van en las rectas, asi que son bandas alineadas con los ejes (limpias).
 *
 * Cada jugador corre en su propia copia (hueco), separada de las demas {@link #SEP} bloques en x. Los bloques de obstaculo se destruyen al pasar
 * el Tyrantrum y se vuelven a poner al empezar cada partida; el resto del terreno se construye una sola vez por sesion del servidor.
 */
final class Nivel {
    private Nivel() {}

    static final int Y0 = 64;                 // altura de los pies del jugador (el camino va a Y0-1)
    static final int Y_SELVA = Y0 - 3;        // superficie del terreno de selva alrededor
    static final int MAX_HUECOS = 12;
    static final int PRE = 30;                // bloques de camino detras del punto de partida del jugador
    static final int PUENTE_LARGO = 14;
    static final double PASO = 0.5;
    static final int PAD = 27;                // alcance maximo del terreno desde la linea central
    static double anchoMax(double s) { return mediaAncho(s) + grosorMuro(s); }
    static final int ANCHO_SELVA = 16;        // ancho del terreno de selva mas alla de los muros

    // ---------------------------------------------------------------- la ruta
    /** S = recta de ese largo; L / R = arco de 90 grados a la izquierda / derecha con ese radio. */
    private static final char[] TIPO = {'S', 'L', 'S', 'R', 'L', 'S', 'L', 'S', 'L', 'R', 'S', 'R', 'S', 'R', 'S', 'L', 'S', 'L', 'S', 'R', 'S'};
    private static final int[] VAL = {100, 16, 44, 12, 12, 56, 16, 40, 12, 12, 64, 16, 50, 12, 36, 12, 44, 16, 84, 12, 72};
    private static final int NSEG = TIPO.length;
    static final double[] SEG_S0 = new double[NSEG + 1];

    /** Un modulo de obstaculo: T tronco (saltar), B barra baja con raices (agacharse), L/R/C pilares a la izquierda / derecha / centro (esquivar),
     *  H hueco de 2 con lava y W hueco ancho de 3 (saltar), P puente de tablas que se derrumba. `seg` = indice de la recta en la ruta, `u` = distancia desde su inicio. */
    record Mod(int seg, int u, char tipo) {
        int filas() { return switch (tipo) { case 'H' -> 2; case 'W' -> 3; case 'P' -> PUENTE_LARGO; case 'B', 'L', 'R', 'C' -> 2; default -> 1; }; }
        double s0() { return SEG_S0[seg] + u; }
    }
    static final Mod[] MODS = {
            new Mod(0, 58, 'T'), new Mod(0, 78, 'B'),
            new Mod(2, 8, 'L'), new Mod(2, 15, 'R'), new Mod(2, 22, 'L'), new Mod(2, 29, 'R'), new Mod(2, 35, 'H'),
            new Mod(5, 8, 'P'), new Mod(5, 32, 'T'), new Mod(5, 42, 'C'),
            new Mod(7, 8, 'H'), new Mod(7, 15, 'T'), new Mod(7, 24, 'B'),
            new Mod(10, 8, 'L'), new Mod(10, 14, 'R'), new Mod(10, 20, 'L'), new Mod(10, 26, 'R'), new Mod(10, 34, 'T'), new Mod(10, 40, 'T'), new Mod(10, 50, 'B'),
            new Mod(12, 8, 'H'), new Mod(12, 15, 'T'), new Mod(12, 24, 'C'), new Mod(12, 32, 'B'), new Mod(12, 40, 'T'),
            new Mod(14, 8, 'T'), new Mod(14, 13, 'T'), new Mod(14, 18, 'T'), new Mod(14, 26, 'B'),
            new Mod(16, 8, 'P'), new Mod(16, 26, 'R'), new Mod(16, 33, 'L'),
            new Mod(18, 8, 'B'), new Mod(18, 16, 'C'), new Mod(18, 24, 'T'), new Mod(18, 32, 'H'), new Mod(18, 42, 'L'), new Mod(18, 48, 'R'),
            new Mod(18, 56, 'T'), new Mod(18, 62, 'B'), new Mod(18, 70, 'T'),
            new Mod(20, 10, 'T'), new Mod(20, 18, 'H'), new Mod(20, 28, 'B'), new Mod(20, 36, 'R'), new Mod(20, 44, 'T'), new Mod(20, 54, 'C')};

    // muestras de la linea central
    static final int NM;
    static final double[] MX, MZ, MTX, MTZ, MS, MYAW;
    static final double S_TOTAL;
    static final double S_SALIDA = PRE;                    // distancia en la que empieza el jugador
    static final double S_PERSEGUIDOR = PRE - 11;          // y en la que nace el Tyrantrum (11 bloques por detras)
    static final double S_META;                            // la puerta dorada, en la ultima recta

    // rejilla de celdas (coordenadas locales de la copia)
    static final int GX0, GZ0, GW, GH, SEP;
    private static final float[] GS, GV, GD;               // s, v y distancia de cada celda
    private static final byte[] CAT, HOYO;                 // categoria (0 nada, 1 suelo, 2 muro, 3 selva) y tipo de hueco (1 hueco, 2 puente)
    private static final byte[] ALT;                       // altura del muro sobre Y0 (1..9)
    private static final byte[] OBS;                       // tipo de obstaculo en la celda (0 ninguno)
    static final Set<Long> CHUNKS_LOCALES = new HashSet<>();

    static {
        // --- muestras
        List<double[]> pts = new ArrayList<>();
        double x = 0.5, z = 0.5 - PRE, hx = 0, hz = 1, s = 0;
        for (int i = 0; i < NSEG; i++) {
            SEG_S0[i] = s;
            if (TIPO[i] == 'S') {
                int n = (int) Math.round(VAL[i] / PASO);
                for (int k = 0; k < n; k++) pts.add(new double[]{x + hx * k * PASO, z + hz * k * PASO, hx, hz, s + k * PASO});
                x += hx * VAL[i]; z += hz * VAL[i]; s += VAL[i];
            } else {
                double R = VAL[i]; int sg = TIPO[i] == 'L' ? 1 : -1;
                double lx = hz, lz = -hx;                           // izquierda de quien mira
                double cx = x + sg * lx * R, cz = z + sg * lz * R;
                double L = Math.PI / 2 * R; int n = (int) (L / PASO);
                for (int k = 0; k < n; k++) {
                    double ph = k * PASO / R;
                    pts.add(new double[]{cx + R * (-sg * lx * Math.cos(ph) + hx * Math.sin(ph)), cz + R * (-sg * lz * Math.cos(ph) + hz * Math.sin(ph)),
                            sg * lx * Math.sin(ph) + hx * Math.cos(ph), sg * lz * Math.sin(ph) + hz * Math.cos(ph), s + k * PASO});
                }
                double nx = cx + R * hx, nz = cz + R * hz, nhx = sg * lx, nhz = sg * lz;
                x = nx; z = nz; hx = nhx; hz = nhz; s += L;
            }
        }
        SEG_S0[NSEG] = s;
        pts.add(new double[]{x, z, hx, hz, s});
        S_TOTAL = s;
        S_META = S_TOTAL - 8;
        NM = pts.size();
        MX = new double[NM]; MZ = new double[NM]; MTX = new double[NM]; MTZ = new double[NM]; MS = new double[NM]; MYAW = new double[NM];
        double minX = 1e9, maxX = -1e9, minZ = 1e9, maxZ = -1e9;
        for (int i = 0; i < NM; i++) {
            double[] p = pts.get(i);
            MX[i] = p[0]; MZ[i] = p[1]; MTX[i] = p[2]; MTZ[i] = p[3]; MS[i] = p[4];
            MYAW[i] = Math.toDegrees(Math.atan2(-p[2], p[3]));
            minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]); minZ = Math.min(minZ, p[1]); maxZ = Math.max(maxZ, p[1]);
        }
        validar();

        // --- rejilla: cada celda con su muestra mas cercana
        GX0 = (int) Math.floor(minX) - PAD; GZ0 = (int) Math.floor(minZ) - PAD;
        GW = (int) Math.ceil(maxX) + PAD - GX0 + 1; GH = (int) Math.ceil(maxZ) + PAD - GZ0 + 1;
        SEP = ((GW + 16 + 15) / 16) * 16;
        GS = new float[GW * GH]; GV = new float[GW * GH]; GD = new float[GW * GH];
        CAT = new byte[GW * GH]; HOYO = new byte[GW * GH]; ALT = new byte[GW * GH]; OBS = new byte[GW * GH];
        int[] gk = new int[GW * GH];
        double[] d2min = new double[GW * GH];
        java.util.Arrays.fill(d2min, Double.MAX_VALUE); java.util.Arrays.fill(gk, -1);
        for (int k = 0; k < NM; k++) {
            int cx0 = (int) Math.floor(MX[k]), cz0 = (int) Math.floor(MZ[k]);
            for (int iz = cz0 - PAD; iz <= cz0 + PAD; iz++) for (int ix = cx0 - PAD; ix <= cx0 + PAD; ix++) {
                int id = (ix - GX0) + (iz - GZ0) * GW;
                double dx = ix + 0.5 - MX[k], dz = iz + 0.5 - MZ[k], d2 = dx * dx + dz * dz;
                if (d2 < d2min[id]) { d2min[id] = d2; gk[id] = k; }
            }
        }
        for (int iz = GZ0; iz < GZ0 + GH; iz++) for (int ix = GX0; ix < GX0 + GW; ix++) {
            int id = (ix - GX0) + (iz - GZ0) * GW, k = gk[id];
            if (k < 0) continue;
            double rx = ix + 0.5 - MX[k], rz = iz + 0.5 - MZ[k];
            GD[id] = (float) Math.sqrt(d2min[id]);
            GS[id] = (float) (MS[k] + rx * MTX[k] + rz * MTZ[k]);
            GV[id] = (float) (rx * (-MTZ[k]) + rz * MTX[k]);
        }
        // --- categorias de cada celda
        for (int iz = GZ0; iz < GZ0 + GH; iz++) for (int ix = GX0; ix < GX0 + GW; ix++) {
            int id = (ix - GX0) + (iz - GZ0) * GW;
            if (gk[id] < 0) continue;
            double sc = GS[id], d = GD[id], v = GV[id];
            boolean tapa = sc < -0.5 || sc > S_TOTAL + 0.5;            // detras de la salida o despues del final: muro que cierra el camino
            double hw = mediaAncho(sc), wt = grosorMuro(sc);
            if (!tapa && d <= hw) {
                CAT[id] = 1;
                for (Mod m : MODS) {
                    double s0 = m.s0();
                    if (sc < s0 - 0.5 || sc > s0 + m.filas() - 0.5) continue;
                    switch (m.tipo()) {
                        case 'H', 'W' -> HOYO[id] = 1;
                        case 'P' -> HOYO[id] = (byte) (Math.abs(v) <= 1.0 ? 2 : 1);
                        case 'T' -> OBS[id] = 'T';
                        case 'B' -> OBS[id] = 'B';
                        case 'L' -> { if (v <= -1) OBS[id] = 'r'; }
                        case 'R' -> { if (v >= 1) OBS[id] = 'r'; }
                        case 'C' -> { if (Math.abs(v) <= 1) OBS[id] = 'r'; }
                        default -> {}
                    }
                }
            } else if (d <= hw + wt) {
                CAT[id] = 2;
                int h = hash(ix, iz, 5);
                int alto = 3 + (int) Math.round(1.2 * Math.sin(sc / 9.0) + (h % 3) - 1);
                if (tapa) alto = 6;
                else if (h % 29 == 0) alto = 3;                         // tramo bajo del muro (3: no se puede saltar por encima)
                else if (h % 41 == 0) alto = 7 + h % 3;                 // columna alta de las ruinas
                else alto = Math.max(4, alto);
                ALT[id] = (byte) Math.max(3, Math.min(9, alto));
            } else if (d <= hw + wt + ANCHO_SELVA) CAT[id] = 3;
            if (CAT[id] != 0) CHUNKS_LOCALES.add(clave(Math.floorDiv(ix, 16), Math.floorDiv(iz, 16)));
        }
    }

    private static void validar() {
        for (Mod m : MODS) {
            if (TIPO[m.seg()] != 'S') throw new IllegalStateException("Obstaculo en un arco: " + m);
            int min = m.seg() == 0 ? PRE + 22 : 6;
            if (m.u() < min || m.u() + m.filas() > VAL[m.seg()] - 6) throw new IllegalStateException("Obstaculo mal colocado: " + m);
        }
        for (int i = 0; i < MODS.length; i++) for (int j = i + 1; j < MODS.length; j++) {
            Mod a = MODS[i], b = MODS[j];
            if (a.seg() == b.seg() && a.u() < b.u() + b.filas() + 4 && b.u() < a.u() + a.filas() + 4) throw new IllegalStateException("Obstaculos pegados: " + a + " " + b);
        }
        if (VAL[NSEG - 1] - 8 < 52) throw new IllegalStateException("Ultima recta demasiado corta");
    }

    private static double distMod(double s) {
        double d = 1e9;
        for (Mod m : MODS) { double a = m.s0(), b = a + m.filas(); d = Math.min(d, s < a ? a - s : s > b ? s - b : 0); }
        return d;
    }

    /** Medio ancho del camino: 3,5 (7 celdas) en los obstaculos y hasta 4,7 (9 celdas) en el resto. */
    static double mediaAncho(double s) {
        double n = 0.5 * Math.sin(s / 19.0 + 1.3) + 0.5 * Math.sin(s / 8.3 + 0.4);
        double f = Math.max(0, Math.min(1, (distMod(s) - 3) / 6));
        return 3.5 + 1.2 * (n + 1) / 2 * f;
    }

    private static double grosorMuro(double s) { return 2.3 + 0.9 * Math.sin(s / 11.0 + 2.0); }

    private static int hash(int a, int b, int c) {
        int v = a * 73856093 ^ b * 19349663 ^ c * 83492791;
        v ^= v >>> 13; v *= 0x5bd1e995; v ^= v >>> 15;
        return v & 0x7fffffff;
    }

    private static long clave(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }

    static int ox(int hueco) { return hueco * SEP; }

    // ---------------------------------------------------------------- geometria (servidor)

    private static int indice(double s) { return Math.max(0, Math.min(NM - 2, (int) (s / PASO))); }

    /** {x, z, yaw} (coordenadas del mundo) del eje del camino a distancia s del principio. */
    static double[] ejePos(int hueco, double s) {
        s = Math.max(0, Math.min(S_TOTAL, s));
        int i = indice(s);
        double f = Math.max(0, Math.min(1, (s - MS[i]) / (MS[i + 1] - MS[i])));
        return new double[]{ox(hueco) + MX[i] + (MX[i + 1] - MX[i]) * f, MZ[i] + (MZ[i + 1] - MZ[i]) * f, rumbo(s)};
    }

    /** Direccion del camino (yaw de Minecraft) a distancia s; en los arcos va girando de forma continua. */
    static float rumbo(double s) {
        s = Math.max(0, Math.min(S_TOTAL, s));
        int i = indice(s);
        double f = Math.max(0, Math.min(1, (s - MS[i]) / (MS[i + 1] - MS[i])));
        double d = ((MYAW[i + 1] - MYAW[i] + 540) % 360) - 180;
        return (float) (MYAW[i] + d * f);
    }

    /** Posicion del jugador en el camino: muestra mas cercana, distancia recorrida s y lateral v. */
    record Prog(int idx, double s, double v, double dist) {}

    /** `pista` = indice de muestra en el que estaba: solo se busca cerca (6 bloques atras y 40 adelante). */
    static Prog proyectar(int hueco, double x, double z, int pista) {
        double lx = x - ox(hueco);
        int a = Math.max(0, pista - 12), b = Math.min(NM - 1, pista + 80);
        int mejor = a; double md = Double.MAX_VALUE;
        for (int k = a; k <= b; k++) { double dx = lx - MX[k], dz = z - MZ[k], d = dx * dx + dz * dz; if (d < md) { md = d; mejor = k; } }
        double rx = lx - MX[mejor], rz = z - MZ[mejor];
        double s = MS[mejor] + rx * MTX[mejor] + rz * MTZ[mejor];
        return new Prog(mejor, Math.max(0, Math.min(S_TOTAL, s)), rx * (-MTZ[mejor]) + rz * MTX[mejor], Math.sqrt(md));
    }

    static int idxDe(double s) { return indice(s); }

    // ---------------------------------------------------------------- construccion

    /** Un bloque que el Tyrantrum destruira al llegar a el, con su distancia s. */
    record Bloque(BlockPos pos, double s) {}
    /** Punto seguro al que se vuelve tras caer (x, z locales de la copia). */
    record Check(double s, double x, double z, float yaw) {}
    record Construido(List<Bloque> obstaculos, List<Bloque> puente, List<Check> checks) {}

    private static final Block[] MURO = {Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS};
    private static final Block[] SUELO = {Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.COARSE_DIRT, Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.MOSS_BLOCK};
    private static final int F = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;     // sin actualizar vecinos: mucho mas rapido
    private static final Set<Integer> CONSTRUIDOS = new HashSet<>();

    /** Carga (y mantiene cargados) los chunks de una copia. */
    static void chunks(ServerWorld w, int hueco, boolean forzar) {
        int c0 = ox(hueco) >> 4;
        for (long k : CHUNKS_LOCALES) {
            int cx = c0 + (int) (k >> 32), cz = (int) k;
            if (forzar) { w.getChunk(cx, cz); w.setChunkForced(cx, cz, true); } else w.setChunkForced(cx, cz, false);
        }
    }

    private static boolean hoyoEn(int ix, int iz) {
        int x = ix - GX0, z = iz - GZ0;
        return x >= 0 && z >= 0 && x < GW && z < GH && HOYO[x + z * GW] != 0;
    }

    private static boolean muroEn(int ix, int iz) {
        int x = ix - GX0, z = iz - GZ0;
        return x >= 0 && z >= 0 && x < GW && z < GH && CAT[x + z * GW] == 2 && ALT[x + z * GW] >= 3;
    }

    /** Construye (la primera vez en esta sesion del servidor) el terreno de la copia y coloca los obstaculos y las tablas del puente. */
    static Construido construir(ServerWorld w, int hueco) {
        int ox = ox(hueco);
        chunks(w, hueco, true);
        boolean primera = CONSTRUIDOS.add(hueco);
        BlockPos.Mutable p = new BlockPos.Mutable();
        BlockState lava = Blocks.LAVA.getDefaultState(), piedra = Blocks.STONE_BRICKS.getDefaultState();
        if (primera) {
            long t0 = System.nanoTime();
            for (int iz = GZ0; iz < GZ0 + GH; iz++) for (int ix = GX0; ix < GX0 + GW; ix++) {
                int id = (ix - GX0) + (iz - GZ0) * GW;
                switch (CAT[id]) {
                    case 1 -> {
                        if (HOYO[id] != 0) {
                            w.setBlockState(p.set(ox + ix, Y0 - 6, iz), lava, F);
                            w.setBlockState(p.set(ox + ix, Y0 - 7, iz), piedra, F);
                        } else {
                            w.setBlockState(p.set(ox + ix, Y0 - 1, iz), SUELO[hash(ix, iz, 1) % SUELO.length].getDefaultState(), F);
                            w.setBlockState(p.set(ox + ix, Y0 - 2, iz), piedra, F);
                            if (hoyoEn(ix + 1, iz) || hoyoEn(ix - 1, iz) || hoyoEn(ix, iz + 1) || hoyoEn(ix, iz - 1))
                                for (int y = -5; y <= -3; y++) w.setBlockState(p.set(ox + ix, Y0 + y, iz), piedra, F);   // la pared del hueco
                        }
                    }
                    case 2 -> {
                        int alto = ALT[id];
                        boolean junto = hoyoEn(ix + 1, iz) || hoyoEn(ix - 1, iz) || hoyoEn(ix, iz + 1) || hoyoEn(ix, iz - 1);
                        for (int y = junto ? -7 : Y_SELVA - Y0; y < alto; y++) {
                            Block b = alto >= 7 && y >= alto - 1 ? Blocks.CHISELED_STONE_BRICKS : MURO[hash(ix, iz, y + 11) % MURO.length];
                            w.setBlockState(p.set(ox + ix, Y0 + y, iz), b.getDefaultState(), F);
                        }
                        if (alto <= 5 && hash(ix, iz, 77) % 4 == 0) w.setBlockState(p.set(ox + ix, Y0 + alto, iz), Blocks.JUNGLE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true), F);
                    }
                    case 3 -> {
                        int y = Y_SELVA - (hash(ix, iz, 3) % 5 == 0 ? 1 : 0);
                        w.setBlockState(p.set(ox + ix, y, iz), Blocks.GRASS_BLOCK.getDefaultState(), F);
                        w.setBlockState(p.set(ox + ix, y - 1, iz), Blocks.DIRT.getDefaultState(), F);
                        if (hash(ix, iz, 7) % 52 == 0 && GD[id] > mediaAncho(GS[id]) + grosorMuro(GS[id]) + 3) arbol(w, p, ox + ix, y + 1, iz, hash(ix, iz, 9));
                    }
                    default -> {}
                }
            }
            // antorchas en las paredes (en la celda del suelo pegada a un muro alto)
            for (int iz = GZ0; iz < GZ0 + GH; iz++) for (int ix = GX0; ix < GX0 + GW; ix++) {
                int id = (ix - GX0) + (iz - GZ0) * GW;
                if (CAT[id] != 1 || HOYO[id] != 0 || OBS[id] != 0 || hash(ix, iz, 21) % 40 != 0) continue;
                for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    if (!muroEn(ix + d.getOffsetX(), iz + d.getOffsetZ())) continue;
                    w.setBlockState(p.set(ox + ix, Y0 + 2, iz), Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, d.getOpposite()), F);
                    break;
                }
            }
            // la puerta dorada de la meta
            for (int iz = GZ0; iz < GZ0 + GH; iz++) for (int ix = GX0; ix < GX0 + GW; ix++) {
                int id = (ix - GX0) + (iz - GZ0) * GW;
                if (CAT[id] != 1 || GS[id] < S_META - 0.5 || GS[id] > S_META + 0.5) continue;
                w.setBlockState(p.set(ox + ix, Y0 - 1, iz), Blocks.EMERALD_BLOCK.getDefaultState(), F);
                w.setBlockState(p.set(ox + ix, Y0 + 4, iz), Blocks.GOLD_BLOCK.getDefaultState(), F);
                boolean borde = GD[id] > mediaAncho(GS[id]) - 1;
                if (borde) for (int y = 0; y <= 3; y++) w.setBlockState(p.set(ox + ix, Y0 + y, iz), Blocks.GOLD_BLOCK.getDefaultState(), F);
                else w.setBlockState(p.set(ox + ix, Y0 + 3, iz), Blocks.SEA_LANTERN.getDefaultState(), F);
            }
            EmiHuida.LOG.info("Huida: copia {} del nivel construida en {} ms", hueco, (System.nanoTime() - t0) / 1_000_000);
        }

        // obstaculos y tablas (se vuelven a poner en cada partida)
        List<Bloque> obst = new ArrayList<>(), puente = new ArrayList<>();
        BlockState roca = Blocks.MOSSY_COBBLESTONE.getDefaultState();
        BlockState losa = Blocks.DARK_OAK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);
        BlockState tablon = Blocks.JUNGLE_PLANKS.getDefaultState();
        for (int iz = GZ0; iz < GZ0 + GH; iz++) for (int ix = GX0; ix < GX0 + GW; ix++) {
            int id = (ix - GX0) + (iz - GZ0) * GW;
            if (CAT[id] != 1) continue;
            double sc = GS[id];
            if (HOYO[id] == 2) {
                BlockPos pos = new BlockPos(ox + ix, Y0 - 1, iz);
                w.setBlockState(pos, tablon, F);
                puente.add(new Bloque(pos, sc));
            }
            switch (OBS[id]) {
                case 'T' -> {
                    int k = indice(sc);
                    Direction.Axis eje = Math.abs(MTX[k]) > Math.abs(MTZ[k]) ? Direction.Axis.Z : Direction.Axis.X;       // atraviesa el camino
                    obst.add(poner(w, ox + ix, Y0, iz, Blocks.JUNGLE_LOG.getDefaultState().with(PillarBlock.AXIS, eje), sc));
                }
                case 'B' -> {
                    obst.add(poner(w, ox + ix, Y0 + 1, iz, losa, sc));                                     // la barra: hay que agacharse
                    if (hash(ix, iz, 13) % 2 == 0) obst.add(poner(w, ox + ix, Y0, iz, Blocks.HANGING_ROOTS.getDefaultState(), sc));   // raices colgando (sin colision)
                }
                case 'r' -> { obst.add(poner(w, ox + ix, Y0, iz, roca, sc)); obst.add(poner(w, ox + ix, Y0 + 1, iz, roca, sc)); }
                default -> {}
            }
        }
        obst.sort((a, b) -> Double.compare(a.s(), b.s()));
        puente.sort((a, b) -> Double.compare(a.s(), b.s()));
        List<Check> checks = new ArrayList<>();
        for (Mod m : MODS) if (m.tipo() == 'H' || m.tipo() == 'W' || m.tipo() == 'P') {
            double sc = m.s0() - 4;
            double[] e = ejePos(0, sc);
            checks.add(new Check(sc, e[0], e[1], (float) e[2]));
        }
        checks.sort((a, b) -> Double.compare(a.s(), b.s()));
        return new Construido(obst, puente, checks);
    }

    private static Bloque poner(ServerWorld w, int x, int y, int z, BlockState s, double sd) {
        BlockPos pos = new BlockPos(x, y, z);
        w.setBlockState(pos, s, F);
        return new Bloque(pos, sd);
    }

    private static void arbol(ServerWorld w, BlockPos.Mutable p, int x, int y, int z, int h) {
        int alto = 7 + h % 6;
        for (int i = 0; i < alto; i++) w.setBlockState(p.set(x, y + i, z), Blocks.JUNGLE_LOG.getDefaultState(), F);
        BlockState hoja = Blocks.JUNGLE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        for (int dy = -2; dy <= 2; dy++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            int r = dx * dx + dz * dz + dy * dy * 2;
            if (r > 10 || (r > 6 && hash(x + dx, z + dz, y + dy) % 3 == 0)) continue;
            if (dx == 0 && dz == 0 && dy < 1) continue;
            w.setBlockState(p.set(x + dx, y + alto - 1 + dy, z + dz), hoja, F);
        }
    }
}
