package emi.ftbkeys;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;

/**
 * Deja SIN TECLA todas las teclas de FTB Quests y FTB Teams, una sola vez por instalacion (marca en config/).
 * Las teclas por defecto ya van vacias en el pack (Default Options), pero quien ya abrio el juego las tiene guardadas en options.txt.
 * Despues de esa unica vez, el jugador puede asignar las que quiera y no se tocan mas.
 * Todo va dentro de try/catch: si algo falla (otra version del juego), no se hace nada y el juego arranca igual.
 */
public final class EmiFtbKeys implements ClientModInitializer {
    static final String MARKER = "emi_ftb_keys_reset_v1.txt";

    @Override
    public void onInitializeClient() {
        try {
            Path marker = FabricLoader.getInstance().getConfigDir().resolve(MARKER);
            ClientLifecycleEvents.CLIENT_STARTED.register(client -> run(client, marker));
        } catch (Throwable error) {
            System.err.println("[emi_ftb_keys] no se pudo registrar: " + error);
        }
    }

    /** @return cuantas teclas se dejaron sin asignar (-1 si ya estaba hecho o fallo) */
    static int run(class_310 client, Path marker) {
        try {
            if (Files.exists(marker)) {
                return -1;
            }
            int changed = 0;
            class_304[] keys = client.field_1690.field_1839;
            for (class_304 key : keys) {
                String name = key.method_1431();
                if (name != null && (name.startsWith("key.ftbquests.") || name.startsWith("key.ftbteams."))) {
                    key.method_1422(class_3675.field_16237);
                    changed++;
                }
            }
            if (changed > 0) {
                class_304.method_1426();
                client.field_1690.method_1640();
            }
            Files.writeString(marker, "Teclas de FTB Quests/Teams dejadas sin asignar (" + changed + "). Borra este archivo para repetirlo.\n", StandardCharsets.UTF_8);
            System.out.println("[emi_ftb_keys] " + changed + " teclas de FTB sin asignar");
            return changed;
        } catch (Throwable error) {
            System.err.println("[emi_ftb_keys] error: " + error);
            return -1;
        }
    }
}
