package emi.survivors.mixin;

import emi.survivors.client.CamaraSurvivors;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Durante una partida de Emi Survivors no suena el «oof» de Minecraft cuando te hacen daño ({@code entity.player.hurt...}): el jugador va transformado en su Pokemon y el servidor ya
 * pone su propio sonido de golpe (ver {@code Sonidos.GOLPE}). Los demas sonidos de Minecraft no se tocan.
 */
@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void emi_survivors$sinOofDeMinecraft(SoundInstance sound, CallbackInfo ci) {
        if (!CamaraSurvivors.activa()) return;
        var id = sound.getId();
        if ("minecraft".equals(id.getNamespace()) && id.getPath().startsWith("entity.player.hurt")) {
            if (Boolean.getBoolean("emi.survivors.debug")) emi.survivors.EmiSurvivors.LOG.info("[sonido] quitado el sonido de golpe de Minecraft ({})", id);
            ci.cancel();
        }
    }
}
