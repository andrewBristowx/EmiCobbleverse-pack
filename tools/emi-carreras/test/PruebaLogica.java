package emi.carreras;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.UUID;

/** Prueba de la logica pura de Emi Carreras (estrellas, logros, tiempos, tienda) sin abrir Minecraft: tools/emi-carreras/test/probar.sh. */
public class PruebaLogica {
    static int fallos = 0;

    static void ok(boolean c, String que) { if (!c) { fallos++; System.out.println("  FALLO: " + que); } }

    public static void main(String[] args) throws Exception {
        System.out.println("== estrellas por carrera");
        var carrera1 = new Logros.Carrera("circuito_pokeball", "pikachu", 1, 4, false, 12, 2, 0, 1800, 2000, false);
        ok(Logros.estrellasDe(carrera1) == 5 + 25 + 12, "1.º con 12 michiestrellitas: 5 + 25 + 12 = " + Logros.estrellasDe(carrera1));
        ok(Logros.estrellasDe(new Logros.Carrera("p", "e", 2, 4, false, 0, 0, 0, 1, 1, false)) == 5 + 15, "2.º");
        ok(Logros.estrellasDe(new Logros.Carrera("p", "e", 3, 4, false, 0, 0, 0, 1, 1, false)) == 5 + 10, "3.º");
        ok(Logros.estrellasDe(new Logros.Carrera("p", "e", 4, 4, false, 0, 0, 0, 1, 1, false)) == 5 + 5, "4.º");
        ok(Logros.estrellasDe(new Logros.Carrera("p", "e", 0, 4, false, 30, 0, 0, 1, 1, false)) == 0, "no terminarla no da nada, ni las michiestrellitas");
        ok(Logros.estrellasDe(new Logros.Carrera("p", "e", 1, 1, false, 7, 0, 0, 1, 1, false)) == 5 + 7, "sola (1 corredor) no hay premio de posicion");
        ok(Logros.estrellasDe(new Logros.Carrera("p", "e", 1, 1, true, 7, 0, 0, 1, 1, false)) == 3 + 3, "entrenamiento: 3 + la mitad de las michiestrellitas (3)");

        System.out.println("== logros y contadores");
        Datos.Jugador j = new Datos.Jugador();
        var n = Logros.aplicar(j, carrera1, true, true);                   // primera marca: no cuenta como mejora
        ok(j.carreras == 1 && j.terminadas == 1 && j.victorias == 1 && j.podios == 1 && j.intocables == 1, "contadores de la primera victoria sin golpes");
        ok(j.recordsMejorados == 0 && n.porRecord == 0, "la primera marca en una pista no es un record que mejorar");
        ok(j.recogidas == 12 && j.golpesDados == 2 && j.pistas.contains("circuito_pokeball") && j.especies.contains("pikachu"), "recogidas, golpes, pista y especie");
        ok(j.logros.get("piloto") == 1 && j.logros.get("campeon") == 1 && j.logros.get("podio") == 1 && j.logros.get("intocable") == 1 && j.logros.get("estrellas") == 1 && j.logros.get("francotirador") == 1, "escalones de la primera carrera");
        int esperado = (5 + 25 + 12) + 10 + 20 + 15 + 25 + 10 + 10;          // carrera + piloto + campeon + podio + intocable + cazaestrellas(10) + francotirador(1)
        ok(j.estrellas == esperado, "estrellas tras la primera carrera: " + j.estrellas + " (esperado " + esperado + ")");
        ok(j.estrellasGanadas == j.estrellas, "las ganadas coinciden con el saldo mientras no se gaste");
        ok(!n.lineas.isEmpty() && n.total() == j.estrellas, "las novedades cuentan lo ganado");
        int antes = j.estrellas;
        n = Logros.aplicar(j, new Logros.Carrera("circuito_pokeball", "pikachu", 2, 4, false, 0, 0, 3, 1700, 2000, false), true, false);   // mejora su tiempo
        ok(j.recordsMejorados == 1 && n.porRecord == Logros.PREMIO_RECORD, "mejorar el tiempo da el premio de record");
        ok(j.logros.get("records") == 1, "logro Rompe-récords");
        ok(j.victorias == 1 && j.intocables == 1 && j.podios == 2, "quedar 2.º suma podio pero no victoria");
        ok(j.estrellas > antes, "las estrellas suben");
        // un escalon no se da dos veces
        int e = j.estrellas;
        Logros.comprobar(j, new Logros.Novedades());
        ok(j.estrellas == e, "comprobar de nuevo no repite premios");
        // entrenamiento y Michialien
        var j2 = new Datos.Jugador();
        Logros.aplicar(j2, new Logros.Carrera("volcan_cinnabar", "gatitoalien", 1, 1, true, 4, 0, 0, 2000, 2200, true), true, true);
        ok(j2.entrenamientos == 1 && j2.victorias == 0 && j2.podios == 0 && j2.michiTerminadas == 1, "un entrenamiento no cuenta como victoria ni podio");
        ok(j2.logros.get("entrenador") == 1 && j2.logros.get("fanemi") == 1 && !j2.logros.containsKey("campeon") || j2.logros.get("campeon") == 0, "logros Entrenador y Fan de Emi");
        // abandonar no da nada
        var j3 = new Datos.Jugador();
        var n3 = Logros.aplicar(j3, new Logros.Carrera("circuito_pokeball", "pikachu", 0, 4, false, 9, 1, 0, 0, 500, false), false, false);
        ok(j3.terminadas == 0 && j3.recogidas == 0 && n3.porCarrera == 0 && n3.porRecord == 0, "no terminar no da estrellas de la carrera ni cuenta las michiestrellitas");
        ok(j3.carreras == 1 && j3.golpesDados == 1, "pero la carrera y los golpes acertados si se apuntan");
        // todos los escalones suben y los premios son positivos
        for (Logros.Familia f : Logros.FAMILIAS) {
            boolean sube = f.escalones().length == f.premios().length && f.escalones().length > 0;
            for (int i = 1; i < f.escalones().length; i++) sube &= f.escalones()[i] > f.escalones()[i - 1] && f.premios()[i] >= f.premios()[i - 1];
            ok(sube, "familia " + f.id() + ": escalones y premios crecientes");
        }
        ok(Logros.texto(Logros.familia("fiel"), 1800).contains("30 min"), "texto del logro de tiempo: " + Logros.texto(Logros.familia("fiel"), 1800));
        ok(Logros.tiempo(1800).equals("1:30.0"), "formato de tiempo " + Logros.tiempo(1800));

        System.out.println("== datos en disco y tiempos");
        Path dir = Files.createTempDirectory("carreras_prueba");
        Datos d = new Datos(dir);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        Datos.Jugador ja = d.jugador(a, "Ana");
        ja.estrellas = 77; ja.logros.put("piloto", 2);
        d.guardar(a);
        Datos d2 = new Datos(dir);
        ok(d2.jugador(a, "Ana").estrellas == 77 && d2.jugador(a, "Ana").logros.get("piloto") == 2, "los datos del jugador sobreviven al reinicio");
        ok(d.registrarTiempo(a, "Ana", "circuito_pokeball", 1000, "pikachu", 1), "primera marca");
        ok(!d.registrarTiempo(a, "Ana", "circuito_pokeball", 1100, "pikachu", 2), "un tiempo peor no mejora");
        ok(d.registrarTiempo(a, "Ana", "circuito_pokeball", 900, "pikachu", 3), "un tiempo mejor si");
        d.registrarTiempo(b, "Beto", "circuito_pokeball", 950, "squirtle", 4);
        var top = d.top("circuito_pokeball");
        ok(top.size() == 2 && top.get(0).nombre.equals("Ana") && top.get(0).ticks == 900 && top.get(1).nombre.equals("Beto"), "la tabla va ordenada y un jugador solo aparece una vez");
        ok(new Datos(dir).top("circuito_pokeball").size() == 2, "los tiempos sobreviven al reinicio");
        for (int i = 0; i < 20; i++) d.registrarTiempo(UUID.randomUUID(), "R" + i, "volcan_cinnabar", 2000 + i, "x", i);
        ok(d.top("volcan_cinnabar").size() == Datos.MAX_TOP, "la tabla guarda solo " + Datos.MAX_TOP);

        System.out.println("== tienda");
        long ahora = System.currentTimeMillis();
        ZoneId z = ZoneId.systemDefault();
        Datos.Jugador t = new Datos.Jugador();
        t.estrellas = 100;
        ok(Tienda.comprar(t, "gacha", ahora, z, x -> true) == null && t.estrellas == 40, "comprar un ticket gasta 60 estrellas");
        ok(Tienda.comprar(t, "gacha", ahora, z, x -> true) != null && t.estrellas == 40, "sin estrellas suficientes no se compra ni se cobra");
        ok(Tienda.comprar(t, "ultra", ahora, z, x -> false) != null && t.estrellas == 40, "si el correo no lo acepta, no se cobra");
        t.estrellas = 100000;
        for (int i = 0; i < Tienda.LIMITE_DIA; i++) ok(Tienda.comprar(t, "ultra", ahora, z, x -> true) == null, "compra " + (i + 1) + " de la ultra ball");
        ok(Tienda.comprar(t, "ultra", ahora, z, x -> true) != null, "tope de " + Tienda.LIMITE_DIA + " al dia");
        ok(Tienda.comprar(t, "ultra", ahora + 24L * 3600 * 1000, z, x -> true) == null, "al dia siguiente se puede otra vez");
        ok(Tienda.comprar(t, "peluche_carrito", ahora, z, x -> true) == null && Tienda.yaLoTiene(t, "peluche_carrito"), "el peluche exclusivo se compra una vez");
        ok(Tienda.comprar(t, "peluche_carrito", ahora, z, x -> true) != null, "y no una segunda");
        ok(Tienda.comprar(t, "nada", ahora, z, x -> true) != null, "un articulo que no existe");
        ok(Tienda.ARTICULOS.stream().allMatch(x -> x.precio() > 0), "todos los precios son positivos");

        System.out.println(fallos == 0 ? "\nTODO BIEN" : "\nFALLOS: " + fallos);
        System.exit(fallos == 0 ? 0 : 1);
    }
}
