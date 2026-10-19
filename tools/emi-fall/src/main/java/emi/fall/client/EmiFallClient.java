package emi.fall.client;

import emi.fall.net.CamaraPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;

/**
 * Parte de cliente de Emi Fall: solo cambia la camara. Mientras dura el torneo se juega en tercera persona (se ve el michi y la bomba que lleva encima); al acabar, o al
 * salir del servidor, vuelve a la camara que tenia el jugador. Se puede cambiar con F5 mientras se juega.
 */
public class EmiFallClient implements ClientModInitializer {
    private static boolean forzada;
    private static Perspective antes;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(CamaraPayload.ID, (p, ctx) -> ctx.client().execute(() -> poner(p.tercera())));
        ClientPlayNetworking.registerGlobalReceiver(emi.fall.net.TextoPayload.ID, (p, ctx) -> ctx.client().execute(() -> menu(p)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> forzada = false);
    }

    /** Los datos del menu de la sala: abre la pantalla, la refresca o la cierra (texto vacio). */
    private static void menu(emi.fall.net.TextoPayload p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            switch (p.tipo()) {
                case "menu" -> {
                    if (p.json().isEmpty()) { if (mc.currentScreen instanceof MenuPantalla) mc.setScreen(null); }
                    else if (MenuPantalla.abierta != null && mc.currentScreen == MenuPantalla.abierta) MenuPantalla.abierta.cargar(p.json(), false);
                    else mc.setScreen(new MenuPantalla(p.json()));
                }
                case "menu_act" -> { if (!p.json().isEmpty() && MenuPantalla.abierta != null && mc.currentScreen == MenuPantalla.abierta) MenuPantalla.abierta.cargar(p.json(), true); }
                default -> { }
            }
        } catch (RuntimeException e) {
            emi.fall.EmiFall.LOG.warn("No se pudo abrir el menu de Emi Fall: {}", e.toString());
        }
    }

    private static void poner(boolean tercera) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (tercera) {
            if (forzada) return;
            forzada = true;
            antes = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        } else if (forzada) {
            forzada = false;
            if (antes != null) mc.options.setPerspective(antes);
        }
    }
}
