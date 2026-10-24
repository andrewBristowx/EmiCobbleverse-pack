package emi.cafe;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Cafe Michialien: un restaurante de Pokemon donde se sirve llevando los ingredientes en las manos de una estacion a otra. Servidor + texturas de objetos y bloques propios. Ver README. */
public class EmiCafe implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("emi_cafe");

    /** Lo guardado de cada jugador (monedas...); se abre al arrancar el servidor, dentro de la carpeta del mundo ({@code emi_cafe/}). */
    static Datos datos;

    /** ¿Esta el jugador dentro de un cafe (no toca nada del mundo)? */
    static boolean encerrado(net.minecraft.entity.player.PlayerEntity p) { return p instanceof ServerPlayerEntity sp && Cafes.enCafe(sp); }

    @Override
    public void onInitialize() {
        CafeItems.init();
        CafeBlocks.init();
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(emi.cafe.net.TextoPayload.ID, emi.cafe.net.TextoPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(emi.cafe.net.AccionPayload.ID, emi.cafe.net.AccionPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(emi.cafe.net.AccionPayload.ID, (payload, ctx) -> ctx.server().execute(() -> Menus.recibir(ctx.player(), payload)));
        ServerLifecycleEvents.SERVER_STARTED.register(s -> datos = new Datos(s.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("emi_cafe")));
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> { Cafes.cerrarTodos(); datos = null; });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Cmds.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(Cafes::tick);
        ServerTickEvents.END_SERVER_TICK.register(Retornos::tick);
        ServerTickEvents.END_SERVER_TICK.register(Sonidos::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Retornos.alEntrar(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Cafes.alDesconectar(handler.player));

        // dentro del cafe nadie se hace daño
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayerEntity p && encerrado(p)));

        // el unico gesto del juego es el clic derecho en un bloque o en un cliente; todo lo demas queda bloqueado
        UseItemCallback.EVENT.register((p, w, hand) -> !w.isClient && encerrado(p) ? TypedActionResult.fail(p.getStackInHand(hand)) : TypedActionResult.pass(p.getStackInHand(hand)));
        UseBlockCallback.EVENT.register((p, w, hand, hit) -> {
            if (w.isClient || !encerrado(p)) return ActionResult.PASS;
            if (hand == Hand.MAIN_HAND && p instanceof ServerPlayerEntity sp) {
                Cafe c = Cafes.de(sp.getUuid());
                if (c != null && c.construyendo && c.esDuenio(sp)) return c.clicConstruccion(sp, hit, hand);
                if (c != null) c.usarBloque(sp, hit.getBlockPos());
            }
            return ActionResult.FAIL;
        });
        UseEntityCallback.EVENT.register((p, w, hand, e, hit) -> {
            if (w.isClient || !encerrado(p)) return ActionResult.PASS;
            if (hand == Hand.MAIN_HAND && p instanceof ServerPlayerEntity sp) {
                Cafe c = Cafes.de(sp.getUuid());
                if (c != null) c.usarEntidad(sp, e);
            }
            return ActionResult.FAIL;
        });
        AttackEntityCallback.EVENT.register((p, w, hand, e, hit) -> !w.isClient && encerrado(p) ? ActionResult.FAIL : ActionResult.PASS);
        AttackBlockCallback.EVENT.register((p, w, hand, pos, dir) -> {
            if (w.isClient || !encerrado(p)) return ActionResult.PASS;
            if (p instanceof ServerPlayerEntity sp) {
                Cafe c = Cafes.de(sp.getUuid());
                if (c != null && c.construyendo && c.esDuenio(sp)) return c.ataqueConstruccion(sp, pos);
            }
            return ActionResult.FAIL;
        });
        PlayerBlockBreakEvents.BEFORE.register((w, p, pos, st, be) -> !encerrado(p));
    }
}
