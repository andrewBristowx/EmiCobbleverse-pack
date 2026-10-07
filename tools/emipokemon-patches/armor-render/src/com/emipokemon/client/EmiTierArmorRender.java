package com.emipokemon.client;

import com.emipokemon.armor.EmiGeoArmorPiece;
import com.emipokemon.registry.ModRegistries;
import java.util.List;
import net.minecraft.class_1792;

/**
 * Registra el renderizador de GeckoLib para las piezas de armadura de los niveles Hierro, Oro y Esmeralda.
 * EmipokemonClient solo lo registraba para las 4 piezas base (EMI_HELMET...EMI_BOOTS); las demas son EmiGeoArmorItem
 * con el proveedor a null y GeckoLib lanza NullPointerException al dibujarlas (p. ej. la vista previa de la mesa de herreria).
 */
public final class EmiTierArmorRender {
    private EmiTierArmorRender() {}

    public static void register() {
        try {
            for (List<class_1792> set : List.of(ModRegistries.EMI_IRON_SET, ModRegistries.EMI_GOLD_SET, ModRegistries.EMI_EMERALD_SET)) {
                if (set == null) continue;
                for (class_1792 item : set) {
                    if (item instanceof EmiGeoArmorPiece) EmipokemonClient.registerEmiArmorRenderer(item);
                }
            }
        } catch (Throwable t) {
            System.err.println("[emi_armor] no se pudo registrar el renderizador de las armaduras de nivel: " + t);
        }
    }
}
