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
        curvas(); catalogo(); cartas(); meta(); datos(); grupos();
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
        ok(Curvas.monedas(0, 0, 1, false, 1.0) >= 0, "monedas no negativas");
        int m5 = Curvas.monedas(300, 250, 15, false, 1.0);
        System.out.println("  monedas de una partida de 5 min, 250 derrotados, nivel 15: " + m5 + " (con jefe: " + Curvas.monedas(300, 250, 15, true, 1.0) + ")");
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
        Stats s = new Stats();
        Map<String, Integer> p = new HashMap<>();
        for (Catalogo.Pasiva x : Catalogo.PASIVAS) p.put(x.id(), Catalogo.MAX_NIVEL);
        Catalogo.aplicarPasivas(s, p);
        System.out.printf("  con todas las pasivas al maximo: vida %.0f regen %.1f vel x%.2f dano x%.2f recarga x%.2f area x%.2f iman %.1f xp x%.2f armadura %.0f%%%n",
                s.hpMax, s.regen, s.vel, s.dano, s.recarga, s.area, s.iman, s.xp, s.armadura * 100);
        ok(s.recarga > 0.3 && s.armadura <= 0.6, "los limites de recarga y armadura");
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
        // todo al maximo: solo curar y xp
        armas.replaceAll((k, v) -> Catalogo.MAX_NIVEL); pasivas.replaceAll((k, v) -> Catalogo.MAX_NIVEL);
        List<Cartas.Carta> l = Cartas.sortear(rnd, armas, pasivas);
        ok(l.size() == 2 && l.contains(Cartas.CURAR) && l.contains(Cartas.XP), "todo al maximo: solo quedan curar y xp: " + l);
        // un solo candidato posible
        armas.clear(); pasivas.clear();
        for (Catalogo.Arma a : Catalogo.ARMAS) armas.put(a.id(), a.id().equals("tajo") ? 4 : Catalogo.MAX_NIVEL);
        for (Catalogo.Pasiva p : Catalogo.PASIVAS) pasivas.put(p.id(), Catalogo.MAX_NIVEL);
        armas.remove("disparo");     // 9 armas (tope 6): no se pueden anadir y el tajo sube de 4 a 5
        l = Cartas.sortear(rnd, armas, pasivas);
        ok(l.get(0).equals(new Cartas.Carta("arma", "tajo", 5)), "la unica subida posible sale primero: " + l);
        System.out.println("  sorteos de prueba: bien");
    }

    static void meta() {
        System.out.println("== mejoras permanentes");
        int total = 0;
        for (int n = 0; n < Meta.MAX; n++) total += Meta.costo(n);
        System.out.println("  coste de subir una mejora al maximo: " + total + " michiestrellitas; las 7 mejoras: " + total * Meta.MEJORAS.size());
        ok(Meta.costo(0) == 25 && Meta.costo(4) == 125, "costes 25..125");
        Stats s = new Stats();
        Map<String, Integer> m = new HashMap<>();
        for (Meta.Mejora x : Meta.MEJORAS) m.put(x.id(), Meta.MAX);
        Meta.aplicar(s, m);
        System.out.printf("  con todas al maximo: vida %.0f dano x%.2f vel x%.3f xp x%.2f iman %.2f regen %.2f monedas x%.2f%n", s.hpMax, s.dano, s.vel, s.xp, s.iman, s.regen, s.monedas);
        ok(s.hpMax == 160, "vigor 5 = +60 de vida");
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

    static Datos.Entrada entrada(String especie, int seg, int derrotados) {
        Datos.Entrada e = new Datos.Entrada();
        e.especie = especie; e.segundos = seg; e.derrotados = derrotados; e.nivel = 10; e.nivelPokemon = 30;
        return e;
    }
}
