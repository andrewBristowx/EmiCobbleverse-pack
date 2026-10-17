package emi.survivors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que se guarda en disco, dentro de la carpeta del mundo ({@code emi_survivors/}): por cada jugador sus michiestrellitas y las mejoras permanentes de cada Pokemon
 * ({@code jugadores/<uuid>.json}) y el top por tiempo ({@code top.json}, la mejor marca de cada jugador). Se escribe en un archivo temporal y se renombra, para no
 * dejar nunca un archivo a medias. Sin Minecraft (solo Gson), asi se prueba con un main.
 */
final class Datos {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final int MAX_TOP = 100;

    /** Mejoras de un Pokemon concreto (se identifica por su UUID de Cobblemon: si lo sueltas o lo cambias, pierdes sus mejoras). */
    static final class PokeMeta {
        Map<String, Integer> mejoras = new HashMap<>();
        String especie = "";
        int mejorTiempo, partidas;
    }

    static final class Jugador {
        String nombre = "";
        int monedas, partidas, mejorTiempo;
        int coronas;                 // la moneda de las recompensas (desde 0.8.0): se gana con misiones y logros, es escasa y no sirve para las mejoras
        Map<String, PokeMeta> pokemon = new HashMap<>();
        // misiones (desde 0.8.0): las del dia y las de la semana con su avance; si cambia la fecha se renuevan ({@link Retos#refrescar})
        String diaClave = "", semanaClave = "";
        List<Retos.Prog> dia = new ArrayList<>(), semana = new ArrayList<>();
        // logros (desde 0.8.0): escalones conseguidos de cada familia y los contadores que los miden (los de antes se cuentan desde que se instala la 0.8.0)
        Map<String, Integer> logros = new HashMap<>();
        int derrotadosTotal, jefesTotal, evolucionesTotal, partidasGrupo, mejorNivel, mejorJefes, shiniesTotal, legendariosTotal;
        long segundosTotal;
        List<String> biomas = new ArrayList<>(), especies = new ArrayList<>();
        // tienda de recompensas (desde 0.9.0): lo comprado hoy de cada articulo (se vacia al cambiar el dia, ver {@link Tienda#refrescar})
        String tiendaDia = "";
        Map<String, Integer> tiendaCompras = new HashMap<>();
    }

    static final String SOLO = "solo", GRUPO = "grupo";

    /** Un jugador de un equipo del top: quien era, con que Pokemon y como le fue. */
    static final class MiembroTop {
        String uuid = "", nombre = "", especie = "", aspectos = "";
        int nivelPokemon, derrotados, nivel;
    }

    /**
     * Una marca del top: la mejor partida de un equipo. En solitario ({@code modo} = «solo») {@code uuid} es el del jugador y solo cuenta su mejor marca; en grupo
     * ({@code modo} = «grupo») es la clave del equipo (los uuid de sus miembros ordenados y unidos con «+»: un mismo grupo solo guarda su mejor marca).
     * {@code nombre} es el del jugador o los de todos («A + B»); {@code especie}, {@code aspectos} y {@code nivelPokemon} son los del primero. Las marcas de la fase 1 no
     * tienen {@code modo} ni {@code miembros}: son de un jugador.
     */
    static final class Entrada {
        String uuid = "", nombre = "", especie = "", aspectos = "";
        int nivelPokemon, segundos, derrotados, nivel;
        boolean jefe;                // mato a algun jefe (de la fase 1: el unico)
        int jefes;                   // cuantos jefes mato (desde 0.5.0; las marcas viejas con jefe=true cuentan como 1)
        long fecha;      // milisegundos desde 1970
        String modo = SOLO;
        List<MiembroTop> miembros = new ArrayList<>();
    }

    /** Todas las partidas acabadas (solo las ultimas {@link #MAX_HISTORIAL}), para los tops de hoy, de la semana y del mes. */
    private static final class HistorialArchivo {
        List<Entrada> partidas = new ArrayList<>();
    }

    static final int MAX_HISTORIAL = 3000;

    private static final class TopArchivo {
        List<Entrada> entradas = new ArrayList<>();     // en solitario
        List<Entrada> grupos = new ArrayList<>();       // en grupo
    }

    private final Path raiz;
    private final Map<UUID, Jugador> jugadores = new HashMap<>();
    private TopArchivo top;
    private HistorialArchivo historial;

    Datos(Path raiz) { this.raiz = raiz; }

    // ------------------------------------------------------------ jugadores

    private Path archivo(UUID id) { return raiz.resolve("jugadores").resolve(id + ".json"); }

