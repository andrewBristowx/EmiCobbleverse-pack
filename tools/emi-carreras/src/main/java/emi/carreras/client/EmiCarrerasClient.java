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

    private static boolean ocultando, tercera;
    private static net.minecraft.client.option.Perspective perspectivaAntes;
    private static long escalaMod; private static int escalaPuesta = -1;
    private static Boolean partyAntes, minimapaAntes, jadeAntes;
    private static Object dolaresAntes;

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
        if (Boolean.getBoolean("emi.carreras.debug")) probarEscala(mc);
        if (activo()) {
            if (!ocultando) { ocultando = true; ocultarOtros(); }
            mc.getToastManager().clear();
            fijarParty(true);
            if (estado != null && estado.fase() >= 1 && !tercera) {          // en la carrera se juega en tercera persona (se puede cambiar con F5)
                tercera = true; perspectivaAntes = mc.options.getPerspective();
                mc.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
            }
        } else if (ocultando) {
            restaurar();
        }
    }

    /** Solo pruebas (-Demi.carreras.debug=true): cambia la escala de la interfaz leyendo un numero de <juego>/emi_escala.txt, para probar la interfaz con varias escalas sin reiniciar. */
    private static void probarEscala(MinecraftClient mc) {
        try {
            java.nio.file.Path f = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("emi_escala.txt");
            if (!java.nio.file.Files.exists(f)) return;
            long mod = java.nio.file.Files.getLastModifiedTime(f).toMillis();
            if (mod == escalaMod) return;
            escalaMod = mod;
            int n = Integer.parseInt(java.nio.file.Files.readString(f).trim());
            mc.options.getGuiScale().setValue(n);
            mc.onResolutionChanged();
            LOG.info("escala de interfaz -> {} (ventana escalada {}x{})", n, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
        } catch (Throwable t) { LOG.debug("escala: {}", t.toString()); }
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
        fijarDolares(false);
    }

    /** Contador de CobbleDollars (esquina de abajo a la derecha): se pone en DISABLED solo en memoria y se devuelve al terminar. */
    private static void fijarDolares(boolean mostrar) {
        try {
            Class<?> cl = Class.forName("fr.harmex.cobbledollars.common.client.CobbleDollarsClient");
            Object cfg = cl.getMethod("getConfig").invoke(cl.getField("INSTANCE").get(null));
            Class<?> pos = Class.forName("fr.harmex.cobbledollars.common.client.utils.OverlayPosition");
            if (mostrar) {
                if (dolaresAntes != null) cfg.getClass().getMethod("setOverlayPosition", pos).invoke(cfg, dolaresAntes);
            } else {
                Object actual = cfg.getClass().getMethod("getOverlayPosition").invoke(cfg);
                if (dolaresAntes == null) dolaresAntes = actual;
                cfg.getClass().getMethod("setOverlayPosition", pos).invoke(cfg, pos.getMethod("valueOf", String.class).invoke(null, "DISABLED"));
            }
        } catch (Throwable t) { LOG.debug("cobbledollars: {}", t.toString()); }
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
        if (tercera) { tercera = false; if (perspectivaAntes != null) MinecraftClient.getInstance().options.setPerspective(perspectivaAntes); }
        if (partyAntes != null) fijarParty(partyAntes);
        if (minimapaAntes != null && minimapaAntes) {
            try {
                Class<?> c = Class.forName("journeymap.api.client.impl.ClientAPI");
                Object api = c.getField("INSTANCE").get(null);
                c.getMethod("toggleMinimap", boolean.class).invoke(api, true);
            } catch (Throwable t) { LOG.debug("no se pudo restaurar el minimapa: {}", t.toString()); }
        }
        if (jadeAntes != null) fijarJade(jadeAntes);
        fijarDolares(true);
        partyAntes = null; minimapaAntes = null; jadeAntes = null; dolaresAntes = null;
    }
}
