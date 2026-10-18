package emi.plush.gen;

import dev.mrshawn.pokeblocks.block.custom.PokedollBlock;
import dev.mrshawn.pokeblocks.block.entity.BlockEntityTypeRegistry;
import dev.mrshawn.pokeblocks.block.entity.PokedollBlockEntity;
import dev.mrshawn.pokeblocks.item.DollRarity;
import dev.mrshawn.pokeblocks.item.custom.WearablePokedollBlockItem;
import emi.plush.EmiDollItemModel;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.class_1792;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2378;
import net.minecraft.class_2591;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/** Registro de todos los peluches Emi (generado por gen_dolls.py). Los objetos son «llevables» (se ponen en la cabeza, como los de Pokeblocks). */
public final class DollRegistry {
    private DollRegistry() {}

    public static final Map<String, class_2591<? extends PokedollBlockEntity>> TIPOS = new LinkedHashMap<>();
    public static final Map<String, class_1792> OBJETOS = new LinkedHashMap<>();
    /** peluches gigantes: id -> tipo de entidad de bloque, e id -> nombre del modelo (geo/textura) que comparten con el peluche normal */
    public static final Map<String, class_2591<? extends PokedollBlockEntity>> TIPOS_GIGANTES = new LinkedHashMap<>();
    public static final Map<String, String> MODELO_GIGANTE = new LinkedHashMap<>();

    private static <T extends PokedollBlockEntity> class_2591<T> reg(String nombre, String modelo, Class<T> clase, BiFunction<class_2338, class_2680, T> ctor) {
        class_2960 id = class_2960.method_60655("emi_plush", nombre);
        class_2248 bloque = class_2378.method_10230(class_7923.field_41175, id, new PokedollBlock<T>(() -> clase));
        class_1792 objeto = class_2378.method_10230(class_7923.field_41178, id, new WearablePokedollBlockItem(bloque, DollRarity.NONE, 0, () -> new EmiDollItemModel(modelo)));
        class_2591<T> tipo = class_2378.method_10230(class_7923.field_41181, id, FabricBlockEntityTypeBuilder.create(ctor::apply, bloque).build());
        BlockEntityTypeRegistry.register(clase, tipo);
        OBJETOS.put(nombre, objeto);
        return tipo;
    }

    private static <T extends PokedollBlockEntity> void reg(String nombre, Class<T> clase, BiFunction<class_2338, class_2680, T> ctor) {
        TIPOS.put(nombre, reg(nombre, nombre, clase, ctor));
    }

    private static <T extends PokedollBlockEntity> void regGigante(String nombre, String modelo, Class<T> clase, BiFunction<class_2338, class_2680, T> ctor) {
        TIPOS_GIGANTES.put(nombre, reg(nombre, modelo, clase, ctor));
        MODELO_GIGANTE.put(nombre, modelo);
    }

