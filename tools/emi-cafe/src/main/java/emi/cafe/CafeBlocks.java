package emi.cafe;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/** Los bloques del cafe: las estaciones de trabajo (con modelo y textura propios en {@code assets/emi_cafe}). Son bloques normales; lo que hacen lo decide {@link Cafe}. */
public final class CafeBlocks {
    private CafeBlocks() {}

    public static final Block CAJA_PAPAS = reg("caja_papas");
    public static final Block TABLA = reg("tabla_cortar");
    public static final Block FREIDORA = reg("freidora");
    public static final Block BASURA = reg("basura");
    public static final Block MESA = reg("mesa");
    public static final Block ENCIMERA = reg("encimera");

    private static Block reg(String id) {
        Block b = new Block(AbstractBlock.Settings.create().strength(1.5f, 6.0f).nonOpaque().sounds(BlockSoundGroup.WOOD));
        Identifier i = Identifier.of("emi_cafe", id);
        Registry.register(Registries.BLOCK, i, b);
        Registry.register(Registries.ITEM, i, new BlockItem(b, new Item.Settings()));
        return b;
    }

    static void init() { }
}
