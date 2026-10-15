package emi.huida.mixin;

import emi.huida.client.CamaraHuida;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Durante la huida no se dibujan la barra de objetos, la vida, el hambre ni la experiencia (el jugador no tiene nada: solo corre). */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = {"renderMainHud", "renderExperienceLevel"}, at = @At("HEAD"), cancellable = true)
    private void emi_huida$ocultarBarras(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (CamaraHuida.activa()) ci.cancel();
    }
}
