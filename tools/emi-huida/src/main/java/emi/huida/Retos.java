package emi.huida;

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
 * Las estrellas que se ganan corriendo, las misiones (3 del dia y 3 de la semana, las mismas para todos y las elige la fecha) y los logros (familias con varios
 * escalones) de la huida. Todo se apunta al acabar cada huida ({@link #aplicar}); lo ganado son <b>estrellas</b>, la moneda de la tienda de la huida. Los numeros son una
 * primera estimacion del asistente que el dueño puede retocar aqui. Sin Minecraft (solo {@link Datos}), asi se prueba con un main (tools/emi-huida/test/PruebaLogica.java).
 */
final class Retos {
    private Retos() {}

    /**
     * Lo que se mide de la huida de un jugador cuando acaba. {@code puntos} = lo que sumaron sus michiestrellas, {@code michiestrellas} = cuantas cogio, {@code puesto} = su puesto en la clasificacion
     * ({@code jugadores} = cuantos corrian: 1 = en solitario), {@code veredicto} = lo que dijo la tabla del nivel (2 primera marca, 1 mejora la suya, 0 no la mejora).
     */
    record Partida(String nivel, boolean escapo, int puntos, int michiestrellas, int errores, double tiempo, int puesto, int jugadores, int veredicto) {
        boolean perfecta() { return escapo && errores == 0; }
        boolean enGrupo() { return jugadores > 1; }
    }

    /** El avance de una mision en un jugador (la mision es la de ese id). */
    static final class Prog {
        String id = "";
        int prog;
        boolean hecha;
    }

    /** Lo que ha pasado al apuntar una huida: lineas para contarselo al jugador y las estrellas ganadas (por la huida, por misiones y por logros). */
    static final class Novedades {
        final List<String> lineas = new ArrayList<>();
        int porHuida, porRecord, porMisiones, porLogros;
        int total() { return porHuida + porRecord + porMisiones + porLogros; }
    }

    // ------------------------------------------------------------ estrellas por huida

    static final int BASE_ESCAPE = 10, BASE_ATRAPADO = 3, PREMIO_RECORD = 5;

    /** Estrellas de la medalla: oro (0 errores) 10, plata (1-2 errores) 5, bronce 0. */
    static int porMedalla(boolean escapo, int errores) { return !escapo ? 0 : errores == 0 ? 10 : errores <= 2 ? 5 : 0; }

    /** Estrellas por la posicion (solo con mas de un jugador): 1.º 20, 2.º 12, 3.º 8, el resto 4. */
    static int porPuesto(int puesto, int jugadores) {
        if (jugadores < 2 || puesto <= 0) return 0;
        return switch (puesto) { case 1 -> 20; case 2 -> 12; case 3 -> 8; default -> 4; };
    }

    /** Estrellas por las michiestrellas recogidas: una por cada 5 puntos (se cobran aunque te alcancen). */
    static int porPuntos(int puntos) { return Math.max(0, puntos) / 5; }

    /** Estrellas que da una huida: escapar da 10 (3 si te alcanzan) + la medalla + los puntos recogidos + el puesto. */
    static int estrellasDe(Partida p) {
        return (p.escapo() ? BASE_ESCAPE : BASE_ATRAPADO) + porMedalla(p.escapo(), p.errores()) + porPuntos(p.puntos()) + porPuesto(p.puesto(), p.jugadores());
    }

    // ------------------------------------------------------------ misiones

    /** SUMA = se acumula entre huidas dentro del periodo; MAX = vale la mejor huida del periodo. */
    enum Modo { SUMA, MAX }

    /** Una mision posible. {@code metrica}: partidas, escapes, michiestrellas, puntos, perfectas o grupo. */
    record Def(String id, String texto, String metrica, Modo modo, int objetivo, int premio) {}

    static final List<Def> DIARIAS = List.of(
            new Def("d_partidas_1", "Juega 1 huida", "partidas", Modo.SUMA, 1, 5),
            new Def("d_partidas_3", "Juega 3 huidas", "partidas", Modo.SUMA, 3, 12),
            new Def("d_escapa_1", "Escapa 1 vez", "escapes", Modo.SUMA, 1, 8),
            new Def("d_escapa_2", "Escapa 2 veces", "escapes", Modo.SUMA, 2, 15),
            new Def("d_estrellas_60", "Recoge 60 michiestrellas", "michiestrellas", Modo.SUMA, 60, 6),
            new Def("d_estrellas_150", "Recoge 150 michiestrellas", "michiestrellas", Modo.SUMA, 150, 12),
            new Def("d_puntos_150", "Consigue 150 puntos en una huida", "puntos", Modo.MAX, 150, 8),
            new Def("d_puntos_250", "Consigue 250 puntos en una huida", "puntos", Modo.MAX, 250, 15),
            new Def("d_perfecta_1", "Escapa sin ningún error", "perfectas", Modo.SUMA, 1, 12),
            new Def("d_grupo_1", "Corre una huida con más jugadores", "grupo", Modo.SUMA, 1, 8));

    static final List<Def> SEMANALES = List.of(
            new Def("s_partidas_8", "Juega 8 huidas", "partidas", Modo.SUMA, 8, 35),
            new Def("s_escapa_5", "Escapa 5 veces", "escapes", Modo.SUMA, 5, 45),
            new Def("s_estrellas_500", "Recoge 500 michiestrellas", "michiestrellas", Modo.SUMA, 500, 40),
            new Def("s_puntos_280", "Consigue 280 puntos en una huida", "puntos", Modo.MAX, 280, 40),
            new Def("s_perfecta_3", "Escapa sin errores 3 veces", "perfectas", Modo.SUMA, 3, 55),
            new Def("s_grupo_3", "Corre 3 huidas con más jugadores", "grupo", Modo.SUMA, 3, 35));

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
        Collections.shuffle(mezcla, new Random(clave.hashCode() * 31L + 11));
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

    /** Lo que vale esa medida en esta huida. */
    static int medida(Partida p, String metrica) {
        return switch (metrica) {
            case "partidas" -> 1;
            case "escapes" -> p.escapo() ? 1 : 0;
            case "michiestrellas" -> p.michiestrellas();
            case "puntos" -> p.puntos();
            case "perfectas" -> p.perfecta() ? 1 : 0;
            case "grupo" -> p.enGrupo() ? 1 : 0;
            default -> 0;
        };
    }

    private static void avanzar(Datos.Jugador j, List<Prog> lista, Partida s, String periodo, Novedades n) {
        for (Prog p : lista) {
            Def d = def(p.id);
            if (d == null || p.hecha) continue;
            int m = medida(s, d.metrica());
            p.prog = Math.min(d.objetivo(), d.modo() == Modo.SUMA ? p.prog + m : Math.max(p.prog, m));
            if (p.prog >= d.objetivo()) {
                p.hecha = true;
                n.porMisiones += d.premio();
                n.lineas.add("¡Misión " + periodo + " cumplida: " + d.texto() + "! +" + d.premio() + " ★");
            }
        }
    }

    // ------------------------------------------------------------ logros

    /** Un logro con varios escalones (cada uno con su premio en estrellas). {@code unidad}: «n» = el numero tal cual, «min» = los valores estan en segundos y se leen en minutos. */
    record Familia(String id, String nombre, String texto, String unidad, long[] escalones, int[] premios, ToLongFunction<Datos.Jugador> valor) {}

    static final List<Familia> FAMILIAS = List.of(
            new Familia("escapista", "Escapista", "Escapa {n} veces", "n", new long[]{1, 10, 50, 200}, new int[]{10, 30, 80, 200}, j -> j.escapes),
            new Familia("perfecto", "Perfecto", "Escapa sin errores {n} veces", "n", new long[]{1, 5, 25, 100}, new int[]{15, 40, 100, 250}, j -> j.perfectos),
            new Familia("cazaestrellas", "Cazaestrellas", "Recoge {n} michiestrellas", "n", new long[]{50, 500, 2500, 10000}, new int[]{10, 30, 80, 200}, j -> j.michiestrellas),
            new Familia("puntuador", "Gran puntuación", "Consigue {n} puntos en una sola huida", "n", new long[]{100, 200, 290}, new int[]{10, 40, 100}, j -> j.mejorPuntos),
            new Familia("viajero", "Viajero", "Escapa en {n} niveles distintos", "n", new long[]{2, 3}, new int[]{20, 50}, j -> j.escapesNivel.values().stream().filter(v -> v > 0).count()),
            new Familia("rey", "Rey de la huida", "Gana {n} huidas contra otros jugadores", "n", new long[]{1, 5, 25}, new int[]{20, 50, 120}, j -> j.victorias),
            new Familia("podio", "Sube al podio", "Acaba {n} veces entre los 3 primeros con otros jugadores", "n", new long[]{1, 10, 50}, new int[]{15, 40, 120}, j -> j.podios),
            new Familia("tyrantrum", "Doma-dinos", "Escapa {n} veces del Tyrantrum", "n", new long[]{1, 10, 30}, new int[]{15, 40, 100}, j -> j.escapesNivel.getOrDefault("tyrantrum", 0)),
            new Familia("calabaza", "Cazacalabazas", "Escapa {n} veces de la calabaza", "n", new long[]{1, 10, 30}, new int[]{20, 50, 120}, j -> j.escapesNivel.getOrDefault("calabaza", 0)),
            new Familia("michialiens", "Amigo de los michialiens", "Escapa {n} veces de los michialiens", "n", new long[]{1, 10, 30}, new int[]{25, 60, 140}, j -> j.escapesNivel.getOrDefault("cosmos", 0)),
            new Familia("records", "Rompe-récords", "Mejora tu mejor puntuación {n} veces", "n", new long[]{1, 10, 50}, new int[]{10, 30, 100}, j -> j.mejoradas),
            new Familia("fiel", "Fiel a la huida", "Pasa {n} corriendo", "min", new long[]{1800, 10800, 36000}, new int[]{15, 50, 150}, j -> j.segundos));

    static Familia familia(String id) {
        for (Familia f : FAMILIAS) if (f.id().equals(id)) return f;
        return null;
    }

    /** Da los escalones de logros que ya se cumplen (con su premio en estrellas) y anota las lineas. */
    static void comprobarLogros(Datos.Jugador j, Novedades n) {
        for (Familia f : FAMILIAS) {
            int hechos = Math.min(j.logros.getOrDefault(f.id(), 0), f.escalones().length);
            long valor = f.valor().applyAsLong(j);
            while (hechos < f.escalones().length && valor >= f.escalones()[hechos]) {
                int premio = f.premios()[hechos];
                n.porLogros += premio;
                n.lineas.add("★ Logro «" + f.nombre() + "» " + (hechos + 1) + "/" + f.escalones().length + ": " + texto(f, f.escalones()[hechos]) + " (+" + premio + " estrellas)");
                hechos++;
            }
            j.logros.put(f.id(), hechos);
        }
    }

    // ------------------------------------------------------------ aplicar una huida

    /** Apunta la huida en las estadisticas, misiones y logros del jugador y le da lo ganado en estrellas. */
    static Novedades aplicar(Datos.Jugador j, Partida p, long ahoraMs, ZoneId zona) {
        Novedades n = new Novedades();
        refrescar(j, ahoraMs, zona);
        // estadisticas
        j.huidas++;
        j.segundos += Math.max(0, Math.round(p.tiempo()));
        j.michiestrellas += p.michiestrellas();
        j.mejorPuntos = Math.max(j.mejorPuntos, p.puntos());
        if (p.escapo()) { j.escapes++; j.escapesNivel.merge(p.nivel(), 1, Integer::sum); if (p.perfecta()) j.perfectos++; } else j.atrapado++;
        if (p.enGrupo()) {
            if (p.puesto() == 1) j.victorias++;
            if (p.puesto() >= 1 && p.puesto() <= 3) j.podios++;
        }
        if (p.escapo() && p.veredicto() == 1) j.mejoradas++;
        // estrellas de la huida
        n.porHuida = estrellasDe(p);
        n.lineas.add("+" + n.porHuida + " estrellas por la huida (" + (p.escapo() ? BASE_ESCAPE + " por escapar" : BASE_ATRAPADO + " por intentarlo")
                + (porMedalla(p.escapo(), p.errores()) > 0 ? " + " + porMedalla(p.escapo(), p.errores()) + " de la medalla" : "")
                + (porPuntos(p.puntos()) > 0 ? " + " + porPuntos(p.puntos()) + " de las michiestrellas" : "")
                + (porPuesto(p.puesto(), p.jugadores()) > 0 ? " + " + porPuesto(p.puesto(), p.jugadores()) + " del puesto" : "") + ")");
        if (p.escapo() && p.veredicto() == 1) { n.porRecord = PREMIO_RECORD; n.lineas.add("+" + PREMIO_RECORD + " estrellas por batir tu mejor puntuación en este nivel"); }
        // misiones y logros
        avanzar(j, j.dia, p, "del día", n);
        avanzar(j, j.semana, p, "de la semana", n);
        comprobarLogros(j, n);
        int total = n.total();
        j.estrellas += total; j.estrellasGanadas += total;
        return n;
    }

    // ------------------------------------------------------------ textos

    static String texto(Familia f, long meta) {
        String s = f.texto().replace("{n}", formato(meta, f.unidad()));
        if (meta == 1) s = s.replace(" 1 veces", " 1 vez").replace(" 1 niveles distintos", " 1 nivel").replace(" 1 huidas", " 1 huida").replace(" 1 puntos", " 1 punto");
        return s;
    }

    static String formato(long v, String unidad) {
        return switch (unidad) {
            case "min" -> v >= 120 && v % 60 == 0 ? (v / 60) + " min" : (v / 60) + " min " + (v % 60 == 0 ? "" : (v % 60) + " s");
            case "h" -> (v / 3600) + " h";
            default -> String.valueOf(v);
        };
    }

    /** Un tiempo en segundos como «m:ss». */
    static String tiempo(double seg) {
        int s = (int) Math.round(seg);
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }
}
