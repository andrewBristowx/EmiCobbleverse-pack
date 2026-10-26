package emi.cafe;

import java.util.Map;
import java.util.Random;

/**
 * Clientela habitual (0.31.0): cada especie de cliente va cogiendo cariño al cafe segun las veces que se le ha servido. A las 4 visitas es HABITUAL, a las 12 FIEL y a las 25 VIP.
 * Cuanto mas alto el nivel: viene con mas frecuencia, aguanta mas esperando, paga mas, y al subir de nivel deja un regalo en monedas. Sin Minecraft.
 */
final class Clientela {
    private Clientela() {}

    static final String[] NOMBRES = {"Nuevo", "Habitual", "Fiel", "VIP"};
    static final String[] ICONOS = {"", "♥", "★", "♛"};
    static final String[] COLORES = {"white", "light_purple", "yellow", "gold"};
    /** Visitas (clientes servidos de esa especie) que hacen falta para cada nivel: 0, Habitual, Fiel, VIP. */
    static final int[] VISITAS = {0, 4, 12, 25};
    /** Lo que paga de mas cada nivel (en %), cuanta mas paciencia tiene (%), y el regalo en monedas al llegar a el. */
    static final int[] PAGO_PCT = {0, 6, 12, 22};
    static final int[] PACIENCIA_PCT = {0, 10, 20, 35};
    static final int[] REGALO = {0, 40, 120, 300};
    /** Estrellas extra por servir a un VIP. */
    static final int ESTRELLAS_VIP = 1;

    static int nivel(int visitas) {
        int n = 0;
        for (int i = 1; i < VISITAS.length; i++) if (visitas >= VISITAS[i]) n = i;
        return n;
    }

    static int nivel(Map<String, Integer> clientela, String especie) { return clientela == null ? 0 : nivel(clientela.getOrDefault(especie, 0)); }

    /** Las visitas que faltan para el siguiente nivel (-1 = ya es VIP). */
    static int siguiente(int visitas) {
        int n = nivel(visitas);
        return n >= VISITAS.length - 1 ? -1 : VISITAS[n + 1];
    }

    /** Cuanto mas probable es que venga una especie de ese nivel que una nueva (peso 2, 4, 6, 8). */
    static int peso(int nivel) { return 2 + 2 * nivel; }

    /** Una especie al azar entre las {@code candidatas}, con mas peso las de mas nivel. */
    static String elegir(Random rnd, java.util.List<String> candidatas, Map<String, Integer> clientela) {
        int total = 0;
        for (String s : candidatas) total += peso(nivel(clientela, s));
        int k = rnd.nextInt(Math.max(1, total));
        for (String s : candidatas) { k -= peso(nivel(clientela, s)); if (k < 0) return s; }
        return candidatas.get(candidatas.size() - 1);
    }
}
