package emi.huida.client;

import emi.huida.net.EstadoPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;

/** Parte de cliente de Emi Huida: durante la huida pone la camara en tercera persona por detras y, al terminar, devuelve la que tenia el jugador. */
@Environment(EnvType.CLIENT)
public final class EmiHuidaClient implements ClientModInitializer {
    private static boolean activo, corriendo;
    private static Perspective antes;
    private static volatile EstadoPayload estado;
    private static volatile long ultimo;

    /** El ultimo estado recibido si la huida sigue en marcha (el servidor lo manda cada 2 ticks), si no null. */
    public static EstadoPayload estado() {
        EstadoPayload e = estado;
        return e != null && e.activo() && System.currentTimeMillis() - ultimo < 3000 ? e : null;
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(EstadoPayload.ID, (p, ctx) -> { estado = p.activo() ? p : null; ultimo = System.currentTimeMillis(); ctx.client().execute(() -> poner(p.activo())); });
        // antes de cada tick el jugador mira a lo largo del trazado (el movimiento usa esa mirada)
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            boolean on = CamaraHuida.activa() && mc.player != null;
            if (on) {
                CamaraHuida.fijarMirada(mc.player);
                mc.options.sprintKey.setPressed(true);        // se corre siempre (asi el salto es largo, como en un juego de plataformas)
                corriendo = true;
            } else if (corriendo) {
                corriendo = false;
                net.minecraft.client.option.KeyBinding.updatePressedStates();      // devuelve la tecla de correr a lo que el jugador tenga pulsado de verdad
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> { estado = null; poner(false); });
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
