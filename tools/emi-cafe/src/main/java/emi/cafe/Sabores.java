package emi.cafe;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Regiones de la semana y temporadas del ano (0.32.0). Sin Minecraft.
 * <p>Region de la semana: cada semana toca una region (Kanto, Johto, Hoenn o Sinnoh). Los clientes de esa region vienen el doble de a menudo y pagan un 10 % mas, y sus platos tipicos se pagan un 30 % mas.
 * <p>Temporada: segun la fecha (Navidad, San Valentin, Primavera, Verano, Cosecha). Todo se paga un 8 % mas y los platos de la temporada un 40 % mas. La Noche de brujas sigue siendo un evento aparte.
 */
final class Sabores {
    private Sabores() {}

    enum Region {
        KANTO("Kanto", 1, 151, List.of("papas_fritas", "hamburguesa", "pizza_margarita", "sandwich")),
        JOHTO("Johto", 152, 251, List.of("te", "arroz_leche", "pan_miel", "onigiri")),
        HOENN("Hoenn", 252, 386, List.of("pescado_parrilla", "sopa_tomate", "bayas_chocolate", "pescado_vapor")),
        SINNOH("Sinnoh", 387, 493, List.of("sopa_cebolla", "estofado", "chocolate", "bistec"));

        final String nombre;
        final int desde, hasta;
        final List<String> platos;

        Region(String nombre, int desde, int hasta, List<String> platos) { this.nombre = nombre; this.desde = desde; this.hasta = hasta; this.platos = platos; }

        static Region de(String id) { for (Region r : values()) if (r.name().equalsIgnoreCase(id)) return r; return null; }
    }

    enum Temporada {
        NINGUNA("", "", List.of()),
        NAVIDAD("Navidad", "Todo se paga un 8 % más y el chocolate, los pasteles y lo de canela un 40 % más.", List.of("chocolate", "pastel", "pastel_chocolate", "panqueques", "te", "chocolate_nata", "galletas_canela", "cafe_canela", "tarta_manzana")),
        SAN_VALENTIN("San Valentín", "Todo se paga un 8 % más y lo dulce con chocolate o fresas un 40 % más.", List.of("bayas_chocolate", "helado_chocolate", "pastel_chocolate", "sundae", "crepes_miel", "fresas_nata", "fresas_caramelo", "tarta_fresa", "tarta_nata", "copa_fresas")),
        PRIMAVERA("Primavera", "Todo se paga un 8 % más y lo fresco y ligero (con fresas incluido) un 40 % más.", List.of("ensalada", "ensalada_completa", "limonada", "crepes", "batido", "ensalada_fresa", "setas_salteadas", "tarta_fresa", "fresas_nata")),
        VERANO("Verano", "Todo se paga un 8 % más y los helados, granizados y bebidas frías un 40 % más.", List.of("helado", "helado_bayas", "sundae", "batido", "limonada", "granizado_fresa", "granizado_coco", "arroz_coco", "copa_fresas", "palomitas")),
        COSECHA("Cosecha", "Todo se paga un 8 % más y lo de calabaza, cebolla, manzana, maíz y setas un 40 % más.", List.of("sopa_calabaza", "calabaza_gratinada", "sopa_cebolla", "arroz_leche", "sopa_cebolla_gratinada", "sopa_maiz", "tarta_manzana", "manzana_caramelo", "calabaza_canela", "sopa_setas"));

        final String nombre, descripcion;
        final List<String> platos;

        Temporada(String nombre, String descripcion, List<String> platos) { this.nombre = nombre; this.descripcion = descripcion; this.platos = platos; }

        static Temporada de(String id) { for (Temporada t : values()) if (t.name().equalsIgnoreCase(id)) return t; return null; }
    }

    static final int PAGO_REGION_PCT = 10, PLATO_REGION_PCT = 30, PAGO_TEMPORADA_PCT = 8, PLATO_TEMPORADA_PCT = 40;

