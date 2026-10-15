package emi.huida;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.UUID;

/** Solo comprueba (por reflexion, sin depender de CobbleMorph al compilar) si el jugador esta transformado en un Pokemon. */
final class Morph {
    private static Boolean hay;
    private static Method esTransformado;

    static boolean transformado(UUID id) {
        if (hay == null) {
            hay = FabricLoader.getInstance().isModLoaded("cobblemorph");
            if (hay) {
                try { esTransformado = Class.forName("cobblemorph.TransformManager").getMethod("isTransformed", UUID.class); }
                catch (Throwable t) { hay = false; EmiHuida.LOG.warn("CobbleMorph esta pero no se puede consultar: {}", t.toString()); }
            }
        }
        if (!hay) return false;
        try { return (boolean) esTransformado.invoke(null, id); } catch (Throwable t) { return false; }
    }
}
