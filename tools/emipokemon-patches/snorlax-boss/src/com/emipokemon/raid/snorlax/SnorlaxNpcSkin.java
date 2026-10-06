package com.emipokemon.raid.snorlax;

import com.emipokemon.visual.VisualAssetService;
import java.io.InputStream;
import net.minecraft.server.MinecraftServer;

/** Pone la skin de Snorlax Emi (assets/emipokemon/bundled_npc_skins/emi_snorlax_queue.png) al NPC emi_snorlax_queue recien creado. */
public final class SnorlaxNpcSkin {
    public static final String NPC_ID = "emi_snorlax_queue";

    private SnorlaxNpcSkin() {
    }

    public static void apply(VisualAssetService assets, MinecraftServer server, String npcId) {
        if (!NPC_ID.equals(npcId)) {
            return;
        }
        try (InputStream in = SnorlaxNpcSkin.class.getResourceAsStream("/assets/emipokemon/bundled_npc_skins/" + NPC_ID + ".png")) {
            if (in == null) {
                return;
            }
            VisualAssetService.Asset asset = assets.storeNpc(NPC_ID, in.readAllBytes());
            assets.broadcast(server, asset);
        } catch (Throwable error) {
            System.err.println("[snorlax_emi] no se pudo poner la skin al NPC: " + error);
        }
    }
}
