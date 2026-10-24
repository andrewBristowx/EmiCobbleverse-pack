package emi.cafe;

import java.util.Collection;

/** Cada especie de cliente tiene un plato favorito. Cada especie ordena todos los platos a su manera (por su nombre) y el favorito es el primero que el dueño sabe hacer. Sin Minecraft. */
final class Gustos {
    private Gustos() {}

    /** El id del plato favorito de esa especie entre los que se conocen (null si no se conoce ninguno). */
    static String favorito(String especie, Collection<String> conocidos) {
        String mejor = null; int pm = Integer.MAX_VALUE;
        for (String id : conocidos) {
            int p = (especie + "#" + id).hashCode() & 0x7fffffff;
            if (p < pm || (p == pm && id.compareTo(mejor) < 0)) { pm = p; mejor = id; }
        }
        return mejor;
    }
}
