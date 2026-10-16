package emi.survivors;

import java.util.List;
import java.util.Map;

/**
 * Las mejoras PERMANENTES de cada Pokemon (menu «Mejoras»): se compran con michiestrellitas y valen para todas las partidas de ese Pokemon.
 * Cada una llega a nivel 5. Es la version basica de la fase 1; en la fase 3 se amplia (misiones, logros...). Logica pura (sin Minecraft).
 */
final class Meta {
    private Meta() {}

    static final int MAX = 5;

    record Mejora(String id, String nombre, String icono, String texto) {}

    static final List<Mejora> MEJORAS = List.of(
            new Mejora("vigor", "Vigor", "minecraft:golden_apple", "+12 de vida máxima por nivel."),
            new Mejora("fuerza", "Fuerza", "minecraft:blaze_powder", "+4% de daño por nivel."),
            new Mejora("agilidad", "Agilidad", "minecraft:feather", "+2,5% de velocidad por nivel."),
            new Mejora("sabiduria", "Sabiduría", "minecraft:experience_bottle", "+6% de experiencia por nivel."),
            new Mejora("iman", "Imán", "minecraft:ender_eye", "+10% de radio para recoger gemas por nivel."),
            new Mejora("recuperacion", "Recuperación", "minecraft:glistering_melon_slice", "+0,15 de vida por segundo por nivel."),
            new Mejora("suerte", "Suerte", "minecraft:rabbit_foot", "+5% de michiestrellitas al acabar por nivel."));

    static Mejora mejora(String id) { for (Mejora m : MEJORAS) if (m.id().equals(id)) return m; return null; }

    /** Lo que cuesta subir una mejora que esta en el nivel {@code actual} al siguiente (25, 50, 75, 100, 125). */
    static int costo(int actual) { return 25 * (actual + 1); }

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
                default -> { }
            }
        }
    }
}
