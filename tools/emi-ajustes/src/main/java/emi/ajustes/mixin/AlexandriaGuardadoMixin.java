package emi.ajustes.mixin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Alexandria (la libreria de Pickable Villagers) engancha el guardado de CADA dimension (el evento SAVE de Architectury, una vez por mundo) y en cada una escribe
 * entero el archivo de aldeanos recogidos ({@code world/data/villagers.shift}: {@code CachedEntities.saveAll} no mira de que mundo viene). Con unas 18 dimensiones
 * (las de los minijuegos de Emi incluidas) cada autoguardado reescribia ese archivo 18 veces en el hilo principal: en un informe de spark, el 56 % del tiempo de los
 * ticks lentos. Aqui se salta el guardado en todo mundo que no sea el Overworld (el archivo es uno solo para todo el servidor, asi que con una vez basta).
 * Es un mixin sobre la funcion anonima {@code lambda$init$1} de Alexandria 1.0.3.3.2: si otra version la cambia de nombre, no se aplica y no pasa nada (require = 0).
 */
@Mixin(targets = "com.shiftthedev.alexandria.Alexandria", remap = false)
public abstract class AlexandriaGuardadoMixin {
    @Inject(method = "lambda$init$1", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void emi_ajustes$soloOverworld(ServerWorld mundo, CallbackInfo ci) {
        boolean omitir = mundo.getRegistryKey() != World.OVERWORLD;
        if (DEBUG) System.out.println("[emi-ajustes] guardado de aldeanos de Alexandria en " + mundo.getRegistryKey().getValue() + ": " + (omitir ? "omitido" : "se hace"));
        if (omitir) ci.cancel();
    }

    /** {@code -Demi.ajustes.debug=true} escribe en la consola cada vez que se omite o se hace el guardado (para comprobarlo). */
    private static final boolean DEBUG = Boolean.getBoolean("emi.ajustes.debug");
}
