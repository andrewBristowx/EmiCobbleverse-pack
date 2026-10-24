package emi.cafe;

import net.minecraft.item.Item;

import java.util.EnumSet;
import java.util.Set;

/**
 * Los visitantes especiales: de vez en cuando entra un Michialien con su accesorio (el navideño, la brujita, el chef…), el café «aprende» su plato (te lo explica en el chat) y
 * te lo pide. El plato se hace juntando dos objetos en la mesa de emplatar. Solo vienen si el dueño ya tiene todas las estaciones que hacen falta para sus dos ingredientes.
 * {@code aspecto} son las propiedades de Cobblemon del GatitoAlien (las de emi-plush).
 */
enum Especial {
    NAVIDENO("navideno", "accessory=santa_hat", "Michialien Navideño", CafeItems.EV_NAVIDENO, CafeItems.LECHE, CafeItems.PAN, 40, Estacion.CAJA_LECHE, Estacion.CAJA_HARINA, Estacion.HORNO),
    BRUJITA("brujita", "accessory=witch_hat", "Michialien Brujita", CafeItems.EV_BRUJITA, CafeItems.TE, CafeItems.BAYAS_LECHE, 42, Estacion.CAJA_BAYAS, Estacion.OLLA, Estacion.CAJA_LECHE),
    CALABAZA("calabaza", "accessory=pumpkin", "Michialien Calabaza", CafeItems.EV_CALABAZA, CafeItems.PASTEL, CafeItems.CAFE, 50, Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.HORNO, Estacion.CAJA_GRANOS, Estacion.CAFETERA),
    CHEF("chef", "accessory=chef", "Michialien Chef", CafeItems.EV_CHEF, CafeItems.HAMBURGUESA, CafeItems.PAPAS_FRITAS, 70, Estacion.CAJA_CARNE, Estacion.CAJA_HARINA, Estacion.PLANCHA, Estacion.HORNO, Estacion.CAJA_PAPAS, Estacion.TABLA, Estacion.FREIDORA),
    DINO("dino", "accessory=dino", "Michialien Dino", CafeItems.EV_DINO, CafeItems.CARNE_ASADA, CafeItems.LECHUGA_PICADA, 55, Estacion.CAJA_CARNE, Estacion.PLANCHA, Estacion.CAJA_LECHUGA, Estacion.TABLA),
    CARRITO("carrito", "accessory=cart", "Michialien Carrito", CafeItems.EV_CARRITO, CafeItems.BATIDO, CafeItems.PAPAS_FRITAS, 55, Estacion.CAJA_BAYAS, Estacion.CAJA_LECHE, Estacion.BATIDORA, Estacion.CAJA_PAPAS, Estacion.TABLA, Estacion.FREIDORA),
    EJECUTIVO("ejecutivo", "accessory=suit", "Michialien Ejecutivo", CafeItems.EV_EJECUTIVO, CafeItems.CAPUCHINO, CafeItems.TORTILLA, 55, Estacion.CAJA_GRANOS, Estacion.CAFETERA, Estacion.CAJA_LECHE, Estacion.CAJA_HUEVOS, Estacion.PLANCHA),
    GAFAS("gafas", "accessory=sunglasses", "Michialien Gafas de Sol", CafeItems.EV_GAFAS, CafeItems.BATIDO, CafeItems.LIMONADA, 45, Estacion.CAJA_BAYAS, Estacion.CAJA_LECHE, Estacion.BATIDORA, Estacion.EXPRIMIDOR),
    PLAYERO("playero", "accessory=beach_hat", "Michialien Playero", CafeItems.EV_PLAYERO, CafeItems.LIMONADA, CafeItems.BAYAS_LECHE, 42, Estacion.CAJA_BAYAS, Estacion.EXPRIMIDOR, Estacion.CAJA_LECHE),
    REY("rey", "accessory=crown", "Michialien Rey", CafeItems.EV_REY, CafeItems.PASTEL, CafeItems.CAPUCHINO, 60, Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.HORNO, Estacion.CAJA_GRANOS, Estacion.CAFETERA, Estacion.CAJA_LECHE),
    LAZO("lazo", "accessory=bow", "Michialien Lazo", CafeItems.EV_LAZO, CafeItems.PANQUEQUES, CafeItems.BAYAS_LECHE, 50, Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.PLANCHA, Estacion.CAJA_BAYAS, Estacion.CAJA_LECHE),
    DJ("dj", "accessory=headphones", "Michialien DJ", CafeItems.EV_DJ, CafeItems.CAFE, CafeItems.HAMBURGUESA, 60, Estacion.CAJA_GRANOS, Estacion.CAFETERA, Estacion.CAJA_CARNE, Estacion.CAJA_HARINA, Estacion.PLANCHA, Estacion.HORNO),
    BUFANDA("bufanda", "accessory=scarf", "Michialien Bufanda", CafeItems.EV_BUFANDA, CafeItems.SOPA, CafeItems.PAN, 45, Estacion.CAJA_PAPAS, Estacion.TABLA, Estacion.OLLA, Estacion.CAJA_HARINA, Estacion.HORNO),
    ANGEL("angel", "accessory=halo", "Michialien Ángel", CafeItems.EV_ANGEL, CafeItems.PANQUEQUES, CafeItems.LECHE, 45, Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.PLANCHA, Estacion.CAJA_LECHE),
    DIABLILLO("diablillo", "accessory=devil_horns", "Michialien Diablillo", CafeItems.EV_DIABLILLO, CafeItems.HAMBURGUESA, CafeItems.BAYA, 55, Estacion.CAJA_CARNE, Estacion.CAJA_HARINA, Estacion.PLANCHA, Estacion.HORNO, Estacion.CAJA_BAYAS),
    FLORIDO("florido", "accessory=flower_crown", "Michialien Florido", CafeItems.EV_FLORIDO, CafeItems.ENSALADA, CafeItems.LIMONADA, 48, Estacion.CAJA_LECHUGA, Estacion.CAJA_BAYAS, Estacion.TABLA, Estacion.EXPRIMIDOR),
    PIRATA("pirata", "accessory=pirate", "Michialien Pirata", CafeItems.EV_PIRATA, CafeItems.TORTILLA, CafeItems.CARNE_ASADA, 55, Estacion.CAJA_HUEVOS, Estacion.PLANCHA, Estacion.CAJA_CARNE),
    DORADO("dorado", "gold=true", "Michialien Dorado", CafeItems.EV_DORADO, CafeItems.PASTEL, CafeItems.HAMBURGUESA, 90, Estacion.CAJA_HARINA, Estacion.CAJA_HUEVOS, Estacion.HORNO, Estacion.CAJA_CARNE, Estacion.PLANCHA);

    final String id, aspecto, michi;
    final Item plato, a, b;
    final int precio;
    final Set<Estacion> requiere = EnumSet.noneOf(Estacion.class);

    Especial(String id, String aspecto, String michi, Item plato, Item a, Item b, int precio, Estacion... req) {
        this.id = id; this.aspecto = aspecto; this.michi = michi; this.plato = plato; this.a = a; this.b = b; this.precio = precio;
        requiere.add(Estacion.EMPLATAR);
        for (Estacion e : req) requiere.add(e);
    }

    String nombrePlato() { return CafeItems.nombre(plato); }

    /** Como se hace, para el chat y el recetario. */
    String receta() { return CafeItems.nombre(a) + " + " + CafeItems.nombre(b) + " en la Mesa de emplatar"; }

    static Especial de(String id) { for (Especial e : values()) if (e.id.equals(id)) return e; return null; }
    static Especial de(Item plato) { for (Especial e : values()) if (e.plato == plato) return e; return null; }
}
