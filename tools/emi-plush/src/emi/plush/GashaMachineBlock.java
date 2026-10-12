package emi.plush;

import com.mojang.serialization.MapCodec;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1657;
import net.minecraft.class_1750;
import net.minecraft.class_1937;
import net.minecraft.class_2237;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2383;
import net.minecraft.class_2464;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2689;
import net.minecraft.class_265;
import net.minecraft.class_2591;
import net.minecraft.class_3965;
import net.minecraft.class_4970;
import net.minecraft.class_5558;
import net.minecraft.class_1922;
import net.minecraft.class_3726;
import net.minecraft.class_1309;
import net.minecraft.class_1799;

/**
 * Máquina del Gasha Especial de Eventos: dos bloques de alto (este de abajo + {@link GashaMachineTopBlock}), con el modelo GeckoLib de la
 * máquina de Emipokemon. Clic derecho con un Ticket Gasha = 1 tirada (ver {@link GashaMachineBlockEntity}).
 * Nombres intermediary: class_2237 = BlockWithEntity, class_2383 = HorizontalFacingBlock (field_11177 = FACING), class_265 = VoxelShape.
 */
public class GashaMachineBlock extends class_2237 {
    public static final MapCodec<GashaMachineBlock> CODEC = class_4970.method_54094(GashaMachineBlock::new);
    static final class_265 FORMA = class_2248.method_9541(1, 0, 1, 15, 16, 15);

    public GashaMachineBlock(class_4970.class_2251 settings) {
        super(settings);
        method_9590(method_9564().method_11657(class_2383.field_11177, class_2350.field_11043));
    }

    @Override
    protected MapCodec<? extends class_2237> method_53969() {
        return CODEC;
    }

    @Override
    protected void method_9515(class_2689.class_2690<class_2248, class_2680> builder) {
        builder.method_11667(class_2383.field_11177);
    }

    @Override
    public class_2680 method_9605(class_1750 ctx) {
        class_2338 arriba = ctx.method_8037().method_10084();
        if (!ctx.method_8045().method_8320(arriba).method_45474()) return null;
        return method_9564().method_11657(class_2383.field_11177, ctx.method_8042().method_10153());
    }

    @Override
    public void method_9567(class_1937 world, class_2338 pos, class_2680 state, class_1309 placer, class_1799 stack) {
        super.method_9567(world, pos, state, placer, stack);
        world.method_8652(pos.method_10084(), EmiPlush.MAQUINA_GASHA_TOP.method_9564().method_11657(class_2383.field_11177, state.method_11654(class_2383.field_11177)), 3);
    }

    @Override
    protected void method_9536(class_2680 state, class_1937 world, class_2338 pos, class_2680 nuevo, boolean movido) {
        if (!state.method_27852(nuevo.method_26204())) {
            class_2338 arriba = pos.method_10084();
            if (world.method_8320(arriba).method_27852(EmiPlush.MAQUINA_GASHA_TOP)) world.method_8650(arriba, false);
        }
        super.method_9536(state, world, pos, nuevo, movido);
    }

    @Override
    protected class_1269 method_55766(class_2680 state, class_1937 world, class_2338 pos, class_1657 player, class_3965 hit) {
        if (world.method_8608()) return class_1269.field_5812;
        if (world.method_8321(pos) instanceof GashaMachineBlockEntity maquina) maquina.usar(player);
        return class_1269.field_5812;
    }

    @Override
    public class_2586 method_10123(class_2338 pos, class_2680 state) {
        return new GashaMachineBlockEntity(pos, state);
    }

    @Override
    public <T extends class_2586> class_5558<T> method_31645(class_1937 world, class_2680 state, class_2591<T> type) {
        return method_31618(type, EmiPlush.MAQUINA_GASHA_BE, GashaMachineBlockEntity::tick);
    }

    @Override
    protected class_2464 method_9604(class_2680 state) {
        return class_2464.field_11456;
    }

    @Override
    protected class_265 method_9530(class_2680 state, class_1922 world, class_2338 pos, class_3726 ctx) {
        return FORMA;
    }

    @Override
    protected class_265 method_9549(class_2680 state, class_1922 world, class_2338 pos, class_3726 ctx) {
        return FORMA;
    }
}
