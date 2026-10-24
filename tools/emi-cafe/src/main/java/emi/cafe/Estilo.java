package emi.cafe;

/** Los materiales que el dueño puede elegir para su local en el modo construccion (paredes, franja y los dos suelos). Sin Minecraft: solo nombres de bloques. */
final class Estilo {
    private Estilo() {}

    static final int PARED = 0, FRANJA = 1, COCINA = 2, COMEDOR = 3, TECHO = 4, VENTANA = 5;
    static final String[] PARTES = {"Paredes", "Franja de la pared", "Suelo de la cocina", "Suelo del comedor", "Techo", "Ventanas"};

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

    /** El techo: {@code a} = el bloque del techo, {@code b} = la luz que se pone cada pocos bloques (los techos de cristal dejan pasar el sol; los demas llevan mas luces). */
    static final Op[] TECHOS = {
        new Op("Cristal", "glass", "sea_lantern"), new Op("Cristal rosa", "pink_stained_glass", "sea_lantern"), new Op("Cristal celeste", "light_blue_stained_glass", "sea_lantern"),
        new Op("Cristal amarillo", "yellow_stained_glass", "glowstone"), new Op("Cristal morado", "purple_stained_glass", "sea_lantern"), new Op("Cristal verde", "lime_stained_glass", "sea_lantern"),
        new Op("Madera clara", "birch_planks", "sea_lantern"), new Op("Madera oscura", "dark_oak_planks", "glowstone"), new Op("Ladrillo", "bricks", "glowstone"),
        new Op("Blanco", "white_concrete", "sea_lantern"),
    };

    /** Las ventanas (paneles de cristal de las paredes): {@code a} = el bloque del panel. */
    static final Op[] VENTANAS = {
        new Op("Cristal", "glass_pane", null), new Op("Cristal rosa", "pink_stained_glass_pane", null), new Op("Cristal celeste", "light_blue_stained_glass_pane", null),
        new Op("Cristal amarillo", "yellow_stained_glass_pane", null), new Op("Cristal morado", "purple_stained_glass_pane", null), new Op("Cristal verde", "lime_stained_glass_pane", null),
        new Op("Cristal blanco", "white_stained_glass_pane", null), new Op("Cristal negro", "black_stained_glass_pane", null), new Op("Barrotes", "iron_bars", null),
    };

    /** Los valores de siempre (los que tenia el local antes de poder cambiarlos). */
    static final int[] DEFECTO = {0, 0, 0, 1, 0, 0};

    static Op[] opciones(int parte) { return parte == PARED ? PAREDES : parte == FRANJA ? FRANJAS : parte == TECHO ? TECHOS : parte == VENTANA ? VENTANAS : SUELOS; }

    /** Una lista de pintura de la longitud de hoy (null o corta en los archivos viejos). */
    static String[] normalizarPintura(String[] p) {
        String[] r = new String[PARTES.length];
        if (p != null) for (int i = 0; i < Math.min(p.length, r.length); i++) r[i] = p[i] == null || p[i].isEmpty() ? null : p[i];
        return r;
    }

    /** El bloque (sin «minecraft:») con el que el dueño pinto esa parte, o null si no la pinto. */
    static String pintado(String[] p, int parte) {
        if (p == null || parte >= p.length || p[parte] == null || p[parte].isEmpty()) return null;
        String s = p[parte];
        return s.startsWith("minecraft:") ? s.substring(10) : s;
    }

    /** Una lista de estilo de la longitud de hoy (las de antes de existir el techo tienen 4 valores): lo que falta toma el valor de siempre. */
    static int[] normalizar(int[] v) {
        int[] r = DEFECTO.clone();
        if (v != null) for (int i = 0; i < Math.min(v.length, r.length); i++) r[i] = v[i];
        return r;
    }

    static int total(int parte) { return opciones(parte).length; }

    /** El indice de la opcion elegida para {@code parte} dentro de {@code v} (corrigiendo valores fuera de rango). */
    static int indice(int[] v, int parte) {
        int n = total(parte);
        int i = v == null || v.length <= parte ? DEFECTO[parte] : v[parte];
        return ((i % n) + n) % n;
    }

