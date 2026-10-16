package emi.survivors.client;

import emi.survivors.net.EstadoPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;

/**
 * Parte de cliente de Emi Survivors: mientras el servidor dice que hay partida pone la camara cenital (en tercera persona, para ver al Pokemon),
 * oculta las interfaces de otros mods y dibuja la interfaz propia; al terminar devuelve la camara que tenia el jugador.
 */
@Environment(EnvType.CLIENT)
public final class EmiSurvivorsClient implements ClientModInitializer {
    private static boolean activo;
    private static Perspective antes;
    private static volatile EstadoPayload estado;
    private static volatile long ultimo;
    private static long ultimaMedida;

    /** El ultimo estado recibido si la partida sigue en marcha (el servidor lo manda cada 2 ticks), si no null. */
    public static EstadoPayload estado() {
        EstadoPayload e = estado;
        return e != null && e.activo() && System.currentTimeMillis() - ultimo < 3000 ? e : null;
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(EstadoPayload.ID, (p, ctx) -> {
            estado = p.activo() ? p : null; ultimo = System.currentTimeMillis();
            ctx.client().execute(() -> poner(p.activo()));
        });
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            boolean on = CamaraSurvivors.activa() && mc.player != null;
            if (on) { if (!Ocultar.activo()) Ocultar.aplicar(); Ocultar.mantener(); CamaraSurvivors.fijarMirada(mc.player); }
            else if (Ocultar.activo()) Ocultar.restaurar();
            if (on && Boolean.getBoolean("emi.survivors.debug") && System.currentTimeMillis() - ultimaMedida > 5000) {
                ultimaMedida = System.currentTimeMillis();
                emi.survivors.EmiSurvivors.LOG.info("[cliente] fps={} enemigos={}", mc.getCurrentFps(), estado == null ? -1 : estado.enemigos());
            }
        });
        HudRenderCallback.EVENT.register(SurvivorsHud::render);
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> { estado = null; Ocultar.restaurar(); poner(false); });
    }

    private static void poner(boolean on) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (on && !activo) {
            activo = true; antes = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        } else if (!on && activo) {
            activo = false;
            if (antes != null) mc.options.setPerspective(antes);
        }
    }
}
