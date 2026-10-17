package emi.carreras;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que se guarda en disco, dentro de la carpeta del mundo ({@code emi_carreras/}): por cada jugador sus estrellas, sus estadisticas, sus logros y lo que ha comprado
 * ({@code jugadores/<uuid>.json}) y los mejores tiempos de cada pista ({@code tiempos.json}). Se escribe en un archivo temporal y se renombra, para no dejar nunca un
 * archivo a medias. Sin Minecraft (solo Gson), asi se prueba con un main.
 */
final class Datos {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final int MAX_TOP = 10;

    static final class Jugador {
        String nombre = "";
        int estrellas;                  // la moneda de las carreras: se gana corriendo y con los logros, y se gasta en la tienda
        int estrellasGanadas;           // las que ha ganado en total (no baja al comprar)
        // estadisticas (miden los logros)
        int carreras, terminadas, victorias, podios, entrenamientos, recogidas, golpesDados, intocables, recordsMejorados, michiTerminadas;
        long segundos;                  // tiempo total corriendo
        Map<String, Integer> mejores = new HashMap<>();      // pista -> mejor tiempo (ticks), de carreras y de entrenamientos
        List<String> pistas = new ArrayList<>(), especies = new ArrayList<>();
        Map<String, Integer> logros = new HashMap<>();       // familia -> escalones conseguidos
        // tienda: lo comprado hoy de cada articulo (se vacia al cambiar el dia) y los articulos unicos ya comprados
        String tiendaDia = "";
        Map<String, Integer> tiendaCompras = new HashMap<>();
        List<String> unicos = new ArrayList<>();
    }

    /** Una marca de la tabla de tiempos de una pista. */
    static final class Marca {
        String uuid = "", nombre = "", especie = "";
        int ticks;
        long fecha;
    }

    private static final class TiemposArchivo { Map<String, List<Marca>> pistas = new HashMap<>(); }

    private final Path raiz;
    private final Map<UUID, Jugador> jugadores = new HashMap<>();
    private TiemposArchivo tiempos;

    Datos(Path raiz) { this.raiz = raiz; }

    // ------------------------------------------------------------ jugadores

    private Path archivo(UUID id) { return raiz.resolve("jugadores").resolve(id + ".json"); }

    /** Los datos del jugador (se leen del disco la primera vez; si no existen, se crean vacios). */
    Jugador jugador(UUID id, String nombre) {
        Jugador j = jugadores.get(id);
        if (j == null) {
            j = leer(archivo(id), Jugador.class);
            if (j == null) j = new Jugador();
            if (j.mejores == null) j.mejores = new HashMap<>();
            if (j.pistas == null) j.pistas = new ArrayList<>();
            if (j.especies == null) j.especies = new ArrayList<>();
            if (j.logros == null) j.logros = new HashMap<>();
            if (j.tiendaCompras == null) j.tiendaCompras = new HashMap<>();
            if (j.unicos == null) j.unicos = new ArrayList<>();
            jugadores.put(id, j);
        }
        if (nombre != null && !nombre.isEmpty()) j.nombre = nombre;
        return j;
    }

    void guardar(UUID id) {
        Jugador j = jugadores.get(id);
        if (j != null) escribir(archivo(id), j);
    }

    // ------------------------------------------------------------ tiempos por pista

    private Path archivoTiempos() { return raiz.resolve("tiempos.json"); }

    private TiemposArchivo tiempos() {
        if (tiempos == null) {
            tiempos = leer(archivoTiempos(), TiemposArchivo.class);
            if (tiempos == null) tiempos = new TiemposArchivo();
            if (tiempos.pistas == null) tiempos.pistas = new HashMap<>();
        }
        return tiempos;
    }

    /**
     * Apunta el tiempo de un jugador en una pista si mejora el suyo (cada jugador solo tiene su mejor marca por pista). Devuelve true si era su record personal
     * (o su primera marca en esa pista).
     */
    boolean registrarTiempo(UUID id, String nombre, String pista, int ticks, String especie, long ahora) {
        Jugador j = jugador(id, nombre);
        Integer antes = j.mejores.get(pista);
        boolean record = antes == null || ticks < antes;
        if (!record) return false;
        j.mejores.put(pista, ticks);
        List<Marca> l = tiempos().pistas.computeIfAbsent(pista, k -> new ArrayList<>());
        l.removeIf(m -> m.uuid.equals(id.toString()));
        Marca m = new Marca();
        m.uuid = id.toString(); m.nombre = nombre; m.ticks = ticks; m.especie = especie; m.fecha = ahora;
        l.add(m);
        l.sort(Comparator.comparingInt(x -> x.ticks));
        while (l.size() > MAX_TOP) l.remove(l.size() - 1);
        escribir(archivoTiempos(), tiempos());
        return true;
    }

    /** Los mejores tiempos de una pista (de menor a mayor). */
    List<Marca> top(String pista) { return tiempos().pistas.getOrDefault(pista, List.of()); }

    // ------------------------------------------------------------ disco

    private <T> T leer(Path f, Class<T> c) {
        try {
            if (!Files.exists(f)) return null;
            return GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), c);
        } catch (Exception e) {
            // un archivo roto no tumba el servidor: se aparta y se empieza de cero
            try { Files.move(f, f.resolveSibling(f.getFileName() + ".roto"), StandardCopyOption.REPLACE_EXISTING); } catch (IOException ex) { /* nada */ }
            return null;
        }
    }

    private void escribir(Path f, Object o) {
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar " + f, e);
        }
    }
}
