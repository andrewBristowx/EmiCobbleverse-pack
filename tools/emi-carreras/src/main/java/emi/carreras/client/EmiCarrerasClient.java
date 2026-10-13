package emi.carreras.client;

import emi.carreras.net.EstadoPayload;
import emi.carreras.net.PoderPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parte de cliente de Emi Carreras: interfaz propia durante la carrera (tabla de posiciones, mapa con las cabezas de los corredores y tarjeta del poder)
 * y, mientras dura, oculta las interfaces que estorban (lista de Pokémon de Cobblemon, minimapa de JourneyMap y avisos). Si el servidor no manda nada, no hace nada.
 */
@Environment(EnvType.CLIENT)
public final class EmiCarrerasClient implements ClientModInitializer {
    static final Logger LOG = LoggerFactory.getLogger("emi_carreras/cliente");

    public static volatile EstadoPayload estado;
    public static volatile long ultimoEstado;
    public static volatile PoderPayload poder;
    public static volatile long poderDesde;

    private static boolean ocultando;
    private static Boolean partyAntes, minimapaAntes, jadeAntes;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(EstadoPayload.ID, (pl, ctx) -> {
            estado = pl.activo() ? pl : null;
            ultimoEstado = System.currentTimeMillis();
            if (!pl.activo()) poder = null;
        });
        ClientPlayNetworking.registerGlobalReceiver(PoderPayload.ID, (pl, ctx) -> {
            poder = pl.id().isEmpty() ? null : pl;
            poderDesde = System.currentTimeMillis();
        });
        ClientTickEvents.END_CLIENT_TICK.register(EmiCarrerasClient::tick);
        HudRenderCallback.EVENT.register(CarrerasHud::render);
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> { estado = null; poder = null; restaurar(); });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(c -> restaurar());
    }

    /** La carrera esta en marcha para este cliente (el servidor manda el estado cada ~0,25 s). */
    public static boolean activo() { return estado != null && System.currentTimeMillis() - ultimoEstado < 3000; }

    private static void tick(MinecraftClient mc) {
        if (activo()) {
            if (!ocultando) { ocultando = true; ocultarOtros(); }
            mc.getToastManager().clear();
            fijarParty(true);
        } else if (ocultando) {
            restaurar();
        }
    }

    // ---------- ocultar la lista de Pokémon de Cobblemon, el minimapa de JourneyMap y el cuadro de Jade (por reflexion: si no estan, no pasa nada)

    private static void ocultarOtros() {
        try {
            Class<?> k = Class.forName("com.cobblemon.mod.common.client.keybind.keybinds.HidePartyBinding");
            Object inst = k.getField("INSTANCE").get(null);
            partyAntes = (Boolean) k.getMethod("getShouldHide").invoke(inst);
            k.getMethod("setShouldHide", boolean.class).invoke(inst, true);
        } catch (Throwable t) { LOG.debug("no se pudo ocultar la lista de Pokémon: {}", t.toString()); }
        try {
            Class<?> c = Class.forName("journeymap.api.client.impl.ClientAPI");
            Object api = c.getField("INSTANCE").get(null);
            minimapaAntes = (Boolean) c.getMethod("minimapEnabled").invoke(api);
            c.getMethod("toggleMinimap", boolean.class).invoke(api, false);
        } catch (Throwable t) { LOG.debug("no se pudo ocultar el minimapa: {}", t.toString()); }
        fijarJade(false);
    }

    private static void fijarJade(boolean mostrar) {
        try {
            Class<?> c = Class.forName("snownee.jade.api.config.IWailaConfig");
            Object cfg = c.getMethod("get").invoke(null);
            Object gen = c.getMethod("getGeneral").invoke(cfg);
            Class<?> g = Class.forName("snownee.jade.api.config.IWailaConfig$IConfigGeneral");
            if (jadeAntes == null) jadeAntes = (Boolean) g.getMethod("shouldDisplayTooltip").invoke(gen);
            g.getMethod("setDisplayTooltip", boolean.class).invoke(gen, mostrar);
        } catch (Throwable t) { LOG.debug("jade: {}", t.toString()); }
    }

    private static void fijarParty(boolean oculto) {
        try {
            Class<?> k = Class.forName("com.cobblemon.mod.common.client.keybind.keybinds.HidePartyBinding");
            Object inst = k.getField("INSTANCE").get(null);
            k.getMethod("setShouldHide", boolean.class).invoke(inst, oculto);
        } catch (Throwable ignored) { }
    }

    private static void restaurar() {
        if (!ocultando) return;
        ocultando = false;
        if (partyAntes != null) fijarParty(partyAntes);
        if (minimapaAntes != null && minimapaAntes) {
            try {
                Class<?> c = Class.forName("journeymap.api.client.impl.ClientAPI");
                Object api = c.getField("INSTANCE").get(null);
                c.getMethod("toggleMinimap", boolean.class).invoke(api, true);
            } catch (Throwable t) { LOG.debug("no se pudo restaurar el minimapa: {}", t.toString()); }
        }
        if (jadeAntes != null) fijarJade(jadeAntes);
        partyAntes = null; minimapaAntes = null; jadeAntes = null;
    }
}
