package emi.recursos.mixin;

import emi.recursos.Mundos;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SaveProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Al guardar, el juego apunta la pelea del dragon del mundo en el archivo del mundo principal: el End de recursos no debe pisar la del End de siempre. */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @Redirect(method = "saveLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/SaveProperties;setDragonFight(Lnet/minecraft/entity/boss/dragon/EnderDragonFight$Data;)V"))
    private void emi$noPisarElDragon(SaveProperties propiedades, EnderDragonFight.Data datos) {
        if (Mundos.esRecursos((ServerWorld) (Object) this)) return;
        propiedades.setDragonFight(datos);
    }
}
