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
    static final int PRE = 44;                // bloques de camino detras del punto de partida del jugador (la jaula del Tyrantrum, que mide ~20 de largo, cabe detras de la entrada)
    static final int PUENTE_LARGO = 14;
    static final double PASO = 0.5;
    static final int PAD = 27;                // alcance maximo del terreno desde la linea central
    static double anchoMax(double s) { return mediaAncho(s) + grosorMuro(s); }
    static final int ANCHO_SELVA = 16;        // ancho del terreno de selva mas alla de los muros

    // ---------------------------------------------------------------- la ruta
    /** S = recta de ese largo; L / R = arco de 90 grados a la izquierda / derecha con ese radio. */
    private static final char[] TIPO = {'S', 'L', 'S', 'R', 'L', 'S', 'L', 'S', 'L', 'R', 'S', 'R', 'S', 'R', 'S', 'L', 'S', 'L', 'S', 'R', 'S'};
    private static final int[] VAL = {114, 16, 44, 12, 12, 70, 16, 40, 12, 12, 64, 16, 50, 12, 36, 12, 60, 16, 84, 12, 72};
    private static final int NSEG = TIPO.length;
    static final double[] SEG_S0 = new double[NSEG + 1];

    /** Un modulo de obstaculo: T tronco (saltar), B barra baja con raices (agacharse), L/R/C pilares a la izquierda / derecha / centro (esquivar),
     *  H hueco de 2 con lava y W hueco ancho de 3 (saltar), P puente de tablas que se derrumba. `seg` = indice de la recta en la ruta, `u` = distancia desde su inicio. */
    record Mod(int seg, int u, char tipo) {
        int filas() { return switch (tipo) { case 'H' -> 2; case 'W' -> 3; case 'P' -> PUENTE_LARGO; case 'B', 'L', 'R', 'C' -> 2; default -> 1; }; }
        double s0() { return SEG_S0[seg] + u; }
    }
    static final Mod[] MODS = {
            new Mod(0, 72, 'T'), new Mod(0, 92, 'B'),
            new Mod(2, 8, 'L'), new Mod(2, 15, 'R'), new Mod(2, 22, 'L'), new Mod(2, 29, 'R'), new Mod(2, 35, 'H'),
            new Mod(5, 16, 'P'), new Mod(5, 46, 'T'), new Mod(5, 56, 'C'),
            new Mod(7, 8, 'H'), new Mod(7, 15, 'T'), new Mod(7, 24, 'B'),
            new Mod(10, 8, 'L'), new Mod(10, 14, 'R'), new Mod(10, 20, 'L'), new Mod(10, 26, 'R'), new Mod(10, 34, 'T'), new Mod(10, 40, 'T'), new Mod(10, 50, 'B'),
            new Mod(12, 8, 'H'), new Mod(12, 15, 'T'), new Mod(12, 24, 'C'), new Mod(12, 32, 'B'), new Mod(12, 40, 'T'),
            new Mod(14, 8, 'T'), new Mod(14, 13, 'T'), new Mod(14, 18, 'T'), new Mod(14, 26, 'B'),
            new Mod(16, 14, 'P'), new Mod(16, 44, 'R'), new Mod(16, 52, 'L'),
            new Mod(18, 8, 'B'), new Mod(18, 16, 'C'), new Mod(18, 24, 'T'), new Mod(18, 32, 'H'), new Mod(18, 42, 'L'), new Mod(18, 48, 'R'),
            new Mod(18, 56, 'T'), new Mod(18, 62, 'B'), new Mod(18, 70, 'T'),
            new Mod(20, 10, 'T'), new Mod(20, 18, 'H'), new Mod(20, 28, 'B'), new Mod(20, 36, 'R'), new Mod(20, 44, 'T'), new Mod(20, 54, 'C')};

    // muestras de la linea central
    static final int NM;
    static final double[] MX, MZ, MTX, MTZ, MS, MYAW;
    static final double S_TOTAL;
    static final double S_SALIDA = PRE;                    // distancia en la que empieza el jugador
    static final double S_PERSEGUIDOR = 14;                // y en la que nace el Tyrantrum, encerrado detras de la entrada (22 bloques por detras del jugador)
    static final double S_ENTRADA = 25;                    // la entrada (reja que levanta el michi demonio): el Tyrantrum esta detras, entre ella y el muro del fondo
    static final int ENTRADA_ALTO = 11;                    // filas de barrotes de la reja
    static final double S_META;                            // la puerta dorada, en la ultima recta

    // rejilla de celdas (coordenadas locales de la copia)
    static final int GX0, GZ0, GW, GH, SEP;
    private static final float[] GS, GV, GD;               // s, v y distancia de cada celda
    private static final byte[] CAT, HOYO;                 // categoria (0 nada, 1 suelo, 2 muro, 3 selva) y tipo de hueco (1 hueco, 2 puente)
    private static final byte[] ALT;                       // altura del muro sobre Y0 (1..9)
    private static final byte[] OBS;                       // tipo de obstaculo en la celda (0 ninguno)
    private static final byte[] CARRIL;                    // 1 = camino del Tyrantrum por la selva, 2 = maleza que cierra el hueco del muro, 3 = sin arboles
    static final Set<Long> CHUNKS_LOCALES = new HashSet<>();

    /**
     * Camino del Tyrantrum alrededor de un puente: sale del camino por un lado (atravesando una maleza en el muro), rodea el hueco por la selva
     * por un sendero de tierra y vuelve. {@code a0..a1} es la rampa de salida, {@code b0..b1} la de vuelta y {@code lateral} lo que se aparta del eje.
     */
    record Desvio(double a0, double a1, double b0, double b1, int lado, double lateral) {}
    static final List<Desvio> DESVIOS = new ArrayList<>();
    static final double RAMPA = 8, MARGEN_DESVIO = 2, RADIO_CARRIL = 3.7;

    /** Una michiestrella: distancia s a lo largo del camino, lateral v (positivo = derecha del jugador), altura h sobre los pies y puntos que da. */
    record Estrella(double s, double v, double h, int valor) {}
    static final Estrella[] ESTRELLAS;

    private static double suave(double t) { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); }

    /** Lo que se aparta el Tyrantrum del eje del camino a distancia s (positivo = a la derecha del jugador): 0 salvo en los desvios de los puentes. */
    static double desfase(double s) {
        for (Desvio d : DESVIOS) {
            if (s <= d.a0() || s >= d.b1()) continue;
            double f = s < d.a1() ? suave((s - d.a0()) / (d.a1() - d.a0())) : s <= d.b0() ? 1 : 1 - suave((s - d.b0()) / (d.b1() - d.b0()));
            return d.lado() * d.lateral() * f;
        }
        return 0;
    }

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
        // --- desvios del Tyrantrum alrededor de los puentes
        CARRIL = new byte[GW * GH];
        for (Mod m : MODS) {
            if (m.tipo() != 'P') continue;
            double a1 = m.s0() - MARGEN_DESVIO, b0 = m.s0() + m.filas() + MARGEN_DESVIO;
            double lat = 0;
            for (double ss = a1; ss <= b0; ss += 1) lat = Math.max(lat, anchoMax(ss) + 4.6);
            int mejor = 0, malos = Integer.MAX_VALUE;
            for (int lado : new int[]{1, -1}) {
                int n = marcarDesvio(new Desvio(a1 - RAMPA, a1, b0, b0 + RAMPA, lado, lat), false);
                if (n < malos) { malos = n; mejor = lado; }
            }
            if (malos > 0) throw new IllegalStateException("El desvio del puente en s=" + m.s0() + " choca con otra parte del camino (" + malos + " celdas)");
            Desvio d = new Desvio(a1 - RAMPA, a1, b0, b0 + RAMPA, mejor, lat);
            DESVIOS.add(d);
            marcarDesvio(d, true);
        }
        ESTRELLAS = disenarEstrellas();
    }

    /**
     * Reparte las michiestrellas por el camino: arcos sobre troncos y huecos (hay que saltar para cogerlas), filas bajo las barras (hay que agacharse),
     * estrellas en el lado libre de los pilares (hay que esquivar), un rastro sobre los puentes y grupos ondulados en los tramos libres.
     * Las grandes (5 puntos) estan en lo alto del salto de cada hueco, al final de cada puente y cerca de la meta.
     */
    private static Estrella[] disenarEstrellas() {
        List<Estrella> l = new ArrayList<>();
        List<Mod> mods = new ArrayList<>(List.of(MODS));
        mods.sort((a, b) -> Double.compare(a.s0(), b.s0()));
        int alt = 0;
        for (Mod m : mods) {
            double a = m.s0(), n = m.filas();
            switch (m.tipo()) {
                case 'T' -> { double[][] arco = {{-2.6, 1.7}, {-1.3, 2.5}, {0.5, 2.9}, {2.3, 2.5}, {3.6, 1.7}}; for (double[] q : arco) l.add(new Estrella(a + q[0], 0, q[1], 1)); }
                case 'B' -> { for (double q : new double[]{-1.2, 0.6, 2.4}) l.add(new Estrella(a + q, 0, 1.0, 1)); }
                case 'L' -> { for (double q : new double[]{-1.0, 1.0, 3.0}) l.add(new Estrella(a + q, 1.9, 1.0, 1)); }
                case 'R' -> { for (double q : new double[]{-1.0, 1.0, 3.0}) l.add(new Estrella(a + q, -1.9, 1.0, 1)); }
                case 'C' -> { double lado = (alt++ % 2 == 0) ? 2.4 : -2.4; for (double q : new double[]{-1.0, 1.0, 3.0}) l.add(new Estrella(a + q, lado, 1.0, 1)); }
                case 'H', 'W' -> {
                    l.add(new Estrella(a - 2.6, 0, 1.7, 1)); l.add(new Estrella(a - 1.2, 0, 2.6, 1));
                    l.add(new Estrella(a + n / 2, 0, 3.3, 5));
                    l.add(new Estrella(a + n + 1.2, 0, 2.6, 1)); l.add(new Estrella(a + n + 2.6, 0, 1.7, 1));
                }
                case 'P' -> { for (double q = 2; q <= n - 2; q += 2.6) l.add(new Estrella(a + q, 0, 1.0, 1)); l.add(new Estrella(a + n + 2.5, 0, 1.0, 5)); }
                default -> {}
            }
        }
        // tramos libres: grupos de 4 estrellas con ondulacion, lejos de los obstaculos
        double s = S_SALIDA + 6; int g = 0;
        while (s < S_META - 14) {
            Mod ocup = null;
            for (Mod m : mods) if (s + 8 > m.s0() - 6 && s < m.s0() + m.filas() + 6) { ocup = m; break; }
            if (ocup != null) { s = ocup.s0() + ocup.filas() + 6; continue; }
            for (int k = 0; k < 4; k++) {
                double ss = s + k * 2.4, lim = mediaAncho(ss) - 1.4;
                l.add(new Estrella(ss, Math.max(-lim, Math.min(lim, 1.7 * Math.sin(g * 1.7 + k * 0.9))), 1.0, 1));
            }
            s += 4 * 2.4 + 6; g++;
        }
        l.add(new Estrella(S_META - 12, 0, 1.0, 5));
        l.sort((x, y) -> Double.compare(x.s(), y.s()));
        return l.toArray(new Estrella[0]);
    }

    static final double PASO_MAPA = 4.0;

    /** Puntos (x, z del mundo) del eje del camino de la copia {@code hueco}, uno cada {@link #PASO_MAPA} bloques desde s=0 hasta el final: para el minimapa. */
    static float[] mapa(int hueco) {
        int n = (int) Math.ceil(S_TOTAL / PASO_MAPA) + 1;
        float[] r = new float[n * 2];
        for (int i = 0; i < n; i++) { double[] e = ejePos(hueco, Math.min(S_TOTAL, i * PASO_MAPA)); r[i * 2] = (float) e[0]; r[i * 2 + 1] = (float) e[1]; }
        return r;
    }

    static int puntosTotal() { int t = 0; for (Estrella e : ESTRELLAS) t += e.valor(); return t; }

    /** {x, y, z} en el mundo de una michiestrella de la copia {@code hueco}. */
    static double[] estrellaPos(int hueco, Estrella e) {
        double[] c = ejePos(hueco, e.s());
        int i = indice(e.s());
        return new double[]{c[0] + e.v() * -MTZ[i], Y0 + e.h(), c[1] + e.v() * MTX[i]};
    }

    /** Marca (si aplicar) las celdas por las que pasa el Tyrantrum en un desvio y cuenta las que chocarian con otra parte del camino. */
    private static int marcarDesvio(Desvio d, boolean aplicar) {
        int malos = 0;
        DESVIOS.add(d);                                 // desfase() lo usa
        try {
            for (double ss = d.a0() - 2; ss <= d.b1() + 2; ss += 0.5) {
                int k = Math.max(0, Math.min(NM - 1, (int) Math.round(ss / PASO)));
                double off = desfase(ss);
                double cx = MX[k] + off * -MTZ[k], cz = MZ[k] + off * MTX[k];
                int ix0 = (int) Math.floor(cx), iz0 = (int) Math.floor(cz), R = (int) Math.ceil(RADIO_CARRIL + 3);
                for (int iz = iz0 - R; iz <= iz0 + R; iz++) for (int ix = ix0 - R; ix <= ix0 + R; ix++) {
                    int x = ix - GX0, z = iz - GZ0;
                    if (x < 0 || z < 0 || x >= GW || z >= GH) continue;
                    int id = x + z * GW;
                    double dist = Math.hypot(ix + 0.5 - cx, iz + 0.5 - cz);
                    if (dist > RADIO_CARRIL + 3) continue;
                    boolean ajeno = GS[id] < d.a0() - 8 || GS[id] > d.b1() + 8;
                    if (dist <= RADIO_CARRIL) {
                        if (((CAT[id] == 1 || CAT[id] == 2) && ajeno) || CAT[id] == 0) malos++;
                        if (aplicar && CAT[id] == 3) CARRIL[id] = 1;
                        else if (aplicar && CAT[id] == 2 && !ajeno) CARRIL[id] = 2;
                    } else if (aplicar && CARRIL[id] == 0) CARRIL[id] = 3;
                }
            }
        } finally { DESVIOS.remove(DESVIOS.size() - 1); }
        return malos;
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

    /**
     * Desplazamiento en x de cada copia. El mundo de la huida se guarda en disco y el terreno viejo NO se borra al construir: si cambia la forma del
     * camino, los bloques de la version anterior (muros, columnas) se quedan y cierran el camino nuevo. Por eso {@code BASE_X} hay que SUBIRLO (en
     * multiplos de 16 y mas alla de lo ocupado: 12 copias * SEP) cada vez que cambie la ruta o los muros; asi las copias nuevas salen en terreno virgen.
     * (0.2.0 y 0.3.0: 0; 0.4.0 y 0.5.0: 8192; 0.6.0: 16384.)
     */
    static final int BASE_X = 16384;
    static int ox(int hueco) { return BASE_X + hueco * SEP; }

    // ---------------------------------------------------------------- geometria (servidor)

    private static int indice(double s) { return Math.max(0, Math.min(NM - 2, (int) (s / PASO))); }

    /** {x, z, yaw} (coordenadas del mundo) del eje del camino a distancia s del principio. */
    static double[] ejePos(int hueco, double s) {
        s = Math.max(0, Math.min(S_TOTAL, s));
        int i = indice(s);
        double f = Math.max(0, Math.min(1, (s - MS[i]) / (MS[i + 1] - MS[i])));
        return new double[]{ox(hueco) + MX[i] + (MX[i + 1] - MX[i]) * f, MZ[i] + (MZ[i + 1] - MZ[i]) * f, rumbo(s)};
    }

    /** {x, y, z, yaw} del Tyrantrum a distancia s: el eje del camino y, en los puentes, su desvio por la selva (mas bajo, a nivel del suelo de la selva). */
    static double[] perseguidorPos(int hueco, double s) {
        double[] e = ejePos(hueco, s);
        double off = desfase(s);
        if (off == 0) return new double[]{e[0], Y0, e[1], e[2]};
        int i = indice(Math.max(0, Math.min(S_TOTAL, s)));
        double x = e[0] + off * -MTZ[i], z = e[1] + off * MTX[i];
        double pend = desfase(s + 0.5) - desfase(s - 0.5);                          // lateral que gana por bloque avanzado
        double yaw = e[2] + Math.toDegrees(Math.atan(pend));
        double hw = mediaAncho(s), wt = grosorMuro(s);
        double y = Y0 - 2 * suave((Math.abs(off) - (hw - 1.0)) / (wt + 3.0));
        return new double[]{x, y, z, yaw};
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
    record Construido(List<Bloque> obstaculos, List<Bloque> puente, List<Check> checks, List<Bloque> maleza, List<Bloque> entrada) {}

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
                        if (CARRIL[id] == 2) {          // el Tyrantrum atraviesa el muro por aqui: maleza en vez de piedra (se pone en cada partida y el la destroza al pasar); aqui solo la base
                            w.setBlockState(p.set(ox + ix, Y_SELVA, iz), Blocks.MOSS_BLOCK.getDefaultState(), F);
                            break;
                        }
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
                        if (CARRIL[id] == 1) {          // sendero del Tyrantrum alrededor del puente
                            y = Y_SELVA;
                            int h = hash(ix, iz, 4) % 6;
                            w.setBlockState(p.set(ox + ix, y, iz), h == 0 ? Blocks.COARSE_DIRT.getDefaultState() : h == 1 ? Blocks.MOSSY_COBBLESTONE.getDefaultState() : Blocks.DIRT_PATH.getDefaultState(), F);
                            w.setBlockState(p.set(ox + ix, y - 1, iz), Blocks.DIRT.getDefaultState(), F);
                            break;
                        }
                        w.setBlockState(p.set(ox + ix, y, iz), Blocks.GRASS_BLOCK.getDefaultState(), F);
                        w.setBlockState(p.set(ox + ix, y - 1, iz), Blocks.DIRT.getDefaultState(), F);
                        if (CARRIL[id] == 0 && hash(ix, iz, 7) % 52 == 0 && GD[id] > mediaAncho(GS[id]) + grosorMuro(GS[id]) + 3) arbol(w, p, ox + ix, y + 1, iz, hash(ix, iz, 9));
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
            // la entrada: dos torres de piedra a los lados del camino y un dintel por encima de la reja (los barrotes se ponen en cada partida)
            for (int iz = GZ0; iz < GZ0 + GH; iz++) for (int ix = GX0; ix < GX0 + GW; ix++) {
                int id = (ix - GX0) + (iz - GZ0) * GW;
                if (Math.abs(GS[id] - S_ENTRADA) > 1.5) continue;
                if (CAT[id] == 2) {
                    for (int y = ALT[id]; y <= 14; y++) w.setBlockState(p.set(ox + ix, Y0 + y, iz), y == 14 || y == 10 && hash(ix, iz, 3) % 2 == 0 ? Blocks.CHISELED_STONE_BRICKS.getDefaultState() : MURO[hash(ix, iz, y + 5) % MURO.length].getDefaultState(), F);
                    if (hash(ix, iz, 17) % 3 == 0) w.setBlockState(p.set(ox + ix, Y0 + 15, iz), Blocks.LANTERN.getDefaultState(), F);
                } else if (CAT[id] == 1) {
                    w.setBlockState(p.set(ox + ix, Y0 + ENTRADA_ALTO, iz), (Math.abs(GS[id] - S_ENTRADA) < 0.5 ? Blocks.CHISELED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS).getDefaultState(), F);
                    w.setBlockState(p.set(ox + ix, Y0 + ENTRADA_ALTO + 1, iz), Blocks.STONE_BRICKS.getDefaultState(), F);
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
        List<Bloque> obst = new ArrayList<>(), puente = new ArrayList<>(), maleza = new ArrayList<>(), entrada = new ArrayList<>();
        BlockState barrote = Blocks.IRON_BARS.getDefaultState().with(net.minecraft.block.PaneBlock.EAST, true).with(net.minecraft.block.PaneBlock.WEST, true);
        for (int iz = GZ0; iz < GZ0 + GH; iz++) for (int ix = GX0; ix < GX0 + GW; ix++) {
            int id = (ix - GX0) + (iz - GZ0) * GW;
            if (CAT[id] != 1 || Math.abs(GS[id] - S_ENTRADA) > 0.5) continue;
            for (int y = 0; y < ENTRADA_ALTO; y++) entrada.add(poner(w, ox + ix, Y0 + y, iz, barrote, y));       // «s» = fila (0 = abajo): la reja sube quitando filas desde abajo
        }
        BlockState hoja = Blocks.JUNGLE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        for (int iz = GZ0; iz < GZ0 + GH; iz++) for (int ix = GX0; ix < GX0 + GW; ix++) {
            int id = (ix - GX0) + (iz - GZ0) * GW;
            if (CAT[id] != 2 || CARRIL[id] != 2) continue;
            for (int y = Y_SELVA - Y0 + 1; y < 4; y++) maleza.add(poner(w, ox + ix, Y0 + y, iz, hoja, GS[id]));
        }
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
            checks.add(new Check(sc, e[0] - ox(0), e[1], (float) e[2]));      // x local de la copia
        }
        checks.sort((a, b) -> Double.compare(a.s(), b.s()));
        maleza.sort((a, b) -> Double.compare(a.s(), b.s()));
        return new Construido(obst, puente, checks, maleza, entrada);
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
