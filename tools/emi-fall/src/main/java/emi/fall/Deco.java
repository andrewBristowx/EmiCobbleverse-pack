package emi.fall;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * La decoracion de Halloween y del Michialien de las arenas: calabazas con luz, farolas de calabaza sobre pilares, telarañas, arboles secos, lapidas, una luna creciente
 * (la de Emi) y la cara del Michialien dibujada con bloques; y, si esta emi-plush, peluches gigantes del michi con sombrero de bruja o cuernos de diablo. Todo se pone
 * una vez por arena (con una marca propia en el fondo del mundo, distinta de la del terreno, para poder rehacerla subiendo {@link #VERSION}).
 */
final class Deco {
    private Deco() {}

    /** Version de la decoracion: al subirla, todas las arenas se redecoran (lo viejo se queda; las marcas del fondo son distintas por version). */
    static final int VERSION = 2;

    // ------------------------------------------------------------------------------------------------ bloques

    static void p(ServerWorld w, int x, int y, int z, String bloque) { Obra.poner(w, x, y, z, bloque); }

    /** ¿Falta decorar esta arena (marca en el fondo del mundo en (10+VERSION, -64, zc))? */
    static boolean pendiente(ServerWorld w, int zc) {
        BlockPos marca = new BlockPos(10 + VERSION, -64, zc);
        return !w.getBlockState(marca).isOf(net.minecraft.block.Blocks.REINFORCED_DEEPSLATE);
    }

    /** Apunta que la decoracion de esta arena esta terminada (se llama al final, para que un fallo a medias se reintente). */
    static void marcar(ServerWorld w, int zc) { Obra.poner(w, 10 + VERSION, -64, zc, "reinforced_deepslate"); }

    private static String dir(String m) { return switch (m) { case "n" -> "north"; case "e" -> "east"; case "w" -> "west"; default -> "south"; }; }

    /** Calabaza con luz mirando a un lado ({@code n, s, e, w}). */
    static void calabaza(ServerWorld w, int x, int y, int z, String mira) { p(w, x, y, z, "jack_o_lantern[facing=" + dir(mira) + "]"); }

    /** Una farola: pilar de piedra oscura desde {@code base} hasta {@code y}, valla y calabaza encima. */
    static void farola(ServerWorld w, int x, int y, int z, int base, String mira) {
        for (int yy = base; yy < y; yy++) p(w, x, yy, z, "polished_blackstone_bricks");
        p(w, x, y, z, "dark_oak_fence"); p(w, x, y + 1, z, "dark_oak_fence");
        calabaza(w, x, y + 2, z, mira);
        p(w, x, y + 3, z, "dark_oak_slab");
    }

    static void arbolSeco(ServerWorld w, int x, int y, int z, int alto, Random r) {
        for (int i = 0; i < alto; i++) p(w, x, y + i, z, "dark_oak_log");
        for (int k = 0; k < 4; k++) {
            int dx = (k == 0 ? 1 : k == 1 ? -1 : 0), dz = (k == 2 ? 1 : k == 3 ? -1 : 0);
            int yy = y + alto - 1 - r.nextInt(2), largo = 1 + r.nextInt(3);
            for (int i = 1; i <= largo; i++) p(w, x + dx * i, yy + (i > 1 ? 1 : 0), z + dz * i, "dark_oak_fence");
            if (r.nextInt(3) == 0) p(w, x + dx * largo, yy + 1, z + dz * largo, "cobweb");
        }
        p(w, x, y + alto, z, "dark_oak_fence");
    }

    static void lapida(ServerWorld w, int x, int y, int z, int giro) {
        p(w, x, y, z, "polished_blackstone_brick_wall"); p(w, x, y + 1, z, "polished_blackstone_brick_wall");
        p(w, x, y + 2, z, "stone_brick_slab");
        if (giro % 2 == 0) { p(w, x + 1, y, z, "mossy_cobblestone_slab"); } else { p(w, x, y, z + 1, "mossy_cobblestone_slab"); }
    }

    static void telarañas(ServerWorld w, int x, int y, int z, int n, Random r) {
        for (int i = 0; i < n; i++) p(w, x + r.nextInt(3) - 1, y + r.nextInt(2), z + r.nextInt(3) - 1, "cobweb");
    }

    /** Una luna creciente brillante en un plano vertical (eje 'x' = el plano XY, 'z' = el plano ZY), de radio {@code r} centrada en (cx, cy, cz). */
    static void luna(ServerWorld w, int cx, int cy, int cz, int r, char eje) {
        for (int a = -r; a <= r; a++) for (int b = -r; b <= r; b++) {
            double d1 = Math.hypot(a, b), d2 = Math.hypot(a - r * 0.45, b - r * 0.2);
            if (d1 <= r && d2 > r * 0.85) p(w, eje == 'x' ? cx + a : cx, cy + b, eje == 'x' ? cz : cz + a, (d1 > r - 1.2) ? "shroomlight" : "glowstone");
        }
    }

    /**
     * Dibuja con bloques una imagen vertical. {@code filas} de arriba abajo; cada caracter es un bloque segun {@code leyenda} («.» = nada). El plano es XY si {@code eje == 'x'} y ZY si
     * es 'z'; {@code esc} es el tamaño de cada «pixel» en bloques. La esquina superior izquierda va en (x, y, z).
     */
    static void imagen(ServerWorld w, int x, int y, int z, char eje, int esc, String[] filas, java.util.Map<Character, String> leyenda) {
        for (int f = 0; f < filas.length; f++) for (int c = 0; c < filas[f].length(); c++) {
            String b = leyenda.get(filas[f].charAt(c));
            if (b == null) continue;
            for (int dy = 0; dy < esc; dy++) for (int dc = 0; dc < esc; dc++) {
                int px = eje == 'x' ? x + c * esc + dc : x, pz = eje == 'x' ? z : z + c * esc + dc;
                p(w, px, y - f * esc - dy, pz, b);
            }
        }
    }

    private static final String[] MICHI = {
            "..Y.......Y..",
            "..a.......a..",
            ".WW.......WW.",
            "WPWW.....WWPW",
            "WWWWWWWWWWWWW",
            "WWKKWWWWWKKWW",
            "WWKKWWWWWKKWW",
            "WWWWWWWWWWWWW",
            "WPPWWWNWWWPPW",
            "WWWWWKKKWWWWW",
            ".WWWWWWWWWWW.",
            "..WWWWWWWWW..",
    };
    private static final java.util.Map<Character, String> LEY_MICHI = java.util.Map.of('W', "white_concrete", 'a', "light_gray_concrete", 'Y', "glowstone", 'K', "black_concrete", 'P', "pink_concrete", 'N', "magenta_concrete");

    /** La cara del Michialien (blanco, ojos negros, antenas amarillas), de pie sobre {@code y}; {@code eje} = el plano donde se dibuja. */
    static void michi(ServerWorld w, int x, int y, int z, char eje, int esc) {
        imagen(w, x, y + MICHI.length * esc - 1, z, eje, esc, MICHI, LEY_MICHI);
    }

    // ------------------------------------------------------------------------------------------------ peluches de emi-plush

    /** Pone un peluche gigante de emi-plush (si el mod esta), mirando hacia donde digas ({@code n, s, e, w}). Devuelve false si no existe ese bloque. */
    static boolean peluche(ServerWorld w, int x, int y, int z, String id, String mira) {
        try {
            Block b = Registries.BLOCK.get(Identifier.of("emi_plush", id));
            if (b == null || Registries.BLOCK.getId(b).getPath().equals("air")) return false;
            BlockState st = b.getDefaultState();
            net.minecraft.state.property.Property<?> pr = b.getStateManager().getProperty("facing");
            if (pr instanceof net.minecraft.state.property.DirectionProperty dp) {
                net.minecraft.util.math.Direction d = switch (mira) { case "n" -> net.minecraft.util.math.Direction.NORTH; case "e" -> net.minecraft.util.math.Direction.EAST; case "w" -> net.minecraft.util.math.Direction.WEST; default -> net.minecraft.util.math.Direction.SOUTH; };
                if (dp.getValues().contains(d)) st = st.with(dp, d);
            }
            Obra.poner(w, x, y, z, st);
            return true;
        } catch (Throwable t) { EmiFall.LOG.debug("peluche {}: {}", id, t.toString()); return false; }
    }

    static final String[] PELUCHES = {"gigante_gatitoalien_witch_hat", "gigante_gatitoalien_devil_horns", "gigante_gatitoalien_halo", "gigante_gatitoalien_gold_witch_hat", "gigante_michi_dramatico"};

    /** Un pedestal con un peluche gigante del michi encima (si no esta emi-plush, una calabaza grande de bloques). */
    static void pedestal(ServerWorld w, int x, int y, int z, String id, String mira) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) { p(w, x + dx, y, z + dz, "polished_blackstone_bricks"); p(w, x + dx, y - 1, z + dz, "polished_blackstone_bricks"); }
        p(w, x, y, z, "gilded_blackstone");
        if (!peluche(w, x, y + 1, z, id, mira)) { calabaza(w, x, y + 1, z, mira); calabaza(w, x, y + 2, z, mira); }
    }

    /** Un anillo de farolas de calabaza de radio {@code r} (n puestos). */
    static void anilloFarolas(ServerWorld w, int cx, int cz, double r, int n, int y, int base) {
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            farola(w, (int) Math.round(cx + Math.cos(a) * r), y, (int) Math.round(cz + Math.sin(a) * r), base, "s");
        }
    }

    /** Un anillo de bloques invisibles (barrera) que impide caerse por el borde: de radio {@code r}, desde {@code y0} hasta {@code y1}. */
    static void anilloBarrera(ServerWorld w, int cx, int cz, double r, int y0, int y1) {
        int n = (int) Math.ceil(r) + 2;
        for (int dx = -n; dx <= n; dx++) for (int dz = -n; dz <= n; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d >= r && d < r + 1.2) for (int y = y0; y <= y1; y++) p(w, cx + dx, y, cz + dz, "barrier");
        }
    }
}
