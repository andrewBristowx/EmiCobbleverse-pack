package emi.plush;

import dev.mrshawn.pokeblocks.block.client.PokedollBlockModel;
import dev.mrshawn.pokeblocks.block.entity.PokedollBlockEntity;
import net.minecraft.class_2960;

/** Modelo GeckoLib del bloque: busca el geo y la textura en el espacio de nombres emi_plush (la animacion generica es la de Pokeblocks). */
public class MichiBlockModel extends PokedollBlockModel {
    public MichiBlockModel() {
        super("michi_dramatico.geo.json", "michi_dramatico.png", "generic.animation.json");
    }

    @Override
    public class_2960 getModelResource(PokedollBlockEntity e) {
        return class_2960.method_60655(EmiPlush.ID, "geo/michi_dramatico.geo.json");
    }

    @Override
    public class_2960 getTextureResource(PokedollBlockEntity e) {
        return class_2960.method_60655(EmiPlush.ID, "textures/entity/michi_dramatico.png");
    }
}