    public static void registrar() {
        reg("happiny_emi", Doll_happiny_emi.class, Doll_happiny_emi::new);
        reg("chansey_emi", Doll_chansey_emi.class, Doll_chansey_emi::new);
        reg("blissey_emi", Doll_blissey_emi.class, Doll_blissey_emi::new);
        reg("cleffa_emi", Doll_cleffa_emi.class, Doll_cleffa_emi::new);
        reg("clefairy_emi", Doll_clefairy_emi.class, Doll_clefairy_emi::new);
        reg("clefable_emi", Doll_clefable_emi.class, Doll_clefable_emi::new);
        reg("igglybuff_emi", Doll_igglybuff_emi.class, Doll_igglybuff_emi::new);
        reg("jigglypuff_emi", Doll_jigglypuff_emi.class, Doll_jigglypuff_emi::new);
        reg("wigglytuff_emi", Doll_wigglytuff_emi.class, Doll_wigglytuff_emi::new);
        reg("eevee_emi", Doll_eevee_emi.class, Doll_eevee_emi::new);
        reg("vaporeon_emi", Doll_vaporeon_emi.class, Doll_vaporeon_emi::new);
        reg("jolteon_emi", Doll_jolteon_emi.class, Doll_jolteon_emi::new);
        reg("flareon_emi", Doll_flareon_emi.class, Doll_flareon_emi::new);
        reg("espeon_emi", Doll_espeon_emi.class, Doll_espeon_emi::new);
        reg("umbreon_emi", Doll_umbreon_emi.class, Doll_umbreon_emi::new);
        reg("leafeon_emi", Doll_leafeon_emi.class, Doll_leafeon_emi::new);
        reg("glaceon_emi", Doll_glaceon_emi.class, Doll_glaceon_emi::new);
        reg("sylveon_emi", Doll_sylveon_emi.class, Doll_sylveon_emi::new);
        reg("ralts_emi", Doll_ralts_emi.class, Doll_ralts_emi::new);
        reg("kirlia_emi", Doll_kirlia_emi.class, Doll_kirlia_emi::new);
        reg("gardevoir_emi", Doll_gardevoir_emi.class, Doll_gardevoir_emi::new);
        reg("gatitoalien_gold", Doll_gatitoalien_gold.class, Doll_gatitoalien_gold::new);
        reg("gatitoalien_sunglasses", Doll_gatitoalien_sunglasses.class, Doll_gatitoalien_sunglasses::new);
        reg("gatitoalien_beach_hat", Doll_gatitoalien_beach_hat.class, Doll_gatitoalien_beach_hat::new);
        reg("gatitoalien_crown", Doll_gatitoalien_crown.class, Doll_gatitoalien_crown::new);
        reg("gatitoalien_bow", Doll_gatitoalien_bow.class, Doll_gatitoalien_bow::new);
        reg("gatitoalien_headphones", Doll_gatitoalien_headphones.class, Doll_gatitoalien_headphones::new);
        reg("gatitoalien_scarf", Doll_gatitoalien_scarf.class, Doll_gatitoalien_scarf::new);
        reg("gatitoalien_halo", Doll_gatitoalien_halo.class, Doll_gatitoalien_halo::new);
        reg("gatitoalien_devil_horns", Doll_gatitoalien_devil_horns.class, Doll_gatitoalien_devil_horns::new);
        reg("gatitoalien_witch_hat", Doll_gatitoalien_witch_hat.class, Doll_gatitoalien_witch_hat::new);
        reg("gatitoalien_santa_hat", Doll_gatitoalien_santa_hat.class, Doll_gatitoalien_santa_hat::new);
        reg("gatitoalien_flower_crown", Doll_gatitoalien_flower_crown.class, Doll_gatitoalien_flower_crown::new);
        reg("gatitoalien_pirate", Doll_gatitoalien_pirate.class, Doll_gatitoalien_pirate::new);
        reg("gatitoalien_gold_sunglasses", Doll_gatitoalien_gold_sunglasses.class, Doll_gatitoalien_gold_sunglasses::new);
        reg("gatitoalien_gold_beach_hat", Doll_gatitoalien_gold_beach_hat.class, Doll_gatitoalien_gold_beach_hat::new);
        reg("gatitoalien_gold_crown", Doll_gatitoalien_gold_crown.class, Doll_gatitoalien_gold_crown::new);
        reg("gatitoalien_gold_bow", Doll_gatitoalien_gold_bow.class, Doll_gatitoalien_gold_bow::new);
        reg("gatitoalien_gold_headphones", Doll_gatitoalien_gold_headphones.class, Doll_gatitoalien_gold_headphones::new);
        reg("gatitoalien_gold_scarf", Doll_gatitoalien_gold_scarf.class, Doll_gatitoalien_gold_scarf::new);
        reg("gatitoalien_gold_halo", Doll_gatitoalien_gold_halo.class, Doll_gatitoalien_gold_halo::new);
        reg("gatitoalien_gold_devil_horns", Doll_gatitoalien_gold_devil_horns.class, Doll_gatitoalien_gold_devil_horns::new);
        reg("gatitoalien_gold_witch_hat", Doll_gatitoalien_gold_witch_hat.class, Doll_gatitoalien_gold_witch_hat::new);
        reg("gatitoalien_gold_santa_hat", Doll_gatitoalien_gold_santa_hat.class, Doll_gatitoalien_gold_santa_hat::new);
        reg("gatitoalien_gold_flower_crown", Doll_gatitoalien_gold_flower_crown.class, Doll_gatitoalien_gold_flower_crown::new);
        reg("gatitoalien_gold_pirate", Doll_gatitoalien_gold_pirate.class, Doll_gatitoalien_gold_pirate::new);
        reg("gatitoalien_suit", Doll_gatitoalien_suit.class, Doll_gatitoalien_suit::new);
        reg("gatitoalien_carrito", Doll_gatitoalien_carrito.class, Doll_gatitoalien_carrito::new);
        reg("gatitoalien_dino", Doll_gatitoalien_dino.class, Doll_gatitoalien_dino::new);
        regGigante("gigante_michi_dramatico", "michi_dramatico", Gigantic_michi_dramatico.class, Gigantic_michi_dramatico::new);
        regGigante("gigante_gatitoalien_gold", "gatitoalien_gold", Gigantic_gatitoalien_gold.class, Gigantic_gatitoalien_gold::new);
        regGigante("gigante_gatitoalien_sunglasses", "gatitoalien_sunglasses", Gigantic_gatitoalien_sunglasses.class, Gigantic_gatitoalien_sunglasses::new);
        regGigante("gigante_gatitoalien_beach_hat", "gatitoalien_beach_hat", Gigantic_gatitoalien_beach_hat.class, Gigantic_gatitoalien_beach_hat::new);
        regGigante("gigante_gatitoalien_crown", "gatitoalien_crown", Gigantic_gatitoalien_crown.class, Gigantic_gatitoalien_crown::new);
        regGigante("gigante_gatitoalien_bow", "gatitoalien_bow", Gigantic_gatitoalien_bow.class, Gigantic_gatitoalien_bow::new);
        regGigante("gigante_gatitoalien_headphones", "gatitoalien_headphones", Gigantic_gatitoalien_headphones.class, Gigantic_gatitoalien_headphones::new);
        regGigante("gigante_gatitoalien_scarf", "gatitoalien_scarf", Gigantic_gatitoalien_scarf.class, Gigantic_gatitoalien_scarf::new);
        regGigante("gigante_gatitoalien_halo", "gatitoalien_halo", Gigantic_gatitoalien_halo.class, Gigantic_gatitoalien_halo::new);
        regGigante("gigante_gatitoalien_devil_horns", "gatitoalien_devil_horns", Gigantic_gatitoalien_devil_horns.class, Gigantic_gatitoalien_devil_horns::new);
        regGigante("gigante_gatitoalien_witch_hat", "gatitoalien_witch_hat", Gigantic_gatitoalien_witch_hat.class, Gigantic_gatitoalien_witch_hat::new);
        regGigante("gigante_gatitoalien_santa_hat", "gatitoalien_santa_hat", Gigantic_gatitoalien_santa_hat.class, Gigantic_gatitoalien_santa_hat::new);
        regGigante("gigante_gatitoalien_flower_crown", "gatitoalien_flower_crown", Gigantic_gatitoalien_flower_crown.class, Gigantic_gatitoalien_flower_crown::new);
        regGigante("gigante_gatitoalien_pirate", "gatitoalien_pirate", Gigantic_gatitoalien_pirate.class, Gigantic_gatitoalien_pirate::new);
        regGigante("gigante_gatitoalien_gold_sunglasses", "gatitoalien_gold_sunglasses", Gigantic_gatitoalien_gold_sunglasses.class, Gigantic_gatitoalien_gold_sunglasses::new);
        regGigante("gigante_gatitoalien_gold_beach_hat", "gatitoalien_gold_beach_hat", Gigantic_gatitoalien_gold_beach_hat.class, Gigantic_gatitoalien_gold_beach_hat::new);
        regGigante("gigante_gatitoalien_gold_crown", "gatitoalien_gold_crown", Gigantic_gatitoalien_gold_crown.class, Gigantic_gatitoalien_gold_crown::new);
        regGigante("gigante_gatitoalien_gold_bow", "gatitoalien_gold_bow", Gigantic_gatitoalien_gold_bow.class, Gigantic_gatitoalien_gold_bow::new);
        regGigante("gigante_gatitoalien_gold_headphones", "gatitoalien_gold_headphones", Gigantic_gatitoalien_gold_headphones.class, Gigantic_gatitoalien_gold_headphones::new);
        regGigante("gigante_gatitoalien_gold_scarf", "gatitoalien_gold_scarf", Gigantic_gatitoalien_gold_scarf.class, Gigantic_gatitoalien_gold_scarf::new);
        regGigante("gigante_gatitoalien_gold_halo", "gatitoalien_gold_halo", Gigantic_gatitoalien_gold_halo.class, Gigantic_gatitoalien_gold_halo::new);
        regGigante("gigante_gatitoalien_gold_devil_horns", "gatitoalien_gold_devil_horns", Gigantic_gatitoalien_gold_devil_horns.class, Gigantic_gatitoalien_gold_devil_horns::new);
        regGigante("gigante_gatitoalien_gold_witch_hat", "gatitoalien_gold_witch_hat", Gigantic_gatitoalien_gold_witch_hat.class, Gigantic_gatitoalien_gold_witch_hat::new);
        regGigante("gigante_gatitoalien_gold_santa_hat", "gatitoalien_gold_santa_hat", Gigantic_gatitoalien_gold_santa_hat.class, Gigantic_gatitoalien_gold_santa_hat::new);
        regGigante("gigante_gatitoalien_gold_flower_crown", "gatitoalien_gold_flower_crown", Gigantic_gatitoalien_gold_flower_crown.class, Gigantic_gatitoalien_gold_flower_crown::new);
        regGigante("gigante_gatitoalien_gold_pirate", "gatitoalien_gold_pirate", Gigantic_gatitoalien_gold_pirate.class, Gigantic_gatitoalien_gold_pirate::new);
    }
}
