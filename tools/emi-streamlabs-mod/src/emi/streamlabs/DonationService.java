package emi.streamlabs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Logica de las donaciones, sin nada de Minecraft: a quien le toca, cuantos dolares, evitar repetidas, pendientes. */
public final class DonationService {
    /** Entrega las tiradas. Devuelve true si se entregaron (jugador conectado). */
    public interface Entregador {
        boolean entregar(String jugador, int dolares, String donador);
    }

    public static final class Pendiente {
        public String id;
        public String donador;
        public String jugador;
        public int dolares;
        public long creada;
    }

    private static final class Estado {
        List<String> procesadas = new ArrayList<>();
        List<Pendiente> pendientes = new ArrayList<>();
    }

    private static final Pattern MC_NAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final Pattern MC_TAG = Pattern.compile("mc\\s*:\\s*([A-Za-z0-9_]{3,16})", Pattern.CASE_INSENSITIVE);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Config cfg;
    private final Path estadoFile;
    private final Entregador entregador;
    private final Consumer<String> log;
    private final VinculosTwitch vinculos;
    private final Set<String> procesadas = new LinkedHashSet<>();
    private final List<Pendiente> pendientes = new ArrayList<>();

    public DonationService(Config cfg, Path estadoFile, VinculosTwitch vinculos, Entregador entregador, Consumer<String> log) {
        this.cfg = cfg;
        this.vinculos = vinculos;
        this.estadoFile = estadoFile;
        this.entregador = entregador;
        this.log = log;
        cargar();
    }

    /**
     * A que jugador de Minecraft va la donacion (o null). Orden: 1) mc:Nombre en el mensaje; 2) mapa "jugadores" del config;
     * 3) quien vinculo ese nombre de Twitch en Emipokemon; 4) el propio nombre del donador si es un nombre valido de Minecraft.
     */
    public static String resolverJugador(Config cfg, VinculosTwitch vinculos, String donador, String mensaje) {
        Matcher m = MC_TAG.matcher(mensaje == null ? "" : mensaje);
        if (m.find()) return m.group(1);
        String d = donador == null ? "" : donador;
        String mapeado = cfg.jugadores.get(d.toLowerCase(Locale.ROOT));
        if (mapeado != null && MC_NAME.matcher(mapeado).matches()) return mapeado;
        String vinculado = vinculos == null ? null : vinculos.minecraftDe(d);
        if (vinculado != null && MC_NAME.matcher(vinculado).matches()) return vinculado;
        String limpio = d.replaceAll("\\s+", "");
        return MC_NAME.matcher(limpio).matches() ? limpio : null;
    }

