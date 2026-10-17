package emi.survivors;

/**
 * La Sentencia: la prueba final de una partida. A los {@link #INICIO_SEG} segundos (20:00) empieza una cuenta atras de {@link #DURACION_SEG} segundos en la que llueven
 * explosiones avisadas bajo los jugadores, cada vez mas seguidas, y salen mas enemigos; ya no salen jefes, mareas ni grietas nuevas. Si el equipo sigue en pie cuando
 * acaba, SUPERA la Sentencia: la partida termina con victoria (premio de michiestrellitas, marca especial en el top y logro). Sin Minecraft: se prueba con un main.
 */
final class Sentencia {
    private Sentencia() {}

    static final int INICIO_SEG = Integer.getInteger("emi.survivors.sentencia_inicio", 20 * 60), DURACION_SEG = Integer.getInteger("emi.survivors.sentencia_duracion", 90);      // las propiedades solo se usan en pruebas
    /** Michiestrellitas extra que gana cada jugador que supera la Sentencia (antes del bonus de suerte). */
    static final int MONEDAS = 100;
    /** Multiplicador del ritmo de aparicion de enemigos durante la Sentencia. */
    static final double RITMO = 1.5;

    /** ¿Esta a punto de empezar (o ya ha empezado)? {@code t} = segundos de partida. */
    static boolean toca(double t) { return t >= INICIO_SEG; }

    /** Ticks entre una tanda de explosiones y la siguiente a los {@code segundosDentro} segundos de Sentencia: de 2 s al principio a 0,7 s al final. */
    static int ticksEntreTandas(double segundosDentro) {
        double f = Math.max(0, Math.min(1, segundosDentro / DURACION_SEG));
        return (int) Math.round(40 - 26 * f);
    }

    /** Cuantas explosiones extra (ademas de la de debajo de cada jugador) caen en cada tanda: de 1 a 3. */
    static int extrasPorTanda(double segundosDentro) { return 1 + (int) Math.min(2, segundosDentro / (DURACION_SEG / 3.0)); }
}
