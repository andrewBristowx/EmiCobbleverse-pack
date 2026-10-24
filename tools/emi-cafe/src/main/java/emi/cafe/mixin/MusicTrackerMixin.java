package emi.cafe.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** En la dimension del cafe ({@code emi_cafe:*}) no suena la musica de fondo de Minecraft (la musica del cafe la manda el servidor). */
@Mixin(MusicTracker.class)
public abstract class MusicTrackerMixin {
    @Shadow private SoundInstance current;
    @Shadow public abstract void stop();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void emi_cafe$sinMusicaDeMinecraft(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || !"emi_cafe".equals(mc.world.getRegistryKey().getValue().getNamespace())) return;
        if (current != null) stop();
        ci.cancel();
    }
}
