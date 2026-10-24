package emi.recursos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Lo que se guarda en disco (carpeta {@code <mundo>/emi_recursos/}): el ciclo actual de los mundos de recursos y por donde entro cada jugador. */
final class Estado {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** El ciclo actual: cada ciclo tiene su propio numero, su semilla y su hora de reinicio. */
    static final class Ciclo {
        int epoca;
        long semilla;
        long inicioMs;
        long reinicioMs;
        /** Punto de aparicion del overworld de recursos (se guarda porque Fantasy no lo conserva al reiniciar el servidor). */
        int spawnX, spawnY, spawnZ;
        boolean hayspawn;
        /** La pelea del dragon del End de recursos (texto SNBT), para no perderla si el servidor se reinicia a mitad de ciclo. */
        String dragon;
    }

    /** Un jugador: si esta dentro de los mundos de recursos y desde donde entro (para devolverlo ahi al salir). */
    static final class Jug {
        boolean dentro;
        String dim;
        double x, y, z;
        float yaw, pitch;
    }

    private static final class Fichero {
        Ciclo ciclo;
        Map<String, Jug> jugadores = new HashMap<>();
    }

    private static Fichero datos = new Fichero();
    private static Path ruta;

    static Path carpeta(MinecraftServer s) { return s.getSavePath(WorldSavePath.ROOT).resolve("emi_recursos"); }

    static void cargar(MinecraftServer s) {
        ruta = carpeta(s).resolve("estado.json");
        datos = new Fichero();
        try {
            if (Files.exists(ruta)) {
                Fichero f = GSON.fromJson(Files.readString(ruta, StandardCharsets.UTF_8), Fichero.class);
                if (f != null) datos = f;
                if (datos.jugadores == null) datos.jugadores = new HashMap<>();
            }
        } catch (Exception e) { EmiRecursos.LOG.error("No se pudo leer estado.json (se empieza de cero)", e); }
    }

    static void guardar() {
        if (ruta == null) return;
        try {
            Files.createDirectories(ruta.getParent());
            Path tmp = ruta.resolveSibling("estado.json.tmp");
            Files.writeString(tmp, GSON.toJson(datos), StandardCharsets.UTF_8);
            Files.move(tmp, ruta, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) { EmiRecursos.LOG.error("No se pudo guardar estado.json", e); }
    }

    static Ciclo ciclo() { return datos.ciclo; }
    static void ciclo(Ciclo c) { datos.ciclo = c; guardar(); }

    static Jug jug(UUID id) { return datos.jugadores.get(id.toString()); }
    static Jug jugNuevo(UUID id) { return datos.jugadores.computeIfAbsent(id.toString(), k -> new Jug()); }
}
