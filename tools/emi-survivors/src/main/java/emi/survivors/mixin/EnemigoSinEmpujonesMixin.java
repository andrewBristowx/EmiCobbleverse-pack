package emi.survivors.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Los Pokemon enemigos de Emi Survivors (los que llevan la etiqueta {@code emi_survivors}) no se empujan entre si ni al jugador: en una horda de decenas, la
 * busqueda de vecinos de cada tick ({@code tickCramming}) cuesta mucho y no hace falta (se solapan, como en el juego original).
 */
@Mixin(LivingEntity.class)
public abstract class EnemigoSinEmpujonesMixin extends Entity {
    protected EnemigoSinEmpujonesMixin(EntityType<?> type, World world) { super(type, world); }

    @Inject(method = "tickCramming", at = @At("HEAD"), cancellable = true)
    private void emi_survivors$sinApretujones(CallbackInfo ci) {
        if (this.getCommandTags().contains("emi_survivors")) ci.cancel();
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void emi_survivors$sinEmpujones(Entity other, CallbackInfo ci) {
        if (this.getCommandTags().contains("emi_survivors")) ci.cancel();
    }
}
