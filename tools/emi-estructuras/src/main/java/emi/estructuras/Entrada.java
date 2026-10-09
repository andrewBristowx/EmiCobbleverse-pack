package emi.estructuras;

import net.minecraft.util.Identifier;

/** Una estructura que se coloca en la dimension plana. */
public final class Entrada {
    public final Identifier id;
    public final boolean plantilla;   // true = es un .nbt suelto (se coloca con /place template), false = estructura de worldgen
    public final String clave;        // "kanto:1", "johto:5"... clave de Emipokemon (null si no es una de las 69)
    public final String etiqueta;
    public final boolean oceano;      // necesita una balsa de agua alrededor

    public Entrada(Identifier id, boolean plantilla, String clave, String etiqueta, boolean oceano) {
        this.id = id; this.plantilla = plantilla; this.clave = clave; this.etiqueta = etiqueta; this.oceano = oceano;
    }
}
