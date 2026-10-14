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
    private static boolean activo;
    private static Perspective antes;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(EstadoPayload.ID, (p, ctx) -> ctx.client().execute(() -> poner(p.activo())));
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> poner(false));
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
