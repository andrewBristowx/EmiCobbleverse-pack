package emi.recursos;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mundos de recursos: un overworld con su nether y su end que se borran y se vuelven a crear (con otra semilla) cada pocos dias. Ver README. */
public class EmiRecursos implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("emi_recursos");

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((d, ra, env) -> Cmds.register(d));
        ServerLifecycleEvents.SERVER_STARTED.register(Ciclo::alArrancar);
        ServerLifecycleEvents.SERVER_STOPPING.register(Ciclo::alParar);
        ServerTickEvents.END_SERVER_TICK.register(Ciclo::tick);
        ServerPlayConnectionEvents.JOIN.register((h, sender, server) -> Ciclo.alEntrar(h.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((viejo, nuevo, vivo) -> Ciclo.alCambiarDeMundo(nuevo, null, nuevo.getServerWorld()));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((p, origen, destino) -> Ciclo.alCambiarDeMundo(p, origen, destino));
    }
}
