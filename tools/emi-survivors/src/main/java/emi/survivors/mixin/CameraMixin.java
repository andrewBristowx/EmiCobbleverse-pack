package emi.survivors.mixin;

import emi.survivors.client.CamaraSurvivors;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Durante una partida la camara la coloca {@link CamaraSurvivors} (cenital, fija, siguiendo al jugador). */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("RETURN"))
    private void emi_survivors$camara(BlockView area, Entity focused, boolean thirdPerson, boolean inverse, float tickDelta, CallbackInfo ci) {
        double[] c = CamaraSurvivors.calcular(tickDelta);
        if (c == null) return;
        setPos(c[0], c[1], c[2]);
        setRotation((float) c[3], (float) c[4]);
    }
}
