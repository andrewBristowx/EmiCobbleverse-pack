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

    static final String DEFAULT = "{\n"
            + "  \"_ayuda\": \"Cada entrada añade un hueco al menu de ubicaciones. region: Kanto, Johto, Hoenn, Sinnoh o una nueva. structure: id de la estructura (la usa el mod emi_estructuras). aliases: textos para que Emipokemon la reconozca.\",\n"
            + "  \"ubicaciones\": [\n"
            + "    {\"region\": \"Kanto\", \"label\": \"Torre Team Rocket\", \"structure\": \"cobbleverse:team_rocket_tower\", \"aliases\": [\"cobbleverse team rocket tower\"]},\n"
            + "    {\"region\": \"Kanto\", \"label\": \"Casa de Ash\", \"structure\": \"cobbleverse:ash\", \"aliases\": [\"cobbleverse ash\"]},\n"
            + "    {\"region\": \"Sinnoh\", \"label\": \"Isla Creciente (Crescent Isle)\", \"structure\": \"cobbleverse:crescent_isle\", \"aliases\": [\"cobbleverse crescent isle\"]},\n"
            + "    {\"region\": \"Sinnoh\", \"label\": \"Templo Manaphy\", \"structure\": \"cobbleverse:mythical/manaphy\", \"aliases\": [\"cobbleverse mythical manaphy\"]},\n"
            + "    {\"region\": \"Sinnoh\", \"label\": \"Isla Giratina\", \"structure\": \"legendarymonuments:giratina_island\", \"aliases\": [\"legendarymonuments giratina island\"]},\n"
            + "    {\"region\": \"Sinnoh\", \"label\": \"Portal de la Distorsión\", \"structure\": \"legendarymonuments:distortion_portal\", \"aliases\": [\"legendarymonuments distortion portal\"]},\n"
            + "    {\"region\": \"Galar\", \"label\": \"Torre de la Corona (Calyrex)\", \"structure\": \"cobbleverse:crown_spire\", \"aliases\": [\"cobbleverse crown spire\"]},\n"
            + "    {\"region\": \"Galar\", \"label\": \"Cementerio de la Corona (Spectrier)\", \"structure\": \"cobbleverse:crown_cemetery\", \"aliases\": [\"cobbleverse crown cemetery\"]},\n"
            + "    {\"region\": \"Galar\", \"label\": \"Árbol Dyna (Dynamax)\", \"structure\": \"cobbleverse:dyna_tree\", \"aliases\": [\"cobbleverse dyna tree\"]},\n"
            + "    {\"region\": \"Galar\", \"label\": \"Capullo de Eternatus\", \"structure\": \"legendarymonuments:eternatus_cocoon\", \"aliases\": [\"legendarymonuments eternatus cocoon\"]},\n"
            + "    {\"region\": \"Paldea\", \"label\": \"Santuario Firescourge (Chi-Yu)\", \"structure\": \"legendarymonuments:firescourge_shrine\", \"aliases\": [\"legendarymonuments firescourge shrine\"]},\n"
            + "    {\"region\": \"Paldea\", \"label\": \"Santuario Grasswither (Wo-Chien)\", \"structure\": \"legendarymonuments:grasswither_shrine\", \"aliases\": [\"legendarymonuments grasswither shrine\"]},\n"
            + "    {\"region\": \"Paldea\", \"label\": \"Santuario Groundblight (Ting-Lu)\", \"structure\": \"legendarymonuments:groundblight_shrine\", \"aliases\": [\"legendarymonuments groundblight shrine\"]},\n"
            + "    {\"region\": \"Paldea\", \"label\": \"Santuario Icerend (Chien-Pao)\", \"structure\": \"legendarymonuments:icerend_shrine\", \"aliases\": [\"legendarymonuments icerend shrine\"]},\n"
            + "    {\"region\": \"Paldea\", \"label\": \"Outskirt Stand\", \"structure\": \"legendarymonuments:outskirt_stand\", \"aliases\": [\"legendarymonuments outskirt stand\"]},\n"
            + "    {\"region\": \"Alola\", \"label\": \"Torre del Amanecer\", \"structure\": \"cobbleverse:dawn_tower\", \"aliases\": [\"cobbleverse dawn tower\"]},\n"
            + "    {\"region\": \"Alola\", \"label\": \"Torre del Anochecer\", \"structure\": \"cobbleverse:dusk_tower\", \"aliases\": [\"cobbleverse dusk tower\"]}\n"
            + "  ]\n"
            + "}\n";

    /** Se llama con el mapa EXPECTED de RegionalStructureAuditService (region -> lista de huecos). */
    public static void apply(Map<String, List<RegionalStructureAuditService.Expected>> expected) {
        try {
            Path file = FabricLoader.getInstance().getConfigDir().resolve("emipokemon").resolve("extra-locations.json");
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, DEFAULT);
            }
            JsonObject root = GSON.fromJson(Files.readString(file), JsonObject.class);
            JsonArray list = root == null ? null : root.getAsJsonArray("ubicaciones");
            if (list == null) return;
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
