package emi.recursos.mixin;

import emi.recursos.Mundos;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** El fuego solo enciende portales del nether en el overworld y el nether de siempre: tambien en los de recursos. */
@Mixin(AbstractFireBlock.class)
public abstract class AbstractFireBlockMixin {
    @Inject(method = "isOverworldOrNether", at = @At("HEAD"), cancellable = true)
    private static void emi$recursos(World world, CallbackInfoReturnable<Boolean> cir) {
        if (Mundos.esRecursos(world) && (world == Mundos.overworld() || world == Mundos.nether())) cir.setReturnValue(true);
    }
}
