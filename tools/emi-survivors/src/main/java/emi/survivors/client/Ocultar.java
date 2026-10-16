package emi.survivors.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mientras dura una partida se ocultan las interfaces de otros mods que estorban (lista de Pokemon de Cobblemon, minimapa de JourneyMap, cuadro de Jade y
 * contador de CobbleDollars) y al terminar se devuelven a como estaban. Todo por reflexion y solo en memoria: si un mod no esta, no pasa nada.
 * (Es lo mismo que hacen Emi Carreras y Emi Huida.)
 */
@Environment(EnvType.CLIENT)
final class Ocultar {
    private Ocultar() {}
    private static final Logger LOG = LoggerFactory.getLogger("emi_survivors/cliente");

    private static boolean activo;
    private static Boolean partyAntes, minimapaAntes, jadeAntes;
    private static Object dolaresAntes;

    static boolean activo() { return activo; }

    static void aplicar() {
        if (activo) return;
        activo = true;
        try {
            Class<?> k = Class.forName("com.cobblemon.mod.common.client.keybind.keybinds.HidePartyBinding");
            Object inst = k.getField("INSTANCE").get(null);
            partyAntes = (Boolean) k.getMethod("getShouldHide").invoke(inst);
            k.getMethod("setShouldHide", boolean.class).invoke(inst, true);
        } catch (Throwable t) { LOG.debug("no se pudo ocultar la lista de Pokemon: {}", t.toString()); }
        try {
            Class<?> c = Class.forName("journeymap.api.client.impl.ClientAPI");
            Object api = c.getField("INSTANCE").get(null);
            minimapaAntes = (Boolean) c.getMethod("minimapEnabled").invoke(api);
            c.getMethod("toggleMinimap", boolean.class).invoke(api, false);
        } catch (Throwable t) { LOG.debug("no se pudo ocultar el minimapa: {}", t.toString()); }
        fijarJade(false);
        fijarDolares(false);
    }

    /** Cada tick: Cobblemon puede volver a mostrar su lista, y los avisos (logros, etc.) se quitan. */
    static void mantener() {
        fijarParty(true);
        net.minecraft.client.MinecraftClient.getInstance().getToastManager().clear();
    }

    static void restaurar() {
        if (!activo) return;
        activo = false;
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

    private static void fijarParty(boolean oculto) {
        try {
            Class<?> k = Class.forName("com.cobblemon.mod.common.client.keybind.keybinds.HidePartyBinding");
            Object inst = k.getField("INSTANCE").get(null);
            k.getMethod("setShouldHide", boolean.class).invoke(inst, oculto);
        } catch (Throwable ignored) { }
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
}
