package emi.cafe;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Si alguien se conecta y tenia guardado lo suyo de un cafe que se corto (servidor parado, desconexion...), se le devuelve todo un instante despues de entrar. */
final class Retornos {
    private Retornos() {}

    private static final List<UUID> PENDIENTES = new ArrayList<>();
    private static final List<Integer> ESPERA = new ArrayList<>();

    static void alEntrar(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        if (Cafes.tiene(p.getUuid())) return;
        if (Saved.exists(s, p.getUuid())) { PENDIENTES.add(p.getUuid()); ESPERA.add(20); }
    }

    static void tick(MinecraftServer s) {
        if (PENDIENTES.isEmpty()) return;
        for (int i = PENDIENTES.size() - 1; i >= 0; i--) {
            if (ESPERA.get(i) - 1 > 0) { ESPERA.set(i, ESPERA.get(i) - 1); continue; }
            UUID id = PENDIENTES.remove(i); ESPERA.remove(i);
            ServerPlayerEntity p = s.getPlayerManager().getPlayer(id);
            if (p == null || Cafes.tiene(id)) continue;
            if (Saved.restore(s, p)) EmiCafe.LOG.info("Devuelto lo guardado a {} (cafe cortado)", p.getGameProfile().getName());
        }
    }
}
