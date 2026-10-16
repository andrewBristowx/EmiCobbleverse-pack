package emi.survivors.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Camara de Emi Survivors: cenital e inclinada, fija (no gira con el raton), al sur del jugador y mirando hacia el norte, siguiendolo. Asi «arriba» en la pantalla es
 * el norte (-z) y «derecha» el este (+x); las teclas de movimiento se interpretan respecto a la pantalla (ver KeyboardInputMixin) y el Pokemon mira hacia donde anda.
 * La altura y la distancia se pueden cambiar para probar con -Demi.survivors.altura=N y -Demi.survivors.atras=N.
 */
public final class CamaraSurvivors {
    private CamaraSurvivors() {}

    static final double ALTURA = Double.parseDouble(System.getProperty("emi.survivors.altura", "14"));
    static final double ATRAS = Double.parseDouble(System.getProperty("emi.survivors.atras", "6"));
    static final double MIRA_ALTO = 0.8;

    /** Hacia donde mira el Pokemon (grados de Minecraft): la direccion de la ultima tecla pulsada. */
    public static volatile float rumbo = 180f;

    public static boolean activa() { return EmiSurvivorsClient.estado() != null; }

    /** {x, y, z, yaw, pitch} de la camara, o null si no hay partida. */
    public static double[] calcular(float tickDelta) {
        if (!activa()) return null;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity pl = mc.player;
        if (pl == null || mc.world == null) return null;
        fijarMirada(pl);
        Vec3d p = pl.getLerpedPos(tickDelta);
        double cx = p.x, cy = p.y + ALTURA, cz = p.z + ATRAS;
        double dx = p.x - cx, dy = (p.y + MIRA_ALTO) - cy, dz = p.z - cz, h = Math.sqrt(dx * dx + dz * dz);
        return new double[]{cx, cy, cz, Math.toDegrees(Math.atan2(-dx, dz)), -Math.toDegrees(Math.atan2(dy, h))};
    }

    /** El jugador mira siempre hacia donde anda (el raton no lo desvia). */
    public static void fijarMirada(ClientPlayerEntity pl) {
        float r = rumbo;
        pl.setYaw(r); pl.prevYaw = r;
        pl.setPitch(0f); pl.prevPitch = 0f;
        pl.setHeadYaw(r); pl.prevHeadYaw = r;
        pl.setBodyYaw(r); pl.prevBodyYaw = r;
    }
}
