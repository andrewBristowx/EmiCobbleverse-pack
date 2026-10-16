package emi.survivors.client;

import emi.survivors.net.EstadoPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Interfaz minima de la fase 0: barra de vida, tiempo, enemigos y derrotados, y (para la prueba) las medidas de rendimiento. */
@Environment(EnvType.CLIENT)
public final class SurvivorsHud {
    private SurvivorsHud() {}

    public static void render(DrawContext g, RenderTickCounter tick) {
        EstadoPayload e = EmiSurvivorsClient.estado();
        if (e == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int x = 10, y = 10, w = 170;
        float f = e.hpMax() <= 0 ? 0f : Math.max(0f, Math.min(1f, e.hp() / (float) e.hpMax()));
        int color = f > 0.5f ? 0xFF3FD35A : f > 0.25f ? 0xFFE6B422 : 0xFFE0433A;
        g.fill(x - 2, y - 2, x + w + 2, y + 12, 0xC0101018);
        g.fill(x, y, x + (int) (w * f), y + 10, color);
        g.drawTextWithShadow(tr, "Vida " + e.hp() + "/" + e.hpMax(), x + 4, y + 1, 0xFFFFFFFF);
        String t = (e.segundos() / 60) + ":" + String.format("%02d", e.segundos() % 60);
        g.drawTextWithShadow(tr, "Tiempo " + t + "    Enemigos " + e.enemigos() + "    Derrotados " + e.derrotados(), x, y + 18, 0xFFFFFFFF);
        g.drawTextWithShadow(tr, String.format(java.util.Locale.ROOT, "Servidor %.1f ms/tick · mod %.2f ms · FPS %d", e.msptServidor(), e.msNuestro(), mc.getCurrentFps()), x, y + 30, 0xFFB8B8C8);
    }
}
