package emi.cafe;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Los objetos del cafe (ingredientes y platos). Cada uno tiene su textura en {@code assets/emi_cafe/textures/item}. */
public final class CafeItems {
    private CafeItems() {}

    public static final Item PAPA = reg("papa");
    public static final Item PAPA_TIRAS = reg("papa_tiras");
    public static final Item PAPAS_FRITAS = reg("papas_fritas");
    /** Solo se usa como dibujo (el globo de los pedidos y la moneda de la interfaz). */
    public static final Item BURBUJA = reg("burbuja");
    public static final Item MONEDA = reg("moneda");

    private static Item reg(String id) { return Registry.register(Registries.ITEM, Identifier.of("emi_cafe", id), new Item(new Item.Settings().maxCount(1))); }

    static void init() { }

    /** El identificador de un objeto en texto (para los displays), p. ej. {@code emi_cafe:papa}. */
    static String id(Item i) { return Registries.ITEM.getId(i).toString(); }

    /** El nombre en español para los mensajes. */
    static String nombre(Item i) {
        if (i == PAPA) return "una papa";
        if (i == PAPA_TIRAS) return "papa en tiras";
        if (i == PAPAS_FRITAS) return "papas fritas";
        return "algo";
    }
}
