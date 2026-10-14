package emi.huida.mixin;

import emi.huida.client.CamaraHuida;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Durante la huida no se dibujan las barras de jefe (bosses de Emipokemon, etc.). */
@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void emi_huida$ocultar(DrawContext context, CallbackInfo ci) {
        if (CamaraHuida.activa()) ci.cancel();
    }
}
