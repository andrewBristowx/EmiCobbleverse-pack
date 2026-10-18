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

    /** Vida del legendario con un solo jugador en el mundo 0: sube con el tiempo y con los jugadores (igual que un jefe). Antes eran 700 y caian en dos segundos. */
    static double vida(double t, int jugadores) { return vida(t, jugadores, 0); }

    /** Igual, en el mundo {@code mundo} (0 pradera, 1 bosque, 2 cosmos...; cada uno suma un 35 %): ~4 300 a los 110 s, ~22 000 en el bosque a los 10 minutos. */
    static double vida(double t, int jugadores, int mundo) { return (1500 + 25 * t) * (1 + 0.35 * Math.max(0, mundo)) * Curvas.escalaJefe(jugadores); }

    /** Lo que daña de contacto el legendario (ademas de sus ataques): bastante mas que un campeon. */
    static final double DANO_CONTACTO = 30;

    /** Dano base de los ataques del legendario (cada ataque lo multiplica, como en los jefes): 26 en la pradera, +8 en cada mundo siguiente. */
    static double danoAtaques(int mundo) { return 26 + 8 * Math.max(0, mundo); }

    /** Dano de las explosiones que marca bajo cada jugador (antes 12-14, ahora 22 en la pradera y +6 por mundo). */
    static double danoBomba(int mundo) { return 22 + 6 * Math.max(0, mundo); }

    /** Los ataques del legendario segun el bioma: son los mismos de los jefes, todos avisados en el suelo (salto, carga, zarpazo, lluvia, ola con hueco y rayos giratorios). */
    static Jefes.Ataque[] ataques(String bioma) {
        return switch (bioma) {
            case "bosque" -> new Jefes.Ataque[]{Jefes.Ataque.LLUVIA, Jefes.Ataque.CARGA, Jefes.Ataque.SALTO, Jefes.Ataque.CONO, Jefes.Ataque.HAZ};
            case "cosmos" -> new Jefes.Ataque[]{Jefes.Ataque.HAZ, Jefes.Ataque.ANILLO, Jefes.Ataque.LLUVIA, Jefes.Ataque.CARGA, Jefes.Ataque.SALTO, Jefes.Ataque.CONO};
            default -> new Jefes.Ataque[]{Jefes.Ataque.SALTO, Jefes.Ataque.CARGA, Jefes.Ataque.CONO, Jefes.Ataque.LLUVIA};
        };
    }

    /** La «ficha de jefe» del legendario (para reutilizar los ataques de {@link Jefe}); siempre se enfurece al bajar del 50 %. */
    static Jefes.Def def(String nombre, String especie, String bioma, float alto) {
        int tema = switch (bioma) { case "bosque" -> 2; case "cosmos" -> 3; default -> 1; };
        return new Jefes.Def("legendario", nombre, especie, especie, "", "", alto, switch (bioma) { case "bosque" -> 3.0; case "cosmos" -> 3.3; default -> 2.8; }, 0f, 0, 60, ataques(bioma), true,
                "Un legendario ha salido de la grieta", "light_purple", tema);
    }

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
