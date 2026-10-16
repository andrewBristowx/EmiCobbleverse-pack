package emi.survivors;

import java.util.List;

/**
 * Los biomas del minijuego (datos puros, sin Minecraft): cada uno es una arena distinta, con su propia dimension (cielo, hora y niebla), su decoracion, su musica y
 * unos tipos de Pokemon que salen mas de enemigos. Se elige al empezar (en solitario, el jugador; en grupo, el lider). Lo que se construye en cada uno esta en {@link Arena}.
 * <ul>
 *   <li>{@code pradera} — «Pradera Michialien»: de dia, hierba, flores, rocas y la luna de Emi (la arena de siempre).</li>
 *   <li>{@code bosque} — «Bosque Embrujado»: de noche, suelo de pizarra con musgo, lapidas, calabazas encendidas y circulos de runas; salen mas fantasmas, siniestros y venenosos.</li>
 *   <li>{@code cosmos} — «Cosmos Michialien»: en el espacio, un damero magenta y purpura con orbitas y planetas pintados en el suelo; salen mas psiquicos, hadas y dragones.</li>
 * </ul>
 */
public final class Biomas {
    private Biomas() {}

    /**
     * @param id        nombre corto (el que se escribe en los comandos y viaja al servidor)
     * @param nombre    lo que se ve en la interfaz
     * @param dimension el nombre de la dimension de Minecraft de sus arenas (en el espacio de nombres {@code emi_survivors})
     * @param texto     una frase corta para el menu
     * @param tipos     los tipos de Pokemon que salen mas (nombres de tipo de Cobblemon)
     * @param sesgo     probabilidad (0..1) de que un enemigo normal sea de uno de esos tipos
     * @param musica    el nombre de la cancion de fondo (ver {@code Sonidos})
     * @param color     color del tema (ARGB) para el menu
     */
    public record Bioma(String id, String nombre, String dimension, String texto, List<String> tipos, double sesgo, String musica, int color) {}

    public static final Bioma PRADERA = new Bioma("pradera", "Pradera Michialien", "arena", "De día · hierba, flores y la luna de Emi", List.of("grass", "bug", "normal", "fairy"), 0.30, "pradera", 0xFF6FE08A);
    public static final Bioma BOSQUE = new Bioma("bosque", "Bosque Embrujado", "arena_bosque", "De noche · lápidas, calabazas y runas", List.of("ghost", "dark", "poison", "grass"), 0.40, "bosque", 0xFFFF9A3C);
    public static final Bioma COSMOS = new Bioma("cosmos", "Cosmos Michialien", "arena_cosmos", "En el espacio · órbitas y planetas", List.of("psychic", "fairy", "dragon", "steel"), 0.40, "cosmos", 0xFFC9B3FF);

    public static final List<Bioma> TODOS = List.of(PRADERA, BOSQUE, COSMOS);

    /** El bioma con ese id (el que se escribe en los comandos); si no existe, null. */
    public static Bioma porId(String id) {
        if (id == null) return null;
        for (Bioma b : TODOS) if (b.id().equalsIgnoreCase(id.trim())) return b;
        return null;
    }

    /** El bioma con ese id o, si no existe (un cliente viejo, un error), la pradera. */
    public static Bioma deIdOPradera(String id) {
        Bioma b = porId(id);
        return b == null ? PRADERA : b;
    }

    /** El bioma cuya dimension se llama asi ({@code arena}, {@code arena_bosque}...), o null si no es ninguna arena. */
    public static Bioma porDimension(String nombreDimension) {
        for (Bioma b : TODOS) if (b.dimension().equals(nombreDimension)) return b;
        return null;
    }

    /** ¿Es el nombre de la dimension de alguna arena (no la sala)? */
    public static boolean esArena(String nombreDimension) { return porDimension(nombreDimension) != null; }
}
