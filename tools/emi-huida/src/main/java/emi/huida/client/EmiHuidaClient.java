package emi.huida.client;

import emi.huida.net.EstadoPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import org.lwjgl.glfw.GLFW;

/** Parte de cliente de Emi Huida: durante la huida pone la camara en tercera persona por detras y, al terminar, devuelve la que tenia el jugador. */
@Environment(EnvType.CLIENT)
public final class EmiHuidaClient implements ClientModInitializer {
    private static boolean activo, corriendo;
    private static Perspective antes;
    private static volatile EstadoPayload estado;
    private static volatile long ultimo;
    private static KeyBinding teclaListo;
    static volatile emi.huida.net.MapaPayload mapa;
    private static volatile emi.huida.net.HudPayload hud;
    private static volatile long hudDesde;

    /** Lo ultimo que mando el servidor para la interfaz, si es reciente (3 s), si no null. */
    static emi.huida.net.HudPayload hud() { return System.currentTimeMillis() - hudDesde < 3000 ? hud : null; }

    /** El ultimo estado recibido si la huida sigue en marcha (el servidor lo manda cada 2 ticks), si no null. */
    public static EstadoPayload estado() {
        EstadoPayload e = estado;
        return e != null && e.activo() && System.currentTimeMillis() - ultimo < 3000 ? e : null;
    }

    @Override
    public void onInitializeClient() {
        teclaListo = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.emi_huida.listo", GLFW.GLFW_KEY_R, "key.categories.emi_huida"));
        ClientPlayNetworking.registerGlobalReceiver(EstadoPayload.ID, (p, ctx) -> { estado = p.activo() ? p : null; ultimo = System.currentTimeMillis(); ctx.client().execute(() -> poner(p.activo())); });
        // antes de cada tick el jugador mira a lo largo del trazado (el movimiento usa esa mirada)
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            boolean on = CamaraHuida.activa() && mc.player != null;
            if (on) { if (!Ocultar.activo()) Ocultar.aplicar(); Ocultar.mantener(); } else if (Ocultar.activo()) Ocultar.restaurar();
            // mientras el servidor espera a que todos confirmen, la tecla de listo (R) equivale a /huida listo
            while (teclaListo.wasPressed()) {
                EstadoPayload e = estado();
                if (on && e != null && e.espera() && mc.getNetworkHandler() != null) mc.getNetworkHandler().sendChatCommand("huida listo");
            }
            if (on) {
                CamaraHuida.fijarMirada(mc.player);
                mc.options.sprintKey.setPressed(true);        // se corre siempre (asi el salto es largo, como en un juego de plataformas)
                corriendo = true;
            } else if (corriendo) {
                corriendo = false;
                net.minecraft.client.option.KeyBinding.updatePressedStates();      // devuelve la tecla de correr a lo que el jugador tenga pulsado de verdad
            }
        });
        // el menu de la sala: los datos del servidor (JSON) abren o refrescan la pantalla (vacio = cerrarla)
        ClientPlayNetworking.registerGlobalReceiver(emi.huida.net.TextoPayload.ID, (p, ctx) -> ctx.client().execute(() -> menu(p)));
        ClientPlayNetworking.registerGlobalReceiver(emi.huida.net.CamaraPayload.ID, (p, ctx) -> CineCamara.recibir(p));
        ClientPlayNetworking.registerGlobalReceiver(emi.huida.net.EfectoPayload.ID, (p, ctx) -> Efectos.recibir(p));
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(emi.huida.EmiEntity.TIPO, EmiRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(emi.huida.net.MapaPayload.ID, (p, ctx) -> mapa = p);
        ClientPlayNetworking.registerGlobalReceiver(emi.huida.net.HudPayload.ID, (p, ctx) -> { hud = p; hudDesde = System.currentTimeMillis(); });
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(HuidaHud::render);
        ClientPlayNetworking.registerGlobalReceiver(emi.huida.net.EstrellasPayload.ID, (p, ctx) -> ctx.client().execute(() -> EstrellasCliente.recibir(p)));
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(EstrellasCliente::dibujar);
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(EstrellasCliente::dibujarConProveedor);
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> { CineCamara.reiniciar(); Efectos.reiniciar(); estado = null; hud = null; mapa = null; Ocultar.restaurar(); poner(false); EstrellasCliente.recibir(emi.huida.net.EstrellasPayload.borrar()); });
    }

    /** Los datos del menu de la sala: abrirlo o refrescarlo (vacio = cerrarlo). */
    private static void menu(emi.huida.net.TextoPayload p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        switch (p.tipo()) {
            case "menu" -> {
                if (p.json().isEmpty()) { if (mc.currentScreen instanceof MenuPantalla) mc.setScreen(null); }
                else if (MenuPantalla.abierta != null && mc.currentScreen == MenuPantalla.abierta) MenuPantalla.abierta.cargar(p.json(), false);
                else mc.setScreen(new MenuPantalla(p.json()));
            }
            case "menu_act" -> { if (!p.json().isEmpty() && MenuPantalla.abierta != null && mc.currentScreen == MenuPantalla.abierta) MenuPantalla.abierta.cargar(p.json(), true); }
            default -> { }
        }
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
