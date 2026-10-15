package com.emipokemon.challenge;

import net.minecraft.class_3222;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Los lideres de las regiones propias (Teselia, Kalos, Galar, Paldea y Alola) no tienen serie en RCT, asi que el limite de nivel de RCT/Cobbleverse
 * que les aplicaba Emipokemon no tiene sentido ahi: un jugador con un equipo de nivel alto no podia combatir («Tu equipo supera el limite de nivel...»).
 * Para esas regiones (1) {@code NpcBattleService.start} ya no comprueba el limite y (2) {@code DynamicTrainerLevelService.scale} sube el equipo del
 * lider hasta el nivel mas alto del jugador (mas 1 en el Alto Mando y 2 en el Campeon, como antes) SIN el tope del limite de RCT.
 */
public final class LevelCapBypass {
    private LevelCapBypass() {}

    private static final Set<String> REGIONES = Set.of("teselia", "kalos", "galar", "paldea", "alola");
    private static final Pattern LEVEL = Pattern.compile("(?i)\\blevel=(\\d+)");

    /** true = el lider es de una region propia y no se le aplica el limite de nivel de RCT. */
    public static boolean skip(ChallengeProfile profile) {
        return profile != null && profile.regionId() != null && REGIONES.contains(profile.regionId().toLowerCase());
    }

    /** Lo mismo que {@code DynamicTrainerLevelService.scale} pero sin el tope del limite de RCT. */
    public static DynamicTrainerLevelService.ScalingResult scale(class_3222 player, ChallengeProfile profile) {
        int roleBonus = profile.isChampion() ? 2 : (profile.isEliteFour() ? 1 : 0);
        List<String> source = profile.team() == null ? List.of() : profile.team();
        int authoredMax = maxLevel(source);
        if (player == null || authoredMax <= 0) return new DynamicTrainerLevelService.ScalingResult(source, authoredMax, authoredMax, false);
        int highest = RctCobbleverseBridge.levelCap(player).highestPartyLevel();
        if (highest <= authoredMax) return new DynamicTrainerLevelService.ScalingResult(source, authoredMax, authoredMax, false);
        int target = Math.max(authoredMax, Math.min(100, highest + roleBonus));
        if (target <= authoredMax) return new DynamicTrainerLevelService.ScalingResult(source, authoredMax, authoredMax, false);
        int delta = target - authoredMax;
        ArrayList<String> scaled = new ArrayList<>(source.size());
        for (String spec : source) scaled.add(shiftLevel(spec, delta, target));
        return new DynamicTrainerLevelService.ScalingResult(List.copyOf(scaled), authoredMax, target, true);
    }

    private static int maxLevel(List<String> team) {
        int max = 0;
        for (String spec : team) {
            Matcher m = LEVEL.matcher(spec == null ? "" : spec);
            if (m.find()) max = Math.max(max, Integer.parseInt(m.group(1)));
        }
        return max;
    }

    private static String shiftLevel(String spec, int delta, int targetMax) {
        if (spec == null) return "";
        Matcher m = LEVEL.matcher(spec);
        if (!m.find()) return spec;
        int original = Integer.parseInt(m.group(1));
        return m.replaceFirst("level=" + Math.min(targetMax, Math.min(100, original + delta)));
    }
}
