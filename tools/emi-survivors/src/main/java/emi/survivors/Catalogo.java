package emi.survivors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Los ataques y los objetos (mejoras pasivas) del juego: nombres, iconos, lo que hace cada nivel, las evoluciones y el ataque con el que empieza cada tipo de Pokemon.
 * Son datos puros (sin Minecraft). El comportamiento de los ataques esta en {@code Armas} y {@code Armas2}; aqui solo lo que hace falta para las cartas y la interfaz.
 *
 * Un ataque tiene 5 niveles y, si lleva el objeto adecuado cuando llega al 5, puede EVOLUCIONAR (nivel 6): cambia de nombre y se vuelve mucho mas fuerte.
 */
public final class Catalogo {
    private Catalogo() {}

    public static final int MAX_NIVEL = 5, NIVEL_EVO = 6, MAX_ARMAS = 4, MAX_PASIVAS = 6;

    /**
     * Los iconos son claves del atlas {@code textures/iconos.png} (lo genera {@code assets/generar_iconos.py}): {@code a_<ataque>}, {@code e_<ataque>} (su evolucion),
     * {@code i_<tipo>} (el ataque inicial de ese tipo de Pokemon), {@code p_<objeto>}, {@code m_<mejora permanente>} y {@code x_<otros>}.
     */
    public static String claveAtaque(String id) { return "a_" + id; }

    /** La evolucion de un ataque: nombre nuevo, lo que hace y el objeto (id de una pasiva) que hay que llevar. Su icono es {@code e_<ataque>}. */
    public record Evo(String id, String nombre, String texto, String requiere) {
        public String icono() { return "e_" + id; }
    }

    /** @param textos lo que hace cada nivel (textos[0] = nivel 1, que describe el ataque entero); {@code evo} = su evolucion (null si no evoluciona) */
    public record Arma(String id, String nombre, String[] textos, Evo evo) {
        public String icono() { return claveAtaque(id); }
    }

    /** @param texto lo que da cada nivel */
    public record Pasiva(String id, String nombre, String texto) {
        public String icono() { return "p_" + id; }
    }

    private static Arma a(String id, String nombre, String evoNombre, String requiere, String evoTexto, String... textos) {
        if (textos.length != MAX_NIVEL) throw new IllegalArgumentException("el ataque " + id + " necesita " + MAX_NIVEL + " textos");
        return new Arma(id, nombre, textos, new Evo(id, evoNombre, evoTexto, requiere));
    }

