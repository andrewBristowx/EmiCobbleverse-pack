package emi.survivors;

import java.util.Random;

/**
 * Las mareas (datos puros, sin Minecraft): sucesos con aviso que rompen el ritmo normal de la horda. Llega una a mitad de cada tramo entre jefes (1:30, 4:30, 7:30,
 * 10:30, 13:30…; si hay un jefe vivo espera a que caiga) y van en este orden: de Michialiens, cerco, de un tipo de Pokemon, estampida (y vuelta a empezar).
 * <ul>
 *   <li>MICHIS — durante 30 s salen muchisimos mas enemigos y TODOS son Michialiens (rapidos y de poca vida).</li>
 *   <li>CERCO — un anillo de enemigos aparece de golpe alrededor de los jugadores con un hueco verde marcado en el suelo por donde se puede salir.</li>
 *   <li>TIPO — durante 30 s salen mas enemigos y TODOS son de un mismo tipo (fuego, agua, planta…).</li>
 *   <li>ESTAMPIDA — tres carriles rojos cruzan la arena; pasado el aviso, una fila de enemigos muy rapidos los recorre sin desviarse.</li>
 * </ul>
 * Lo que hacen en la partida esta en {@link Marea}. Los numeros son primeras estimaciones: nadie los ha jugado a mano.
 */
final class Mareas {
    private Mareas() {}

    enum Tipo { MICHIS, CERCO, TIPO, ESTAMPIDA }

    /** Segundos en que sale la primera marea y cada cuantos sale otra. */
    static final int PRIMERA = 90, CADA = 180;
    /** Lo que dura una marea de enemigos (MICHIS y TIPO), en segundos. */
    static final int DURACION = 30;

    /** Cuantas mareas le tocan ya a la partida a los {@code t} segundos. */
    static int tocan(double t) { return t < PRIMERA ? 0 : 1 + (int) ((t - PRIMERA) / CADA); }

    /** El segundo en que le toca salir a la marea numero {@code k}. */
    static int segundo(int k) { return PRIMERA + CADA * (Math.max(1, k) - 1); }

    private static final Tipo[] ORDEN = {Tipo.MICHIS, Tipo.CERCO, Tipo.TIPO, Tipo.ESTAMPIDA};

    static Tipo tipo(int k) { return ORDEN[(Math.max(1, k) - 1) % ORDEN.length]; }

    static Tipo porNombre(String s) {
        return switch (s) { case "michis" -> Tipo.MICHIS; case "cerco" -> Tipo.CERCO; case "tipo" -> Tipo.TIPO; case "estampida" -> Tipo.ESTAMPIDA; default -> null; };
    }

    /** Cuantas veces mas deprisa salen los enemigos durante la marea. */
    static double factorRitmo(Tipo t) { return t == Tipo.MICHIS ? 2.2 : t == Tipo.TIPO ? 1.8 : 1.0; }

    /** Cuanto mas deprisa andan los enemigos de la marea de Michialiens. */
    static final double VELOCIDAD_MICHIS = 1.25, VIDA_MICHIS = 0.65;

    /** Un tipo de Pokemon de las mareas «de un tipo»: el nombre que usa Cobblemon, el que se ve, y el color del titulo. */
    record TipoPk(String id, String nombre, String color) {}

    static final TipoPk[] TIPOS = {
            new TipoPk("fire", "FUEGO", "red"), new TipoPk("water", "AGUA", "blue"), new TipoPk("grass", "PLANTA", "green"), new TipoPk("electric", "ELÉCTRICA", "yellow"),
            new TipoPk("ice", "HIELO", "aqua"), new TipoPk("ghost", "FANTASMA", "dark_purple"), new TipoPk("dark", "SINIESTRA", "dark_gray"), new TipoPk("psychic", "PSÍQUICA", "light_purple"),
            new TipoPk("poison", "VENENO", "dark_purple"), new TipoPk("rock", "ROCA", "gold"), new TipoPk("bug", "BICHO", "green"), new TipoPk("ground", "TIERRA", "gold"),
            new TipoPk("fighting", "LUCHA", "red"), new TipoPk("steel", "ACERO", "gray"), new TipoPk("fairy", "HADA", "light_purple"), new TipoPk("dragon", "DRAGÓN", "blue"),
            new TipoPk("normal", "NORMAL", "white")};

    static TipoPk tipoPk(String id) { for (TipoPk t : TIPOS) if (t.id().equals(id)) return t; return null; }

    static TipoPk tipoAlAzar(Random rnd) { return TIPOS[rnd.nextInt(TIPOS.length)]; }

    /** Cuantos enemigos tiene el cerco segun el tiempo y los jugadores (entre 18 y 54). */
    static int cerco(double t, int jugadores) { return (int) Math.max(18, Math.min(54, 16 + t / 12.0 + 4 * (Math.max(1, Math.min(4, jugadores)) - 1))); }

    /** Cuantos enemigos corren por cada carril de la estampida (entre 8 y 18). */
    static int porCarril(double t) { return (int) Math.max(8, Math.min(18, 8 + t / 60.0)); }

    /** Cuantos carriles tiene la estampida: uno por jugador (hasta 3) y como poco 2. */
    static int carriles(int jugadores) { return Math.max(2, Math.min(3, jugadores)); }

    /** Titulo de la marea (con el tipo de Pokemon si es «de un tipo»). */
    static String titulo(Tipo t, TipoPk pk) {
        return switch (t) {
            case MICHIS -> "¡MAREA MICHI!";
            case CERCO -> "¡CERCO!";
            case TIPO -> "¡MAREA " + (pk == null ? "?" : pk.nombre()) + "!";
            case ESTAMPIDA -> "¡ESTAMPIDA!";
        };
    }

    static String subtitulo(Tipo t) {
        return switch (t) {
            case MICHIS -> "Son muchos y muy rápidos";
            case CERCO -> "Sal por el hueco verde";
            case TIPO -> "Todos los enemigos son del mismo tipo";
            case ESTAMPIDA -> "Apártate de las líneas rojas";
        };
    }
}
