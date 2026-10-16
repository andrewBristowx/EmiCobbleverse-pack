package emi.survivors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Prueba de la logica pura de Emi Survivors (sin Minecraft): curvas, cartas, mejoras permanentes, ataque inicial por tipo y guardado de datos / top.
 * Se compila y se ejecuta con tools/emi-survivors/test/probar.sh (necesita un gson*.jar).
 */
public final class PruebaLogica {
    static int fallos;

    static void ok(boolean cond, String msg) {
        if (!cond) { fallos++; System.out.println("  FALLA: " + msg); }
    }

    public static void main(String[] args) throws Exception {
        curvas(); catalogo(); iconos(); cartas(); objetos(); jefes(); mareas(); meta(); datos(); grupos(); biomas(); periodos(); retos(); tienda();
        System.out.println(fallos == 0 ? "\nTODO BIEN" : "\nFALLOS: " + fallos);
        System.exit(fallos == 0 ? 0 : 1);
    }

    static void curvas() {
        System.out.println("== curvas");
        StringBuilder sb = new StringBuilder("XP para pasar de nivel: ");
        int acum = 0;
        for (int n = 1; n <= 20; n++) { sb.append(n).append("->").append(Curvas.xpPara(n)).append(' '); acum += Curvas.xpPara(n); }
        System.out.println(sb + "\n  XP total hasta el nivel 21: " + acum);
        for (int n = 1; n < 40; n++) ok(Curvas.xpPara(n + 1) > Curvas.xpPara(n), "la XP necesaria sube con el nivel " + n);
        ok(Curvas.xpPara(1) >= 8 && Curvas.xpPara(1) <= 12, "el primer nivel cuesta 8-12 de XP");
        for (int t : new int[]{0, 60, 120, 180, 300, 600, 900})
            System.out.printf("  t=%3ds  tope=%3d  ritmo=%.1f/s  vida x%.2f  dano x%.2f  vel x%.2f  fuertes %.0f%%%n", t, Curvas.tope(t), Curvas.ritmo(t), Curvas.vidaMul(t), Curvas.danoMul(t), Curvas.velMul(t), Curvas.pFuerte(t) * 100);
        ok(Curvas.tope(0) >= 15 && Curvas.tope(100000) == 120, "el tope va de ~22 a 120");
        ok(Curvas.monedas(0, 0, 1, 0, 1.0) >= 0, "monedas no negativas");
        int m5 = Curvas.monedas(300, 250, 15, 0, 1.0);
        System.out.println("  monedas de una partida de 5 min, 250 derrotados, nivel 15: " + m5 + " (con un jefe: " + Curvas.monedas(300, 250, 15, 1, 1.0) + ", con cinco: " + Curvas.monedas(300, 250, 15, 5, 1.0) + ")");
        ok(Curvas.monedas(300, 250, 15, 1, 1.0) - m5 == Jefes.MONEDAS_POR_JEFE && Curvas.monedas(300, 250, 15, 5, 1.0) - m5 == 5 * Jefes.MONEDAS_POR_JEFE, "cada jefe derrotado suma sus monedas");
        ok(m5 > 40 && m5 < 120, "una partida normal da 40-120 monedas");
            // varios jugadores en la misma arena
        ok(Curvas.escalaCantidad(1) == 1.0 && Curvas.escalaCantidad(2) > 1.5 && Curvas.escalaCantidad(4) < 3.0, "la cantidad de enemigos crece con los jugadores sin disparatarse");
        ok(Curvas.tope(100, 1) == Curvas.tope(100) && Curvas.tope(100, 4) <= Curvas.TOPE_EQUIPO && Curvas.tope(100, 2) > Curvas.tope(100, 1), "el tope de enemigos sube con los jugadores y no pasa del tope del equipo");
        ok(Curvas.ritmo(30, 3) > Curvas.ritmo(30, 1) && Curvas.escalaVida(4) < 1.6 && Curvas.escalaJefe(2) > 1.5 && Curvas.escalaJefe(9) == Curvas.escalaJefe(4), "ritmo, vida y jefe escalan; mas de 4 jugadores no cuentan");
    }

    static void catalogo() {
        System.out.println("== catalogo");
        String[] tipos = {"normal", "fire", "water", "grass", "electric", "ice", "fighting", "poison", "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy"};
        for (String t : tipos) {
            Catalogo.Inicial i = Catalogo.inicial(t);
            ok(i != null && Catalogo.arma(i.arma()) != null, "el tipo " + t + " tiene un ataque inicial valido");
        }
        ok(Catalogo.inicial("desconocido").arma().equals("disparo"), "un tipo raro empieza con el disparo");
        Set<String> ids = new HashSet<>();
        for (Catalogo.Arma a : Catalogo.ARMAS) {
            ok(ids.add(a.id()), "id de arma repetido: " + a.id());
            ok(a.textos().length == Catalogo.MAX_NIVEL, "el arma " + a.id() + " tiene un texto por nivel");
        }
        for (Catalogo.Pasiva p : Catalogo.PASIVAS) ok(ids.add(p.id()), "id repetido: " + p.id());
        System.out.println("  " + Catalogo.ARMAS.size() + " ataques, " + Catalogo.PASIVAS.size() + " mejoras pasivas, " + Meta.MEJORAS.size() + " mejoras permanentes");
        ok(Catalogo.ARMAS.size() == 30 && Catalogo.PASIVAS.size() == 16, "30 ataques y 16 objetos");
        // nombres e iconos: sin repetir (cada ataque y cada evolucion se reconoce por el suyo)
        Set<String> nombres = new HashSet<>(), iconos = new HashSet<>();
        Map<String, Integer> evolucionanCon = new HashMap<>();
        for (Catalogo.Arma a : Catalogo.ARMAS) {
            ok(nombres.add(a.nombre()), "nombre de ataque repetido: " + a.nombre());
            ok(iconos.add(a.icono()), "icono repetido: " + a.icono() + " (" + a.id() + ")");
            ok(a.evo() != null, "todos los ataques evolucionan: " + a.id());
            if (a.evo() == null) continue;
            ok(nombres.add(a.evo().nombre()), "nombre de evolucion repetido: " + a.evo().nombre());
            ok(Catalogo.pasiva(a.evo().requiere()) != null, "la evolucion de " + a.id() + " pide un objeto que no existe: " + a.evo().requiere());
            ok(a.evo().texto().length() > 10 && a.evo().texto().length() < 110, "texto de evolucion con una longitud razonable: " + a.id());
            for (String t : a.textos()) ok(t.length() > 3 && t.length() < 110, "texto de nivel con una longitud razonable: " + a.id() + " -> " + t);
            evolucionanCon.merge(a.evo().requiere(), 1, Integer::sum);
        }
        for (Catalogo.Pasiva p : Catalogo.PASIVAS) {
            ok(iconos.add(p.icono()), "icono de objeto repetido: " + p.icono() + " (" + p.id() + ")");
            ok(nombres.add(p.nombre()), "nombre de objeto repetido: " + p.nombre());
            ok(evolucionanCon.getOrDefault(p.id(), 0) >= 1 || p.id().equals("xp") && false, "el objeto " + p.nombre() + " sirve para evolucionar algun ataque");
        }
        System.out.println("  evoluciones por objeto: " + evolucionanCon);
        ok(Catalogo.focusSegundos(1) == 160 && Catalogo.focusSegundos(5) == 80 && Catalogo.bayaSegundos(1) == 60 && Catalogo.bayaSegundos(5) == 20, "recargas de la Banda Focus y la Baya Zidra");
        ok(Catalogo.nombre(Catalogo.arma("disparo"), 5).equals("Disparo Rápido") && Catalogo.nombre(Catalogo.arma("disparo"), 6).equals("Ráfaga Infinita"), "el nombre cambia al evolucionar");
        Map<String, Integer> con = new HashMap<>(Map.of("recarga", 1));
        ok(!Catalogo.puedeEvolucionar(Catalogo.arma("disparo"), 4, con) && Catalogo.puedeEvolucionar(Catalogo.arma("disparo"), 5, con) && !Catalogo.puedeEvolucionar(Catalogo.arma("disparo"), 6, con)
                && !Catalogo.puedeEvolucionar(Catalogo.arma("orbes"), 5, con), "solo evoluciona un ataque al nivel 5 con su objeto (y una sola vez)");
        Stats s = new Stats();
        Map<String, Integer> p = new HashMap<>();
        for (Catalogo.Pasiva x : Catalogo.PASIVAS) p.put(x.id(), Catalogo.MAX_NIVEL);
        Catalogo.aplicarPasivas(s, p);
        System.out.printf("  con todas las pasivas al maximo: vida %.0f regen %.1f vel x%.2f dano x%.2f recarga x%.2f area x%.2f iman %.1f xp x%.2f armadura %.0f%%%n",
                s.hpMax, s.regen, s.vel, s.dano, s.recarga, s.area, s.iman, s.xp, s.armadura * 100);
        ok(s.recarga > 0.3 && s.armadura <= 0.6, "los limites de recarga y armadura");
        ok(s.critico <= 0.5 && s.critico > 0.3, "el critico llega al tope de 50%: " + s.critico);
        ok(s.focus == Catalogo.MAX_NIVEL && s.baya == Catalogo.MAX_NIVEL && s.casco == 40 && Math.abs(s.experto - 0.75) < 1e-9 && Math.abs(s.concha - 2.0) < 1e-9, "efectos de los objetos nuevos al maximo");
        Stats s3 = new Stats();
        Catalogo.aplicarPasivas(s3, Map.of("moneda", 5, "critico", 1));
        ok(Math.abs(s3.monedas - 1.5) < 1e-9 && Math.abs(s3.critico - 0.08) < 1e-9, "Moneda Amuleto +50% y Periscopio +8% al nivel 1");
    }

