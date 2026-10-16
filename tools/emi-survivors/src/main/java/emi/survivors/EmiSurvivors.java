package emi.survivors;

import emi.survivors.net.AccionPayload;
import emi.survivors.net.EstadoPayload;
import emi.survivors.net.GemasPayload;
import emi.survivors.net.TextoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emi Survivors: minijuego de supervivencia contra hordas de Pokemon (estilo Vampire Survivors / PokeSurvivor). Solo servidor, salvo la camara, las teclas y la
 * interfaz. Fase 0 (0.1.0): prueba de viabilidad (camara cenital, hordas de Pokemon de Cobblemon movidos por codigo, un ataque automatico y medidas de rendimiento).
 * Ver README.
 */
public class EmiSurvivors implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("emi_survivors");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(EstadoPayload.ID, EstadoPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GemasPayload.ID, GemasPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TextoPayload.ID, TextoPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AccionPayload.ID, AccionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AccionPayload.ID, (payload, ctx) -> ctx.server().execute(() -> Acciones.recibir(ctx.player(), payload)));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Cmds.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(Partida::init);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> Partida.shutdown());
        ServerTickEvents.END_SERVER_TICK.register(Partida::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Partida.alEntrar(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Partida.alSalir(handler.player));

        ServerTickEvents.END_SERVER_TICK.register(Mundo::tick);

        // en una partida y en la sala no se toca nada (ni bloques, ni objetos, ni entidades, ni a los Pokemon enemigos), salvo el terminal y el objeto del menu de la sala
        UseItemCallback.EVENT.register((p, w, hand) -> {
            if (w.isClient || !Mundo.bloqueado(p)) return TypedActionResult.pass(p.getStackInHand(hand));
            if (!Partida.participa(p) && Mundo.esItemMenu(p.getStackInHand(hand)) && p instanceof net.minecraft.server.network.ServerPlayerEntity sp) Menu.enviar(sp, "jugar", null);
            return TypedActionResult.fail(p.getStackInHand(hand));
        });
        UseBlockCallback.EVENT.register((p, w, hand, hit) -> {
            if (w.isClient || !Mundo.bloqueado(p)) return ActionResult.PASS;
            if (!Partida.participa(p) && hit.getBlockPos().equals(Mundo.TERMINAL) && p instanceof net.minecraft.server.network.ServerPlayerEntity sp) { Menu.enviar(sp, "jugar", null); return ActionResult.SUCCESS; }
            return ActionResult.FAIL;
        });
        UseEntityCallback.EVENT.register((p, w, hand, e, hit) -> !w.isClient && Mundo.bloqueado(p) ? ActionResult.FAIL : ActionResult.PASS);
        AttackEntityCallback.EVENT.register((p, w, hand, e, hit) -> !w.isClient && Mundo.bloqueado(p) ? ActionResult.FAIL : ActionResult.PASS);
        AttackBlockCallback.EVENT.register((p, w, hand, pos, dir) -> !w.isClient && Mundo.bloqueado(p) ? ActionResult.FAIL : ActionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((w, p, pos, st, be) -> !Mundo.bloqueado(p));
    }
}
