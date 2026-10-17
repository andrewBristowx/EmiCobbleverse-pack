package emi.survivors;

import java.util.List;

/**
 * Las grietas: de vez en cuando se abre en la arena una grieta (un cuadrado morado en el suelo) y los jugadores tienen que quedarse DENTRO un rato para abrirla del todo;
 * entonces sale un Pokemon legendario (un campeon enorme con mucha vida, que ademas marca en el suelo explosiones bajo los jugadores) y, al derrotarlo, da un tesoro. Aqui
 * estan los numeros y las listas (sin Minecraft, se prueba con un main); la partida en marcha esta en {@link GrietaEquipo}.
 */
final class Grietas {
    private Grietas() {}

    /** El segundo en que sale la primera grieta y cada cuantos segundos sale otra (si no hay un jefe en medio). */
    static final int PRIMERA = 110, CADA = 180;
    /** Segundos que dura abierta la grieta esperando a que la abran, y «ticks de jugador» dentro que hacen falta (un jugador dentro suma 1 por tick: 8 s en solitario). */
    static final int DURACION_SEG = 60, CARGA = 160;
    /** Tamaño (lado, en bloques) del cuadrado de la grieta. */
    static final double LADO = 7.0;
    /** Michiestrellitas por cada legendario derrotado que se suman al acabar la partida (al jugador que le dio el ultimo golpe). */
    static final int MONEDAS_POR_LEGENDARIO = 8;

    /** Cuantas grietas le tocan ya a la partida a los {@code t} segundos. */
    static int tocan(double t) { return t < PRIMERA ? 0 : 1 + (int) ((t - PRIMERA) / CADA); }

    /** Vida del legendario: sube con el tiempo y con los jugadores (menos que el jefe). */
    static double vida(double t, int jugadores) { return 700 * (1 + t / 200.0) * Curvas.escalaJefe(jugadores); }

    /** Lo que derrota al legendario daña de contacto: algo mas que un campeon. */
    static final double DANO_CONTACTO = 18;

    /** Legendarios que pueden salir en cada bioma (se usan los que existan en el servidor; todos caminan, o al menos andan por el suelo en este juego). */
    static List<String> legendarios(String bioma) {
        return switch (bioma) {
            case "bosque" -> List.of("spectrier", "marshadow", "zarude", "hoopa", "calyrex", "giratina");
            case "cosmos" -> List.of("mewtwo", "solgaleo", "necrozma", "deoxys", "zeraora", "magearna", "genesect", "dialga", "palkia");
            default -> List.of("entei", "raikou", "suicune", "virizion", "terrakion", "cobalion", "tapubulu", "keldeo");
        };
    }

    /** Frase con la direccion de la grieta vista desde la pantalla (la camara mira al norte: arriba = norte, derecha = este). */
    static String direccion(double dx, double dz) {
        double ax = Math.abs(dx), az = Math.abs(dz);
        String v = az < ax * 0.4 ? "" : dz < 0 ? "arriba" : "abajo";
        String h = ax < az * 0.4 ? "" : dx > 0 ? "a la derecha" : "a la izquierda";
        if (v.isEmpty() && h.isEmpty()) return "aquí mismo";
        return v.isEmpty() ? h : h.isEmpty() ? v : v + " " + h;
    }
}
