package emi.cafe;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Busca caminos por la parcela de un cafe esquivando lo que hay construido (mesas, estaciones, decoracion, paredes): una cuadricula de casillas de 1x1 a la altura de los pies
 * (se puede pisar una casilla si tiene suelo y el bloque de los pies no tiene colision) y un A* de 8 direcciones sin cortar esquinas. Lo usan los clientes y los empleados.
 * Sin Minecraft mas alla de leer bloques; la cuadricula se vuelve a leer cuando cambia algo ({@link #sucia}) o cada medio segundo.
 */
final class Rutas {
    private final ServerWorld w;
    private final int x0, x1, z0, z1, ancho, largo;
    private boolean[] pisable;
    private long leida = Long.MIN_VALUE;
    private boolean sucia = true;

    Rutas(ServerWorld w, int ox) {
        this.w = w;
        this.x0 = ox + Ciudad.X0; this.x1 = ox + Ciudad.X1; this.z0 = Local.Z_ATRAS; this.z1 = Local.Z_FRENTE;
        this.ancho = x1 - x0 + 1; this.largo = z1 - z0 + 1;
    }

    /** Algo ha cambiado en el terreno (se construyo, se quito, se movio una mesa...): hay que volver a mirar. */
    void sucia() { sucia = true; }

    private void leer(long ahora) {
        if (pisable != null && !sucia && ahora - leida < 10) return;
        if (pisable == null) pisable = new boolean[ancho * largo];
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            p.set(x, Local.Y, z);
            BlockState suelo = w.getBlockState(p);
            boolean ok = !suelo.getCollisionShape(w, p).isEmpty();
            if (ok) {
                p.set(x, Local.Y + 1, z);
                BlockState pies = w.getBlockState(p);
                var forma = pies.getCollisionShape(w, p);
                ok = forma.isEmpty() || forma.getMax(Direction.Axis.Y) <= 0.3;
            }
            pisable[(x - x0) * largo + (z - z0)] = ok;
        }
        sucia = false;
        leida = ahora;
    }

    /** ¿Se puede pisar la casilla (x, z) (coordenadas del mundo)? */
    boolean pisable(int x, int z, long ahora) {
        if (x < x0 || x > x1 || z < z0 || z > z1) return false;
        leer(ahora);
        return pisable[(x - x0) * largo + (z - z0)];
    }

    /** ¿Esta libre el punto (x, z) y la caja de 0,3 a cada lado? (para esquivar sin meterse en una pared) */
    boolean libre(double x, double z, long ahora) {
        for (int dx = -1; dx <= 1; dx += 2) for (int dz = -1; dz <= 1; dz += 2)
            if (!pisable((int) Math.floor(x + 0.3 * dx), (int) Math.floor(z + 0.3 * dz), ahora)) return false;
        return true;
    }

    /** Las casillas pisables pegadas (4 direcciones) a un bloque: donde se puede estar para usarlo. */
    List<int[]> adyacentes(BlockPos b, long ahora) {
        List<int[]> r = new ArrayList<>();
        for (Direction d : Direction.Type.HORIZONTAL) {
            int x = b.getX() + d.getOffsetX(), z = b.getZ() + d.getOffsetZ();
            if (pisable(x, z, ahora)) r.add(new int[]{x, z});
        }
        return r;
    }

    /** La casilla pisable mas cercana a (x, z) que no este en {@code usadas} ni a menos de {@code sep} de ninguna de ellas, o null. */
    int[] casillaCerca(double x, double z, List<int[]> usadas, double sep, long ahora) {
        leer(ahora);
        int cx = (int) Math.floor(x), cz = (int) Math.floor(z);
        int[] mejor = null;
        double md = Double.MAX_VALUE;
        for (int r = 0; r <= 40; r++) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                int px = cx + dx, pz = cz + dz;
                if (!pisable(px, pz, ahora)) continue;
                boolean lejos = true;
                for (int[] u : usadas) if (Math.hypot(u[0] - px, u[1] - pz) < sep) { lejos = false; break; }
                double d = Math.hypot(px + 0.5 - x, pz + 0.5 - z);
                if (lejos && d < md) { md = d; mejor = new int[]{px, pz}; }
            }
            if (mejor != null && md <= r) return mejor;
        }
        return mejor;
    }

    private record Nodo(int idx, double f) {}

    /**
     * El camino de (fx, fz) hasta la casilla mas cercana de {@code metas} (coordenadas del mundo: puntos por los que pasar, el ultimo es el centro de la casilla de la meta), o null si no se
     * llega. Si el punto de salida esta metido en una pared o una mesa, se sale por la casilla pisable mas cercana.
     */
    List<double[]> camino(double fx, double fz, List<int[]> metas, long ahora) {
        if (metas.isEmpty()) return null;
        leer(ahora);
        int sx = (int) Math.floor(fx), sz = (int) Math.floor(fz);
        if (!pisable(sx, sz, ahora)) {
            int[] c = casillaCerca(fx, fz, Collections.emptyList(), 0, ahora);
            if (c == null) return null;
            sx = c[0]; sz = c[1];
        }
        boolean[] esMeta = new boolean[ancho * largo];
        for (int[] m : metas) if (m[0] >= x0 && m[0] <= x1 && m[1] >= z0 && m[1] <= z1) esMeta[(m[0] - x0) * largo + (m[1] - z0)] = true;
        int ini = (sx - x0) * largo + (sz - z0);
        double[] g = new double[ancho * largo];
        int[] padre = new int[ancho * largo];
        Arrays.fill(g, Double.MAX_VALUE);
        Arrays.fill(padre, -1);
        g[ini] = 0;
        PriorityQueue<Nodo> cola = new PriorityQueue<>((a, b) -> Double.compare(a.f, b.f));
        cola.add(new Nodo(ini, h(sx, sz, metas)));
        int expandidos = 0, fin = -1;
        while (!cola.isEmpty() && expandidos++ < 9000) {
            Nodo n = cola.poll();
            if (esMeta[n.idx]) { fin = n.idx; break; }
            int cx = n.idx / largo + x0, cz = n.idx % largo + z0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int nx = cx + dx, nz = cz + dz;
                if (!pisable(nx, nz, ahora)) continue;
                if (dx != 0 && dz != 0 && (!pisable(cx + dx, cz, ahora) || !pisable(cx, cz + dz, ahora))) continue;      // sin cortar esquinas
                int ni = (nx - x0) * largo + (nz - z0);
                double ng = g[n.idx] + (dx != 0 && dz != 0 ? 1.4142 : 1.0);
                if (ng < g[ni]) { g[ni] = ng; padre[ni] = n.idx; cola.add(new Nodo(ni, ng + h(nx, nz, metas))); }
            }
        }
        if (fin < 0) return null;
        List<double[]> puntos = new ArrayList<>();
        for (int i = fin; i >= 0; i = padre[i]) puntos.add(new double[]{i / largo + x0 + 0.5, i % largo + z0 + 0.5});
        Collections.reverse(puntos);
        // quitar los puntos intermedios cuando hay linea recta libre entre dos mas lejanos (el camino sale mas natural)
        List<double[]> r = new ArrayList<>();
        int a = 0;
        double[] origen = {fx, fz};
        if (puntos.size() > 1) puntos.remove(0);                       // el primero es la casilla de salida: se va desde donde esta
        double[] desde = origen;
        int i = 0;
        while (i < puntos.size()) {
            int j = puntos.size() - 1;
            while (j > i && !recta(desde, puntos.get(j), ahora)) j--;
            r.add(puntos.get(j));
            desde = puntos.get(j);
            i = j + 1;
        }
        return r;
    }

    private static double h(int x, int z, List<int[]> metas) {
        double m = Double.MAX_VALUE;
        for (int[] t : metas) m = Math.min(m, Math.hypot(t[0] - x, t[1] - z));
        return m;
    }

    /** ¿Se puede ir en linea recta de a a b con 0,3 de margen a cada lado? */
    private boolean recta(double[] a, double[] b, long ahora) {
        double d = Math.hypot(b[0] - a[0], b[1] - a[1]);
        int n = Math.max(1, (int) Math.ceil(d / 0.25));
        for (int k = 1; k <= n; k++) {
            double t = (double) k / n;
            if (!libre(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, ahora)) return false;
        }
        return true;
    }
}
