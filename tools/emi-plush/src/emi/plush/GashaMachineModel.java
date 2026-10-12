package emi.plush;

import net.minecraft.class_2960;
import software.bernie.geckolib.model.GeoModel;

/** Geometría y animaciones: las de la máquina Emi de Emipokemon; la textura es propia (michialien). */
public class GashaMachineModel extends GeoModel<GashaMachineBlockEntity> {
    @Override
    public class_2960 getModelResource(GashaMachineBlockEntity e) {
        return class_2960.method_60655("emipokemon", "geo/emi_gacha_machine.geo.json");
    }

    @Override
    public class_2960 getTextureResource(GashaMachineBlockEntity e) {
        return class_2960.method_60655(EmiPlush.ID, "textures/block/maquina_gasha_michi.png");
    }

    @Override
    public class_2960 getAnimationResource(GashaMachineBlockEntity e) {
        return class_2960.method_60655("emipokemon", "animations/standard_gacha_machine.animation.json");
    }
}
