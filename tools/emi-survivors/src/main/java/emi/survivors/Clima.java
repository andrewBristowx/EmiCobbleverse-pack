package emi.survivors;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * El clima de una partida: cambia cada {@link #CADA_SEG} segundos (el primero a los {@link #PRIMERO_SEG}) y favorece a unos tipos de Pokemon (+25 % de dano) y perjudica
 * a otros (-15 %). La nieve ralentiza a los enemigos un 10 % y la tormenta suma rayos que se avisan en el suelo (ver {@link ClimaEquipo}). Sin Minecraft: se prueba con un main.
 * Cuenta el tipo PRINCIPAL del Pokemon del jugador, no el de cada ataque.
 */
enum Clima {
    DESPEJADO("Despejado", "", Set.of(), Set.of(), 1.0),
    SOL("Sol", "¡Hace sol!", Set.of("fire", "grass", "ground"), Set.of("water", "ice"), 1.0),
    LLUVIA("Lluvia", "¡Llueve!", Set.of("water", "electric", "bug"), Set.of("fire"), 1.0),
    TORMENTA("Tormenta", "¡Tormenta!", Set.of("electric", "flying", "water"), Set.of("fire"), 1.0),
    NIEVE("Nieve", "¡Nieva!", Set.of("ice", "steel"), Set.of("fire", "grass"), 0.9),
    NIEBLA("Niebla", "¡Niebla!", Set.of("ghost", "dark", "psychic", "fairy"), Set.of(), 1.0);

    static final double BONUS = 1.25, MALUS = 0.85;
    /** El primer cambio de clima y cada cuantos segundos hay otro. */
    static final int PRIMERO_SEG = 60, CADA_SEG = 150;

    final String nombre, aviso;
    final Set<String> favorables, perjudicados;
    /** Multiplicador de la velocidad de los enemigos. */
    final double velEnemigos;

    Clima(String nombre, String aviso, Set<String> favorables, Set<String> perjudicados, double velEnemigos) {
        this.nombre = nombre; this.aviso = aviso; this.favorables = favorables; this.perjudicados = perjudicados; this.velEnemigos = velEnemigos;
    }

    /** Multiplicador del dano de un Pokemon de ese tipo (el nombre en ingles de Cobblemon: «fire», «water»...). */
    double multiplicador(String tipo) { return favorables.contains(tipo) ? BONUS : perjudicados.contains(tipo) ? MALUS : 1.0; }

    /** Lo que sale bajo el titulo (corto, para que quepa en pantalla): los tipos favorecidos. */
    String subtitulo() {
        if (favorables.isEmpty()) return "";
        return "Favorece: " + lista(favorables);
    }

    /** Una linea para el aviso: quien sube y quien baja, con los nombres en español. */
    String resumen() {
        if (favorables.isEmpty() && perjudicados.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        if (!favorables.isEmpty()) sb.append(lista(favorables)).append(": +25 % de daño");
        if (!perjudicados.isEmpty()) sb.append(sb.length() > 0 ? " · " : "").append(lista(perjudicados)).append(": −15 %");
        if (velEnemigos < 1.0) sb.append(" · enemigos más lentos");
        return sb.toString();
    }

    private static String lista(Set<String> tipos) {
        StringBuilder sb = new StringBuilder();
        for (String t : ORDEN) if (tipos.contains(t)) sb.append(sb.length() > 0 ? ", " : "").append(NOMBRE_TIPO[ORDEN.indexOf(t)]);
        return sb.toString();
    }

    private static final List<String> ORDEN = List.of("normal", "fire", "water", "grass", "electric", "ice", "fighting", "poison", "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy");
    private static final String[] NOMBRE_TIPO = {"Normal", "Fuego", "Agua", "Planta", "Eléctrico", "Hielo", "Lucha", "Veneno", "Tierra", "Volador", "Psíquico", "Bicho", "Roca", "Fantasma", "Dragón", "Siniestro", "Acero", "Hada"};

    /** Cuantos cambios de clima han tocado ya a los {@code segundos} de partida (0 antes del primero). */
    static int cambiosHasta(double segundos) { return segundos < PRIMERO_SEG ? 0 : 1 + (int) ((segundos - PRIMERO_SEG) / CADA_SEG); }

    /** El siguiente clima: uno distinto del actual (y despejado de vez en cuando). */
    static Clima sortear(Random rnd, Clima actual) {
        Clima[] v = values();
        Clima c;
        do { c = v[rnd.nextInt(v.length)]; } while (c == actual);
        return c;
    }
}
