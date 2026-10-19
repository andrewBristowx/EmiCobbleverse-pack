package emi.fall;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Torneo de minijuegos por rondas (estilo Fall Guys) para el servidor de Emi. Solo servidor: los jugadores no necesitan instalar nada. Ver README. */
public class EmiFall implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("emi_fall");

    /** ¿Esta en la sala o en un torneo en marcha (no toca nada del mundo)? */
    static boolean encerrado(net.minecraft.entity.player.PlayerEntity p) {
        return p instanceof ServerPlayerEntity sp && (Sala.enSala(sp) || Torneo.participa(sp));
    }

    @Override
    public void onInitialize() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(emi.fall.net.CamaraPayload.ID, emi.fall.net.CamaraPayload.CODEC);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(Minijuegos::soltarTodoForzado);          // por si el servidor se paro a medias
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Cmds.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(Torneo::tick);
        ServerTickEvents.END_SERVER_TICK.register(Sala::tick);
        ServerTickEvents.END_SERVER_TICK.register(Sonido::tick);
        ServerTickEvents.END_SERVER_TICK.register(Retornos::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Retornos.alEntrar(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Torneo.alDesconectar(handler.player));

        // nadie se hace daño ni pierde hambre en la sala ni en el torneo (las caidas las gestiona cada minijuego)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayerEntity p && encerrado(p)));

        // en la sala y en el torneo no se toca nada (el boton derecho con un objeto lo atiende el minijuego: las bombas)
        UseItemCallback.EVENT.register((p, w, hand) -> {
            if (w.isClient || !(p instanceof ServerPlayerEntity sp) || !encerrado(sp)) return TypedActionResult.pass(p.getStackInHand(hand));
            Torneo.alUsar(sp);
            return TypedActionResult.fail(p.getStackInHand(hand));
        });
        UseBlockCallback.EVENT.register((p, w, hand, hit) -> {
            if (w.isClient || !encerrado(p)) return ActionResult.PASS;
            if (p instanceof ServerPlayerEntity sp) Torneo.alUsar(sp);                 // tirar la bomba tambien al hacer clic derecho sobre un bloque
            return ActionResult.FAIL;
        });
        UseEntityCallback.EVENT.register((p, w, hand, e, hit) -> !w.isClient && encerrado(p) ? ActionResult.FAIL : ActionResult.PASS);
        AttackEntityCallback.EVENT.register((p, w, hand, e, hit) -> !w.isClient && encerrado(p) ? ActionResult.FAIL : ActionResult.PASS);
        AttackBlockCallback.EVENT.register((p, w, hand, pos, dir) -> !w.isClient && encerrado(p) ? ActionResult.FAIL : ActionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((w, p, pos, st, be) -> !encerrado(p));
    }
}
