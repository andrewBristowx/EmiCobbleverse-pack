package emi.huida;

/**
 * Los niveles de la huida. Cada uno es una {@link Def}: su dimension, la ruta (rectas y arcos), los obstaculos, el perseguidor (especie, tamano,
 * hasta donde llega su «boca») y los textos. {@link Nivel#cargar(Def)} prepara la geometria del nivel elegido; solo hay uno cargado a la vez
 * (una huida a la vez), y se cambia al abrir la cola con {@code /huida abrir [nivel]}.
 */
final class Niveles {
    private Niveles() {}

    /**
     * @param tipo     S = recta de ese largo; L / R = arco de 90 grados a la izquierda / derecha con ese radio (en {@code val})
     * @param desvios  el perseguidor rodea los puentes por tierra (no vuela): solo el Tyrantrum
     * @param entrada  true = reja de hierro que levanta el michi demonio (nivel 1); false = arco abierto
     * @param especie  propiedades de Cobblemon del perseguidor; {@code alto} = altura a la que se escala
     * @param captura  distancia centro del perseguidor - jugador a la que te alcanza; {@code frente} = hasta donde llega por delante de su centro destruyendo obstaculos
     * @param flota    altura de sus pies sobre el suelo (0 = anda; mas = flota)
     */
    record Def(String id, String titulo, String dimension, int baseX, char[] tipo, int[] val, Nivel.Mod[] mods, boolean desvios, boolean entrada,
               String especie, float alto, double captura, double frente, double flota,
               String el, String El, String come, int introTicks, int finalTicks, int comidoTicks) {
        boolean calabaza() { return id.equals("calabaza"); }
    }

    // ---------------------------------------------------------------- nivel 1: la huida del Tyrantrum (selva)
    static final Def TYRANTRUM = new Def("tyrantrum", "la huida del Tyrantrum", "selva", 16384,
            new char[]{'S', 'L', 'S', 'R', 'L', 'S', 'L', 'S', 'L', 'R', 'S', 'R', 'S', 'R', 'S', 'L', 'S', 'L', 'S', 'R', 'S'},
            new int[]{114, 16, 44, 12, 12, 70, 16, 40, 12, 12, 64, 16, 50, 12, 36, 12, 60, 16, 84, 12, 72},
            new Nivel.Mod[]{
                    new Nivel.Mod(0, 72, 'T'), new Nivel.Mod(0, 92, 'B'),
                    new Nivel.Mod(2, 8, 'L'), new Nivel.Mod(2, 15, 'R'), new Nivel.Mod(2, 22, 'L'), new Nivel.Mod(2, 29, 'R'), new Nivel.Mod(2, 35, 'H'),
                    new Nivel.Mod(5, 16, 'P'), new Nivel.Mod(5, 46, 'T'), new Nivel.Mod(5, 56, 'C'),
                    new Nivel.Mod(7, 8, 'H'), new Nivel.Mod(7, 15, 'T'), new Nivel.Mod(7, 24, 'B'),
                    new Nivel.Mod(10, 8, 'L'), new Nivel.Mod(10, 14, 'R'), new Nivel.Mod(10, 20, 'L'), new Nivel.Mod(10, 26, 'R'), new Nivel.Mod(10, 34, 'T'), new Nivel.Mod(10, 40, 'T'), new Nivel.Mod(10, 50, 'B'),
                    new Nivel.Mod(12, 8, 'H'), new Nivel.Mod(12, 15, 'T'), new Nivel.Mod(12, 24, 'C'), new Nivel.Mod(12, 32, 'B'), new Nivel.Mod(12, 40, 'T'),
                    new Nivel.Mod(14, 8, 'T'), new Nivel.Mod(14, 13, 'T'), new Nivel.Mod(14, 18, 'T'), new Nivel.Mod(14, 26, 'B'),
                    new Nivel.Mod(16, 14, 'P'), new Nivel.Mod(16, 44, 'R'), new Nivel.Mod(16, 52, 'L'),
                    new Nivel.Mod(18, 8, 'B'), new Nivel.Mod(18, 16, 'C'), new Nivel.Mod(18, 24, 'T'), new Nivel.Mod(18, 32, 'H'), new Nivel.Mod(18, 42, 'L'), new Nivel.Mod(18, 48, 'R'),
                    new Nivel.Mod(18, 56, 'T'), new Nivel.Mod(18, 62, 'B'), new Nivel.Mod(18, 70, 'T'),
                    new Nivel.Mod(20, 10, 'T'), new Nivel.Mod(20, 18, 'H'), new Nivel.Mod(20, 28, 'B'), new Nivel.Mod(20, 36, 'R'), new Nivel.Mod(20, 44, 'T'), new Nivel.Mod(20, 54, 'C')},
            true, true, "tyrantrum level=60", 9.0f, 6.0, 6.5, 0.0,
            "el Tyrantrum", "El Tyrantrum", "come", 200, 266, 175);

