package emi.fall;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cuando alguien se conecta y tenia guardado lo suyo de una sesion del torneo que se corto (el servidor se paro, se desconecto jugando...), se le devuelve todo
 * y vuelve a su sitio un instante despues (no se puede teletransportar a un jugador en el mismo tick en que entra).
 */
final class Retornos {
    private Retornos() {}

    private static final List<UUID> PENDIENTES = new ArrayList<>();
    private static final List<Integer> ESPERA = new ArrayList<>();

    static void alEntrar(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        if (Torneo.apuntado(p.getUuid())) return;
        if (Saved.exists(s, p.getUuid())) { PENDIENTES.add(p.getUuid()); ESPERA.add(20); }
    }

    static void tick(MinecraftServer s) {
        if (PENDIENTES.isEmpty()) return;
        for (int i = PENDIENTES.size() - 1; i >= 0; i--) {
            if (ESPERA.get(i) - 1 > 0) { ESPERA.set(i, ESPERA.get(i) - 1); continue; }
            UUID id = PENDIENTES.remove(i); ESPERA.remove(i);
            ServerPlayerEntity p = s.getPlayerManager().getPlayer(id);
            if (p == null || Torneo.apuntado(id)) continue;
            Morph.quitar(s, p);
            if (Saved.restore(s, p)) EmiFall.LOG.info("Devuelto lo guardado a {} (sesion del torneo cortada)", p.getGameProfile().getName());
        }
    }
}