    /** Dolares enteros (1 tirada por dolar). null si la moneda no se conoce o la cantidad no es valida. */
    public static Integer aDolares(Config cfg, double cantidad, String moneda) {
        Double rate = cfg.monedas.get((moneda == null || moneda.isBlank() ? "USD" : moneda).toUpperCase(Locale.ROOT));
        if (rate == null || Double.isNaN(cantidad)) return null;
        double n = Math.floor(cantidad * rate + 1e-9);
        return n > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) n;
    }

    /** Evento "event" de Streamlabs (type=donation, message=[{id,name,amount,currency,message,isTest}]). */
    public synchronized void onStreamlabsEvent(String nombre, JsonElement payload) {
        try {
            if (!"event".equals(nombre) || payload == null || !payload.isJsonObject()) return;
            JsonObject ev = payload.getAsJsonObject();
            if (!ev.has("type") || !"donation".equals(str(ev.get("type"))) || !ev.has("message") || !ev.get("message").isJsonArray()) return;
            JsonArray arr = ev.getAsJsonArray("message");
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject m = el.getAsJsonObject();
                if (m.has("isTest") && !m.get("isTest").isJsonNull() && m.get("isTest").getAsBoolean() && !cfg.aceptarPruebasDeStreamlabs) {
                    log.accept("Alerta de PRUEBA de Streamlabs ignorada (aceptarPruebasDeStreamlabs=false).");
                    continue;
                }
                String id = firstNonEmpty(str(m.get("id")), str(m.get("_id")), str(m.get("donation_id")), str(ev.get("event_id")));
                if (id.isEmpty()) {
                    log.accept("Donacion sin id; se ignora por seguridad.");
                    continue;
                }
                double cantidad;
                try {
                    cantidad = Double.parseDouble(str(m.get("amount")));
                } catch (NumberFormatException e) {
                    log.accept("Donacion " + id + " con cantidad no valida; se ignora.");
                    continue;
                }
                String moneda = str(m.get("currency"));
                procesar(id, firstNonEmpty(str(m.get("name")), str(m.get("from"))), cantidad, moneda.isEmpty() ? "USD" : moneda, str(m.get("message")));
            }
        } catch (RuntimeException e) {
            log.accept("Error procesando evento de Streamlabs: " + e);
        }
    }

    synchronized void procesar(String id, String donador, double cantidad, String moneda, String mensaje) {
        log.accept("Donacion recibida: " + donador + " - " + cantidad + " " + moneda + " - \"" + mensaje + "\"");
        if (procesadas.contains(id)) {
            log.accept("Donacion " + id + " repetida: se ignora.");
            return;
        }
        procesadas.add(id);
        Integer dolares = aDolares(cfg, cantidad, moneda);
        if (dolares == null) {
            log.accept("AVISO: moneda \"" + moneda + "\" desconocida (donacion " + id + " de " + donador + "). Anadela a \"monedas\" en config/emi-streamlabs.json. NO se ha entregado nada.");
            guardar();
            return;
        }
        if (dolares < cfg.minimoDolares) {
            log.accept("Donacion de " + donador + " (" + cantidad + " " + moneda + ") por debajo del minimo; sin tiradas.");
            guardar();
            return;
        }
        String jugador = resolverJugador(cfg, vinculos, donador, mensaje);
        if (jugador == null) {
            log.accept("AVISO: no se a que jugador dar la donacion " + id + " de \"" + donador + "\" (" + dolares + " USD). Pide que ponga mc:NombreMinecraft en el mensaje o que vincule su Twitch con Emipokemon, o anadelo a \"jugadores\".");
            guardar();
            return;
        }
        Pendiente p = new Pendiente();
        p.id = id;
        p.donador = donador;
        p.jugador = jugador;
        p.dolares = Math.min(dolares, cfg.maximoDolares);
        p.creada = System.currentTimeMillis();
        if (!intentar(p)) pendientes.add(p);
        guardar();
    }

    /** Reintenta las pendientes (llamar cada reintentoSegundos). */
    public synchronized void reintentar() {
        if (pendientes.isEmpty()) return;
        long limite = System.currentTimeMillis() - cfg.horasMaximasPendiente * 3600L * 1000L;
        List<Pendiente> restantes = new ArrayList<>();
        for (Pendiente p : pendientes) {
            if (p.creada < limite) {
                log.accept("Pendiente " + p.id + " (" + p.jugador + ", " + p.dolares + " USD) descartada por antigua.");
                continue;
            }
            if (!intentar(p)) restantes.add(p);
        }
        boolean cambio = restantes.size() != pendientes.size();
        pendientes.clear();
        pendientes.addAll(restantes);
        if (cambio) guardar();
    }

    public synchronized int numPendientes() {
        return pendientes.size();
    }

    private boolean intentar(Pendiente p) {
        boolean ok;
        try {
            ok = entregador.entregar(p.jugador, p.dolares, p.donador);
        } catch (RuntimeException e) {
            log.accept("Error entregando a " + p.jugador + ": " + e);
            ok = false;
        }
        if (ok) log.accept("ENTREGADO: " + p.dolares + " tirada(s) a " + p.jugador + " (donacion de " + p.donador + ").");
        else log.accept("No entregado a " + p.jugador + " (desconectado?); se reintentara.");
        return ok;
    }

    private void cargar() {
        try {
            if (!Files.exists(estadoFile)) return;
            Estado e = GSON.fromJson(Files.readString(estadoFile, StandardCharsets.UTF_8), Estado.class);
            if (e == null) return;
            if (e.procesadas != null) procesadas.addAll(e.procesadas);
            if (e.pendientes != null) pendientes.addAll(e.pendientes);
        } catch (IOException | RuntimeException ex) {
            log.accept("No se pudo leer " + estadoFile.getFileName() + ": " + ex);
        }
    }

    private void guardar() {
        try {
            Estado e = new Estado();
            List<String> todas = new ArrayList<>(procesadas);
            e.procesadas = todas.size() > 5000 ? new ArrayList<>(todas.subList(todas.size() - 5000, todas.size())) : todas;
            e.pendientes = new ArrayList<>(pendientes);
            Files.createDirectories(estadoFile.getParent());
            Path tmp = estadoFile.resolveSibling(estadoFile.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(e), StandardCharsets.UTF_8);
            Files.move(tmp, estadoFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.accept("No se pudo guardar el estado: " + ex);
        }
    }

    private static String str(JsonElement e) {
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) return "";
        return e.getAsString();
    }

    private static String firstNonEmpty(String... v) {
        for (String s : v) if (s != null && !s.isEmpty()) return s;
        return "";
    }
}
