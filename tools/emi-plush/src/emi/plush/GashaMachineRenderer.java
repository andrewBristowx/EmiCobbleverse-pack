package emi.plush;

import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_4597;
import net.minecraft.class_5614;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GashaMachineRenderer extends GeoBlockRenderer<GashaMachineBlockEntity> {
    public GashaMachineRenderer(class_5614.class_5615 ctx) {
        super(new GashaMachineModel());
    }

    @Override
    public class_1921 getRenderType(GashaMachineBlockEntity e, class_2960 textura, class_4597 buffers, float parcial) {
        return class_1921.method_23580(textura);
    }

    @Override
    public boolean method_3563(GashaMachineBlockEntity e) {
        return true;
    }
}
