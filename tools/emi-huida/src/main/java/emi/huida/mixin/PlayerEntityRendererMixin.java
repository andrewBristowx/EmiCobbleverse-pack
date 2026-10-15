package emi.huida.mixin;

import emi.huida.client.Efectos;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nivel 3: cuando la horda te pasa por encima, el jugador se dibuja aplastado (muy bajo y ancho, como una tortita). Solo el propio jugador. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "scale(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("TAIL"))
    private void emi_huida$aplastar(AbstractClientPlayerEntity jugador, MatrixStack m, float f, CallbackInfo ci) {
        float a = Efectos.aplastado();
        if (a > 0f && jugador == MinecraftClient.getInstance().player) m.scale(1f + 0.5f * a, Math.max(0.06f, 1f - 0.94f * a), 1f + 0.5f * a);
    }
}
