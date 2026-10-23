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
        int monedas;                 // el dinero del cafe: se gana sirviendo y se gasta en mejoras (mas adelante)
        int clientes;                // clientes servidos en total
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
