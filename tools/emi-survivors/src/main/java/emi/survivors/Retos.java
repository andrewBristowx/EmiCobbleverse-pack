package emi.survivors;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.ToLongFunction;

/**
 * Las misiones (3 del dia y 3 de la semana, las mismas para todos y las elige la fecha) y los logros (familias con varios escalones) del minijuego. Todo se mide al
 * acabar cada partida que cuenta ({@link #aplicar}) y las recompensas son SOLO coronas (una moneda aparte, mucho mas escasa que las michiestrellitas, que son solo para las mejoras); los premios de verdad los sigue dando el dueño a mano.
 * Sin Minecraft (solo Datos), asi se prueba con un main.
 */
final class Retos {
    private Retos() {}

    /** Lo que se mide de la partida de un jugador cuando acaba. {@code jefes} = jefes que mato el equipo; {@code jugadores} = cuantos jugaban (1 = en solitario). */
    record Partida(int segundos, int derrotados, int nivel, int jefes, String bioma, int jugadores, int evoluciones, String especie) {}

    /** El avance de una mision en un jugador (la mision es la de ese id). */
    static final class Prog {
        String id = "";
        int prog;
        boolean hecha;
    }

    /** SUMA = se acumula entre partidas dentro del periodo; MAX = vale la mejor partida del periodo. */
    enum Modo { SUMA, MAX }

    /** Una mision posible. {@code metrica}: derrotados, partidas, segundos, nivel, jefes, evoluciones o grupo (partidas con mas de un jugador). */
    record Def(String id, String texto, String metrica, Modo modo, int objetivo, int premio) {}

    static final List<Def> DIARIAS = List.of(
            new Def("d_derrota_100", "Derrota a 100 Pokémon", "derrotados", Modo.SUMA, 100, 1),
            new Def("d_derrota_300", "Derrota a 300 Pokémon", "derrotados", Modo.SUMA, 300, 2),
            new Def("d_partidas_2", "Juega 2 partidas", "partidas", Modo.SUMA, 2, 1),
            new Def("d_partidas_4", "Juega 4 partidas", "partidas", Modo.SUMA, 4, 2),
            new Def("d_aguanta_3", "Aguanta 3 minutos en una partida", "segundos", Modo.MAX, 180, 1),
            new Def("d_aguanta_6", "Aguanta 6 minutos en una partida", "segundos", Modo.MAX, 360, 2),
            new Def("d_nivel_8", "Llega al nivel 8 en una partida", "nivel", Modo.MAX, 8, 1),
            new Def("d_nivel_15", "Llega al nivel 15 en una partida", "nivel", Modo.MAX, 15, 2),
            new Def("d_jefe_1", "Derrota a un jefe", "jefes", Modo.SUMA, 1, 2),
            new Def("d_grupo_1", "Juega una partida en grupo", "grupo", Modo.SUMA, 1, 1),
            new Def("d_evo_1", "Evoluciona un ataque", "evoluciones", Modo.SUMA, 1, 2));

    static final List<Def> SEMANALES = List.of(
            new Def("s_derrota_1000", "Derrota a 1000 Pokémon", "derrotados", Modo.SUMA, 1000, 5),
            new Def("s_derrota_3000", "Derrota a 3000 Pokémon", "derrotados", Modo.SUMA, 3000, 8),
            new Def("s_partidas_10", "Juega 10 partidas", "partidas", Modo.SUMA, 10, 4),
            new Def("s_jefes_5", "Derrota a 5 jefes", "jefes", Modo.SUMA, 5, 6),
            new Def("s_aguanta_10", "Aguanta 10 minutos en una partida", "segundos", Modo.MAX, 600, 5),
            new Def("s_nivel_25", "Llega al nivel 25 en una partida", "nivel", Modo.MAX, 25, 5),
            new Def("s_evo_5", "Evoluciona 5 ataques", "evoluciones", Modo.SUMA, 5, 6),
            new Def("s_grupo_3", "Juega 3 partidas en grupo", "grupo", Modo.SUMA, 3, 4));

    static final int POR_PERIODO = 3;

    /**
     * Un logro con varios escalones (cada uno con su recompensa). {@code unidad}: «n» = el numero tal cual, «min» = los valores estan en segundos y se leen en minutos,
     * «h» = en segundos y se leen en horas. {@code texto} lleva «{n}» donde va el objetivo del escalon.
     */
    record Familia(String id, String nombre, String texto, String unidad, long[] escalones, int[] premios, ToLongFunction<Datos.Jugador> valor) {}

