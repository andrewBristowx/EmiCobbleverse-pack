package emi.carreras.client;

import emi.carreras.net.EstadoPayload;
import emi.carreras.net.PoderPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Dibuja la interfaz de la carrera: posiciones (arriba a la derecha), mapa con cabezas (debajo) y el poder (tarjeta al cogerlo y recordatorio abajo). */
@Environment(EnvType.CLIENT)
public final class CarrerasHud {
    private static final int FONDO = 0xB0101018, BORDE = 0xFF3A3A55;

    private CarrerasHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!EmiCarrerasClient.activo() || mc.options.hudHidden || mc.player == null) return;
        EstadoPayload e = EmiCarrerasClient.estado;
        if (e == null) return;
        try {
            int ancho = ctx.getScaledWindowWidth(), alto = ctx.getScaledWindowHeight();
            int y = posiciones(ctx, mc, e, ancho);
            mapa(ctx, mc, e, ancho, y + 4);
            poder(ctx, mc, ancho, alto);
        } catch (Throwable t) { /* una interfaz que falla no debe romper el dibujado del juego */ }
    }

    // ------------------------------------------------------------ posiciones

    private static void caja(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, FONDO);
        ctx.fill(x, y, x + w, y + 1, BORDE); ctx.fill(x, y + h - 1, x + w, y + h, BORDE);
        ctx.fill(x, y, x + 1, y + h, BORDE); ctx.fill(x + w - 1, y, x + w, y + h, BORDE);
    }

    private static void cabeza(DrawContext ctx, MinecraftClient mc, UUID id, int x, int y, int size) {
        PlayerListEntry le = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(id);
        PlayerSkinDrawer.draw(ctx, le != null ? le.getSkinTextures() : DefaultSkinHelper.getSkinTextures(id), x, y, size);
    }

    static String tiempo(int ticks) {
        int ms = ticks * 50, m = ms / 60000, s = (ms / 1000) % 60, d = (ms % 1000) / 100;
        return m + ":" + (s < 10 ? "0" : "") + s + "." + d;
    }

    /** Devuelve la y donde acaba el panel. */
    private static int posiciones(DrawContext ctx, MinecraftClient mc, EstadoPayload e, int ancho) {
        TextRenderer tr = mc.textRenderer;
        List<EstadoPayload.Jugador> l = new ArrayList<>(e.jugadores());
        l.sort(Comparator.comparingInt(EstadoPayload.Jugador::puesto));
        int pw = 124, fila = 11, x = ancho - pw - 4, y = 4;
        int ph = 14 + l.size() * fila + 3;
        caja(ctx, x, y, pw, ph);
        ctx.drawText(tr, Text.literal("POSICIONES").formatted(Formatting.GOLD, Formatting.BOLD), x + 4, y + 3, 0xFFFFFF, true);
        String t = tiempo(e.tiempo());
        ctx.drawText(tr, t, x + pw - 4 - tr.getWidth(t), y + 3, 0xFFFFFF, true);
        UUID yo = mc.player.getUuid();
        int fy = y + 14;
        for (EstadoPayload.Jugador j : l) {
            if (j.id().equals(yo)) ctx.fill(x + 2, fy - 1, x + pw - 2, fy + fila - 1, 0x60FFFFFF);
            int col = j.puesto() == 1 ? 0xFFD700 : j.puesto() == 2 ? 0xDDDDDD : j.puesto() == 3 ? 0xCD7F32 : 0xFFFFFF;
            ctx.drawText(tr, String.valueOf(j.puesto()), x + 5, fy + 1, col, true);
            cabeza(ctx, mc, j.id(), x + 15, fy, 8);
            String n = j.nombre().length() > 11 ? j.nombre().substring(0, 11) : j.nombre();
            ctx.drawText(tr, n, x + 26, fy + 1, col, true);
            String v = j.fin() ? "META" : "V" + Math.min(j.vuelta() + 1, Math.max(1, e.vueltas()));
            ctx.drawText(tr, v, x + pw - 5 - tr.getWidth(v), fy + 1, j.fin() ? 0x55FF55 : 0xAAAAAA, true);
            fy += fila;
        }
        return y + ph;
    }

    // ------------------------------------------------------------ mapa

    private static void mapa(DrawContext ctx, MinecraftClient mc, EstadoPayload e, int ancho, int y) {
        if (e.mpx() <= 0 || e.mancho() <= 0) return;
        int mw = 124;
        int mh = Math.round(mw * (e.mlargo() / e.mancho()));
        mh = Math.min(mh, 150);
        mw = Math.round(mh * (e.mancho() / e.mlargo()));
        int x = ancho - 124 - 4 + (124 - mw) / 2;
        caja(ctx, x - 2, y - 2, mw + 4, mh + 4);
        ctx.drawTexture(Identifier.of("emi_carreras", "textures/gui/mapa_" + e.pista() + ".png"), x, y, mw, mh, 0, 0, e.mpx(), e.mpy(), e.mpx(), e.mpy());
        UUID yo = mc.player.getUuid();
        EstadoPayload.Jugador mio = null;
        List<EstadoPayload.Jugador> l = new ArrayList<>(e.jugadores());
        l.sort(Comparator.comparingInt(EstadoPayload.Jugador::puesto).reversed());       // el primero se dibuja encima
        for (EstadoPayload.Jugador j : l) {
            if (j.id().equals(yo)) { mio = j; continue; }
            icono(ctx, mc, e, j, x, y, mw, mh, false);
        }
        if (mio != null) icono(ctx, mc, e, mio, x, y, mw, mh, true);
    }

    private static void icono(DrawContext ctx, MinecraftClient mc, EstadoPayload e, EstadoPayload.Jugador j, int x, int y, int mw, int mh, boolean yo) {
        double u = (j.x() - e.mx()) / e.mancho(), v = (j.z() - e.mz()) / e.mlargo();
        int px = x + (int) Math.round(Math.max(0, Math.min(1, u)) * mw) - 4;
        int py = y + (int) Math.round(Math.max(0, Math.min(1, v)) * mh) - 4;
        ctx.fill(px - 1, py - 1, px + 9, py + 9, yo ? 0xFFFFFFFF : 0xFF101018);
        cabeza(ctx, mc, j.id(), px, py, 8);
    }

    // ------------------------------------------------------------ poder

    private static void poder(DrawContext ctx, MinecraftClient mc, int ancho, int alto) {
        PoderPayload p = EmiCarrerasClient.poder;
        if (p == null) return;
        TextRenderer tr = mc.textRenderer;
        ItemStack st = new ItemStack(Registries.ITEM.get(Identifier.of(p.item())));
        int color = 0xFF000000 | p.color();
        // recordatorio fijo encima de la barra de objetos
        Text nombreB = Text.literal(p.nombre()).styled(s -> s.withColor(p.color()).withBold(true));
        int wn = tr.getWidth(nombreB), wc = tr.getWidth("(clic derecho)");
        int w = 24 + wn + 6 + wc + 8, x = (ancho - w) / 2, y = alto - 62;
        caja(ctx, x, y, w, 22);
        ctx.drawItem(st, x + 4, y + 3);
        ctx.drawText(tr, nombreB, x + 24, y + 7, 0xFFFFFF, true);
        ctx.drawText(tr, "(clic derecho)", x + 24 + wn + 6, y + 7, 0xAAAAAA, true);
        // tarjeta grande al cogerlo
        long dt = System.currentTimeMillis() - EmiCarrerasClient.poderDesde;
        if (!p.nuevo() || dt > 5200) return;
        float a = dt < 250 ? dt / 250f : dt > 4500 ? Math.max(0f, 1f - (dt - 4500) / 700f) : 1f;
        int al = Math.max(4, (int) (a * 255));
        int cw = 260, ch = 74, cx = (ancho - cw) / 2, cy = 34;
        int fondo = ((int) (a * 0xC0) << 24) | 0x101018, borde = (al << 24) | (p.color() & 0xFFFFFF);
        ctx.fill(cx, cy, cx + cw, cy + ch, fondo);
        ctx.fill(cx, cy, cx + cw, cy + 2, borde); ctx.fill(cx, cy + ch - 2, cx + cw, cy + ch, borde);
        ctx.fill(cx, cy, cx + 2, cy + ch, borde); ctx.fill(cx + cw - 2, cy, cx + cw, cy + ch, borde);
        MatrixStack m = ctx.getMatrices();
        m.push(); m.translate(cx + 10, cy + 13, 0); m.scale(2.6f, 2.6f, 1f);
        ctx.drawItem(st, 0, 0);
        m.pop();
        ctx.drawText(tr, "¡PODER CONSEGUIDO!", cx + 62, cy + 7, (al << 24) | 0xAAAAAA, true);
        m.push(); m.translate(cx + 62, cy + 18, 0); m.scale(1.4f, 1.4f, 1f);
        ctx.drawText(tr, Text.literal(p.nombre()).styled(s -> s.withColor(p.color()).withBold(true)), 0, 0, (al << 24) | 0xFFFFFF, true);
        m.pop();
        List<OrderedText> lineas = tr.wrapLines(Text.literal(p.descripcion()), cw - 74);
        int ly = cy + 34;
        for (OrderedText ot : lineas) { if (ly > cy + ch - 24) break; ctx.drawText(tr, ot, cx + 62, ly, (al << 24) | 0xDDDDDD, true); ly += 10; }
        ctx.drawText(tr, "Clic derecho para usarlo", cx + 62, cy + ch - 12, (al << 24) | 0x88FF88, true);
    }
}