    /** Todos los ataques, evoluciones, objetos, mejoras y tipos tienen su icono en el atlas (lo que genera assets/generar_iconos.py). */
    static void iconos() throws Exception {
        System.out.println("== iconos");
        Path json = Path.of("../src/main/resources/assets/emi_survivors/iconos.json");
        com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(Files.readString(json)).getAsJsonObject();
        com.google.gson.JsonObject ic = o.getAsJsonObject("iconos");
        int celda = o.get("celda").getAsInt(), cols = o.get("columnas").getAsInt(), filas = o.get("filas").getAsInt();
        int n = 0;
        for (Catalogo.Arma a : Catalogo.ARMAS) {
            ok(ic.has(a.icono()), "falta el icono " + a.icono()); ok(ic.has(a.evo().icono()), "falta el icono " + a.evo().icono()); n += 2;
        }
        for (Catalogo.Pasiva p : Catalogo.PASIVAS) { ok(ic.has(p.icono()), "falta el icono " + p.icono()); n++; }
        for (Meta.Mejora m : Meta.MEJORAS) { ok(ic.has(m.icono()), "falta el icono " + m.icono()); n++; }
        for (String t : List.of("normal", "fire", "water", "grass", "electric", "ice", "fighting", "poison", "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy", "stellar")) {
            ok(ic.has(Catalogo.iconoInicial(t)), "falta el icono del ataque inicial del tipo " + t); n++;
        }
        ok(ic.has("x_curar") && ic.has("x_xp"), "faltan los iconos de las tarjetas curar y experiencia");
        for (Biomas.Bioma b : Biomas.TODOS) { ok(ic.has("b_" + b.id()), "falta el icono de la arena b_" + b.id()); n++; }
        for (var e : ic.entrySet()) {
            int c = e.getValue().getAsJsonArray().get(0).getAsInt(), f = e.getValue().getAsJsonArray().get(1).getAsInt();
            ok(c >= 0 && c < cols && f >= 0 && f < filas, "el icono " + e.getKey() + " esta fuera del atlas");
        }
        ok(o.get("ancho").getAsInt() == cols * celda && o.get("alto").getAsInt() == filas * celda, "el tamano del atlas no cuadra con sus columnas y filas");
        System.out.println("  " + n + " iconos comprobados (" + ic.size() + " en el atlas)");
    }

