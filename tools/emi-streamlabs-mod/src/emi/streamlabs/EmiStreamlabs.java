package emi.streamlabs;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;

/**
 * Escucha las donaciones de Streamlabs desde el propio servidor y da 1 tirada de gacha por dolar ejecutando
 * function emipokemon:twitch/donacion {jugador:"Nombre",dolares:N} (datapack EmiTwitch-DP). No hace falta RCON ni abrir puertos:
 * solo conexion de salida a sockets.streamlabs.com. Configuracion: config/emi-streamlabs.json.
 * Todo va dentro de try/catch: si algo falla, el servidor arranca igual (solo se pierde la escucha de donaciones).
 */
public final class EmiStreamlabs implements DedicatedServerModInitializer {
    private static final String TAG = "[emi_streamlabs] ";

    private volatile MinecraftServer server;
    private StreamlabsSocket socket;
    private ScheduledExecutorService timer;

    static void log(String msg) {
        System.out.println(TAG + msg);
    }

    @Override
    public void onInitializeServer() {
        try {
            ServerLifecycleEvents.SERVER_STARTED.register(this::iniciar);
            ServerLifecycleEvents.SERVER_STOPPING.register(s -> detener());
        } catch (Throwable error) {
            System.err.println(TAG + "no se pudo registrar: " + error);
        }
    }

    private void iniciar(MinecraftServer s) {
        try {
            Path dir = FabricLoader.getInstance().getConfigDir();
            Config cfg = Config.load(dir.resolve("emi-streamlabs.json"));
            if (!cfg.tokenConfigurado()) {
                log("Falta el token: pon tu Socket API Token de Streamlabs en config/emi-streamlabs.json (\"streamlabsToken\") y reinicia. Las donaciones NO se escuchan.");
                return;
            }
            server = s;
            DonationService service = new DonationService(cfg, dir.resolve("emi-streamlabs-estado.json"), this::entregar, EmiStreamlabs::log);
            socket = new StreamlabsSocket(cfg.streamlabsUrl, cfg.streamlabsToken, service::onStreamlabsEvent, EmiStreamlabs::log);
            socket.start();
            timer = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "emi-streamlabs-reintentos");
                t.setDaemon(true);
                return t;
            });
            timer.scheduleWithFixedDelay(() -> {
                try {
                    service.reintentar();
                } catch (Throwable e) {
                    log("Error en reintento: " + e);
                }
            }, cfg.reintentoSegundos, cfg.reintentoSegundos, TimeUnit.SECONDS);
            log("Activo. Pendientes guardadas: " + service.numPendientes() + ". Moneda base: USD (1 tirada por dolar).");
        } catch (Throwable error) {
            System.err.println(TAG + "error al iniciar: " + error);
        }
    }

    private void detener() {
        try {
            if (socket != null) socket.close();
            if (timer != null) timer.shutdownNow();
        } catch (Throwable ignored) {
            // cerrando
        }
        server = null;
    }

    /** Se llama desde el hilo del socket o del temporizador: pasa al hilo del servidor y espera el resultado. */
    private boolean entregar(String jugador, int dolares, String donador) {
        MinecraftServer s = server;
        if (s == null) return false;
        CompletableFuture<Boolean> r = new CompletableFuture<>();
        s.execute(() -> {
            try {
                r.complete(entregarEnServidor(s, jugador, dolares));
            } catch (Throwable e) {
                r.completeExceptionally(e);
            }
        });
        try {
            return r.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            log("No se pudo entregar a " + jugador + ": " + e);
            return false;
        }
    }

    private static boolean entregarEnServidor(MinecraftServer s, String jugador, int dolares) {
        List<class_3222> online = s.method_3760().method_14571();
        String nombre = null;
        for (class_3222 p : online) {
            String n = p.method_7334().getName();
            if (n != null && n.equalsIgnoreCase(jugador)) {
                nombre = n;
                break;
            }
        }
        if (nombre == null) return false;
        s.method_3734().method_44252(s.method_3739().method_9217(), "function emipokemon:twitch/donacion {jugador:\"" + nombre + "\",dolares:" + dolares + "}");
        return true;
    }
}
