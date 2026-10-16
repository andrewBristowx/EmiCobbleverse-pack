package emi.survivors;

import java.util.List;
import java.util.Map;

/**
 * Los ataques y las mejoras pasivas del juego (nombres, iconos, lo que hace cada nivel) y el ataque con el que empieza cada tipo de Pokemon.
 * Son datos puros (sin Minecraft). El comportamiento de los ataques esta en {@code Armas}; aqui solo lo que hace falta para las cartas y la interfaz.
 */
public final class Catalogo {
    private Catalogo() {}

    public static final int MAX_NIVEL = 5, MAX_ARMAS = 6, MAX_PASIVAS = 6;

    /** @param textos lo que hace cada nivel (textos[0] = nivel 1, que describe el ataque entero) */
    public record Arma(String id, String nombre, String icono, String[] textos) {}

    /** @param texto lo que da cada nivel */
    public record Pasiva(String id, String nombre, String icono, String texto) {}

    static final List<Arma> ARMAS = List.of(
            new Arma("disparo", "Disparo Rápido", "minecraft:arrow", new String[]{
                    "Lanza un proyectil al enemigo más cercano.", "+25% de daño.", "Lanza 2 proyectiles a la vez.",
                    "Atraviesa 1 enemigo más y dispara más rápido.", "Lanza 3 proyectiles y +25% de daño."}),
            new Arma("orbes", "Orbes Lunares", "minecraft:ender_pearl", new String[]{
                    "2 orbes giran a tu alrededor y dañan a quien tocan.", "3 orbes.", "Los orbes giran más rápido y son más grandes.",
                    "4 orbes.", "5 orbes y +25% de daño."}),
            new Arma("pulso", "Onda Expansiva", "minecraft:bell", new String[]{
                    "Cada pocos segundos lanza una onda que empuja y daña.", "+30% de daño y +10% de alcance.", "La onda llega más lejos y se lanza antes.",
                    "+30% de daño.", "Onda enorme: +40% de alcance y +30% de daño."}),
            new Arma("cadena", "Rayo en Cadena", "minecraft:lightning_rod", new String[]{
                    "Un rayo salta entre 2 enemigos cercanos.", "Salta a 3 enemigos.", "+25% de daño y se lanza antes.",
                    "Salta a 4 enemigos.", "Salta a 5 enemigos y +25% de daño."}),
            new Arma("meteoros", "Lluvia de Meteoros", "minecraft:fire_charge", new String[]{
                    "Cae un meteoro sobre un enemigo y explota.", "Caen 2 meteoros.", "Explosiones más grandes y +20% de daño.",
                    "Caen 3 meteoros.", "Caen 4 meteoros y se lanzan antes."}),
            new Arma("tajo", "Tajo Frontal", "minecraft:iron_sword", new String[]{
                    "Un tajo ancho hacia donde miras.", "+25% de daño.", "También golpea por detrás.",
                    "Tajo más grande y +25% de daño.", "Doble tajo y más rápido."}),
            new Arma("bumeran", "Disco Boomerang", "minecraft:trident", new String[]{
                    "Lanza un disco que vuelve a ti.", "+25% de daño.", "Lanzas 2 discos.",
                    "Los discos llegan más lejos.", "3 discos y +25% de daño."}),
            new Arma("estela", "Charco Tóxico", "minecraft:fermented_spider_eye", new String[]{
                    "Dejas charcos al andar que dañan a quien los pisa.", "Los charcos duran más.", "+30% de daño.",
                    "Charcos más grandes.", "Charcos enormes y +30% de daño."}),
            new Arma("minas", "Púas Ocultas", "minecraft:pointed_dripstone", new String[]{
                    "Dejas una púa que explota cuando se acerca un enemigo.", "Hasta 3 púas a la vez.", "+25% de daño y se reponen antes.",
                    "Hasta 4 púas y explosión más grande.", "Hasta 5 púas y +25% de daño."}),
            new Arma("rayo", "Rayo Giratorio", "minecraft:end_rod", new String[]{
                    "Un rayo gira a tu alrededor y quema a quien toca.", "El rayo es más largo.", "2 rayos.",
                    "+30% de daño y más largo.", "3 rayos."}));

