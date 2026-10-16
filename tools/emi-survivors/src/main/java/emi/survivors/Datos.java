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
        Map<String, PokeMeta> pokemon = new HashMap<>();
    }

    /** Una marca del top: la mejor partida de un jugador. */
    static final class Entrada {
        String uuid = "", nombre = "", especie = "", aspectos = "";
        int nivelPokemon, segundos, derrotados, nivel;
        boolean jefe;
        long fecha;      // milisegundos desde 1970
    }

    private static final class TopArchivo { List<Entrada> entradas = new ArrayList<>(); }

    private final Path raiz;
    private final Map<UUID, Jugador> jugadores = new HashMap<>();
    private TopArchivo top;

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
        int costo = Meta.costo(nivel);
        if (j.monedas < costo) return "Te faltan " + (costo - j.monedas) + " michiestrellitas.";
        j.monedas -= costo;
        m.mejoras.put(mejoraId, nivel + 1);
        guardar(id);
        return null;
    }

    /** Apunta una partida acabada: suma las monedas, cuenta la partida y, si es su mejor marca, la pone en el top. Devuelve el puesto (1 = el primero) o 0 si no entra al top. */
    int registrar(UUID id, String nombre, String pokeId, Entrada e, int monedas) {
        Jugador j = jugador(id, nombre);
        PokeMeta m = poke(j, pokeId, e.especie);
        j.monedas += monedas;
        j.partidas++; m.partidas++;
        j.mejorTiempo = Math.max(j.mejorTiempo, e.segundos);
        m.mejorTiempo = Math.max(m.mejorTiempo, e.segundos);
        guardar(id);
        e.uuid = id.toString(); e.nombre = nombre; e.fecha = System.currentTimeMillis();
        List<Entrada> t = top().entradas;
        Entrada previa = null;
        for (Entrada x : t) if (x.uuid.equals(e.uuid)) previa = x;
        if (previa != null) {
            if (previa.segundos >= e.segundos) return puesto(id);      // no mejora su marca
            t.remove(previa);
        }
        t.add(e);
        ordenar(t);
        while (t.size() > MAX_TOP) t.remove(t.size() - 1);
        escribir(raiz.resolve("top.json"), top);
        return puesto(id);
    }

    // ------------------------------------------------------------ top

    private TopArchivo top() {
        if (top == null) {
            top = leer(raiz.resolve("top.json"), TopArchivo.class);
            if (top == null || top.entradas == null) top = new TopArchivo();
            ordenar(top.entradas);
        }
        return top;
    }

    private static void ordenar(List<Entrada> l) {
        l.sort(Comparator.comparingInt((Entrada x) -> -x.segundos).thenComparingLong(x -> x.fecha));      // mas tiempo primero; a igualdad, el que lo hizo antes
    }

    /** Las {@code n} mejores marcas. */
    List<Entrada> top(int n) { List<Entrada> t = top().entradas; return new ArrayList<>(t.subList(0, Math.min(n, t.size()))); }

    /** Puesto (1 = primero) del jugador en el top, o 0 si no esta. */
    int puesto(UUID id) {
        List<Entrada> t = top().entradas;
        for (int i = 0; i < t.size(); i++) if (t.get(i).uuid.equals(id.toString())) return i + 1;
        return 0;
    }

    /** Vacia el top (para empezar un periodo nuevo) guardando antes una copia con la fecha. Devuelve el nombre de la copia, o null si no habia nada. */
    String reiniciarTop() {
        List<Entrada> t = top().entradas;
        if (t.isEmpty()) return null;
        String copia = "top_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
        escribir(raiz.resolve(copia), top);
        t.clear();
        escribir(raiz.resolve("top.json"), top);
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
