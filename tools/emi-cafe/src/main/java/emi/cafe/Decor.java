package emi.cafe;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * La tienda de decoracion del modo construccion: bloques sin «entidad de bloque» (nada con inventario ni texto) para que se puedan guardar como un simple estado de bloque
 * ({@link #guardar}/{@link #leer}). {@code cantidad} es el paquete que se entrega por el precio (en monedas del cafe).
 */
final class Decor {
    private Decor() {}

    record Articulo(String id, String item, int cantidad, int precio, String grupo, String nombre) {
        Item objeto() { return Registries.ITEM.get(Identifier.of(item)); }
    }

    static final int MAX_BLOQUES = 12000;

    /** Los articulos de siempre (0.8.0-0.19.0): mesas, madera, color, cristal, luces, Halloween, puertas, vitrinas... */
    private static final List<Articulo> BASE = List.of(
            new Articulo("mesa", "emi_cafe:mesa", 1, 60, "Café", "Mesa del café"),
            new Articulo("mesa_azul", "emi_cafe:mesa_azul", 1, 70, "Mesas", "Mesa azul"),
            new Articulo("mesa_verde", "emi_cafe:mesa_verde", 1, 70, "Mesas", "Mesa verde"),
            new Articulo("mesa_rosa", "emi_cafe:mesa_rosa", 1, 70, "Mesas", "Mesa rosa"),
            new Articulo("mesa_negra", "emi_cafe:mesa_negra", 1, 110, "Mesas", "Mesa elegante negra"),
            new Articulo("mesa_blanca", "emi_cafe:mesa_blanca", 1, 80, "Mesas", "Mesa blanca"),
            new Articulo("mesa_madera", "emi_cafe:mesa_madera", 1, 70, "Mesas", "Mesa de madera"),
            new Articulo("mesa_piedra", "emi_cafe:mesa_piedra", 1, 90, "Mesas", "Mesa de piedra"),
            new Articulo("mesa_dorada", "emi_cafe:mesa_dorada", 1, 150, "Mesas", "Mesa dorada"),
            new Articulo("mesa_terraza", "emi_cafe:mesa_terraza", 1, 80, "Mesas", "Mesa de terraza"),
            new Articulo("encimera", "emi_cafe:encimera", 8, 40, "Café", "Encimeras"),
            new Articulo("roble", "minecraft:oak_planks", 16, 30, "Madera", "Tablones de roble"),
            new Articulo("abeto", "minecraft:spruce_planks", 16, 30, "Madera", "Tablones de abeto"),
            new Articulo("abedul", "minecraft:birch_planks", 16, 30, "Madera", "Tablones de abedul"),
            new Articulo("cerezo", "minecraft:cherry_planks", 16, 40, "Madera", "Tablones de cerezo"),
            new Articulo("roble_oscuro", "minecraft:dark_oak_planks", 16, 30, "Madera", "Tablones de roble oscuro"),
            new Articulo("roble_losa", "minecraft:oak_slab", 8, 18, "Madera", "Losas de roble"),
            new Articulo("roble_escalera", "minecraft:oak_stairs", 8, 24, "Madera", "Escaleras de roble"),
            new Articulo("valla", "minecraft:oak_fence", 8, 20, "Madera", "Vallas de roble"),
            new Articulo("estanteria", "minecraft:bookshelf", 4, 60, "Madera", "Estanterías"),
            new Articulo("hormigon_blanco", "minecraft:white_concrete", 16, 35, "Color", "Hormigón blanco"),
            new Articulo("hormigon_rosa", "minecraft:pink_concrete", 16, 35, "Color", "Hormigón rosa"),
            new Articulo("hormigon_celeste", "minecraft:light_blue_concrete", 16, 35, "Color", "Hormigón celeste"),
            new Articulo("hormigon_lima", "minecraft:lime_concrete", 16, 35, "Color", "Hormigón lima"),
            new Articulo("hormigon_amarillo", "minecraft:yellow_concrete", 16, 35, "Color", "Hormigón amarillo"),
            new Articulo("hormigon_negro", "minecraft:black_concrete", 16, 35, "Color", "Hormigón negro"),
            new Articulo("lana_blanca", "minecraft:white_wool", 16, 28, "Color", "Lana blanca"),
            new Articulo("lana_rosa", "minecraft:pink_wool", 16, 28, "Color", "Lana rosa"),
            new Articulo("lana_roja", "minecraft:red_wool", 16, 28, "Color", "Lana roja"),
            new Articulo("terracota", "minecraft:terracotta", 16, 30, "Color", "Terracota"),
            new Articulo("cristal", "minecraft:glass", 16, 30, "Cristal", "Cristal"),
            new Articulo("panel", "minecraft:glass_pane", 16, 20, "Cristal", "Paneles de cristal"),
            new Articulo("cristal_rosa", "minecraft:pink_stained_glass", 16, 36, "Cristal", "Cristal rosa"),
            new Articulo("ladrillo_piedra", "minecraft:stone_bricks", 16, 32, "Piedra", "Ladrillos de piedra"),
            new Articulo("ladrillo", "minecraft:bricks", 16, 40, "Piedra", "Ladrillos"),
            new Articulo("cuarzo", "minecraft:quartz_block", 16, 60, "Piedra", "Bloques de cuarzo"),
            new Articulo("piedra_lisa", "minecraft:smooth_stone", 16, 30, "Piedra", "Piedra lisa"),
            new Articulo("arenisca", "minecraft:smooth_sandstone", 16, 30, "Piedra", "Arenisca lisa"),
            new Articulo("alfombra_blanca", "minecraft:white_carpet", 16, 20, "Alfombras", "Alfombras blancas"),
            new Articulo("alfombra_rosa", "minecraft:pink_carpet", 16, 20, "Alfombras", "Alfombras rosas"),
            new Articulo("alfombra_roja", "minecraft:red_carpet", 16, 20, "Alfombras", "Alfombras rojas"),
            new Articulo("alfombra_azul", "minecraft:light_blue_carpet", 16, 20, "Alfombras", "Alfombras celestes"),
            new Articulo("alfombra_amarilla", "minecraft:yellow_carpet", 16, 20, "Alfombras", "Alfombras amarillas"),
            new Articulo("linterna", "minecraft:lantern", 4, 36, "Luces", "Linternas"),
            new Articulo("antorcha", "minecraft:torch", 8, 16, "Luces", "Antorchas"),
            new Articulo("linterna_marina", "minecraft:sea_lantern", 4, 50, "Luces", "Linternas marinas"),
            new Articulo("piedra_luminosa", "minecraft:glowstone", 4, 50, "Luces", "Piedra luminosa"),
            new Articulo("calabaza", "minecraft:jack_o_lantern", 4, 40, "Luces", "Calabazas con luz"),
            new Articulo("vela_rosa", "minecraft:pink_candle", 4, 24, "Luces", "Velas rosas"),
            new Articulo("vela_blanca", "minecraft:white_candle", 4, 24, "Luces", "Velas blancas"),
            new Articulo("hojas", "minecraft:oak_leaves", 16, 24, "Plantas", "Hojas de roble"),
            new Articulo("azalea", "minecraft:flowering_azalea_leaves", 16, 36, "Plantas", "Hojas de azalea"),
            new Articulo("musgo", "minecraft:moss_block", 8, 30, "Plantas", "Musgo"),
            new Articulo("paja", "minecraft:hay_block", 4, 24, "Plantas", "Fardos de paja"),
            new Articulo("calabaza_tallada", "minecraft:carved_pumpkin", 4, 40, "Halloween", "Calabazas talladas"),
            new Articulo("calabaza_entera", "minecraft:pumpkin", 4, 30, "Halloween", "Calabazas"),
            new Articulo("telarana", "minecraft:cobweb", 8, 30, "Halloween", "Telarañas"),
            new Articulo("vela_negra", "minecraft:black_candle", 4, 24, "Halloween", "Velas negras"),
            new Articulo("vela_morada", "minecraft:purple_candle", 4, 24, "Halloween", "Velas moradas"),
            new Articulo("vela_naranja", "minecraft:orange_candle", 4, 24, "Halloween", "Velas naranjas"),
            new Articulo("linterna_alma", "minecraft:soul_lantern", 4, 40, "Halloween", "Linternas de almas"),
            new Articulo("antorcha_alma", "minecraft:soul_torch", 8, 20, "Halloween", "Antorchas de almas"),
            new Articulo("hormigon_naranja", "minecraft:orange_concrete", 16, 40, "Halloween", "Hormigón naranja"),
            new Articulo("hormigon_negro", "minecraft:black_concrete", 16, 40, "Halloween", "Hormigón negro"),
            new Articulo("hormigon_morado", "minecraft:purple_concrete", 16, 40, "Halloween", "Hormigón morado"),
            new Articulo("ladrillo_negro", "minecraft:polished_blackstone_bricks", 16, 36, "Halloween", "Ladrillos de piedra negra"),
            new Articulo("hueso", "minecraft:bone_block", 8, 36, "Halloween", "Bloques de hueso"),
            new Articulo("obsidiana_llorona", "minecraft:crying_obsidian", 4, 60, "Halloween", "Obsidiana llorona"),
            new Articulo("fungiluz", "minecraft:shroomlight", 4, 50, "Halloween", "Hongoluz"),
            new Articulo("alfombra_naranja", "minecraft:orange_carpet", 16, 20, "Halloween", "Alfombras naranjas"),
            new Articulo("alfombra_negra", "minecraft:black_carpet", 16, 20, "Halloween", "Alfombras negras"),
            new Articulo("vidrio_morado", "minecraft:purple_stained_glass", 8, 30, "Halloween", "Cristal morado"),
            // para construir de verdad (0.18.0): estructura, escaleras (los clientes y los michis las suben) y losas
            new Articulo("tronco_roble", "minecraft:oak_log", 8, 26, "Estructura", "Troncos de roble"),
            new Articulo("tronco_pelado", "minecraft:stripped_oak_log", 8, 28, "Estructura", "Troncos de roble pelados"),
            new Articulo("tronco_abeto", "minecraft:spruce_log", 8, 26, "Estructura", "Troncos de abeto"),
            new Articulo("piedra", "minecraft:stone", 16, 24, "Estructura", "Piedra"),
            new Articulo("adoquin", "minecraft:cobblestone", 16, 20, "Estructura", "Adoquín"),
            new Articulo("andesita_pulida", "minecraft:polished_andesite", 16, 30, "Estructura", "Andesita pulida"),
            new Articulo("pizarra_ladrillos", "minecraft:deepslate_bricks", 16, 36, "Estructura", "Ladrillos de pizarra"),
            new Articulo("barrotes", "minecraft:iron_bars", 8, 28, "Estructura", "Barrotes de hierro"),
            new Articulo("abeto_escalera", "minecraft:spruce_stairs", 8, 24, "Escaleras", "Escaleras de abeto"),
            new Articulo("abedul_escalera", "minecraft:birch_stairs", 8, 24, "Escaleras", "Escaleras de abedul"),
            new Articulo("cerezo_escalera", "minecraft:cherry_stairs", 8, 30, "Escaleras", "Escaleras de cerezo"),
            new Articulo("roble_oscuro_escalera", "minecraft:dark_oak_stairs", 8, 24, "Escaleras", "Escaleras de roble oscuro"),
            new Articulo("ladrillo_piedra_escalera", "minecraft:stone_brick_stairs", 8, 26, "Escaleras", "Escaleras de ladrillo de piedra"),
            new Articulo("ladrillo_escalera", "minecraft:brick_stairs", 8, 30, "Escaleras", "Escaleras de ladrillo"),
            new Articulo("cuarzo_escalera", "minecraft:quartz_stairs", 8, 44, "Escaleras", "Escaleras de cuarzo"),
            new Articulo("arenisca_escalera", "minecraft:sandstone_stairs", 8, 24, "Escaleras", "Escaleras de arenisca"),
            new Articulo("abeto_losa", "minecraft:spruce_slab", 8, 18, "Losas", "Losas de abeto"),
            new Articulo("abedul_losa", "minecraft:birch_slab", 8, 18, "Losas", "Losas de abedul"),
            new Articulo("cerezo_losa", "minecraft:cherry_slab", 8, 22, "Losas", "Losas de cerezo"),
            new Articulo("roble_oscuro_losa", "minecraft:dark_oak_slab", 8, 18, "Losas", "Losas de roble oscuro"),
            new Articulo("ladrillo_piedra_losa", "minecraft:stone_brick_slab", 8, 20, "Losas", "Losas de ladrillo de piedra"),
            new Articulo("ladrillo_losa", "minecraft:brick_slab", 8, 22, "Losas", "Losas de ladrillo"),
            new Articulo("cuarzo_losa", "minecraft:quartz_slab", 8, 32, "Losas", "Losas de cuarzo"),
            new Articulo("piedra_lisa_losa", "minecraft:smooth_stone_slab", 8, 20, "Losas", "Losas de piedra lisa"),
            // puertas de cristal de colores (0.18.2): de verdad, de 2 bloques; se abren solas con quien se acerca y suena una campanilla
            new Articulo("puerta_madera_white", "emi_cafe:puerta_madera_white", 1, 70, "Puertas", "Puerta blanca con marco de madera"),
            new Articulo("puerta_madera_pink", "emi_cafe:puerta_madera_pink", 1, 70, "Puertas", "Puerta rosa con marco de madera"),
            new Articulo("puerta_madera_magenta", "emi_cafe:puerta_madera_magenta", 1, 70, "Puertas", "Puerta magenta con marco de madera"),
            new Articulo("puerta_madera_purple", "emi_cafe:puerta_madera_purple", 1, 70, "Puertas", "Puerta morada con marco de madera"),
            new Articulo("puerta_madera_blue", "emi_cafe:puerta_madera_blue", 1, 70, "Puertas", "Puerta azul con marco de madera"),
            new Articulo("puerta_madera_light_blue", "emi_cafe:puerta_madera_light_blue", 1, 70, "Puertas", "Puerta celeste con marco de madera"),
            new Articulo("puerta_madera_cyan", "emi_cafe:puerta_madera_cyan", 1, 70, "Puertas", "Puerta cian con marco de madera"),
            new Articulo("puerta_madera_lime", "emi_cafe:puerta_madera_lime", 1, 70, "Puertas", "Puerta lima con marco de madera"),
            new Articulo("puerta_madera_yellow", "emi_cafe:puerta_madera_yellow", 1, 70, "Puertas", "Puerta amarilla con marco de madera"),
            new Articulo("puerta_madera_orange", "emi_cafe:puerta_madera_orange", 1, 70, "Puertas", "Puerta naranja con marco de madera"),
            new Articulo("puerta_madera_red", "emi_cafe:puerta_madera_red", 1, 70, "Puertas", "Puerta roja con marco de madera"),
            new Articulo("puerta_madera_black", "emi_cafe:puerta_madera_black", 1, 70, "Puertas", "Puerta negra con marco de madera"),
            new Articulo("puerta_blanca_white", "emi_cafe:puerta_blanca_white", 1, 70, "Puertas", "Puerta blanca con marco blanco"),
            new Articulo("puerta_blanca_pink", "emi_cafe:puerta_blanca_pink", 1, 70, "Puertas", "Puerta rosa con marco blanco"),
            new Articulo("puerta_blanca_magenta", "emi_cafe:puerta_blanca_magenta", 1, 70, "Puertas", "Puerta magenta con marco blanco"),
            new Articulo("puerta_blanca_purple", "emi_cafe:puerta_blanca_purple", 1, 70, "Puertas", "Puerta morada con marco blanco"),
            new Articulo("puerta_blanca_blue", "emi_cafe:puerta_blanca_blue", 1, 70, "Puertas", "Puerta azul con marco blanco"),
            new Articulo("puerta_blanca_light_blue", "emi_cafe:puerta_blanca_light_blue", 1, 70, "Puertas", "Puerta celeste con marco blanco"),
            new Articulo("puerta_blanca_cyan", "emi_cafe:puerta_blanca_cyan", 1, 70, "Puertas", "Puerta cian con marco blanco"),
            new Articulo("puerta_blanca_lime", "emi_cafe:puerta_blanca_lime", 1, 70, "Puertas", "Puerta lima con marco blanco"),
            new Articulo("puerta_blanca_yellow", "emi_cafe:puerta_blanca_yellow", 1, 70, "Puertas", "Puerta amarilla con marco blanco"),
            new Articulo("puerta_blanca_orange", "emi_cafe:puerta_blanca_orange", 1, 70, "Puertas", "Puerta naranja con marco blanco"),
            new Articulo("puerta_blanca_red", "emi_cafe:puerta_blanca_red", 1, 70, "Puertas", "Puerta roja con marco blanco"),
            new Articulo("puerta_blanca_black", "emi_cafe:puerta_blanca_black", 1, 70, "Puertas", "Puerta negra con marco blanco"),
            // vitrinas de cristal de colores (0.19.0): se pone en ellas un ingrediente o un plato para lucirlo
            new Articulo("vitrina_white", "emi_cafe:vitrina_white", 1, 60, "Vitrinas", "Vitrina de cristal blanca"),
            new Articulo("vitrina_pink", "emi_cafe:vitrina_pink", 1, 60, "Vitrinas", "Vitrina de cristal rosa"),
            new Articulo("vitrina_magenta", "emi_cafe:vitrina_magenta", 1, 60, "Vitrinas", "Vitrina de cristal magenta"),
            new Articulo("vitrina_purple", "emi_cafe:vitrina_purple", 1, 60, "Vitrinas", "Vitrina de cristal morada"),
            new Articulo("vitrina_blue", "emi_cafe:vitrina_blue", 1, 60, "Vitrinas", "Vitrina de cristal azul"),
            new Articulo("vitrina_light_blue", "emi_cafe:vitrina_light_blue", 1, 60, "Vitrinas", "Vitrina de cristal celeste"),
            new Articulo("vitrina_cyan", "emi_cafe:vitrina_cyan", 1, 60, "Vitrinas", "Vitrina de cristal cian"),
            new Articulo("vitrina_lime", "emi_cafe:vitrina_lime", 1, 60, "Vitrinas", "Vitrina de cristal lima"),
            new Articulo("vitrina_yellow", "emi_cafe:vitrina_yellow", 1, 60, "Vitrinas", "Vitrina de cristal amarilla"),
            new Articulo("vitrina_orange", "emi_cafe:vitrina_orange", 1, 60, "Vitrinas", "Vitrina de cristal naranja"),
            new Articulo("vitrina_red", "emi_cafe:vitrina_red", 1, 60, "Vitrinas", "Vitrina de cristal roja"),
            new Articulo("vitrina_black", "emi_cafe:vitrina_black", 1, 60, "Vitrinas", "Vitrina de cristal negra"));

    // ------------------------------------------------------------ el catalogo completo (desde 0.21.0)

    record Grupo(String nombre, List<Articulo> arts) {}
    record Fuente(String nombre, List<Grupo> grupos) {}

    /** Los mods de decoracion del servidor cuyos objetos se venden (espacio de nombres -> nombre que se ve). Sus muebles con «entidad de bloque» (cajones, estantes...) se permiten: dentro del cafe no se abren. */
    static final java.util.Map<String, String> MODS = new java.util.LinkedHashMap<>();
    static {
        MODS.put("handcrafted", "Handcrafted");
        MODS.put("cozyhome", "Cozy Home");
        MODS.put("cobblefurnies", "CobbleFurnies");
        MODS.put("beautify", "Beautify");
        MODS.put("carved_wood", "Carved Wood");
        MODS.put("comforts", "Comforts");
        MODS.put("moarconcrete", "Moar Concrete");
        MODS.put("chipped", "Chipped");
    }

    private static final String[][] COLORES = {{"white", "blanco"}, {"orange", "naranja"}, {"magenta", "magenta"}, {"light_blue", "celeste"}, {"yellow", "amarillo"}, {"lime", "lima"}, {"pink", "rosa"}, {"gray", "gris"},
            {"light_gray", "gris claro"}, {"cyan", "cian"}, {"purple", "morado"}, {"blue", "azul"}, {"brown", "marrón"}, {"green", "verde"}, {"red", "rojo"}, {"black", "negro"}};
    private static final String[][] MADERAS = {{"oak", "roble"}, {"spruce", "abeto"}, {"birch", "abedul"}, {"jungle", "jungla"}, {"acacia", "acacia"}, {"dark_oak", "roble oscuro"}, {"mangrove", "manglar"},
            {"cherry", "cerezo"}, {"bamboo", "bambú"}, {"crimson", "carmesí"}, {"warped", "distorsionado"}};
    /** grupo | bloque | raiz de las escaleras/losas/muro | letras: e = escaleras, l = losa, m = muro */
    private static final String[] FAMILIAS = {
            "Piedra|stone|stone|el", "Piedra|cobblestone|cobblestone|elm", "Piedra|mossy_cobblestone|mossy_cobblestone|elm", "Piedra|smooth_stone|smooth_stone|l", "Piedra|andesite|andesite|elm", "Piedra|polished_andesite|polished_andesite|el",
            "Piedra|diorite|diorite|elm", "Piedra|polished_diorite|polished_diorite|el", "Piedra|granite|granite|elm", "Piedra|polished_granite|polished_granite|el", "Piedra|calcite|calcite|", "Piedra|dripstone_block|dripstone_block|",
            "Ladrillos|stone_bricks|stone_brick|elm", "Ladrillos|mossy_stone_bricks|mossy_stone_brick|elm", "Ladrillos|cracked_stone_bricks|cracked_stone_bricks|", "Ladrillos|chiseled_stone_bricks|chiseled_stone_bricks|",
            "Ladrillos|bricks|brick|elm", "Ladrillos|mud_bricks|mud_brick|elm", "Ladrillos|tuff|tuff|elm", "Ladrillos|polished_tuff|polished_tuff|elm", "Ladrillos|tuff_bricks|tuff_brick|elm",
            "Arenisca y cuarzo|sandstone|sandstone|elm", "Arenisca y cuarzo|smooth_sandstone|smooth_sandstone|el", "Arenisca y cuarzo|cut_sandstone|cut_sandstone|l", "Arenisca y cuarzo|chiseled_sandstone|chiseled_sandstone|",
            "Arenisca y cuarzo|red_sandstone|red_sandstone|elm", "Arenisca y cuarzo|smooth_red_sandstone|smooth_red_sandstone|el", "Arenisca y cuarzo|quartz_block|quartz|el", "Arenisca y cuarzo|quartz_bricks|quartz_bricks|",
            "Arenisca y cuarzo|smooth_quartz|smooth_quartz|el", "Arenisca y cuarzo|chiseled_quartz_block|chiseled_quartz_block|", "Arenisca y cuarzo|quartz_pillar|quartz_pillar|",
            "Pizarra y piedra negra|cobbled_deepslate|cobbled_deepslate|elm", "Pizarra y piedra negra|polished_deepslate|polished_deepslate|elm", "Pizarra y piedra negra|deepslate_bricks|deepslate_brick|elm",
            "Pizarra y piedra negra|deepslate_tiles|deepslate_tile|elm", "Pizarra y piedra negra|chiseled_deepslate|chiseled_deepslate|", "Pizarra y piedra negra|blackstone|blackstone|elm",
            "Pizarra y piedra negra|polished_blackstone|polished_blackstone|elm", "Pizarra y piedra negra|polished_blackstone_bricks|polished_blackstone_brick|elm", "Pizarra y piedra negra|gilded_blackstone|gilded_blackstone|",
            "Nether, End y mar|nether_bricks|nether_brick|elm", "Nether, End y mar|red_nether_bricks|red_nether_brick|elm", "Nether, End y mar|end_stone_bricks|end_stone_brick|elm", "Nether, End y mar|purpur_block|purpur|el",
            "Nether, End y mar|prismarine|prismarine|elm", "Nether, End y mar|prismarine_bricks|prismarine_brick|el", "Nether, End y mar|dark_prismarine|dark_prismarine|el", "Nether, End y mar|bone_block|bone_block|",
            "Metal y gemas|cut_copper|cut_copper|el", "Metal y gemas|exposed_cut_copper|exposed_cut_copper|el", "Metal y gemas|weathered_cut_copper|weathered_cut_copper|el", "Metal y gemas|oxidized_cut_copper|oxidized_cut_copper|el",
            "Metal y gemas|copper_block|copper_block|", "Metal y gemas|copper_grate|copper_grate|", "Metal y gemas|iron_block|iron_block|", "Metal y gemas|gold_block|gold_block|", "Metal y gemas|amethyst_block|amethyst_block|",
            "Metal y gemas|emerald_block|emerald_block|", "Metal y gemas|diamond_block|diamond_block|", "Metal y gemas|lapis_block|lapis_block|", "Metal y gemas|coal_block|coal_block|", "Metal y gemas|netherite_block|netherite_block|",
            "Metal y gemas|iron_bars|iron_bars|", "Metal y gemas|chain|chain|"};
    private static final String[] PLANTAS = {"dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley",
            "torchflower", "sunflower", "lilac", "rose_bush", "peony", "pink_petals", "short_grass", "fern", "large_fern", "spore_blossom", "moss_block", "moss_carpet", "big_dripleaf", "small_dripleaf", "azalea",
            "flowering_azalea", "azalea_leaves", "flowering_azalea_leaves", "cactus", "bamboo", "sugar_cane", "lily_pad", "vine", "hanging_roots", "glow_lichen", "flower_pot", "mangrove_roots", "melon", "pumpkin", "hay_block",
            "grass_block", "dirt_path", "coarse_dirt", "podzol", "mycelium", "rooted_dirt", "mud", "packed_mud", "clay", "snow_block", "ice", "packed_ice", "brown_mushroom_block", "red_mushroom_block", "mushroom_stem"};
    private static final String[] HOGAR = {"bookshelf", "crafting_table", "cartography_table", "fletching_table", "smithing_table", "loom", "stonecutter", "grindstone", "composter", "cauldron", "bell", "lodestone", "ladder",
            "scaffolding", "glass", "tinted_glass", "glass_pane", "end_rod", "lightning_rod",
            "candle", "lantern", "soul_lantern", "torch", "soul_torch", "sea_lantern", "glowstone", "shroomlight", "ochre_froglight", "verdant_froglight", "pearlescent_froglight", "jack_o_lantern",
            "sponge", "honeycomb_block", "dried_kelp_block", "bamboo_mosaic", "bamboo_mosaic_stairs", "bamboo_mosaic_slab", "chiseled_copper", "copper_door", "copper_trapdoor"};

    private static List<Fuente> fuentes;
    private static final java.util.Map<String, Articulo> POR_ID = new java.util.HashMap<>();
    private record Ent(Articulo a, String clave) {}
    private static final List<Ent> TODOS = new java.util.ArrayList<>();

    private static boolean existe(String item) { return Registries.ITEM.containsId(Identifier.of(item)); }

    private static String sinAcentos(String t) { return java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT); }

    private static final class Constructor {
        final java.util.LinkedHashMap<String, List<Articulo>> grupos = new java.util.LinkedHashMap<>();
        final String fuente;
        Constructor(String fuente) { this.fuente = fuente; }
        void add(String grupo, String item, int cantidad, int precio, String nombre) {
            Articulo previo = POR_ID.get(item);                                // un articulo de «Básicos» sale tambien en su grupo de «Minecraft» (es el mismo: mismo precio)
            if (previo != null) { grupos.computeIfAbsent(grupo, k -> new java.util.ArrayList<>()).add(previo); return; }
            if (!existe(item)) return;
            if (!(Registries.ITEM.get(Identifier.of(item)) instanceof net.minecraft.item.BlockItem bi) || !Obras.permitido(bi.getBlock())) return;          // solo bloques que se puedan poner en el cafe
            Articulo a = new Articulo(item, item, cantidad, precio, grupo, nombre);
            POR_ID.put(item, a);
            grupos.computeIfAbsent(grupo, k -> new java.util.ArrayList<>()).add(a);
            TODOS.add(new Ent(a, sinAcentos(item + " " + nombre + " " + grupo + " " + fuente)));
        }
        Fuente fin() {
            List<Grupo> gs = new java.util.ArrayList<>();
            for (var e : grupos.entrySet()) if (!e.getValue().isEmpty()) gs.add(new Grupo(e.getKey(), e.getValue()));
            return gs.isEmpty() ? null : new Fuente(fuente, gs);
        }
    }

    /** Todas las fuentes de la tienda (Basicos, Minecraft y un mod por fuente), construidas la primera vez: lo que no existe en este servidor (un mod que falta) simplemente no sale. */
    static synchronized List<Fuente> fuentes() {
        if (fuentes != null) return fuentes;
        List<Fuente> res = new java.util.ArrayList<>();
        Constructor bas = new Constructor("Básicos");
        for (Articulo a : BASE) {
            if (POR_ID.containsKey(a.item()) || !existe(a.item())) continue;
            POR_ID.put(a.item(), a);
            POR_ID.putIfAbsent(a.id(), a);
            bas.grupos.computeIfAbsent(a.grupo(), k -> new java.util.ArrayList<>()).add(a);
            TODOS.add(new Ent(a, sinAcentos(a.item() + " " + a.nombre() + " " + a.grupo() + " Básicos")));
        }
        for (Estacion e : Estacion.values()) {                                  // copias extra de las estaciones de cocina (0.27.0): el precio sube con las copias que ya tienes (Obras.precioDe)
            if (e.x < 0 || e.tipo == Estacion.Tipo.MESA) continue;
            String item = Registries.ITEM.getId(e.bloque.asItem()).toString();
            int base = switch (e.tipo) { case CAJA -> 50; case BASURA -> 40; case EMPLATAR -> 100; default -> 120; };
            Articulo a = new Articulo("est_" + e.name().toLowerCase(java.util.Locale.ROOT), item, 1, base, "Cocina: copias extra", e.nombre + " extra");
            if (POR_ID.containsKey(a.item()) || !existe(a.item())) continue;
            POR_ID.put(a.item(), a);
            POR_ID.putIfAbsent(a.id(), a);
            bas.grupos.computeIfAbsent(a.grupo(), k -> new java.util.ArrayList<>()).add(a);
            TODOS.add(new Ent(a, sinAcentos(a.item() + " " + a.nombre() + " " + a.grupo() + " Básicos copias extra estacion")));
        }
        Fuente fb = bas.fin();
        if (fb != null) res.add(fb);
        res.add(vanilla());
        for (var m : MODS.entrySet()) {
            Fuente f = deMod(m.getKey(), m.getValue());
            if (f != null) res.add(f);
        }
        fuentes = res;
        EmiCafe.LOG.info("Tienda de decoracion del cafe: {} articulos en {} fuentes", TODOS.size(), res.size());
        return res;
    }

    private static Fuente vanilla() {
        Constructor c = new Constructor("Minecraft");
        for (String[] w : MADERAS) {
            String id = w[0], g = "Madera de " + w[1];
            boolean hongo = id.equals("crimson") || id.equals("warped");
            c.add(g, "minecraft:" + id + "_planks", 16, 28, "");
            c.add(g, "minecraft:" + id + "_stairs", 8, 22, "");
            c.add(g, "minecraft:" + id + "_slab", 8, 16, "");
            c.add(g, "minecraft:" + id + "_fence", 8, 18, "");
            c.add(g, "minecraft:" + id + "_fence_gate", 2, 14, "");
            c.add(g, "minecraft:" + id + "_trapdoor", 4, 22, "");
            c.add(g, "minecraft:" + id + "_door", 2, 24, "");
            c.add(g, "minecraft:" + id + (hongo ? "_stem" : id.equals("bamboo") ? "_block" : "_log"), 8, 26, "");
            c.add(g, "minecraft:stripped_" + id + (hongo ? "_stem" : id.equals("bamboo") ? "_block" : "_log"), 8, 28, "");
            c.add(g, "minecraft:" + id + "_leaves", 16, 22, "");
        }
        for (String f : FAMILIAS) {
            String[] x = f.split("\\|", -1);
            String g = x[0], b = x[1], r = x[2], fl = x[3];
            c.add(g, "minecraft:" + b, 16, 30, "");
            if (fl.contains("e")) c.add(g, "minecraft:" + r + "_stairs", 8, 24, "");
            if (fl.contains("l")) c.add(g, "minecraft:" + r + "_slab", 8, 18, "");
            if (fl.contains("m")) c.add(g, "minecraft:" + r + "_wall", 8, 22, "");
        }
        String[][] tipos = {{"concrete", "Hormigón de colores", "16", "30"}, {"wool", "Lana de colores", "16", "26"}, {"terracotta", "Terracota de colores", "16", "28"}, {"glazed_terracotta", "Terracota esmaltada", "8", "34"},
                {"stained_glass", "Cristal de colores", "16", "34"}, {"stained_glass_pane", "Paneles de colores", "16", "22"}, {"carpet", "Alfombras de colores", "16", "18"}, {"candle", "Velas de colores", "4", "22"},
                {"bed", "Camas de colores", "1", "70"}, {"banner", "Estandartes", "1", "30"}};
        for (String[] t : tipos) for (String[] col : COLORES) c.add(t[1], "minecraft:" + col[0] + "_" + t[0], Integer.parseInt(t[2]), Integer.parseInt(t[3]), "");
        for (String pl : PLANTAS) c.add("Plantas y suelos", "minecraft:" + pl, 8, 24, "");
        for (String h : HOGAR) c.add("Hogar y luces", "minecraft:" + h, 4, 34, "");
        for (String[] w : MADERAS) c.add("Plantones", "minecraft:" + w[0] + "_sapling", 4, 20, "");
        return c.fin();
    }

    private static boolean excluido(String ns, String path) {
        if (path.contains("chest") || path.contains("barrel") || path.contains("crafter") || path.contains("crafting") || path.contains("hopper") || path.contains("furnicrafter") || path.contains("falling_liquid")
                || path.contains("saddle_block") || path.contains("controller") || path.contains("dye_vat") || path.contains("beehive") || path.contains("campfire") || path.contains("workbench") || path.contains("sorter") || path.contains("redstone")) return true;
        return ns.equals("chipped") && (path.endsWith("_bench") || path.endsWith("_table") || path.equals("glassblower") || path.equals("alchemy_bench") || path.equals("botanist_workbench"));
    }

    /** ¿El nombre tiene alguna de estas palabras? (palabra entera entre «_»; una clave con «_» se busca tal cual). */
    private static boolean tiene(String p, String... ks) {
        List<String> toks = java.util.Arrays.asList(p.split("_"));
        for (String k : ks) if (k.contains("_") ? p.contains(k) : toks.contains(k)) return true;
        return false;
    }

    /** El grupo de un objeto de un mod segun su nombre: muebles -> sillas, mesas, lamparas...; Chipped, Carved Wood y Moar Concrete -> por material. */
    private static String clasificar(String ns, String p) {
        if (ns.equals("carved_wood")) return tiene(p, "stairs", "slab") ? "Escaleras y losas" : tiene(p, "lantern", "lamp") ? "Luces" : tiene(p, "ladder", "bookshelf", "campfire") ? "Adornos de madera" : "Madera tallada";
        if (ns.equals("moarconcrete")) return tiene(p, "stairs", "slab") ? "Escaleras y losas" : tiene(p, "wall", "fence") ? "Muros y vallas" : "Bloques";
        if (ns.equals("chipped")) {
            if (tiene(p, "carpet")) return "Alfombras";
            if (tiene(p, "glazed_terracotta")) return "Terracota esmaltada";
            if (tiene(p, "terracotta")) return "Terracota";
            if (tiene(p, "wool")) return "Lana";
            if (tiene(p, "concrete")) return "Hormigón";
            if (tiene(p, "glass")) return "Cristal";
            if (tiene(p, "trapdoor", "door")) return "Puertas y trampillas";
            if (tiene(p, "lantern", "torch", "lamp", "glowstone", "froglight", "shroomlight")) return "Luces";
            if (tiene(p, "leaves", "flower", "moss", "grass", "vine", "azalea", "mushroom", "sapling", "dirt", "mud", "clay")) return "Plantas y tierra";
            if (tiene(p, "planks", "log", "stem", "wood", "bookshelf", "hyphae", "bamboo", "oak", "spruce", "birch", "acacia", "jungle", "cherry", "mangrove", "crimson", "warped")) return "Madera";
            if (tiene(p, "iron", "gold", "copper", "diamond", "emerald", "netherite", "lapis", "coal", "amethyst", "quartz")) return "Metal y gemas";
            return "Piedra y ladrillo";
        }
        if (tiene(p, "bed", "hammock", "sleeping_bag")) return "Camas y hamacas";
        if (tiene(p, "sofa", "couch", "armchair", "bench", "zaisu")) return "Sofás y bancos";
        if (tiene(p, "chair", "stool")) return "Sillas y taburetes";
        if (tiene(p, "table", "desk", "nightstand")) return "Mesas y escritorios";
        if (tiene(p, "lamp", "lantern", "chandelier", "candelabra")) return "Lámparas";
        if (tiene(p, "counter", "cabinet", "cabinetry", "cupboard", "drawer", "shelf", "sink", "stove", "fridge", "freezer", "oven", "hood", "bathtub", "toilet", "fountain", "chimney", "wall_support", "tv")) return "Cocina y baño";
        if (tiene(p, "bowl", "cup", "plate", "crockery", "pot", "potted", "jar")) return "Vajilla y macetas";
        if (tiene(p, "cushion", "carpet", "wool", "curtain", "blinds")) return "Cojines y cortinas";
        if (tiene(p, "trim", "pillar", "column", "corner", "floor", "ceramic", "porcelain", "mosaic", "grass_block")) return "Molduras y suelos";
        return "Adornos";
    }

    private static int[] medida(String grupo, String p) {
        if (tiene(p, "stairs", "slab", "wall") && !tiene(grupo, "Sofás", "Camas")) return new int[]{8, 24};
        return switch (grupo) {
            case "Camas y hamacas" -> new int[]{1, 80};
            case "Sofás y bancos" -> new int[]{1, 90};
            case "Sillas y taburetes" -> new int[]{1, 50};
            case "Mesas y escritorios", "Cocina y baño" -> new int[]{1, 70};
            case "Lámparas" -> new int[]{1, 55};
            case "Vajilla y macetas" -> new int[]{2, 30};
            case "Cojines y cortinas" -> new int[]{2, 36};
            case "Adornos" -> new int[]{1, 45};
            case "Luces" -> new int[]{4, 40};
            case "Madera tallada", "Adornos de madera", "Escaleras y losas", "Muros y vallas" -> new int[]{8, 30};
            case "Alfombras" -> new int[]{16, 22};
            case "Cristal", "Puertas y trampillas" -> new int[]{8, 34};
            case "Metal y gemas" -> new int[]{8, 70};
            default -> new int[]{16, 36};
        };
    }

    private static Fuente deMod(String ns, String nombre) {
        Constructor c = new Constructor(nombre);
        List<Identifier> ids = new java.util.ArrayList<>();
        for (Identifier id : Registries.ITEM.getIds()) if (id.getNamespace().equals(ns)) ids.add(id);
        ids.sort(Identifier::compareTo);
        for (Identifier id : ids) {
            net.minecraft.item.Item it = Registries.ITEM.get(id);
            if (!(it instanceof net.minecraft.item.BlockItem bi) || excluido(ns, id.getPath()) || !Obras.permitido(bi.getBlock())) continue;
            String g = clasificar(ns, id.getPath());
            int[] m = medida(g, id.getPath());
            c.add(g, id.toString(), m[0], m[1], "");
        }
        return c.fin();
    }

    // ------------------------------------------------------------ lo que ve cada jugador en la tienda (fuente, grupo, pagina y busqueda)

    static final int POR_PAG = 12;

    static final class Vista {
        int fuente, grupo, pag;
        String buscar = "";
        boolean verFav;                                   // la lista de favoritos (las estrellas) en vez de una fuente
        List<Articulo> resultados = List.of();
        List<Articulo> favs = List.of();                  // los favoritos del dueño, puestos por Menus antes de usar la vista
        int baulFuente;                                   // filtro del baul: 0 = todo, 1.. = una fuente (ver Menus)
        String planoElegido = "";                         // el plano que se va a compartir (0.27.0): lo elige el clic en COMPARTIR y lo usa /cafe plano compartir <jugador>
        int baulGrupo;                                    // filtro del baul por categoria (0.27.0): 0 = todas, 1.. = un grupo de los que hay en lo ya filtrado por fuente
        String baulBuscar = "";

        void ponerFavoritos(java.util.Collection<String> ids) {
            List<Articulo> r = new java.util.ArrayList<>();
            for (String id : ids) { Articulo a = articulo(id); if (a != null) r.add(a); }
            favs = r;
        }

        List<Articulo> lista() {
            List<Fuente> fs = fuentes();
            if (!buscar.isEmpty()) return resultados;
            if (verFav) return favs;
            fuente = Math.max(0, Math.min(fs.size() - 1, fuente));
            Fuente f = fs.get(fuente);
            grupo = Math.max(0, Math.min(f.grupos().size() - 1, grupo));
            return f.grupos().get(grupo).arts();
        }

        int paginas() { return Math.max(1, (lista().size() + POR_PAG - 1) / POR_PAG); }

        void buscar(String texto) {
            buscar = texto == null ? "" : texto.trim();
            pag = 0;
            if (!buscar.isEmpty()) verFav = false;
            if (buscar.isEmpty()) { resultados = List.of(); return; }
            fuentes();
            String[] toks = sinAcentos(buscar).split("\\s+");
            List<Articulo> r = new java.util.ArrayList<>();
            for (Ent e : TODOS) {
                boolean ok = true;
                for (String t : toks) if (!e.clave().contains(t)) { ok = false; break; }
                if (ok) r.add(e.a());
                if (r.size() >= 600) break;
            }
            resultados = r;
        }

        /** Mueve la pagina (delta), la fuente o el grupo (con vuelta); cambiar de fuente o de grupo vuelve a la pagina 1. */
        void mover(String que, int delta) {
            List<Fuente> fs = fuentes();
            switch (que) {
                case "pag" -> pag = Math.max(0, Math.min(paginas() - 1, pag + delta));
                case "fav" -> { verFav = !verFav; buscar = ""; resultados = List.of(); pag = 0; }
                case "fuente" -> { verFav = false; buscar = ""; resultados = List.of(); fuente = Math.floorMod(fuente + delta, fs.size()); grupo = 0; pag = 0; }
                case "grupo" -> { verFav = false; buscar = ""; resultados = List.of(); int n = fs.get(Math.max(0, Math.min(fs.size() - 1, fuente))).grupos().size(); grupo = Math.floorMod(grupo + delta, n); pag = 0; }
                default -> { }
            }
            pag = Math.min(pag, paginas() - 1);
        }
    }

    /** El articulo de la tienda por su identificador (null si no existe). */
    static Articulo articulo(String id) { fuentes(); return POR_ID.get(id); }

    /** El estado de un bloque como texto (para el archivo del jugador). */
    static String guardar(BlockState st) { return NbtHelper.fromBlockState(st).toString(); }

    /** Lo contrario de {@link #guardar}; null si el texto ya no vale (un bloque que ya no existe). */
    static BlockState leer(String txt) {
        try {
            NbtCompound n = StringNbtReader.parse(txt);
            return NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), n);
        } catch (Exception e) { return null; }
    }

    static String clave(int rx, int ry, int rz) { return rx + "," + ry + "," + rz; }
}