    static final List<Pasiva> PASIVAS = List.of(
            new Pasiva("vida", "Corazón Michi", "minecraft:golden_apple", "+20 de vida máxima (y te cura 20)."),
            new Pasiva("regen", "Hierba Curativa", "minecraft:glistering_melon_slice", "Recuperas +0,6 de vida por segundo."),
            new Pasiva("vel", "Zapatos Veloces", "minecraft:feather", "+8% de velocidad."),
            new Pasiva("dano", "Garras Afiladas", "minecraft:blaze_powder", "+12% de daño."),
            new Pasiva("recarga", "Reloj de Arena", "minecraft:clock", "Tus ataques se usan un 8% más rápido."),
            new Pasiva("area", "Lupa Estelar", "minecraft:spyglass", "+10% de tamaño en tus ataques."),
            new Pasiva("iman", "Imán de Estrellas", "minecraft:ender_eye", "+30% de radio para recoger gemas."),
            new Pasiva("xp", "Sombrero de Sabio", "minecraft:experience_bottle", "+10% de experiencia."),
            new Pasiva("armadura", "Escudo Lunar", "minecraft:shield", "Recibes un 8% menos de daño."));

    public static Arma arma(String id) { for (Arma a : ARMAS) if (a.id().equals(id)) return a; return null; }
    public static Pasiva pasiva(String id) { for (Pasiva p : PASIVAS) if (p.id().equals(id)) return p; return null; }

    /** Suma el efecto de las mejoras pasivas (id -> nivel) a las estadisticas. */
    static void aplicarPasivas(Stats s, Map<String, Integer> niveles) {
        for (var e : niveles.entrySet()) {
            int n = e.getValue();
            switch (e.getKey()) {
                case "vida" -> s.hpMax += 20 * n;
                case "regen" -> s.regen += 0.6 * n;
                case "vel" -> s.vel *= 1 + 0.08 * n;
                case "dano" -> s.dano *= 1 + 0.12 * n;
                case "recarga" -> s.recarga *= 1 - 0.08 * n;
                case "area" -> s.area *= 1 + 0.10 * n;
                case "iman" -> s.iman *= 1 + 0.30 * n;
                case "xp" -> s.xp *= 1 + 0.10 * n;
                case "armadura" -> s.armadura = Math.min(0.6, s.armadura + 0.08 * n);
                default -> { }
            }
        }
    }

    // ------------------------------------------------------------ ataque inicial segun el tipo del Pokemon

    /** @param arma el ataque (id de {@link #ARMAS}); nombre = como se llama el movimiento de ese tipo */
    record Inicial(String arma, String nombre) {}

    private static final Map<String, Inicial> POR_TIPO = Map.ofEntries(
            Map.entry("fire", new Inicial("disparo", "Ascuas")), Map.entry("water", new Inicial("disparo", "Pistola Agua")),
            Map.entry("electric", new Inicial("cadena", "Impactrueno")), Map.entry("grass", new Inicial("orbes", "Hoja Afilada")),
            Map.entry("ice", new Inicial("disparo", "Rayo Hielo")), Map.entry("fighting", new Inicial("tajo", "Golpe Karate")),
            Map.entry("poison", new Inicial("estela", "Cola Veneno")), Map.entry("ground", new Inicial("pulso", "Terremoto")),
            Map.entry("flying", new Inicial("bumeran", "Ataque Ala")), Map.entry("psychic", new Inicial("pulso", "Confusión")),
            Map.entry("bug", new Inicial("tajo", "Tijera X")), Map.entry("rock", new Inicial("meteoros", "Avalancha")),
            Map.entry("ghost", new Inicial("disparo", "Bola Sombra")), Map.entry("dragon", new Inicial("tajo", "Garra Dragón")),
            Map.entry("dark", new Inicial("tajo", "Mordisco")), Map.entry("steel", new Inicial("bumeran", "Garra Metal")),
            Map.entry("fairy", new Inicial("pulso", "Fuerza Lunar")), Map.entry("normal", new Inicial("disparo", "Ataque Rápido")));

    static Inicial inicial(String tipo) { return POR_TIPO.getOrDefault(tipo, POR_TIPO.get("normal")); }
}
