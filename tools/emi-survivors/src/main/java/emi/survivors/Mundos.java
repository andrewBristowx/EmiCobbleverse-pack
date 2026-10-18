package emi.survivors;

import java.util.List;

/**
 * Los mundos de una partida, en orden: pradera, bosque y cosmos. No se elige: al superar la Sentencia de un mundo el equipo pasa al siguiente, y al superar la del
 * cosmos sale un Michi Gigante; al vencerlo (la primera vez es la victoria) la partida SIGUE en «el Mas alla»: otra vuelta por los tres mundos, cada vez mas dificil. Logica pura (sin Minecraft); los biomas completos estan en {@link Biomas}.
 */
final class Mundos {
    private Mundos() {}

    static final List<String> ORDEN = List.of("pradera", "bosque", "cosmos");

    /** La posicion de ese bioma en el recorrido (0 = el primero), o -1. */
    static int indice(String id) { return ORDEN.indexOf(id); }

    /** ¿Hay un mundo despues del {@code i}? Siempre: tras el cosmos y el Michi Gigante la partida sigue en «el Mas alla» (vuelve a la pradera, mas dificil). */
    static boolean hayOtro(int i) { return true; }

    /** ¿El mundo {@code i} es el ultimo de una vuelta (el cosmos)? Tras su Sentencia sale un Michi Gigante. */
    static boolean ultimoDeVuelta(int i) { return i % ORDEN.size() == ORDEN.size() - 1; }

    /** El bioma del mundo numero {@code i} (0 pradera, 1 bosque, 2 cosmos, 3 pradera otra vez...). */
    static Biomas.Bioma bioma(int i) { return Biomas.porId(ORDEN.get(Math.floorMod(i, ORDEN.size()))); }

    /** El bioma del mundo que viene despues del {@code i}. */
    static Biomas.Bioma siguiente(int i) { return bioma(i + 1); }
}
