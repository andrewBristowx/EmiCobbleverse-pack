package emi.cafe;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

/** Las estaciones de trabajo y lo que hacen. Una estacion «de proceso» recibe un objeto, lo transforma en unos segundos y lo deja listo para recoger. */
enum Estacion {
    CAJA_PAPAS("Caja de papas", null, 0), TABLA("Tabla de cortar", CafeBlocks.TABLA, 60), FREIDORA("Freidora", CafeBlocks.FREIDORA, 120), BASURA("Basura", CafeBlocks.BASURA, 0), MESA("Mesa", CafeBlocks.MESA, 0);

    final String nombre;
    final Block bloque;
    final int ticks;                      // lo que tarda en procesar (0 = no procesa)

    Estacion(String nombre, Block bloque, int ticks) { this.nombre = nombre; this.bloque = bloque; this.ticks = ticks; }

    /** Que estacion es un bloque (null si no es ninguna). */
    static Estacion de(Block b) {
        if (b == CafeBlocks.CAJA_PAPAS) return CAJA_PAPAS;
        if (b == CafeBlocks.TABLA) return TABLA;
        if (b == CafeBlocks.FREIDORA) return FREIDORA;
        if (b == CafeBlocks.BASURA) return BASURA;
        if (b == CafeBlocks.MESA) return MESA;
        return null;
    }

    /** Lo que sale de procesar {@code entra} en esta estacion (null si no lo acepta). */
    Item resultado(Item entra) {
        if (this == TABLA && entra == CafeItems.PAPA) return CafeItems.PAPA_TIRAS;
        if (this == FREIDORA && entra == CafeItems.PAPA_TIRAS) return CafeItems.PAPAS_FRITAS;
        return null;
    }

    /** Altura (sobre el bloque) a la que se dibuja lo que hay dentro y su barra. */
    double alturaVisual() { return this == FREIDORA ? 1.1 : 0.85; }
}
