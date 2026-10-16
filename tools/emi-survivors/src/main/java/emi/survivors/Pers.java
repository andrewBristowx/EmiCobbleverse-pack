package emi.survivors;

import java.util.Random;

/**
 * Las personalidades de los enemigos normales (datos puros, sin Minecraft): a medida que avanza la partida, algunos enemigos salen con una personalidad que cambia
 * como se comportan y se marcan con un contorno de color para reconocerlos de lejos:
 * <ul>
 *   <li>VELOZ (azul claro) — muy rapido y de poca vida.</li>
 *   <li>BRUTO (rojo) — grande, lento, con mucha vida y que pega fuerte.</li>
 *   <li>EMBISTE (naranja) — de vez en cuando se queda quieto un momento (con particulas de enfado) y se lanza en linea recta hacia ti.</li>
 *   <li>EXPLOSIVO (rosa) — al derrotarlo deja una bomba en el suelo (aviso rojo ~1,2 s) que hace dano a los jugadores que esten dentro; hay que matarlo desde lejos.</li>
 * </ul>
 * Lo que hace cada una esta en {@link Enemigo} y {@link Equipo}. Los numeros son primeras estimaciones.
 */
enum Pers {
    //      nombre       color del contorno  vida  dano  vel   tamano
    VELOZ("Veloz", "aqua", 0.60, 1.00, 1.55, 1.00),
    BRUTO("Bruto", "red", 2.40, 1.50, 0.85, 1.30),
    EMBISTE("Embiste", "gold", 1.00, 1.00, 1.00, 1.00),
    EXPLOSIVO("Explosivo", "light_purple", 1.00, 1.00, 1.00, 1.00);

    final String nombre, color;
    final double vida, dano, vel, tam;

    Pers(String nombre, String color, double vida, double dano, double vel, double tam) {
        this.nombre = nombre; this.color = color; this.vida = vida; this.dano = dano; this.vel = vel; this.tam = tam;
    }

    /** Probabilidad de que un enemigo normal nuevo tenga personalidad: 0 el primer minuto y luego sube hasta un 25 % (a los 6:50). */
    static double probabilidad(double t) { return Math.min(0.25, Math.max(0, (t - 60) / 1400.0)); }

    static Pers sortear(Random rnd) { Pers[] v = values(); return v[rnd.nextInt(v.length)]; }

    static Pers porNombre(String s) {
        for (Pers p : values()) if (p.name().equalsIgnoreCase(s) || p.nombre.equalsIgnoreCase(s)) return p;
        return null;
    }

    /** Lo que se le dice al jugador la primera vez que sale una en la partida. */
    static final String LEYENDA = "Enemigos con contorno de color: azul claro = veloz · rojo = bruto · naranja = embiste · rosa = explota al caer";

    /** Dano de la bomba de un explosivo derrotado a los {@code t} segundos de partida. */
    static float danoBomba(double t) { return (float) Math.min(60, 12 + t / 25.0); }
}