    static void cartas() {
        System.out.println("== cartas");
        Random rnd = new Random(7);
        Map<String, Integer> armas = new HashMap<>(), pasivas = new HashMap<>();
        armas.put("disparo", 1);
        // 2000 sorteos con el estado inicial: siempre 3 distintas
        int nuevas = 0, subidas = 0;
        for (int i = 0; i < 2000; i++) {
            List<Cartas.Carta> l = Cartas.sortear(rnd, armas, pasivas);
            ok(l.size() == 3, "siempre 3 cartas");
            ok(new HashSet<>(l).size() == 3, "las 3 son distintas: " + l);
            for (Cartas.Carta c : l) { if (c.nueva()) nuevas++; else subidas++; }
        }
        System.out.println("  estado inicial (solo el disparo): cartas nuevas " + nuevas + ", subidas " + subidas);
        // con 6 armas y 6 pasivas no salen cosas nuevas
        armas.clear(); pasivas.clear();
        for (int i = 0; i < 6; i++) armas.put(Catalogo.ARMAS.get(i).id(), 2);
        for (int i = 0; i < 6; i++) pasivas.put(Catalogo.PASIVAS.get(i).id(), 3);
        for (int i = 0; i < 500; i++) for (Cartas.Carta c : Cartas.sortear(rnd, armas, pasivas)) ok(!c.nueva() || c.tipo().equals("curar") || c.tipo().equals("xp"), "con los huecos llenos no salen cartas nuevas: " + c);
        // un solo candidato posible: todos los ataques evolucionados menos el tajo (nivel 4), todos los objetos al maximo
        armas.clear(); pasivas.clear();
        for (Catalogo.Arma a : Catalogo.ARMAS) armas.put(a.id(), a.id().equals("tajo") ? 4 : Catalogo.NIVEL_EVO);
        for (Catalogo.Pasiva p : Catalogo.PASIVAS) pasivas.put(p.id(), Catalogo.MAX_NIVEL);
        List<Cartas.Carta> l = Cartas.sortear(rnd, armas, pasivas);
        ok(l.get(0).equals(new Cartas.Carta("arma", "tajo", 5)) && l.size() == 3 && l.contains(Cartas.CURAR) && l.contains(Cartas.XP), "la unica subida posible sale primero y se rellena con curar y xp: " + l);
        // todo al maximo y evolucionado: solo curar y xp
        armas.put("tajo", Catalogo.NIVEL_EVO);
        l = Cartas.sortear(rnd, armas, pasivas);
        ok(l.size() == 3 && l.contains(Cartas.CURAR) && l.contains(Cartas.XP) && l.stream().filter(c -> c.tipo().equals("arma") || c.tipo().equals("pasiva")).count() == 0 || l.size() == 2, "todo evolucionado: no queda nada que subir: " + l);
        // evoluciones: el ataque al nivel 5 con su objeto ofrece la carta dorada, siempre
        armas.clear(); pasivas.clear();
        armas.put("disparo", Catalogo.MAX_NIVEL); armas.put("orbes", 3);
        pasivas.put("area", 1);
        for (int i = 0; i < 300; i++) for (Cartas.Carta c : Cartas.sortear(rnd, armas, pasivas)) ok(!c.evo(), "sin el objeto de Disparo Rápido no hay evolucion: " + c);
        pasivas.put("recarga", 1);
        for (int i = 0; i < 300; i++) {
            l = Cartas.sortear(rnd, armas, pasivas);
            ok(l.size() == 3 && l.get(0).equals(new Cartas.Carta("arma", "disparo", Catalogo.NIVEL_EVO)) && l.get(0).evo() && !l.get(0).nueva(), "con el objeto, la evolucion sale la primera: " + l);
            ok(new HashSet<>(l).size() == 3, "las 3 distintas: " + l);
        }
        // dos evoluciones a la vez (como mucho 2 cartas doradas y una normal)
        armas.put("orbes", Catalogo.MAX_NIVEL);
        for (int i = 0; i < 300; i++) {
            l = Cartas.sortear(rnd, armas, pasivas);
            ok(l.get(0).evo() && l.get(1).evo() && !l.get(2).evo(), "dos evoluciones: salen las dos primero: " + l);
        }
        armas.put("disparo", Catalogo.NIVEL_EVO);
        for (int i = 0; i < 300; i++) for (Cartas.Carta c : Cartas.sortear(rnd, armas, pasivas)) ok(!(c.tipo().equals("arma") && c.id().equals("disparo")), "un ataque ya evolucionado no vuelve a salir: " + c);
        // reparto por categorias (estado inicial: solo el disparo; sin objetos)
        armas.clear(); pasivas.clear(); armas.put("disparo", 1);
        int[] cuenta = new int[4];
        for (int i = 0; i < 20000; i++) for (Cartas.Carta c : Cartas.sortear(rnd, armas, pasivas)) cuenta[c.tipo().equals("arma") ? (c.nueva() ? 1 : 0) : (c.nueva() ? 3 : 2)]++;
        System.out.printf("  reparto de cartas al empezar (subir ataque / ataque nuevo / subir objeto / objeto nuevo): %d / %d / %d / %d%n", cuenta[0], cuenta[1], cuenta[2], cuenta[3]);
        ok(cuenta[0] > 8000 && cuenta[1] > 5000 && cuenta[3] > 3000 && cuenta[2] == 0, "al empezar sale a menudo subir el ataque que ya tienes");
        System.out.println("  sorteos de prueba: bien");
    }

    static void objetos() {
        System.out.println("== objetos del suelo");
        Random rnd = new Random(11);
        int[] normal = new int[4], campeon = new int[4];
        for (int i = 0; i < 20000; i++) { normal[Objeto.sortear(rnd, false) - Objeto.BAYA]++; campeon[Objeto.sortear(rnd, true) - Objeto.BAYA]++; }
        System.out.printf("  reparto (baya / iman / bomba / caramelo): normales %d/%d/%d/%d, campeones %d/%d/%d/%d%n", normal[0], normal[1], normal[2], normal[3], campeon[0], campeon[1], campeon[2], campeon[3]);
        ok(normal[0] > normal[1] && normal[1] > normal[2] && normal[2] > normal[3] && normal[3] > 1000, "de los normales salen mas bayas que imanes, bombas y caramelos (en ese orden)");
        ok(campeon[3] > normal[3] && campeon[2] > normal[2], "los campeones sueltan mas bombas y caramelos");
        for (int t : Objeto.TODOS) ok(Objeto.esObjeto(t) && !Objeto.nombre(t).equals("?") && !Objeto.texto(t).isEmpty(), "objeto " + t + " con nombre y texto");
        ok(!Objeto.esObjeto(0) && !Objeto.esObjeto(2) && !Objeto.esObjeto(14), "las gemas (0-2) no son objetos");
        ok(Objeto.porNombre("baya") == Objeto.BAYA && Objeto.porNombre("caramelo") == Objeto.CARAMELO && Objeto.porNombre("nada") == -1, "nombres de los comandos de prueba");
        ok(Curvas.P_OBJETO > 0 && Curvas.P_OBJETO < 0.02, "pocos objetos de los enemigos normales");
    }

    static void jefes() {
        System.out.println("== jefes");
        Set<String> ids = new HashSet<>(), nombres = new HashSet<>();
        double vidaAnt = 0;
        for (int n = 1; n <= Jefes.TODOS.length; n++) {
            Jefes.Def d = Jefes.def(n);
            ok(ids.add(d.id()) && nombres.add(d.nombre()), "jefe repetido: " + d.id());
            ok(d.ataques().length >= 2, "el jefe " + d.nombre() + " tiene varios ataques");
            ok(d.espera() >= 40 && d.espera() <= 120, "tiempo entre ataques razonable: " + d.nombre());
            ok(d.vel() > 1.5 && d.vel() < 4.5 && d.alto() >= 4 && d.alto() <= 7, "velocidad y altura razonables: " + d.nombre());
            ok(d.vida() > vidaAnt, "cada jefe tiene mas vida que el anterior: " + d.nombre());
            ok(Formatting_ok(d.color()), "color con nombre valido: " + d.color());
            ok(d.aviso().length() > 10 && d.aviso().length() < 80, "frase de aparicion con una longitud razonable: " + d.nombre());
            boolean invoca = false; for (Jefes.Ataque a : d.ataques()) if (a == Jefes.Ataque.INVOCAR) invoca = true;
            ok(!invoca || !d.secuaz().isEmpty(), "el jefe que invoca tiene refuerzos: " + d.nombre());
            vidaAnt = d.vida();
            System.out.printf("  jefe %d a los %d s: %s (%s), vida %.0f, ataques %s, furia %b%n", n, Jefes.segundo(n), d.nombre(), d.especie(), d.vida(), java.util.Arrays.toString(d.ataques()), d.furia());
        }
        ok(Jefes.segundo(1) == 180 && Jefes.segundo(2) == 360 && Jefes.segundo(5) == 900, "los jefes salen a los 3, 6, 9, 12 y 15 minutos");
        ok(Jefes.tocan(0) == 0 && Jefes.tocan(179.9) == 0 && Jefes.tocan(180) == 1 && Jefes.tocan(359.9) == 1 && Jefes.tocan(360) == 2 && Jefes.tocan(900) == 5 && Jefes.tocan(1080) == 6, "cuantos jefes tocan segun el tiempo");
        ok(Jefes.ciclo(1) == 0 && Jefes.ciclo(5) == 0 && Jefes.ciclo(6) == 1 && Jefes.def(6) == Jefes.def(1), "pasados los cinco, el ciclo vuelve a empezar");
        ok(Jefes.vida(6) > Jefes.vida(1) * 1.5 && Jefes.dano(6) > Jefes.dano(1), "la segunda vuelta es mas dura");
        ok(Jefes.esElUltimo(5) && Jefes.esElUltimo(10) && !Jefes.esElUltimo(4), "solo el quinto de cada vuelta es el ultimo");
        ok(Jefes.impactos(1, 1) >= 6 && Jefes.impactos(4, 1) > Jefes.impactos(1, 1) && Jefes.impactos(4, 10) <= 16, "la lluvia tiene entre 6 y 16 impactos");
        ok(Jefes.refuerzos(1, 1, false) >= 6 && Jefes.refuerzos(1, 1, true) > Jefes.refuerzos(1, 1, false) && Jefes.refuerzos(4, 10, true) <= 24, "los refuerzos van de 6 a 24");
        // lo que dura cada pelea con un dano por segundo de ejemplo (los jugadores reales haran mas o menos)
        double[] dps = {150, 400, 800, 1300, 2200};
        for (int n = 1; n <= 5; n++) System.out.printf("  jefe %d: con %.0f de dano por segundo dura %.0f s con 1 jugador%n", n, dps[n - 1], Jefes.vida(n) / dps[n - 1]);
    }

