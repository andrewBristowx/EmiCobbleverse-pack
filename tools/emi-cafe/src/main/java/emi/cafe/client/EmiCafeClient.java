package emi.cafe.client;

import emi.cafe.net.TextoPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;

/** Parte de cliente del Cafe Michialien: abre, refresca y cierra la pantalla del menu con los datos que manda el servidor. */
public class EmiCafeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        for (net.minecraft.block.Block b : emi.cafe.CafeBlocks.PUERTAS) BlockRenderLayerMap.INSTANCE.putBlock(b, RenderLayer.getTranslucent());      // el cristal de las puertas se ve a traves
        for (net.minecraft.block.Block b : emi.cafe.CafeBlocks.VITRINAS) BlockRenderLayerMap.INSTANCE.putBlock(b, RenderLayer.getTranslucent());     // y el de las vitrinas
        ClientPlayNetworking.registerGlobalReceiver(TextoPayload.ID, (p, ctx) -> ctx.client().execute(() -> menu(p)));
        HudRenderCallback.EVENT.register((g, tick) -> PanelCafe.dibujar(g));
        // dentro del cafe se esconden el equipo Pokemon, el minimapa y los otros cuadros (hay un panel propio); al salir se devuelven
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (PanelCafe.activo()) { if (!Ocultar.activo()) Ocultar.aplicar(); Ocultar.mantener(); }
            else if (Ocultar.activo()) Ocultar.restaurar();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> { PanelCafe.olvidar(); Ocultar.restaurar(); });
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
                case "panel" -> PanelCafe.recibir(p.json());
                case "libro" -> mc.setScreen(new LibroPantalla(p.json()));
                case "menu_act" -> { if (!p.json().isEmpty() && MenuPantalla.abierta != null && mc.currentScreen == MenuPantalla.abierta) MenuPantalla.abierta.cargar(p.json(), true); }
                default -> { }
            }
        } catch (RuntimeException e) {
            emi.cafe.EmiCafe.LOG.warn("No se pudo abrir el menu del cafe: {}", e.toString());
        }
    }
}
