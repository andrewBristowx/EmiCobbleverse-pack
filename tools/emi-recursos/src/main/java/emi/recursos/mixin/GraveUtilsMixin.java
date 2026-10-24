package emi.recursos.mixin;

import emi.recursos.Tumbas;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Universal Graves crea la tumba donde muere el jugador. Si muere en un mundo de recursos (que se borra en cada reinicio) la tumba se hace en su punto
 * de cama del mundo normal. Solo se aplica si Universal Graves esta instalado (@Pseudo); los nombres van en intermediary porque asi esta el otro mod.
 */
@Pseudo
@Mixin(targets = "eu.pb4.graves.other.GraveUtils", remap = false)
public abstract class GraveUtilsMixin {
    @Inject(method = "createGrave(Lnet/minecraft/class_3222;Lnet/minecraft/class_3218;Lnet/minecraft/class_1282;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void emi$tumbaEnLaCama(ServerPlayerEntity player, ServerWorld world, DamageSource source, CallbackInfo ci) {
        if (Tumbas.crearEnLaCama(player, world, source)) ci.cancel();
    }
}
