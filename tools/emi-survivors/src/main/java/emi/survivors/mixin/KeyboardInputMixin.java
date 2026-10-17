package emi.survivors.mixin;

import emi.survivors.client.CamaraSurvivors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Con la camara cenital fija, W/A/S/D se mueven respecto a la PANTALLA: W = arriba (norte), S = abajo (sur), A = izquierda (oeste), D = derecha (este), y en diagonal
 * con dos teclas. Minecraft mueve al jugador hacia donde mira, asi que aqui se gira al jugador hacia la direccion pedida y se le dice «avanza».
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void emi_survivors$relativoAPantalla(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (!CamaraSurvivors.activa()) return;
        double derecha = -this.movementSideways, arriba = this.movementForward;      // en Minecraft la A da +1 de lateral (izquierda)
        if (derecha == 0 && arriba == 0) { this.movementForward = 0; this.movementSideways = 0; return; }
        double dx = derecha, dz = -arriba;                                             // este = +x, norte = -z
        CamaraSurvivors.rumbo = (float) Math.toDegrees(Math.atan2(-dx, dz));
        var pl = MinecraftClient.getInstance().player;
        if (pl != null) CamaraSurvivors.fijarMirada(pl);
        this.movementForward = 1f; this.movementSideways = 0f;
    }
}
