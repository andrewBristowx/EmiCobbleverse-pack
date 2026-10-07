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

    @Override
    public void onInitialize() {
        class_2960 id = class_2960.method_60655(ID, "michi_dramatico");
        MICHI = class_2378.method_10230(class_7923.field_41175, id, new PokedollBlock<MichiBlockEntity>(() -> MichiBlockEntity.class));
        MICHI_ITEM = class_2378.method_10230(class_7923.field_41178, id,
                new PokedollBlockItem(MICHI, DollRarity.NONE, 0, () -> new MichiItemModel()));
        MICHI_BE = class_2378.method_10230(class_7923.field_41181, id,
                FabricBlockEntityTypeBuilder.create(MichiBlockEntity::new, MICHI).build());
        BlockEntityTypeRegistry.register(MichiBlockEntity.class, MICHI_BE);
        EmiPlushTab.register();
    }
}
