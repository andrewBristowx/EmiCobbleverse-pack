package emi.huida.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Camara de la huida (como en las persecuciones de Crash Bandicoot): va POR DELANTE del jugador, elevada, mirando hacia atras. Asi se ven
 * los obstaculos que vienen (entre la camara y el jugador), al jugador corriendo hacia la pantalla y, detras, al Tyrantrum que lo persigue.
 * Sigue el rumbo del trazado (que el servidor suaviza en las esquinas). Como el jugador va de cara a la camara, su mirada se fija al trazado
 * (el raton no gira) y izquierda/derecha se invierten (ver KeyboardInputMixin) para que A sea «hacia la izquierda de la pantalla».
 */
public final class CamaraHuida {
    private CamaraHuida() {}

    /** bloques por delante del jugador, altura sobre sus pies y bloques por detras del jugador a los que mira la camara */
    static final double ADELANTE = 6.0, ALTURA = 3.8, MIRA_ATRAS = 8.0, MIRA_ALTO = 2.3;

    /** Prueba: si existe el archivo indicado con -Demi.huida.dbgfile, la camara va al lado del Tyrantrum (para revisar sus animaciones). */
    private static boolean debugLado() {
        String f = System.getProperty("emi.huida.dbgfile");
        return f != null && new java.io.File(f).exists();
    }
    private static float yaw;
    private static long ultimo;
    private static boolean listo;

    public static boolean activa() { return EmiHuidaClient.estado() != null; }

    /** Rumbo suavizado del trazado (el que mira el jugador). */
    public static float rumbo() { return yaw; }

    /** {x, y, z, yaw, pitch} de la camara, o null si no hay huida. */
    public static double[] calcular(float tickDelta) {
        var st = EmiHuidaClient.estado();
        if (st == null) { listo = false; return null; }
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity pl = mc.player;
        if (pl == null || mc.world == null) return null;
        long ahora = System.nanoTime();
        double dt = listo ? Math.min(0.1, (ahora - ultimo) / 1e9) : 1;
        ultimo = ahora;
        if (!listo) { listo = true; yaw = st.rumbo(); }
        yaw = lerpAngulo(yaw, st.rumbo(), 1 - Math.exp(-dt * 8));
        fijarMirada(pl);

        double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
        Vec3d p = pl.getLerpedPos(tickDelta);
        double cx = p.x + fx * ADELANTE, cy = p.y + ALTURA, cz = p.z + fz * ADELANTE;
        double lx = p.x - fx * MIRA_ATRAS, ly = p.y + MIRA_ALTO, lz = p.z - fz * MIRA_ATRAS;
        if (debugLado()) {   // prueba: camara al lado del Tyrantrum, para ver su animacion
            var ch = st.perseguidor() >= 0 ? mc.world.getEntityById(st.perseguidor()) : null;
            if (ch != null) {
                Vec3d c = ch.getLerpedPos(tickDelta);
                cx = c.x + fz * 11; cz = c.z - fx * 11; cy = c.y + 4; lx = c.x; lz = c.z; ly = c.y + 3;
            }
        }
        double dx = lx - cx, dy = ly - cy, dz = lz - cz, h = Math.sqrt(dx * dx + dz * dz);
        return new double[]{cx, cy, cz, Math.toDegrees(Math.atan2(-dx, dz)), -Math.toDegrees(Math.atan2(dy, h))};
    }

    /** El jugador mira siempre a lo largo del trazado (asi W es «adelante» por el camino y el raton no lo desvia). */
    public static void fijarMirada(ClientPlayerEntity pl) {
        pl.setYaw(yaw); pl.prevYaw = yaw;
        pl.setPitch(0f); pl.prevPitch = 0f;
        pl.setHeadYaw(yaw); pl.prevHeadYaw = yaw;
    }

    private static float lerpAngulo(float a, float b, double t) {
        float d = ((b - a + 540f) % 360f) - 180f;
        return (float) (a + d * t);
    }
}
