package emi.recursos.mixin;

import emi.recursos.Mundos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dormir en una cama (o usar un ancla de reaparicion) de un mundo de recursos no cambia el punto de reaparicion: esos mundos se borran. */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "setSpawnPoint", at = @At("HEAD"), cancellable = true)
    private void emi$sinCamaEnRecursos(RegistryKey<World> dimension, BlockPos pos, float angle, boolean forced, boolean sendMessage, CallbackInfo ci) {
        if (pos != null && Mundos.esRecursos(dimension)) ci.cancel();
    }
}
