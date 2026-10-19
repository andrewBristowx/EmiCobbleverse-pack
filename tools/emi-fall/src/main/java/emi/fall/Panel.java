package emi.fall;

import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreResetS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * El marcador de la derecha de la pantalla (la «sidebar» de Minecraft): ronda y objetivo, el top de puntos del torneo y lo que llevas en esta ronda. Se manda por paquetes directamente a
 * cada participante con un marcador PRIVADO (no toca el marcador del servidor, asi no lo ve nadie mas ni choca con otros mods); cada linea es un texto libre sin numero.
 */
final class Panel {
    private Panel() {}

    private static final String OBJ = "emi_fall_panel";
    private static final Scoreboard SB = new Scoreboard();
    private static final ScoreboardObjective OBJETIVO = SB.addObjective(OBJ, ScoreboardCriterion.DUMMY, Text.literal("EMI FALL"), ScoreboardCriterion.RenderType.INTEGER, false, BlankNumberFormat.INSTANCE);
    private static final int MAX = 15;

    private static final class Est { List<Text> lineas = new ArrayList<>(); String titulo = ""; }
    private static final Map<UUID, Est> ESTADO = new HashMap<>();

    /** Pone el marcador del jugador con esas lineas (de arriba a abajo; hasta 15) y ese titulo; solo manda lo que ha cambiado. */
    static void mostrar(ServerPlayerEntity p, Text titulo, List<Text> lineas) {
        Est e = ESTADO.get(p.getUuid());
        if (e == null) {
            e = new Est();
            ESTADO.put(p.getUuid(), e);
            p.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(OBJETIVO, ScoreboardObjectiveUpdateS2CPacket.ADD_MODE));
            p.networkHandler.sendPacket(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, OBJETIVO));
        }
        String t = titulo.getString();
        if (!t.equals(e.titulo)) {
            e.titulo = t;
            OBJETIVO.setDisplayName(titulo);
            p.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(OBJETIVO, ScoreboardObjectiveUpdateS2CPacket.UPDATE_MODE));
        }
        int n = Math.min(MAX, lineas.size());
        for (int i = 0; i < n; i++) {
            Text tx = lineas.get(i);
            if (i < e.lineas.size() && e.lineas.get(i).equals(tx)) continue;
            if (i < e.lineas.size()) e.lineas.set(i, tx); else e.lineas.add(tx);
            p.networkHandler.sendPacket(new ScoreboardScoreUpdateS2CPacket("l" + i, OBJ, MAX - i, Optional.of(tx), Optional.of(BlankNumberFormat.INSTANCE)));
        }
        while (e.lineas.size() > n) {
            int i = e.lineas.size() - 1;
            e.lineas.remove(i);
            p.networkHandler.sendPacket(new ScoreboardScoreResetS2CPacket("l" + i, OBJ));
        }
    }

    /** Quita el marcador de ese jugador (al acabar el torneo o irse). */
    static void quitar(ServerPlayerEntity p) {
        if (p == null || ESTADO.remove(p.getUuid()) == null) return;
        p.networkHandler.sendPacket(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, null));
        p.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(OBJETIVO, ScoreboardObjectiveUpdateS2CPacket.REMOVE_MODE));
    }

    static boolean tiene(UUID id) { return ESTADO.containsKey(id); }

    // ------------------------------------------------------------------------------------------------ el contenido

    static Text l(String s, Formatting... f) { return Text.literal(s).formatted(f); }

    private static final Formatting[] PUESTO = {Formatting.GOLD, Formatting.WHITE, Formatting.YELLOW};

    /** El contenido para ese jugador segun la fase del torneo. */
    static void actualizar(Torneo q, ServerPlayerEntity p, Jug yo) {
        List<Text> out = new ArrayList<>();
        Text titulo = Text.literal("★ EMI FALL ★").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
        if (q.fase == Torneo.Fase.COLA) {
            out.add(l("Torneo en preparación", Formatting.GOLD));
            out.add(l(q.sinElim ? "Sin eliminaciones" : "Con eliminaciones", Formatting.GRAY));
            out.add(l(" "));
            out.add(l("APUNTADOS (" + q.jugs.size() + ")", Formatting.LIGHT_PURPLE, Formatting.BOLD));
            int k = 0;
            for (Jug j : q.jugs.values()) { if (k++ >= 9) { out.add(l("  …", Formatting.GRAY)); break; } out.add(l("  " + j.nombre + (j.id.equals(q.anfitrion) ? " ★" : ""), Formatting.WHITE)); }
            mostrar(p, titulo, out);
            return;
        }
        if (q.mini != null) {
            out.add(l("Ronda " + (q.idx + 1) + "/" + q.rondas.size() + " · " + q.mini.nombre(), Formatting.GOLD, Formatting.BOLD));
            String ob = q.mini.objetivo();
            if (!ob.isEmpty()) out.add(l(ob, Formatting.YELLOW));
        } else {
            out.add(l("¡Fin del torneo!", Formatting.GOLD, Formatting.BOLD));
        }
        out.add(l(q.sinElim ? "Sin eliminaciones" : "Con eliminaciones", Formatting.DARK_GRAY));
        out.add(l(" "));
        out.add(l("TOP DEL TORNEO", Formatting.LIGHT_PURPLE, Formatting.BOLD));
        List<Jug> top = q.clasificacionTotal();
        int sitios = (q.fase == Torneo.Fase.JUEGO && q.mini != null && q.mini.vivo(q.ctx, yo) >= 0) ? 5 : 7;
        for (int i = 0; i < top.size() && i < sitios; i++) {
            Jug j = top.get(i);
            Formatting col = i < 3 ? PUESTO[i] : Formatting.GRAY;
            Text linea = Text.literal((i + 1) + ". ").formatted(col).append(Text.literal(j.nombre).formatted(j == yo ? Formatting.AQUA : col)).append(Text.literal("  " + j.puntos).formatted(Formatting.YELLOW, Formatting.BOLD));
            out.add(linea);
        }
        if (top.size() > sitios) {
            int pos = top.indexOf(yo);
            if (pos >= sitios) out.add(Text.literal((pos + 1) + ". ").formatted(Formatting.GRAY).append(Text.literal(yo.nombre).formatted(Formatting.AQUA)).append(Text.literal("  " + yo.puntos).formatted(Formatting.YELLOW, Formatting.BOLD)));
        }
        if (q.fase == Torneo.Fase.JUEGO && q.mini != null && q.ctx != null && q.mini.vivo(q.ctx, yo) >= 0) {
            out.add(l(" "));
            out.add(l("ESTA RONDA · " + q.mini.unidad(), Formatting.GREEN, Formatting.BOLD));
            List<Jug> l = new ArrayList<>(q.jugs.values());
            l.sort((a, b) -> Integer.compare(q.mini.vivo(q.ctx, b), q.mini.vivo(q.ctx, a)));
            for (int i = 0; i < l.size() && i < 3; i++) {
                Jug j = l.get(i);
                out.add(Text.literal("  " + j.nombre).formatted(j == yo ? Formatting.AQUA : Formatting.WHITE).append(Text.literal("  " + q.mini.vivo(q.ctx, j)).formatted(Formatting.GREEN, Formatting.BOLD)));
            }
            int pos = l.indexOf(yo);
            if (pos >= 3) out.add(Text.literal("  " + yo.nombre).formatted(Formatting.AQUA).append(Text.literal("  " + q.mini.vivo(q.ctx, yo)).formatted(Formatting.GREEN, Formatting.BOLD)));
        } else if (q.fase == Torneo.Fase.RESULTADO) {
            out.add(l(" "));
            out.add(l("Has hecho +" + yo.puntosRonda + " (puesto " + yo.puestoRonda + "º)", Formatting.GREEN));
        }
        mostrar(p, titulo, out);
    }
}
