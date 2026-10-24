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

    static final List<Articulo> CATALOGO = List.of(
            new Articulo("mesa", "emi_cafe:mesa", 1, 60, "Café", "Mesa del café"),
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
            new Articulo("puerta_blanca_black", "emi_cafe:puerta_blanca_black", 1, 70, "Puertas", "Puerta negra con marco blanco"));

    /** El articulo de la tienda que corresponde a un objeto (null si no se vende: asi solo se pueden colocar cosas sin «entidad de bloque»). */
    static Articulo articuloDe(net.minecraft.item.Item it) {
        String id = Registries.ITEM.getId(it).toString();
        for (Articulo a : CATALOGO) if (a.item().equals(id)) return a;
        return null;
    }

    static Articulo articulo(String id) { for (Articulo a : CATALOGO) if (a.id().equals(id)) return a; return null; }

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
