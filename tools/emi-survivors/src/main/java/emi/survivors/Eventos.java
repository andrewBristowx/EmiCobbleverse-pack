package emi.survivors;

import java.util.Random;

/**
 * Los eventos de partida: sucesos cortos y alegres que salen de vez en cuando (la primera vez a los {@link #PRIMERO_SEG} y luego cada {@link #CADA_SEG} segundos, con algo de
 * azar) para romper el ritmo. Son aparte de las mareas, las grietas y el clima. Sin Minecraft: se prueba con un main; lo que hace cada uno esta en {@link EventoEquipo}.
 * <ul>
 *   <li>{@code LLUVIA_ESTRELLAS} (20 s): caen gemas de experiencia alrededor de los jugadores.</li>
 *   <li>{@code HORA_DORADA} (30 s): toda la experiencia vale el doble.</li>
 *   <li>{@code REGALO_EMI} (al instante): vida completa para todos y un objeto bueno en el suelo junto a cada uno.</li>
 *   <li>{@code MICHI_DORADO} (25 s): un Michialien dorado que HUYE; si lo alcanzas, tesoro grande.</li>
 * </ul>
 */
enum Eventos {
    LLUVIA_ESTRELLAS("¡Lluvia de estrellas!", "Recoge las estrellas que caen", 20),
    HORA_DORADA("¡Hora dorada!", "Toda la experiencia vale el doble", 30),
    REGALO_EMI("¡Un regalo de Emi!", "Vida completa y un objeto junto a ti", 0),
    MICHI_DORADO("¡Un Michialien dorado!", "¡Atrápalo antes de que huya!", 25);

    /** El primer evento y cada cuantos segundos sale otro (se suma un poco de azar: ±20 s). */
    static final int PRIMERO_SEG = 75, CADA_SEG = 150, AZAR_SEG = 20;

    final String titulo, subtitulo;
    final int duracionSeg;

    Eventos(String titulo, String subtitulo, int duracionSeg) { this.titulo = titulo; this.subtitulo = subtitulo; this.duracionSeg = duracionSeg; }

    /** Segundos de partida a los que sale el evento numero {@code n} (0 = el primero); {@code rnd} pone el azar, que es el mismo para el mismo numero. */
    static int segundo(int n, long semilla) {
        int azar = new Random(semilla * 31 + n).nextInt(2 * AZAR_SEG + 1) - AZAR_SEG;
        return PRIMERO_SEG + CADA_SEG * n + (n == 0 ? Math.max(0, azar) : azar);
    }

    /** El evento que sale: nunca el mismo dos veces seguidas. */
    static Eventos sortear(Random rnd, Eventos anterior) {
        Eventos[] v = values();
        Eventos e;
        do { e = v[rnd.nextInt(v.length)]; } while (e == anterior);
        return e;
    }
}
