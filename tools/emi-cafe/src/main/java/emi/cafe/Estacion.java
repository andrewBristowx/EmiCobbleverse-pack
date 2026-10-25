package emi.cafe;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

/**
 * Las estaciones de trabajo. {@code Tipo.CAJA} da un ingrediente; {@code PROCESO} recibe un objeto, lo transforma y lo deja listo para recoger ({@link Recetas});
 * {@code EMPLATAR} junta dos objetos; {@code FREGADERO} lava platos sucios (cola) y los devuelve a la pila de platos limpios.
 */
enum Estacion {
    BASURA("Basura", CafeBlocks.BASURA, Tipo.BASURA, 1, 1, 0, null, true),
    CAJA_PAPAS("Caja de papas", CafeBlocks.CAJA_PAPAS, Tipo.CAJA, 3, 1, 0, CafeItems.PAPA, false),
    CAJA_BAYAS("Caja de bayas", CafeBlocks.CAJA_BAYAS, Tipo.CAJA, 4, 1, 0, CafeItems.BAYA, false),
    CAJA_LECHUGA("Caja de lechugas", CafeBlocks.CAJA_LECHUGA, Tipo.CAJA, 5, 1, 0, CafeItems.LECHUGA, false),
    TABLA("Tabla de cortar", CafeBlocks.TABLA, Tipo.PROCESO, 7, 1, 60, null, false),
    FREIDORA("Freidora", CafeBlocks.FREIDORA, Tipo.PROCESO, 9, 1, 120, null, false),
    EXPRIMIDOR("Exprimidor", CafeBlocks.EXPRIMIDOR, Tipo.PROCESO, 10, 1, 80, null, false),
    EMPLATAR("Mesa de emplatar", CafeBlocks.EMPLATAR, Tipo.EMPLATAR, 12, 1, 60, null, false),
    FREGADERO("Fregadero", CafeBlocks.FREGADERO, Tipo.FREGADERO, 13, 1, 80, null, true),
    // la segunda fila de la cocina (z = 5, pegada a la barra; los huecos x = 7-8 quedan libres para pasar al comedor)
    CAJA_HUEVOS("Caja de huevos", CafeBlocks.CAJA_HUEVOS, Tipo.CAJA, 1, 5, 0, CafeItems.HUEVO, false),
    CAJA_HARINA("Saco de harina", CafeBlocks.CAJA_HARINA, Tipo.CAJA, 2, 5, 0, CafeItems.HARINA, false),
    CAJA_CARNE("Caja de carne", CafeBlocks.CAJA_CARNE, Tipo.CAJA, 3, 5, 0, CafeItems.CARNE, false),
    CAJA_LECHE("Caja de leche", CafeBlocks.CAJA_LECHE, Tipo.CAJA, 4, 5, 0, CafeItems.LECHE, false),
    CAJA_GRANOS("Saco de granos de café", CafeBlocks.CAJA_GRANOS, Tipo.CAJA, 5, 5, 0, CafeItems.GRANOS, false),
    CAJA_QUESO("Caja de queso", CafeBlocks.CAJA_QUESO, Tipo.CAJA, 6, 5, 0, CafeItems.QUESO, false),
    PLANCHA("Plancha", CafeBlocks.PLANCHA, Tipo.PROCESO, 9, 5, 100, null, false),
    OLLA("Olla", CafeBlocks.OLLA, Tipo.PROCESO, 10, 5, 140, null, false),
    HORNO("Horno", CafeBlocks.HORNO, Tipo.PROCESO, 11, 5, 200, null, false),
    BATIDORA("Batidora", CafeBlocks.BATIDORA, Tipo.PROCESO, 12, 5, 70, null, false),
    CAFETERA("Cafetera", CafeBlocks.CAFETERA, Tipo.PROCESO, 13, 5, 90, null, false),
    // las cuatro de la 0.11.0 (se ponen en los huecos libres de la fila de atras, que existen desde el local de nivel 1)
    CAJA_CALABAZA("Caja de calabazas", CafeBlocks.CAJA_CALABAZA, Tipo.CAJA, 2, 1, 0, CafeItems.CALABAZA, false),
    CAJA_ARROZ("Saco de arroz", CafeBlocks.CAJA_ARROZ, Tipo.CAJA, 6, 1, 0, CafeItems.ARROZ, false),
    HELADERA("Heladera", CafeBlocks.HELADERA, Tipo.PROCESO, 8, 1, 100, null, false),
    WAFLERA("Waflera", CafeBlocks.WAFLERA, Tipo.PROCESO, 11, 1, 90, null, false),
    // las ocho del local de nivel 6 a 10 (0.20.0; en un cafe de antes van al final de las dos filas de la cocina)
    CAJA_TOMATE("Caja de tomates", CafeBlocks.CAJA_TOMATE, Tipo.CAJA, 14, 1, 0, CafeItems.TOMATE, false),
    CAJA_PESCADO("Caja de pescado", CafeBlocks.CAJA_PESCADO, Tipo.CAJA, 15, 1, 0, CafeItems.PESCADO, false),
    CAJA_CACAO("Saco de cacao", CafeBlocks.CAJA_CACAO, Tipo.CAJA, 16, 1, 0, CafeItems.CACAO, false),
    CAJA_MIEL("Tarro de miel", CafeBlocks.CAJA_MIEL, Tipo.CAJA, 17, 1, 0, CafeItems.MIEL, false),
    CAJA_CEBOLLA("Caja de cebollas", CafeBlocks.CAJA_CEBOLLA, Tipo.CAJA, 18, 1, 0, CafeItems.CEBOLLA, false),
    PARRILLA("Parrilla", CafeBlocks.PARRILLA, Tipo.PROCESO, 14, 5, 140, null, false),
    CREPERA("Crepera", CafeBlocks.CREPERA, Tipo.PROCESO, 15, 5, 90, null, false),
    VAPORERA("Vaporera", CafeBlocks.VAPORERA, Tipo.PROCESO, 16, 5, 130, null, false),
    FUENTE_CHOCOLATE("Fuente de chocolate", CafeBlocks.FUENTE_CHOCOLATE, Tipo.PROCESO, 17, 5, 100, null, false),
    MESA("Mesa", CafeBlocks.MESA, Tipo.MESA, -1, 0, 0, null, true),
    REGISTRADORA("Caja registradora", CafeBlocks.REGISTRADORA, Tipo.REGISTRADORA, -1, 0, 0, null, true);

