package emi.carreras.mixin;

import emi.carreras.client.EmiCarrerasClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Durante la carrera no se dibujan las barras de jefe (bosses de Emipokemon, etc.): tapan la interfaz de la carrera. */
@Environment(EnvType.CLIENT)
@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void emi_carreras$ocultar(DrawContext context, CallbackInfo ci) {
        if (EmiCarrerasClient.activo()) ci.cancel();
    }
}
