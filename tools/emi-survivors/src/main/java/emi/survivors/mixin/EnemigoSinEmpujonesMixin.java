package emi.survivors.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dentro de los mundos de Emi Survivors nadie se empuja: ni los Pokemon enemigos entre si, ni los enemigos al jugador, ni los jugadores entre si. En una horda de decenas
 * la busqueda de vecinos de cada tick ({@code tickCramming}) cuesta mucho y no hace falta (se solapan, como en el juego original), y sin empujones una horda (o un jefe
 * enorme) no arrastra al jugador hasta el muro. Se mira la DIMENSION y no solo la etiqueta {@code emi_survivors}, porque el cliente no conoce las etiquetas del servidor
 * y el empujon que mueve al jugador lo calcula el propio cliente.
 */
@Mixin(LivingEntity.class)
public abstract class EnemigoSinEmpujonesMixin extends Entity {
    protected EnemigoSinEmpujonesMixin(EntityType<?> type, World world) { super(type, world); }

    @Unique
    private boolean emi_survivors$enElMinijuego() {
        return this.getCommandTags().contains("emi_survivors") || "emi_survivors".equals(this.getWorld().getRegistryKey().getValue().getNamespace());
    }

    @Inject(method = "tickCramming", at = @At("HEAD"), cancellable = true)
    private void emi_survivors$sinApretujones(CallbackInfo ci) {
        if (emi_survivors$enElMinijuego()) ci.cancel();
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void emi_survivors$sinEmpujones(Entity other, CallbackInfo ci) {
        if (emi_survivors$enElMinijuego()) ci.cancel();
    }
}
