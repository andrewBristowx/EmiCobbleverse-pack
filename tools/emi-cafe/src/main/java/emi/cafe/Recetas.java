package emi.cafe;

import net.minecraft.item.Item;

import java.util.List;

/** Lo que se cocina: que sale de cada estacion, los platos que se pueden vender y las mejoras de la tienda. Sin estado: todo son tablas. */
final class Recetas {
    private Recetas() {}

    /** Lo que sale de procesar {@code entra} en una estacion de proceso (null si no lo acepta). */
    static Item procesa(Estacion e, Item entra) {
        switch (e) {
            case TABLA -> { if (entra == CafeItems.PAPA) return CafeItems.PAPA_TIRAS; if (entra == CafeItems.LECHUGA) return CafeItems.LECHUGA_PICADA; if (entra == CafeItems.CEBOLLA) return CafeItems.CEBOLLA_PICADA; }
            case FREIDORA -> { if (entra == CafeItems.PAPA_TIRAS) return CafeItems.PAPAS_FRITAS; if (entra == CafeItems.HUEVO) return CafeItems.HUEVOS_FRITOS; if (entra == CafeItems.MASA) return CafeItems.DONAS; if (entra == CafeItems.CEBOLLA_PICADA) return CafeItems.AROS_CEBOLLA; }
            case EXPRIMIDOR -> { if (entra == CafeItems.BAYA) return CafeItems.LIMONADA; }
            case PLANCHA -> { if (entra == CafeItems.PAN) return CafeItems.TOSTADAS; if (entra == CafeItems.HUEVO) return CafeItems.TORTILLA; if (entra == CafeItems.MASA) return CafeItems.PANQUEQUES; if (entra == CafeItems.CARNE) return CafeItems.CARNE_ASADA; }
            case OLLA -> { if (entra == CafeItems.CEBOLLA_PICADA) return CafeItems.SOPA_CEBOLLA; if (entra == CafeItems.TOMATE) return CafeItems.SOPA_TOMATE; if (entra == CafeItems.PAPA_TIRAS) return CafeItems.SOPA; if (entra == CafeItems.BAYA) return CafeItems.TE; if (entra == CafeItems.CARNE) return CafeItems.ESTOFADO; if (entra == CafeItems.CALABAZA) return CafeItems.SOPA_CALABAZA; if (entra == CafeItems.ARROZ) return CafeItems.ARROZ_COCIDO; if (entra == CafeItems.SETA) return CafeItems.SOPA_SETAS; if (entra == CafeItems.MAIZ) return CafeItems.SOPA_MAIZ; }
            case HORNO -> { if (entra == CafeItems.PIZZA_CRUDA) return CafeItems.PIZZA; if (entra == CafeItems.MASA_CANELA) return CafeItems.GALLETAS_CANELA; if (entra == CafeItems.MASA) return CafeItems.PASTEL; if (entra == CafeItems.HARINA) return CafeItems.PAN; if (entra == CafeItems.CALABAZA) return CafeItems.CALABAZA_ASADA; }
            case HELADERA -> { if (entra == CafeItems.LECHE) return CafeItems.HELADO; if (entra == CafeItems.BAYAS_LECHE) return CafeItems.HELADO_BAYAS; }
            case PARRILLA -> { if (entra == CafeItems.PESCADO) return CafeItems.PESCADO_ASADO; if (entra == CafeItems.CARNE) return CafeItems.BISTEC; }
            case CREPERA -> { if (entra == CafeItems.MASA) return CafeItems.CREPES; }
            case VAPORERA -> { if (entra == CafeItems.PESCADO) return CafeItems.PESCADO_VAPOR; if (entra == CafeItems.MASA) return CafeItems.BOLLOS_VAPOR; }
            case FUENTE_CHOCOLATE -> { if (entra == CafeItems.CACAO) return CafeItems.CHOCOLATE_FUNDIDO; }
            case WAFLERA -> { if (entra == CafeItems.MASA) return CafeItems.WAFFLES; }
            case BATIDORA -> { if (entra == CafeItems.BAYAS_LECHE) return CafeItems.BATIDO; }
            case CAFETERA -> { if (entra == CafeItems.GRANOS) return CafeItems.CAFE; if (entra == CafeItems.LECHE) return CafeItems.CHOCOLATE; }
            // local de nivel 11 a 20 (0.36.0)
            case WOK -> { if (entra == CafeItems.FIDEOS) return CafeItems.YAKISOBA; if (entra == CafeItems.ARROZ_COCIDO) return CafeItems.ARROZ_FRITO; if (entra == CafeItems.SETA) return CafeItems.SETAS_SALTEADAS; }
            case AHUMADOR -> { if (entra == CafeItems.PESCADO) return CafeItems.PESCADO_AHUMADO; if (entra == CafeItems.CARNE) return CafeItems.COSTILLAS; }
            case FIDEERA -> { if (entra == CafeItems.MASA) return CafeItems.FIDEOS; }
            case NATERA -> { if (entra == CafeItems.LECHE) return CafeItems.NATA; }
            case GRANIZADORA -> { if (entra == CafeItems.FRESA) return CafeItems.GRANIZADO_FRESA; if (entra == CafeItems.COCO) return CafeItems.GRANIZADO_COCO; }
            case CARAMELIZADORA -> { if (entra == CafeItems.MANZANA) return CafeItems.MANZANA_CARAMELO; if (entra == CafeItems.MAIZ) return CafeItems.PALOMITAS; if (entra == CafeItems.FRESA) return CafeItems.FRESAS_CARAMELO; }
            default -> { }
        }
        return null;
    }