    enum Tipo { CAJA, PROCESO, EMPLATAR, FREGADERO, BASURA, MESA, REGISTRADORA }

    final String nombre;
    final Block bloque;
    final Tipo tipo;
    final int x, z;                       // su hueco en la cocina (x y z del local; las dos filas son z = 1 y z = 5); x = -1 = no va en la cocina
    final int ticks;                      // lo que tarda en procesar (a velocidad normal)
    final Item ingrediente;               // lo que da una caja
    final boolean siempre;                // viene con el cafe (no hace falta comprar un plato que la use)

    Estacion(String nombre, Block bloque, Tipo tipo, int x, int z, int ticks, Item ingrediente, boolean siempre) {
        this.nombre = nombre; this.bloque = bloque; this.tipo = tipo; this.x = x; this.z = z; this.ticks = ticks; this.ingrediente = ingrediente; this.siempre = siempre;
    }

    /** Que estacion es un bloque (null si no es ninguna). */
    static Estacion de(Block b) {
        if (CafeBlocks.esMesa(b)) return MESA;                          // los diseños de mesa (0.19.0) son mesas
        for (Estacion e : values()) if (e.bloque == b) return e;
        return null;
    }

    /** Altura (sobre el bloque) a la que se dibuja lo que hay dentro y su barra. */
    double alturaVisual() { return this == FREIDORA ? 1.1 : this == EXPRIMIDOR || this == BATIDORA || this == CAFETERA ? 1.3 : this == OLLA ? 1.0 : this == HORNO ? 1.1 : 0.85; }

    /** ¿Se quema lo que se deja esperando? (la freidora y el horno; la plancha no) */
    boolean quema() { return this == FREIDORA || this == HORNO || this == PARRILLA; }

    /** Lo que sale cuando se quema. */
    Item quemado() { return this == HORNO || this == PARRILLA ? CafeItems.COMIDA_QUEMADA : CafeItems.PAPAS_QUEMADAS; }
}