    /** La region de la semana en que cae esa fecha (rota cada semana ISO). */
    static Region region(LocalDate f) {
        int semana = f.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return Region.values()[semana % Region.values().length];
    }

    static Temporada temporada(LocalDate f) {
        int m = f.getMonthValue(), d = f.getDayOfMonth();
        if ((m == 12 && d >= 15) || (m == 1 && d <= 6)) return Temporada.NAVIDAD;
        if (m == 2 && d >= 10 && d <= 16) return Temporada.SAN_VALENTIN;
        if ((m == 3 && d >= 20) || (m == 4 && d <= 20)) return Temporada.PRIMAVERA;
        if ((m == 6 && d >= 21) || m == 7 || m == 8) return Temporada.VERANO;
        if ((m == 9 && d >= 22) || (m == 10 && d <= 23)) return Temporada.COSECHA;
        return Temporada.NINGUNA;
    }

    /** Numero de Pokedex nacional de las especies que pueden venir de cliente (0 = sin region). */
    private static final Map<String, Integer> DEX = Map.ofEntries(
            Map.entry("rattata", 19), Map.entry("pidgey", 16), Map.entry("eevee", 133), Map.entry("pikachu", 25), Map.entry("slowpoke", 79), Map.entry("meowth", 52),
            Map.entry("psyduck", 54), Map.entry("jigglypuff", 39), Map.entry("snubbull", 209), Map.entry("growlithe", 58), Map.entry("vulpix", 37), Map.entry("mareep", 179),
            Map.entry("marill", 183), Map.entry("cubone", 104), Map.entry("teddiursa", 216), Map.entry("wooper", 194), Map.entry("shinx", 403), Map.entry("piplup", 393),
            Map.entry("buneary", 427), Map.entry("togepi", 175), Map.entry("skitty", 300), Map.entry("chikorita", 152), Map.entry("torchic", 255), Map.entry("mudkip", 258),
            Map.entry("treecko", 252), Map.entry("riolu", 447), Map.entry("munchlax", 446), Map.entry("pichu", 172), Map.entry("ponyta", 77), Map.entry("dratini", 147),
            Map.entry("oddish", 43), Map.entry("espeon", 196), Map.entry("umbreon", 197), Map.entry("vaporeon", 134), Map.entry("flareon", 136), Map.entry("jolteon", 135),
            Map.entry("leafeon", 470), Map.entry("persian", 53),
            Map.entry("sylveon", 700), Map.entry("glaceon", 471), Map.entry("lucario", 448), Map.entry("ralts", 280), Map.entry("delcatty", 301), Map.entry("roselia", 315),
            Map.entry("absol", 359), Map.entry("swablu", 333), Map.entry("cyndaquil", 155), Map.entry("totodile", 158), Map.entry("bidoof", 399), Map.entry("ditto", 132),
            Map.entry("mimikyu", 778), Map.entry("minccino", 572), Map.entry("audino", 531), Map.entry("fennekin", 653), Map.entry("froakie", 656), Map.entry("chespin", 650),
            Map.entry("rowlet", 722), Map.entry("litten", 725), Map.entry("popplio", 728), Map.entry("grookey", 810), Map.entry("scorbunny", 813), Map.entry("sobble", 816),
            Map.entry("pawmi", 921), Map.entry("fidough", 926), Map.entry("quaxly", 912), Map.entry("bulbasaur", 1), Map.entry("charmander", 4), Map.entry("squirtle", 7));

    static Region regionDe(String especie) {
        int n = DEX.getOrDefault(especie, 0);
        for (Region r : Region.values()) if (n >= r.desde && n <= r.hasta) return r;
        return null;
    }

    /** Cuantas veces mas probable es que venga esa especie con esa region de la semana (1 = igual). */
    static int pesoRegion(Region semana, String especie) { return semana != null && regionDe(especie) == semana ? 2 : 1; }
}
