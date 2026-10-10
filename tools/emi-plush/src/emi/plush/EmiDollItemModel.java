package emi.plush;

import dev.mrshawn.pokeblocks.item.client.PokedollBlockItemModel;
import dev.mrshawn.pokeblocks.item.custom.PokedollBlockItem;
import net.minecraft.class_2960;

/** Igual que EmiDollModel pero para el objeto en la mano / inventario. */
public class EmiDollItemModel extends PokedollBlockItemModel {
    private final String nombre;

    public EmiDollItemModel(String nombre) {
        super(nombre + ".geo.json", nombre + ".png", "generic.animation.json");
        this.nombre = nombre;
    }

    @Override
    public class_2960 getModelResource(PokedollBlockItem i) {
        return class_2960.method_60655(EmiPlush.ID, "geo/" + nombre + ".geo.json");
    }

    @Override
    public class_2960 getTextureResource(PokedollBlockItem i) {
        return class_2960.method_60655(EmiPlush.ID, "textures/entity/" + nombre + ".png");
    }
}
