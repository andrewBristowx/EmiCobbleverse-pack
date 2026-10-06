package com.emipokemon.twitch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Guardado de twitch_profiles.json SIN mantener el candado del almacen durante la escritura a disco.
 * El original (TwitchProfileStore.save) era synchronized y escribia el archivo con el candado cogido; el hilo principal del servidor
 * pide ese mismo candado muchas veces por tick (nombres de los hologramas, lista de jugadores, chat), asi que un guardado lento
 * (disco ocupado, varias desconexiones a la vez) congelaba el tick. Ahora: se copia el mapa con el candado un instante, se escribe
 * fuera del candado a un archivo temporal propio y se mueve al sitio; si una copia mas nueva ya se movio, la vieja se descarta.
 */
final class TwitchStoreSave {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<UUID, TwitchProfile>>() { }.getType();
    private static final AtomicLong SEQ = new AtomicLong();
    private static final Object ORDER = new Object();
    private static long lastMoved;

    private TwitchStoreSave() {
    }

    static void save(TwitchProfileStore store) {
        Map<UUID, TwitchProfile> copy;
        long mine;
        synchronized (store) {
            copy = new LinkedHashMap<>(store.profiles);
            mine = SEQ.incrementAndGet();
        }
        Path target = store.file;
        Path tmp = null;
        try {
            Files.createDirectories(target.getParent());
            tmp = target.resolveSibling(target.getFileName() + "." + Thread.currentThread().getId() + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(copy, MAP_TYPE, writer);
            }
            synchronized (ORDER) {
                if (mine > lastMoved) {
                    try {
                        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (Exception atomicFailed) {
                        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    lastMoved = mine;
                } else {
                    Files.deleteIfExists(tmp);
                }
            }
        } catch (Exception error) {
            System.err.println("[emipokemon] No se pudo guardar twitch_profiles.json: " + error);
            try {
                if (tmp != null) {
                    Files.deleteIfExists(tmp);
                }
            } catch (Exception ignored) {
                // nada que hacer
            }
        }
    }
}
