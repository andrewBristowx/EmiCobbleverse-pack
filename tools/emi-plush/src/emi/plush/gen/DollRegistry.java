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
        reg("happiny_emi", Doll_happiny.class, Doll_happiny::new);
        reg("chansey_emi", Doll_chansey.class, Doll_chansey::new);
        reg("blissey_emi", Doll_blissey.class, Doll_blissey::new);
        reg("cleffa_emi", Doll_cleffa.class, Doll_cleffa::new);
        reg("clefairy_emi", Doll_clefairy.class, Doll_clefairy::new);
        reg("clefable_emi", Doll_clefable.class, Doll_clefable::new);
        reg("igglybuff_emi", Doll_igglybuff.class, Doll_igglybuff::new);
        reg("jigglypuff_emi", Doll_jigglypuff.class, Doll_jigglypuff::new);
        reg("wigglytuff_emi", Doll_wigglytuff.class, Doll_wigglytuff::new);
        reg("eevee_emi", Doll_eevee.class, Doll_eevee::new);
        reg("vaporeon_emi", Doll_vaporeon.class, Doll_vaporeon::new);
        reg("jolteon_emi", Doll_jolteon.class, Doll_jolteon::new);
        reg("flareon_emi", Doll_flareon.class, Doll_flareon::new);
        reg("espeon_emi", Doll_espeon.class, Doll_espeon::new);
        reg("umbreon_emi", Doll_umbreon.class, Doll_umbreon::new);
        reg("leafeon_emi", Doll_leafeon.class, Doll_leafeon::new);
        reg("glaceon_emi", Doll_glaceon.class, Doll_glaceon::new);
        reg("sylveon_emi", Doll_sylveon.class, Doll_sylveon::new);
        reg("ralts_emi", Doll_ralts.class, Doll_ralts::new);
        reg("kirlia_emi", Doll_kirlia.class, Doll_kirlia::new);
        reg("gardevoir_emi", Doll_gardevoir.class, Doll_gardevoir::new);
    }
}