    static final List<Arma> ARMAS = List.of(
            // ---- los 10 de la fase 1
            a("disparo", "Disparo Rápido", "Ráfaga Infinita", "recarga", "Dispara sin parar 4 proyectiles que atraviesan a todos los enemigos.",
                    "Lanza un proyectil al enemigo más cercano.", "+25% de daño.", "Lanza 2 proyectiles a la vez.",
                    "Atraviesa 1 enemigo más y dispara más rápido.", "Lanza 3 proyectiles y +25% de daño."),
            a("orbes", "Orbes Lunares", "Eclipse Total", "area", "8 orbes enormes giran en dos anillos y empujan a la horda.",
                    "2 orbes giran a tu alrededor y dañan a quien tocan.", "3 orbes.", "Los orbes giran más rápido y son más grandes.",
                    "4 orbes.", "5 orbes y +25% de daño."),
            a("pulso", "Onda Expansiva", "Onda Gravitatoria", "iman", "Atrae a los enemigos hacia ti y los lanza lejos con una onda enorme.",
                    "Cada pocos segundos lanza una onda que empuja y daña.", "+30% de daño y +10% de alcance.", "La onda llega más lejos y se lanza antes.",
                    "+30% de daño.", "Onda enorme: +40% de alcance y +30% de daño."),
            a("cadena", "Rayo en Cadena", "Tormenta Eléctrica", "vel", "3 rayos a la vez saltan entre 6 enemigos y los aturden.",
                    "Un rayo salta entre 2 enemigos cercanos.", "Salta a 3 enemigos.", "+25% de daño y se lanza antes.",
                    "Salta a 4 enemigos.", "Salta a 5 enemigos y +25% de daño."),
            a("meteoros", "Lluvia de Meteoros", "Lluvia Apocalíptica", "dano", "Caen 6 meteoros enormes que dejan el suelo ardiendo.",
                    "Cae un meteoro sobre un enemigo y explota.", "Caen 2 meteoros.", "Explosiones más grandes y +20% de daño.",
                    "Caen 3 meteoros.", "Caen 4 meteoros y se lanzan antes."),
            a("tajo", "Tajo Frontal", "Corte Dimensional", "critico", "Un tajo enorme en todas direcciones que siempre es crítico.",
                    "Un tajo ancho hacia donde miras.", "+25% de daño.", "También golpea por detrás.",
                    "Tajo más grande y +25% de daño.", "Doble tajo y más rápido."),
            a("bumeran", "Disco Boomerang", "Disco Maestro", "experto", "4 discos enormes que van y vuelven más rápido.",
                    "Lanza un disco que vuelve a ti.", "+25% de daño.", "Lanzas 2 discos.",
                    "Los discos llegan más lejos.", "3 discos y +25% de daño."),
            a("estela", "Charco Tóxico", "Pantano Vital", "regen", "Charcos enormes que además te curan mientras los pisas.",
                    "Dejas charcos al andar que dañan a quien los pisa.", "Los charcos duran más.", "+30% de daño.",
                    "Charcos más grandes.", "Charcos enormes y +30% de daño."),
            a("minas", "Púas Ocultas", "Erizo Explosivo", "casco", "Hasta 7 púas que al explotar lanzan esquirlas en todas direcciones.",
                    "Dejas una púa que explota cuando se acerca un enemigo.", "Hasta 3 púas a la vez.", "+25% de daño y se reponen antes.",
                    "Hasta 4 púas y explosión más grande.", "Hasta 5 púas y +25% de daño."),
            a("rayo", "Rayo Giratorio", "Aspas Solares", "armadura", "4 rayos largos que giran muy deprisa y empujan a los enemigos.",
                    "Un rayo gira a tu alrededor y quema a quien toca.", "El rayo es más largo.", "2 rayos.",
                    "+30% de daño y más largo.", "3 rayos."),

            // ---- los 20 nuevos de la fase 2B
            a("lanza", "Lanzallamas", "Infierno Voraz", "dano", "Un cono gigante de fuego azul que lo quema todo.",
                    "Escupe llamas en cono hacia el enemigo más cercano.", "+15% de daño.", "Llamas más largas y que duran más.",
                    "Cono más ancho.", "Llamas enormes y +25% de daño."),
            a("hidro", "Hidrobomba", "Maremoto", "area", "Olas enormes en las 4 direcciones que frenan a todos.",
                    "Lanza una ola ancha que atraviesa a todos y los frena.", "+20% de daño.", "Ola más ancha.",
                    "También lanza otra ola hacia atrás.", "Olas más grandes y +20% de daño."),
            a("solar", "Rayo Solar", "Rayo Dorado", "moneda", "4 rayos gigantes de luz dorada con un daño brutal.",
                    "Tras cargar un momento, dispara un rayo enorme que atraviesa todo.", "+20% de daño.", "2 rayos en direcciones distintas.",
                    "Rayos más anchos y más largos.", "3 rayos y +25% de daño."),
            a("hojas", "Hoja Navaja", "Ciclón de Hojas", "vel", "8 hojas veloces que persiguen a todos y lo cortan todo.",
                    "Lanza 3 hojas que persiguen a enemigos al azar.", "4 hojas.", "Las hojas atraviesan 1 enemigo más.",
                    "5 hojas y +15% de daño.", "6 hojas, atraviesan más y se lanzan antes."),
            a("petalos", "Danza Pétalo", "Lluvia de Pétalos", "vida", "18 pétalos en una espiral doble que cubre toda la zona.",
                    "Lanza una espiral de pétalos en todas direcciones.", "8 pétalos.", "Los pétalos atraviesan 1 enemigo más.",
                    "10 pétalos y +15% de daño.", "12 pétalos y se lanzan antes."),
            a("lodo", "Bomba Lodo", "Marea de Lodo", "concha", "4 bombas enormes y charcos que duran el doble.",
                    "Lanza una bomba de lodo: explota y deja un charco que frena.", "El charco dura más.", "Lanza 2 bombas.",
                    "Explosión más grande.", "3 bombas y +20% de daño."),
            a("terremoto", "Gran Temblor", "Cataclismo", "armadura", "Sacude toda la zona con un daño brutal y aturde mucho más.",
                    "Marca el suelo a tu alrededor y lo sacude: daña y aturde.", "+20% de daño.", "Sacudida más grande y aturde más.",
                    "Aún más grande.", "+25% de daño y aturde más."),
            a("ventisca", "Ventisca", "Corazón de Hielo", "vida", "Una ventisca enorme que casi congela a los enemigos.",
                    "Un aire helado a tu alrededor daña y frena a los enemigos.", "+20% de daño.", "Más grande.",
                    "Frena más.", "Enorme y +25% de daño."),
            a("torbellino", "Torbellino", "Tornado Colosal", "iman", "3 tornados gigantes que duran mucho y lo arrastran todo.",
                    "Un tornado va hacia los enemigos, los atrae y los daña.", "Dura más.", "Más grande.",
                    "2 tornados.", "Tornados enormes y +25% de daño."),
            a("trueno", "Trueno", "Rayo Divino", "critico", "6 rayos enormes del cielo que aturden más tiempo.",
                    "Un rayo cae del cielo sobre un enemigo y lo aturde.", "Caen 2 rayos.", "Los rayos explotan más grande.",
                    "Caen 3 rayos.", "4 rayos y +20% de daño."),
            a("granizo", "Granizo", "Granizada Eterna", "recarga", "4 tormentas de granizo enormes, una tras otra.",
                    "Una tormenta de granizo cae sobre un grupo de enemigos.", "Dura más.", "2 tormentas.",
                    "Tormentas más grandes.", "3 tormentas y +25% de daño."),
            a("cometa", "Cometa", "Lluvia de Cometas", "xp", "3 cometas gigantes cruzan la arena.",
                    "Un cometa cruza la arena y tritura a quien se cruce.", "+20% de daño.", "Cometa más grande.",
                    "Lanzas 2 cometas.", "Cometas enormes y +20% de daño."),
            a("garra", "Garra Umbría", "Garra Abismal", "experto", "9 zarpazos enormes de sombras en todas direcciones.",
                    "Zarpazos rápidos en todas direcciones alrededor de ti.", "4 zarpazos.", "5 zarpazos y más largos.",
                    "+15% de daño.", "6 zarpazos y +20% de daño."),
            a("agujero", "Agujero Negro", "Singularidad", "iman", "Atrae a casi todos los enemigos y explota con una fuerza brutal.",
                    "Crea un agujero negro que atrae a los enemigos y explota.", "+20% de daño.", "Atrae desde más lejos.",
                    "Dura más antes de explotar.", "Mucho más grande y +25% de daño."),
            a("centinela", "Centinela Michi", "Guardián Real", "focus", "Hasta 5 centinelas que disparan sin parar.",
                    "Coloca un centinela michi que dispara solo al enemigo más cercano.", "Hasta 2 centinelas.", "Duran más y +20% de daño.",
                    "Hasta 3 centinelas.", "Disparan más rápido y +20% de daño."),
            a("aroma", "Aromaterapia", "Prado Florido", "baya", "Cura muchísimo y más a menudo a todos tus compañeros cercanos.",
                    "Cada pocos segundos te cura a ti y a tus compañeros cercanos, y aparta a los enemigos.", "Cura más.", "Mayor alcance.",
                    "Cura aún más y se usa antes.", "Cura mucho más."),
            a("pokebola", "Pokébola Saltarina", "Master Ball", "moneda", "5 bolas enormes que rebotan por la arena sin parar.",
                    "Lanza una Pokébola que rebota por la arena golpeando a todos.", "2 Pokébolas.", "Duran más y son más grandes.",
                    "3 Pokébolas.", "Más grandes y +25% de daño."),
            a("burbujas", "Burbujas", "Burbuja Gigante", "concha", "8 burbujas enormes que frenan a los enemigos al reventar.",
                    "Burbujas lentas que persiguen a los enemigos y revientan al tocarlos.", "3 burbujas.", "Explosión más grande.",
                    "4 burbujas y +20% de daño.", "5 burbujas y se lanzan antes."),
            a("latigo", "Látigo Cepa", "Látigo de Espinas", "casco", "4 latigazos larguísimos que golpean a todos en línea.",
                    "Un latigazo largo golpea a todos en línea hacia el enemigo más cercano.", "+15% de daño.", "También azota hacia el otro lado.",
                    "Más largo y +15% de daño.", "Aún más largo y más rápido."),
            a("lunar", "Media Luna", "Luna Llena", "xp", "10 esquirlas de luz y un daño enorme.",
                    "Lanza una media luna que, al golpear, se rompe en 3 esquirlas.", "4 esquirlas.", "5 esquirlas.",
                    "+15% de daño.", "6 esquirlas y +15% de daño."));