    static final List<Familia> FAMILIAS = List.of(
            new Familia("cazador", "Cazador", "Derrota a {n} Pokémon en total", "n", new long[]{100, 1000, 5000, 20000}, new int[]{1, 3, 8, 20}, j -> j.derrotadosTotal),
            new Familia("veterano", "Veterano", "Juega {n} partidas", "n", new long[]{1, 10, 50, 200}, new int[]{1, 2, 6, 15}, j -> j.partidas),
            new Familia("aguante", "Aguante", "Aguanta {n} en una sola partida", "min", new long[]{300, 600, 900, 1200}, new int[]{2, 4, 10, 15}, j -> j.mejorTiempo),
            new Familia("nivel", "Campeón", "Llega al nivel {n} en una partida", "n", new long[]{10, 20, 30, 40}, new int[]{1, 3, 6, 10}, j -> j.mejorNivel),
            new Familia("matajefes", "Matajefes", "Derrota a {n} jefes en total", "n", new long[]{1, 5, 25, 100}, new int[]{2, 4, 10, 25}, j -> j.jefesTotal),
            new Familia("rey", "Rey de reyes", "Derrota a {n} jefes en una sola partida", "n", new long[]{3, 5}, new int[]{5, 15}, j -> j.mejorJefes),
            new Familia("evolucionista", "Evolucionista", "Evoluciona {n} ataques en total", "n", new long[]{1, 10, 50}, new int[]{2, 4, 12}, j -> j.evolucionesTotal),
            new Familia("amigos", "Mejores amigos", "Juega {n} partidas en grupo", "n", new long[]{1, 10, 50}, new int[]{2, 5, 12}, j -> j.partidasGrupo),
            new Familia("viajero", "Viajero", "Juega en {n} arenas distintas", "n", new long[]{2, 3}, new int[]{2, 3}, j -> j.biomas.size()),
            new Familia("coleccionista", "Coleccionista", "Juega con {n} Pokémon distintos", "n", new long[]{3, 10, 25}, new int[]{2, 4, 10}, j -> j.especies.size()),
            new Familia("fiel", "Fiel al juego", "Juega {n} en total", "h", new long[]{3600, 36000, 180000}, new int[]{2, 8, 20}, j -> j.segundosTotal));

    /** Lo que ha pasado al apuntar una partida: lineas para contarselo al jugador y las coronas que ha ganado. */
    static final class Novedades {
        final List<String> lineas = new ArrayList<>();
        int coronas;
    }

    // ------------------------------------------------------------ fechas

    static String claveDia(long ms, ZoneId zona) { return Instant.ofEpochMilli(ms).atZone(zona).toLocalDate().toString(); }

    /** La clave de la semana es la fecha de su lunes. */
    static String claveSemana(long ms, ZoneId zona) {
        LocalDate d = Instant.ofEpochMilli(ms).atZone(zona).toLocalDate();
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
    }

    /** Las 3 misiones de un periodo: salen de la clave (la misma fecha da siempre las mismas, para todos) y no se repite la misma medida dos veces. */
    static List<Def> elegir(List<Def> pool, String clave) {
        List<Def> mezcla = new ArrayList<>(pool);
        Collections.shuffle(mezcla, new Random(clave.hashCode() * 31L + 7));
        List<Def> r = new ArrayList<>();
        outer:
        for (Def d : mezcla) {
            for (Def y : r) if (y.metrica().equals(d.metrica())) continue outer;
            r.add(d);
            if (r.size() == POR_PERIODO) break;
        }
        return r;
    }

    static Def def(String id) {
        for (Def d : DIARIAS) if (d.id().equals(id)) return d;
        for (Def d : SEMANALES) if (d.id().equals(id)) return d;
        return null;
    }

    private static List<Prog> nuevas(List<Def> pool, String clave) {
        List<Prog> l = new ArrayList<>();
        for (Def d : elegir(pool, clave)) { Prog p = new Prog(); p.id = d.id(); l.add(p); }
        return l;
    }

    /** Pone al dia las misiones del jugador: si ha cambiado el dia o la semana, las de antes se pierden y salen las nuevas. */
    static void refrescar(Datos.Jugador j, long ahoraMs, ZoneId zona) {
        String cd = claveDia(ahoraMs, zona), cs = claveSemana(ahoraMs, zona);
        if (!cd.equals(j.diaClave) || j.dia == null || j.dia.isEmpty()) { j.diaClave = cd; j.dia = nuevas(DIARIAS, cd); }
        if (!cs.equals(j.semanaClave) || j.semana == null || j.semana.isEmpty()) { j.semanaClave = cs; j.semana = nuevas(SEMANALES, cs); }
    }

