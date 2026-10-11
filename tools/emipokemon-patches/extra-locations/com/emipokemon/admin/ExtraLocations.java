package com.emipokemon.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ubicaciones extra para el menu de ubicaciones del Diario (parche PatchExtraLocations, pack 1.0.74).
 * Emipokemon solo trae 69 ubicaciones fijas (Kanto, Johto, Hoenn y Sinnoh); las demas estructuras de Cobbleverse y
 * Legendary Monuments no tenian hueco. Esto lee config/emipokemon/extra-locations.json y las añade al final de su region
 * (si la region no existe la crea, p. ej. Galar o Paldea). Si el archivo no existe se crea con los valores por defecto.
 * La clase RegionalStructureAuditService la llama al final de su inicializador estatico.
 */
public final class ExtraLocations {
    private ExtraLocations() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** Lista completa por defecto (la misma que config/emipokemon/extra-locations.json del paquete), dentro del jar como recurso. */
    static String porDefecto() throws java.io.IOException {
        try (var in = ExtraLocations.class.getResourceAsStream("/com/emipokemon/admin/extra-locations-default.json")) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /** Se llama con el mapa EXPECTED de RegionalStructureAuditService (region -> lista de huecos). */
    public static void apply(Map<String, List<RegionalStructureAuditService.Expected>> expected) {
        try {
            Path file = FabricLoader.getInstance().getConfigDir().resolve("emipokemon").resolve("extra-locations.json");
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, porDefecto());
            }
            JsonObject root = GSON.fromJson(Files.readString(file), JsonObject.class);
            JsonArray list = root == null ? null : root.getAsJsonArray("ubicaciones");
            if (list == null) return;
            // Un config viejo (creado antes de que existieran Galar, Paldea, Alola o Dungeons) no se actualiza solo: las entradas por defecto que
            // falten (por id de estructura) se añaden en memoria al final, sin tocar el archivo, asi las pestañas nuevas salen siempre.
            java.util.Set<String> tiene = new java.util.HashSet<>();
            for (JsonElement el : list) { JsonObject o = el.getAsJsonObject(); if (o.has("structure")) tiene.add(o.get("structure").getAsString()); }
            JsonArray todas = new JsonArray(); todas.addAll(list);
            JsonArray def = GSON.fromJson(porDefecto(), JsonObject.class).getAsJsonArray("ubicaciones");
            for (JsonElement el : def) { JsonObject o = el.getAsJsonObject(); if (!tiene.contains(o.get("structure").getAsString())) todas.add(o); }
            list = todas;
            for (JsonElement el : list) {
                JsonObject o = el.getAsJsonObject();
                String region = o.get("region").getAsString().trim();
                String label = o.get("label").getAsString().trim();
                List<String> aliases = new ArrayList<>();
                if (o.has("aliases")) for (JsonElement a : o.getAsJsonArray("aliases")) aliases.add(a.getAsString().toLowerCase(Locale.ROOT));
                String key = region;
                for (String k : expected.keySet()) if (k.equalsIgnoreCase(region)) key = k;
                List<RegionalStructureAuditService.Expected> cur = new ArrayList<>(expected.getOrDefault(key, List.of()));
                cur.add(new RegionalStructureAuditService.Expected(label, List.copyOf(aliases)));
                expected.put(key, cur);
            }
        } catch (Throwable t) {
            System.err.println("[Emipokemon] No se pudieron cargar las ubicaciones extra: " + t);
        }
    }
}
