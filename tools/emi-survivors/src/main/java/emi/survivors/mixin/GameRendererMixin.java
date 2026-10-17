package emi.survivors.mixin;

import emi.survivors.client.CamaraSurvivors;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Durante una partida el campo de vision queda FIJO (Minecraft lo ensancha al correr y el zoom cambiaria con cada paso). */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow private float fovMultiplier;
    @Shadow private float lastFovMultiplier;

    @Inject(method = "updateFovMultiplier", at = @At("TAIL"))
    private void emi_survivors$fovFijo(CallbackInfo ci) {
        if (CamaraSurvivors.activa()) { fovMultiplier = 1.0f; lastFovMultiplier = 1.0f; }
    }
}
