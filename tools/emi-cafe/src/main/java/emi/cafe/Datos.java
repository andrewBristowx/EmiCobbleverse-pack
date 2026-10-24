package emi.cafe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Lo que se guarda en disco de cada jugador ({@code <mundo>/emi_cafe/jugadores/<uuid>.json}); se escribe en un archivo temporal y se renombra. Sin Minecraft (solo Gson). */
final class Datos {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static final class Jugador {
        String nombre = "";
        int monedas;                 // el dinero del cafe: se gana sirviendo y se gasta en platos y mejoras
        int clientes;                // clientes servidos en total
        int turnos;                  // turnos completos
        int mejorTurno;              // lo mas que se ha ganado en un turno
        String inicial;              // el Pokemon cocinero elegido (null = aun no ha elegido)
        java.util.List<String> platos = new java.util.ArrayList<>();                 // platos que sabe hacer
        java.util.Map<String, Integer> mejoras = new java.util.HashMap<>();          // nivel de cada mejora
        int mesasCap;                                                                  // cuantas mesas tenia ya asignadas (para saber cuando una mejora da mesas nuevas)
        java.util.List<int[]> mesasPos = new java.util.ArrayList<>();                // donde estan las mesas ({x, z} del local): se pueden mover en el modo construccion
        java.util.Map<String, String> decor = new java.util.LinkedHashMap<>();         // lo que el dueño ha colocado en el modo construccion: «x,y,z» (relativos al local) -> estado del bloque (SNBT)
        int[] estilo = {0, 0, 0, 1};                                                  // materiales del local (ver Estilo): paredes, franja, suelo de la cocina y suelo del comedor
        java.util.Map<String, int[]> estPos = new java.util.HashMap<>();              // donde el dueño ha puesto cada estacion ({x, z} del local; nombre del enum -> posicion); las que no estan aqui van en su sitio de siempre
        int rep;                                                                      // puntos de reputacion del cafe (ver Reputacion)
        int megusta;                                                                  // «me gusta» recibidos de otros jugadores
        java.util.List<String> likesDe = new java.util.ArrayList<>();                 // uuid de quienes ya dieron «me gusta» a este cafe (uno por persona)
        boolean visitas = true;                                                       // ¿se puede visitar su cafe?
        java.util.List<String> especiales = new java.util.ArrayList<>();             // platos de visitantes especiales que ya conoce
        int especialesServidos;                                                       // visitantes especiales atendidos en total

        // estrellas (la moneda de la tienda del servidor), estadisticas, tienda, misiones y logros
        int estrellas, estrellasTotal;
        long monedasTotal; int propinasTotal, sinEnfados, lavados;
        String tiendaDia = "";
        java.util.Map<String, Integer> tiendaCompras = new java.util.HashMap<>();      // articulo -> compras de hoy
        java.util.List<String> unicos = new java.util.ArrayList<>();                   // articulos de una sola vez ya comprados
        String misDia = "", misSemana = "";
        java.util.Map<String, Integer> misProg = new java.util.HashMap<>();            // progreso de las misiones de hoy y de la semana
        java.util.List<String> misHechas = new java.util.ArrayList<>();
        java.util.Map<String, Integer> logros = new java.util.HashMap<>();             // familia de logro -> nivel ya cobrado

        int nivel(String mejora) { return mejoras == null ? 0 : mejoras.getOrDefault(mejora, 0); }
    }

    private final Path raiz;
    private final Map<UUID, Jugador> jugadores = new HashMap<>();

    Datos(Path raiz) { this.raiz = raiz; }

    private Path archivo(UUID id) { return raiz.resolve("jugadores").resolve(id + ".json"); }

    Jugador jugador(UUID id, String nombre) {
        Jugador j = jugadores.get(id);
        if (j == null) {
            try { if (Files.exists(archivo(id))) j = GSON.fromJson(Files.readString(archivo(id), StandardCharsets.UTF_8), Jugador.class); }
            catch (Exception e) { EmiCafe.LOG.error("No se pudo leer los datos de {}", id, e); }
            if (j == null) j = new Jugador();
            jugadores.put(id, j);
        }
        if (nombre != null && !nombre.isEmpty()) j.nombre = nombre;
        return j;
    }

    /** Un resumen de cada cafe para los tops (lee los archivos de todos los jugadores; se guarda 20 s para no leer cada vez). */
    record Fila(String nombre, int megusta, int rep, int estilo) {}
    private long topHasta;
    private java.util.List<Fila> topCache = new java.util.ArrayList<>();

    java.util.List<Fila> top(int n) {
        long ahora = System.currentTimeMillis();
        if (ahora > topHasta) {
            topHasta = ahora + 20_000;
            java.util.List<Fila> l = new java.util.ArrayList<>();
            java.util.Set<UUID> vistos = new java.util.HashSet<>();
            for (java.util.Map.Entry<UUID, Jugador> en : jugadores.entrySet()) { vistos.add(en.getKey()); añadir(l, en.getValue()); }      // los cargados (mas recientes que el disco)
            try (java.util.stream.Stream<Path> st = Files.list(raiz.resolve("jugadores"))) {
                for (Path f : (Iterable<Path>) st::iterator) {
                    String nom = f.getFileName().toString();
                    if (!nom.endsWith(".json")) continue;
                    try {
                        if (vistos.contains(UUID.fromString(nom.substring(0, nom.length() - 5)))) continue;
                        añadir(l, GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Jugador.class));
                    } catch (Exception e) { /* archivo raro: se salta */ }
                }
            } catch (IOException e) { /* aun no hay carpeta */ }
            l.sort((a, b) -> a.megusta() != b.megusta() ? Integer.compare(b.megusta(), a.megusta()) : Integer.compare(b.estilo(), a.estilo()));
            topCache = l;
        }
        return topCache.subList(0, Math.min(n, topCache.size()));
    }

    private static void añadir(java.util.List<Fila> l, Jugador j) {
        if (j == null || j.nombre == null || j.nombre.isEmpty() || j.inicial == null) return;
        l.add(new Fila(j.nombre, j.megusta, j.rep, Estilo.puntos(j.decor, j.estilo)));
    }

    void guardar(UUID id) {
        Jugador j = jugadores.get(id);
        if (j == null) return;
        try {
            Path f = archivo(id);
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(j), StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { EmiCafe.LOG.error("No se pudo guardar los datos de {}", id, e); }
    }
}
