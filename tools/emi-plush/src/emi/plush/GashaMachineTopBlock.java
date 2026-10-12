package emi.plush;

import net.minecraft.class_1922;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_1269;
import net.minecraft.class_2338;
import net.minecraft.class_2383;
import net.minecraft.class_2350;
import net.minecraft.class_2464;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_2689;
import net.minecraft.class_3726;
import net.minecraft.class_3965;
import net.minecraft.class_4970;

/** Mitad de arriba de la máquina del Gasha: invisible (el modelo lo dibuja el bloque de abajo); el clic se lo pasa a la máquina y al romperla se rompe la de abajo. */
public class GashaMachineTopBlock extends class_2248 {
    public GashaMachineTopBlock(class_4970.class_2251 settings) {
        super(settings);
        method_9590(method_9564().method_11657(class_2383.field_11177, class_2350.field_11043));
    }

    @Override
    protected void method_9515(class_2689.class_2690<class_2248, class_2680> builder) {
        builder.method_11667(class_2383.field_11177);
    }

    @Override
    protected class_1269 method_55766(class_2680 state, class_1937 world, class_2338 pos, class_1657 player, class_3965 hit) {
        if (world.method_8608()) return class_1269.field_5812;
        if (world.method_8321(pos.method_10074()) instanceof GashaMachineBlockEntity maquina) maquina.usar(player);
        return class_1269.field_5812;
    }

    @Override
    protected void method_9536(class_2680 state, class_1937 world, class_2338 pos, class_2680 nuevo, boolean movido) {
        if (!state.method_27852(nuevo.method_26204())) {
            class_2338 abajo = pos.method_10074();
            if (world.method_8320(abajo).method_27852(EmiPlush.MAQUINA_GASHA)) world.method_8651(abajo, true, null);
        }
        super.method_9536(state, world, pos, nuevo, movido);
    }

    @Override
    protected class_2464 method_9604(class_2680 state) {
        return class_2464.field_11455;
    }

    @Override
    protected class_265 method_9530(class_2680 state, class_1922 world, class_2338 pos, class_3726 ctx) {
        return GashaMachineBlock.FORMA;
    }

    @Override
    protected class_265 method_9549(class_2680 state, class_1922 world, class_2338 pos, class_3726 ctx) {
        return GashaMachineBlock.FORMA;
    }
}
