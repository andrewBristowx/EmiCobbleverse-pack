package emi.survivors.mixin;

import emi.survivors.client.CamaraSurvivors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Compatibilidad con Better Third Person (va en el pack). Ese mod, en tercera persona, REESCRIBE las teclas de movimiento: convierte A/D/S en
 * «girar al jugador hacia esa direccion y avanzar» respecto a SU camara, asi que en una partida no dejaba moverse a los lados. Mientras dura la
 * huida se le apaga su camara propia (nuestra camara y nuestras teclas mandan). Si el mod no esta instalado, este mixin se ignora (@Pseudo).
 */
@Pseudo
@Mixin(targets = "io.socol.betterthirdperson.api.CustomCameraManager", remap = false)
public abstract class BetterThirdPersonMixin {
    @Inject(method = "hasCustomCamera", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void emi_survivors$sinCamaraPropia(CallbackInfoReturnable<Boolean> cir) {
        if (CamaraSurvivors.activa()) cir.setReturnValue(false);
    }

    @Inject(method = "mustHaveCustomCamera", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void emi_survivors$nuncaCamaraPropia(CallbackInfoReturnable<Boolean> cir) {
        if (CamaraSurvivors.activa()) cir.setReturnValue(false);
    }
}
