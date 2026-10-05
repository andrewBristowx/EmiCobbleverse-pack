package emi.streamlabs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lee los jugadores que ya vincularon su Twitch con Emipokemon (config/emipokemon/twitch_profiles.json):
 * nombre de Twitch (minusculas) -> nombre de Minecraft. Solo lectura; se vuelve a leer cuando el archivo cambia.
 */
public final class VinculosTwitch {
    private final Path file;
    private long lastModified = Long.MIN_VALUE;
    private Map<String, String> mapa = new HashMap<>();

    public VinculosTwitch(Path file) {
        this.file = file;
    }

    /** Nombre de Minecraft vinculado a ese nombre de Twitch, o null. */
    public synchronized String minecraftDe(String twitchLogin) {
        if (twitchLogin == null || twitchLogin.isBlank()) return null;
        recargarSiCambio();
        String limpio = twitchLogin.trim().toLowerCase(Locale.ROOT);
        String r = mapa.get(limpio);
        return r != null ? r : mapa.get(limpio.replaceAll("\\s+", ""));
    }

    private void recargarSiCambio() {
        try {
            if (!Files.exists(file)) {
                mapa = new HashMap<>();
                lastModified = Long.MIN_VALUE;
                return;
            }
            long m = Files.getLastModifiedTime(file).toMillis();
            if (m == lastModified) return;
            Map<String, String> nuevo = new HashMap<>();
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (root.isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().entrySet()) {
                    if (!e.getValue().isJsonObject()) continue;
                    JsonObject p = e.getValue().getAsJsonObject();
                    String login = str(p, "twitchLogin");
                    String mc = str(p, "minecraftName");
                    boolean linked = p.has("linked") && p.get("linked").isJsonPrimitive() && p.get("linked").getAsBoolean();
                    if (linked && !login.isEmpty() && !mc.isEmpty()) nuevo.put(login.toLowerCase(Locale.ROOT), mc);
                }
            }
            mapa = nuevo;
            lastModified = m;
        } catch (Exception e) {
            // archivo a medio escribir o ilegible: se conserva lo ultimo bueno y se reintenta en la proxima donacion
            lastModified = Long.MIN_VALUE;
        }
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() || !e.isJsonPrimitive() ? "" : e.getAsString();
    }
}