    static void mareas() {
        System.out.println("== mareas y personalidades");
        ok(Mareas.tocan(0) == 0 && Mareas.tocan(89.9) == 0 && Mareas.tocan(90) == 1 && Mareas.tocan(269.9) == 1 && Mareas.tocan(270) == 2 && Mareas.tocan(810) == 5, "cuantas mareas tocan segun el tiempo");
        ok(Mareas.segundo(1) == 90 && Mareas.segundo(2) == 270 && Mareas.segundo(5) == 810, "las mareas salen a mitad de cada tramo entre jefes");
        for (int k = 1; k <= 30; k++) for (int n = 1; n <= 30; n++) ok(Mareas.segundo(k) != Jefes.segundo(n), "una marea nunca sale justo cuando un jefe (" + k + ", " + n + ")");
        ok(Mareas.tipo(1) == Mareas.Tipo.MICHIS && Mareas.tipo(2) == Mareas.Tipo.CERCO && Mareas.tipo(3) == Mareas.Tipo.TIPO && Mareas.tipo(4) == Mareas.Tipo.ESTAMPIDA && Mareas.tipo(5) == Mareas.Tipo.MICHIS, "el orden de las mareas");
        for (Mareas.Tipo t : Mareas.Tipo.values()) ok(Mareas.porNombre(t.name().toLowerCase()) == t, "se pueden pedir por nombre: " + t);
        Set<String> ids = new HashSet<>();
        for (Mareas.TipoPk pk : Mareas.TIPOS) {
            ok(ids.add(pk.id()) && Mareas.tipoPk(pk.id()) == pk, "tipo repetido: " + pk.id());
            ok(Formatting_ok(pk.color()), "color valido en la marea de " + pk.nombre());
            for (Mareas.Tipo t : new Mareas.Tipo[]{Mareas.Tipo.TIPO}) ok(Mareas.titulo(t, pk).length() <= 20, "titulo corto (cabe en la pantalla): " + Mareas.titulo(t, pk));
        }
        for (Mareas.Tipo t : Mareas.Tipo.values()) {
            ok(Mareas.titulo(t, Mareas.TIPOS[0]).length() <= 20, "titulo corto: " + t);
            ok(Mareas.subtitulo(t).length() <= 38, "subtitulo corto: " + Mareas.subtitulo(t));
        }
        ok(Mareas.factorRitmo(Mareas.Tipo.MICHIS) > Mareas.factorRitmo(Mareas.Tipo.TIPO) && Mareas.factorRitmo(Mareas.Tipo.CERCO) == 1.0, "ritmo de las mareas");
        ok(Mareas.cerco(0, 1) >= 18 && Mareas.cerco(100000, 4) <= 54 && Mareas.cerco(300, 2) > Mareas.cerco(300, 1), "tamano del cerco");
        ok(Mareas.porCarril(0) == 8 && Mareas.porCarril(100000) == 18, "enemigos por carril");
        ok(Mareas.carriles(1) == 2 && Mareas.carriles(2) == 2 && Mareas.carriles(3) == 3 && Mareas.carriles(4) == 3, "carriles de la estampida");
        // personalidades
        ok(Pers.probabilidad(0) == 0 && Pers.probabilidad(60) == 0 && Pers.probabilidad(130) > 0.04 && Pers.probabilidad(100000) == 0.25, "probabilidad de personalidad: 0 el primer minuto y tope del 25 %");
        for (int t = 0; t < 1000; t += 10) ok(Pers.probabilidad(t + 10) >= Pers.probabilidad(t), "la probabilidad no baja con el tiempo");
        Set<Pers> vistas = new HashSet<>(); Random r = new Random(7);
        for (int i = 0; i < 400; i++) vistas.add(Pers.sortear(r));
        ok(vistas.size() == Pers.values().length, "salen las cuatro personalidades");
        for (Pers p : Pers.values()) {
            ok(Pers.porNombre(p.name()) == p && Pers.porNombre(p.nombre.toLowerCase()) == p, "se pueden pedir por nombre: " + p);
            ok(Formatting_ok(p.color), "color de contorno valido: " + p);
            ok(p.vida >= 0.3 && p.vida <= 3 && p.dano >= 0.5 && p.dano <= 2 && p.vel >= 0.5 && p.vel <= 2 && p.tam >= 1 && p.tam <= 1.5, "numeros razonables de " + p);
        }
        ok(Pers.LEYENDA.length() < 130, "la leyenda de colores cabe en dos lineas del chat");
        ok(Pers.danoBomba(0) >= 10 && Pers.danoBomba(100000) == 60 && Pers.danoBomba(600) > Pers.danoBomba(60), "dano de la bomba del explosivo");
        System.out.printf("  marea 1 a los %d s, 2 a los %d, 3 a los %d, 4 a los %d; personalidad: %.0f %% a los 3 min y %.0f %% a los 8 min%n", Mareas.segundo(1), Mareas.segundo(2), Mareas.segundo(3), Mareas.segundo(4), Pers.probabilidad(180) * 100, Pers.probabilidad(480) * 100);
    }

    private static boolean Formatting_ok(String c) {
        return java.util.Set.of("black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white").contains(c);
    }

    static void meta() {
        System.out.println("== mejoras permanentes");
        int total = 0;
        int totalTodas = 0;
        for (Meta.Mejora x : Meta.MEJORAS) for (int n = 0; n < Meta.MAX; n++) { totalTodas += Meta.costo(x, n); if (x.base() == 25) total += Meta.costo(x, n); }
        System.out.println("  coste de subir una mejora basica al maximo: " + total / 7 + " michiestrellitas; las " + Meta.MEJORAS.size() + " mejoras: " + totalTodas);
        ok(Meta.costo(Meta.mejora("vigor"), 0) == 25 && Meta.costo(Meta.mejora("vigor"), 4) == 125, "costes 25..125 de las basicas");
        ok(Meta.costo(Meta.mejora("armadura"), 0) == 40 && Meta.costo(Meta.mejora("armadura"), 4) == 200, "costes 40..200 de las de la fase 3A");
        ok(Meta.MEJORAS.size() == 12, "hay 12 mejoras");
        Stats s = new Stats();
        Map<String, Integer> m = new HashMap<>();
        for (Meta.Mejora x : Meta.MEJORAS) m.put(x.id(), Meta.MAX);
        Meta.aplicar(s, m);
        System.out.printf("  con todas al maximo: vida %.0f dano x%.2f vel x%.3f xp x%.2f iman %.2f regen %.2f monedas x%.2f%n", s.hpMax, s.dano, s.vel, s.xp, s.iman, s.regen, s.monedas);
        ok(s.hpMax == 160, "vigor 5 = +60 de vida");
        ok(Math.abs(s.armadura - 0.15) < 1e-9 && Math.abs(s.critico - 0.15) < 1e-9 && Math.abs(s.area - 1.2) < 1e-9 && Math.abs(s.recarga - 0.85) < 1e-9 && Math.abs(s.concha - 0.5) < 1e-9, "las 5 mejoras nuevas a nivel 5: armadura/critico 15%, alcance x1,2, rapidez x0,85, vampirismo 0,5");
        Stats s2 = new Stats();
        Meta.aplicar(s2, Map.of("vigor", 99, "mejora_rara", 3));
        ok(s2.hpMax == 160, "un nivel absurdo se recorta al maximo y una mejora desconocida se ignora");
    }

