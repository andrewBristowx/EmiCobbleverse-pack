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
            if (a != null) Lienzo.icono(g, a.icono(), x, ay, 1f);
            g.drawTextWithShadow(tr, p[2], x + 19, ay + 1, 0xFFFFFFFF);
            for (int k = 0; k < Catalogo.MAX_NIVEL; k++) g.fill(x + 19 + k * 5, ay + 11, x + 22 + k * 5, ay + 14, k < n ? 0xFFFFD34E : 0xFF555566);
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

        // el jefe
        if (!e.jefe().isEmpty() && e.jefeMax() > 0) {
            int sw = mc.getWindow().getScaledWidth(), bw = 220, bx = (sw - bw) / 2, by = 12;
            g.drawCenteredTextWithShadow(tr, e.jefe(), sw / 2, by - 11, 0xFFFFB6F0);
            barra(g, bx, by, bw, 8, e.jefeHp() / (float) e.jefeMax(), 0xFFC03AB8);
        }
        if (Boolean.getBoolean("emi.survivors.debug"))
            g.drawTextWithShadow(tr, String.format(java.util.Locale.ROOT, "Enemigos %d · gemas %d · servidor %.1f ms/tick · mod %.2f ms · FPS %d", e.enemigos(), GemasCliente.cuantas(), e.msptServidor(), e.msNuestro(), mc.getCurrentFps()),
                    x, mc.getWindow().getScaledHeight() - 12, 0xFFB8B8C8);
    }
}
