package emi.survivors.client;

import emi.survivors.Catalogo;
import emi.survivors.net.EstadoPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** La interfaz de la partida: vida, experiencia y nivel, tiempo y derrotados, la barra del jefe, los ataques y mejoras que llevas, y (solo para probar) las medidas de rendimiento. */
@Environment(EnvType.CLIENT)
public final class SurvivorsHud {
    private SurvivorsHud() {}

    private static void barra(DrawContext g, int x, int y, int w, int h, float f, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xC0101018);
        g.fill(x, y, x + (int) (w * Math.max(0f, Math.min(1f, f))), y + h, color);
    }

    public static void render(DrawContext g, RenderTickCounter tick) {
        EstadoPayload e = EmiSurvivorsClient.estado();
        if (e == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int x = 8, y = 8, w = 150;
        float f = e.hpMax() <= 0 ? 0f : e.hp() / (float) e.hpMax();
        barra(g, x, y, w, 10, f, f > 0.5f ? 0xFF3FD35A : f > 0.25f ? 0xFFE6B422 : 0xFFE0433A);
        g.drawTextWithShadow(tr, "Vida " + e.hp() + "/" + e.hpMax(), x + 4, y + 1, 0xFFFFFFFF);
        barra(g, x, y + 14, w, 6, e.xpSig() <= 0 ? 0f : e.xp() / (float) e.xpSig(), 0xFF5BC8F5);
        g.drawTextWithShadow(tr, "Nv. " + e.nivel(), x + w + 6, y + 13, 0xFFFFE070);
        String t = (e.segundos() / 60) + ":" + String.format("%02d", e.segundos() % 60);
        g.drawTextWithShadow(tr, t + "    Derrotados " + e.derrotados(), x, y + 25, 0xFFFFFFFF);

        // ataques (icono, nombre y puntos de nivel) y mejoras pasivas (solo icono y nivel)
        int ay = y + 40;
        if (!e.armas().isEmpty()) for (String s : e.armas().split(",")) {
            String[] p = s.split(":", 3);
            if (p.length < 3) continue;
            Catalogo.Arma a = Catalogo.arma(p[0]);
            int n = Integer.parseInt(p[1]);
            boolean evo = n >= Catalogo.NIVEL_EVO;                       // evolucionado: nombre e icono nuevos, nombre dorado y una estrella
            if (a != null) Lienzo.icono(g, Catalogo.icono(a, n), x, ay, 1f);
            g.drawTextWithShadow(tr, p[2], x + 19, ay + 1, evo ? 0xFFFFD34E : 0xFFFFFFFF);
            for (int k = 0; k < Catalogo.MAX_NIVEL; k++) g.fill(x + 19 + k * 5, ay + 11, x + 22 + k * 5, ay + 14, k < n ? 0xFFFFD34E : 0xFF555566);
            if (evo) g.drawTextWithShadow(tr, "★", x + 19 + Catalogo.MAX_NIVEL * 5 + 1, ay + 8, 0xFFFFE070);
            ay += 18;
        }
        int px = x;
        if (!e.pasivas().isEmpty()) for (String s : e.pasivas().split(",")) {
            String[] p = s.split(":");
            if (p.length < 2) continue;
            Catalogo.Pasiva a = Catalogo.pasiva(p[0]);
            if (a != null) Lienzo.icono(g, a.icono(), px, ay + 2, 0.75f);
            g.drawTextWithShadow(tr, p[1], px + 9, ay + 8, 0xFFFFD34E);
            px += 22;
        }

        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();

        // el jefe
        if (!e.jefe().isEmpty() && e.jefeMax() > 0) {
            int bw = 220, bx = (sw - bw) / 2, by = 12;
            g.drawCenteredTextWithShadow(tr, e.jefe(), sw / 2, by - 11, 0xFFFFB6F0);
            barra(g, bx, by, bw, 8, e.jefeHp() / (float) e.jefeMax(), 0xFFC03AB8);
        }

        // la marea en marcha (debajo de la barra del jefe, o arriba si no hay jefe): su titulo y lo que le queda
        if (!e.evento().isEmpty()) {
            int ey = (!e.jefe().isEmpty() && e.jefeMax() > 0) ? 26 : 12;
            String txt = e.evento() + (e.eventoSeg() > 0 ? "  " + (e.eventoSeg() / 60) + ":" + String.format("%02d", e.eventoSeg() % 60) : "");
            g.drawCenteredTextWithShadow(tr, txt, sw / 2, ey, 0xFFFFD34E);
        }

        // los companeros de la partida (arriba a la derecha): nombre, vida y si estan caidos o eligiendo carta
        if (!e.companeros().isEmpty()) {
            int cx = sw - 112, cy = 8;
            for (String c : e.companeros().split(";")) {
                String[] q = c.split("\\|");
                if (q.length < 4) continue;
                int hp, max, est;
                try { hp = Integer.parseInt(q[1]); max = Integer.parseInt(q[2]); est = Integer.parseInt(q[3]); } catch (NumberFormatException ex) { continue; }
                String nom = q[0].length() > 14 ? q[0].substring(0, 13) + "…" : q[0];
                g.drawTextWithShadow(tr, nom, cx, cy, est == 1 ? 0xFFFF8A80 : 0xFFFFFFFF);
                if (est == 1) g.drawTextWithShadow(tr, "CAÍDO", cx + 70, cy, 0xFFFF5555);
                else if (est == 2) g.drawTextWithShadow(tr, "elige…", cx + 70, cy, 0xFFB8B8C8);
                float fc = est == 1 || max <= 0 ? 0f : hp / (float) max;
                barra(g, cx, cy + 11, 100, 5, fc, fc > 0.5f ? 0xFF3FD35A : fc > 0.25f ? 0xFFE6B422 : 0xFFE0433A);
                cy += 24;
            }
        }

        // si estas caido: aviso grande y lo que falta para que te revivan
        if (e.caido()) {
            g.fill(0, 0, sw, sh, 0x40A00000);
            var m = g.getMatrices();
            m.push(); m.translate(sw / 2f, sh / 2f - 34, 0); m.scale(2.2f, 2.2f, 1f);
            g.drawCenteredTextWithShadow(tr, "¡ESTÁS CAÍDO!", 0, 0, 0xFFFF5555);
            m.pop();
            g.drawCenteredTextWithShadow(tr, e.companeros().isEmpty() ? "Esperando..." : "Un compañero puede revivirte si se queda a tu lado unos segundos", sw / 2, sh / 2 - 8, 0xFFFFFFFF);
            int bw = 160, bx = (sw - bw) / 2, by = sh / 2 + 8;
            barra(g, bx, by, bw, 8, e.reviveProg() / 100f, 0xFF6FE08A);
            if (e.reviveProg() > 0) g.drawCenteredTextWithShadow(tr, "Reviviendo… " + e.reviveProg() + "%", sw / 2, by + 12, 0xFF9BF0B0);
        }
        if (Boolean.getBoolean("emi.survivors.debug"))
            g.drawTextWithShadow(tr, String.format(java.util.Locale.ROOT, "Enemigos %d · gemas %d · servidor %.1f ms/tick · mod %.2f ms · FPS %d", e.enemigos(), GemasCliente.cuantas(), e.msptServidor(), e.msNuestro(), mc.getCurrentFps()),
                    x, mc.getWindow().getScaledHeight() - 12, 0xFFB8B8C8);
    }
}