    static void datos() throws Exception {
        System.out.println("== datos y top");
        Path dir = Files.createTempDirectory("survivors_prueba");
        Datos d = new Datos(dir);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        ok(d.puesto(a) == 0 && d.top(10).isEmpty(), "top vacio al principio");
        ok(d.comprar(a, "Ana", "pk1", "charmander", "vigor") != null, "sin monedas no se compra");
        int p1 = reg(d, a, "Ana", "pk1", entrada("charmander", 200, 90), 60);
        int p2 = reg(d, b, "Bea", "pk2", entrada("squirtle", 300, 150), 80);
        int p3 = reg(d, c, "Cris", "pk3", entrada("bulbasaur", 100, 40), 30);
        ok(p1 == 1 && p2 == 1 && p3 == 3, "puestos al apuntarse: " + p1 + " " + p2 + " " + p3);
        ok(d.puesto(a) == 2 && d.puesto(b) == 1 && d.puesto(c) == 3, "orden del top por tiempo");
        // una marca peor no cambia nada; una mejor sustituye a la anterior (un jugador = una entrada)
        ok(reg(d, a, "Ana", "pk1", entrada("charmander", 150, 50), 20) == 0 && d.puesto(a) == 2, "marca peor: no cambia (devuelve 0) y sigue segunda");
        ok(d.top(10).size() == 3, "un jugador, una entrada");
        ok(reg(d, a, "Ana", "pk1", entrada("charmander", 400, 200), 100) == 1, "marca mejor: pasa a primera");
        ok(d.top(10).size() == 3 && d.top(1).get(0).nombre.equals("Ana"), "el top se reordena");
        // monedas y compras
        Datos.Jugador ja = d.jugador(a, "Ana");
        ok(ja.monedas == 60 + 20 + 100 && ja.partidas == 3 && ja.mejorTiempo == 400, "monedas y partidas acumuladas: " + ja.monedas + "/" + ja.partidas);
        ok(d.comprar(a, "Ana", "pk1", "charmander", "vigor") == null, "se compra el primer nivel (25)");
        ok(ja.monedas == 180 - 25, "se descuentan 25");
        ok(d.comprar(a, "Ana", "pk1", "charmander", "no_existe") != null, "mejora inexistente");
        for (int i = 0; i < 4; i++) ok(d.comprar(a, "Ana", "pk1", "charmander", "vigor") == null || ja.monedas < 400, "compras seguidas");
        // se guarda y se vuelve a leer
        Datos d2 = new Datos(dir);
        ok(d2.puesto(b) == 2 && d2.top(10).size() == 3, "el top sobrevive a un reinicio");
        ok(d2.jugador(a, null).pokemon.get("pk1").mejoras.getOrDefault("vigor", 0) >= 1, "las mejoras sobreviven a un reinicio");
        // top de grupos: aparte del de solitario, una entrada por grupo (mismo conjunto de jugadores), mejor marca sustituye
        ok(d2.topGrupos(10).isEmpty(), "sin marcas de grupo al principio");
        int g1 = d2.registrarEquipo(grupo("A + B", a, b, 500));
        int g2 = d2.registrarEquipo(grupo("A + C", a, c, 700));
        ok(g1 == 1 && g2 == 1, "puestos de grupos: " + g1 + " " + g2);
        ok(d2.registrarEquipo(grupo("A + B", a, b, 400)) == 0 && d2.topGrupos(10).size() == 2 && d2.topGrupos(10).get(1).segundos == 500, "marca peor del mismo grupo: no cambia");
        ok(d2.registrarEquipo(grupo("A + B", a, b, 900)) == 1 && d2.topGrupos(10).size() == 2, "marca mejor del mismo grupo: sustituye");
        ok(d2.topGrupos(1).get(0).nombre.equals("A + B") && d2.top(10).size() == 3, "los tops de grupo y de solitario no se mezclan");
        Datos d2b = new Datos(dir);
        ok(d2b.topGrupos(10).size() == 2 && d2b.top(10).size() == 3, "los dos tops sobreviven a un reinicio");
        ok(d2b.puestoEnGrupos(c) == 2 && d2b.puestoEnGrupos(a) == 1, "puesto de un jugador en el top de grupos");
        // reinicio del top con copia (los dos)
        String copia = d2.reiniciarTop();
        ok(copia != null && Files.exists(dir.resolve(copia)) && d2.top(10).isEmpty() && d2.topGrupos(10).isEmpty(), "reiniciar el top guarda una copia y vacia los dos tops");
        // archivo roto
        Files.writeString(dir.resolve("jugadores").resolve(b + ".json"), "{esto no es json");
        Datos d3 = new Datos(dir);
        ok(d3.jugador(b, "Bea").monedas == 0, "un archivo roto se aparta y se empieza de cero");
        ok(Files.exists(dir.resolve("jugadores").resolve(b + ".json.roto")), "queda el archivo roto apartado");
        System.out.println("  guardado en " + dir + ": bien");
    }

    /** Apunta una partida en solitario como lo hace el servidor: monedas del jugador y marca del equipo (de uno). */
    static int reg(Datos d, UUID id, String nombre, String pokeId, Datos.Entrada e, int monedas) {
        d.registrarJugador(id, nombre, pokeId, e.especie, e.segundos, monedas);
        e.uuid = id.toString(); e.nombre = nombre; e.modo = Datos.SOLO;
        return d.registrarEquipo(e);
    }

    /** Una marca de un grupo de dos jugadores (la clave son sus uuid ordenados y unidos con «+», como en el servidor). */
    static Datos.Entrada grupo(String nombre, UUID x, UUID y, int seg) {
        Datos.Entrada e = entrada("charmander", seg, 100);
        java.util.List<String> ids = new java.util.ArrayList<>(List.of(x.toString(), y.toString()));
        java.util.Collections.sort(ids);
        e.uuid = String.join("+", ids); e.nombre = nombre; e.modo = Datos.GRUPO;
        for (UUID u : new UUID[]{x, y}) { Datos.MiembroTop m = new Datos.MiembroTop(); m.uuid = u.toString(); m.nombre = u.toString().substring(0, 4); e.miembros.add(m); }
        return e;
    }