    /** Los datos del jugador (se leen del disco la primera vez; si no existen, se crean vacios). */
    Jugador jugador(UUID id, String nombre) {
        Jugador j = jugadores.get(id);
        if (j == null) {
            j = leer(archivo(id), Jugador.class);
            if (j == null) j = new Jugador();
            if (j.pokemon == null) j.pokemon = new HashMap<>();
            if (j.dia == null) j.dia = new ArrayList<>();
            if (j.semana == null) j.semana = new ArrayList<>();
            if (j.logros == null) j.logros = new HashMap<>();
            if (j.biomas == null) j.biomas = new ArrayList<>();
            if (j.especies == null) j.especies = new ArrayList<>();
            if (j.tiendaCompras == null) j.tiendaCompras = new HashMap<>();
            jugadores.put(id, j);
        }
        if (nombre != null && !nombre.isEmpty()) j.nombre = nombre;
        return j;
    }

    void guardar(UUID id) {
        Jugador j = jugadores.get(id);
        if (j != null) escribir(archivo(id), j);
    }

    PokeMeta poke(Jugador j, String pokeId, String especie) {
        PokeMeta m = j.pokemon.computeIfAbsent(pokeId, k -> new PokeMeta());
        if (m.mejoras == null) m.mejoras = new HashMap<>();
        if (especie != null && !especie.isEmpty()) m.especie = especie;
        return m;
    }

    /** Sube una mejora permanente de un Pokemon si hay monedas; devuelve el motivo si no se puede, o null si se hizo. */
    String comprar(UUID id, String nombre, String pokeId, String especie, String mejoraId) {
        if (Meta.mejora(mejoraId) == null) return "Esa mejora no existe.";
        Jugador j = jugador(id, nombre);
        PokeMeta m = poke(j, pokeId, especie);
        int nivel = m.mejoras.getOrDefault(mejoraId, 0);
        if (nivel >= Meta.MAX) return "Esa mejora ya está al máximo.";
        int costo = Meta.costo(Meta.mejora(mejoraId), nivel);
        if (j.monedas < costo) return "Te faltan " + (costo - j.monedas) + " michiestrellitas.";
        j.monedas -= costo;
        m.mejoras.put(mejoraId, nivel + 1);
        guardar(id);
        return null;
    }

    /** Apunta una partida acabada de un jugador: suma las monedas, cuenta la partida y recuerda su mejor tiempo (en total y con ese Pokemon). El top es aparte ({@link #registrarEquipo}). */
    void registrarJugador(UUID id, String nombre, String pokeId, String especie, int segundos, int monedas) {
        Jugador j = jugador(id, nombre);
        PokeMeta m = poke(j, pokeId, especie);
        j.monedas += monedas;
        j.partidas++; m.partidas++;
        j.mejorTiempo = Math.max(j.mejorTiempo, segundos);
        m.mejorTiempo = Math.max(m.mejorTiempo, segundos);
        guardar(id);
    }

    /** Apunta una partida (que cuenta) en las misiones y los logros del jugador y le suma las recompensas. Devuelve lo que ha pasado, para contarselo. */
    Retos.Novedades retos(UUID id, String nombre, Retos.Partida s, long ahoraMs, java.time.ZoneId zona) {
        Jugador j = jugador(id, nombre);
        Retos.Novedades n = Retos.aplicar(j, s, ahoraMs, zona);
        guardar(id);
        return n;
    }

    /** Apunta la partida de un equipo en el top (solo o de grupos, segun su {@code modo}): si mejora la marca de ese jugador o grupo, la sustituye. Devuelve el puesto (1 = el primero) o 0 si no mejora su marca (o no entra). */
    int registrarEquipo(Entrada e) {
        e.fecha = System.currentTimeMillis();
        if (e.miembros == null) e.miembros = new ArrayList<>();
        anotarHistorial(e);
        List<Entrada> t = lista(e.modo);
        Entrada previa = null;
        for (Entrada x : t) if (x.uuid.equals(e.uuid)) previa = x;
        if (previa != null) {
            if (previa.segundos >= e.segundos) return 0;      // no mejora su marca: el top se queda como estaba y no se le dice «puesto N»
            t.remove(previa);
        }
        t.add(e);
        ordenar(t);
        while (t.size() > MAX_TOP) t.remove(t.size() - 1);
        escribir(raiz.resolve("top.json"), top);
        return puesto(e.uuid, e.modo);
    }

    // ------------------------------------------------------------ historial y tops por periodo

    private HistorialArchivo historial() {
        if (historial == null) {
            historial = leer(raiz.resolve("historial.json"), HistorialArchivo.class);
            if (historial == null) {
                // primera vez (o partidas anteriores a 0.7.0): se parte de las marcas del top, que ya llevan su fecha
                historial = new HistorialArchivo();
                historial.partidas.addAll(top().entradas);
                historial.partidas.addAll(top().grupos);
            }
            if (historial.partidas == null) historial.partidas = new ArrayList<>();
        }
        return historial;
    }

    /** Solo para las pruebas: apunta una partida con su fecha en el historial (sin tocar el top). */
    void anotarPartidaConFecha(Entrada e) { anotarHistorial(e); }

