package emi.survivors.client;

import emi.survivors.net.EstadoPayload;
import emi.survivors.net.GemasPayload;
import emi.survivors.net.TextoPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;

/**
 * Parte de cliente de Emi Survivors: mientras el servidor dice que hay partida pone la camara cenital (en tercera persona, para ver al Pokemon),
 * oculta las interfaces de otros mods, dibuja la interfaz propia y las gemas, y abre las pantallas (cartas al subir de nivel); al terminar devuelve la camara que tenia el jugador.
 */
@Environment(EnvType.CLIENT)
public final class EmiSurvivorsClient implements ClientModInitializer {
    private static boolean activo;
    private static Perspective antes;
    private static volatile EstadoPayload estado;
    private static volatile long ultimo;
    private static long ultimaMedida;

    /** El ultimo estado recibido si la partida sigue en marcha (el servidor lo manda cada 4 ticks), si no null. */
    public static EstadoPayload estado() {
        EstadoPayload e = estado;
        return e != null && e.activo() && System.currentTimeMillis() - ultimo < 3000 ? e : null;
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(EstadoPayload.ID, (p, ctx) -> {
            estado = p.activo() ? p : null; ultimo = System.currentTimeMillis();
            ctx.client().execute(() -> {
                poner(p.activo());
                if (!p.activo()) { GemasCliente.borrar(); cerrarCartas(); }
                else if (ctx.client().currentScreen instanceof MenuPantalla) ctx.client().setScreen(null);       // la partida ya esta en marcha: el menu sobra
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(GemasPayload.ID, (p, ctx) -> ctx.client().execute(() -> GemasCliente.recibir(p)));
        ClientPlayNetworking.registerGlobalReceiver(TextoPayload.ID, (p, ctx) -> ctx.client().execute(() -> texto(p)));
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            boolean on = CamaraSurvivors.activa() && mc.player != null;
            if (on) { if (!Ocultar.activo()) Ocultar.aplicar(); Ocultar.mantener(); CamaraSurvivors.fijarMirada(mc.player); }
            else if (Ocultar.activo()) Ocultar.restaurar();
            if (on && Boolean.getBoolean("emi.survivors.debug") && System.currentTimeMillis() - ultimaMedida > 5000) {
                ultimaMedida = System.currentTimeMillis();
                emi.survivors.EmiSurvivors.LOG.info("[cliente] fps={} enemigos={} gemas={}", mc.getCurrentFps(), estado == null ? -1 : estado.enemigos(), GemasCliente.cuantas());
            }
        });
        HudRenderCallback.EVENT.register(SurvivorsHud::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(GemasCliente::dibujar);
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> { estado = null; Ocultar.restaurar(); poner(false); GemasCliente.borrar(); });
    }

    private static void texto(TextoPayload p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        switch (p.tipo()) {
            case "cartas" -> { if (p.json().isEmpty()) cerrarCartas(); else mc.setScreen(new CartasPantalla(p.json())); }
            case "menu" -> {
                if (p.json().isEmpty()) { if (mc.currentScreen instanceof MenuPantalla) mc.setScreen(null); }
                else if (MenuPantalla.abierta != null && mc.currentScreen == MenuPantalla.abierta) MenuPantalla.abierta.cargar(p.json(), false);
                else mc.setScreen(new MenuPantalla(p.json()));
            }
            case "menu_act" -> { if (!p.json().isEmpty() && MenuPantalla.abierta != null && mc.currentScreen == MenuPantalla.abierta) MenuPantalla.abierta.cargar(p.json(), true); }
            default -> { }
        }
    }

    private static void cerrarCartas() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof CartasPantalla) mc.setScreen(null);
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
