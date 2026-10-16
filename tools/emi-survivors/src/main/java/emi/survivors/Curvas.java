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
    static int monedas(int segundos, int derrotados, int nivel, int jefes, double mult) { return monedas(segundos, derrotados, nivel, jefes, 0, mult); }

    /** Igual, con {@code shinies} = los shinies que derroto ese jugador ({@link #MONEDAS_POR_SHINY} cada uno). */
    static int monedas(int segundos, int derrotados, int nivel, int jefes, int shinies, double mult) {
        double base = segundos / 12.0 + derrotados / 15.0 + nivel * 2.0 + Jefes.MONEDAS_POR_JEFE * Math.max(0, jefes) + MONEDAS_POR_SHINY * Math.max(0, shinies);
        return (int) Math.max(0, Math.round(base * mult));
    }

    // ------------------------------------------------------------ oleadas

    /** Cuantos enemigos puede haber a la vez (tope «blando»: sube con el tiempo hasta 120). */
    static int tope(double t) { return (int) Math.min(120, 22 + t * 0.42); }

    /** Enemigos por segundo que se intentan crear (siempre sin pasar del tope). */
    static double ritmo(double t) { return 1.6 + t / 45.0; }

    /** Multiplicador de vida de los enemigos nuevos. */
    static double vidaMul(double t) { return 1 + t / 140.0; }

    // ------------------------------------------------------------ varios jugadores en la misma arena

    /** Tope duro de enemigos de un solo equipo, sean los jugadores que sean (el servidor y el cliente aguantan lo que aguantan). */
    static final int TOPE_EQUIPO = 200;

    /** Probabilidad de que un enemigo normal derrotado suelte un objeto (los campeones y el jefe siempre sueltan). */
    static final double P_OBJETO = 0.006;

    /**
     * Probabilidad de que un enemigo normal sea un shiny (variocolor): 1 de cada 250. Da mucha mas experiencia, un objeto seguro y {@link #MONEDAS_POR_SHINY} michiestrellitas
     * al acabar. {@code -Demi.survivors.shiny=<0-1>} la cambia (para pruebas).
     */
    static final double P_SHINY = leerShiny();
    static final int MONEDAS_POR_SHINY = 3;

    private static double leerShiny() {
        try { return Math.max(0.0, Math.min(1.0, Double.parseDouble(System.getProperty("emi.survivors.shiny", "0.004")))); }
        catch (NumberFormatException e) { return 0.004; }
    }

    /** Cuantos enemigos de mas (en proporcion) hay por cada jugador extra: con 2 jugadores, 1,6 veces; con 3, 2,2; con 4, 2,8 (sin pasar de {@link #TOPE_EQUIPO}). */
    static double escalaCantidad(int jugadores) { return 1 + 0.6 * (Math.max(1, Math.min(4, jugadores)) - 1); }

    /** Tope de enemigos a la vez para un equipo de {@code jugadores}. */
    static int tope(double t, int jugadores) { return (int) Math.min(TOPE_EQUIPO, tope(t) * escalaCantidad(jugadores)); }

    /** Enemigos por segundo que se intentan crear para un equipo de {@code jugadores}. */
    static double ritmo(double t, int jugadores) { return ritmo(t) * escalaCantidad(jugadores); }

    /** Multiplicador de la vida del jefe segun los jugadores (el dano total sube casi en proporcion: 1, 1,8, 2,6 y 3,4). */
    static double escalaJefe(int jugadores) { return 1 + 0.8 * (Math.max(1, Math.min(4, jugadores)) - 1); }

    /** Multiplicador extra de vida de los enemigos nuevos por cada jugador de mas (mas jugadores = mas dano total, asi que aguantan algo mas). */
    static double escalaVida(int jugadores) { return 1 + 0.15 * (Math.max(1, Math.min(4, jugadores)) - 1); }

    /** Multiplicador del dano por contacto de los enemigos nuevos. */
    static double danoMul(double t) { return 1 + t / 420.0; }

    /** Multiplicador de la velocidad de los enemigos nuevos (sube un poco, hasta +35%). */
    static double velMul(double t) { return 1 + Math.min(0.35, t / 900.0); }

    /** Probabilidad de que un enemigo normal salga del tramo fuerte de estadisticas (el de mas vida y dano). */
    static double pFuerte(double t) { return Math.min(0.75, 0.05 + t / 400.0); }

    /** Probabilidad de que un enemigo sea un Michialien (el toque de Emi). */
    static final double P_MICHI = 0.10;

    /** Segundos entre elites (uno cada minuto, el primero al minuto). Los jefes salen cada 3 minutos: ver {@link Jefes}. */
    static final int CADA_ELITE = 60;

}