    static Op op(int[] v, int parte) { return opciones(parte)[indice(v, parte)]; }

    /** El bloque de la pared (y del borde del techo). */
    static String pared(int[] v, String[] p) { String s = pintado(p, PARED); return s != null ? s : op(v, PARED).a(); }

    /** El bloque de la franja a la altura y = 2 (el de la pared si no hay franja). */
    static String franja(int[] v, String[] p) { String s = pintado(p, FRANJA); if (s != null) return s; String f = op(v, FRANJA).a(); return f == null ? pared(v, p) : f; }

    /** El bloque del suelo en (x, z): damero de los dos colores (o un solo bloque si el dueño lo pinto). */
    static String suelo(int[] v, String[] p, int parte, int x, int z) { String s = pintado(p, parte); if (s != null) return s; Op o = op(v, parte); return ((x + z) & 1) == 0 ? o.a() : o.b(); }

    /** El bloque del techo, el de sus luces y cada cuantos bloques va una luz (un techo opaco lleva una cada 3 en vez de cada 4). */
    static String techo(int[] v, String[] p) { String s = pintado(p, TECHO); return s != null ? s : op(v, TECHO).a(); }
    static String luz(int[] v, String[] p) { return op(v, TECHO).b(); }
    static int periodoLuz(int[] v, String[] p) { return techo(v, p).contains("glass") ? 4 : 3; }

    /** El panel de las ventanas. */
    static String ventana(int[] v, String[] p) { String s = pintado(p, VENTANA); return s != null ? s : op(v, VENTANA).a(); }

    // ------------------------------------------------------------------------------------------------ puntos de estilo (lo decorado cuenta)

    /** Puntos que hacen falta para cada nivel de estilo (nivel 1 = 15 puntos... nivel 5 = 220). */
    static final int[] NIVELES = {15, 40, 80, 140, 220};
    static final String[] NOMBRES = {"Sencillo", "Acogedor", "Bonito", "Elegante", "De revista", "De ensueño"};

    private static final java.util.regex.Pattern NOMBRE_BLOQUE = java.util.regex.Pattern.compile("Name:\\s*\"([^\"]+)\"");

    /**
     * Los puntos de estilo del local: 1 por cada bloque decorativo colocado (hasta 12 del mismo tipo, para que llenar de lo mismo no valga), +3 por cada tipo distinto de bloque
     * y +3 por cada material de paredes o suelos que no sea el de siempre. {@code decor} = la lista guardada del jugador (clave -> estado del bloque como texto).
     */
    static int puntos(java.util.Map<String, String> decor, int[] estilo, String[] pintura) {
        int extra = 0;
        if (pintura != null) for (String s : pintura) if (s != null && !s.isEmpty()) extra += 3;
        return puntos(decor, estilo) + extra;
    }

    static int puntos(java.util.Map<String, String> decor, int[] estilo) {
        java.util.Map<String, Integer> por = new java.util.HashMap<>();
        if (decor != null) for (String v : decor.values()) {
            java.util.regex.Matcher m = NOMBRE_BLOQUE.matcher(v);
            por.merge(m.find() ? m.group(1) : "?", 1, Integer::sum);
        }
        int pts = 0;
        for (int n : por.values()) pts += Math.min(n, 12) + 3;
        for (int i = 0; i < PARTES.length; i++) if (indice(estilo, i) != DEFECTO[i]) pts += 3;
        return pts;
    }

    /** El nivel de estilo (0 a 5) de esos puntos. */
    static int nivelDe(int pts) { int n = 0; for (int u : NIVELES) if (pts >= u) n++; return n; }

    /** Puntos para el siguiente nivel (-1 si ya esta al maximo). */
    static int siguiente(int pts) { int n = nivelDe(pts); return n >= NIVELES.length ? -1 : NIVELES[n]; }

    /** Lo que da el estilo: mas propina, clientes mas seguido y mas visitantes especiales. */
    static double multPropina(int nivel) { return 1.0 + 0.08 * nivel; }
    static double multLlegada(int nivel) { return 1.0 - 0.05 * nivel; }
    static int probEspecialExtra(int nivel) { return 2 * nivel; }
}
