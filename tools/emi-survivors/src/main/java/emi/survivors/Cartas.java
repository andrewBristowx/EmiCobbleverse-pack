package emi.survivors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * El sorteo de las 3 cartas que se ofrecen al subir de nivel: ataque nuevo, subir de nivel un ataque que ya tienes, mejora pasiva nueva o subida de una que ya tienes.
 * Subir lo que ya tienes sale mas a menudo que lo nuevo. Si no hay 3 opciones (todo al maximo), se rellena con «curar» y «xp». Logica pura (sin Minecraft).
 */
final class Cartas {
    private Cartas() {}

    /** @param tipo "arma", "pasiva", "curar" o "xp"; {@code nivel} = nivel que tendra tras elegirla (1 = nueva) */
    record Carta(String tipo, String id, int nivel) {
        boolean nueva() { return nivel == 1; }
    }

    static final Carta CURAR = new Carta("curar", "", 0), XP = new Carta("xp", "", 0);

    /** Las cartas posibles ahora mismo con su peso (para el sorteo y para probarlo). */
    static List<Object[]> candidatas(Map<String, Integer> armas, Map<String, Integer> pasivas) {
        List<Object[]> l = new ArrayList<>();
        for (Catalogo.Arma a : Catalogo.ARMAS) {
            int n = armas.getOrDefault(a.id(), 0);
            if (n > 0 && n < Catalogo.MAX_NIVEL) l.add(new Object[]{new Carta("arma", a.id(), n + 1), 1.6});
            else if (n == 0 && armas.size() < Catalogo.MAX_ARMAS) l.add(new Object[]{new Carta("arma", a.id(), 1), 1.0});
        }
        for (Catalogo.Pasiva p : Catalogo.PASIVAS) {
            int n = pasivas.getOrDefault(p.id(), 0);
            if (n > 0 && n < Catalogo.MAX_NIVEL) l.add(new Object[]{new Carta("pasiva", p.id(), n + 1), 1.2});
            else if (n == 0 && pasivas.size() < Catalogo.MAX_PASIVAS) l.add(new Object[]{new Carta("pasiva", p.id(), 1), 0.8});
        }
        return l;
    }

    /** 3 cartas distintas (o menos si no hay mas; se rellena con curar / xp). */
    static List<Carta> sortear(Random rnd, Map<String, Integer> armas, Map<String, Integer> pasivas) {
        List<Object[]> cand = candidatas(armas, pasivas);
        List<Carta> out = new ArrayList<>(3);
        while (out.size() < 3 && !cand.isEmpty()) {
            double total = 0;
            for (Object[] c : cand) total += (double) c[1];
            double r = rnd.nextDouble() * total;
            int idx = cand.size() - 1;
            for (int i = 0; i < cand.size(); i++) { r -= (double) cand.get(i)[1]; if (r <= 0) { idx = i; break; } }
            out.add((Carta) cand.remove(idx)[0]);
        }
        if (out.size() < 3) out.add(CURAR);
        if (out.size() < 3) out.add(XP);
        return out;
    }
}
