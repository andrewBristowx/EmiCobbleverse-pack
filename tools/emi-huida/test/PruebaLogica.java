package emi.huida;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Prueba de la logica pura de Emi Huida (estrellas, logros, misiones, tablas, tienda) sin abrir Minecraft: tools/emi-huida/test/probar.sh. */
public class PruebaLogica {
    static int fallos = 0;
    static final ZoneId Z = ZoneId.of("UTC");
    static final long T0 = java.time.LocalDate.of(2026, 10, 19).atStartOfDay(Z).toInstant().toEpochMilli() + 3_600_000;       // lunes 19-10-2026, 01:00

    static void ok(boolean c, String que) { if (!c) { fallos++; System.out.println("  FALLO: " + que); } }

    static Retos.Partida p(String nivel, boolean escapo, int puntos, int errores, int puesto, int jugadores, int veredicto) {
        return new Retos.Partida(nivel, escapo, puntos, puntos / 2, errores, 100.0, puesto, jugadores, veredicto);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== estrellas por huida");
        ok(Retos.estrellasDe(p("tyrantrum", true, 200, 0, 1, 1, 2)) == 10 + 10 + 40, "escapar sin errores con 200 puntos, solo: 10 + 10 + 40 = " + Retos.estrellasDe(p("tyrantrum", true, 200, 0, 1, 1, 2)));
        ok(Retos.estrellasDe(p("tyrantrum", true, 100, 2, 1, 1, 0)) == 10 + 5 + 20, "plata (1-2 errores)");
        ok(Retos.estrellasDe(p("tyrantrum", true, 100, 3, 1, 1, 0)) == 10 + 0 + 20, "bronce no da extra");
        ok(Retos.estrellasDe(p("tyrantrum", false, 49, 3, 1, 1, 0)) == 3 + 0 + 9, "atrapado: 3 + puntos/5 (9), sin medalla");
        ok(Retos.estrellasDe(p("tyrantrum", true, 0, 0, 1, 3, 0)) == 10 + 10 + 20, "1.º con otros jugadores: +20");
        ok(Retos.estrellasDe(p("tyrantrum", true, 0, 0, 2, 3, 0)) == 10 + 10 + 12, "2.º: +12");
        ok(Retos.estrellasDe(p("tyrantrum", true, 0, 0, 3, 3, 0)) == 10 + 10 + 8, "3.º: +8");
        ok(Retos.estrellasDe(p("tyrantrum", true, 0, 0, 4, 4, 0)) == 10 + 10 + 4, "4.º: +4");

        System.out.println("== primera huida: contadores, logros y misiones");
        Datos.Jugador j = new Datos.Jugador();
        var n = Retos.aplicar(j, p("tyrantrum", true, 200, 0, 1, 1, 2), T0, Z);
        ok(j.huidas == 1 && j.escapes == 1 && j.perfectos == 1 && j.atrapado == 0 && j.victorias == 0 && j.podios == 0, "contadores (en solitario no hay victoria ni podio)");
        ok(j.michiestrellas == 100 && j.mejorPuntos == 200 && j.escapesNivel.get("tyrantrum") == 1 && j.mejoradas == 0, "michiestrellas, mejor puntuacion, escapes por nivel, la primera marca no es mejora");
        ok(j.logros.get("escapista") == 1 && j.logros.get("perfecto") == 1 && j.logros.get("cazaestrellas") == 1 && j.logros.get("puntuador") == 2 && j.logros.get("tyrantrum") == 1, "escalones de la primera huida (puntuador: 100 y 200)");
        ok(j.logros.get("calabaza") == 0 && j.logros.get("viajero") == 0 && j.logros.get("rey") == 0, "los demas siguen en 0");
        ok(j.estrellas == j.estrellasGanadas && j.estrellas == n.total() && n.total() == n.porHuida + n.porRecord + n.porMisiones + n.porLogros, "el saldo es lo ganado");
        ok(j.dia.size() == 3 && j.semana.size() == 3, "3 misiones del dia y 3 de la semana");
        Set<String> metricas = new HashSet<>();
        for (Retos.Prog pr : j.dia) metricas.add(Retos.def(pr.id).metrica());
        ok(metricas.size() == 3, "las 3 misiones del dia miden cosas distintas");

        System.out.println("== misiones: el mismo dia da las mismas para todos y cambian con la fecha");
        Datos.Jugador k = new Datos.Jugador();
        Retos.refrescar(k, T0 + 5000, Z);
        ok(k.dia.stream().map(x -> x.id).toList().equals(j.dia.stream().map(x -> x.id).toList()), "mismas misiones del dia");
        ok(k.semana.stream().map(x -> x.id).toList().equals(j.semana.stream().map(x -> x.id).toList()), "mismas misiones de la semana");
        String viejaSemana = k.semanaClave;
        Retos.refrescar(k, T0 + 86_400_000L, Z);                         // el martes: cambia el dia, no la semana
        ok(!k.diaClave.equals(j.diaClave) && k.semanaClave.equals(viejaSemana), "al dia siguiente cambia el dia pero no la semana");
        Retos.refrescar(k, T0 + 7 * 86_400_000L, Z);                     // el lunes siguiente: cambia la semana
        ok(!k.semanaClave.equals(viejaSemana), "el lunes siguiente cambia la semana");

        System.out.println("== misiones: avance y cobro (una vez)");
        Datos.Jugador m = new Datos.Jugador();
        Retos.refrescar(m, T0, Z);
        m.dia.clear(); m.semana.clear();
        Retos.Prog a = new Retos.Prog(); a.id = "d_estrellas_60";
        Retos.Prog b = new Retos.Prog(); b.id = "d_puntos_150";
        m.dia.add(a); m.dia.add(b);
        n = Retos.aplicar(m, new Retos.Partida("calabaza", false, 120, 40, 3, 50.0, 1, 1, 0), T0, Z);
        ok(m.dia.get(0).prog == 40 && !m.dia.get(0).hecha, "recoger michiestrellas suma (40 de 60)");
        ok(m.dia.get(1).prog == 120 && !m.dia.get(1).hecha, "los puntos valen la mejor huida (120 de 150)");
        int antesEstr = m.estrellas;
        n = Retos.aplicar(m, new Retos.Partida("calabaza", false, 90, 30, 3, 50.0, 1, 1, 0), T0, Z);
        ok(m.dia.get(0).hecha && m.dia.get(0).prog == 60, "60 michiestrellas cumplida (40 + 30 con tope)");
        ok(m.dia.get(1).prog == 120 && !m.dia.get(1).hecha, "una huida peor no baja el maximo");
        ok(n.porMisiones == Retos.def("d_estrellas_60").premio(), "se cobra el premio de la mision: " + n.porMisiones);
        int antes = m.estrellas;
        n = Retos.aplicar(m, new Retos.Partida("calabaza", false, 90, 30, 3, 50.0, 1, 1, 0), T0, Z);
        ok(n.porMisiones == 0 && m.dia.get(0).prog == 60, "una mision ya cumplida no se vuelve a cobrar");

        System.out.println("== puestos, tabla y mejoras");
        Datos.Jugador g = new Datos.Jugador();
        Retos.aplicar(g, p("cosmos", true, 100, 1, 1, 3, 2), T0, Z);       // 1.º de 3
        ok(g.victorias == 1 && g.podios == 1, "1.º con otros: victoria y podio");
        Retos.aplicar(g, p("cosmos", true, 100, 1, 3, 3, 0), T0, Z);       // 3.º
        ok(g.victorias == 1 && g.podios == 2, "3.º con otros: podio");
        Retos.aplicar(g, p("cosmos", true, 100, 1, 4, 4, 0), T0, Z);       // 4.º
        ok(g.victorias == 1 && g.podios == 2, "4.º: nada");
        n = Retos.aplicar(g, p("cosmos", true, 150, 0, 1, 1, 1), T0, Z);   // mejora su marca
        ok(g.mejoradas == 1 && n.porRecord == Retos.PREMIO_RECORD, "batir la mejor puntuacion: record");
        ok(g.escapesNivel.get("cosmos") == 4 && g.logros.get("michialiens") == 1, "escapes del nivel 3");
        n = Retos.aplicar(g, p("cosmos", false, 150, 3, 1, 1, 1), T0, Z);  // atrapado: aunque el veredicto diga 1, no cuenta record
        ok(g.mejoradas == 1 && n.porRecord == 0 && g.atrapado == 1, "si te atrapan no hay record ni escape");

        System.out.println("== viajero y rey: logros con varios escalones");
        Datos.Jugador v = new Datos.Jugador();
        Retos.aplicar(v, p("tyrantrum", true, 10, 3, 2, 2, 2), T0, Z);
        ok(v.logros.get("viajero") == 0, "un nivel solo no es viajero");
        Retos.aplicar(v, p("calabaza", true, 10, 3, 2, 2, 2), T0, Z);
        ok(v.logros.get("viajero") == 1, "dos niveles: viajero 1/2");
        Retos.aplicar(v, p("cosmos", true, 10, 3, 2, 2, 2), T0, Z);
        ok(v.logros.get("viajero") == 2, "tres niveles: viajero 2/2");
        int antesV = v.estrellas;
        Retos.aplicar(v, p("cosmos", true, 10, 3, 2, 2, 0), T0, Z);
        ok(v.logros.get("viajero") == 2, "no se repite el premio");

        System.out.println("== datos en disco: tabla de cada nivel");
        Path d = Files.createTempDirectory("emihuida");
        Datos datos = new Datos(d);
        UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
        ok(datos.registrar(u1, "Ana", "tyrantrum", 100, 1, 90.0, T0) == 2, "primera marca = 2");
        ok(datos.registrar(u2, "Beto", "tyrantrum", 100, 0, 95.0, T0) == 2, "primera marca de otro jugador");
        ok(datos.top("tyrantrum").get(0).nombre.equals("Beto"), "a igualdad de puntos gana quien tuvo menos errores");
        ok(datos.registrar(u1, "Ana", "tyrantrum", 150, 2, 80.0, T0) == 1, "mejorar la propia marca = 1");
        ok(datos.top("tyrantrum").get(0).nombre.equals("Ana") && datos.top("tyrantrum").size() == 2, "Ana primera, cada jugador una sola marca");
        ok(datos.registrar(u1, "Ana", "tyrantrum", 120, 0, 70.0, T0) == 0, "una marca peor no mejora (0)");
        ok(datos.top("tyrantrum").get(0).puntos == 150, "la tabla sigue con su mejor marca");
        Datos.Jugador dj = datos.jugador(u1, "Ana");
        dj.estrellas = 77; dj.unicos.add("peluche_dino");
        datos.guardar(u1);
        Datos otro = new Datos(d);
        ok(otro.jugador(u1, "Ana").estrellas == 77 && otro.jugador(u1, "Ana").unicos.contains("peluche_dino"), "se guarda y se lee del disco");
        ok(otro.top("tyrantrum").size() == 2 && otro.top("tyrantrum").get(0).nombre.equals("Ana"), "la tabla se guarda y se lee");

        System.out.println("== tienda");
        Datos.Jugador t = new Datos.Jugador();
        t.estrellas = 1500;
        List<String> entregas = new java.util.ArrayList<>();
        Tienda.Entrega ok1 = art -> { entregas.add(art.id()); return true; };
        Tienda.Entrega falla = art -> false;
        ok(Tienda.comprar(t, "peluche_dino", T0, Z, ok1) == null && t.estrellas == 500 && t.unicos.contains("peluche_dino") && entregas.equals(List.of("peluche_dino")), "comprar el peluche del dinosaurio cuesta 1000 y se entrega al correo");
        ok(Tienda.comprar(t, "peluche_dino", T0, Z, ok1) != null && t.estrellas == 500, "el peluche solo se compra una vez");
        ok(Tienda.comprar(t, "master", T0, Z, falla) != null && t.estrellas == 500, "si el correo no lo acepta no se cobra");
        ok(Tienda.comprar(t, "ultra", T0, Z, ok1) == null && t.estrellas == 460 && Tienda.compradosHoy(t, "ultra") == 1, "Ultra Ball: 40");
        for (int i = 0; i < 9; i++) Tienda.comprar(t, "ultra", T0, Z, ok1);
        ok(Tienda.compradosHoy(t, "ultra") == 10, "10 al dia");
        t.estrellas = 1000;
        ok(Tienda.comprar(t, "ultra", T0, Z, ok1) != null, "el 11.º del dia no se puede");
        ok(Tienda.comprar(t, "ultra", T0 + 86_400_000L, Z, ok1) == null, "al dia siguiente si");
        ok(Tienda.comprar(t, "no_existe", T0, Z, ok1) != null, "un articulo que no existe");
        t.estrellas = 10;
        ok(Tienda.comprar(t, "gacha", T0, Z, ok1) != null, "sin estrellas suficientes");
        ok(Tienda.articulo("peluche_dino").item().equals("emi_plush:gatitoalien_dino") && Tienda.articulo("peluche_dino").precio() == 1000, "el articulo exclusivo es el peluche del dinosaurio a 1000");

        System.out.println(fallos == 0 ? "TODO BIEN" : fallos + " FALLO(S)");
        System.exit(fallos == 0 ? 0 : 1);
    }
}
