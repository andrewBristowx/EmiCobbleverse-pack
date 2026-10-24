package emi.cafe;

import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Busca caminos por la parcela de un cafe esquivando lo que hay construido (mesas, estaciones, decoracion, paredes) y subiendo escaleras (desde 0.18.0, varios pisos).
 * Una «casilla» es una columna (x, z) mas un nivel k (el bloque de debajo de los pies esta a la altura Local.Y + k): se puede pisar si ese bloque aguanta (su colision llega a 1 o menos) y
 * los dos bloques de encima no tienen colision. La altura a la que se pisa es la de la colision del bloque (una losa, 0,5; una alfombra, casi 0; una escalera, 0,75). De una casilla se pasa a la
 * de al lado si la diferencia de altura es de 0,55 como mucho, o de 1 si hay una escalera por medio y se entra o se sale por la direccion de la escalera.
 * La cuadricula se vuelve a leer cuando cambia algo ({@link #sucia}) o cada par de segundos. Lo usan los clientes y los empleados. Un camino son puntos {x, z, y} (y = altura de los pies).
 */
final class Rutas {
    /** Capas de la cuadricula: de Local.Y a Local.Y + NY - 1 (la parcela se construye hasta 9 de alto). */
    static final int NY = 12;
    private static final double SALTO = 0.55, SALTO_ESCALERA = 1.05;

    private final ServerWorld w;
    private final int x0, x1, z0, z1, ancho, largo;
    private float[] h;                  // la colision mas alta de cada bloque (0 = se atraviesa)
    private byte[] esc;                 // 0 = no es escalera; si lo es, hacia donde sube (1 norte, 2 sur, 3 oeste, 4 este)
    private long leida = Long.MIN_VALUE;
    private boolean sucia = true;

    Rutas(ServerWorld w, int ox) {
        this.w = w;
        this.x0 = ox + Ciudad.X0; this.x1 = ox + Ciudad.X1; this.z0 = Local.Z_ATRAS; this.z1 = Local.Z_FRENTE;
        this.ancho = x1 - x0 + 1; this.largo = z1 - z0 + 1;
    }

    /** Algo ha cambiado en el terreno (se construyo, se quito, se movio una mesa...): hay que volver a mirar. */
    void sucia() { sucia = true; }

    private int idx(int x, int z, int k) { return ((x - x0) * largo + (z - z0)) * NY + k; }

    private boolean dentro(int x, int z) { return x >= x0 && x <= x1 && z >= z0 && z <= z1; }

    private void leer(long ahora) {
        if (h != null && !sucia && ahora - leida < 40) return;
        if (h == null) { h = new float[ancho * largo * NY]; esc = new byte[ancho * largo * NY]; }
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int k = 0; k < NY; k++) {
            p.set(x, Local.Y + k, z);
            BlockState s = w.getBlockState(p);
            int i = idx(x, z, k);
            esc[i] = 0;
            if (s.isAir()) { h[i] = 0; continue; }
            net.minecraft.block.Block b = s.getBlock();
            if (b instanceof PuertaCristalBlock) { h[i] = 0; continue; }                 // las puertas de cristal se abren solas: para los caminos no son pared
            if (net.minecraft.registry.Registries.BLOCK.getId(b).getNamespace().equals("emi_cafe") && (b == CafeBlocks.ENCIMERA || b == CafeBlocks.MESA || Estacion.de(b) != null)) { h[i] = 2f; continue; }     // mesas y estaciones: no se pisan
            var forma = s.getCollisionShape(w, p);
            if (forma.isEmpty()) { h[i] = 0; continue; }
            h[i] = (float) forma.getMax(Direction.Axis.Y);
            if (b instanceof StairsBlock && s.get(StairsBlock.HALF) == BlockHalf.BOTTOM) {
                Direction f = s.get(StairsBlock.FACING);
                esc[i] = (byte) (f == Direction.NORTH ? 1 : f == Direction.SOUTH ? 2 : f == Direction.WEST ? 3 : 4);
            }
        }
        sucia = false;
        leida = ahora;
    }

    private static int dx(byte e) { return e == 3 ? -1 : e == 4 ? 1 : 0; }
    private static int dz(byte e) { return e == 1 ? -1 : e == 2 ? 1 : 0; }

    /** ¿Se puede pisar el bloque (x, Y + k, z) como suelo? (sin salirse de la cuadricula) */
    private boolean valida(int x, int z, int k) {
        if (!dentro(x, z) || k < 0 || k > NY - 3) return false;
        float s = h[idx(x, z, k)];
        if (s <= 0f || s > 1.0001f) return false;
        return h[idx(x, z, k + 1)] <= 0f && h[idx(x, z, k + 2)] <= 0f;
    }

    /** La altura de los pies de quien pisa esa casilla. */
    private double altura(int x, int z, int k) {
        int i = idx(x, z, k);
        double s = esc[i] != 0 ? 0.75 : h[i];
        return Local.Y + k + s;
    }

    /** El nivel de la columna (x, z) cuya altura de pies se parece mas a {@code y} (a menos de {@code tol}), o -1. */
    private int nivelCerca(int x, int z, double y, double tol) {
        int mejor = -1;
        double md = tol;
        for (int k = 0; k <= NY - 3; k++) {
            if (!valida(x, z, k)) continue;
            double d = Math.abs(altura(x, z, k) - y);
            if (d <= md) { md = d; mejor = k; }
        }
        return mejor;
    }

    /** ¿Se puede pasar de la casilla (x, z, k) a la de al lado (nx, nz, nk)? */
    private boolean paso(int x, int z, int k, int nx, int nz, int nk) {
        if (!valida(nx, nz, nk)) return false;
        double dy = altura(nx, nz, nk) - altura(x, z, k);
        byte a = esc[idx(x, z, k)], b = esc[idx(nx, nz, nk)];
        int ddx = nx - x, ddz = nz - z;
        if (a != 0 || b != 0) {
            if (ddx != 0 && ddz != 0) return false;                                  // por una escalera no se va en diagonal
            if (a != 0 && (dx(a) != 0 ? ddx == 0 : ddz == 0)) return false;          // y solo se entra y se sale por su eje (de frente o de espaldas)
            if (b != 0 && (dx(b) != 0 ? ddx == 0 : ddz == 0)) return false;
            return Math.abs(dy) <= SALTO_ESCALERA;
        }
        return Math.abs(dy) <= SALTO;
    }

    /** ¿Se puede pisar algo en la columna (x, z) a una altura parecida a {@code y}? */
    boolean pisable(int x, int z, double y, long ahora) {
        if (!dentro(x, z)) return false;
        leer(ahora);
        return nivelCerca(x, z, y, 0.6) >= 0;
    }

    /** ¿Esta libre el punto (x, z) a la altura y, y la caja de 0,3 a cada lado? (para esquivar sin meterse en una pared) */
    boolean libre(double x, double z, double y, long ahora) {
        leer(ahora);
        for (int dx = -1; dx <= 1; dx += 2) for (int dz = -1; dz <= 1; dz += 2) {
            int cx = (int) Math.floor(x + 0.3 * dx), cz = (int) Math.floor(z + 0.3 * dz);
            if (!dentro(cx, cz) || nivelCerca(cx, cz, y, 1.0) < 0) return false;
        }
        return true;
    }

    /** La altura a la que se pisa la columna (x, z) mas cerca de {@code y}; {@code y} si no hay suelo. */
    double alturaCerca(double x, double z, double y, long ahora) {
        leer(ahora);
        int cx = (int) Math.floor(x), cz = (int) Math.floor(z);
        if (!dentro(cx, cz)) return y;
        int k = nivelCerca(cx, cz, y, 1.3);
        return k < 0 ? y : altura(cx, cz, k);
    }

    /** Las casillas pisables pegadas (4 direcciones) a un bloque, a su altura: donde se puede estar para usarlo. Cada una es {x, z, nivel}. */
    List<int[]> adyacentes(BlockPos b, long ahora) {
        leer(ahora);
        List<int[]> r = new ArrayList<>();
        for (Direction d : Direction.Type.HORIZONTAL) {
            int x = b.getX() + d.getOffsetX(), z = b.getZ() + d.getOffsetZ();
            if (!dentro(x, z)) continue;
            int k = nivelCerca(x, z, b.getY(), 0.6);
            if (k >= 0) r.add(new int[]{x, z, k});
        }
        return r;
    }

    /** La casilla pisable mas cercana a (x, z) a la altura y que no este en {@code usadas} ni a menos de {@code sep} de ninguna de ellas ({x, z, nivel}), o null. */
    int[] casillaCerca(double x, double z, double y, List<int[]> usadas, double sep, long ahora) {
        leer(ahora);
        int cx = (int) Math.floor(x), cz = (int) Math.floor(z);
        int[] mejor = null;
        double md = Double.MAX_VALUE;
        for (int r = 0; r <= 40; r++) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                int px = cx + dx, pz = cz + dz;
                if (!dentro(px, pz)) continue;
                int k = nivelCerca(px, pz, y, 0.6);
                if (k < 0) continue;
                boolean lejos = true;
                for (int[] u : usadas) if (Math.hypot(u[0] - px, u[1] - pz) < sep) { lejos = false; break; }
                double d = Math.hypot(px + 0.5 - x, pz + 0.5 - z);
                if (lejos && d < md) { md = d; mejor = new int[]{px, pz, k}; }
            }
            if (mejor != null && md <= r) return mejor;
        }
        return mejor;
    }

    private record Nodo(int idx, double f) {}

    /**
     * El camino de (fx, fz) a la altura fy hasta la casilla mas cercana de {@code metas} ({x, z} o {x, z, nivel}; sin nivel vale cualquiera), o null si no se llega. Son puntos
     * {x, z, y} por los que pasar; el ultimo es el centro de la casilla de la meta. Si el punto de salida esta metido en una pared o una mesa, se sale por la casilla pisable mas cercana.
     */
    List<double[]> camino(double fx, double fz, double fy, List<int[]> metas, long ahora) {
        if (metas.isEmpty()) return null;
        leer(ahora);
        int sx = (int) Math.floor(fx), sz = (int) Math.floor(fz);
        int sk = dentro(sx, sz) ? nivelCerca(sx, sz, fy, 0.7) : -1;
        if (sk < 0) {
            int[] c = casillaCerca(fx, fz, fy, Collections.emptyList(), 0, ahora);
            if (c == null) return null;
            sx = c[0]; sz = c[1]; sk = c[2];
        }
        int total = ancho * largo * NY;
        boolean[] esMeta = new boolean[total];
        for (int[] m : metas) {
            if (!dentro(m[0], m[1])) continue;
            if (m.length > 2) { if (valida(m[0], m[1], m[2])) esMeta[idx(m[0], m[1], m[2])] = true; }
            else for (int k = 0; k <= NY - 3; k++) if (valida(m[0], m[1], k)) esMeta[idx(m[0], m[1], k)] = true;
        }
        int ini = idx(sx, sz, sk);
        double[] g = new double[total];
        int[] padre = new int[total];
        Arrays.fill(g, Double.MAX_VALUE);
        Arrays.fill(padre, -1);
        g[ini] = 0;
        PriorityQueue<Nodo> cola = new PriorityQueue<>((a, b) -> Double.compare(a.f, b.f));
        cola.add(new Nodo(ini, h(sx, sz, metas)));
        int expandidos = 0, fin = -1;
        while (!cola.isEmpty() && expandidos++ < 40000) {
            Nodo n = cola.poll();
            if (esMeta[n.idx]) { fin = n.idx; break; }
            int k = n.idx % NY, col = n.idx / NY;
            int cx = col / largo + x0, cz = col % largo + z0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int nx = cx + dx, nz = cz + dz;
                if (!dentro(nx, nz)) continue;
                for (int nk = k - 1; nk <= k + 1; nk++) {
                    if (nk < 0 || !paso(cx, cz, k, nx, nz, nk)) continue;
                    if (dx != 0 && dz != 0 && (nk != k || !paso(cx, cz, k, cx + dx, cz, k) || !paso(cx, cz, k, cx, cz + dz, k))) continue;      // sin cortar esquinas (y en llano)
                    int ni = idx(nx, nz, nk);
                    double ng = g[n.idx] + (dx != 0 && dz != 0 ? 1.4142 : 1.0) + 0.5 * Math.abs(nk - k);
                    if (ng < g[ni]) { g[ni] = ng; padre[ni] = n.idx; cola.add(new Nodo(ni, ng + h(nx, nz, metas))); }
                }
            }
        }
        if (fin < 0) return null;
        List<double[]> puntos = new ArrayList<>();
        List<Integer> niveles = new ArrayList<>();
        for (int i = fin; i >= 0; i = padre[i]) {
            int k = i % NY, col = i / NY;
            int px = col / largo + x0, pz = col % largo + z0;
            puntos.add(new double[]{px + 0.5, pz + 0.5, altura(px, pz, k)});
            niveles.add(k);
        }
        Collections.reverse(puntos);
        Collections.reverse(niveles);
        if (puntos.size() > 1) { puntos.remove(0); niveles.remove(0); }                     // el primero es la casilla de salida: se va desde donde esta
        // quitar los puntos intermedios cuando hay linea recta libre entre dos mas lejanos del mismo nivel (el camino sale mas natural); en las escaleras se va casilla a casilla
        List<double[]> r = new ArrayList<>();
        double[] desde = {fx, fz, fy};
        int desdeK = sk;
        int i = 0;
        while (i < puntos.size()) {
            int j = puntos.size() - 1;
            while (j > i && !(niveles.get(j) == desdeK && sinEscaleras(i, j, niveles, desdeK) && recta(desde, puntos.get(j), desdeK, ahora))) j--;
            r.add(puntos.get(j));
            desde = puntos.get(j);
            desdeK = niveles.get(j);
            i = j + 1;
        }
        return r;
    }

    private boolean sinEscaleras(int i, int j, List<Integer> niveles, int k) {
        for (int q = i; q <= j; q++) if (niveles.get(q) != k) return false;
        return true;
    }

    private static double h(int x, int z, List<int[]> metas) {
        double m = Double.MAX_VALUE;
        for (int[] t : metas) m = Math.min(m, Math.hypot(t[0] - x, t[1] - z));
        return m;
    }

    /** ¿Se puede ir en linea recta de a a b (en el nivel k, sin escaleras) con 0,3 de margen a cada lado? */
    private boolean recta(double[] a, double[] b, int k, long ahora) {
        double d = Math.hypot(b[0] - a[0], b[1] - a[1]);
        int n = Math.max(1, (int) Math.ceil(d / 0.25));
        for (int q = 1; q <= n; q++) {
            double t = (double) q / n;
            double px = a[0] + (b[0] - a[0]) * t, pz = a[1] + (b[1] - a[1]) * t;
            for (int dx = -1; dx <= 1; dx += 2) for (int dz = -1; dz <= 1; dz += 2) {
                int cx = (int) Math.floor(px + 0.3 * dx), cz = (int) Math.floor(pz + 0.3 * dz);
                if (!dentro(cx, cz) || !valida(cx, cz, k) || esc[idx(cx, cz, k)] != 0) return false;
            }
        }
        return true;
    }
}
