package com.emipokemon.raid.snorlax;

import com.emipokemon.config.EmipokemonConfig;
import net.minecraft.*;
import net.minecraft.server.MinecraftServer;

/** Solo para compilar: las firmas son las del jar real (los metodos y el campo privados se hacen de paquete al parchear). */
final class SnorlaxRaidService {
    static final class ActiveRaid {
        java.util.UUID entityId;
        class_5321<class_1937> worldKey;
        java.util.Set<java.util.UUID> participants;
        int phase;
        boolean shockwaveActive;
        int crushChargeTicksRemaining;
    }
    static volatile ActiveRaid active;
    static void resolveCombat(MinecraftServer s, class_3218 w, class_1297 e, class_1309 l, ActiveRaid r, EmipokemonConfig.SnorlaxBossSettings c) { }
    static void knockback(class_1297 from, class_3222 to, double strength) { }
    static void triggerAnimation(class_1297 e, String name) { }
    static void broadcastToParticipants(MinecraftServer s, ActiveRaid r, String msg) { }
    static double damageMultiplier(int n, EmipokemonConfig.SnorlaxBossSettings c) { return 1; }
    static double phaseMeleeDamage(int phase, EmipokemonConfig.SnorlaxBossSettings c) { return 1; }
    static double phaseAreaSlamDamage(int phase, EmipokemonConfig.SnorlaxBossSettings c) { return 1; }
}
