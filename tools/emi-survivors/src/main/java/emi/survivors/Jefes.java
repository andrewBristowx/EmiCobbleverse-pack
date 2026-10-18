package emi.survivors;

/**
 * Los jefes de la partida (datos puros, sin Minecraft): uno cada 3 minutos (3:00, 6:00, 9:00, 12:00 y 15:00); pasados los cinco, el ciclo vuelve a empezar con los
 * mismos jefes pero mas duros. Cada uno tiene su especie (de Emipokemon si esta instalado: el toque de Emi; si no, la de Cobblemon), su vida, su velocidad y la lista de
 * ataques que repite; TODOS los ataques se avisan en el suelo con particulas rojas para poder esquivarlos. Lo que hacen en la partida esta en {@link Jefe}.
 * Los numeros son primeras estimaciones: nadie los ha jugado a mano.
 */
final class Jefes {
    private Jefes() {}

    /** Los ataques de los jefes (cada uno tiene su aviso en el suelo). */
    enum Ataque {
        SALTO,      // salta sobre el sitio donde estaba un jugador: circulo rojo
        CARGA,      // embiste en linea recta: linea roja
        CONO,       // zarpazo corto y ancho delante de el: cuna roja
        LLUVIA,     // varios impactos a la vez alrededor de los jugadores: circulos rojos
        ANILLO,     // una ola que se expande desde el con un hueco (marcado en verde) por donde se pasa
        HAZ,        // rayos que giran alrededor de el: lineas rojas
        INVOCAR     // llama refuerzos
    }

    /**
     * @param id          nombre corto (para las pruebas: /survivor dev jefe 3)
     * @param nombre      lo que sale en la barra y en el titulo
     * @param especie     especie preferida (de Emipokemon); si no esta instalada se usa {@code alternativa}
     * @param alternativa especie de Cobblemon de repuesto
     * @param extra       propiedades de Cobblemon al crearlo (aspectos), o ""
     * @param secuaz      especie de los refuerzos que invoca («preferida|repuesto»; «gatitoalien» saca Michialiens de todos los aspectos), o ""
     * @param alto        altura en bloques
     * @param vel         velocidad al perseguir (bloques por segundo)
     * @param dmg         dano base por contacto (cada ataque lo multiplica)
     * @param vida        vida con un solo jugador y en el primer ciclo
     * @param espera      ticks que persigue entre un ataque y el siguiente
     * @param ataques     el orden en que los usa (se repite)
     * @param furia       si se enfurece al bajar del 50 % de vida (ataca mas deprisa y con mas)
     * @param aviso       frase del titulo al aparecer
     * @param color       color del nombre (nombre de {@code net.minecraft.util.Formatting})
     * @param tema        0..4: las particulas de sus ataques (ver {@link Jefe})
     */
    record Def(String id, String nombre, String especie, String alternativa, String extra, String secuaz, float alto, double vel, float dmg, double vida, int espera,
               Ataque[] ataques, boolean furia, String aviso, String color, int tema) {}

    static final Def[] TODOS = {
            new Def("snorlax", "Snorlax Emi", "emipokemon:snorlax_emi", "snorlax", "", "", 4.6f, 2.2, 25f, 1200, 90,
                    new Ataque[]{Ataque.SALTO, Ataque.CARGA}, false, "Esquiva los cuadros rojos", "light_purple", 0),
            new Def("meowscarada", "Meowscarada Emi", "emipokemon:meowscarada_emi", "meowscarada", "", "", 4.4f, 3.1, 28f, 6500, 70,
                    new Ataque[]{Ataque.CARGA, Ataque.SALTO, Ataque.CONO, Ataque.LLUVIA}, true, "¡Rápida! Esquiva sus zarpazos", "green", 1),
            new Def("zoroark", "Zoroark Emi", "emipokemon:zoroark_emi", "zoroark", "", "emipokemon:zorua_emi|zorua", 4.6f, 2.9, 32f, 15000, 70,
                    new Ataque[]{Ataque.INVOCAR, Ataque.CONO, Ataque.LLUVIA, Ataque.CARGA}, true, "Sus ilusiones te rodean", "red", 2),
            new Def("meowstic", "Meowstic Emi", "emipokemon:meowstic_emi", "meowstic", "", "", 4.4f, 2.6, 36f, 28000, 64,
                    new Ataque[]{Ataque.ANILLO, Ataque.LLUVIA, Ataque.HAZ, Ataque.SALTO}, true, "Corre al hueco verde de la ola", "dark_purple", 3),
            new Def("michialien", "Rey Michialien", "gatitoalien", "gatitoalien", "accessory=crown gold=true", "gatitoalien", 6.0f, 2.5, 40f, 48000, 60,
                    new Ataque[]{Ataque.HAZ, Ataque.INVOCAR, Ataque.ANILLO, Ataque.LLUVIA, Ataque.CARGA, Ataque.SALTO}, true, "¡El rey de los michialiens!", "gold", 4),
    };

