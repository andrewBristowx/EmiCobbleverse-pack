package emi.cafe;

/**
 * Maestria por plato (0.32.0): cada plato cuenta las veces que se ha servido. A las 10 es APRENDIZ, a las 30 EXPERTO y a las 75 MAESTRO: el plato se paga un 3 / 6 / 10 % mas
 * y, al subir de nivel, el dueño gana estrellas (3 / 8 / 20). Sin Minecraft.
 */
final class Maestria {
    private Maestria() {}

    static final String[] NOMBRES = {"Novato", "Aprendiz", "Experto", "Maestro"};
    static final int[] SERVIDOS = {0, 10, 30, 75};
    static final int[] PAGO_PCT = {0, 3, 6, 10};
    static final int[] ESTRELLAS = {0, 3, 8, 20};

    static int nivel(int servidos) {
        int n = 0;
        for (int i = 1; i < SERVIDOS.length; i++) if (servidos >= SERVIDOS[i]) n = i;
        return n;
    }

    /** Los servidos que faltan para el siguiente nivel (-1 = ya es Maestro). */
    static int siguiente(int servidos) {
        int n = nivel(servidos);
        return n >= SERVIDOS.length - 1 ? -1 : SERVIDOS[n + 1];
    }
}
