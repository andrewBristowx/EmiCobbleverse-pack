package emi.carreras;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Las estrellas que se ganan corriendo y los logros de las carreras. Todo se apunta al acabar cada carrera o entrenamiento ({@link #aplicar}); lo ganado son
 * <b>estrellas</b> (la moneda de la tienda de carreras). Los numeros son una primera estimacion que el dueño puede retocar aqui. Sin Minecraft (solo {@link Datos}), asi se
 * prueba con un main (tools/emi-carreras/test/PruebaLogica.java).
 */
final class Logros {
    private Logros() {}

    /** Lo que se mide de una carrera de un jugador cuando acaba. {@code puesto} = 0 si no la terminó; {@code jugadores} = cuantos corrian (1 = entrenamiento o en solitario). */
    record Carrera(String pista, String especie, int puesto, int jugadores, boolean entrenamiento, int recogidas, int golpesDados, int golpesRecibidos, int ticks, int duracion, boolean michi) {
        boolean terminada() { return puesto > 0; }
    }

    /** Lo que ha pasado al apuntar una carrera: lineas para contarselo al jugador y las estrellas ganadas (por la carrera y por los logros). */
    static final class Novedades {
        final List<String> lineas = new ArrayList<>();
        int porCarrera, porLogros, porRecord;
        boolean record;
        int total() { return porCarrera + porLogros + porRecord; }
    }

    /** Un logro con varios escalones (cada uno con su premio en estrellas). {@code unidad}: «n» = el numero tal cual, «min» = los valores estan en segundos y se leen en minutos, «h» = en horas. */
    record Familia(String id, String nombre, String texto, String unidad, long[] escalones, int[] premios, ToLongFunction<Datos.Jugador> valor) {}

    static final List<Familia> FAMILIAS = List.of(
            new Familia("piloto", "Piloto", "Termina {n} carreras", "n", new long[]{1, 10, 50, 200}, new int[]{10, 30, 80, 200}, j -> j.terminadas),
            new Familia("campeon", "Campeón", "Gana {n} carreras", "n", new long[]{1, 5, 25, 100}, new int[]{20, 50, 120, 300}, j -> j.victorias),
            new Familia("podio", "Sube al podio", "Acaba {n} veces entre los 3 primeros", "n", new long[]{1, 10, 50}, new int[]{15, 40, 120}, j -> j.podios),
            new Familia("estrellas", "Cazaestrellas", "Recoge {n} michiestrellitas en la pista", "n", new long[]{10, 100, 500, 2000}, new int[]{10, 30, 80, 200}, j -> j.recogidas),
            new Familia("viajero", "Viajero", "Corre en {n} pistas distintas", "n", new long[]{2, 3}, new int[]{15, 30}, j -> j.pistas.size()),
            new Familia("entrenador", "Entrenador", "Completa {n} entrenamientos", "n", new long[]{1, 10, 50}, new int[]{10, 30, 100}, j -> j.entrenamientos),
            new Familia("francotirador", "Francotirador", "Acierta {n} golpes a tus rivales", "n", new long[]{1, 10, 50, 200}, new int[]{10, 25, 60, 150}, j -> j.golpesDados),
            new Familia("intocable", "Intocable", "Gana {n} carreras sin recibir ningún golpe", "n", new long[]{1, 5, 20}, new int[]{25, 60, 150}, j -> j.intocables),
            new Familia("coleccionista", "Coleccionista", "Corre con {n} Pokémon distintos", "n", new long[]{3, 10, 25}, new int[]{15, 40, 100}, j -> j.especies.size()),
            new Familia("records", "Rompe-récords", "Mejora tu mejor tiempo {n} veces", "n", new long[]{1, 10, 50}, new int[]{10, 30, 100}, j -> j.recordsMejorados),
            new Familia("fanemi", "Fan de Emi", "Termina {n} carreras con un Michialien", "n", new long[]{1, 10, 50}, new int[]{15, 40, 100}, j -> j.michiTerminadas),
            new Familia("fiel", "Fiel a las carreras", "Pasa {n} corriendo", "min", new long[]{1800, 10800, 36000}, new int[]{15, 50, 150}, j -> j.segundos));

    static Familia familia(String id) {
        for (Familia f : FAMILIAS) if (f.id().equals(id)) return f;
        return null;
    }

    // ------------------------------------------------------------ estrellas por carrera

    /** Estrellas por terminar y por la posicion (solo en carreras con mas de un corredor): 1.º 25, 2.º 15, 3.º 10, el resto 5. */
    static int porPuesto(int puesto, int jugadores) {
        if (puesto <= 0) return 0;
        if (jugadores < 2) return 0;
        return switch (puesto) { case 1 -> 25; case 2 -> 15; case 3 -> 10; default -> 5; };
    }

    /**
     * Estrellas que da una carrera: terminarla da 5 (3 en un entrenamiento), mas las de la posicion y las michiestrellitas que recogiste (todas las que cogiste,
     * aunque luego perdieras la mitad por un golpe; la mitad, redondeando hacia abajo, en un entrenamiento). Si no la terminas no ganas nada.
     */
    static int estrellasDe(Carrera c) {
        if (!c.terminada()) return 0;
        int base = c.entrenamiento() ? 3 : 5;
        int rec = c.entrenamiento() ? c.recogidas() / 2 : c.recogidas();
        return base + porPuesto(c.puesto(), c.jugadores()) + rec;
    }

    /** Estrellas extra por batir tu mejor tiempo en una pista en la que ya tenias marca. */
    static final int PREMIO_RECORD = 5;

    // ------------------------------------------------------------ aplicar una carrera

    /** Apunta la carrera en las estadisticas del jugador y da lo ganado (estrellas y logros). {@code record} = ha mejorado su tiempo en esa pista; {@code primera} = era su primera marca en ella. */
    static Novedades aplicar(Datos.Jugador j, Carrera c, boolean record, boolean primera) {
        Novedades n = new Novedades();
        j.carreras++;
        j.segundos += Math.max(0, c.duracion() / 20);
        if (!j.pistas.contains(c.pista())) j.pistas.add(c.pista());
        if (!c.especie().isEmpty() && !j.especies.contains(c.especie())) j.especies.add(c.especie());
        if (c.entrenamiento()) { if (c.terminada()) j.entrenamientos++; }
        if (c.terminada()) {
            j.terminadas++;
            if (c.michi()) j.michiTerminadas++;
            if (!c.entrenamiento() && c.jugadores() >= 2) {
                if (c.puesto() == 1) { j.victorias++; if (c.golpesRecibidos() == 0) j.intocables++; }
                if (c.puesto() <= 3) j.podios++;
            }
        }
        if (c.terminada()) j.recogidas += c.recogidas();
        j.golpesDados += c.golpesDados();
        if (record && !primera) j.recordsMejorados++;
        n.porCarrera = estrellasDe(c);
        n.record = record && !primera;
        if (n.record) n.porRecord = PREMIO_RECORD;
        j.estrellas += n.porCarrera + n.porRecord;
        j.estrellasGanadas += n.porCarrera + n.porRecord;
        if (n.porCarrera > 0) n.lineas.add("+" + n.porCarrera + " estrellas por la carrera" + (c.recogidas() > 0 && c.terminada() ? " (" + (c.entrenamiento() ? c.recogidas() / 2 : c.recogidas()) + " de michiestrellitas)" : ""));
        if (n.porRecord > 0) n.lineas.add("+" + n.porRecord + " estrellas por batir tu mejor tiempo");
        comprobar(j, n);
        return n;
    }

    /** Da los escalones de logros que ya se cumplen (con su premio en estrellas) y anota las lineas. */
    static void comprobar(Datos.Jugador j, Novedades n) {
        for (Familia f : FAMILIAS) {
            int hechos = Math.min(j.logros.getOrDefault(f.id(), 0), f.escalones().length);
            long valor = f.valor().applyAsLong(j);
            while (hechos < f.escalones().length && valor >= f.escalones()[hechos]) {
                int premio = f.premios()[hechos];
                j.estrellas += premio; j.estrellasGanadas += premio; n.porLogros += premio;
                n.lineas.add("★ Logro «" + f.nombre() + "» " + (hechos + 1) + "/" + f.escalones().length + ": " + texto(f, f.escalones()[hechos]) + " (+" + premio + " estrellas)");
                hechos++;
            }
            j.logros.put(f.id(), hechos);
        }
    }

    // ------------------------------------------------------------ textos

    static String texto(Familia f, long meta) { return f.texto().replace("{n}", formato(meta, f.unidad())); }

    static String formato(long v, String unidad) {
        return switch (unidad) {
            case "min" -> v >= 120 && v % 60 == 0 ? (v / 60) + " min" : (v / 60) + " min " + (v % 60 == 0 ? "" : (v % 60) + " s");
            case "h" -> (v / 3600) + " h";
            default -> String.valueOf(v);
        };
    }

    /** Un tiempo en ticks como «m:ss.d» (1 tick = 50 ms), igual que el de la carrera. */
    static String tiempo(int ticks) {
        int ms = ticks * 50, m = ms / 60000, sec = (ms / 1000) % 60, d = (ms % 1000) / 100;
        return m + ":" + (sec < 10 ? "0" : "") + sec + "." + d;
    }
}
