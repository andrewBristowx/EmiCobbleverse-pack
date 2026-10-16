package emi.survivors;

import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Contorno de color para los enemigos con personalidad: se les pone el brillo de Minecraft y se los mete en un equipo del marcador con ese color (el color del brillo
 * de una entidad es el de su equipo). Los equipos se llaman {@code emi_surv_<color>}; al quitar al enemigo se le saca del equipo (si no, el marcador del mundo iba
 * guardando los identificadores de miles de enemigos ya desaparecidos) y al arrancar el servidor se borran todos por si quedo alguno de una caida.
 */
final class Colores {
    private Colores() {}

    private static final String PREFIJO = "emi_surv_";

    static void marcar(Enemigo en, String color) {
        MinecraftServer s = Partida.server;
        if (s == null || en.e == null) return;
        try {
            Scoreboard sb = s.getScoreboard();
            Team team = sb.getTeam(PREFIJO + color);
            if (team == null) {
                team = sb.addTeam(PREFIJO + color);
                Formatting f = Formatting.byName(color);
                team.setColor(f == null ? Formatting.WHITE : f);
                team.setCollisionRule(AbstractTeam.CollisionRule.NEVER);
            }
            sb.addScoreHolderToTeam(en.e.getUuidAsString(), team);
            en.e.setGlowing(true);
            en.colorEquipo = color;
        } catch (Throwable t) {
            EmiSurvivors.LOG.warn("No se pudo poner el contorno de color {}", color, t);
        }
    }

    static void quitar(Enemigo en) {
        MinecraftServer s = Partida.server;
        if (s == null || en.colorEquipo == null || en.e == null) return;
        try {
            Scoreboard sb = s.getScoreboard();
            Team team = sb.getTeam(PREFIJO + en.colorEquipo);
            if (team != null) sb.removeScoreHolderFromTeam(en.e.getUuidAsString(), team);
        } catch (Throwable t) { /* nada */ }
        en.colorEquipo = null;
    }

    /** Borra todos los equipos de colores (al arrancar el servidor). */
    static void limpiarTodos(MinecraftServer s) {
        try {
            Scoreboard sb = s.getScoreboard();
            for (Team t : new ArrayList<>(sb.getTeams())) if (t.getName().startsWith(PREFIJO)) sb.removeTeam(t);
        } catch (Throwable t) { /* nada */ }
    }
}
