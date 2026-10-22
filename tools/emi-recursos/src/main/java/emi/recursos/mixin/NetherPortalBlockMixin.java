package emi.recursos.mixin;

import emi.recursos.Mundos;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** El portal del nether de un mundo de recursos lleva al otro mundo de recursos (overworld <-> nether), no a los de siempre. */
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {
    @Shadow
    protected abstract TeleportTarget getOrCreateExitPortalTarget(ServerWorld world, Entity entity, BlockPos pos, BlockPos scaledPos, boolean inNether, WorldBorder worldBorder);

    @Inject(method = "createTeleportTarget", at = @At("HEAD"), cancellable = true)
    private void emi$recursos(ServerWorld world, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTarget> cir) {
        if (!Mundos.esRecursos(world)) return;
        ServerWorld destino = world == Mundos.nether() ? Mundos.overworld() : Mundos.nether();
        if (destino == null || world == Mundos.end()) { cir.setReturnValue(null); return; }
        boolean enNether = destino == Mundos.nether();
        WorldBorder borde = destino.getWorldBorder();
        double escala = DimensionType.getCoordinateScaleFactor(world.getDimension(), destino.getDimension());
        BlockPos escalada = borde.clamp(entity.getX() * escala, entity.getY(), entity.getZ() * escala);
        cir.setReturnValue(this.getOrCreateExitPortalTarget(destino, entity, pos, escalada, enNether, borde));
    }
}
