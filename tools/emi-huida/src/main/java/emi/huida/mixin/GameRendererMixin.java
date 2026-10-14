package emi.huida.mixin;

import emi.huida.client.CamaraHuida;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Durante la huida el campo de vision queda FIJO. Minecraft lo cambia segun la velocidad del jugador (media si esta quieto, mas ancho al correr):
 * mientras esperaba la confirmacion o la cuenta atras (jugador congelado, velocidad 0) la imagen salia con el doble de zoom, y al empezar a
 * correr se ensanchaba de golpe. 1,15 es lo que daba al ir corriendo.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow private float fovMultiplier;
    @Shadow private float lastFovMultiplier;

    @Inject(method = "updateFovMultiplier", at = @At("TAIL"))
    private void emi_huida$fovFijo(CallbackInfo ci) {
        if (CamaraHuida.activa()) { fovMultiplier = 1.15f; lastFovMultiplier = 1.15f; }
    }
}
