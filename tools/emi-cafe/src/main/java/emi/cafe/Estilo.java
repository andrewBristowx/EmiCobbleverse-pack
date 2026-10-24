package emi.cafe;

/** Los materiales que el dueño puede elegir para su local en el modo construccion (paredes, franja y los dos suelos). Sin Minecraft: solo nombres de bloques. */
final class Estilo {
    private Estilo() {}

    static final int PARED = 0, FRANJA = 1, COCINA = 2, COMEDOR = 3;
    static final String[] PARTES = {"Paredes", "Franja de la pared", "Suelo de la cocina", "Suelo del comedor"};

    /** Una opcion: nombre y uno o dos bloques (el suelo es un damero de {@code a} y {@code b}). */
    record Op(String nombre, String a, String b) {}

    static final Op[] PAREDES = {
        new Op("Blancas", "white_concrete", null), new Op("Ladrillo", "bricks", null), new Op("Roble", "oak_planks", null),
        new Op("Roble oscuro", "dark_oak_planks", null), new Op("Celestes", "light_blue_concrete", null), new Op("Rosas", "pink_concrete", null),
        new Op("Arenisca", "smooth_sandstone", null), new Op("Piedra", "stone_bricks", null), new Op("Cuarzo", "quartz_block", null),
        new Op("Verde menta", "lime_terracotta", null),
    };
    /** La franja a la altura de los ojos; la primera opcion de «sin franja» usa el bloque de la pared. */
    static final Op[] FRANJAS = {
        new Op("Rosa", "pink_concrete", null), new Op("Sin franja", null, null), new Op("Roja", "red_concrete", null), new Op("Azul", "blue_concrete", null),
        new Op("Amarilla", "yellow_concrete", null), new Op("Verde", "green_concrete", null), new Op("Negra", "black_concrete", null),
        new Op("Morada", "purple_concrete", null), new Op("Naranja", "orange_concrete", null),
    };
    static final Op[] SUELOS = {
        new Op("Gris y blanco", "light_gray_concrete", "white_concrete"), new Op("Rosa y blanco", "pink_concrete", "white_concrete"),
        new Op("Negro y blanco", "black_concrete", "white_concrete"), new Op("Rojo y blanco", "red_concrete", "white_concrete"),
        new Op("Azul y blanco", "light_blue_concrete", "white_concrete"), new Op("Verde y blanco", "lime_concrete", "white_concrete"),
        new Op("Amarillo y blanco", "yellow_concrete", "white_concrete"), new Op("Madera clara", "birch_planks", "oak_planks"),
        new Op("Madera oscura", "dark_oak_planks", "spruce_planks"), new Op("Piedra", "stone", "smooth_stone"),
        new Op("Ladrillos", "stone_bricks", "polished_andesite"), new Op("Oscuro", "gray_concrete", "black_concrete"),
    };

    /** Los valores de siempre (los que tenia el local antes de poder cambiarlos). */
    static final int[] DEFECTO = {0, 0, 0, 1};

    static Op[] opciones(int parte) { return parte == PARED ? PAREDES : parte == FRANJA ? FRANJAS : SUELOS; }

    static int total(int parte) { return opciones(parte).length; }

    /** El indice de la opcion elegida para {@code parte} dentro de {@code v} (corrigiendo valores fuera de rango). */
    static int indice(int[] v, int parte) {
        int n = total(parte);
        int i = v == null || v.length <= parte ? DEFECTO[parte] : v[parte];
        return ((i % n) + n) % n;
    }

    static Op op(int[] v, int parte) { return opciones(parte)[indice(v, parte)]; }

    /** El bloque de la pared (y del borde del techo). */
    static String pared(int[] v) { return op(v, PARED).a(); }

    /** El bloque de la franja a la altura y = 2 (el de la pared si no hay franja). */
    static String franja(int[] v) { String f = op(v, FRANJA).a(); return f == null ? pared(v) : f; }

    /** El bloque del suelo en (x, z): damero de los dos colores. */
    static String suelo(int[] v, int parte, int x, int z) { Op o = op(v, parte); return ((x + z) & 1) == 0 ? o.a() : o.b(); }
}
