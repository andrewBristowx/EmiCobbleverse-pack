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
    public static final Block CAJA_BAYAS = reg("caja_bayas");
    public static final Block CAJA_LECHUGA = reg("caja_lechuga");
    public static final Block EXPRIMIDOR = reg("exprimidor");
    public static final Block EMPLATAR = reg("emplatar");
    public static final Block FREGADERO = reg("fregadero");
    public static final Block REGISTRADORA = reg("caja_registradora");
    public static final Block CAJA_HUEVOS = reg("caja_huevos");
    public static final Block CAJA_HARINA = reg("caja_harina");
    public static final Block CAJA_CARNE = reg("caja_carne");
    public static final Block CAJA_LECHE = reg("caja_leche");
    public static final Block CAJA_GRANOS = reg("caja_granos");
    public static final Block CAJA_QUESO = reg("caja_queso");
    public static final Block PLANCHA = reg("plancha");
    public static final Block OLLA = reg("olla");
    public static final Block HORNO = reg("horno");
    public static final Block BATIDORA = reg("batidora");
    public static final Block CAFETERA = reg("cafetera");
    public static final Block CAJA_CALABAZA = reg("caja_calabaza");
    public static final Block CAJA_ARROZ = reg("caja_arroz");
    public static final Block HELADERA = reg("heladera");
    public static final Block WAFLERA = reg("waflera");

    /** El letrero de la puerta (2 casillas de ancho, medio bloque de alto) y el libro de recetas de la pared (1 casilla): imagenes sobre una placa fina, sin objeto. */
    public static final Block CARTEL = placa("cartel", net.minecraft.util.shape.VoxelShapes.cuboid(0, 0.25, 0, 1, 0.75, 1 / 16.0));
    public static final Block LIBRO_PARED = placa("libro_pared", net.minecraft.util.shape.VoxelShapes.cuboid(0, 0, 0, 1, 1, 1 / 16.0));

    private static Block placa(String id, net.minecraft.util.shape.VoxelShape forma) {
        Block b = new PlacaBlock(forma);
        Registry.register(Registries.BLOCK, Identifier.of("emi_cafe", id), b);
        return b;
    }

    private static Block reg(String id) {
        Block b = new Block(AbstractBlock.Settings.create().strength(1.5f, 6.0f).nonOpaque().sounds(BlockSoundGroup.WOOD));
        Identifier i = Identifier.of("emi_cafe", id);
        Registry.register(Registries.BLOCK, i, b);
        Registry.register(Registries.ITEM, i, new BlockItem(b, new Item.Settings()));
        return b;
    }

    static void init() { }
}
