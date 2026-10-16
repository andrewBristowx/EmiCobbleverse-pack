package emi.survivors;

import java.util.List;
import java.util.Map;

/**
 * Las mejoras PERMANENTES de cada Pokemon (menu «Mejoras»): se compran con michiestrellitas y valen para todas las partidas de ese Pokemon.
 * Cada una llega a nivel 5. 12 mejoras (7 de la fase 1 y 5 de la 3A). Logica pura (sin Minecraft).
 */
final class Meta {
    private Meta() {}

    static final int MAX = 5;

    /** {@code base} es lo que cuesta el primer nivel; cada nivel siguiente cuesta una vez mas (base, 2 x base, 3 x base...). */
    record Mejora(String id, String nombre, String texto, int base) {
        String icono() { return "m_" + id; }
    }

    static final List<Mejora> MEJORAS = List.of(
            new Mejora("vigor", "Vigor", "+12 de vida máxima por nivel.", 25),
            new Mejora("fuerza", "Fuerza", "+4% de daño por nivel.", 25),
            new Mejora("agilidad", "Agilidad", "+2,5% de velocidad por nivel.", 25),
            new Mejora("sabiduria", "Sabiduría", "+6% de experiencia por nivel.", 25),
            new Mejora("iman", "Imán", "+10% de radio para recoger gemas por nivel.", 25),
            new Mejora("recuperacion", "Recuperación", "+0,15 de vida por segundo por nivel.", 25),
            new Mejora("suerte", "Suerte", "+5% de michiestrellitas al acabar por nivel.", 25),
            // desde 0.7.0 (fase 3A): las que antes solo se conseguian con cartas durante la partida; cuestan mas
            new Mejora("armadura", "Armadura", "Recibes un 3% menos de daño por nivel.", 40),
            new Mejora("critico", "Crítico", "+3% de probabilidad de golpe crítico por nivel.", 40),
            new Mejora("area", "Alcance", "+4% de tamaño de los ataques por nivel.", 40),
            new Mejora("recarga", "Rapidez", "Los ataques se usan un 3% más rápido por nivel.", 40),
            new Mejora("concha", "Vampirismo", "Recuperas +0,1 de vida por cada Pokémon derrotado, por nivel.", 40));

    static Mejora mejora(String id) { for (Mejora m : MEJORAS) if (m.id().equals(id)) return m; return null; }

    /** Lo que cuesta subir una mejora que esta en el nivel {@code actual} al siguiente (las basicas: 25, 50, 75, 100, 125; las de la fase 3A: 40, 80, 120, 160, 200). */
    static int costo(Mejora m, int actual) { return m.base() * (actual + 1); }

    /** Suma el efecto de las mejoras permanentes (id -> nivel) a las estadisticas. */
    static void aplicar(Stats s, Map<String, Integer> niveles) {
        for (var e : niveles.entrySet()) {
            int n = Math.max(0, Math.min(MAX, e.getValue()));
            switch (e.getKey()) {
                case "vigor" -> s.hpMax += 12 * n;
                case "fuerza" -> s.dano *= 1 + 0.04 * n;
                case "agilidad" -> s.vel *= 1 + 0.025 * n;
                case "sabiduria" -> s.xp *= 1 + 0.06 * n;
                case "iman" -> s.iman *= 1 + 0.10 * n;
                case "recuperacion" -> s.regen += 0.15 * n;
                case "suerte" -> s.monedas *= 1 + 0.05 * n;
                case "armadura" -> s.armadura = Math.min(0.6, s.armadura + 0.03 * n);
                case "critico" -> s.critico = Math.min(0.5, s.critico + 0.03 * n);
                case "area" -> s.area *= 1 + 0.04 * n;
                case "recarga" -> s.recarga *= 1 - 0.03 * n;
                case "concha" -> s.concha += 0.1 * n;
                default -> { }
            }
        }
    }
}