    static final List<Pasiva> PASIVAS = List.of(
            new Pasiva("vida", "Corazón Michi", "+20 de vida máxima (y te cura 20)."),
            new Pasiva("regen", "Hierba Curativa", "Recuperas +0,6 de vida por segundo."),
            new Pasiva("vel", "Zapatos Veloces", "+8% de velocidad."),
            new Pasiva("dano", "Garras Afiladas", "+12% de daño."),
            new Pasiva("recarga", "Reloj de Arena", "Tus ataques se usan un 8% más rápido."),
            new Pasiva("area", "Lupa Estelar", "+10% de tamaño en tus ataques."),
            new Pasiva("iman", "Imán de Estrellas", "+30% de radio para recoger gemas."),
            new Pasiva("xp", "Sombrero de Sabio", "+10% de experiencia."),
            new Pasiva("armadura", "Escudo Lunar", "Recibes un 8% menos de daño."),
            // ---- objetos nuevos de la fase 2B (con nombres de objetos Pokémon)
            new Pasiva("critico", "Periscopio", "+8% de probabilidad de golpe crítico (el doble de daño)."),
            new Pasiva("concha", "Campana Concha", "Recuperas +0,4 de vida por cada Pokémon derrotado."),
            new Pasiva("focus", "Banda Focus", "Si te dieran un golpe mortal, te quedas con 1 de vida (se recarga: 160 s al nivel 1, 80 s al 5)."),
            new Pasiva("moneda", "Moneda Amuleto", "+10% de michiestrellitas al acabar la partida."),
            new Pasiva("casco", "Casco Dentado", "Quien te golpea sufre 8 de daño por nivel y sale despedido."),
            new Pasiva("experto", "Cinta Experto", "+15% de daño a campeones y jefes."),
            new Pasiva("baya", "Baya Zidra", "Al bajar del 40% de vida te curas un 25% (se recarga: 60 s al nivel 1, 20 s al 5)."));

