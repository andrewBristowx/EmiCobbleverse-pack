package emi.plush.gen;

import dev.mrshawn.pokeblocks.block.custom.PokedollBlock;
import dev.mrshawn.pokeblocks.block.entity.BlockEntityTypeRegistry;
import dev.mrshawn.pokeblocks.block.entity.PokedollBlockEntity;
import dev.mrshawn.pokeblocks.item.DollRarity;
import dev.mrshawn.pokeblocks.item.custom.PokedollBlockItem;
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

/** Registro de todos los peluches Emi (generado por gen_dolls.py). */
public final class DollRegistry {
    private DollRegistry() {}

    public static final Map<String, class_2591<? extends PokedollBlockEntity>> TIPOS = new LinkedHashMap<>();
    public static final Map<String, class_1792> OBJETOS = new LinkedHashMap<>();

    private static <T extends PokedollBlockEntity> void reg(String nombre, Class<T> clase, BiFunction<class_2338, class_2680, T> ctor) {
        class_2960 id = class_2960.method_60655("emi_plush", nombre);
        class_2248 bloque = class_2378.method_10230(class_7923.field_41175, id, new PokedollBlock<T>(() -> clase));
        class_1792 objeto = class_2378.method_10230(class_7923.field_41178, id, new PokedollBlockItem(bloque, DollRarity.NONE, 0, () -> new EmiDollItemModel(nombre)));
        class_2591<T> tipo = class_2378.method_10230(class_7923.field_41181, id, FabricBlockEntityTypeBuilder.create(ctor::apply, bloque).build());
        BlockEntityTypeRegistry.register(clase, tipo);
        TIPOS.put(nombre, tipo);
        OBJETOS.put(nombre, objeto);
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
    }
}