    static void grupos() {
        System.out.println("== grupos");
        Grupos g = new Grupos();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), d = UUID.randomUUID(), e = UUID.randomUUID();
        ok(g.crear(a, "Ana") == null && g.de(a).lider.equals(a), "Ana crea un grupo y es la lider");
        ok(g.crear(a, "Ana") != null, "no se puede crear un segundo grupo");
        ok(g.unir(b, "Bea", UUID.randomUUID()) != null, "unirse a un grupo que no existe");
        ok(g.unir(b, "Bea", a) == null && g.unir(c, "Cris", a) == null && g.unir(d, "Dani", a) == null, "hasta 4 en el grupo");
        ok(g.unir(e, "Eva", a) != null, "el quinto no entra");
        ok(g.abiertos().isEmpty() && g.de(a).completo(), "un grupo lleno no sale entre los abiertos");
        ok(g.unir(a, "Ana", a) != null, "no se une a su propio grupo");
        // listos
        ok(!g.de(a).todosListos(), "al principio nadie esta listo");
        g.listo(a, 1, "charmander", 25); g.listo(b, 2, "gengar", 30); g.listo(c, 1, "pikachu", 20);
        ok(!g.de(a).todosListos() && g.de(a).faltan().equals("Dani"), "falta Dani por estar listo: " + g.de(a).faltan());
        g.listo(d, 3, "snorlax", 40);
        ok(g.de(a).todosListos(), "todos listos");
        ok(g.noListo(d) == null && !g.de(a).todosListos(), "se puede dejar de estar listo");
        g.listo(d, 3, "snorlax", 40);
        // expulsar: solo el lider, y no a uno mismo
        ok(g.expulsar(b, c) != null, "un miembro no puede expulsar");
        ok(g.expulsar(a, a) != null, "el lider no se expulsa a si mismo");
        ok(g.expulsar(a, d) == null && g.de(d) == null && g.de(a).miembros.size() == 3, "el lider expulsa a Dani");
        ok(g.abiertos().size() == 1, "con sitio libre el grupo vuelve a estar abierto");
        // el lider se va: pasa al siguiente
        ok(g.salir(a) == null && g.de(a) == null && g.de(b).lider.equals(b) && g.de(b).miembros.size() == 2, "si se va la lider, pasa a Bea");
        // en partida: nadie entra ni se cambia nada
        g.alEmpezar(g.de(b));
        ok(g.unir(e, "Eva", b) != null && g.listo(c, 1, "x", 1) != null && g.abiertos().isEmpty(), "un grupo en partida esta cerrado");
        g.alTerminar(g.de(b));
        ok(!g.de(b).enPartida && !g.de(b).de(b).listo && !g.de(b).de(c).listo, "al acabar la partida siguen juntos pero nadie esta listo");
        // si no queda nadie, el grupo desaparece
        g.salir(b); g.salir(c);
        ok(g.todos().isEmpty() && g.salir(c) != null, "sin miembros el grupo desaparece y salir otra vez da error");
        System.out.println("  grupos: bien");
    }

    static void periodos() throws Exception {
        System.out.println("== periodos del top");
        java.time.ZoneId z = java.time.ZoneId.of("Europe/Madrid");
        long ahora = java.time.ZonedDateTime.of(2026, 10, 16, 15, 30, 0, 0, z).toInstant().toEpochMilli();       // viernes
        long hoy = Periodos.desde("hoy", ahora, z), semana = Periodos.desde("semana", ahora, z), mes = Periodos.desde("mes", ahora, z);
        ok(hoy == java.time.ZonedDateTime.of(2026, 10, 16, 0, 0, 0, 0, z).toInstant().toEpochMilli(), "hoy empieza a las 00:00");
        ok(semana == java.time.ZonedDateTime.of(2026, 10, 12, 0, 0, 0, 0, z).toInstant().toEpochMilli(), "la semana empieza el lunes 12");
        ok(mes == java.time.ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, z).toInstant().toEpochMilli(), "el mes empieza el 1");
        ok(Periodos.desde("siempre", ahora, z) == 0 && Periodos.existe("Hoy") && !Periodos.existe("ayer"), "siempre = 0; nombres validos");
        // historial: partidas con fechas distintas
        Path dir = Files.createTempDirectory("survivors_per");
        Datos d = new Datos(dir);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        long dia = 86_400_000L;
        d.anotarPartidaConFecha(entradaFecha(a, "Ana", 500, ahora - 20 * dia));      // el mes pasado... (20 dias antes: 26 sep)
        d.anotarPartidaConFecha(entradaFecha(a, "Ana", 300, ahora - 3 * dia));       // esta semana? 13 oct: si
        d.anotarPartidaConFecha(entradaFecha(b, "Bea", 200, ahora - 1000));          // hoy
        d.anotarPartidaConFecha(entradaFecha(b, "Bea", 100, ahora - 5 * dia));       // 11 oct: este mes, no esta semana
        List<Datos.Entrada> h = d.topDesde(Datos.SOLO, hoy, 10), s = d.topDesde(Datos.SOLO, semana, 10), m = d.topDesde(Datos.SOLO, mes, 10);
        ok(h.size() == 1 && h.get(0).nombre.equals("Bea"), "hoy: solo Bea");
        ok(s.size() == 2 && s.get(0).nombre.equals("Ana") && s.get(0).segundos == 300 && s.get(1).segundos == 200, "semana: Ana 300 (la de hace 3 dias) y Bea 200");
        ok(m.size() == 2 && m.get(0).segundos == 300 && m.get(1).segundos == 200, "mes: lo mismo (la de hace 20 dias es del mes pasado)");
        ok(d.topDesde(Datos.SOLO, 0, 10).get(0).segundos == 500, "siempre: Ana 500");
        Datos d2 = new Datos(dir);
        ok(d2.topDesde(Datos.SOLO, semana, 10).size() == 2, "el historial sobrevive a un reinicio");
        System.out.println("  periodos: bien");
    }

    static void retos() throws Exception {
        System.out.println("== misiones y logros");
        java.time.ZoneId z = java.time.ZoneId.of("Europe/Madrid");
        long viernes = java.time.ZonedDateTime.of(2026, 10, 16, 15, 30, 0, 0, z).toInstant().toEpochMilli();
        long dia = 86_400_000L;
        ok(Retos.claveDia(viernes, z).equals("2026-10-16") && Retos.claveSemana(viernes, z).equals("2026-10-12"), "claves: dia y lunes de la semana");
        ok(Retos.claveSemana(viernes + 2 * dia, z).equals("2026-10-12") && Retos.claveSemana(viernes + 3 * dia, z).equals("2026-10-19"), "la semana cambia el lunes");
        // la eleccion: 3, sin repetir medida, siempre la misma para una fecha y distinta entre fechas
        Set<List<String>> distintas = new HashSet<>();
        for (int k = 0; k < 60; k++) {
            String clave = Retos.claveDia(viernes + k * dia, z);
            List<Retos.Def> a = Retos.elegir(Retos.DIARIAS, clave), b = Retos.elegir(Retos.DIARIAS, clave), c = Retos.elegir(Retos.SEMANALES, clave);
            ok(a.size() == 3 && c.size() == 3, "tres misiones por periodo (" + clave + ")");
            ok(a.equals(b), "la misma fecha da las mismas misiones");
            Set<String> met = new HashSet<>();
            for (Retos.Def d : a) ok(met.add(d.metrica()), "no se repite la medida " + d.metrica() + " en " + clave);
            distintas.add(a.stream().map(Retos.Def::id).toList());
        }
        ok(distintas.size() > 10, "las misiones cambian de un dia a otro (" + distintas.size() + " combinaciones en 60 dias)");
        for (Retos.Def d : Retos.DIARIAS) ok(d.objetivo() > 0 && d.premio() > 0 && d.premio() <= 2 && Retos.def(d.id()) == d, "mision valida " + d.id());
        for (Retos.Def d : Retos.SEMANALES) ok(d.objetivo() > 0 && d.premio() >= 4 && d.premio() <= 8 && Retos.def(d.id()) == d, "mision valida " + d.id());
        for (Retos.Familia f : Retos.FAMILIAS) {
            ok(f.escalones().length == f.premios().length && f.escalones().length >= 2 && f.texto().contains("{n}"), "familia bien formada " + f.id());
            for (int i = 1; i < f.escalones().length; i++) ok(f.escalones()[i] > f.escalones()[i - 1] && f.premios()[i] > f.premios()[i - 1], f.id() + ": escalones y premios crecen");
        }
        int logrosTotal = 0;
        for (Retos.Familia f : Retos.FAMILIAS) for (int pr : f.premios()) logrosTotal += pr;
        ok(logrosTotal < 300, "todos los logros juntos dan pocas coronas (" + logrosTotal + ")");
        ok(Curvas.monedas(300, 100, 10, 0, 1.0) == Curvas.monedas(300, 100, 10, 0, 0, 1.0), "sin shinies, las monedas son las de siempre");
        ok(Curvas.monedas(300, 100, 10, 0, 4, 1.0) - Curvas.monedas(300, 100, 10, 0, 0, 1.0) == 4 * Curvas.MONEDAS_POR_SHINY, "cada shiny suma " + Curvas.MONEDAS_POR_SHINY + " michiestrellitas");
        ok(Curvas.P_SHINY > 0 && Curvas.P_SHINY <= 0.01, "los shinies son raros (" + Curvas.P_SHINY + ")");
        // un jugador: una partida da avance, y al cumplir la mision cobra una sola vez
        Path dir = Files.createTempDirectory("survivors_retos");
        Datos d = new Datos(dir);
        UUID id = UUID.randomUUID();
        Datos.Jugador j = d.jugador(id, "Ana");
        Retos.refrescar(j, viernes, z);
        ok(j.coronas == 0 && j.dia.size() == 3 && j.semana.size() == 3 && j.diaClave.equals("2026-10-16"), "al refrescar salen 3 + 3");
        List<String> idsDia = j.dia.stream().map(p -> p.id).toList();
        Retos.Partida grande = new Retos.Partida(700, 5000, 30, 5, "pradera", 2, 6, "pikachu");
        int antes = j.coronas, estrellas = j.monedas;
        j.partidas = 1; j.mejorTiempo = 700;                       // lo que apunta registrarJugador antes
        Retos.Novedades n = d.retos(id, "Ana", grande, viernes, z);
        for (Retos.Prog p : j.dia) ok(Retos.def(p.id).metrica().equals("partidas") ? p.prog == 1 && !p.hecha : p.hecha && p.prog == Retos.def(p.id).objetivo(), "una partida enorme cumple la mision " + p.id + " (menos las de jugar varias partidas, que llevan 1)");
        ok(n.coronas > 0 && j.coronas == antes + n.coronas && j.monedas == estrellas, "las recompensas se suman a las coronas y no tocan las michiestrellitas");
        ok(j.logros.getOrDefault("cazador", 0) == 3 && j.logros.getOrDefault("matajefes", 0) == 2 && j.logros.getOrDefault("aguante", 0) == 2, "logros: cazador 3 (100, 1000, 5000), matajefes 2 (1, 5), aguante 2 (5 y 10 min)");
        ok(j.logros.getOrDefault("rey", 0) == 2 && j.logros.getOrDefault("evolucionista", 0) == 1 && j.logros.getOrDefault("amigos", 0) == 1, "logros: rey 2 (3 y 5 jefes en una), evolucionista 1, amigos 1");
        ok(n.lineas.size() >= 6, "cuenta lo conseguido (" + n.lineas.size() + " lineas)");
        int tras = j.coronas;
        Retos.Novedades n2 = d.retos(id, "Ana", new Retos.Partida(10, 1, 1, 0, "pradera", 1, 0, "pikachu"), viernes, z);
        ok(j.coronas == tras + n2.coronas && n2.lineas.stream().noneMatch(s -> s.contains("Logro")), "una partida floja no vuelve a cobrar los logros ya cobrados");
        ok(j.biomas.size() == 1 && j.especies.size() == 1 && j.derrotadosTotal == 5001 && j.partidasGrupo == 1, "contadores: arenas, especies, derrotados, grupo");
        // al dia siguiente salen otras (sin progreso), la semana sigue
        List<Retos.Prog> semanaAntes = j.semana;
        Retos.refrescar(j, viernes + dia, z);
        ok(j.diaClave.equals("2026-10-17") && j.dia.stream().noneMatch(p -> p.hecha || p.prog > 0), "al dia siguiente las misiones del dia se renuevan sin avance");
        ok(j.semana == semanaAntes, "la semana no cambia el sabado");
        Retos.refrescar(j, viernes + 3 * dia, z);
        ok(j.semanaClave.equals("2026-10-19") && j.semana != semanaAntes && j.semana.stream().noneMatch(p -> p.hecha), "el lunes se renuevan las semanales");
        // MAX no suma entre partidas y SUMA si
        Datos.Jugador k = d.jugador(UUID.randomUUID(), "Bea");
        Retos.refrescar(k, viernes, z);
        k.dia.clear(); k.dia.add(prog("d_aguanta_6")); k.dia.add(prog("d_derrota_300")); k.dia.add(prog("d_partidas_2"));
        d.retos(UUID.randomUUID(), "x", new Retos.Partida(200, 100, 5, 0, "bosque", 1, 0, "eevee"), viernes, z);     // otro jugador: no afecta
        Retos.aplicar(k, new Retos.Partida(200, 100, 5, 0, "bosque", 1, 0, "eevee"), viernes, z);
        Retos.aplicar(k, new Retos.Partida(250, 100, 5, 0, "bosque", 1, 0, "eevee"), viernes, z);
        ok(k.dia.get(0).prog == 250 && !k.dia.get(0).hecha, "aguantar vale la mejor partida (250 y no 450)");
        ok(k.dia.get(1).prog == 200 && !k.dia.get(1).hecha && k.dia.get(2).prog == 2 && k.dia.get(2).hecha, "derrotados suma (200) y 2 partidas cumple");
        // shinies: 6 en una partida dan los dos primeros escalones (1 y 5) y se suman en el total
        Datos.Jugador sj = d.jugador(UUID.randomUUID(), "Cora");
        Retos.Novedades ns = Retos.aplicar(sj, new Retos.Partida(100, 10, 3, 0, "pradera", 1, 0, "eevee", 6), viernes, z);
        ok(sj.shiniesTotal == 6 && sj.logros.getOrDefault("shiny", 0) == 2 && sj.coronas >= 6, "logro Cazashinies: 6 shinies = 2 escalones");
        Retos.aplicar(sj, new Retos.Partida(100, 10, 3, 0, "pradera", 1, 0, "eevee", 20), viernes, z);
        ok(sj.shiniesTotal == 26 && sj.logros.getOrDefault("shiny", 0) == 3, "26 shinies en total: tercer escalon (25)");
        // se guarda y se lee de disco
        d.guardar(id);
        Datos d2 = new Datos(dir);
        Datos.Jugador r = d2.jugador(id, "Ana");
        ok(r.logros.equals(j.logros) && r.derrotadosTotal == 5001 && r.semanaClave.equals(j.semanaClave) && r.dia.size() == 3 && r.biomas.equals(j.biomas), "misiones y logros sobreviven a un reinicio");
        // un archivo de la version anterior (sin campos nuevos) se lee bien
        Path viejo = dir.resolve("jugadores").resolve("11111111-1111-1111-1111-111111111111.json");
        Files.writeString(viejo, "{\"nombre\":\"Vieja\",\"monedas\":50,\"partidas\":3,\"mejorTiempo\":400,\"pokemon\":{}}");
        Datos.Jugador v = d2.jugador(UUID.fromString("11111111-1111-1111-1111-111111111111"), "Vieja");
        ok(v.monedas == 50 && v.dia != null && v.logros != null && v.biomas != null && v.especies != null, "un jugador de la version anterior se lee sin errores");
        Retos.Novedades nv = Retos.aplicar(v, new Retos.Partida(30, 5, 2, 0, "pradera", 1, 0, "eevee"), viernes, z);
        ok(v.logros.getOrDefault("veterano", 0) == 1 && v.logros.getOrDefault("aguante", 0) == 1, "a un jugador antiguo se le reconocen sus partidas y su mejor tiempo (3 partidas, 400 s)");
        System.out.println("  misiones y logros: bien (" + nv.lineas.size() + " avisos al jugador antiguo)");
    }

    static void tienda() throws Exception {
        System.out.println("== tienda");
        java.time.ZoneId z = java.time.ZoneId.of("Europe/Madrid");
        long ahora = java.time.ZonedDateTime.of(2026, 10, 16, 15, 30, 0, 0, z).toInstant().toEpochMilli(), dia = 86_400_000L;
        Set<String> ids = new HashSet<>();
        for (Tienda.Articulo a : Tienda.ARTICULOS) {
            ok(ids.add(a.id()) && a.precio() > 0 && a.item().contains(":") && !a.nombre().isBlank() && (a.grupo().equals("banner") || a.grupo().equals("objeto")), "articulo valido " + a.id());
            ok(Tienda.articulo(a.id()) == a, "se encuentra " + a.id());
        }
        ok(Tienda.ARTICULOS.size() <= 12, "caben en una pagina (12)");
        ok(Tienda.LIMITE_DIA == 10, "el limite del dueño: 10 por articulo y dia");
        Path dir = Files.createTempDirectory("survivors_tienda");
        Datos d = new Datos(dir);
        UUID id = UUID.randomUUID();
        Datos.Jugador j = d.jugador(id, "Ana");
        List<String> entregados = new java.util.ArrayList<>();
        Tienda.Entrega bien = a -> { entregados.add(a.item()); return true; };
        ok("Ese artículo no existe.".equals(Tienda.comprar(j, "nada", ahora, z, bien)), "articulo que no existe");
        String err = Tienda.comprar(j, "gacha", ahora, z, bien);
        ok(err != null && err.contains("Te faltan 3") && entregados.isEmpty() && j.coronas == 0, "sin coronas no se compra: " + err);
        j.coronas = 100;
        ok(Tienda.comprar(j, "gacha", ahora, z, bien) == null && j.coronas == 97 && entregados.equals(List.of("emipokemon:gacha_ticket")), "compra: cobra y entrega");
        ok(Tienda.comprar(j, "gacha", ahora, z, a -> false) != null && j.coronas == 97 && Tienda.compradosHoy(j, "gacha") == 1, "si el correo falla no cobra ni cuenta");
        ok(Tienda.comprar(j, "gacha", ahora, z, a -> { throw new IllegalStateException("x"); }) != null && j.coronas == 97, "si el correo lanza error no cobra");
        for (int k = 0; k < 9; k++) ok(Tienda.comprar(j, "gacha", ahora, z, bien) == null, "compra " + (k + 2));
        ok(Tienda.compradosHoy(j, "gacha") == 10 && j.coronas == 70, "10 compras de 3 coronas");
        err = Tienda.comprar(j, "gacha", ahora, z, bien);
        ok(err != null && err.contains("Hoy ya has comprado 10") && j.coronas == 70, "la undecima se rechaza: " + err);
        ok(Tienda.comprar(j, "ultra", ahora, z, bien) == null && Tienda.compradosHoy(j, "ultra") == 1, "el limite es por articulo");
        d.guardar(id);
        Datos.Jugador r = new Datos(dir).jugador(id, "Ana");
        ok(r.coronas == j.coronas && Tienda.compradosHoy(r, "gacha") == 10 && r.tiendaDia.equals("2026-10-16"), "las compras del dia se guardan");
        ok(Tienda.comprar(j, "gacha", ahora + dia, z, bien) == null && Tienda.compradosHoy(j, "gacha") == 1 && Tienda.compradosHoy(j, "ultra") == 0, "al dia siguiente se vacian las compras");
        j.coronas = 10;
        ok(Tienda.comprar(j, "master", ahora + dia, z, bien) != null && j.coronas == 10, "la Master Ball cuesta mas de 10");
        System.out.println("  tienda: bien");
    }

    static Retos.Prog prog(String id) { Retos.Prog p = new Retos.Prog(); p.id = id; return p; }

    static Datos.Entrada entradaFecha(UUID id, String nombre, int seg, long fecha) {
        Datos.Entrada e = entrada("pikachu", seg, 10);
        e.uuid = id.toString(); e.nombre = nombre; e.fecha = fecha;
        return e;
    }

    static void biomas() {
        System.out.println("== biomas");
        ok(Biomas.TODOS.size() == 3, "hay tres biomas");
        Set<String> ids = new HashSet<>(), dims = new HashSet<>();
        List<String> tiposValidos = List.of("normal", "fire", "water", "grass", "electric", "ice", "fighting", "poison", "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy");
        for (Biomas.Bioma b : Biomas.TODOS) {
            ok(ids.add(b.id()), "id repetido " + b.id());
            ok(dims.add(b.dimension()), "dimension repetida " + b.dimension());
            ok(b.sesgo() > 0 && b.sesgo() < 1, b.id() + ": el sesgo de tipos tiene que estar entre 0 y 1");
            ok(!b.tipos().isEmpty() && tiposValidos.containsAll(b.tipos()), b.id() + ": tipos favoritos validos");
            ok((b.color() >>> 24) == 0xFF, b.id() + ": el color es opaco");
            ok(!b.nombre().isBlank() && !b.texto().isBlank() && !b.musica().isBlank(), b.id() + ": tiene nombre, frase y musica");
            ok(Biomas.porId(b.id().toUpperCase()) == b && Biomas.porDimension(b.dimension()) == b && Biomas.esArena(b.dimension()), b.id() + ": se encuentra por id y por dimension");
        }
        ok(Biomas.porId("nada") == null && Biomas.porId(null) == null && Biomas.deIdOPradera("nada") == Biomas.PRADERA && Biomas.deIdOPradera(null) == Biomas.PRADERA, "un bioma que no existe cae en la pradera");
        ok(!Biomas.esArena("entrada") && !Biomas.esArena("overworld"), "la sala de entrada no es una arena");
        ok(Biomas.porId(new Grupos.Grupo().bioma) == Biomas.PRADERA, "un grupo nuevo juega en la pradera");
        System.out.println("  biomas: bien");
    }

    static Datos.Entrada entrada(String especie, int seg, int derrotados) {
        Datos.Entrada e = new Datos.Entrada();
        e.especie = especie; e.segundos = seg; e.derrotados = derrotados; e.nivel = 10; e.nivelPokemon = 30;
        return e;
    }
}
