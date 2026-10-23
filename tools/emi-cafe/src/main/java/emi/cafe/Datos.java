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
