package com.emipokemon.client.progress;

/** Texto de las pestañas de region del Diario: con mas de 4 regiones solo el nombre (el recuento ya sale en la cabecera de la lista). */
public final class TabLabel {
    private TabLabel() {}

    public static String make(String name, int located, int total, int regionCount) {
        return regionCount > 4 ? name : name + " " + located + "/" + total;
    }
}
