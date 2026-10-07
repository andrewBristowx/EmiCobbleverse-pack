package emi.plush;

import dev.mrshawn.pokeblocks.item.client.PokedollBlockItemModel;
import dev.mrshawn.pokeblocks.item.custom.PokedollBlockItem;
import net.minecraft.class_2960;

/** Igual que MichiBlockModel pero para el objeto en la mano / inventario. */
public class MichiItemModel extends PokedollBlockItemModel {
    public MichiItemModel() {
        super("michi_dramatico.geo.json", "michi_dramatico.png", "generic.animation.json");
    }

    @Override
    public class_2960 getModelResource(PokedollBlockItem i) {
        return class_2960.method_60655(EmiPlush.ID, "geo/michi_dramatico.geo.json");
    }

    @Override
    public class_2960 getTextureResource(PokedollBlockItem i) {
        return class_2960.method_60655(EmiPlush.ID, "textures/entity/michi_dramatico.png");
    }
}
