package emi.carreras;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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

/** Minijuego de carreras de Pokemon (solo servidor). Ver README. */
public class EmiCarreras implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("emi_carreras");

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Cmds.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(Carrera::init);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> Carrera.shutdown());
        ServerTickEvents.END_SERVER_TICK.register(Carrera::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Carrera.alEntrar(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Carrera.alSalir(handler.player));

        // un corredor solo puede usar su poder: no toca bloques, ni objetos, ni entidades
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!Carrera.participa(player)) return TypedActionResult.pass(player.getStackInHand(hand));
            if (!world.isClient && player instanceof ServerPlayerEntity sp) Carrera.usarPoder(sp);
            return TypedActionResult.fail(player.getStackInHand(hand));
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!Carrera.participa(player)) return ActionResult.PASS;
            if (!world.isClient && player instanceof ServerPlayerEntity sp) Carrera.usarPoder(sp);
            return ActionResult.FAIL;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> Carrera.participa(player) ? ActionResult.FAIL : ActionResult.PASS);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> Carrera.participa(player) ? ActionResult.FAIL : ActionResult.PASS);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> Carrera.participa(player) ? ActionResult.FAIL : ActionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> !Carrera.participa(player));
    }
}
