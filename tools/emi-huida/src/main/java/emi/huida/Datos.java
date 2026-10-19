package emi.huida;

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
 * Lo que se guarda en disco, dentro de la carpeta del mundo ({@code emi_huida/}): por cada jugador sus estrellas, estadisticas, logros, misiones y lo que ha comprado
 * ({@code jugadores/<uuid>.json}) y las mejores puntuaciones de cada nivel ({@code tops.json}). Se escribe en un archivo temporal y se renombra, para no dejar nunca un
 * archivo a medias. Sin Minecraft (solo Gson), asi se prueba con un main (tools/emi-huida/test/PruebaLogica.java).
 */
final class Datos {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final int MAX_TOP = 10;

    static final class Jugador {
        String nombre = "";
        int estrellas;                  // la moneda de la huida: se gana corriendo y con logros y misiones, y se gasta en la tienda
        int estrellasGanadas;           // las que ha ganado en total (no baja al comprar)
        // estadisticas (miden los logros)
        int huidas, escapes, perfectos, atrapado, victorias, podios, michiestrellas, mejoradas;
        long segundos;                  // tiempo total corriendo
        int mejorPuntos;                // la mayor puntuacion en una sola huida
        Map<String, Integer> mejores = new HashMap<>();          // nivel -> mejor puntuacion (solo huidas en las que escapo)
        Map<String, Integer> escapesNivel = new HashMap<>();     // nivel -> veces que ha escapado
        Map<String, Integer> logros = new HashMap<>();           // familia -> escalones conseguidos
        // misiones: las del dia y las de la semana con su avance; si cambia la fecha se renuevan ({@link Retos#refrescar})
        String diaClave = "", semanaClave = "";
        List<Retos.Prog> dia = new ArrayList<>(), semana = new ArrayList<>();
        // tienda: lo comprado hoy de cada articulo (se vacia al cambiar el dia) y los articulos unicos ya comprados
        String tiendaDia = "";
        Map<String, Integer> tiendaCompras = new HashMap<>();
        List<String> unicos = new ArrayList<>();
    }

    /** Una marca de la tabla de un nivel: quien escapo con mas puntos. */
    static final class Marca {
        String uuid = "", nombre = "";
        int puntos, errores;
        double tiempo;
        long fecha;
    }

    private static final class TopsArchivo { Map<String, List<Marca>> niveles = new HashMap<>(); }

    private final Path raiz;
    private final Map<UUID, Jugador> jugadores = new HashMap<>();
    private TopsArchivo tops;

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
            if (j.escapesNivel == null) j.escapesNivel = new HashMap<>();
            if (j.logros == null) j.logros = new HashMap<>();
            if (j.dia == null) j.dia = new ArrayList<>();
            if (j.semana == null) j.semana = new ArrayList<>();
            if (j.diaClave == null) j.diaClave = "";
            if (j.semanaClave == null) j.semanaClave = "";
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

    // ------------------------------------------------------------ tabla de cada nivel

    private Path archivoTops() { return raiz.resolve("tops.json"); }

    private TopsArchivo tops() {
        if (tops == null) {
            tops = leer(archivoTops(), TopsArchivo.class);
            if (tops == null) tops = new TopsArchivo();
            if (tops.niveles == null) tops.niveles = new HashMap<>();
        }
        return tops;
    }

    /**
     * Apunta la puntuacion de un jugador en un nivel si mejora la suya (cada jugador solo tiene su mejor marca por nivel; a igualdad de puntos gana quien tuvo menos errores y luego menos tiempo).
     * Devuelve 2 si era su primera marca en ese nivel, 1 si ha batido su marca y 0 si no la ha mejorado.
     */
    int registrar(UUID id, String nombre, String nivel, int puntos, int errores, double tiempo, long ahora) {
        Jugador j = jugador(id, nombre);
        Integer antes = j.mejores.get(nivel);
        int veredicto = antes == null ? 2 : puntos > antes ? 1 : 0;
        List<Marca> l = tops().niveles.computeIfAbsent(nivel, k -> new ArrayList<>());
        Marca previa = null;
        for (Marca m : l) if (m.uuid.equals(id.toString())) previa = m;
        boolean mejor = previa == null || puntos > previa.puntos || (puntos == previa.puntos && (errores < previa.errores || (errores == previa.errores && tiempo < previa.tiempo)));
        if (veredicto != 0) j.mejores.put(nivel, puntos);
        if (!mejor) return veredicto;
        l.removeIf(m -> m.uuid.equals(id.toString()));
        Marca m = new Marca();
        m.uuid = id.toString(); m.nombre = nombre; m.puntos = puntos; m.errores = errores; m.tiempo = tiempo; m.fecha = ahora;
        l.add(m);
        l.sort(Comparator.comparingInt((Marca x) -> -x.puntos).thenComparingInt(x -> x.errores).thenComparingDouble(x -> x.tiempo));
        while (l.size() > MAX_TOP) l.remove(l.size() - 1);
        escribir(archivoTops(), tops());
        return veredicto;
    }

    /** Las mejores puntuaciones de un nivel (de mas a menos). */
    List<Marca> top(String nivel) { return tops().niveles.getOrDefault(nivel, List.of()); }

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
