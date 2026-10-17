package emi.survivors;

/**
 * Los ataques propios de algunos enemigos (datos puros, sin Minecraft). Hasta ahora todos solo hacian dano al tocar; ahora una parte de los enemigos normales y todos
 * los campeones salen con un ataque segun el tipo de su Pokemon. Todos se AVISAN antes de golpear (particulas o un cuadrado en el suelo) para poder esquivarlos:
 * <ul>
 *   <li>TIRADOR (a distancia) — se queda a unos 9 bloques, se carga un momento y lanza una bola recta a donde estabas.</li>
 *   <li>ABANICO (a distancia) — igual, pero lanza tres bolas a la vez abiertas en abanico (mas lentas).</li>
 *   <li>MORTERO (a distancia) — marca un cuadrado rojo en el suelo donde estas tu y, un momento despues, explota.</li>
 *   <li>PISOTON (cuerpo a cuerpo) — al acercarse a ti se detiene, marca un circulo de particulas rojas y golpea el suelo: dana a todo lo que este dentro.</li>
 *   <li>ZARPAZO (cuerpo a cuerpo) — da un salto corto hacia ti con las garras por delante (avisa agachandose un instante) y dana a lo que alcanza al caer.</li>
 * </ul>
 * Lo que hace cada uno esta en {@link Enemigo#atacar}. Los numeros son primeras estimaciones.
 */
enum Ataque {
    //          nombre       a distancia
    TIRADOR("Tirador", true),
    ABANICO("Abanico", true),
    MORTERO("Mortero", true),
    PISOTON("Pisotón", false),
    ZARPAZO("Zarpazo", false);

    final String nombre;
    final boolean distancia;

    Ataque(String nombre, boolean distancia) { this.nombre = nombre; this.distancia = distancia; }

    /** A que distancia (bloques) se queda el enemigo a distancia; el cuerpo a cuerpo sigue acercandose. */
    static final double DISTANCIA = 9.0;

    /** Probabilidad de que un enemigo normal nuevo traiga ataque: 0 los primeros 20 s y luego sube hasta un 40 % (a los 5:00). */
    static double probabilidad(double t) { return Math.min(0.40, Math.max(0, (t - 20) / 700.0)); }

    /** El ataque que le toca a un Pokemon segun su tipo principal (o el secundario si el principal no tiene). */
    static Ataque paraTipo(String tipo1, String tipo2) {
        Ataque a = deTipo(tipo1);
        return a != null ? a : deTipo(tipo2) != null ? deTipo(tipo2) : TIRADOR;
    }

    private static Ataque deTipo(String t) {
        if (t == null) return null;
        return switch (t) {
            case "water", "electric", "ice", "psychic", "ghost", "fairy", "poison" -> TIRADOR;
            case "grass", "bug", "dragon" -> ABANICO;
            case "fire", "rock", "steel" -> MORTERO;
            case "fighting", "ground", "normal" -> PISOTON;
            case "dark", "flying" -> ZARPAZO;
            default -> null;
        };
    }

    /** Lo que se le dice al jugador la primera vez que sale uno en la partida. */
    static final String LEYENDA = "Algunos Pokémon atacan: unos de lejos (bolas, abanicos, cuadrados rojos en el suelo) y otros de cerca (pisotones con círculo rojo, saltos con garra). ¡Todo se avisa antes de golpear!";
}
