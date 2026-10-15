package emi.huida.client;

import emi.huida.net.HudPayload;
import emi.huida.net.MapaPayload;
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
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Interfaz de la huida (al estilo de la de las carreras): arriba a la izquierda las michiestrellas (puntos y total); arriba en el centro lo cerca que
 * esta el Tyrantrum; abajo a la derecha el minimapa del recorrido con tu cabeza y la del Tyrantrum, y debajo tu puesto; debajo de las estrellas el tiempo y los errores.
 */
@Environment(EnvType.CLIENT)
public final class HuidaHud {
    private static final int FONDO = 0xB0101018, BORDE = 0xFF3A3A55;
    private static final int ALTO_VIRTUAL = 300;
    private static final int MW = 144, MH = 100;      // caja del minimapa

    private HuidaHud() {}

    // el dibujo del recorrido se calcula una vez por cada mapa recibido
    private static MapaPayload mapaCalculado;
    private static int[] px, py;

    private static ItemStack iconoEstrella;
    /** Retrato redondo del Tyrantrum (recortado de su modelo en el juego), 64x64 con filtro suave (.mcmeta). */
    private static final Identifier ICONO_TYRANTRUM = Identifier.of("emi_huida", "textures/gui/tyrantrum.png");
    /** Retrato del Pumpkaboo (nivel de la calabaza), igual que el del Tyrantrum. */
    private static final Identifier ICONO_PUMPKABOO = Identifier.of("emi_huida", "textures/gui/pumpkaboo.png");

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!CamaraHuida.activa() || mc.options.hudHidden || mc.player == null) return;
        HudPayload h = EmiHuidaClient.hud();
        if (h == null) return;
        try {
            // se dibuja en un espacio virtual de 300 de alto y se escala a la pantalla: ocupa la misma fraccion con cualquier escala de interfaz
            float esc = ctx.getScaledWindowHeight() / (float) ALTO_VIRTUAL;
            int ancho = Math.round(ctx.getScaledWindowWidth() / esc), alto = ALTO_VIRTUAL;
            MapaPayload mp = EmiHuidaClient.mapa;
            MatrixStack m = ctx.getMatrices();
            m.push();
            m.scale(esc, esc, 1f);
            try {
                estrellas(ctx, mc, h, mp, 6, 6);
                peligro(ctx, mc, h, ancho);
                minimapa(ctx, mc, h, mp, ancho, alto);
                puesto(ctx, mc, h, ancho, alto);
                tiempo(ctx, mc, h);
            } finally { m.pop(); }
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

    private static void icono(DrawContext ctx, ItemStack st, float x, float y, float k) {
        MatrixStack m = ctx.getMatrices();
        m.push(); m.translate(x, y, 0); m.scale(k, k, 1f);
        ctx.drawItem(st, 0, 0);
        m.pop();
    }

    private static ItemStack estrella() {
        if (iconoEstrella == null) {
            iconoEstrella = new ItemStack(Items.NETHER_STAR);          // con el modelo de la michiestrella de emi-carreras (custom_model_data 7020002); sin el, una estrella del Nether
            iconoEstrella.set(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA, new net.minecraft.component.type.CustomModelDataComponent(7020002));
        }
        return iconoEstrella;
    }

    static String tiempo(int ticks) {
        int ms = ticks * 50, m = ms / 60000, s = (ms / 1000) % 60, d = (ms % 1000) / 100;
        return m + ":" + (s < 10 ? "0" : "") + s + "." + d;
    }

    private static int colorPuesto(int puesto) { return puesto == 1 ? 0xFFD700 : puesto == 2 ? 0xDDDDDD : puesto == 3 ? 0xCD7F32 : 0xFFFFFF; }

    // ------------------------------------------------------------ michiestrellas (arriba a la izquierda)

    private static void estrellas(DrawContext ctx, MinecraftClient mc, HudPayload h, MapaPayload mp, int x, int y) {
        TextRenderer tr = mc.textRenderer;
        int total = mp == null ? 0 : Math.max(1, mp.puntosTotal());
        int w = 124, alto = 31, n = total == 0 ? h.puntos() : Math.min(h.puntos(), total);
        caja(ctx, x, y, w, alto, total > 0 && n >= total ? 0xFFFFD700 : BORDE);
        icono(ctx, estrella(), x + 4, y + 3, 1.15f);
        String txt = h.puntos() + (total > 0 ? " / " + total : "");
        MatrixStack m = ctx.getMatrices();
        m.push(); m.translate(x + 26, y + 4, 0); m.scale(1.3f, 1.3f, 1f);
        ctx.drawText(tr, Text.literal(txt).formatted(Formatting.BOLD), 0, 0, 0xFFD700, true);
        m.pop();
        if (total > 0) {
            int bx = x + 26, bw = x + w - 5 - bx, by = y + 20;
            ctx.fill(bx, by, bx + bw, by + 5, 0xFF22222E);
            int lleno = bw * n / total;
            ctx.fill(bx, by, bx + lleno, by + 5, 0xFFFFC21A);
            ctx.fill(bx, by, bx + lleno, by + 1, 0xFFFFE97A);
        }
        if (h.jugadores() > 1 && h.lider() > h.puntos())
            ctx.drawText(tr, Text.literal("Primero: " + h.lider()).formatted(Formatting.GRAY), x + 2, y + alto + 3, 0xFFFFFF, true);
    }

    // ------------------------------------------------------------ cerca del Tyrantrum (arriba, en el centro)

    private static void peligro(DrawContext ctx, MinecraftClient mc, HudPayload h, int ancho) {
        if (h.fin() != 0) return;
        TextRenderer tr = mc.textRenderer;
        double dist = Math.max(0, h.s() - h.sCh() - h.captura());      // de la boca del perseguidor (Tyrantrum) o de su borde (Pumpkaboo) a ti
        int w = 170, alto = 15, x = (ancho - w) / 2, y = 6;
        int col = dist < 2 ? 0xFFEE3B3B : dist < 8 ? 0xFFF0A020 : 0xFF45C060;
        caja(ctx, x, y, w, alto, dist < 2 ? 0xFFEE3B3B : BORDE);
        marcadorTyrantrum(ctx, tr, x + 9, y + 7, 10, h.nivel());
        int bx = x + 22, bw = w - 26;
        ctx.fill(bx, y + 9, bx + bw, y + 12, 0xFF22222E);
        int lleno = (int) Math.round(bw * Math.max(0.04, Math.min(1.0, dist / 20.0)));
        ctx.fill(bx, y + 9, bx + lleno, y + 12, col);
        String t = (h.nivel() == 1 ? "Pumpkaboo" : "Tyrantrum") + " a " + (int) dist + " m";
        ctx.drawText(tr, t, bx, y + 1, dist < 2 ? 0xFF8888 : 0xFFFFFF, true);
    }

    // ------------------------------------------------------------ minimapa (abajo a la derecha)

    private static void calcular(MapaPayload mp) {
        if (mp == mapaCalculado) return;
        mapaCalculado = mp;
        int n = mp.pts().length / 2;
        px = new int[n]; py = new int[n];
        float minX = 1e9f, maxX = -1e9f, minZ = 1e9f, maxZ = -1e9f;
        for (int i = 0; i < n; i++) { float x = mp.pts()[i * 2], z = mp.pts()[i * 2 + 1]; minX = Math.min(minX, x); maxX = Math.max(maxX, x); minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z); }
        float bw = Math.max(1, maxX - minX), bh = Math.max(1, maxZ - minZ);
        float k = Math.min((MW - 16) / bw, (MH - 16) / bh);
        float ox = (MW - bw * k) / 2f, oy = (MH - bh * k) / 2f;
        for (int i = 0; i < n; i++) {
            px[i] = Math.round(ox + (mp.pts()[i * 2] - minX) * k);
            py[i] = Math.round(oy + (mp.pts()[i * 2 + 1] - minZ) * k);
        }
    }

    /** Punto del recorrido a distancia s (coordenadas dentro de la caja del minimapa). */
    private static int[] punto(MapaPayload mp, double s) {
        int n = px.length;
        double f = Math.max(0, Math.min(n - 1.0001, s / mp.paso()));
        int i = (int) f; double t = f - i;
        return new int[]{(int) Math.round(px[i] + (px[i + 1] - px[i]) * t), (int) Math.round(py[i] + (py[i + 1] - py[i]) * t)};
    }

    /** El retrato del Tyrantrum centrado en (cx, cy), de {@code 2*r} de lado. */
    private static void marcadorTyrantrum(DrawContext ctx, TextRenderer tr, int cx, int cy, int r, int nivel) {
        ctx.drawTexture(nivel == 1 ? ICONO_PUMPKABOO : ICONO_TYRANTRUM, cx - r, cy - r, 2 * r, 2 * r, 0f, 0f, 64, 64, 64, 64);
    }

    private static void minimapa(DrawContext ctx, MinecraftClient mc, HudPayload h, MapaPayload mp, int ancho, int alto) {
        if (mp == null || mp.pts().length < 4) return;
        calcular(mp);
        int x = ancho - MW - 8, y = alto - MH - 52;
        caja(ctx, x, y, MW, MH);
        int visto = (int) (Math.max(0, h.s()) / mp.paso());
        for (int i = 0; i + 1 < px.length; i++) {
            int x0 = x + px[i], y0 = y + py[i], dx = px[i + 1] - px[i], dy = py[i + 1] - py[i];
            int pasos = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));
            int col = i < visto ? 0xFF8A7A3A : 0xFFE6E6F2;                       // lo ya recorrido en dorado apagado, lo que queda claro
            for (int k = 0; k <= pasos; k++) {
                int cx = x0 + dx * k / pasos, cy = y0 + dy * k / pasos;
                ctx.fill(cx, cy, cx + 2, cy + 2, col);
            }
        }
        int[] meta = punto(mp, mp.meta());
        ctx.fill(x + meta[0] - 3, y + meta[1] - 3, x + meta[0] + 5, y + meta[1] + 5, 0xFF1F8A3A);
        ctx.fill(x + meta[0] - 2, y + meta[1] - 2, x + meta[0] + 4, y + meta[1] + 4, 0xFFFFD700);
        // el Tyrantrum (cabeza de dragon sobre fondo rojo) y, encima, tu cabeza
        int[] t = punto(mp, Math.max(0, h.sCh()));
        int[] yo = punto(mp, Math.max(0, h.s()));
        // si estan tan cerca que se tapan (el mapa es de todo el recorrido), el retrato se aparta un poco hacia donde esta el Tyrantrum
        double ddx = t[0] - yo[0], ddy = t[1] - yo[1], dd = Math.hypot(ddx, ddy);
        if (dd < 14) {
            double ux, uy;
            if (dd < 0.5) { int[] a = punto(mp, Math.max(0, h.s() - 12)); ux = a[0] - yo[0]; uy = a[1] - yo[1]; double l = Math.hypot(ux, uy); if (l < 0.5) { ux = -1; uy = 0; } else { ux /= l; uy /= l; } }
            else { ux = ddx / dd; uy = ddy / dd; }
            t[0] = yo[0] + (int) Math.round(ux * 14); t[1] = yo[1] + (int) Math.round(uy * 14);
        }
        marcadorTyrantrum(ctx, mc.textRenderer, x + t[0], y + t[1], 8, h.nivel());
        ctx.fill(x + yo[0] - 5, y + yo[1] - 5, x + yo[0] + 5, y + yo[1] + 5, 0xFFFFFFFF);
        cabeza(ctx, mc, mc.player.getUuid(), x + yo[0] - 4, y + yo[1] - 4, 8);
    }

    // ------------------------------------------------------------ puesto en grande (bajo el minimapa) y tiempo/errores (abajo a la izquierda)

    private static void puesto(DrawContext ctx, MinecraftClient mc, HudPayload h, int ancho, int alto) {
        TextRenderer tr = mc.textRenderer;
        String n = String.valueOf(h.puesto());
        String tot = "/" + h.jugadores();
        int col = 0xFF000000 | colorPuesto(h.puesto());
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
        ctx.drawText(tr, "puesto", x - tr.getWidth("puesto") - 5, y + 14, 0xAAAAAA, true);
    }

    private static void tiempo(DrawContext ctx, MinecraftClient mc, HudPayload h) {
        TextRenderer tr = mc.textRenderer;
        int x = 6, y = 52, w = 124, hh = 22;
        int max = h.maxErrores();
        boolean ultimo = max > 0 && h.errores() >= max - 1 && h.errores() > 0;      // un fallo mas y te come
        caja(ctx, x, y, w, hh, h.errores() == 0 ? BORDE : ultimo ? 0xFFE03030 : 0xFF8A3A3A);
        MatrixStack m = ctx.getMatrices();
        m.push(); m.translate(x + 6, y + 6, 0); m.scale(1.3f, 1.3f, 1f);
        ctx.drawText(tr, tiempo(h.ticks()), 0, 0, 0xFFFFFF, true);
        m.pop();
        String e = "Errores " + h.errores() + (max >= 0 ? "/" + max : "");
        ctx.drawText(tr, e, x + w - 6 - tr.getWidth(e), y + 7, h.errores() == 0 ? 0x88DD88 : ultimo ? 0xFF5555 : 0xFF8888, true);
    }
}
