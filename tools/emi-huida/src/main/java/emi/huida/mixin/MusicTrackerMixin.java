package emi.huida.mixin;

import emi.huida.client.CamaraHuida;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * En las dimensiones de la huida ({@code emi_huida:*}: la pista, la cuenta atras, las escenas y la sala de espera) no suena la musica de fondo de
 * Minecraft, para que no se mezcle con la del nivel (que la manda el servidor). Si estaba sonando una cancion al entrar, se corta; el reloj de la
 * siguiente no avanza mientras estes ahi.
 */
@Mixin(MusicTracker.class)
public abstract class MusicTrackerMixin {
    private static boolean emi_huida$avisado;
    @Shadow private SoundInstance current;
    @Shadow public abstract void stop();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void emi_huida$sinMusicaDeMinecraft(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean enHuida = CamaraHuida.activa() || (mc.world != null && "emi_huida".equals(mc.world.getRegistryKey().getValue().getNamespace()));
        if (!enHuida) return;
        if (current != null) { stop(); if (Boolean.getBoolean("emi.huida.debug")) emi.huida.EmiHuida.LOG.info("Musica de Minecraft cortada (estas en la huida)"); }
        else if (!emi_huida$avisado && Boolean.getBoolean("emi.huida.debug")) { emi_huida$avisado = true; emi.huida.EmiHuida.LOG.info("Musica de Minecraft desactivada mientras estes en la huida"); }
        ci.cancel();
    }
}