    // ------------------------------------------------------------ partidas

    /** Lo que vale esa medida en esta partida. */
    static int medida(Partida s, String metrica) {
        return switch (metrica) {
            case "derrotados" -> s.derrotados();
            case "partidas" -> 1;
            case "segundos" -> s.segundos();
            case "nivel" -> s.nivel();
            case "jefes" -> s.jefes();
            case "evoluciones" -> s.evoluciones();
            case "grupo" -> s.jugadores() > 1 ? 1 : 0;
            default -> 0;
        };
    }

    /**
     * Apunta una partida (que cuenta para el top) en las misiones y los logros del jugador. Suma las recompensas a sus coronas y las cuenta en {@link Novedades}.
     * Hay que llamarlo DESPUES de apuntar la partida en el jugador ({@link Datos#registrarJugador}: suma {@code partidas} y {@code mejorTiempo}).
     */
    static Novedades aplicar(Datos.Jugador j, Partida s, long ahoraMs, ZoneId zona) {
        Novedades n = new Novedades();
        refrescar(j, ahoraMs, zona);
        // contadores de los logros
        j.derrotadosTotal += s.derrotados();
        j.jefesTotal += s.jefes();
        j.evolucionesTotal += s.evoluciones();
        if (s.jugadores() > 1) j.partidasGrupo++;
        j.mejorNivel = Math.max(j.mejorNivel, s.nivel());
        j.mejorJefes = Math.max(j.mejorJefes, s.jefes());
        j.segundosTotal += s.segundos();
        if (s.bioma() != null && !s.bioma().isEmpty() && !j.biomas.contains(s.bioma())) j.biomas.add(s.bioma());
        if (s.especie() != null && !s.especie().isEmpty() && !j.especies.contains(s.especie())) j.especies.add(s.especie());
        // misiones del dia y de la semana
        avanzar(j, j.dia, s, "del día", n);
        avanzar(j, j.semana, s, "de la semana", n);
        // logros
        for (Familia f : FAMILIAS) {
            int hechos = j.logros.getOrDefault(f.id(), 0);
            long v = f.valor().applyAsLong(j);
            while (hechos < f.escalones().length && v >= f.escalones()[hechos]) {
                int premio = f.premios()[hechos];
                hechos++;
                n.coronas += premio;
                n.lineas.add("¡Logro «" + f.nombre() + "» " + hechos + "/" + f.escalones().length + ": " + texto(f, f.escalones()[hechos - 1]) + "! +" + coronas(premio) + ".");
            }
            if (hechos > 0) j.logros.put(f.id(), hechos);
        }
        j.coronas += n.coronas;
        return n;
    }

    private static void avanzar(Datos.Jugador j, List<Prog> lista, Partida s, String periodo, Novedades n) {
        for (Prog p : lista) {
            Def d = def(p.id);
            if (d == null || p.hecha) continue;
            int m = medida(s, d.metrica());
            p.prog = Math.min(d.objetivo(), d.modo() == Modo.SUMA ? p.prog + m : Math.max(p.prog, m));
            if (p.prog >= d.objetivo()) {
                p.hecha = true;
                n.coronas += d.premio();
                n.lineas.add("¡Misión " + periodo + " cumplida: " + d.texto() + "! +" + coronas(d.premio()) + ".");
            }
        }
    }

    /** El texto de un escalon con su objetivo puesto y en singular si es 1 («Juega 1 partida»). */
    static String texto(Familia f, long valor) {
        String s = f.texto().replace("{n}", formato(valor, f.unidad()));
        if (valor == 1) s = s.replace(" 1 partidas", " 1 partida").replace(" 1 jefes", " 1 jefe").replace(" 1 ataques", " 1 ataque").replace(" 1 arenas distintas", " 1 arena").replace(" 1 Pokémon distintos", " 1 Pokémon");
        return s;
    }

    static String coronas(int n) { return n + (n == 1 ? " corona" : " coronas"); }

    static String formato(long valor, String unidad) {
        return switch (unidad) {
            case "min" -> (valor / 60) + " min";
            case "h" -> (valor / 3600) + " h";
            default -> String.valueOf(valor);
        };
    }
}
