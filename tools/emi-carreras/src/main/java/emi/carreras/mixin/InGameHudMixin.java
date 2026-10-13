package emi.carreras.mixin;

import emi.carreras.client.EmiCarrerasClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Durante la carrera se juega en tercera persona con la interfaz de Emi Carreras: no se dibujan la barra de objetos, la vida, el hambre ni la experiencia. */
@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = {"renderMainHud", "renderExperienceLevel"}, at = @At("HEAD"), cancellable = true)
    private void emi_carreras$ocultarBarras(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (EmiCarrerasClient.activo()) ci.cancel();
    }
}