    // ---------------------------------------------------------------- nivel 2: la huida de la calabaza (Halloween, Pumpkaboo)
    /** ~640 bloques de camino (~590 de carrera, ~110 s): lo que dura la musica desde que sube hasta que acaba. */
    static final Def CALABAZA = new Def("calabaza", "la huida de la calabaza", "calabazar", 8192,
            new char[]{'S', 'L', 'S', 'R', 'S', 'R', 'S', 'L', 'L', 'S', 'R', 'S', 'L', 'S', 'R', 'S'},
            new int[]{96, 12, 44, 12, 64, 16, 36, 12, 12, 48, 12, 68, 16, 48, 12, 72},
            new Nivel.Mod[]{
                    new Nivel.Mod(0, 70, 'T'), new Nivel.Mod(0, 80, 'B'),
                    new Nivel.Mod(2, 8, 'L'), new Nivel.Mod(2, 15, 'R'), new Nivel.Mod(2, 24, 'H'), new Nivel.Mod(2, 33, 'T'),
                    new Nivel.Mod(4, 8, 'P'), new Nivel.Mod(4, 30, 'T'), new Nivel.Mod(4, 38, 'C'), new Nivel.Mod(4, 48, 'B'),
                    new Nivel.Mod(6, 8, 'T'), new Nivel.Mod(6, 16, 'B'), new Nivel.Mod(6, 24, 'R'),
                    new Nivel.Mod(9, 8, 'H'), new Nivel.Mod(9, 16, 'T'), new Nivel.Mod(9, 22, 'L'), new Nivel.Mod(9, 29, 'R'), new Nivel.Mod(9, 37, 'T'),
                    new Nivel.Mod(11, 8, 'L'), new Nivel.Mod(11, 14, 'R'), new Nivel.Mod(11, 20, 'L'), new Nivel.Mod(11, 26, 'R'), new Nivel.Mod(11, 34, 'T'), new Nivel.Mod(11, 40, 'T'), new Nivel.Mod(11, 48, 'B'), new Nivel.Mod(11, 56, 'W'),
                    new Nivel.Mod(13, 8, 'T'), new Nivel.Mod(13, 16, 'H'), new Nivel.Mod(13, 25, 'C'), new Nivel.Mod(13, 33, 'B'), new Nivel.Mod(13, 40, 'T'),
                    new Nivel.Mod(15, 8, 'B'), new Nivel.Mod(15, 15, 'T'), new Nivel.Mod(15, 22, 'P'), new Nivel.Mod(15, 43, 'L'), new Nivel.Mod(15, 49, 'R'), new Nivel.Mod(15, 55, 'T')},
            false, false, "pumpkaboo level=60", 5.4f, 3.6, 3.4, 0.9,
            "el Pumpkaboo", "El Pumpkaboo", "convierte en calabaza", 184, 250, 130);

    static final Def[] TODOS = {TYRANTRUM, CALABAZA};

    static Def porNombre(String n) {
        if (n == null) return TYRANTRUM;
        String s = n.toLowerCase(java.util.Locale.ROOT);
        for (Def d : TODOS) if (d.id().equals(s)) return d;
        if (s.equals("pumpkaboo") || s.equals("halloween") || s.equals("2")) return CALABAZA;
        if (s.equals("1") || s.equals("selva")) return TYRANTRUM;
        return null;
    }
}