    /** Las parejas que se juntan en la mesa de emplatar: {a, b, resultado} (el orden da igual). */
    private static final Item[][] BASE_COMBINACIONES = {
            {CafeItems.LECHUGA_PICADA, CafeItems.BAYA, CafeItems.ENSALADA},
            {CafeItems.HARINA, CafeItems.HUEVO, CafeItems.MASA},
            {CafeItems.CARNE_ASADA, CafeItems.PAN, CafeItems.HAMBURGUESA},
            {CafeItems.BAYA, CafeItems.LECHE, CafeItems.BAYAS_LECHE},
            {CafeItems.CAFE, CafeItems.LECHE, CafeItems.CAPUCHINO},
            {CafeItems.PAN, CafeItems.QUESO, CafeItems.SANDWICH},
            {CafeItems.MASA, CafeItems.QUESO, CafeItems.PIZZA_CRUDA},
            {CafeItems.PAPAS_FRITAS, CafeItems.QUESO, CafeItems.PAPAS_QUESO},
            {CafeItems.CALABAZA_ASADA, CafeItems.QUESO, CafeItems.CALABAZA_GRATINADA},
            {CafeItems.ARROZ_COCIDO, CafeItems.CARNE_ASADA, CafeItems.ARROZ_CARNE},
            {CafeItems.ARROZ_COCIDO, CafeItems.LECHE, CafeItems.ARROZ_LECHE},
            {CafeItems.WAFFLES, CafeItems.HELADO, CafeItems.SUNDAE},
            // local de nivel 6 a 10 (0.20.0)
            {CafeItems.ARROZ_COCIDO, CafeItems.PESCADO_ASADO, CafeItems.ONIGIRI},
            {CafeItems.BAYA, CafeItems.CHOCOLATE_FUNDIDO, CafeItems.BAYAS_CHOCOLATE},
            {CafeItems.PIZZA, CafeItems.TOMATE, CafeItems.PIZZA_MARGARITA},
            {CafeItems.CREPES, CafeItems.MIEL, CafeItems.CREPES_MIEL},
            {CafeItems.PAN, CafeItems.MIEL, CafeItems.PAN_MIEL},
            {CafeItems.BISTEC, CafeItems.PAPAS_FRITAS, CafeItems.BISTEC_PAPAS},
            {CafeItems.PASTEL, CafeItems.CHOCOLATE_FUNDIDO, CafeItems.PASTEL_CHOCOLATE},
            {CafeItems.HELADO, CafeItems.CHOCOLATE_FUNDIDO, CafeItems.HELADO_CHOCOLATE},
            {CafeItems.BISTEC_PAPAS, CafeItems.SOPA_TOMATE, CafeItems.MENU_GRAN_CHEF},
            // la granja (0.24.0): con la cebolla de Farmer's Delight y lo que ya habia
            {CafeItems.SOPA_CEBOLLA, CafeItems.QUESO, CafeItems.SOPA_CEBOLLA_GRATINADA},
            {CafeItems.ENSALADA, CafeItems.TOMATE, CafeItems.ENSALADA_COMPLETA},
            // local de nivel 11 a 20 (0.36.0)
            {CafeItems.ENSALADA, CafeItems.FRESA, CafeItems.ENSALADA_FRESA},
            {CafeItems.CHOCOLATE, CafeItems.NATA, CafeItems.CHOCOLATE_NATA},
            {CafeItems.FRESA, CafeItems.NATA, CafeItems.FRESAS_NATA},
            {CafeItems.PASTEL, CafeItems.MANZANA, CafeItems.TARTA_MANZANA},
            {CafeItems.PASTEL, CafeItems.FRESA, CafeItems.TARTA_FRESA},
            {CafeItems.TARTA_FRESA, CafeItems.NATA, CafeItems.TARTA_NATA},
            {CafeItems.PAN, CafeItems.COSTILLAS, CafeItems.BOCATA_COSTILLAS},
            {CafeItems.ARROZ_COCIDO, CafeItems.COCO, CafeItems.ARROZ_COCO},
            {CafeItems.HELADO, CafeItems.FRESA, CafeItems.COPA_FRESAS},
            {CafeItems.CALABAZA_ASADA, CafeItems.CANELA, CafeItems.CALABAZA_CANELA},
            {CafeItems.MASA, CafeItems.CANELA, CafeItems.MASA_CANELA},
            {CafeItems.CAFE, CafeItems.CANELA, CafeItems.CAFE_CANELA},
            {CafeItems.FIDEOS, CafeItems.SOPA_SETAS, CafeItems.RAMEN},
            {CafeItems.ARROZ_FRITO, CafeItems.SETAS_SALTEADAS, CafeItems.ARROZ_SETAS},
            {CafeItems.BISTEC, CafeItems.COSTILLAS, CafeItems.PARRILLADA},
            {CafeItems.PESCADO_AHUMADO, CafeItems.PESCADO_VAPOR, CafeItems.FESTIN_MAR},
            {CafeItems.TARTA_NATA, CafeItems.HELADO_CHOCOLATE, CafeItems.BANQUETE_POSTRES},
            {CafeItems.PARRILLADA, CafeItems.RAMEN, CafeItems.MENU_LEYENDA}};
    /** Las de arriba mas una por cada visitante especial. */
    private static final Item[][] COMBINACIONES;
    static {
        Especial[] es = Especial.values();
        COMBINACIONES = java.util.Arrays.copyOf(BASE_COMBINACIONES, BASE_COMBINACIONES.length + es.length);
        for (int i = 0; i < es.length; i++) COMBINACIONES[BASE_COMBINACIONES.length + i] = new Item[]{es[i].a, es[i].b, es[i].plato};
    }

