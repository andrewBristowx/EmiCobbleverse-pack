package emi.cafe;

/** La reputacion del cafe: sube sirviendo bien y baja cuando un cliente se va enfadado. Cada nivel paga un poco mas y desbloquea ayudantes y al crítico gastronómico. Sin Minecraft. */
final class Reputacion {
    private Reputacion() {}

    /** Puntos que hacen falta para cada nivel (el nivel 1 es el de partida). */
    static final int[] UMBRALES = {0, 25, 75, 150, 300, 500, 800, 1200};
    static final String[] NOMBRES = {"Local nuevo", "Conocido", "Popular", "Recomendado", "Famoso", "Estrella", "Leyenda", "Mito"};

    static int max() { return UMBRALES.length; }

    static int nivel(int rep) { int n = 1; for (int i = 1; i < UMBRALES.length; i++) if (rep >= UMBRALES[i]) n = i + 1; return n; }

    static String nombre(int nivel) { return NOMBRES[Math.max(1, Math.min(max(), nivel)) - 1]; }

    /** Puntos para el siguiente nivel (-1 si ya esta al maximo). */
    static int siguiente(int rep) { int n = nivel(rep); return n >= max() ? -1 : UMBRALES[n]; }

    /** Cuanto mas se paga por todo (+3 % por nivel). */
    static double multPago(int nivel) { return 1.0 + 0.03 * (nivel - 1); }

    /** Nivel que hace falta para el crítico gastronómico. */
    static final int NIVEL_CRITICO = 3;
}
