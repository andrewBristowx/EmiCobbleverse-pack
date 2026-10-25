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
    // local de nivel 6 a 10 (0.20.0)
    public static final Block CAJA_TOMATE = reg("caja_tomate");
    public static final Block CAJA_PESCADO = reg("caja_pescado");
    public static final Block CAJA_CACAO = reg("caja_cacao");
    public static final Block CAJA_MIEL = reg("caja_miel");
    public static final Block PARRILLA = reg("parrilla");
    public static final Block CREPERA = reg("crepera");
    public static final Block VAPORERA = reg("vaporera");
    public static final Block FUENTE_CHOCOLATE = reg("fuente_chocolate");
    public static final Block CAJA_CEBOLLA = reg("caja_cebolla");                     // la granja (0.24.0)

    /** El letrero de la puerta (2 casillas de ancho, medio bloque de alto) y el libro de recetas de la pared (1 casilla): imagenes sobre una placa fina, sin objeto. */
    public static final Block CARTEL = placa("cartel", net.minecraft.util.shape.VoxelShapes.cuboid(0, 0.25, 0, 1, 0.75, 1 / 16.0));
    public static final Block LIBRO_PARED = placa("libro_pared", net.minecraft.util.shape.VoxelShapes.cuboid(0, 0, 0, 1, 1, 1 / 16.0));

    /** Las puertas de cristal de colores (0.18.2): 2 estilos (marco de madera y blanco) por 12 colores, puertas de verdad de 2 bloques que ademas se abren solas con quien se acerca. */
    public static final java.util.List<Block> PUERTAS = new java.util.ArrayList<>();
    static final String[] COLORES_PUERTA = {"white", "pink", "magenta", "purple", "blue", "light_blue", "cyan", "lime", "yellow", "orange", "red", "black"};
    static final String[] ESTILOS_PUERTA = {"madera", "blanca"};
    static {
        for (String e : ESTILOS_PUERTA) for (String c : COLORES_PUERTA) {
            Block b = new PuertaCristalBlock();
            Identifier i = Identifier.of("emi_cafe", "puerta_" + e + "_" + c);
            Registry.register(Registries.BLOCK, i, b);
            Registry.register(Registries.ITEM, i, new net.minecraft.item.TallBlockItem(b, new Item.Settings()));
            PUERTAS.add(b);
        }
    }

    /** Los 8 diseños de mesa (0.19.0): mesas de verdad, que cuentan igual que la mesa de siempre ({@link #esMesa}). */
    public static final String[] DISENOS_MESA = {"azul", "verde", "rosa", "negra", "blanca", "madera", "piedra", "dorada"};
    public static final java.util.List<Block> MESAS_DISENO = new java.util.ArrayList<>();
    /** Las vitrinas de cristal de colores (0.19.0), una por color. */
    public static final java.util.List<Block> VITRINAS = new java.util.ArrayList<>();
    static {
        for (String d : DISENOS_MESA) MESAS_DISENO.add(reg("mesa_" + d));
        for (String c : COLORES_PUERTA) {
            Block b = new VitrinaBlock();
            Identifier i = Identifier.of("emi_cafe", "vitrina_" + c);
            Registry.register(Registries.BLOCK, i, b);
            Registry.register(Registries.ITEM, i, new BlockItem(b, new Item.Settings()));
            VITRINAS.add(b);
        }
    }

    /** ¿Es una mesa del cafe (la de siempre o uno de los diseños)? */
    public static boolean esMesa(Block b) { return b == MESA || MESAS_DISENO.contains(b); }

    private static Block placa(String id, net.minecraft.util.shape.VoxelShape forma) {
        Block b = new PlacaBlock(forma);
        Registry.register(Registries.BLOCK, Identifier.of("emi_cafe", id), b);
        Registry.register(Registries.ITEM, Identifier.of("emi_cafe", id), new BlockItem(b, new Item.Settings()));       // desde 0.18.0 se pueden comprar, colocar y quitar
        return b;
    }

    private static Block reg(String id) {
        Block b = new Block(AbstractBlock.Settings.create().strength(1.5f, 6.0f).nonOpaque().sounds(BlockSoundGroup.WOOD));
        Identifier i = Identifier.of("emi_cafe", id);
        Registry.register(Registries.BLOCK, i, b);
        Registry.register(Registries.ITEM, i, new BlockItem(b, new Item.Settings()));
        return b;
    }

    /** Las despensas (0.27.0): una por cada cultivo de la granja, para poner en el café; de ellas sacan los ingredientes los michialiens (ver {@link Granja}). */
    public static final java.util.Map<String, Block> DESPENSAS = new java.util.LinkedHashMap<>();
    static {
        for (String id : new String[]{"papas", "trigo", "col", "tomates", "cebollas", "calabazas", "bayas", "arroz", "cacao", "granos", "vacas", "gallinas", "tauros", "pescado", "queso", "abejas"})
            DESPENSAS.put(id, reg("despensa_" + id));
    }

    static void init() { }
}
