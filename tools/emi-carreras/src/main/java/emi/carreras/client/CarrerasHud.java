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

/**
 * Interfaz de la carrera al estilo Mario Kart: arriba a la izquierda la casilla del poder y debajo los participantes; abajo a la derecha el mapa con las cabezas
 * y tu puesto en grande; abajo a la izquierda la vuelta y el tiempo.
 */
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
            EstadoPayload.Jugador yo = null;
            for (EstadoPayload.Jugador j : e.jugadores()) if (j.id().equals(mc.player.getUuid())) yo = j;
            int y = casillaPoder(ctx, mc, yo);
            participantes(ctx, mc, e, 6, y + 6);
            mapa(ctx, mc, e, ancho, alto);
            puesto(ctx, mc, e, yo, ancho, alto);
            vueltas(ctx, mc, e, yo, alto);
        } catch (Throwable t) { /* una interfaz que falla no debe romper el dibujado del juego */ }
    }

    // ------------------------------------------------------------ utilidades

    private static void caja(DrawContext ctx, int x, int y, int w, int h) { caja(ctx, x, y, w, h, BORDE); }

    private static void caja(DrawContext ctx, int x, int y, int w, int h, int borde) {
        ctx.fill(x, y, x + w, y + h, FONDO);
        ctx.fill(x, y, x + w, y + 1, borde); ctx.fill(x, y + h - 1, x + w, y + h, borde);
        ctx.fill(x, y, x + 1, y + h, borde); ctx.fill(x + w - 1, y, x + w, y + h, borde);
    }

    private static void cabeza(DrawContext ctx, MinecraftClient mc, UUID id, int x, int y, int size) {
        PlayerListEntry le = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(id);
        PlayerSkinDrawer.draw(ctx, le != null ? le.getSkinTextures() : DefaultSkinHelper.getSkinTextures(id), x, y, size);
    }

    static String tiempo(int ticks) {
        int ms = ticks * 50, m = ms / 60000, s = (ms / 1000) % 60, d = (ms % 1000) / 100;
        return m + ":" + (s < 10 ? "0" : "") + s + "." + d;
    }

    private static int colorPuesto(int puesto) { return puesto == 1 ? 0xFFD700 : puesto == 2 ? 0xDDDDDD : puesto == 3 ? 0xCD7F32 : 0xFFFFFF; }

    // ------------------------------------------------------------ casilla del poder (arriba a la izquierda)

    /** Dibuja la casilla y el texto del poder; devuelve la y donde acaba. */
    private static int casillaPoder(DrawContext ctx, MinecraftClient mc, EstadoPayload.Jugador yo) {
        TextRenderer tr = mc.textRenderer;
        PoderPayload p = EmiCarrerasClient.poder;
        int x = 6, y = 6, s = 32;
        long dt = System.currentTimeMillis() - EmiCarrerasClient.poderDesde;
        int borde = p == null ? BORDE : (0xFF000000 | p.color());
        caja(ctx, x, y, s, s, borde);
        if (p == null) {
            ctx.drawText(tr, "?", x + s / 2 - tr.getWidth("?") / 2, y + s / 2 - 4, 0x55FFFFFF, false);
            ctx.drawText(tr, "Sin poder", x + s + 6, y + 12, 0x888899, true);
        } else {
            ItemStack st = new ItemStack(Registries.ITEM.get(Identifier.of(p.item())));
            float k = 1.6f + (p.nuevo() && dt < 450 ? 0.9f * (1f - dt / 450f) : 0f);          // «pop» al cogerlo
            MatrixStack m = ctx.getMatrices();
            m.push(); m.translate(x + s / 2f, y + s / 2f, 0); m.scale(k, k, 1f); m.translate(-8, -8, 0);
            ctx.drawItem(st, 0, 0);
            m.pop();
            ctx.drawText(tr, Text.literal(p.nombre()).styled(f -> f.withColor(p.color()).withBold(true)), x + s + 6, y + 2, 0xFFFFFF, true);
            if (p.nuevo() && dt < 6500) {           // al cogerlo: que hace (un par de lineas)
                List<OrderedText> l = tr.wrapLines(Text.literal(p.descripcion()), 150);
                int ly = y + 13;
                for (int i = 0; i < l.size() && i < 2; i++) { ctx.drawText(tr, l.get(i), x + s + 6, ly, 0xDDDDDD, true); ly += 9; }
            } else {
                ctx.drawText(tr, "Clic derecho para usarlo", x + s + 6, y + 13, 0x88FF88, true);
            }
        }
        if (yo != null && yo.escudo() > 0)
            ctx.drawText(tr, Text.literal("Protegido " + yo.escudo() + " s").formatted(Formatting.GREEN), x + s + 6, y + 23, 0xFFFFFF, true);
        return y + s;
    }

    // ------------------------------------------------------------ participantes (debajo de la casilla)

    private static void participantes(DrawContext ctx, MinecraftClient mc, EstadoPayload e, int x, int y) {
        TextRenderer tr = mc.textRenderer;
        List<EstadoPayload.Jugador> l = new ArrayList<>(e.jugadores());
        l.sort(Comparator.comparingInt(EstadoPayload.Jugador::puesto));
        int pw = 106, fila = 10;
        int ph = l.size() * fila + 5;
        caja(ctx, x, y, pw, ph);
        UUID yo = mc.player.getUuid();
        int fy = y + 3;
        for (EstadoPayload.Jugador j : l) {
            if (j.id().equals(yo)) ctx.fill(x + 2, fy - 1, x + pw - 2, fy + fila - 1, 0x60FFFFFF);
            int col = colorPuesto(j.puesto());
            ctx.drawText(tr, String.valueOf(j.puesto()), x + 5, fy, col, true);
            cabeza(ctx, mc, j.id(), x + 14, fy, 8);
            String n = j.nombre().length() > 10 ? j.nombre().substring(0, 10) : j.nombre();
            ctx.drawText(tr, n, x + 25, fy, col, true);
            String v = j.fin() ? "META" : "V" + Math.min(j.vuelta() + 1, Math.max(1, e.vueltas()));
            ctx.drawText(tr, v, x + pw - 4 - tr.getWidth(v), fy, j.fin() ? 0x55FF55 : 0x999999, true);
            fy += fila;
        }
    }

    // ------------------------------------------------------------ mapa (abajo a la derecha)

    private static void mapa(DrawContext ctx, MinecraftClient mc, EstadoPayload e, int ancho, int alto) {
        if (e.mpx() <= 0 || e.mancho() <= 0) return;
        int mh = Math.min(Math.round(110 * (e.mlargo() / e.mancho())), 130);
        int mw = Math.round(mh * (e.mancho() / e.mlargo()));
        int x = ancho - mw - 8, y = alto - mh - 52;
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

    // ------------------------------------------------------------ puesto en grande (bajo el mapa) y vueltas (abajo a la izquierda)

    private static void puesto(DrawContext ctx, MinecraftClient mc, EstadoPayload e, EstadoPayload.Jugador yo, int ancho, int alto) {
        if (yo == null) return;
        TextRenderer tr = mc.textRenderer;
        String n = String.valueOf(yo.puesto());
        String tot = "/" + e.jugadores().size();
        int col = 0xFF000000 | colorPuesto(yo.puesto());
        float k = 3f;
        int wn = Math.round(tr.getWidth(n) * k), wo = Math.round(tr.getWidth("º") * 1.5f), wt = tr.getWidth(tot);
        int x = ancho - 8 - wn - wo - wt - 6, y = alto - 48;
        MatrixStack m = ctx.getMatrices();
        m.push(); m.translate(x, y, 0); m.scale(k, k, 1f);
        ctx.drawText(tr, Text.literal(n).formatted(Formatting.BOLD), 0, 0, col, true);
        m.pop();
        m.push(); m.translate(x + wn + 1, y + 1, 0); m.scale(1.5f, 1.5f, 1f);
        ctx.drawText(tr, "º", 0, 0, col, true);
        m.pop();
        ctx.drawText(tr, tot, x + wn + wo + 4, y + 14, 0xBBBBBB, true);
    }

    private static void vueltas(DrawContext ctx, MinecraftClient mc, EstadoPayload e, EstadoPayload.Jugador yo, int alto) {
        TextRenderer tr = mc.textRenderer;
        int vuelta = yo == null ? 1 : Math.min(yo.vuelta() + 1, Math.max(1, e.vueltas()));
        boolean fin = yo != null && yo.fin();
        int x = 6, y = alto - 28, w = 112, h = 22;
        caja(ctx, x, y, w, h);
        // banderita de cuadros
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) ctx.fill(x + 5 + i * 3, y + 5 + j * 3, x + 8 + i * 3, y + 8 + j * 3, (i + j) % 2 == 0 ? 0xFFFFFFFF : 0xFF000000);
        ctx.fill(x + 4, y + 4, x + 5, y + 18, 0xFFBBBBBB);
        String v = fin ? "META" : vuelta + "/" + Math.max(1, e.vueltas());
        MatrixStack m = ctx.getMatrices();
        m.push(); m.translate(x + 20, y + 6, 0); m.scale(1.5f, 1.5f, 1f);
        ctx.drawText(tr, v, 0, 0, 0xFFFFFF, true);
        m.pop();
        String t = tiempo(e.tiempo());
        ctx.drawText(tr, t, x + w - 4 - tr.getWidth(t), y + 7, 0xDDDDDD, true);
    }
}