    private void anotarHistorial(Entrada e) {
        List<Entrada> h = historial().partidas;
        h.add(e);
        while (h.size() > MAX_HISTORIAL) h.remove(0);
        escribir(raiz.resolve("historial.json"), historial);
    }

    /** Las {@code n} mejores marcas de ese modo desde {@code desdeMs} (la mejor de cada jugador o grupo en ese tiempo). */
    List<Entrada> topDesde(String modo, long desdeMs, int n) {
        Map<String, Entrada> mejor = new HashMap<>();
        for (Entrada x : historial().partidas) {
            if (!modo.equals(x.modo == null || x.modo.isEmpty() ? SOLO : x.modo) || x.fecha < desdeMs) continue;
            Entrada p = mejor.get(x.uuid);
            if (p == null || x.segundos > p.segundos) mejor.put(x.uuid, x);
        }
        List<Entrada> l = new ArrayList<>(mejor.values());
        ordenar(l);
        return primeras(l, n);
    }

    /** El top de un periodo («hoy», «semana», «mes» o «siempre») en solitario o de grupos. */
    List<Entrada> topPeriodo(String modo, String periodo, int n, long ahoraMs, java.time.ZoneId zona) {
        if (!Periodos.existe(periodo) || "siempre".equalsIgnoreCase(periodo)) return primeras(lista(modo), n);
        return topDesde(modo, Periodos.desde(periodo, ahoraMs, zona), n);
    }

    // ------------------------------------------------------------ top

    private TopArchivo top() {
        if (top == null) {
            top = leer(raiz.resolve("top.json"), TopArchivo.class);
            if (top == null) top = new TopArchivo();
            if (top.entradas == null) top.entradas = new ArrayList<>();
            if (top.grupos == null) top.grupos = new ArrayList<>();
            ordenar(top.entradas);
            ordenar(top.grupos);
        }
        return top;
    }

    private List<Entrada> lista(String modo) { return GRUPO.equals(modo) ? top().grupos : top().entradas; }

    private static void ordenar(List<Entrada> l) {
        l.sort(Comparator.comparingInt((Entrada x) -> -x.segundos).thenComparingLong(x -> x.fecha));      // mas tiempo primero; a igualdad, el que lo hizo antes
    }

    /** Las {@code n} mejores marcas en solitario. */
    List<Entrada> top(int n) { return primeras(lista(SOLO), n); }

    /** Las {@code n} mejores marcas de grupos. */
    List<Entrada> topGrupos(int n) { return primeras(lista(GRUPO), n); }

    private static List<Entrada> primeras(List<Entrada> t, int n) { return new ArrayList<>(t.subList(0, Math.min(n, t.size()))); }

    /** Puesto (1 = primero) del jugador en el top en solitario, o 0 si no esta. */
    int puesto(UUID id) { return puesto(id.toString(), SOLO); }

    /** Puesto (1 = primero) de esa clave (uuid del jugador, o los de un grupo unidos con «+») en el top de ese modo, o 0 si no esta. */
    int puesto(String clave, String modo) {
        List<Entrada> t = lista(modo);
        for (int i = 0; i < t.size(); i++) if (t.get(i).uuid.equals(clave)) return i + 1;
        return 0;
    }

    /** Puesto del jugador en el mejor de sus tops (en solitario o en algun grupo suyo), para enseñarlo en el menu. */
    int puestoEnGrupos(UUID id) {
        List<Entrada> t = lista(GRUPO);
        for (int i = 0; i < t.size(); i++) for (MiembroTop m : t.get(i).miembros) if (m.uuid.equals(id.toString())) return i + 1;
        return 0;
    }

    /** Vacia los dos tops (para empezar un periodo nuevo) guardando antes una copia con la fecha. Devuelve el nombre de la copia, o null si no habia nada. */
    String reiniciarTop() {
        TopArchivo t = top();
        if (t.entradas.isEmpty() && t.grupos.isEmpty()) return null;
        String copia = "top_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
        escribir(raiz.resolve(copia), top);
        historial();                                  // (antes de vaciar el top: si no existia, se crea a partir de el)
        t.entradas.clear(); t.grupos.clear();
        escribir(raiz.resolve("top.json"), top);
        escribir(raiz.resolve(copia.replace("top_", "historial_")), historial);
        historial.partidas.clear();
        escribir(raiz.resolve("historial.json"), historial);
        return copia;
    }

    // ------------------------------------------------------------ disco

    private static <T> T leer(Path f, Class<T> c) {
        try {
            if (!Files.exists(f)) return null;
            return GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), c);
        } catch (Exception e) {
            // un archivo estropeado no debe tumbar el minijuego: se aparta y se empieza de cero
            try { Files.move(f, f.resolveSibling(f.getFileName() + ".roto"), StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) { }
            return null;
        }
    }

    private static void escribir(Path f, Object o) {
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar " + f, e);
        }
    }
}
