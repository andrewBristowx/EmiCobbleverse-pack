package emi.survivors;

/**
 * Los numeros del juego que no dependen de Minecraft: curva de experiencia, monedas al acabar y ritmo de las oleadas segun el tiempo. Todo son formulas
 * sencillas y faciles de retocar; se pueden probar con un main sin abrir el juego (tools/emi-survivors/test/PruebaLogica.java).
 * El tiempo {@code t} es siempre en segundos desde el principio de la partida.
 */
final class Curvas {
    private Curvas() {}

    // ------------------------------------------------------------ experiencia

    /** Experiencia que hace falta para pasar del nivel {@code nivel} al siguiente (10 para el primero, ~33 para el quinto, ~72 para el decimo). */
    static int xpPara(int nivel) { return (int) Math.round(5 + 4.5 * nivel + 0.22 * nivel * nivel); }

    /** Experiencia que suelta un enemigo: 1 el debil, 2 el fuerte, +1 si es un Michialien; los elites y el jefe se fijan aparte. */
    static int xpEnemigo(boolean fuerte, boolean michi) { return (fuerte ? 2 : 1) + (michi ? 1 : 0); }

    // ------------------------------------------------------------ monedas

    /** Michiestrellitas (la moneda de las mejoras permanentes) que se ganan al acabar una partida. {@code mult} = 1 + bonos de «suerte». */
    static int monedas(int segundos, int derrotados, int nivel, boolean jefe, double mult) {
        double base = segundos / 12.0 + derrotados / 15.0 + nivel * 2.0 + (jefe ? 20 : 0);
        return (int) Math.max(0, Math.round(base * mult));
    }

    // ------------------------------------------------------------ oleadas

    /** Cuantos enemigos puede haber a la vez (tope «blando»: sube con el tiempo hasta 120). */
    static int tope(double t) { return (int) Math.min(120, 22 + t * 0.42); }

    /** Enemigos por segundo que se intentan crear (siempre sin pasar del tope). */
    static double ritmo(double t) { return 1.6 + t / 45.0; }

    /** Multiplicador de vida de los enemigos nuevos. */
    static double vidaMul(double t) { return 1 + t / 140.0; }

    /** Multiplicador del dano por contacto de los enemigos nuevos. */
    static double danoMul(double t) { return 1 + t / 420.0; }

    /** Multiplicador de la velocidad de los enemigos nuevos (sube un poco, hasta +35%). */
    static double velMul(double t) { return 1 + Math.min(0.35, t / 900.0); }

    /** Probabilidad de que un enemigo normal salga del tramo fuerte de estadisticas (el de mas vida y dano). */
    static double pFuerte(double t) { return Math.min(0.75, 0.05 + t / 400.0); }

    /** Probabilidad de que un enemigo sea un Michialien (el toque de Emi). */
    static final double P_MICHI = 0.10;

    /** Segundos entre elites (uno cada minuto, el primero al minuto) y segundo en que sale el primer jefe. */
    static final int CADA_ELITE = 60, T_JEFE = 180;

    // ------------------------------------------------------------ tipos de enemigo especial

    /** Vida base del jefe «Snorlax Emi» segun el minuto en que sale (el primero, 3:00). */
    static float vidaJefe(double t) { return (float) (1200 + 4 * (t - T_JEFE)); }
}