    public static Arma arma(String id) { for (Arma a : ARMAS) if (a.id().equals(id)) return a; return null; }
    public static Pasiva pasiva(String id) { for (Pasiva p : PASIVAS) if (p.id().equals(id)) return p; return null; }

    /** Nombre del ataque en ese nivel (el de la evolucion si es el 6). */
    public static String nombre(Arma a, int nivel) { return nivel >= NIVEL_EVO && a.evo() != null ? a.evo().nombre() : a.nombre(); }

    /** Icono del ataque en ese nivel (el de la evolucion si es el 6). */
    public static String icono(Arma a, int nivel) { return nivel >= NIVEL_EVO && a.evo() != null ? a.evo().icono() : a.icono(); }

    /** Icono del ataque con el que EMPIEZA un Pokemon de ese tipo (cada tipo tiene el suyo: llamas, gotas, hielo...). */
    public static String iconoInicial(String tipo) { return "i_" + (POR_TIPO.containsKey(tipo) ? tipo : "normal"); }

    /** Icono del ataque {@code a} en ese nivel para un Pokemon cuyo ataque inicial es {@code idInicial} y es de ese tipo: el inicial lleva el dibujo de su tipo hasta que evoluciona. */
    public static String icono(Arma a, int nivel, String idInicial, String tipo) {
        if (nivel >= NIVEL_EVO && a.evo() != null) return a.evo().icono();
        return a.id().equals(idInicial) ? iconoInicial(tipo) : a.icono();
    }

