package emi.cafe;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Los objetos del cafe (ingredientes, platos, iconos del menu). Cada uno tiene su textura en {@code assets/emi_cafe/textures/item}. */
public final class CafeItems {
    private CafeItems() {}

    private static final Map<Item, String> NOMBRES = new HashMap<>();

    public static final Item PAPA = reg("papa", "Papa");
    public static final Item PAPA_TIRAS = reg("papa_tiras", "Papa en tiras");
    public static final Item PAPAS_FRITAS = reg("papas_fritas", "Papas fritas");
    public static final Item PAPAS_QUEMADAS = reg("papas_quemadas", "Papas quemadas");
    public static final Item BAYA = reg("baya", "Baya");
    public static final Item LIMONADA = reg("limonada", "Limonada de bayas");
    public static final Item LECHUGA = reg("lechuga", "Lechuga");
    public static final Item LECHUGA_PICADA = reg("lechuga_picada", "Lechuga picada");
    public static final Item ENSALADA = reg("ensalada", "Ensalada de bayas");
    public static final Item HUEVO = reg("huevo", "Huevo");
    public static final Item HARINA = reg("harina", "Harina");
    public static final Item CARNE = reg("carne", "Carne");
    public static final Item LECHE = reg("leche", "Leche");
    public static final Item GRANOS = reg("granos", "Granos de café");
    public static final Item MASA = reg("masa", "Masa");
    public static final Item PAN = reg("pan", "Pan");
    public static final Item CARNE_ASADA = reg("carne_asada", "Carne asada");
    public static final Item BAYAS_LECHE = reg("bayas_leche", "Bayas con leche");
    public static final Item TORTILLA = reg("tortilla", "Tortilla");
    public static final Item PANQUEQUES = reg("panqueques", "Panqueques");
    public static final Item HAMBURGUESA = reg("hamburguesa", "Hamburguesa");
    public static final Item PASTEL = reg("pastel", "Pastel");
    public static final Item SOPA = reg("sopa", "Sopa de papas");
    public static final Item BATIDO = reg("batido", "Batido de bayas");
    public static final Item CAFE = reg("cafe", "Café");
    public static final Item CAPUCHINO = reg("capuchino", "Capuchino");
    public static final Item COMIDA_QUEMADA = reg("comida_quemada", "Comida quemada");
    public static final Item PLATO_SUCIO = reg("plato_sucio", "Plato sucio");
    public static final Item PLATO_LIMPIO = reg("plato_limpio", "Plato limpio");
    /** Solo se usan como dibujo (el globo de los pedidos y los iconos de los menus). */
    public static final Item BURBUJA = reg("burbuja", "Burbuja de pedido");
    public static final Item MONEDA = reg("moneda", "Moneda del Café");
    public static final Item COCINERO_CHARMANDER = reg("cocinero_charmander", "Charmander cocinero");
    public static final Item COCINERO_SQUIRTLE = reg("cocinero_squirtle", "Squirtle cocinero");
    public static final Item COCINERO_BULBASAUR = reg("cocinero_bulbasaur", "Bulbasaur cocinero");
    public static final Item CAMPANA = reg("campana", "Campana");
    public static final Item PUERTA = reg("puerta", "Puerta");
    public static final Item RELOJ = reg("reloj", "Reloj");
    public static final Item CORAZON = reg("corazon", "Corazón");
    public static final Item MESAS_ICONO = reg("mesas_icono", "Mesas");
    public static final Item PILA_PLATOS = reg("pila_platos", "Pila de platos");
    public static final Item MEGAFONO = reg("megafono", "Megáfono");
    public static final Item LIBRO = reg("libro", "Recetario");
    public static final Item INVITAR = reg("invitar", "Invitar");
    public static final Item CERRADO = reg("cerrado", "Cerrado");
    public static final Item ESTRELLA = reg("estrella", "Estrella del Café");
    public static final Item TROFEO = reg("trofeo", "Trofeo");
    public static final Item MISION = reg("mision", "Misión");

    private static Item reg(String id, String nombre) {
        Item i = Registry.register(Registries.ITEM, Identifier.of("emi_cafe", id), new Item(new Item.Settings().maxCount(1)));
        NOMBRES.put(i, nombre);
        return i;
    }

    static void init() { }

    /** El identificador de un objeto en texto (para los displays), p. ej. {@code emi_cafe:papa}. */
    static String id(Item i) { return Registries.ITEM.getId(i).toString(); }

    /** El nombre en español para los mensajes. */
    static String nombre(Item i) { return NOMBRES.getOrDefault(i, "algo"); }
}
