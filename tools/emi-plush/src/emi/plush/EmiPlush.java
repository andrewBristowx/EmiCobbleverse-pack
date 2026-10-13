package emi.plush;

import dev.mrshawn.pokeblocks.block.custom.PokedollBlock;
import dev.mrshawn.pokeblocks.block.entity.BlockEntityTypeRegistry;
import dev.mrshawn.pokeblocks.item.DollRarity;
import dev.mrshawn.pokeblocks.item.custom.PokedollBlockItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.class_1792;
import net.minecraft.class_2248;
import net.minecraft.class_2378;
import net.minecraft.class_2591;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

/**
 * Peluches propios de Emi, hechos con las clases de Pokeblocks (PokedollBlock / PokedollBlockEntity / GeckoLib).
 * Se usan nombres intermediary de Minecraft: class_2248 = Block, class_1792 = Item, class_2591 = BlockEntityType,
 * class_2960 = Identifier, class_7923 = Registries, class_2378 = Registry.
 */
public class EmiPlush implements ModInitializer {
    public static final String ID = "emi_plush";

    public static class_2248 MICHI;
    public static class_1792 MICHI_ITEM;
    public static class_2591<MichiBlockEntity> MICHI_BE;
    public static class_1792 TICKET_GASHA;
    public static class_2248 MAQUINA_GASHA;
    public static class_1792 MAQUINA_GASHA_ITEM;
    public static class_2248 MAQUINA_GASHA_TOP;
    public static class_2591<GashaMachineBlockEntity> MAQUINA_GASHA_BE;

    @Override
    public void onInitialize() {
        class_2960 id = class_2960.method_60655(ID, "michi_dramatico");
        MICHI = class_2378.method_10230(class_7923.field_41175, id, new PokedollBlock<MichiBlockEntity>(() -> MichiBlockEntity.class));
        MICHI_ITEM = class_2378.method_10230(class_7923.field_41178, id,
                new PokedollBlockItem(MICHI, DollRarity.NONE, 0, () -> new MichiItemModel()));
        MICHI_BE = class_2378.method_10230(class_7923.field_41181, id,
                FabricBlockEntityTypeBuilder.create(MichiBlockEntity::new, MICHI).build());
        BlockEntityTypeRegistry.register(MichiBlockEntity.class, MICHI_BE);
        TICKET_GASHA = class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(ID, "ticket_gasha_michi"),
                new GashaTicket(new class_1792.class_1793().method_7889(64).method_7894(net.minecraft.class_1814.field_8904)));
        class_2960 idMaquina = class_2960.method_60655(ID, "maquina_gasha_michi");
        MAQUINA_GASHA = class_2378.method_10230(class_7923.field_41175, idMaquina, new GashaMachineBlock(ajustesMaquina()));
        MAQUINA_GASHA_TOP = class_2378.method_10230(class_7923.field_41175, class_2960.method_60655(ID, "maquina_gasha_michi_arriba"), new GashaMachineTopBlock(ajustesMaquina()));
        MAQUINA_GASHA_ITEM = class_2378.method_10230(class_7923.field_41178, idMaquina,
                new net.minecraft.class_1747(MAQUINA_GASHA, new class_1792.class_1793().method_7894(net.minecraft.class_1814.field_8904)));
        MAQUINA_GASHA_BE = class_2378.method_10230(class_7923.field_41181, idMaquina,
                FabricBlockEntityTypeBuilder.create(GashaMachineBlockEntity::new, MAQUINA_GASHA).build());
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(GashaPeticion.ID, GashaPeticion.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(GashaRespuesta.ID, GashaRespuesta.CODEC);
        GashaServidor.registrar();
        emi.plush.gen.DollRegistry.registrar();
        EmiPlushTab.register();
    }

    private static net.minecraft.class_4970.class_2251 ajustesMaquina() {
        return net.minecraft.class_4970.class_2251.method_9637().method_9629(2.0f, 6.0f).method_9626(net.minecraft.class_2498.field_11544).method_22488();
    }
}
