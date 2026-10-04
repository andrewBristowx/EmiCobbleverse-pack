package emi.streamlabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Cliente minimo de Socket.IO v2 (Engine.IO v3 sobre WebSocket) para el Socket API de Streamlabs. Sin dependencias de Minecraft.
 * Paquetes: "0{json}" abrir, "40" conectado al namespace, "2"/"3" ping/pong, "42[\"evento\",datos]" evento.
 * Se reconecta solo con espera creciente (5 s .. 60 s).
 */
public final class StreamlabsSocket implements AutoCloseable {
    private final String baseUrl;
    private final String token;
    private final BiConsumer<String, JsonElement> onEvent;
    private final Consumer<String> log;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "emi-streamlabs-socket");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean closed;
    private volatile WebSocket ws;
    private ScheduledFuture<?> pinger;
    private int attempt;

    public StreamlabsSocket(String baseUrl, String token, BiConsumer<String, JsonElement> onEvent, Consumer<String> log) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.onEvent = onEvent;
        this.log = log;
    }

    public void start() {
        scheduler.execute(this::connect);
    }

    private URI uri() {
        String b = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (b.startsWith("https://")) b = "wss://" + b.substring(8);
        else if (b.startsWith("http://")) b = "ws://" + b.substring(7);
        return URI.create(b + "/socket.io/?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) + "&EIO=3&transport=websocket");
    }

    private void connect() {
        if (closed) return;
        try {
            http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(15)).buildAsync(uri(), new Listener()).whenComplete((socket, error) -> {
                if (error != null) {
                    log.accept("No se pudo conectar con Streamlabs: " + (error.getCause() != null ? error.getCause().getMessage() : error.getMessage()));
                    scheduleReconnect();
                } else {
                    ws = socket;
                }
            });
        } catch (Exception e) {
            log.accept("Error al conectar con Streamlabs: " + e.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (closed) return;
        stopPinger();
        long wait = Math.min(60, 5L * (1L << Math.min(attempt++, 4)));
        scheduler.schedule(this::connect, wait, TimeUnit.SECONDS);
    }

    private void stopPinger() {
        if (pinger != null) {
            pinger.cancel(false);
            pinger = null;
        }
    }

    @Override
    public void close() {
        closed = true;
        stopPinger();
        WebSocket s = ws;
        if (s != null) {
            try {
                s.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
            } catch (Exception ignored) {
            }
        }
        scheduler.shutdownNow();
    }

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder buf = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buf.append(data);
            if (last) {
                String msg = buf.toString();
                buf.setLength(0);
                try {
                    handle(webSocket, msg);
                } catch (Exception e) {
                    log.accept("Error leyendo un paquete de Streamlabs: " + e);
                }
            }
            webSocket.request(1);
            return null;
        }

        private void handle(WebSocket webSocket, String msg) {
            if (msg.isEmpty()) return;
            char t = msg.charAt(0);
            if (t == '0') { // open: {"sid":..,"pingInterval":25000,"pingTimeout":60000}
                long interval = 25000;
                try {
                    interval = JsonParser.parseString(msg.substring(1)).getAsJsonObject().get("pingInterval").getAsLong();
                } catch (Exception ignored) {
                }
                stopPinger();
                pinger = scheduler.scheduleAtFixedRate(() -> {
                    try {
                        webSocket.sendText("2", true);
                    } catch (Exception ignored) {
                    }
                }, interval, interval, TimeUnit.MILLISECONDS);
            } else if (t == '2') { // ping del servidor (Engine.IO v4)
                webSocket.sendText("3", true);
            } else if (msg.startsWith("40")) {
                attempt = 0;
                log.accept("Conectado a Streamlabs. Esperando donaciones...");
            } else if (msg.startsWith("42")) {
                int i = msg.indexOf('[');
                if (i < 0) return;
                JsonElement el = JsonParser.parseString(msg.substring(i));
                if (el.isJsonArray()) {
                    JsonArray a = el.getAsJsonArray();
                    if (a.size() >= 2 && a.get(0).isJsonPrimitive()) {
                        onEvent.accept(a.get(0).getAsString(), a.get(1));
                    }
                }
            } else if (msg.startsWith("44")) {
                log.accept("Streamlabs rechazo la conexion (token no valido?): " + msg);
            }
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.accept("Desconectado de Streamlabs (" + statusCode + " " + reason + "); reintentando.");
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.accept("Error de conexion con Streamlabs: " + error.getMessage());
            scheduleReconnect();
        }
    }
}
