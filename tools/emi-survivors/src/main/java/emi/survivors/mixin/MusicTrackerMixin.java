package emi.survivors.mixin;

import emi.survivors.client.CamaraSurvivors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** En la dimension de Emi Survivors ({@code emi_survivors:*}) no suena la musica de fondo de Minecraft (la musica del minijuego la mandara el servidor). */
@Mixin(MusicTracker.class)
public abstract class MusicTrackerMixin {
    @Shadow private SoundInstance current;
    @Shadow public abstract void stop();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void emi_survivors$sinMusicaDeMinecraft(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean enPartida = CamaraSurvivors.activa() || (mc.world != null && "emi_survivors".equals(mc.world.getRegistryKey().getValue().getNamespace()));
        if (!enPartida) return;
        if (current != null) stop();
        ci.cancel();
    }
}
