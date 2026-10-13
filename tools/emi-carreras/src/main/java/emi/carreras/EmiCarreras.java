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
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(emi.carreras.net.EstadoPayload.ID, emi.carreras.net.EstadoPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(emi.carreras.net.PoderPayload.ID, emi.carreras.net.PoderPayload.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Cmds.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(Carrera::init);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> Carrera.shutdown());
        ServerTickEvents.END_SERVER_TICK.register(Carrera::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Carrera.alEntrar(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Carrera.alSalir(handler.player));

        // un corredor solo puede usar su poder: no toca bloques, ni objetos, ni entidades
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient || !Carrera.participa(player)) return TypedActionResult.pass(player.getStackInHand(hand));   // en el cliente siempre PASS: si no, en un mundo de un jugador/LAN el clic nunca llega al servidor
            if (!world.isClient && player instanceof ServerPlayerEntity sp) Carrera.usarPoder(sp);
            return TypedActionResult.fail(player.getStackInHand(hand));
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || !Carrera.participa(player)) return ActionResult.PASS;
            if (!world.isClient && player instanceof ServerPlayerEntity sp) Carrera.usarPoder(sp);
            return ActionResult.FAIL;
        });
        // clic derecho sobre una entidad o clic izquierdo (entidad o bloque): tambien usan el poder (si hay una entidad en la mira, el clic derecho ya no llega al objeto)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient || !Carrera.participa(player)) return ActionResult.PASS;
            if (!world.isClient && hand == net.minecraft.util.Hand.MAIN_HAND && player instanceof ServerPlayerEntity sp) Carrera.usarPoder(sp);
            return ActionResult.FAIL;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient || !Carrera.participa(player)) return ActionResult.PASS;
            if (!world.isClient && player instanceof ServerPlayerEntity sp) Carrera.usarPoder(sp);
            return ActionResult.FAIL;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
            if (world.isClient || !Carrera.participa(player)) return ActionResult.PASS;
            if (!world.isClient && player instanceof ServerPlayerEntity sp) Carrera.usarPoder(sp);
            return ActionResult.FAIL;
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> !Carrera.participa(player));
    }
}