    /**
     * Los jefes de «el Mas alla» (tras vencer al primer Michi Gigante): mucho mas fuertes, con especies de Cobblemon (no hay version Emi). Salen en orden desde el
     * jefe numero 7 (el primero del bosque de la segunda vuelta, que es el 7: ver {@link #POR_MUNDO}) y se repiten con mas vida cada vez.
     */
    static final Def[] MAS_ALLA = {
            new Def("garchomp", "Garchomp del Vacío", "garchomp", "garchomp", "", "", 4.8f, 3.4, 40f, 40000, 55,
                    new Ataque[]{Ataque.CARGA, Ataque.SALTO, Ataque.CONO, Ataque.LLUVIA, Ataque.HAZ}, true, "Es rapidísimo: no te quedes quieto", "red", 2),
            new Def("dragapult", "Dragapult Espectral", "dragapult", "dragapult", "", "dreepy", 4.6f, 3.0, 42f, 60000, 52,
                    new Ataque[]{Ataque.LLUVIA, Ataque.ANILLO, Ataque.INVOCAR, Ataque.HAZ, Ataque.SALTO}, true, "Sus Dreepy lanzan misiles", "dark_purple", 3),
            new Def("tyranitar", "Tyranitar Colosal", "tyranitar", "tyranitar", "", "", 5.2f, 2.6, 48f, 90000, 56,
                    new Ataque[]{Ataque.SALTO, Ataque.CARGA, Ataque.CONO, Ataque.ANILLO, Ataque.LLUVIA}, true, "Cada golpe sacude la arena", "dark_green", 0),
            new Def("hydreigon", "Hydreigon Voraz", "hydreigon", "hydreigon", "", "", 4.6f, 3.2, 50f, 120000, 48,
                    new Ataque[]{Ataque.HAZ, Ataque.LLUVIA, Ataque.CARGA, Ataque.ANILLO, Ataque.SALTO}, true, "Tres cabezas, tres ataques a la vez", "blue", 3),
            new Def("salamence", "Salamence Eterno", "salamence", "salamence", "", "", 4.8f, 3.6, 52f, 150000, 46,
                    new Ataque[]{Ataque.CARGA, Ataque.HAZ, Ataque.LLUVIA, Ataque.CONO, Ataque.SALTO, Ataque.ANILLO}, true, "Vuela bajo y no perdona", "gold", 1),
            new Def("rey_eterno", "Rey Michialien Eterno", "gatitoalien", "gatitoalien", "accessory=crown gold=true", "gatitoalien", 6.0f, 2.8, 56f, 200000, 44,
                    new Ataque[]{Ataque.HAZ, Ataque.INVOCAR, Ataque.ANILLO, Ataque.LLUVIA, Ataque.CARGA, Ataque.SALTO, Ataque.CONO}, true, "¡El rey no se rinde nunca!", "gold", 4),
    };

    /** El numero con el que se identifica al Michi Gigante, el jefe final de la partida (sale tras la Sentencia del ultimo mundo). */
    static final int GIGANTE_N = 100;
    static final Def GIGANTE = new Def("gigante", "Michi Gigante", "gatitoalien", "gatitoalien", "accessory=devil_horns gold=true", "gatitoalien", 14.0f, 2.2, 45f, 90000, 50,
            new Ataque[]{Ataque.HAZ, Ataque.SALTO, Ataque.INVOCAR, Ataque.ANILLO, Ataque.LLUVIA, Ataque.CARGA, Ataque.CONO}, true, "¡El michi más grande del cosmos!", "light_purple", 4);

    /** Cuantos jefes salen en cada mundo (el 1.º y el 2.º en la pradera, el 3.º y el 4.º en el bosque, el 5.º en el cosmos). */
    static final int POR_MUNDO = 2;

    /** Cuantos jefes le tocan YA al mundo {@code mundo} (0 = el primero) a los {@code t} segundos de ese mundo (aparte de los de mundos anteriores). */
    static int tocanEnMundo(double t, int mundo) { return Math.min(tocan(t), mundo >= 3 ? POR_MUNDO : Math.max(0, Math.min(POR_MUNDO, TODOS.length - POR_MUNDO * mundo))); }

