package emi.huida.mixin;

import emi.huida.Corredor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cobblemon elige la animacion de andar/correr de un Pokemon mirando cuanto ha aumentado su distancia caminada ({@code horizontalSpeed}) en el tick.
 * Esa distancia solo sube cuando la entidad se mueve con la fisica ({@code move}), y el Tyrantrum de la huida se mueve por teletransporte (la fisica
 * lo frenaria contra los muros), asi que se quedaba siempre en la postura de quieto. Aqui se le suma esa distancia a mano, solo a el.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Corredor {
    @Unique private float emi_huida$paso;

    protected LivingEntityMixin(EntityType<?> type, World world) { super(type, world); }

    @Override public void emi_huida$setPaso(float paso) { this.emi_huida$paso = paso; }

    @Inject(method = "tick", at = @At("TAIL"))
    private void emi_huida$sumarPaso(CallbackInfo ci) {
        if (emi_huida$paso > 0f) this.horizontalSpeed += emi_huida$paso;
    }
}
