package com.emipokemon.twitch;

import net.minecraft.server.MinecraftServer;

public class TwitchService {
    private final StreamBonusService streamBonuses = null;

    private void broadcastHudSnapshots() { }

    /** Igual que activateRandomRelicBonus pero SIN el anuncio de "Caja Misteriosa": quien llama anuncia lo que quiera. */
    public String emiActivateBonusQuiet(MinecraftServer server) {
        String label = this.streamBonuses.activateRandomItemBonus();
        if (label == null) {
            return null;
        }
        if (server != null) {
            this.broadcastHudSnapshots();
        }
        return label;
    }

    public String activateRandomRelicBonus(MinecraftServer server) { return null; }
}
