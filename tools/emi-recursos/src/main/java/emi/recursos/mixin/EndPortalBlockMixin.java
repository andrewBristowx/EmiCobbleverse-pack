package emi.recursos.mixin;

import emi.recursos.Mundos;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.gen.feature.EndPlatformFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** El portal del End de un mundo de recursos lleva al End de recursos, y el portal de salida del End de recursos vuelve al overworld de recursos (no a tu cama). */
@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockMixin {
    @Inject(method = "createTeleportTarget", at = @At("HEAD"), cancellable = true)
    private void emi$recursos(ServerWorld world, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTarget> cir) {
        if (!Mundos.esRecursos(world)) return;
        boolean sale = world == Mundos.end();
        ServerWorld destino = sale ? Mundos.overworld() : Mundos.end();
        if (destino == null || world == Mundos.nether()) { cir.setReturnValue(null); return; }
        BlockPos base = sale ? Mundos.spawn() : ServerWorld.END_SPAWN_POS;
        Vec3d v = base.toBottomCenterPos();
        float giro = entity.getYaw();
        if (!sale) {
            EndPlatformFeature.generate(destino, BlockPos.ofFloored(v).down(), true);
            giro = Direction.WEST.asRotation();
            if (entity instanceof ServerPlayerEntity) v = v.subtract(0.0, 1.0, 0.0);
        }
        cir.setReturnValue(new TeleportTarget(destino, v, entity.getVelocity(), giro, entity.getPitch(),
                TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET.then(TeleportTarget.ADD_PORTAL_CHUNK_TICKET)));
    }
}
