package emi.cafe;

/**
 * Decoracion con funcion (0.32.0): unos pocos bloques de la tienda DECORAR (grupo «Funcional») dan una ventaja al cafe mientras esten puestos. Cada uno tiene un tope de unidades que cuentan. Sin Minecraft.
 */
enum Funcional {
    CAJA_MUSICA("minecraft:note_block", "Caja de música", "Los clientes aguantan más esperando: +8 % de paciencia cada una.", 3, 200),
    TARTA("minecraft:cake", "Tarta de exhibición", "Todo se paga un 3 % más por cada tarta.", 3, 250),
    MACETERO("minecraft:flower_pot", "Macetero", "Más propinas: +4 % por cada macetero.", 5, 80),
    TELAR("minecraft:loom", "Telar del personal", "Al cerrar la jornada el personal gana +3 de humor por cada telar.", 2, 300),
    MESA_MAESTRO("minecraft:smithing_table", "Mesa del maestro", "La maestría de los platos sube el doble de rápido.", 1, 400);

    final String bloque, nombre, descripcion;
    /** Cuantas unidades cuentan como maximo, y lo que cuesta cada una en la tienda. */
    final int max, precio;

    Funcional(String bloque, String nombre, String descripcion, int max, int precio) { this.bloque = bloque; this.nombre = nombre; this.descripcion = descripcion; this.max = max; this.precio = precio; }

    static Funcional de(String bloque) { for (Funcional f : values()) if (f.bloque.equals(bloque)) return f; return null; }

    String id() { return "fn_" + name().toLowerCase(java.util.Locale.ROOT); }
}