    /** ¿Puede evolucionar este ataque ahora? Esta al maximo (nivel 5) y llevas el objeto que pide. */
    static boolean puedeEvolucionar(Arma a, int nivelArma, Map<String, Integer> pasivas) {
        return a != null && a.evo() != null && nivelArma == MAX_NIVEL && pasivas.getOrDefault(a.evo().requiere(), 0) >= 1;
    }

    /** Los ataques (de {@code armas}) que evolucionan con la pasiva {@code pasivaId}: para decirlo en la carta del objeto. */
    static List<Arma> evolucionanCon(String pasivaId, Map<String, Integer> armas) {
        List<Arma> l = new ArrayList<>();
        for (Arma a : ARMAS) if (a.evo() != null && a.evo().requiere().equals(pasivaId) && armas.containsKey(a.id())) l.add(a);
        return l;
    }

    // ------------------------------------------------------------ mejoras repetibles (cuando ya no queda nada que subir)

    /** Cuanta vida maxima da la {@code n}-esima vez que coges «Más vida» (cada vez un 7 % menos que la anterior). */
    static double ganaVidaExtra(int n) { return 20 * Math.pow(0.93, Math.max(0, n - 1)); }

    /** Cuanta regeneracion (vida por segundo) da la {@code n}-esima vez que coges «Más regeneración» (cada vez un 10 % menos). */
    static double ganaRegenExtra(int n) { return 0.4 * Math.pow(0.90, Math.max(0, n - 1)); }

    /** Vida maxima total de haber cogido «Más vida» {@code n} veces (suma de las ganancias; nunca pasa de ~286). */
    static double vidaExtra(int n) { return n <= 0 ? 0 : 20 * (1 - Math.pow(0.93, n)) / 0.07; }

    /** Regeneracion total de haber cogido «Más regeneración» {@code n} veces (nunca pasa de 4 por segundo). */
    static double regenExtra(int n) { return n <= 0 ? 0 : 0.4 * (1 - Math.pow(0.90, n)) / 0.10; }

    // ------------------------------------------------------------ vida de la Banda Focus y de la Baya Zidra

    /** Segundos que tarda en recargarse la Banda Focus segun su nivel (1..5). */
    static int focusSegundos(int nivel) { return 180 - 20 * Math.max(1, Math.min(MAX_NIVEL, nivel)); }

    /** Segundos que tarda en recargarse la Baya Zidra segun su nivel (1..5). */
    static int bayaSegundos(int nivel) { return 70 - 10 * Math.max(1, Math.min(MAX_NIVEL, nivel)); }

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
                case "critico" -> s.critico = Math.min(0.5, s.critico + 0.08 * n);
                case "concha" -> s.concha += 0.4 * n;
                case "focus" -> s.focus = Math.max(s.focus, Math.min(MAX_NIVEL, n));
                case "moneda" -> s.monedas *= 1 + 0.10 * n;
                case "casco" -> s.casco += 8 * n;
                case "experto" -> s.experto += 0.15 * n;
                case "baya" -> s.baya = Math.max(s.baya, Math.min(MAX_NIVEL, n));
                default -> { }
            }
        }
    }

    /** Suma las mejoras repetibles («vida», «regen» -> veces cogidas) a las estadisticas. */
    static void aplicarExtras(Stats s, Map<String, Integer> extras) {
        s.hpMax += vidaExtra(extras.getOrDefault("vida", 0));
        s.regen += regenExtra(extras.getOrDefault("regen", 0));
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
