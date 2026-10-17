package emi.survivors.mixin;

import emi.survivors.client.CamaraSurvivors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Durante una partida no se dibujan la barra de objetos, la vida, el hambre, la experiencia ni la mira de Minecraft (la interfaz es la del minijuego). */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = {"renderMainHud", "renderExperienceLevel", "renderCrosshair"}, at = @At("HEAD"), cancellable = true)
    private void emi_survivors$ocultar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (CamaraSurvivors.activa()) ci.cancel();
    }
}
