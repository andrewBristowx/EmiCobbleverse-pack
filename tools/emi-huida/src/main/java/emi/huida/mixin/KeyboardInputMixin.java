package emi.huida.mixin;

import emi.huida.client.CamaraHuida;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Con la camara de frente al jugador, A mueve hacia la izquierda de la PANTALLA (que es la derecha del jugador): se invierte el lateral. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void emi_huida$invertir(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (CamaraHuida.activa()) this.movementSideways = -this.movementSideways;
    }
}