    /** Segundos en que sale el primer jefe y cada cuantos sale el siguiente. */
    static final int PRIMERO = 180, CADA = 180;

    /** Del 1 al 5 los de la primera vuelta ({@link #TODOS}); desde el 7, los de el Mas alla ({@link #MAS_ALLA}); el 100, el Michi Gigante. */
    static Def def(int n) {
        if (n == GIGANTE_N) return GIGANTE;
        if (n <= TODOS.length) return TODOS[Math.max(1, n) - 1];
        return MAS_ALLA[Math.floorMod(n - 7, MAS_ALLA.length)];
    }

    /** En que vuelta de su lista va el jefe numero {@code n} (0 = la primera: los de el Mas alla repiten la lista de seis con mas vida cada vez). */
    static int ciclo(int n) { return n == GIGANTE_N || n <= TODOS.length ? 0 : Math.max(0, (n - 7) / MAS_ALLA.length); }

    /** El jefe con su nombre de esta vuelta: el Michi Gigante lleva numero romano desde la segunda vez («Michi Gigante II»). */
    static Def deLaVuelta(Def d, int n, int vuelta) {
        if (n != GIGANTE_N || vuelta <= 0) return d;
        String[] rom = {"", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        String r = vuelta < rom.length ? rom[vuelta] : String.valueOf(vuelta + 1);
        return new Def(d.id(), d.nombre() + " " + r, d.especie(), d.alternativa(), d.extra(), d.secuaz(), d.alto(), d.vel(), d.dmg(), d.vida(), d.espera(), d.ataques(), d.furia(), d.aviso(), d.color(), d.tema());
    }

    /** El segundo en que le toca salir al jefe numero {@code n}. */
    static int segundo(int n) { return PRIMERO + CADA * (Math.max(1, n) - 1); }

    /** Cuantos jefes le tocan ya a la partida a los {@code t} segundos (0 antes de los 3:00). */
    static int tocan(double t) { return t < PRIMERO ? 0 : 1 + (int) ((t - PRIMERO) / CADA); }

    /** Vida del jefe numero {@code n} con un solo jugador: sube un 60 % en cada vuelta nueva de los cinco. */
    static double vida(int n) { return def(n).vida() * (1 + 0.6 * ciclo(n)); }

    /** Igual, en la vuelta {@code vuelta} del recorrido (0 = la primera): cada vuelta de el Mas alla suma un 40 % (el Michi Gigante, un 80 %). */
    static double vida(int n, int vuelta) { return vida(n) * (1 + (n == GIGANTE_N ? 0.8 : 0.4) * Math.max(0, vuelta)); }

    /** Cuanto suben los danos de los jefes (los ataques de los jefes se calculan sobre esta base); antes los jefes no se notaban con la vida de un jugador. */
    static final float DANO_JEFES = 1.25f;

    /** Dano base de contacto del jefe numero {@code n}: sube un 25 % en cada vuelta de su lista. */
    static float dano(int n) { return (float) (def(n).dmg() * (1 + 0.25 * ciclo(n)) * DANO_JEFES); }

    /** Igual, en la vuelta {@code vuelta} del recorrido: +20 % por cada vuelta de el Mas alla. */
    static float dano(int n, int vuelta) { return dano(n) * (float) (1 + 0.2 * Math.max(0, vuelta)); }

    /** Cuantos impactos tiene la lluvia de un jefe segun los jugadores y la vuelta (entre 6 y 16). */
    static int impactos(int jugadores, int n) { return Math.min(16, 6 + 2 * Math.max(1, Math.min(4, jugadores)) + 2 * ciclo(n)); }

    /** Cuantos refuerzos invoca un jefe (mas con la furia, los jugadores y las vueltas; entre 6 y 24). */
    static int refuerzos(int jugadores, int n, boolean furia) { return Math.min(24, 6 + 2 * (Math.max(1, Math.min(4, jugadores)) - 1) + 3 * ciclo(n) + (furia ? 4 : 0)); }

    /** Cuantos de los cinco jefes de una vuelta se han derrotado (para el titulo del ultimo: «¡Has vencido a …!»). */
    static boolean esElUltimo(int n) { return n == GIGANTE_N || n == TODOS.length || (n > TODOS.length + 1 && (n - 7) % MAS_ALLA.length == MAS_ALLA.length - 1); }

    /** Monedas extra por jefe derrotado (se suman al acabar la partida). */
    static final int MONEDAS_POR_JEFE = 20;
}
