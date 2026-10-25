package emi.cafe;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Los cafes tematicos (0.27.0): edificios ya construidos que se compran con monedas. Cada uno es un {@link Obras.Diseno} generado por codigo con la misma forma que el local de siempre
 * (paredes de 4 de alto, ventanas al sur, puerta en x = 7-8, barra con hueco en z = 6, estaciones en z = 1 y 5, mesas en z = 9) para que clientes y empleados lo recorran igual, mas
 * la decoracion del tema. Incluye la caja registradora, el cartel, el libro, las estaciones que el dueño ya tiene (en su sitio de siempre) y las mesas que le caben; lo que le falte de
 * bloques lo entrega la compra ({@link Obras#comprarTematico}).
 */
final class Tematicos {
    private Tematicos() {}

    record Tema(String id, String nombre, String descripcion, int precio) {}

    static final List<Tema> TEMAS = List.of(
            new Tema("halloween", "Café de Halloween", "Piedra oscura, calabazas encendidas, telarañas y velas. Para la noche de brujas todo el año.", 1400),
            new Tema("playa", "Café de playa", "Bambú, arena, cristal azul y palmeras. Entra la luz del mar.", 1400),
            new Tema("michialien", "Café Michialien", "Rosa y lila como la sala de Emi, con cerezos, pétalos y velas rosas.", 2200),
            new Tema("navidad", "Café navideño", "Abeto, rojo y blanco, abetos pequeños y luces cálidas.", 1400));

    static Tema tema(String id) {
        for (Tema t : TEMAS) if (t.id().equals(id)) return t;
        return null;
    }

    /** Los materiales de un tema. */
    private record Mat(String sueloCocina, String sueloA, String sueloB, String pared, String franja, String ventana, String techo, String luz, String marco, String mesa) {}

    private static Mat materiales(String id) {
        return switch (id) {
            case "halloween" -> new Mat("polished_blackstone_bricks", "polished_blackstone", "orange_terracotta", "deepslate_bricks", "dark_oak_planks", "purple_stained_glass",
                    "dark_oak_planks", "jack_o_lantern[facing=south]", "stripped_dark_oak_log", "emi_cafe:mesa_negra");
            case "playa" -> new Mat("white_concrete", "smooth_sandstone", "cut_sandstone", "bamboo_planks", "stripped_bamboo_block", "light_blue_stained_glass",
                    "bamboo_mosaic", "sea_lantern", "stripped_bamboo_block", "emi_cafe:mesa_madera");
            case "michialien" -> new Mat("pink_terracotta", "pink_concrete", "purple_concrete", "pink_terracotta", "purple_concrete", "pink_stained_glass",
                    "pink_stained_glass", "sea_lantern", "stripped_cherry_log", "emi_cafe:mesa_rosa");
            default -> new Mat("spruce_planks", "red_terracotta", "white_terracotta", "spruce_planks", "red_concrete", "glass",
                    "spruce_planks", "glowstone", "stripped_spruce_log", "emi_cafe:mesa_blanca");
        };
    }

    private static final class B {
        final Map<String, String> m = new LinkedHashMap<>();
        static String k(int x, int y, int z) { return x + "," + y + "," + z; }
        void p(int x, int y, int z, BlockState s) { m.put(k(x, y, z), Decor.guardar(s)); }
        void p(int x, int y, int z, String b) { p(x, y, z, b.contains(":") ? estadoMod(b) : Obra.b(b)); }
        /** Pone el bloque solo si la casilla esta libre (para la decoracion: nunca pisa una estacion, una mesa ni una pared). */
        void sobre(int x, int y, int z, String b) { if (!m.containsKey(k(x, y, z))) p(x, y, z, b); }
        boolean hay(int x, int y, int z) { return m.containsKey(k(x, y, z)); }
    }

    private static BlockState estadoMod(String id) {
        Block b = Registries.BLOCK.get(Identifier.of(id));
        return b.getDefaultState();
    }

    /** Construye el diseño del tema para el café de {@code c}. */
    static Obras.Diseno construir(Cafe c, Tema t) {
        Mat mt = materiales(t.id());
        // las estaciones: una copia de cada tipo que el dueño ya tiene (puesta, guardada o de un plato que sabe), en su sitio de siempre
        Set<Estacion> est = new LinkedHashSet<>();
        Set<Estacion> activas = c.activas();
        for (Estacion e : Estacion.values()) {
            if (e.x < 0 || e.tipo == Estacion.Tipo.MESA) continue;
            if (e.siempre || activas.contains(e) || !c.obras.copias(e).isEmpty() || c.obras.stock(Obras.idItem(e.bloque)) > 0) est.add(e);
        }
        int nMesas = Math.min(c.numMesas(), Local.MESAS.length);
        int necesario = 19;
        for (int i = 0; i < nMesas; i++) necesario = Math.max(necesario, Local.MESAS[i][0] + 2);
        int W = Local.ancho(1);
        for (int k = 1; k <= 4 && W < necesario; k++) W = Local.ancho(k);
        B b = new B();

        // suelo: damero en el comedor, liso en la cocina
        for (int x = 0; x <= W; x++) for (int z = 0; z <= 13; z++) b.p(x, 0, z, z <= 6 ? mt.sueloCocina() : ((x + z) & 1) == 0 ? mt.sueloA() : mt.sueloB());
        // paredes de 4 de alto con franja a la altura de los ojos
        for (int y = 1; y <= 4; y++) {
            String mat = y == 2 ? mt.franja() : mt.pared();
            for (int x = 0; x <= W; x++) { b.p(x, y, 0, mat); b.p(x, y, 13, mat); }
            for (int z = 0; z <= 13; z++) { b.p(0, y, z, mat); b.p(W, y, z, mat); }
        }
        // ventanas al sur (y 2-3) a los lados de la puerta, y la puerta (x 7-8) con su marco
        for (int x = 1; x < W; x++) if (x < 6 || x > 9) if (x % 4 != 0) for (int y = 2; y <= 3; y++) b.p(x, y, 13, mt.ventana());
        for (int y = 1; y <= 3; y++) { m(b, 7, y, 13); m(b, 8, y, 13); }
        for (int y = 1; y <= 4; y++) { b.p(6, y, 13, mt.marco()); b.p(9, y, 13, mt.marco()); }
        for (int x = 6; x <= 9; x++) b.p(x, 4, 13, mt.marco());
        // techo con luces cada 4 casillas, y el borde de arriba
        for (int x = 1; x < W; x++) for (int z = 1; z <= 12; z++) b.p(x, 5, z, (x % 4 == 1 && z % 4 == 1) ? mt.luz() : mt.techo());
        for (int x = 0; x <= W; x++) { b.p(x, 5, 0, mt.pared()); b.p(x, 5, 13, mt.pared()); }
        for (int z = 0; z <= 13; z++) { b.p(0, 5, z, mt.pared()); b.p(W, 5, z, mt.pared()); }

        // la cocina: fila de atras (z = 1), segunda fila (z = 5) si hace falta, barra (z = 6) con el hueco x = 7-8 y la caja registradora
        Set<Integer> fila1 = new LinkedHashSet<>(), fila5 = new LinkedHashSet<>();
        for (Estacion e : est) if (e.x < W) { if (e.z == 5) fila5.add(e.x); else fila1.add(e.x); }
        BlockState encimera = CafeBlocks.ENCIMERA.getDefaultState();
        for (int x = 1; x < W; x++) b.p(x, 1, 1, encimera);
        if (!fila5.isEmpty()) for (int x = 1; x < W; x++) if (x != 7 && x != 8) b.p(x, 1, 5, encimera);
        for (int x = 1; x < W; x++) if (x != 7 && x != 8) b.p(x, 1, 6, encimera);
        for (Estacion e : est) if (e.x < W) b.p(e.x, 1, e.z, e.bloque.getDefaultState());
        // las despensas de las fincas del dueño, al final de las filas de la cocina (x desde 19)
        List<Block> desp = new ArrayList<>();
        for (Datos.Finca f : c.dj().granja) { Block bl = CafeBlocks.DESPENSAS.get(f.tipo); if (bl != null && !desp.contains(bl)) desp.add(bl); }
        if (!desp.isEmpty()) {
            if (desp.size() > W - 19 && fila5.isEmpty()) for (int x = 1; x < W; x++) if (x != 7 && x != 8) b.p(x, 1, 5, encimera);
            int i = 0;
            for (int x = 19; x < W && i < desp.size(); x++) b.p(x, 1, 1, desp.get(i++).getDefaultState());
            for (int x = 19; x < W && i < desp.size(); x++) b.p(x, 1, 5, desp.get(i++).getDefaultState());
        }
        b.p(Local.X_REGISTRADORA, 1, Local.Z_REGISTRADORA, CafeBlocks.REGISTRADORA.getDefaultState());
        b.p(Local.X_LIBRO, Local.Y_LIBRO, Local.Z_LIBRO, CafeBlocks.LIBRO_PARED.getDefaultState());
        for (int k = 0; k < 2; k++) b.p(Local.X_CARTEL + k, Local.Y_CARTEL, Local.Z_CARTEL, CafeBlocks.CARTEL.getDefaultState().with(PlacaBlock.ABIERTO, false).with(PlacaBlock.DER, k == 1));
        // las mesas
        BlockState mesa = estadoMod(mt.mesa());
        for (int i = 0; i < nMesas; i++) b.p(Local.MESAS[i][0], 1, Local.MESAS[i][1], mesa);

        decorar(b, t.id(), W, fila1);
        return new Obras.Diseno(b.m, new LinkedHashSet<>(), new LinkedHashMap<>());
    }

    /** Quita lo que hubiera en una casilla del diseño (la puerta queda abierta). */
    private static void m(B b, int x, int y, int z) { b.m.remove(B.k(x, y, z)); }

    /** La decoracion de cada tema: solo en los bordes, el techo y los rincones, para no tapar el paso de los clientes (aseo: x 7-8 y la fila z 11-12 quedan libres). */
    private static void decorar(B b, String id, int W, Set<Integer> fila1) {
        int[] rincones = {1, W - 1};
        switch (id) {
            case "halloween" -> {
                for (int x : rincones) for (int z : new int[]{2, 12}) b.sobre(x, 4, z, "cobweb");
                for (int x = 3; x < W - 2; x += 8) for (int z : new int[]{3, 9}) b.sobre(x, 4, z, "soul_lantern[hanging=true]");
                for (int z = 8; z <= 11; z++) for (int x : rincones) b.sobre(x, 1, z, "orange_carpet");
                for (int x = 1; x < W; x += 3) if (!fila1.contains(x) && !(x == 8)) b.sobre(x, 2, 1, x % 2 == 0 ? "carved_pumpkin[facing=south]" : "orange_candle[candles=3,lit=true]");
                b.sobre(4, 1, 14, "jack_o_lantern[facing=south]"); b.sobre(11, 1, 14, "jack_o_lantern[facing=south]");
            }
            case "playa" -> {
                for (int x : new int[]{1, W - 1}) {                                         // palmeras en los rincones del comedor
                    for (int y = 1; y <= 3; y++) b.sobre(x, y, 12, "jungle_log[axis=y]");
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 0; dz++) {
                        int xx = x + dx;
                        if (xx < 1 || xx > W - 1) continue;
                        b.sobre(xx, 4, 12 + dz, "jungle_leaves[persistent=true]");
                    }
                    b.sobre(x == 1 ? 2 : W - 2, 4, 12, "jungle_leaves[persistent=true]");
                }
                for (int z = 8; z <= 11; z++) for (int x : rincones) b.sobre(x, 1, z, z % 2 == 0 ? "light_blue_carpet" : "white_carpet");
                for (int x = 3; x < W - 2; x += 8) for (int z : new int[]{3, 9}) b.sobre(x, 4, z, "lantern[hanging=true]");
                for (int x = 1; x < W; x += 3) if (!fila1.contains(x) && x != 8) b.sobre(x, 2, 1, "sea_pickle[pickles=3]");
            }
            case "michialien" -> {
                for (int x : rincones) for (int z : new int[]{2, 12}) for (int y = 1; y <= 2; y++) b.sobre(x, y, z, "cherry_leaves[persistent=true]");
                for (int z = 8; z <= 11; z++) for (int x : rincones) b.sobre(x, 1, z, z % 2 == 0 ? "pink_carpet" : "purple_carpet");
                for (int x = 3; x < W - 2; x += 8) for (int z : new int[]{3, 9}) b.sobre(x, 4, z, "lantern[hanging=true]");
                for (int x = 1; x < W; x += 3) if (!fila1.contains(x) && x != 8) b.sobre(x, 2, 1, "pink_candle[candles=3,lit=true]");
                for (int x : rincones) b.sobre(x, 1, 7, "pink_petals[flower_amount=4,facing=north]");
            }
            default -> {                                                                  // navidad
                for (int x : rincones) {                                                  // abetos pequeños en los rincones del comedor
                    b.sobre(x, 1, 12, "spruce_log[axis=y]"); b.sobre(x, 2, 12, "spruce_log[axis=y]");
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 0; dz++) { int xx = x + dx; if (xx >= 1 && xx <= W - 1) b.sobre(xx, 3, 12 + dz, "spruce_leaves[persistent=true]"); }
                    b.sobre(x, 4, 12, "spruce_leaves[persistent=true]");
                }
                for (int z = 8; z <= 11; z++) for (int x : rincones) b.sobre(x, 1, z, z % 2 == 0 ? "red_carpet" : "green_carpet");
                for (int x = 3; x < W - 2; x += 8) for (int z : new int[]{3, 9}) b.sobre(x, 4, z, "lantern[hanging=true]");
                for (int x = 1; x < W; x += 3) if (!fila1.contains(x) && x != 8) b.sobre(x, 2, 1, x % 2 == 0 ? "red_candle[candles=2,lit=true]" : "white_candle[candles=3,lit=true]");
            }
        }
    }

    /** Cuantos bloques lleva el diseño (para ensenarlo en el menu). */
    static int bloques(Cafe c, Tema t) { return construir(c, t).decor().size(); }
}
