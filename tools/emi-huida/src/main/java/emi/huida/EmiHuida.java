package emi.huida;

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

/** Minijuego de huida de un Pokemon perseguidor (solo servidor, salvo la camara). Ver README. */
public class EmiHuida implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("emi_huida");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(emi.huida.net.EstadoPayload.ID, emi.huida.net.EstadoPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(emi.huida.net.EstrellasPayload.ID, emi.huida.net.EstrellasPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(emi.huida.net.MapaPayload.ID, emi.huida.net.MapaPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(emi.huida.net.HudPayload.ID, emi.huida.net.HudPayload.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Cmds.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(Huida::init);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> Huida.shutdown());
        ServerTickEvents.END_SERVER_TICK.register(Huida::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Huida.alEntrar(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Huida.alSalir(handler.player));

        // quien esta en la huida no toca bloques, objetos ni entidades (ni al Pokemon perseguidor)
        UseItemCallback.EVENT.register((p, w, hand) -> !w.isClient && Huida.participa(p) ? TypedActionResult.fail(p.getStackInHand(hand)) : TypedActionResult.pass(p.getStackInHand(hand)));
        UseBlockCallback.EVENT.register((p, w, hand, hit) -> !w.isClient && Huida.participa(p) ? ActionResult.FAIL : ActionResult.PASS);
        UseEntityCallback.EVENT.register((p, w, hand, e, hit) -> !w.isClient && Huida.participa(p) ? ActionResult.FAIL : ActionResult.PASS);
        AttackEntityCallback.EVENT.register((p, w, hand, e, hit) -> !w.isClient && Huida.participa(p) ? ActionResult.FAIL : ActionResult.PASS);
        AttackBlockCallback.EVENT.register((p, w, hand, pos, dir) -> !w.isClient && Huida.participa(p) ? ActionResult.FAIL : ActionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((w, p, pos, st, be) -> !Huida.participa(p));
    }
}
