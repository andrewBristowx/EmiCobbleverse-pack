package emi.fall;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Transforma al jugador en un michi (el Pokemon GatitoAlien de emi-plush) con el mod CobbleMorph, por sus comandos {@code /transform} y {@code /untransform}:
 * todos se ven iguales, como los muñecos de Fall Guys. Si CobbleMorph no esta instalado, el jugador sigue con su aspecto.
 */
final class Morph {
    private Morph() {}

    private static Boolean hay;
    private static Method esTransformado;

    private static boolean disponible() {
        if (hay == null) {
            hay = FabricLoader.getInstance().isModLoaded("cobblemorph");
            if (hay) {
                try { esTransformado = Class.forName("cobblemorph.TransformManager").getMethod("isTransformed", UUID.class); }
                catch (Throwable t) { esTransformado = null; EmiFall.LOG.warn("CobbleMorph esta pero no se puede consultar: {}", t.toString()); }
            }
        }
        return hay;
    }

    static boolean transformado(UUID id) {
        if (!disponible() || esTransformado == null) return false;
        try { return (boolean) esTransformado.invoke(null, id); } catch (Throwable t) { return false; }
    }

    /** Lo deja como un michi; {@code aspectos} = por ejemplo «suit-accessory» o «devil_horns-accessory gold» (vacio = el michi normal). */
    static void michi(MinecraftServer s, ServerPlayerEntity p, String aspectos) {
        camara(p, true);
        if (!disponible()) return;
        if (transformado(p.getUuid())) ejecutar(s, "untransform " + p.getGameProfile().getName());
        ejecutar(s, "transform " + p.getGameProfile().getName() + " gatitoalien" + (aspectos == null || aspectos.isBlank() ? "" : " " + aspectos));
    }

    /** Tercera persona (como en Fall Guys) mientras dura el torneo: lo hace el cliente con el mod; si no lo tiene, no pasa nada. */
    static void camara(ServerPlayerEntity p, boolean tercera) {
        try { if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p, emi.fall.net.CamaraPayload.ID)) net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, new emi.fall.net.CamaraPayload(tercera)); }
        catch (Throwable t) { EmiFall.LOG.debug("camara: {}", t.toString()); }
    }

    static void quitar(MinecraftServer s, ServerPlayerEntity p) {
        camara(p, false);
        if (!disponible() || !transformado(p.getUuid())) return;
        ejecutar(s, "untransform " + p.getGameProfile().getName());
    }

    private static void ejecutar(MinecraftServer s, String cmd) {
        try { s.getCommandManager().executeWithPrefix(s.getCommandSource().withSilent(), cmd); }
        catch (Throwable t) { EmiFall.LOG.warn("No se pudo ejecutar /{}: {}", cmd, t.toString()); }
    }
}
