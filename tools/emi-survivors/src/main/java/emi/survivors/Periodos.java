package emi.survivors;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Los periodos del top (hoy, esta semana, este mes y siempre). «Hoy» empieza a las 00:00 de la hora del servidor, la semana el lunes a las 00:00 y el mes el dia 1 a las 00:00.
 * Logica pura (sin Minecraft).
 */
final class Periodos {
    private Periodos() {}

    static final List<String> TODOS = List.of("hoy", "semana", "mes", "siempre");

    static boolean existe(String p) { return p != null && TODOS.contains(p.toLowerCase()); }

    /** El instante (milisegundos desde 1970) en que empieza el periodo que contiene a {@code ahoraMs}; 0 para «siempre». */
    static long desde(String periodo, long ahoraMs, ZoneId zona) {
        LocalDate hoy = Instant.ofEpochMilli(ahoraMs).atZone(zona).toLocalDate();
        LocalDate d = switch (periodo.toLowerCase()) {
            case "hoy" -> hoy;
            case "semana" -> hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "mes" -> hoy.withDayOfMonth(1);
            default -> null;
        };
        return d == null ? 0L : d.atStartOfDay(zona).toInstant().toEpochMilli();
    }
}
