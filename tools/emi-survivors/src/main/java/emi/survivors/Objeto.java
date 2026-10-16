package emi.survivors;

import java.util.Random;

/**
 * Un objeto del suelo: lo suelta un campeon o el jefe, y a veces un enemigo normal. No es una entidad (lo lleva el servidor y lo dibuja el cliente, como las gemas):
 * el primer jugador que lo pisa lo recoge. Datos puros (sin Minecraft).
 * <ul>
 *   <li>BAYA — Baya Zidra: cura el 30 % de la vida (y el 15 % a los companeros que esten cerca).</li>
 *   <li>IMAN — Iman: todas las michiestrellitas de la arena vuelan hacia quien lo coge.</li>
 *   <li>BOMBA — Bomba Michi: hace dano a todos los enemigos cercanos.</li>
 *   <li>CARAMELO — Caramelo Raro: sube un nivel al instante.</li>
 * </ul>
 */
final class Objeto {
    /** Los tipos; el numero es el «tipo» que viaja al cliente (las gemas usan 0, 1 y 2). */
    static final int BAYA = 10, IMAN = 11, BOMBA = 12, CARAMELO = 13;
    static final int[] TODOS = {BAYA, IMAN, BOMBA, CARAMELO};
    /** Ticks que dura en el suelo (90 s). */
    static final int VIDA = 20 * 90;

    final int tipo;
    final double x, z;
    int vida = VIDA;

    Objeto(int tipo, double x, double z) { this.tipo = tipo; this.x = x; this.z = z; }

    static boolean esObjeto(int tipo) { return tipo >= BAYA && tipo <= CARAMELO; }

    static String nombre(int tipo) {
        return switch (tipo) { case BAYA -> "Baya Zidra"; case IMAN -> "Imán"; case BOMBA -> "Bomba Michi"; case CARAMELO -> "Caramelo Raro"; default -> "?"; };
    }

    /** Lo que se ve al cogerlo. */
    static String texto(int tipo) {
        return switch (tipo) {
            case BAYA -> "¡Baya Zidra! +30% de vida";
            case IMAN -> "¡Imán! Las michiestrellitas vuelan hacia ti";
            case BOMBA -> "¡Bomba Michi! Todos los enemigos cercanos reciben daño";
            case CARAMELO -> "¡Caramelo Raro! Subes un nivel";
            default -> "";
        };
    }

    static int porNombre(String s) {
        return switch (s) { case "baya" -> BAYA; case "iman" -> IMAN; case "bomba" -> BOMBA; case "caramelo" -> CARAMELO; default -> -1; };
    }

    /** Que objeto suelta un enemigo derrotado: los campeones sueltan mas bombas y caramelos que los normales. */
    static int sortear(Random rnd, boolean campeon) {
        int[] peso = campeon ? new int[]{35, 25, 25, 15} : new int[]{45, 25, 20, 10};
        int total = 0;
        for (int p : peso) total += p;
        int r = rnd.nextInt(total);
        for (int i = 0; i < peso.length; i++) { r -= peso[i]; if (r < 0) return TODOS[i]; }
        return BAYA;
    }
}
