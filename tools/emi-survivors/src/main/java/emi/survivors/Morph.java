package emi.survivors;

import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Transforma al jugador en su Pokemon con el mod CobbleMorph (si esta instalado): el cliente dibuja el modelo del Pokemon en lugar del
 * jugador, asi que va exactamente pegado a el (sin desfase). Si CobbleMorph no esta, el jugador seguiria con su aspecto normal.
 * Se maneja por reflexion y por los comandos /transform y /untransform de CobbleMorph, para no depender de el al compilar.
 */
public final class Morph {
    private static Boolean hay;
    private static Method esTransformado, nombre, aspectos;

    public static boolean disponible() {
        if (hay == null) {
            hay = FabricLoader.getInstance().isModLoaded("cobblemorph");
            if (hay) {
                try {
                    Class<?> c = Class.forName("cobblemorph.TransformManager");
                    esTransformado = c.getMethod("isTransformed", UUID.class);
                    nombre = c.getMethod("getPokemonName", UUID.class);
                    aspectos = c.getMethod("getAspects", UUID.class);
                } catch (Throwable t) { hay = false; EmiSurvivors.LOG.warn("CobbleMorph esta pero no se puede usar: {}", t.toString()); }
            }
        }
        return hay;
    }

    /** "nombre|aspectos" de la transformacion actual del jugador, o "" si no esta transformado. */
    public static String actual(UUID id) {
        if (!disponible()) return "";
        try {
            if (!(boolean) esTransformado.invoke(null, id)) return "";
            String n = (String) nombre.invoke(null, id), a = (String) aspectos.invoke(null, id);
            return n + "|" + (a == null ? "" : a);
        } catch (Throwable t) { return ""; }
    }

    /** Convierte al jugador en el Pokemon dado. */
    public static void transformar(MinecraftServer s, ServerPlayerEntity p, Pokemon pk) {
        if (!disponible()) return;
        String especie = pk.getSpecies().getResourceIdentifier().getPath();
        String asp = String.join(" ", pk.getAspects());
        ejecutar(s, "transform " + p.getGameProfile().getName() + " " + especie + (asp.isBlank() ? "" : " " + asp));
    }

    /** Quita la transformacion y, si tenia otra antes ("nombre|aspectos"), se la devuelve. */
    public static void restaurar(MinecraftServer s, ServerPlayerEntity p, String antes) {
        if (!disponible()) return;
        String nom = p.getGameProfile().getName();
        ejecutar(s, "untransform " + nom);
        if (antes != null && !antes.isEmpty()) {
            String[] x = antes.split("\\|", -1);
            ejecutar(s, "transform " + nom + " " + x[0] + (x.length > 1 && !x[1].isBlank() ? " " + x[1] : ""));
        }
    }

    private static void ejecutar(MinecraftServer s, String cmd) {
        try { s.getCommandManager().executeWithPrefix(s.getCommandSource().withSilent(), cmd); }
        catch (Exception e) { EmiSurvivors.LOG.warn("Fallo '{}': {}", cmd, e.toString()); }
    }
}
