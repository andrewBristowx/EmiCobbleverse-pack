package emi.plush;

import dev.mrshawn.pokeblocks.block.client.PokedollBlockModel;
import dev.mrshawn.pokeblocks.block.entity.PokedollBlockEntity;
import net.minecraft.class_2960;

/** Modelo GeckoLib del bloque de un peluche Emi: geo y textura propios en el espacio emi_plush (la animacion generica es la de Pokeblocks). */
public class EmiDollModel extends PokedollBlockModel {
    private final String nombre;

    public EmiDollModel(String nombre) {
        super(nombre + ".geo.json", nombre + ".png", "generic.animation.json");
        this.nombre = nombre;
    }

    @Override
    public class_2960 getModelResource(PokedollBlockEntity e) {
        return class_2960.method_60655(EmiPlush.ID, "geo/" + nombre + ".geo.json");
    }

    @Override
    public class_2960 getTextureResource(PokedollBlockEntity e) {
        return class_2960.method_60655(EmiPlush.ID, "textures/entity/" + nombre + ".png");
    }
}
