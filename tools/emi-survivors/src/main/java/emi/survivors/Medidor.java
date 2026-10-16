package emi.survivors;

import net.minecraft.server.MinecraftServer;

/**
 * Medidas de rendimiento de una partida: cuanto tarda este mod en cada tick (media de los ultimos 20 y maximo) y cuanto tarda el servidor entero (media y
 * maximo de los ultimos 100 ticks, los que guarda Minecraft). El limite de un servidor sano son 50 ms por tick (20 ticks por segundo).
 */
final class Medidor {
    private final long[] ventana = new long[20];
    private int n;
    private float nuestro, nuestroMax, mspt, msptMax;

    void registrar(long ns, MinecraftServer server) {
        ventana[n++ % ventana.length] = ns;
        if (n % 10 != 0) return;
        long suma = 0; int cuantos = Math.min(n, ventana.length);
        for (int i = 0; i < cuantos; i++) suma += ventana[i];
        nuestro = suma / (float) cuantos / 1e6f;
        nuestroMax = Math.max(nuestroMax, ns / 1e6f);
        if (server != null) {
            long[] t = server.getTickTimes();
            long s = 0, m = 0;
            for (long v : t) { s += v; m = Math.max(m, v); }
            mspt = t.length == 0 ? 0 : s / (float) t.length / 1e6f;
            msptMax = Math.max(msptMax, m / 1e6f);
        }
    }

    float msNuestro() { return nuestro; }
    float msNuestroMax() { return nuestroMax; }
    float msptServidor() { return mspt; }
    float msptMax() { return msptMax; }
}
