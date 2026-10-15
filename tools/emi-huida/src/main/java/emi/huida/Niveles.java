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
     * @param anchoBase, anchoVar  el camino mide 2 * (anchoBase + anchoVar * (algo entre 0 y 1)) de ancho (nivel 1: 7 a 9,4; nivel 2: mas estrecho)
     * @param gapMin   distancia minima (en bloques) entre dos obstaculos seguidos de la misma recta
     * @param vel      velocidad base del perseguidor (bloques/s; el jugador corre a ~5,6)
     */
    record Def(String id, String titulo, String dimension, int baseX, char[] tipo, int[] val, Nivel.Mod[] mods, boolean desvios, boolean entrada,
               String especie, float alto, double captura, double frente, double flota,
               double anchoBase, double anchoVar, int gapMin, double vel,
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
            3.5, 1.2, 4, 5.3,
            "el Tyrantrum", "El Tyrantrum", "come", 200, 266, 175);

    // ---------------------------------------------------------------- nivel 2: la huida de la calabaza (Halloween, Pumpkaboo)
    /**
     * Un recorrido distinto al del Tyrantrum (el dueño pidio otro trazado, obstaculos distintos y mas dificiles): una serpentina de curvas cerradas (radio 8),
     * dos horquillas de 180 grados y rectas cortas; camino mas estrecho (6,2 a 7,6), un Pumpkaboo algo mas rapido y tres tipos nuevos de obstaculo:
     * {@code Z} islas de piedra sobre lava (hay que saltar de isla en isla, cinco veces seguidas), {@code K} calabazas rodantes que vienen hacia ti (en tres carriles: cambia de carril o salta en el momento justo)
     * y {@code F} aspas giratorias alrededor de un pilar (hay que saltarlas). ~670 bloques (~620 de carrera, ~115 s: lo que dura la musica desde que sube).
     */
    static final Def CALABAZA = new Def("calabaza", "la huida de la calabaza", "calabazar", 16384,
            new char[]{'S', 'R', 'S', 'L', 'S', 'R', 'S', 'L', 'S', 'L', 'S', 'R', 'S', 'L', 'S', 'R', 'S', 'R', 'S', 'L', 'S'},
            new int[]{100, 8, 24, 8, 26, 8, 44, 14, 56, 14, 40, 8, 22, 8, 22, 8, 64, 12, 36, 12, 80},
            new Nivel.Mod[]{
                    new Nivel.Mod(0, 70, 'T'), new Nivel.Mod(0, 76, 'B'), new Nivel.Mod(0, 83, 'T'), new Nivel.Mod(0, 88, 'T'),
                    new Nivel.Mod(2, 7, 'C'), new Nivel.Mod(2, 13, 'T'),
                    new Nivel.Mod(4, 6, 'L'), new Nivel.Mod(4, 12, 'R'), new Nivel.Mod(4, 18, 'T'),
                    new Nivel.Mod(6, 6, 'Z'), new Nivel.Mod(6, 34, 'T'),
                    new Nivel.Mod(8, 8, 'F'), new Nivel.Mod(8, 22, 'B'), new Nivel.Mod(8, 29, 'W'), new Nivel.Mod(8, 37, 'T'), new Nivel.Mod(8, 42, 'T'),
                    new Nivel.Mod(10, 6, 'K'), new Nivel.Mod(10, 27, 'H'), new Nivel.Mod(10, 33, 'T'),
                    new Nivel.Mod(12, 6, 'W'), new Nivel.Mod(12, 13, 'B'),
                    new Nivel.Mod(14, 6, 'C'), new Nivel.Mod(14, 12, 'L'),
                    new Nivel.Mod(16, 6, 'P'), new Nivel.Mod(16, 24, 'T'), new Nivel.Mod(16, 29, 'R'), new Nivel.Mod(16, 35, 'L'), new Nivel.Mod(16, 41, 'R'), new Nivel.Mod(16, 47, 'T'), new Nivel.Mod(16, 52, 'B'),
                    new Nivel.Mod(18, 6, 'Z'),
                    new Nivel.Mod(20, 8, 'F'), new Nivel.Mod(20, 20, 'T'), new Nivel.Mod(20, 25, 'B'), new Nivel.Mod(20, 31, 'K'), new Nivel.Mod(20, 51, 'H'), new Nivel.Mod(20, 57, 'W'), new Nivel.Mod(20, 64, 'T')},
            false, false, "pumpkaboo level=60", 5.4f, 3.6, 3.4, 0.9,
            3.1, 0.7, 4, 5.45,
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
