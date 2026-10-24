package emi.cafe;

import net.minecraft.item.Item;

import java.util.List;

/** Lo que se cocina: que sale de cada estacion, los platos que se pueden vender y las mejoras de la tienda. Sin estado: todo son tablas. */
final class Recetas {
    private Recetas() {}

    /** Lo que sale de procesar {@code entra} en una estacion de proceso (null si no lo acepta). */
    static Item procesa(Estacion e, Item entra) {
        switch (e) {
            case TABLA -> { if (entra == CafeItems.PAPA) return CafeItems.PAPA_TIRAS; if (entra == CafeItems.LECHUGA) return CafeItems.LECHUGA_PICADA; }
            case FREIDORA -> { if (entra == CafeItems.PAPA_TIRAS) return CafeItems.PAPAS_FRITAS; if (entra == CafeItems.HUEVO) return CafeItems.HUEVOS_FRITOS; if (entra == CafeItems.MASA) return CafeItems.DONAS; }
            case EXPRIMIDOR -> { if (entra == CafeItems.BAYA) return CafeItems.LIMONADA; }
            case PLANCHA -> { if (entra == CafeItems.PAN) return CafeItems.TOSTADAS; if (entra == CafeItems.HUEVO) return CafeItems.TORTILLA; if (entra == CafeItems.MASA) return CafeItems.PANQUEQUES; if (entra == CafeItems.CARNE) return CafeItems.CARNE_ASADA; }
            case OLLA -> { if (entra == CafeItems.PAPA_TIRAS) return CafeItems.SOPA; if (entra == CafeItems.BAYA) return CafeItems.TE; if (entra == CafeItems.CARNE) return CafeItems.ESTOFADO; if (entra == CafeItems.CALABAZA) return CafeItems.SOPA_CALABAZA; if (entra == CafeItems.ARROZ) return CafeItems.ARROZ_COCIDO; }
            case HORNO -> { if (entra == CafeItems.PIZZA_CRUDA) return CafeItems.PIZZA; if (entra == CafeItems.MASA) return CafeItems.PASTEL; if (entra == CafeItems.HARINA) return CafeItems.PAN; if (entra == CafeItems.CALABAZA) return CafeItems.CALABAZA_ASADA; }
            case HELADERA -> { if (entra == CafeItems.LECHE) return CafeItems.HELADO; if (entra == CafeItems.BAYAS_LECHE) return CafeItems.HELADO_BAYAS; }
            case WAFLERA -> { if (entra == CafeItems.MASA) return CafeItems.WAFFLES; }
            case BATIDORA -> { if (entra == CafeItems.BAYAS_LECHE) return CafeItems.BATIDO; }
            case CAFETERA -> { if (entra == CafeItems.GRANOS) return CafeItems.CAFE; if (entra == CafeItems.LECHE) return CafeItems.CHOCOLATE; }
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
            {CafeItems.WAFFLES, CafeItems.HELADO, CafeItems.SUNDAE}};
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
                "Waffles (masa → Waflera) + helado (leche → Heladera) en la Mesa de emplatar");

        final String id;
        final Item item;
        final int precio, coste;
        final List<Estacion> estaciones;
        final String receta;

        Plato(String id, Item item, int precio, int coste, List<Estacion> estaciones, String receta) {
            this.id = id; this.item = item; this.precio = precio; this.coste = coste; this.estaciones = estaciones; this.receta = receta;
        }

        String nombre() { return CafeItems.nombre(item); }

        /** El nivel de local que hace falta para poder comprar este plato (el local se amplia en la tienda: nivel 1 a 5). */
        int nivelLocal() {
            return switch (this) {
                case PAPAS_FRITAS, LIMONADA, ENSALADA, TE, CAFE -> 1;
                case TORTILLA, CHOCOLATE, HUEVOS_FRITOS, SOPA, HELADO -> 2;
                case BATIDO, PANQUEQUES, TOSTADAS, ESTOFADO, DONAS, CAPUCHINO, SOPA_CALABAZA, WAFFLES -> 3;
                case HAMBURGUESA, PASTEL, PAPAS_QUESO, SANDWICH, HELADO_BAYAS, ARROZ_LECHE, CALABAZA_GRATINADA -> 4;
                case PIZZA, ARROZ_CARNE, SUNDAE -> 5;
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
        MESAS("mesas", "Más mesas", CafeItems.MESAS_ICONO, n(100, 200), "Una mesa más: más clientes a la vez.", n(1, 2), n(0, 0)),
        VELOCIDAD("velocidad", "Estaciones más rápidas", CafeItems.RELOJ, n(150, 300, 600), "Las estaciones tardan un 20 % menos por nivel.", n(1, 2, 4), n(0, 0, 0)),
        PACIENCIA("paciencia", "Clientes más pacientes", CafeItems.CORAZON, n(120, 240), "Esperan un 25 % más por nivel antes de irse.", n(1, 3), n(0, 0)),
        VAJILLA("vajilla", "Más vajilla", CafeItems.PILA_PLATOS, n(100, 200, 400), "Dos platos limpios más en la pila por nivel.", n(1, 2, 4), n(0, 0, 0)),
        AMPLIAR("ampliar", "Ampliar el local", CafeItems.PUERTA, n(300, 600, 1000, 1600), "8 bloques más de ancho, 2 mesas y más estaciones por nivel.", n(1, 2, 3, 4), n(2, 3, 4, 5)),
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
