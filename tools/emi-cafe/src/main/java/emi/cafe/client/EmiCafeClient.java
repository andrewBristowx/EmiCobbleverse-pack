package emi.cafe.client;

import emi.cafe.net.TextoPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/** Parte de cliente del Cafe Michialien: abre, refresca y cierra la pantalla del menu con los datos que manda el servidor. */
public class EmiCafeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(TextoPayload.ID, (p, ctx) -> ctx.client().execute(() -> menu(p)));
    }

    private static void menu(TextoPayload p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            switch (p.tipo()) {
                case "menu" -> {
                    if (p.json().isEmpty()) { if (mc.currentScreen instanceof MenuPantalla) mc.setScreen(null); }
                    else if (MenuPantalla.abierta != null && mc.currentScreen == MenuPantalla.abierta) MenuPantalla.abierta.cargar(p.json(), false);
                    else mc.setScreen(new MenuPantalla(p.json()));
                }
                case "libro" -> mc.setScreen(new LibroPantalla(p.json()));
                case "menu_act" -> { if (!p.json().isEmpty() && MenuPantalla.abierta != null && mc.currentScreen == MenuPantalla.abierta) MenuPantalla.abierta.cargar(p.json(), true); }
                default -> { }
            }
        } catch (RuntimeException e) {
            emi.cafe.EmiCafe.LOG.warn("No se pudo abrir el menu del cafe: {}", e.toString());
        }
    }
}
