package emi.survivors;

import java.util.List;

/**
 * Los mundos de una partida, en orden: pradera, bosque y cosmos. No se elige: al superar la Sentencia de un mundo el equipo pasa al siguiente, y al superar la del
 * ultimo sale el Michi Gigante (el jefe final). Logica pura (sin Minecraft); los biomas completos estan en {@link Biomas}.
 */
final class Mundos {
    private Mundos() {}

    static final List<String> ORDEN = List.of("pradera", "bosque", "cosmos");

    /** La posicion de ese bioma en el recorrido (0 = el primero), o -1. */
    static int indice(String id) { return ORDEN.indexOf(id); }

    /** ¿Hay un mundo despues del {@code i}? */
    static boolean hayOtro(int i) { return i + 1 < ORDEN.size(); }

    /** El bioma del mundo que viene despues del {@code i}. */
    static Biomas.Bioma siguiente(int i) { return Biomas.porId(ORDEN.get(i + 1)); }
}
