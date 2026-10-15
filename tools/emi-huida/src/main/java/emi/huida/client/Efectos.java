package emi.huida.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Efectos de dibujo del propio jugador mandados por el servidor ({@link emi.huida.net.EfectoPayload}); el mixin del dibujo del jugador los lee. */
@Environment(EnvType.CLIENT)
public final class Efectos {
    private Efectos() {}
    private static volatile float aplastado;
    private static volatile long desde;

    public static void recibir(emi.huida.net.EfectoPayload p) { aplastado = p.aplastado(); desde = System.currentTimeMillis(); }

    /** 0 = normal, 1 = aplastado del todo (si el servidor deja de mandarlo, a los 2 s vuelve a 0). */
    public static float aplastado() { return System.currentTimeMillis() - desde < 2000 ? aplastado : 0f; }

    public static void reiniciar() { aplastado = 0f; }
}
