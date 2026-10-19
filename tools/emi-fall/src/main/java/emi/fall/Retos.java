package emi.fall;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Las estrellas que se ganan jugando al torneo y las misiones (3 del dia y 3 de la semana, las mismas para todos; las elige la fecha) de Emi Fall. Se apunta todo al acabar cada
 * ronda ({@link #aplicarRonda}) y cada torneo ({@link #aplicarTorneo}); lo ganado son <b>estrellas</b>, la moneda de la tienda de Emi Fall. Los numeros son una primera estimacion del
 * asistente que el dueño puede retocar aqui. Sin Minecraft (solo {@link Datos}).
 */
final class Retos {
    private Retos() {}

    /** Una ronda de un jugador: el juego, su puesto entre {@code jugadores}, y {@code cuenta} = dulces recogidos o muros esquivados (segun el juego). */
    record Ronda(String juego, int puesto, int jugadores, boolean sinElim, int cuenta) {
        boolean enGrupo() { return jugadores > 1; }
    }

    /** Un torneo acabado: el puesto final, los puntos y las rondas jugadas. */
    record Torneo(int puesto, int jugadores, boolean sinElim, int puntos, int rondas, long segundos) {
        boolean enGrupo() { return jugadores > 1; }
    }

    /** El avance de una mision en un jugador (la mision es la de ese id). */
    static final class Prog {
        String id = "";
        int prog;
        boolean hecha;
    }

    /** Lo ganado en un torneo, para contarselo al jugador. */
    static final class Novedades {
        final List<String> lineas = new ArrayList<>();
        int porRondas, porTorneo, porMisiones;
        int total() { return porRondas + porTorneo + porMisiones; }
    }

    // ------------------------------------------------------------ estrellas

    static final int BASE_RONDA = 3, BASE_TORNEO = 10;

    /** Por el puesto de una ronda (solo con mas de un jugador): 1.º 10, 2.º 6, 3.º 4, el resto 2. */
    static int porPuestoRonda(int puesto, int jugadores) {
        if (jugadores < 2 || puesto <= 0) return 0;
        return switch (puesto) { case 1 -> 10; case 2 -> 6; case 3 -> 4; default -> 2; };
    }

    /** Por el puesto final del torneo (solo con mas de un jugador): 1.º 30, 2.º 18, 3.º 10, el resto 5. */
    static int porPuestoTorneo(int puesto, int jugadores) {
        if (jugadores < 2 || puesto <= 0) return 0;
        return switch (puesto) { case 1 -> 30; case 2 -> 18; case 3 -> 10; default -> 5; };
    }

    // ------------------------------------------------------------ misiones

    /** SUMA = se acumula entre rondas/torneos dentro del periodo; MAX = vale lo mejor del periodo. */
    enum Modo { SUMA, MAX }

    /** Una mision posible. {@code metrica}: rondas, ganadas, torneos, dulces, muros, sinelim, elim, podio, grupo o puntos. */
    record Def(String id, String texto, String metrica, Modo modo, int objetivo, int premio) {}

    static final List<Def> DIARIAS = List.of(
            new Def("d_rondas_4", "Juega 4 rondas", "rondas", Modo.SUMA, 4, 5),
            new Def("d_rondas_8", "Juega 8 rondas", "rondas", Modo.SUMA, 8, 12),
            new Def("d_torneo_1", "Termina un torneo", "torneos", Modo.SUMA, 1, 12),
            new Def("d_ganadas_1", "Gana una ronda contra otros jugadores", "ganadas", Modo.SUMA, 1, 8),
            new Def("d_dulces_20", "Recoge 20 dulces en el Cazacaramelos", "dulces", Modo.SUMA, 20, 6),
            new Def("d_dulces_45", "Recoge 45 dulces en el Cazacaramelos", "dulces", Modo.SUMA, 45, 12),
            new Def("d_muros_6", "Esquiva 6 muros fantasma", "muros", Modo.SUMA, 6, 8),
            new Def("d_sinelim_1", "Termina un torneo sin eliminaciones", "sinelim", Modo.SUMA, 1, 12),
            new Def("d_elim_1", "Termina un torneo con eliminaciones", "elim", Modo.SUMA, 1, 12),
            new Def("d_podio_1", "Acaba un torneo entre los 3 primeros con otros jugadores", "podio", Modo.SUMA, 1, 15),
            new Def("d_grupo_1", "Juega un torneo con más jugadores", "grupo", Modo.SUMA, 1, 8));

    static final List<Def> SEMANALES = List.of(
            new Def("s_rondas_24", "Juega 24 rondas", "rondas", Modo.SUMA, 24, 35),
            new Def("s_torneos_3", "Termina 3 torneos", "torneos", Modo.SUMA, 3, 45),
            new Def("s_ganadas_5", "Gana 5 rondas contra otros jugadores", "ganadas", Modo.SUMA, 5, 45),
            new Def("s_dulces_150", "Recoge 150 dulces en el Cazacaramelos", "dulces", Modo.SUMA, 150, 40),
            new Def("s_muros_40", "Esquiva 40 muros fantasma", "muros", Modo.SUMA, 40, 40),
            new Def("s_podio_2", "Acaba 2 torneos entre los 3 primeros con otros jugadores", "podio", Modo.SUMA, 2, 55),
            new Def("s_grupo_3", "Juega 3 torneos con más jugadores", "grupo", Modo.SUMA, 3, 35));

    static final int POR_PERIODO = 3;

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

    private static int medidaRonda(Ronda r, String m) {
        return switch (m) {
            case "rondas" -> 1;
            case "ganadas" -> r.puesto() == 1 && r.enGrupo() ? 1 : 0;
            case "dulces" -> r.juego().equals("caramelos") ? r.cuenta() : 0;
            case "muros" -> r.juego().equals("muro") ? r.cuenta() : 0;
            default -> 0;
        };
    }

    private static int medidaTorneo(Torneo t, String m) {
        return switch (m) {
            case "torneos" -> 1;
            case "sinelim" -> t.sinElim() ? 1 : 0;
            case "elim" -> t.sinElim() ? 0 : 1;
            case "podio" -> t.enGrupo() && t.puesto() >= 1 && t.puesto() <= 3 ? 1 : 0;
            case "grupo" -> t.enGrupo() ? 1 : 0;
            case "puntos" -> t.puntos();
            default -> 0;
        };
    }

    private static boolean esDeRonda(String m) { return m.equals("rondas") || m.equals("ganadas") || m.equals("dulces") || m.equals("muros"); }

    private static void avanzar(List<Prog> lista, String periodo, Novedades n, java.util.function.ToIntFunction<String> medida, boolean deRonda) {
        for (Prog p : lista) {
            Def d = def(p.id);
            if (d == null || p.hecha || esDeRonda(d.metrica()) != deRonda) continue;
            int m = medida.applyAsInt(d.metrica());
            p.prog = Math.min(d.objetivo(), d.modo() == Modo.SUMA ? p.prog + m : Math.max(p.prog, m));
            if (p.prog >= d.objetivo()) {
                p.hecha = true;
                n.porMisiones += d.premio();
                n.lineas.add("Misión " + periodo + " cumplida: " + d.texto() + " (+" + d.premio() + " ★)");
            }
        }
    }

    // ------------------------------------------------------------ aplicar

    /** Apunta una ronda: estadisticas, misiones y las estrellas de la ronda (se suman al saldo ya). Devuelve lo ganado. */
    static Novedades aplicarRonda(Datos.Jugador j, Ronda r, long ahoraMs, ZoneId zona) {
        Novedades n = new Novedades();
        refrescar(j, ahoraMs, zona);
        j.rondas++;
        if (r.puesto() == 1 && r.enGrupo()) j.rondasGanadas++;
        if (r.juego().equals("caramelos")) j.dulces += Math.max(0, r.cuenta());
        if (r.juego().equals("muro")) j.muros += Math.max(0, r.cuenta());
        n.porRondas = BASE_RONDA + porPuestoRonda(r.puesto(), r.jugadores());
        avanzar(j.dia, "del día", n, m -> medidaRonda(r, m), true);
        avanzar(j.semana, "de la semana", n, m -> medidaRonda(r, m), true);
        int total = n.total();
        j.estrellas += total; j.estrellasGanadas += total;
        return n;
    }

    /** Apunta un torneo acabado: estadisticas, misiones y las estrellas del final. Devuelve lo ganado. */
    static Novedades aplicarTorneo(Datos.Jugador j, Torneo t, long ahoraMs, ZoneId zona) {
        Novedades n = new Novedades();
        refrescar(j, ahoraMs, zona);
        j.torneos++;
        j.segundos += Math.max(0, t.segundos());
        j.mejorPuntos = Math.max(j.mejorPuntos, t.puntos());
        if (t.sinElim()) j.torneosSinElim++;
        if (t.enGrupo()) {
            if (t.puesto() == 1) j.torneosGanados++;
            if (t.puesto() >= 1 && t.puesto() <= 3) j.podios++;
        }
        n.porTorneo = BASE_TORNEO + porPuestoTorneo(t.puesto(), t.jugadores());
        avanzar(j.dia, "del día", n, m -> medidaTorneo(t, m), false);
        avanzar(j.semana, "de la semana", n, m -> medidaTorneo(t, m), false);
        int total = n.total();
        j.estrellas += total; j.estrellasGanadas += total;
        return n;
    }
}
