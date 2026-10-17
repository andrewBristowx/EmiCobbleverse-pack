package emi.survivors;

/**
 * Los tesoros del mapa (numeros y reglas, sin Minecraft: se prueba con un main; lo que pasa en la partida esta en {@link TesorosEquipo}).
 * <ul>
 *   <li><b>Tesoros pequeños</b> (barriles repartidos por la arena): se ROMPEN con los ataques como si fueran enemigos sin vida propia; sueltan experiencia y a veces un objeto.
 *       Siempre hay unos cuantos y, cuando se rompe uno, sale otro mas tarde.</li>
 *   <li><b>Cofres grandes</b>: de vez en cuando aparece uno; hay que QUEDARSE dentro de su cuadrado dorado un rato (una barra de progreso lo cuenta) y al abrirse sube un nivel
 *       (con la pantalla de elegir carta) a quien estuviera dentro y suelta un objeto bueno.</li>
 * </ul>
 */
final class Tesoros {
    private Tesoros() {}

    /** Cuantos tesoros pequeños hay a la vez en la arena (se reponen de uno en uno cada {@link #REPONER_TICKS}). */
    static final int PEQUENOS = 16, REPONER_TICKS = 20 * 12;
    /** Gemas de experiencia que suelta un tesoro pequeño (cantidad y valor de cada una) y probabilidad de que suelte tambien un objeto. */
    static final int GEMAS = 3, XP_GEMA = 3;
    static final double P_OBJETO = 0.30;

    /** El primer cofre grande y cada cuantos segundos sale otro; cuanto dura abierto (esperando) y los «ticks de jugador» dentro que hacen falta. */
    static final int PRIMER_COFRE_SEG = 45, CADA_COFRE_SEG = 100, DURACION_COFRE_SEG = 70, CARGA_COFRE = 120;
    /** Lado del cuadrado del cofre grande. */
    static final double LADO_COFRE = 6.0;

    /** Vida de un tesoro pequeño a los {@code t} segundos: sube un poco con el tiempo para que cueste lo mismo romperlo. */
    static double vida(double t) { return 30 + t / 8.0; }

    /** El segundo en que sale el cofre grande numero {@code n} (0 = el primero). */
    static int segundoCofre(int n) { return PRIMER_COFRE_SEG + CADA_COFRE_SEG * n; }

    /** Cuantos cofres grandes le tocan ya a la partida a los {@code t} segundos. */
    static int cofresQueTocan(double t) { return t < PRIMER_COFRE_SEG ? 0 : 1 + (int) ((t - PRIMER_COFRE_SEG) / CADA_COFRE_SEG); }
}
