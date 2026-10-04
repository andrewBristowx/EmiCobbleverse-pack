package emi.streamlabs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** config/emi-streamlabs.json (se crea con valores por defecto la primera vez). */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public String streamlabsToken = "PEGA_AQUI_TU_SOCKET_API_TOKEN";
    public String streamlabsUrl = "https://sockets.streamlabs.com";
    /** nombre de Twitch en minusculas -> nombre de Minecraft */
    public Map<String, String> jugadores = new LinkedHashMap<>();
    /** moneda -> valor en USD de una unidad */
    public Map<String, Double> monedas = new LinkedHashMap<>();
    public int minimoDolares = 1;
    public int maximoDolares = 1279;
    public int reintentoSegundos = 60;
    public int horasMaximasPendiente = 72;
    public boolean aceptarPruebasDeStreamlabs = false;

    public Config() {
        jugadores.put("nombre_de_twitch_en_minusculas", "NombreDeMinecraft");
        monedas.put("USD", 1.0);
        monedas.put("EUR", 1.08);
        monedas.put("MXN", 0.055);
        monedas.put("ARS", 0.001);
        monedas.put("CLP", 0.001);
        monedas.put("COP", 0.00025);
        monedas.put("PEN", 0.27);
        monedas.put("GBP", 1.27);
        monedas.put("CAD", 0.73);
        monedas.put("BRL", 0.18);
    }

    public boolean tokenConfigurado() {
        return streamlabsToken != null && !streamlabsToken.isBlank() && !streamlabsToken.startsWith("PEGA_");
    }

    public static Config load(Path file) throws IOException {
        if (!Files.exists(file)) {
            Config def = new Config();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(def), StandardCharsets.UTF_8);
            return def;
        }
        Config c = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Config.class);
        if (c == null) {
            c = new Config();
        }
        if (c.jugadores == null) c.jugadores = new LinkedHashMap<>();
        if (c.monedas == null || c.monedas.isEmpty()) c.monedas = new Config().monedas;
        c.monedas.putIfAbsent("USD", 1.0);
        Map<String, String> low = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : c.jugadores.entrySet()) low.put(e.getKey().toLowerCase(java.util.Locale.ROOT), e.getValue());
        c.jugadores = low;
        if (c.minimoDolares < 1) c.minimoDolares = 1;
        if (c.maximoDolares < c.minimoDolares) c.maximoDolares = 1279;
        if (c.reintentoSegundos < 5) c.reintentoSegundos = 5;
        if (c.horasMaximasPendiente < 1) c.horasMaximasPendiente = 72;
        return c;
    }
}
