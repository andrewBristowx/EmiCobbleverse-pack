package emi.survivors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Las fusiones de ataques. Tienes {@link Catalogo#MAX_ARMAS} ranuras de ataque; fusionar dos ataques los junta en UNA sola ranura (hasta {@link #MAX_GRUPO} ataques por
 * ranura) y deja una ranura libre. Los ataques fusionados siguen funcionando y siguen subiendo de nivel y evolucionando cada uno por su lado. Se guarda como
 * «ataque fusionado → ataque cabeza de su ranura» ({@code huesped}). Logica pura (sin Minecraft).
 */
final class Fusiones {
    private Fusiones() {}

    static final int MAX_GRUPO = 4;

    /** Cuantas ranuras ocupan estos ataques: los que no estan fusionados dentro de otro. */
    static int ranuras(Collection<String> armas, Map<String, String> huesped) {
        int n = 0;
        for (String a : armas) if (!huesped.containsKey(a)) n++;
        return n;
    }

    /** Cuantos ataques hay en la ranura cuya cabeza es {@code cabeza} (ella incluida). */
    static int tamano(String cabeza, Map<String, String> huesped) {
        int n = 1;
        for (String h : huesped.values()) if (h.equals(cabeza)) n++;
        return n;
    }

    /** Los ataques de una ranura (la cabeza primero), en el orden de {@code armas}. */
    static List<String> grupo(String cabeza, Collection<String> armas, Map<String, String> huesped) {
        List<String> l = new ArrayList<>();
        l.add(cabeza);
        for (String a : armas) if (cabeza.equals(huesped.get(a))) l.add(a);
        return l;
    }

    /** Las fusiones posibles: pares {cabeza que se queda, cabeza que se une}; la suma de los dos grupos no pasa de {@link #MAX_GRUPO}. */
    static List<String[]> pares(Collection<String> armas, Map<String, String> huesped) {
        List<String> cabezas = new ArrayList<>();
        for (String a : armas) if (!huesped.containsKey(a)) cabezas.add(a);
        List<String[]> out = new ArrayList<>();
        for (int i = 0; i < cabezas.size(); i++)
            for (int j = i + 1; j < cabezas.size(); j++)
                if (tamano(cabezas.get(i), huesped) + tamano(cabezas.get(j), huesped) <= MAX_GRUPO) out.add(new String[]{cabezas.get(i), cabezas.get(j)});
        return out;
    }

    /** Hasta {@code n} fusiones distintas al azar. */
    static List<String[]> sortear(Random rnd, Collection<String> armas, Map<String, String> huesped, int n) {
        List<String[]> l = pares(armas, huesped);
        List<String[]> out = new ArrayList<>();
        while (out.size() < n && !l.isEmpty()) out.add(l.remove(rnd.nextInt(l.size())));
        return out;
    }

    /** Une la ranura de {@code b} a la de {@code a} (cabeza {@code a}); devuelve si se pudo. */
    static boolean fusionar(Map<String, String> huesped, Collection<String> armas, String a, String b) {
        if (a.equals(b) || huesped.containsKey(a) || huesped.containsKey(b) || !armas.contains(a) || !armas.contains(b)) return false;
        if (tamano(a, huesped) + tamano(b, huesped) > MAX_GRUPO) return false;
        for (Map.Entry<String, String> e : huesped.entrySet()) if (e.getValue().equals(b)) e.setValue(a);
        huesped.put(b, a);
        return true;
    }
}
