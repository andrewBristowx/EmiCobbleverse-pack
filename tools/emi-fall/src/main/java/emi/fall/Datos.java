package emi.fall;

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
import java.util.stream.Stream;

/**
 * Lo que se guarda en disco, dentro de la carpeta del mundo ({@code emi_fall/jugadores/<uuid>.json}): por cada jugador sus estrellas, estadisticas, misiones y lo que ha comprado.
 * Se escribe en un archivo temporal y se renombra, para no dejar nunca un archivo a medias. Sin Minecraft (solo Gson).
 */
final class Datos {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static final class Jugador {
        String nombre = "";
        int estrellas;                  // la moneda de Emi Fall: se gana jugando y con misiones, y se gasta en la tienda
        int estrellasGanadas;           // las que ha ganado en total (no baja al comprar)
        // estadisticas (miden las misiones y el top)
        int torneos, torneosGanados, podios, rondas, rondasGanadas, dulces, muros, torneosSinElim;
        int mejorPuntos;                // la mayor puntuacion en un torneo
        long segundos;
        // misiones: las del dia y las de la semana con su avance; si cambia la fecha se renuevan ({@link Retos#refrescar})
        String diaClave = "", semanaClave = "";
        List<Retos.Prog> dia = new ArrayList<>(), semana = new ArrayList<>();
        // tienda: lo comprado hoy de cada articulo (se vacia al cambiar el dia) y los articulos unicos ya comprados
        String tiendaDia = "";
        Map<String, Integer> tiendaCompras = new HashMap<>();
        List<String> unicos = new ArrayList<>();
    }

    private final Path raiz;
    private final Map<UUID, Jugador> jugadores = new HashMap<>();

    Datos(Path raiz) { this.raiz = raiz; }

    private Path archivo(UUID id) { return raiz.resolve("jugadores").resolve(id + ".json"); }

    /** Los datos del jugador (se leen del disco la primera vez; si no existen, se crean vacios). */
    Jugador jugador(UUID id, String nombre) {
        Jugador j = jugadores.get(id);
        if (j == null) {
            j = leer(archivo(id));
            if (j == null) j = new Jugador();
            if (j.dia == null) j.dia = new ArrayList<>();
            if (j.semana == null) j.semana = new ArrayList<>();
            if (j.diaClave == null) j.diaClave = "";
            if (j.semanaClave == null) j.semanaClave = "";
            if (j.tiendaDia == null) j.tiendaDia = "";
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

    // ------------------------------------------------------------ top

    private List<Jugador> todos;
    private long todosHora;

    /** Todos los jugadores con datos (los del disco y los de memoria), con una copia que se renueva cada 20 s. */
    private List<Jugador> todos() {
        long ahora = System.currentTimeMillis();
        if (todos == null || ahora - todosHora > 20_000) {
            Map<String, Jugador> m = new HashMap<>();
            try (Stream<Path> s = Files.isDirectory(raiz.resolve("jugadores")) ? Files.list(raiz.resolve("jugadores")) : Stream.<Path>empty()) {
                for (Path p : (Iterable<Path>) s::iterator) {
                    if (!p.getFileName().toString().endsWith(".json")) continue;
                    Jugador j = leer(p);
                    if (j != null) m.put(p.getFileName().toString().replace(".json", ""), j);
                }
            } catch (IOException e) { /* se usa lo que haya */ }
            for (Map.Entry<UUID, Jugador> e : jugadores.entrySet()) m.put(e.getKey().toString(), e.getValue());
            todos = new ArrayList<>(m.values());
            todosHora = ahora;
        }
        return todos;
    }

    /** Los mejores 10 por torneos ganados (desempata por estrellas ganadas); solo cuentan los que han ganado algo. */
    List<Jugador> topGanadores() {
        List<Jugador> l = new ArrayList<>();
        for (Jugador j : todos()) if (j.torneosGanados > 0) l.add(j);
        l.sort(Comparator.comparingInt((Jugador x) -> -x.torneosGanados).thenComparingInt(x -> -x.estrellasGanadas));
        return l.size() > 10 ? l.subList(0, 10) : l;
    }

    /** Los mejores 10 por estrellas ganadas en total. */
    List<Jugador> topEstrellas() {
        List<Jugador> l = new ArrayList<>();
        for (Jugador j : todos()) if (j.estrellasGanadas > 0) l.add(j);
        l.sort(Comparator.comparingInt((Jugador x) -> -x.estrellasGanadas));
        return l.size() > 10 ? l.subList(0, 10) : l;
    }

    void invalidarTop() { todos = null; }

    // ------------------------------------------------------------ disco

    private Jugador leer(Path f) {
        try {
            if (!Files.exists(f)) return null;
            return GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Jugador.class);
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
