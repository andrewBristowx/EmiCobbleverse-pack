package com.emipokemon.npc;

import java.util.Locale;

public final class ServiceNpcEntity {
    public enum NpcKind {
        NURSE;

        public static String safeCategory(String category) {
            String normalized;
            if (category == null || category.isBlank()) {
                return "balls";
            }
            return switch (normalized = category.trim().toLowerCase(Locale.ROOT)) {
                case "balls", "medicine", "battle", "evolution", "supplies", "special_balls", "special_evolution", "protections", "gacha",
                     "tm_moves", "egg_moves", "star_moves", "tutor_moves" -> normalized;
                default -> "balls";
            };
        }
    }
}
