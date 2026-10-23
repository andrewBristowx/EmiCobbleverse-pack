package emi.cafe;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

/**
 * Las estaciones de trabajo. {@code Tipo.CAJA} da un ingrediente; {@code PROCESO} recibe un objeto, lo transforma y lo deja listo para recoger ({@link Recetas});
 * {@code EMPLATAR} junta dos objetos; {@code FREGADERO} lava platos sucios (cola) y los devuelve a la pila de platos limpios.
 */
enum Estacion {
    BASURA("Basura", CafeBlocks.BASURA, Tipo.BASURA, 1, 0, null, true),
    CAJA_PAPAS("Caja de papas", CafeBlocks.CAJA_PAPAS, Tipo.CAJA, 3, 0, CafeItems.PAPA, false),
    CAJA_BAYAS("Caja de bayas", CafeBlocks.CAJA_BAYAS, Tipo.CAJA, 4, 0, CafeItems.BAYA, false),
    CAJA_LECHUGA("Caja de lechugas", CafeBlocks.CAJA_LECHUGA, Tipo.CAJA, 5, 0, CafeItems.LECHUGA, false),
    TABLA("Tabla de cortar", CafeBlocks.TABLA, Tipo.PROCESO, 7, 60, null, false),
    FREIDORA("Freidora", CafeBlocks.FREIDORA, Tipo.PROCESO, 9, 120, null, false),
    EXPRIMIDOR("Exprimidor", CafeBlocks.EXPRIMIDOR, Tipo.PROCESO, 10, 80, null, false),
    EMPLATAR("Mesa de emplatar", CafeBlocks.EMPLATAR, Tipo.EMPLATAR, 12, 60, null, false),
    FREGADERO("Fregadero", CafeBlocks.FREGADERO, Tipo.FREGADERO, 13, 80, null, true),
    MESA("Mesa", CafeBlocks.MESA, Tipo.MESA, -1, 0, null, true),
    REGISTRADORA("Caja registradora", CafeBlocks.REGISTRADORA, Tipo.REGISTRADORA, -1, 0, null, true);

    enum Tipo { CAJA, PROCESO, EMPLATAR, FREGADERO, BASURA, MESA, REGISTRADORA }

    final String nombre;
    final Block bloque;
    final Tipo tipo;
    final int x;                          // su hueco en la fila de la cocina (x del local, z = 1); -1 = no va en la fila
    final int ticks;                      // lo que tarda en procesar (a velocidad normal)
    final Item ingrediente;               // lo que da una caja
    final boolean siempre;                // viene con el cafe (no hace falta comprar un plato que la use)

    Estacion(String nombre, Block bloque, Tipo tipo, int x, int ticks, Item ingrediente, boolean siempre) {
        this.nombre = nombre; this.bloque = bloque; this.tipo = tipo; this.x = x; this.ticks = ticks; this.ingrediente = ingrediente; this.siempre = siempre;
    }

    /** Que estacion es un bloque (null si no es ninguna). */
    static Estacion de(Block b) {
        for (Estacion e : values()) if (e.bloque == b) return e;
        return null;
    }

    /** Altura (sobre el bloque) a la que se dibuja lo que hay dentro y su barra. */
    double alturaVisual() { return this == FREIDORA ? 1.1 : this == EXPRIMIDOR ? 1.3 : 0.85; }
}
