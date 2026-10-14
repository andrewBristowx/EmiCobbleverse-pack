package emi.huida.mixin;

import emi.huida.client.CamaraHuida;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Durante la huida la camara la coloca {@link CamaraHuida} (detras del Tyrantrum, elevada, mirando al jugador y al camino). */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("RETURN"))
    private void emi_huida$camara(BlockView area, Entity focused, boolean thirdPerson, boolean inverse, float tickDelta, CallbackInfo ci) {
        double[] c = CamaraHuida.calcular(tickDelta);
        if (c == null) return;
        setPos(c[0], c[1], c[2]);
        setRotation((float) c[3], (float) c[4]);
    }
}
