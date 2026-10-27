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
    private static final java.util.List<Item> ORDEN = new java.util.ArrayList<>();          // en el orden en que se registran

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
    public static final Item QUESO = reg("queso", "Queso");
    public static final Item PIZZA_CRUDA = reg("pizza_cruda", "Pizza cruda");
    public static final Item PIZZA = reg("pizza", "Pizza");
    public static final Item SANDWICH = reg("sandwich", "Sándwich de queso");
    public static final Item TOSTADAS = reg("tostadas", "Tostadas");
    public static final Item PAPAS_QUESO = reg("papas_queso", "Papas con queso");
    public static final Item CHOCOLATE = reg("chocolate", "Chocolate caliente");
    public static final Item TE = reg("te", "Té de bayas");
    public static final Item HUEVOS_FRITOS = reg("huevos_fritos", "Huevos fritos");
    public static final Item ESTOFADO = reg("estofado", "Estofado");
    public static final Item DONAS = reg("donas", "Donas");
    public static final Item CALABAZA = reg("calabaza", "Calabaza");
    public static final Item CALABAZA_ASADA = reg("calabaza_asada", "Calabaza asada");
    public static final Item ARROZ = reg("arroz", "Arroz");
    public static final Item ARROZ_COCIDO = reg("arroz_cocido", "Arroz cocido");
    public static final Item SOPA_CALABAZA = reg("sopa_calabaza", "Sopa de calabaza");
    public static final Item CALABAZA_GRATINADA = reg("calabaza_gratinada", "Calabaza gratinada");
    public static final Item HELADO = reg("helado", "Helado");
    public static final Item HELADO_BAYAS = reg("helado_bayas", "Helado de bayas");
    public static final Item WAFFLES = reg("waffles", "Waffles");
    public static final Item ARROZ_CARNE = reg("arroz_carne", "Bol de arroz con carne");
    public static final Item ARROZ_LECHE = reg("arroz_leche", "Arroz con leche");
    public static final Item SUNDAE = reg("sundae", "Waffle con helado");
    public static final Item EV_NAVIDENO = reg("ev_navideno", "Leche y galletas");
    public static final Item EV_BRUJITA = reg("ev_brujita", "Poción de bayas");
    public static final Item EV_CALABAZA = reg("ev_calabaza", "Tarta de calabaza");
    public static final Item EV_CHEF = reg("ev_chef", "Menú del chef");
    public static final Item EV_DINO = reg("ev_dino", "Costillar jurásico");
    public static final Item EV_CARRITO = reg("ev_carrito", "Menú del carrito");
    public static final Item EV_EJECUTIVO = reg("ev_ejecutivo", "Almuerzo ejecutivo");
    public static final Item EV_GAFAS = reg("ev_gafas", "Batido tropical");
    public static final Item EV_PLAYERO = reg("ev_playero", "Granizado de playa");
    public static final Item EV_REY = reg("ev_rey", "Banquete real");
    public static final Item EV_LAZO = reg("ev_lazo", "Desayuno de lazos");
    public static final Item EV_DJ = reg("ev_dj", "Combo DJ");
    public static final Item EV_BUFANDA = reg("ev_bufanda", "Sopa con pan");
    public static final Item EV_ANGEL = reg("ev_angel", "Maná celestial");
    public static final Item EV_DIABLILLO = reg("ev_diablillo", "Hamburguesa infernal");
    public static final Item EV_FLORIDO = reg("ev_florido", "Ensalada floral");
    public static final Item EV_PIRATA = reg("ev_pirata", "Desayuno pirata");
    public static final Item EV_DORADO = reg("ev_dorado", "Festín dorado");
    // local de nivel 6 a 10 (0.20.0)
    public static final Item TOMATE = reg("tomate", "Tomate");
    public static final Item PESCADO = reg("pescado", "Pescado");
    public static final Item CACAO = reg("cacao", "Cacao");
    public static final Item MIEL = reg("miel", "Miel");
    public static final Item PESCADO_ASADO = reg("pescado_asado", "Pescado a la parrilla");
    public static final Item BISTEC = reg("bistec", "Bistec");
    public static final Item CHOCOLATE_FUNDIDO = reg("chocolate_fundido", "Chocolate fundido");
    public static final Item SOPA_TOMATE = reg("sopa_tomate", "Sopa de tomate");
    public static final Item CREPES = reg("crepes", "Crepes");
    public static final Item ONIGIRI = reg("onigiri", "Onigiri de pescado");
    public static final Item BAYAS_CHOCOLATE = reg("bayas_chocolate", "Bayas con chocolate");
    public static final Item PIZZA_MARGARITA = reg("pizza_margarita", "Pizza margarita");
    public static final Item CREPES_MIEL = reg("crepes_miel", "Crepes con miel");
    public static final Item PAN_MIEL = reg("pan_miel", "Pan con miel");
    public static final Item BISTEC_PAPAS = reg("bistec_papas", "Bistec con papas");
    public static final Item PESCADO_VAPOR = reg("pescado_vapor", "Pescado al vapor");
    public static final Item BOLLOS_VAPOR = reg("bollos_vapor", "Bollos al vapor");
    public static final Item PASTEL_CHOCOLATE = reg("pastel_chocolate", "Pastel de chocolate");
    public static final Item HELADO_CHOCOLATE = reg("helado_chocolate", "Copa de helado de chocolate");
    public static final Item MENU_GRAN_CHEF = reg("menu_gran_chef", "Menú del gran chef");
    // la granja (0.24.0)
    public static final Item CEBOLLA = reg("cebolla", "Cebolla");
    public static final Item CEBOLLA_PICADA = reg("cebolla_picada", "Cebolla picada");
    public static final Item SOPA_CEBOLLA = reg("sopa_cebolla", "Sopa de cebolla");
    public static final Item AROS_CEBOLLA = reg("aros_cebolla", "Aros de cebolla");
    public static final Item SOPA_CEBOLLA_GRATINADA = reg("sopa_cebolla_gratinada", "Sopa de cebolla gratinada");
    public static final Item ENSALADA_COMPLETA = reg("ensalada_completa", "Ensalada completa");
    // local de nivel 11 a 20 (0.36.0): ingredientes, intermedios y platos nuevos (los de temporada incluidos)
    public static final Item MANZANA = reg("manzana", "Manzana");
    public static final Item FRESA = reg("fresa", "Fresa");
    public static final Item COCO = reg("coco", "Coco");
    public static final Item CANELA = reg("canela", "Canela");
    public static final Item SETA = reg("seta", "Seta");
    public static final Item MAIZ = reg("maiz", "Maíz");
    public static final Item NATA = reg("nata", "Nata");
    public static final Item MASA_CANELA = reg("masa_canela", "Masa con canela");
    public static final Item FIDEOS = reg("fideos", "Fideos");
    public static final Item PALOMITAS = reg("palomitas", "Palomitas");
    public static final Item MANZANA_CARAMELO = reg("manzana_caramelo", "Manzana caramelizada");
    public static final Item GRANIZADO_FRESA = reg("granizado_fresa", "Granizado de fresa");
    public static final Item GRANIZADO_COCO = reg("granizado_coco", "Granizado de coco");
    public static final Item SOPA_MAIZ = reg("sopa_maiz", "Sopa de maíz");
    public static final Item ENSALADA_FRESA = reg("ensalada_fresa", "Ensalada de fresas");
    public static final Item SOPA_SETAS = reg("sopa_setas", "Sopa de setas");
    public static final Item CHOCOLATE_NATA = reg("chocolate_nata", "Chocolate con nata");
    public static final Item FRESAS_NATA = reg("fresas_nata", "Fresas con nata");
    public static final Item YAKISOBA = reg("yakisoba", "Fideos salteados");
    public static final Item ARROZ_FRITO = reg("arroz_frito", "Arroz frito");
    public static final Item SETAS_SALTEADAS = reg("setas_salteadas", "Setas salteadas");
    public static final Item TARTA_MANZANA = reg("tarta_manzana", "Tarta de manzana");
    public static final Item FRESAS_CARAMELO = reg("fresas_caramelo", "Fresas caramelizadas");
    public static final Item PESCADO_AHUMADO = reg("pescado_ahumado", "Pescado ahumado");
    public static final Item COSTILLAS = reg("costillas", "Costillas ahumadas");
    public static final Item BOCATA_COSTILLAS = reg("bocata_costillas", "Bocata de costillas");
    public static final Item ARROZ_COCO = reg("arroz_coco", "Arroz con coco");
    public static final Item COPA_FRESAS = reg("copa_fresas", "Copa de helado con fresas");
    public static final Item CALABAZA_CANELA = reg("calabaza_canela", "Calabaza con canela");
    public static final Item GALLETAS_CANELA = reg("galletas_canela", "Galletas de canela");
    public static final Item CAFE_CANELA = reg("cafe_canela", "Café con canela");
    public static final Item TARTA_FRESA = reg("tarta_fresa", "Tarta de fresas");
    public static final Item RAMEN = reg("ramen", "Ramen de setas");
    public static final Item ARROZ_SETAS = reg("arroz_setas", "Arroz frito con setas");
    public static final Item TARTA_NATA = reg("tarta_nata", "Tarta de fresas con nata");
    public static final Item PARRILLADA = reg("parrillada", "Parrillada del chef");
    public static final Item FESTIN_MAR = reg("festin_mar", "Festín del mar");
    public static final Item BANQUETE_POSTRES = reg("banquete_postres", "Banquete de postres");
    public static final Item MENU_LEYENDA = reg("menu_leyenda", "Menú de la leyenda");
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
    public static final Item ESTACIONES_ICONO = reg("estaciones_icono", "Más estaciones");
    public static final Item MESA_LUJO_ICONO = reg("mesa_lujo_icono", "Mesas elegantes");
    public static final Item PILA_PLATOS = reg("pila_platos", "Pila de platos");
    public static final Item MEGAFONO = reg("megafono", "Megáfono");
    public static final Item LIBRO = reg("libro", "Recetario");
    public static final Item INVITAR = reg("invitar", "Invitar");
    public static final Item CERRADO = reg("cerrado", "Cerrado");
    public static final Item ESTRELLA = reg("estrella", "Estrella del Café");
    public static final Item TROFEO = reg("trofeo", "Trofeo");
    public static final Item MISION = reg("mision", "Misión");
    public static final Item ICONO_HUERTO = reg("icono_huerto", "Huerto");
    public static final Item ICONO_CORRAL = reg("icono_corral", "Corral");
    public static final Item ICONO_DESPENSA = reg("icono_despensa", "Despensa");
    public static final Item ICONO_GRANJERO = reg("icono_granjero", "Michigranjero");

    private static Item reg(String id, String nombre) {
        Item i = Registry.register(Registries.ITEM, Identifier.of("emi_cafe", id), new Item(new Item.Settings().maxCount(1)));
        NOMBRES.put(i, nombre);
        ORDEN.add(i);
        return i;
    }

    static void init() { }

    /** Todo lo que se puede exponer en una vitrina: el plato limpio, los ingredientes y los platos (en el orden en que se registraron; sin lo quemado ni los iconos). */
    static java.util.List<Item> comidas() {
        java.util.List<Item> l = new java.util.ArrayList<>();
        l.add(PLATO_LIMPIO);
        for (Item i : ORDEN) { if (i == PLATO_SUCIO) break; if (i != PAPAS_QUEMADAS && i != COMIDA_QUEMADA) l.add(i); }
        return l;
    }

    /** El identificador de un objeto en texto (para los displays), p. ej. {@code emi_cafe:papa}. */
    static String id(Item i) { return Registries.ITEM.getId(i).toString(); }

    /** El nombre en español para los mensajes. */
    static String nombre(Item i) { return NOMBRES.getOrDefault(i, "algo"); }
}
