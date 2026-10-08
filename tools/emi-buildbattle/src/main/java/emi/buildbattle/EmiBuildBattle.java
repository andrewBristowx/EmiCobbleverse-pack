package emi.buildbattle;

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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmiBuildBattle implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("emi_buildbattle");

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Cmds.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(Game::init);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> Game.shutdown());
        ServerTickEvents.END_SERVER_TICK.register(Game::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Game.onJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Game.onDisconnect(handler.player));

        // el NPC constructor abre el menu de bloques
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!Game.isNpc(entity)) return ActionResult.PASS;
            if (world.isClient) return ActionResult.SUCCESS;
            if (hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity sp && Game.isBuilding(sp)) Menu.open(sp);
            return ActionResult.SUCCESS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> Game.isNpc(entity) ? ActionResult.FAIL : ActionResult.PASS);

        // limites de la parcela
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> Game.allowBuildAt(player, pos));
        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) ->
                Game.allowBuildAt(player, pos) ? ActionResult.PASS : ActionResult.FAIL);
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!Game.isParticipant(player)) return ActionResult.PASS;
            ItemStack st = player.getStackInHand(hand);
            int score = Game.voteScore(st);
            if (score > 0) {
                if (!world.isClient && player instanceof ServerPlayerEntity sp) Game.onVote(sp, score);
                return ActionResult.FAIL;
            }
            if (Game.phase() != Game.Phase.BUILDING) return ActionResult.FAIL;
            BlockPos target = st.isEmpty() ? hit.getBlockPos() : hit.getBlockPos().offset(hit.getSide());
            return Game.allowBuildAt(player, target) ? ActionResult.PASS : ActionResult.FAIL;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack st = player.getStackInHand(hand);
            if (!Game.isParticipant(player)) return TypedActionResult.pass(st);
            int score = Game.voteScore(st);
            if (score > 0) {
                if (!world.isClient && player instanceof ServerPlayerEntity sp) Game.onVote(sp, score);
                return TypedActionResult.fail(st);
            }
            if (Game.phase() != Game.Phase.BUILDING) return TypedActionResult.fail(st);
            if (st.getItem() instanceof BucketItem) {
                HitResult hr = player.raycast(5.0, 0f, true);
                if (hr instanceof BlockHitResult bhr && hr.getType() == HitResult.Type.BLOCK) {
                    if (!Game.allowBuildAt(player, bhr.getBlockPos().offset(bhr.getSide())) && !Game.allowBuildAt(player, bhr.getBlockPos()))
                        return TypedActionResult.fail(st);
                }
            }
            return TypedActionResult.pass(st);
        });
    }
}
