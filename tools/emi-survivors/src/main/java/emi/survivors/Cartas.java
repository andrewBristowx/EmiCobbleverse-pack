package emi.survivors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * El sorteo de las 3 cartas que se ofrecen al subir de nivel. Si algun ataque puede EVOLUCIONAR (esta al nivel 5 y llevas el objeto que pide), su carta dorada sale
 * siempre (hasta 2). Las demas se sortean por categorias: subir un ataque que ya tienes, un ataque nuevo, subir un objeto que ya tienes o un objeto nuevo. Asi, aunque
 * haya 30 ataques, subir lo que ya llevas sigue saliendo a menudo. Si no hay 3 opciones (todo al maximo), se rellena con «curar» y «xp». Logica pura (sin Minecraft).
 */
final class Cartas {
    private Cartas() {}

    /** @param tipo "arma", "pasiva", "curar", "xp" o "fusion" (id = «cabeza+unido»); {@code nivel} = nivel que tendra tras elegirla (1 = nueva; 6 = evolucion) */
    record Carta(String tipo, String id, int nivel) {
        boolean nueva() { return nivel == 1; }
        boolean evo() { return "arma".equals(tipo) && nivel >= Catalogo.NIVEL_EVO; }
    }

    static final Carta CURAR = new Carta("curar", "", 0), XP = new Carta("xp", "", 0);

    /** El peso de cada categoria al sortear (subir ataque, ataque nuevo, subir objeto, objeto nuevo). */
    static final double P_SUBIR_ARMA = 3.0, P_NUEVA_ARMA = 2.2, P_SUBIR_PASIVA = 2.0, P_NUEVA_PASIVA = 1.4;

    /** Las cartas de evolucion posibles ahora mismo. */
    static List<Carta> evoluciones(Map<String, Integer> armas, Map<String, Integer> pasivas) {
        List<Carta> l = new ArrayList<>();
        for (Catalogo.Arma a : Catalogo.ARMAS)
            if (Catalogo.puedeEvolucionar(a, armas.getOrDefault(a.id(), 0), pasivas)) l.add(new Carta("arma", a.id(), Catalogo.NIVEL_EVO));
        return l;
    }

    /** Las cartas de cada categoria: [0] subir ataque, [1] ataque nuevo, [2] subir objeto, [3] objeto nuevo. */
    static List<List<Carta>> categorias(Map<String, Integer> armas, Map<String, Integer> pasivas, Map<String, String> fusion) {
        List<Carta> subirArma = new ArrayList<>(), nuevaArma = new ArrayList<>(), subirPasiva = new ArrayList<>(), nuevaPasiva = new ArrayList<>();
        for (Catalogo.Arma a : Catalogo.ARMAS) {
            int n = armas.getOrDefault(a.id(), 0);
            if (n > 0 && n < Catalogo.MAX_NIVEL) subirArma.add(new Carta("arma", a.id(), n + 1));
            else if (n == 0 && Fusiones.ranuras(armas.keySet(), fusion) < Catalogo.MAX_ARMAS) nuevaArma.add(new Carta("arma", a.id(), 1));
        }
        for (Catalogo.Pasiva p : Catalogo.PASIVAS) {
            int n = pasivas.getOrDefault(p.id(), 0);
            if (n > 0 && n < Catalogo.MAX_NIVEL) subirPasiva.add(new Carta("pasiva", p.id(), n + 1));
            else if (n == 0 && pasivas.size() < Catalogo.MAX_PASIVAS) nuevaPasiva.add(new Carta("pasiva", p.id(), 1));
        }
        return List.of(subirArma, nuevaArma, subirPasiva, nuevaPasiva);
    }

    private static final double[] PESOS = {P_SUBIR_ARMA, P_NUEVA_ARMA, P_SUBIR_PASIVA, P_NUEVA_PASIVA};

    /** 3 cartas distintas (o menos si no hay mas; se rellena con curar / xp). */
    static List<Carta> sortear(Random rnd, Map<String, Integer> armas, Map<String, Integer> pasivas) { return sortear(rnd, armas, pasivas, Map.of()); }

    /** Igual, sabiendo que ataques estan fusionados (una ranura con varios ataques cuenta como una). */
    static List<Carta> sortear(Random rnd, Map<String, Integer> armas, Map<String, Integer> pasivas, Map<String, String> fusion) {
        List<Carta> out = new ArrayList<>(3);
        List<Carta> evos = evoluciones(armas, pasivas);
        while (out.size() < 2 && !evos.isEmpty()) out.add(evos.remove(rnd.nextInt(evos.size())));
        List<List<Carta>> cat = new ArrayList<>(categorias(armas, pasivas, fusion));
        while (out.size() < 3) {
            double total = 0;
            for (int i = 0; i < cat.size(); i++) if (!cat.get(i).isEmpty()) total += PESOS[i];
            if (total <= 0) break;
            double r = rnd.nextDouble() * total;
            int elegida = -1;
            for (int i = 0; i < cat.size(); i++) {
                if (cat.get(i).isEmpty()) continue;
                elegida = i;
                r -= PESOS[i];
                if (r <= 0) break;
            }
            List<Carta> l = cat.get(elegida);
            out.add(l.remove(rnd.nextInt(l.size())));
        }
        if (out.size() < 3) out.add(CURAR);
        if (out.size() < 3) out.add(XP);
        return out;
    }
}