    /** Lo que sale de juntar dos objetos en la mesa de emplatar (null si no combinan). */
    static Item combina(Item a, Item b) {
        for (Item[] c : COMBINACIONES) if ((a == c[0] && b == c[1]) || (a == c[1] && b == c[0])) return c[2];
        return null;
    }

    /** ¿Puede entrar este objeto a la mesa de emplatar (es parte de alguna receta)? */
    static boolean sirveParaEmplatar(Item i) {
        for (Item[] c : COMBINACIONES) if (i == c[0] || i == c[1]) return true;
        return false;
    }

    /** Los platos que se venden. {@code precio} es lo que paga un cliente; {@code coste} lo que cuesta aprenderlo en la tienda; {@code pasos} es la receta para el recetario. */
    enum Plato {
        PAPAS_FRITAS("papas_fritas", CafeItems.PAPAS_FRITAS, 12, 60, List.of(Estacion.CAJA_PAPAS, Estacion.TABLA, Estacion.FREIDORA), "Caja de papas → Tabla de cortar → Freidora"),
        LIMONADA("limonada", CafeItems.LIMONADA, 10, 80, List.of(Estacion.CAJA_BAYAS, Estacion.EXPRIMIDOR), "Caja de bayas → Exprimidor"),
        ENSALADA("ensalada", CafeItems.ENSALADA, 16, 120, List.of(Estacion.CAJA_LECHUGA, Estacion.CAJA_BAYAS, Estacion.TABLA, Estacion.EMPLATAR),
                "Lechuga → Tabla de cortar → Mesa de emplatar (con una baya)"),
        TORTILLA("tortilla", CafeItems.TORTILLA, 14, 100, List.of(Estacion.CAJA_HUEVOS, Estacion.PLANCHA), "Caja de huevos → Plancha"),
        CAFE("cafe", CafeItems.CAFE, 12, 90, List.of(Estacion.CAJA_GRANOS, Estacion.CAFETERA), "Saco de granos → Cafetera"),
        SOPA("sopa", CafeItems.SOPA, 19, 140, List.of(Estacion.CAJA_PAPAS, Estacion.TABLA, Estacion.OLLA), "Caja de papas → Tabla de cortar → Olla"),
        BATIDO("batido", CafeItems.BATIDO, 20, 160, List.of(Estacion.CAJA_BAYAS, Estacion.CAJA_LECHE, Estacion.EMPLATAR, Estacion.BATIDORA),
                "Baya + leche en la Mesa de emplatar → Batidora"),
        PANQUEQUES("panqueques", CafeItems.PANQUEQUES, 22, 160, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.PLANCHA),
                "Harina + huevo en la Mesa de emplatar (masa) → Plancha"),
        CAPUCHINO("capuchino", CafeItems.CAPUCHINO, 20, 200, List.of(Estacion.CAJA_GRANOS, Estacion.CAJA_LECHE, Estacion.CAFETERA, Estacion.EMPLATAR),
                "Granos → Cafetera; café + leche en la Mesa de emplatar"),
        HAMBURGUESA("hamburguesa", CafeItems.HAMBURGUESA, 30, 260, List.of(Estacion.CAJA_CARNE, Estacion.CAJA_HARINA, Estacion.PLANCHA, Estacion.HORNO, Estacion.EMPLATAR),
                "Carne → Plancha; harina → Horno (pan); carne asada + pan en la Mesa de emplatar"),
        PASTEL("pastel", CafeItems.PASTEL, 32, 300, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.HORNO),
                "Harina + huevo (masa) en la Mesa de emplatar → Horno. ¡Se quema si lo dejas!"),
        TE("te", CafeItems.TE, 13, 100, List.of(Estacion.CAJA_BAYAS, Estacion.OLLA), "Caja de bayas → Olla"),
        CHOCOLATE("chocolate", CafeItems.CHOCOLATE, 14, 110, List.of(Estacion.CAJA_LECHE, Estacion.CAFETERA), "Caja de leche → Cafetera"),
        HUEVOS_FRITOS("huevos_fritos", CafeItems.HUEVOS_FRITOS, 15, 110, List.of(Estacion.CAJA_HUEVOS, Estacion.FREIDORA), "Caja de huevos → Freidora (¡se quema!)"),
        TOSTADAS("tostadas", CafeItems.TOSTADAS, 18, 150, List.of(Estacion.CAJA_HARINA, Estacion.HORNO, Estacion.PLANCHA), "Saco de harina → Horno (pan) → Plancha"),
        PAPAS_QUESO("papas_queso", CafeItems.PAPAS_QUESO, 22, 190, List.of(Estacion.CAJA_PAPAS, Estacion.TABLA, Estacion.FREIDORA, Estacion.CAJA_QUESO, Estacion.EMPLATAR),
                "Papas fritas + queso en la Mesa de emplatar"),
        SANDWICH("sandwich", CafeItems.SANDWICH, 24, 220, List.of(Estacion.CAJA_HARINA, Estacion.HORNO, Estacion.CAJA_QUESO, Estacion.EMPLATAR),
                "Harina → Horno (pan); pan + queso en la Mesa de emplatar"),
        DONAS("donas", CafeItems.DONAS, 24, 210, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.FREIDORA),
                "Harina + huevo (masa) en la Mesa de emplatar → Freidora"),
        ESTOFADO("estofado", CafeItems.ESTOFADO, 26, 240, List.of(Estacion.CAJA_CARNE, Estacion.OLLA), "Caja de carne → Olla"),
        PIZZA("pizza", CafeItems.PIZZA, 34, 320, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.CAJA_QUESO, Estacion.EMPLATAR, Estacion.HORNO),
                "Masa (harina + huevo) + queso en la Mesa de emplatar → Horno"),
        HELADO("helado", CafeItems.HELADO, 16, 130, List.of(Estacion.CAJA_LECHE, Estacion.HELADERA), "Caja de leche → Heladera"),
        SOPA_CALABAZA("sopa_calabaza", CafeItems.SOPA_CALABAZA, 26, 230, List.of(Estacion.CAJA_CALABAZA, Estacion.OLLA), "Caja de calabazas → Olla"),
        WAFFLES("waffles", CafeItems.WAFFLES, 24, 200, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.WAFLERA),
                "Harina + huevo (masa) en la Mesa de emplatar → Waflera"),
        HELADO_BAYAS("helado_bayas", CafeItems.HELADO_BAYAS, 24, 220, List.of(Estacion.CAJA_BAYAS, Estacion.CAJA_LECHE, Estacion.EMPLATAR, Estacion.HELADERA),
                "Baya + leche en la Mesa de emplatar → Heladera"),
        ARROZ_LECHE("arroz_leche", CafeItems.ARROZ_LECHE, 22, 200, List.of(Estacion.CAJA_ARROZ, Estacion.OLLA, Estacion.CAJA_LECHE, Estacion.EMPLATAR),
                "Arroz → Olla; arroz cocido + leche en la Mesa de emplatar"),
        CALABAZA_GRATINADA("calabaza_gratinada", CafeItems.CALABAZA_GRATINADA, 34, 330, List.of(Estacion.CAJA_CALABAZA, Estacion.HORNO, Estacion.CAJA_QUESO, Estacion.EMPLATAR),
                "Calabaza → Horno; calabaza asada + queso en la Mesa de emplatar"),
        ARROZ_CARNE("arroz_carne", CafeItems.ARROZ_CARNE, 36, 340, List.of(Estacion.CAJA_ARROZ, Estacion.OLLA, Estacion.CAJA_CARNE, Estacion.PLANCHA, Estacion.EMPLATAR),
                "Arroz → Olla; carne → Plancha; arroz cocido + carne asada en la Mesa de emplatar"),
        SUNDAE("sundae", CafeItems.SUNDAE, 40, 380, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.WAFLERA, Estacion.CAJA_LECHE, Estacion.HELADERA),
                "Waffles (masa → Waflera) + helado (leche → Heladera) en la Mesa de emplatar"),
        // local de nivel 6 a 10 (0.20.0)
        SOPA_TOMATE("sopa_tomate", CafeItems.SOPA_TOMATE, 30, 400, List.of(Estacion.CAJA_TOMATE, Estacion.OLLA), "Caja de tomates → Olla"),
        PESCADO_PARRILLA("pescado_parrilla", CafeItems.PESCADO_ASADO, 32, 420, List.of(Estacion.CAJA_PESCADO, Estacion.PARRILLA), "Caja de pescado → Parrilla (¡se quema!)"),
        CREPES("crepes", CafeItems.CREPES, 34, 440, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.CREPERA),
                "Harina + huevo (masa) en la Mesa de emplatar → Crepera"),
        BISTEC("bistec", CafeItems.BISTEC, 40, 520, List.of(Estacion.CAJA_CARNE, Estacion.PARRILLA), "Caja de carne → Parrilla (¡se quema!)"),
        ONIGIRI("onigiri", CafeItems.ONIGIRI, 44, 560, List.of(Estacion.CAJA_ARROZ, Estacion.OLLA, Estacion.CAJA_PESCADO, Estacion.PARRILLA, Estacion.EMPLATAR),
                "Arroz → Olla; pescado → Parrilla; arroz cocido + pescado a la parrilla en la Mesa de emplatar"),
        BAYAS_CHOCOLATE("bayas_chocolate", CafeItems.BAYAS_CHOCOLATE, 42, 540, List.of(Estacion.CAJA_BAYAS, Estacion.CAJA_CACAO, Estacion.FUENTE_CHOCOLATE, Estacion.EMPLATAR),
                "Cacao → Fuente de chocolate; baya + chocolate fundido en la Mesa de emplatar"),
        PIZZA_MARGARITA("pizza_margarita", CafeItems.PIZZA_MARGARITA, 58, 700, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.CAJA_QUESO, Estacion.CAJA_TOMATE, Estacion.EMPLATAR, Estacion.HORNO),
                "Pizza (masa + queso en la Mesa de emplatar → Horno) + tomate en la Mesa de emplatar"),
        CREPES_MIEL("crepes_miel", CafeItems.CREPES_MIEL, 50, 640, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.CAJA_MIEL, Estacion.EMPLATAR, Estacion.CREPERA),
                "Crepes (masa → Crepera) + miel en la Mesa de emplatar"),
        PAN_MIEL("pan_miel", CafeItems.PAN_MIEL, 38, 480, List.of(Estacion.CAJA_HARINA, Estacion.HORNO, Estacion.CAJA_MIEL, Estacion.EMPLATAR),
                "Harina → Horno (pan); pan + miel en la Mesa de emplatar"),
        BISTEC_PAPAS("bistec_papas", CafeItems.BISTEC_PAPAS, 66, 820, List.of(Estacion.CAJA_CARNE, Estacion.PARRILLA, Estacion.CAJA_PAPAS, Estacion.TABLA, Estacion.FREIDORA, Estacion.EMPLATAR),
                "Carne → Parrilla; papas → Tabla → Freidora; bistec + papas fritas en la Mesa de emplatar"),
        PESCADO_VAPOR("pescado_vapor", CafeItems.PESCADO_VAPOR, 48, 620, List.of(Estacion.CAJA_PESCADO, Estacion.VAPORERA), "Caja de pescado → Vaporera"),
        BOLLOS_VAPOR("bollos_vapor", CafeItems.BOLLOS_VAPOR, 46, 600, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.VAPORERA),
                "Harina + huevo (masa) en la Mesa de emplatar → Vaporera"),
        PASTEL_CHOCOLATE("pastel_chocolate", CafeItems.PASTEL_CHOCOLATE, 76, 950, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.HORNO, Estacion.CAJA_CACAO, Estacion.FUENTE_CHOCOLATE),
                "Pastel (masa → Horno) + chocolate fundido (cacao → Fuente) en la Mesa de emplatar"),
        HELADO_CHOCOLATE("helado_chocolate", CafeItems.HELADO_CHOCOLATE, 64, 800, List.of(Estacion.CAJA_LECHE, Estacion.HELADERA, Estacion.CAJA_CACAO, Estacion.FUENTE_CHOCOLATE, Estacion.EMPLATAR),
                "Helado (leche → Heladera) + chocolate fundido (cacao → Fuente) en la Mesa de emplatar"),
        MENU_GRAN_CHEF("menu_gran_chef", CafeItems.MENU_GRAN_CHEF, 110, 1400, List.of(Estacion.CAJA_CARNE, Estacion.PARRILLA, Estacion.CAJA_PAPAS, Estacion.TABLA, Estacion.FREIDORA, Estacion.CAJA_TOMATE, Estacion.OLLA, Estacion.EMPLATAR),
                "Bistec con papas + sopa de tomate en la Mesa de emplatar"),
        // la granja (0.24.0): ingredientes de Farmer's Delight y platos nuevos con ellos
        SOPA_CEBOLLA("sopa_cebolla", CafeItems.SOPA_CEBOLLA, 30, 360, List.of(Estacion.CAJA_CEBOLLA, Estacion.TABLA, Estacion.OLLA), "Caja de cebollas → Tabla de cortar → Olla"),
        AROS_CEBOLLA("aros_cebolla", CafeItems.AROS_CEBOLLA, 34, 420, List.of(Estacion.CAJA_CEBOLLA, Estacion.TABLA, Estacion.FREIDORA), "Caja de cebollas → Tabla de cortar → Freidora (¡se quema!)"),
        ENSALADA_COMPLETA("ensalada_completa", CafeItems.ENSALADA_COMPLETA, 36, 440, List.of(Estacion.CAJA_LECHUGA, Estacion.CAJA_BAYAS, Estacion.TABLA, Estacion.CAJA_TOMATE, Estacion.EMPLATAR),
                "Lechuga → Tabla de cortar → Mesa de emplatar (con una baya) → ensalada; ensalada + tomate en la Mesa de emplatar"),
        SOPA_CEBOLLA_GRATINADA("sopa_cebolla_gratinada", CafeItems.SOPA_CEBOLLA_GRATINADA, 56, 640, List.of(Estacion.CAJA_CEBOLLA, Estacion.TABLA, Estacion.OLLA, Estacion.CAJA_QUESO, Estacion.EMPLATAR),
                "Sopa de cebolla (cebolla → Tabla → Olla) + queso en la Mesa de emplatar"),
        // local de nivel 11 a 20 (0.36.0); los de temporada nuevos van marcados en Sabores.Temporada
        PALOMITAS("palomitas", CafeItems.PALOMITAS, 46, 900, List.of(Estacion.CAJA_MAIZ, Estacion.CARAMELIZADORA),
                "Saco de maíz → Caramelizadora"),
        MANZANA_CARAMELO("manzana_caramelo", CafeItems.MANZANA_CARAMELO, 56, 1000, List.of(Estacion.CAJA_MANZANA, Estacion.CARAMELIZADORA),
                "Caja de manzanas → Caramelizadora"),
        GRANIZADO_FRESA("granizado_fresa", CafeItems.GRANIZADO_FRESA, 52, 950, List.of(Estacion.CAJA_FRESA, Estacion.GRANIZADORA),
                "Caja de fresas → Granizadora"),
        SOPA_MAIZ("sopa_maiz", CafeItems.SOPA_MAIZ, 60, 1050, List.of(Estacion.CAJA_MAIZ, Estacion.OLLA),
                "Saco de maíz → Olla"),
        ENSALADA_FRESA("ensalada_fresa", CafeItems.ENSALADA_FRESA, 64, 1150, List.of(Estacion.CAJA_LECHUGA, Estacion.CAJA_BAYAS, Estacion.TABLA, Estacion.CAJA_FRESA, Estacion.EMPLATAR),
                "Lechuga → Tabla de cortar → Mesa de emplatar (con una baya) → ensalada; ensalada + fresa en la Mesa de emplatar"),
        FIDEOS("fideos", CafeItems.FIDEOS, 72, 1300, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.FIDEERA),
                "Harina + huevo (masa) en la Mesa de emplatar → Fideera"),
        GRANIZADO_COCO("granizado_coco", CafeItems.GRANIZADO_COCO, 62, 1250, List.of(Estacion.CAJA_COCO, Estacion.GRANIZADORA),
                "Saco de cocos → Granizadora"),
        SOPA_SETAS("sopa_setas", CafeItems.SOPA_SETAS, 68, 1300, List.of(Estacion.CAJA_SETA, Estacion.OLLA),
                "Caja de setas → Olla"),
        CHOCOLATE_NATA("chocolate_nata", CafeItems.CHOCOLATE_NATA, 74, 1400, List.of(Estacion.CAJA_LECHE, Estacion.CAFETERA, Estacion.NATERA, Estacion.EMPLATAR),
                "Leche → Cafetera (chocolate); leche → Batidor de nata; chocolate + nata en la Mesa de emplatar"),
        FRESAS_NATA("fresas_nata", CafeItems.FRESAS_NATA, 70, 1350, List.of(Estacion.CAJA_FRESA, Estacion.CAJA_LECHE, Estacion.NATERA, Estacion.EMPLATAR),
                "Leche → Batidor de nata; fresa + nata en la Mesa de emplatar"),
        SETAS_SALTEADAS("setas_salteadas", CafeItems.SETAS_SALTEADAS, 76, 1600, List.of(Estacion.CAJA_SETA, Estacion.WOK),
                "Caja de setas → Wok (¡se quema!)"),
        ARROZ_FRITO("arroz_frito", CafeItems.ARROZ_FRITO, 86, 1700, List.of(Estacion.CAJA_ARROZ, Estacion.OLLA, Estacion.WOK),
                "Arroz → Olla; arroz cocido → Wok (¡se quema!)"),
        TARTA_MANZANA("tarta_manzana", CafeItems.TARTA_MANZANA, 100, 2000, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.HORNO, Estacion.CAJA_MANZANA),
                "Pastel (masa → Horno) + manzana en la Mesa de emplatar"),
        YAKISOBA("yakisoba", CafeItems.YAKISOBA, 94, 1900, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.FIDEERA, Estacion.WOK),
                "Fideos (masa → Fideera) → Wok (¡se quema!)"),
        FRESAS_CARAMELO("fresas_caramelo", CafeItems.FRESAS_CARAMELO, 74, 1750, List.of(Estacion.CAJA_FRESA, Estacion.CARAMELIZADORA),
                "Caja de fresas → Caramelizadora"),
        PESCADO_AHUMADO("pescado_ahumado", CafeItems.PESCADO_AHUMADO, 84, 1900, List.of(Estacion.CAJA_PESCADO, Estacion.AHUMADOR),
                "Caja de pescado → Ahumador"),
        COSTILLAS("costillas", CafeItems.COSTILLAS, 100, 2200, List.of(Estacion.CAJA_CARNE, Estacion.AHUMADOR),
                "Caja de carne → Ahumador"),
        CALABAZA_CANELA("calabaza_canela", CafeItems.CALABAZA_CANELA, 92, 2000, List.of(Estacion.CAJA_CALABAZA, Estacion.HORNO, Estacion.CAJA_CANELA, Estacion.EMPLATAR),
                "Calabaza → Horno; calabaza asada + canela en la Mesa de emplatar"),
        BOCATA_COSTILLAS("bocata_costillas", CafeItems.BOCATA_COSTILLAS, 124, 2700, List.of(Estacion.CAJA_HARINA, Estacion.HORNO, Estacion.CAJA_CARNE, Estacion.AHUMADOR, Estacion.EMPLATAR),
                "Harina → Horno (pan); carne → Ahumador (costillas); pan + costillas en la Mesa de emplatar"),
        ARROZ_COCO("arroz_coco", CafeItems.ARROZ_COCO, 92, 2200, List.of(Estacion.CAJA_ARROZ, Estacion.OLLA, Estacion.CAJA_COCO, Estacion.EMPLATAR),
                "Arroz → Olla; arroz cocido + coco en la Mesa de emplatar"),
        COPA_FRESAS("copa_fresas", CafeItems.COPA_FRESAS, 96, 2300, List.of(Estacion.CAJA_LECHE, Estacion.HELADERA, Estacion.CAJA_FRESA, Estacion.EMPLATAR),
                "Helado (leche → Heladera) + fresa en la Mesa de emplatar"),
        GALLETAS_CANELA("galletas_canela", CafeItems.GALLETAS_CANELA, 88, 2300, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.CAJA_CANELA, Estacion.EMPLATAR, Estacion.HORNO),
                "Masa (harina + huevo) + canela en la Mesa de emplatar → Horno"),
        CAFE_CANELA("cafe_canela", CafeItems.CAFE_CANELA, 80, 2000, List.of(Estacion.CAJA_GRANOS, Estacion.CAFETERA, Estacion.CAJA_CANELA, Estacion.EMPLATAR),
                "Granos → Cafetera; café + canela en la Mesa de emplatar"),
        TARTA_FRESA("tarta_fresa", CafeItems.TARTA_FRESA, 112, 2700, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.HORNO, Estacion.CAJA_FRESA),
                "Pastel (masa → Horno) + fresa en la Mesa de emplatar"),
        RAMEN("ramen", CafeItems.RAMEN, 136, 3300, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.FIDEERA, Estacion.CAJA_SETA, Estacion.OLLA),
                "Fideos (masa → Fideera) + sopa de setas (setas → Olla) en la Mesa de emplatar"),
        ARROZ_SETAS("arroz_setas", CafeItems.ARROZ_SETAS, 140, 3400, List.of(Estacion.CAJA_ARROZ, Estacion.OLLA, Estacion.WOK, Estacion.CAJA_SETA, Estacion.EMPLATAR),
                "Arroz frito (arroz → Olla → Wok) + setas salteadas (setas → Wok) en la Mesa de emplatar"),
        TARTA_NATA("tarta_nata", CafeItems.TARTA_NATA, 152, 3800, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.HORNO, Estacion.CAJA_FRESA, Estacion.CAJA_LECHE, Estacion.NATERA),
                "Tarta de fresas (pastel + fresa) + nata (leche → Batidor de nata) en la Mesa de emplatar"),
        PARRILLADA("parrillada", CafeItems.PARRILLADA, 180, 4500, List.of(Estacion.CAJA_CARNE, Estacion.PARRILLA, Estacion.AHUMADOR, Estacion.EMPLATAR),
                "Carne → Parrilla (bistec); carne → Ahumador (costillas); bistec + costillas en la Mesa de emplatar"),
        FESTIN_MAR("festin_mar", CafeItems.FESTIN_MAR, 172, 4400, List.of(Estacion.CAJA_PESCADO, Estacion.AHUMADOR, Estacion.VAPORERA, Estacion.EMPLATAR),
                "Pescado → Ahumador; pescado → Vaporera; los dos juntos en la Mesa de emplatar"),
        BANQUETE_POSTRES("banquete_postres", CafeItems.BANQUETE_POSTRES, 224, 5600, List.of(Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.HORNO, Estacion.CAJA_FRESA, Estacion.CAJA_LECHE, Estacion.NATERA, Estacion.HELADERA, Estacion.CAJA_CACAO, Estacion.FUENTE_CHOCOLATE),
                "Tarta de fresas con nata + helado de chocolate (helado + chocolate fundido) en la Mesa de emplatar"),
        MENU_LEYENDA("menu_leyenda", CafeItems.MENU_LEYENDA, 300, 7500, List.of(Estacion.CAJA_CARNE, Estacion.PARRILLA, Estacion.AHUMADOR, Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.EMPLATAR, Estacion.FIDEERA, Estacion.CAJA_SETA, Estacion.OLLA),
                "Parrillada (bistec + costillas) + ramen (fideos + sopa de setas) en la Mesa de emplatar");

        final String id;
        final Item item;
        final int precio, coste;
        final List<Estacion> estaciones;
        final String receta;

        Plato(String id, Item item, int precio, int coste, List<Estacion> estaciones, String receta) {
            this.id = id; this.item = item; this.precio = precio; this.coste = coste; this.estaciones = estaciones; this.receta = receta;
        }

        String nombre() { return CafeItems.nombre(item); }

        /** El nivel de local que hace falta para poder comprar este plato (el local se amplia en la tienda: nivel 1 a 20). */
        int nivelLocal() {
            return switch (this) {
                case PAPAS_FRITAS, LIMONADA, ENSALADA, TE, CAFE -> 1;
                case TORTILLA, CHOCOLATE, HUEVOS_FRITOS, SOPA, HELADO -> 2;
                case BATIDO, PANQUEQUES, TOSTADAS, ESTOFADO, DONAS, CAPUCHINO, SOPA_CALABAZA, WAFFLES -> 3;
                case HAMBURGUESA, PASTEL, PAPAS_QUESO, SANDWICH, HELADO_BAYAS, ARROZ_LECHE, CALABAZA_GRATINADA -> 4;
                case PIZZA, ARROZ_CARNE, SUNDAE -> 5;
                case SOPA_CEBOLLA -> 5;
                case SOPA_TOMATE, PESCADO_PARRILLA, CREPES, AROS_CEBOLLA, ENSALADA_COMPLETA -> 6;
                case BISTEC, ONIGIRI, BAYAS_CHOCOLATE -> 7;
                case PIZZA_MARGARITA, CREPES_MIEL, PAN_MIEL, SOPA_CEBOLLA_GRATINADA -> 8;
                case BISTEC_PAPAS, PESCADO_VAPOR, BOLLOS_VAPOR -> 9;
                case PASTEL_CHOCOLATE, HELADO_CHOCOLATE, MENU_GRAN_CHEF -> 10;
                case PALOMITAS, MANZANA_CARAMELO, GRANIZADO_FRESA, SOPA_MAIZ, ENSALADA_FRESA -> 11;
                case FIDEOS, GRANIZADO_COCO, SOPA_SETAS, CHOCOLATE_NATA, FRESAS_NATA -> 12;
                case SETAS_SALTEADAS, ARROZ_FRITO, TARTA_MANZANA, YAKISOBA -> 13;
                case FRESAS_CARAMELO, PESCADO_AHUMADO, COSTILLAS, CALABAZA_CANELA -> 14;
                case BOCATA_COSTILLAS, ARROZ_COCO, COPA_FRESAS -> 15;
                case GALLETAS_CANELA, CAFE_CANELA, TARTA_FRESA -> 16;
                case RAMEN, ARROZ_SETAS -> 17;
                case TARTA_NATA, PARRILLADA -> 18;
                case FESTIN_MAR, BANQUETE_POSTRES -> 19;
                case MENU_LEYENDA -> 20;
            };
        }

        static Plato de(String id) { for (Plato p : values()) if (p.id.equals(id)) return p; return null; }
        static Plato de(Item i) { for (Plato p : values()) if (p.item == i) return p; return null; }
    }

    /** Los tres cocineros con los que se empieza: cada uno te enseña un plato. */
    enum Inicial {
        CHARMANDER("charmander", "Charmander", CafeItems.COCINERO_CHARMANDER, Plato.PAPAS_FRITAS),
        SQUIRTLE("squirtle", "Squirtle", CafeItems.COCINERO_SQUIRTLE, Plato.LIMONADA),
        BULBASAUR("bulbasaur", "Bulbasaur", CafeItems.COCINERO_BULBASAUR, Plato.ENSALADA);

        final String id, nombre;
        final Item icono;
        final Plato plato;

        Inicial(String id, String nombre, Item icono, Plato plato) { this.id = id; this.nombre = nombre; this.icono = icono; this.plato = plato; }

        static Inicial de(String id) { for (Inicial i : values()) if (i.id.equals(id)) return i; return null; }
    }

    private static int[] n(int... v) { return v; }

    /**
     * Las mejoras de las instalaciones. {@code precios[k]} es lo que cuesta pasar del nivel k al k+1; {@code reqLocal[k]} el nivel de local (1 a 5) que hace falta para esa subida
     * y {@code reqRep[k]} el nivel de reputacion (0 = ninguno). Cuanto mas grande es el local, mas mejoras (y mas niveles de cada una) se pueden comprar.
     */
    enum Mejora {
        MESAS("mesas", "Más mesas", CafeItems.MESAS_ICONO, n(100, 200, 350, 550, 800), "Una mesa más: más clientes a la vez.", n(1, 2, 3, 4, 5), n(0, 0, 0, 0, 0)),
        LUJO("lujo", "Mesas elegantes", CafeItems.MESA_LUJO_ICONO, n(150, 300, 600), "Mesas más cuidadas: los clientes pagan un 6 % más por nivel.", n(1, 2, 3), n(0, 0, 0)),
        ESTACIONES("estaciones", "Más estaciones", CafeItems.ESTACIONES_ICONO, n(200, 400, 700, 1100, 1600), "Dos estaciones de cocina más por nivel (se suman al límite de tu local).", n(1, 2, 3, 4, 5), n(0, 0, 0, 0, 0)),
        VELOCIDAD("velocidad", "Estaciones más rápidas", CafeItems.RELOJ, n(150, 300, 600), "Las estaciones tardan un 20 % menos por nivel.", n(1, 2, 4), n(0, 0, 0)),
        PACIENCIA("paciencia", "Clientes más pacientes", CafeItems.CORAZON, n(120, 240), "Esperan un 25 % más por nivel antes de irse.", n(1, 3), n(0, 0)),
        VAJILLA("vajilla", "Más vajilla", CafeItems.PILA_PLATOS, n(100, 200, 400), "Dos platos limpios más en la pila por nivel.", n(1, 2, 4), n(0, 0, 0)),
        AMPLIAR("ampliar", "Ampliar el local", CafeItems.PUERTA, n(300, 600, 1000, 1600, 2400, 3400, 4600, 6000, 8000, 10500, 13500, 17000, 21000, 25500, 30500, 36000, 42000, 48500, 56000), "2 mesas, más estaciones, platos, personal y mejoras por nivel (hasta el nivel 20).",
                n(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19), n(2, 3, 4, 5, 6, 6, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)),
        PUBLICIDAD("publicidad", "Publicidad", CafeItems.MEGAFONO, n(120, 240, 480), "Llegan clientes más seguido.", n(1, 2, 4), n(0, 0, 0)),
        PROPINAS("propinas", "Buen servicio", CafeItems.MONEDA, n(300, 600, 1200), "Un 10 % más de propina por nivel.", n(2, 3, 5), n(0, 0, 0));

        final String id, nombre, descripcion;
        final Item icono;
        final int[] precios, reqLocal, reqRep;

        Mejora(String id, String nombre, Item icono, int[] precios, String descripcion, int[] reqLocal, int[] reqRep) {
            this.id = id; this.nombre = nombre; this.icono = icono; this.precios = precios; this.descripcion = descripcion; this.reqLocal = reqLocal; this.reqRep = reqRep;
        }

        int max() { return precios.length; }

        /** El nivel de local / de reputacion que hace falta para subir desde el nivel {@code nv} (0 si ya esta al maximo). */
        int reqLocal(int nv) { return nv >= max() ? 0 : reqLocal[nv]; }
        int reqRep(int nv) { return nv >= max() ? 0 : reqRep[nv]; }

        static Mejora de(String id) { for (Mejora m : values()) if (m.id.equals(id)) return m; return null; }
    }
}
