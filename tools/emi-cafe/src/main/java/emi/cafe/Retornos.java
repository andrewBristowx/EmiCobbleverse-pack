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
        else if (Cafes.enCafe(p)) { PENDIENTES.add(p.getUuid()); ESPERA.add(20); }          // varado en el cafe sin nada guardado (se perdio el archivo): se le saca igual
    }

    private static int reloj;

    static void tick(MinecraftServer s) {
        // red de seguridad: alguien en la dimension del cafe que no esta en ningun cafe (por ejemplo vuelve de otro minijuego a donde se guardo) se saca
        if (++reloj % 40 == 0) {
            for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
                if (Cafes.enCafe(p) && !Cafes.tiene(p.getUuid()) && !PENDIENTES.contains(p.getUuid())) { PENDIENTES.add(p.getUuid()); ESPERA.add(1); }
            }
        }
        if (PENDIENTES.isEmpty()) return;
        for (int i = PENDIENTES.size() - 1; i >= 0; i--) {
            if (ESPERA.get(i) - 1 > 0) { ESPERA.set(i, ESPERA.get(i) - 1); continue; }
            UUID id = PENDIENTES.remove(i); ESPERA.remove(i);
            ServerPlayerEntity p = s.getPlayerManager().getPlayer(id);
            if (p == null || Cafes.tiene(id)) continue;
            if (Saved.restore(s, p)) EmiCafe.LOG.info("Devuelto lo guardado a {} (cafe cortado)", p.getGameProfile().getName());
            else if (Cafes.enCafe(p)) {
                net.minecraft.util.math.BlockPos sp = s.getOverworld().getSpawnPos();
                p.teleport(s.getOverworld(), sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, 0f, 0f);
                p.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
                EmiCafe.LOG.warn("{} estaba varado en el cafe sin nada guardado: al spawn del mundo normal", p.getGameProfile().getName());
            }
        }
    }
}
