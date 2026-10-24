package emi.cafe.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emi.cafe.net.AccionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * El libro de recetas de la pared de la cocina: a la izquierda el indice con un dibujo de cada plato, a la derecha la receta del plato elegido (los pasos, las estaciones que usa y si ya
 * sabes hacerlo). «¡MUÉSTRAMELO!» hace que el servidor ilumine con chispas las estaciones, una detras de otra. Los datos los manda el servidor (JSON, {@code Menus.libro}).
 */
@Environment(EnvType.CLIENT)
public final class LibroPantalla extends Lienzo {
    private record Est(String nombre, String item, boolean tiene) {}
    private record Plato(String id, String item, String nombre, int precio, int coste, String receta, boolean tengo, boolean especial, String michi, int nivelLocal, String bloqueo, List<Est> estaciones) {}

    private static final int COLS = 6, FILAS = 6, CELDA = 40, POR_PAG = COLS * FILAS;
    private static final int PAPEL = 0xFFF6E9C9, PAPEL2 = 0xFFE7D4A6, PAPEL3 = 0xFFD8C08A, TINTA = 0xFF2B1708, TINTA2 = 0xFF6B4423;
    private static final int TAPA = 0xFF8E1F26, TAPA2 = 0xFF4E0F14, TAPA3 = 0xFFB5303A, DORADO = 0xFFE8B84A, DORADO2 = 0xFFA97A1E;
    private static final int PW = 290, PH = 292, PX = 30, PY = 40, PX2 = PX + PW + 6;      // las dos paginas
    private static final int GX = PX + (PW - COLS * CELDA) / 2, GY = PY + 30;               // la rejilla del indice
    private static final int BY = PY + PH - 34;                                              // la fila del boton y las flechas

    private final List<Plato> platos = new ArrayList<>();
    private final Map<String, ItemStack> pilas = new HashMap<>();
    private int sel, pag, nivelLocal = 1;

    public LibroPantalla(String json) {
        super(Text.literal("Libro de recetas"));
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        nivelLocal = o.has("nivelLocal") ? o.get("nivelLocal").getAsInt() : 1;
        for (JsonElement e : o.getAsJsonArray("platos")) {
            JsonObject j = e.getAsJsonObject();
            List<Est> ests = new ArrayList<>();
            if (j.has("estaciones")) for (JsonElement x : j.getAsJsonArray("estaciones")) { JsonObject k = x.getAsJsonObject(); ests.add(new Est(k.get("nombre").getAsString(), k.get("item").getAsString(), k.get("tiene").getAsBoolean())); }
            platos.add(new Plato(j.get("id").getAsString(), j.get("item").getAsString(), j.get("nombre").getAsString(), j.get("precio").getAsInt(), j.get("coste").getAsInt(), j.get("receta").getAsString(),
                    j.get("tengo").getAsBoolean(), j.has("especial") && j.get("especial").getAsBoolean(), j.has("michi") ? j.get("michi").getAsString() : "",
                    j.has("nivelLocal") ? j.get("nivelLocal").getAsInt() : 1, j.has("bloqueo") ? j.get("bloqueo").getAsString() : "", ests));
        }
        for (int i = 0; i < platos.size(); i++) if (platos.get(i).tengo()) { sel = i; break; }          // empieza en el primer plato que ya sabes
        pag = sel / POR_PAG;
    }

    private ItemStack pila(String item) {
        return pilas.computeIfAbsent(item, k -> {
            var it = Registries.ITEM.getOrEmpty(Identifier.of(k));
            return it.isPresent() ? new ItemStack(it.get()) : ItemStack.EMPTY;
        });
    }

    private void objeto(DrawContext g, String item, int x, int y, float k) {
        ItemStack st = pila(item);
        if (st.isEmpty()) return;
        var m = g.getMatrices();
        m.push(); m.translate(x, y, 0); m.scale(k, k, 1f);
        g.drawItem(st, 0, 0);
        m.pop();
    }

    private int paginas() { return Math.max(1, (platos.size() + POR_PAG - 1) / POR_PAG); }

    // ------------------------------------------------------------ ayudas: texto de tinta (sin sombra, que sobre el papel se lee mal) y adornos

    private void tinta(DrawContext g, String s, int x, int y, int color, float tam) {
        var m = g.getMatrices();
        m.push(); m.translate(x, y, 0); m.scale(tam, tam, 1f);
        g.drawText(tr(), s, 0, 0, color, false);
        m.pop();
    }

    private int parrafoTinta(DrawContext g, String s, int x, int y, int ancho, int color, float tam) {
        List<net.minecraft.text.OrderedText> l = tr().wrapLines(Text.literal(s), (int) (ancho / tam));
        var m = g.getMatrices();
        for (int i = 0; i < l.size(); i++) {
            m.push(); m.translate(x, y + i * (int) (10 * tam), 0); m.scale(tam, tam, 1f);
            g.drawText(tr(), l.get(i), 0, 0, color, false);
            m.pop();
        }
        return l.size();
    }

    private static void disco(DrawContext g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) { int w = (int) Math.round(Math.sqrt(r * r - dy * dy + r * 0.5)); g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color); }
    }

    /** Una raya con un rombo en medio (adorno de los titulos de seccion). */
    private static void adorno(DrawContext g, int x, int y, int w) {
        g.fill(x, y, x + w / 2 - 6, y + 1, DORADO2); g.fill(x + w / 2 + 6, y, x + w, y + 1, DORADO2);
        for (int d = 0; d <= 4; d++) g.fill(x + w / 2 - d, y - 4 + d, x + w / 2 + d + 1, y - 3 + d, DORADO2);
        for (int d = 0; d <= 4; d++) g.fill(x + w / 2 - d, y + 4 - d, x + w / 2 + d + 1, y + 5 - d, DORADO2);
    }

    private static void esquinero(DrawContext g, int x, int y, int sx, int sy) {                // adorno dorado de las esquinas de la tapa
        g.fill(x, y, x + 14 * sx, y + 3 * sy, DORADO); g.fill(x, y, x + 3 * sx, y + 14 * sy, DORADO);
        g.fill(x + 6 * sx, y + 6 * sy, x + 10 * sx, y + 10 * sy, DORADO2);
    }

    private void tapaYPaginas(DrawContext g) {
        int x0 = PX - 18, y0 = PY - 30, x1 = PX2 + PW + 18, y1 = PY + PH + 18;
        g.fill(x0, y0, x1, y1, TAPA2);                                                          // la tapa de cuero
        g.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, TAPA);
        g.fill(x0 + 7, y0 + 7, x1 - 7, y0 + 9, DORADO2); g.fill(x0 + 7, y1 - 9, x1 - 7, y1 - 7, DORADO2);       // el filete dorado
        g.fill(x0 + 7, y0 + 7, x0 + 9, y1 - 7, DORADO2); g.fill(x1 - 9, y0 + 7, x1 - 7, y1 - 7, DORADO2);
        esquinero(g, x0 + 4, y0 + 4, 1, 1); esquinero(g, x1 - 4, y0 + 4, -1, 1); esquinero(g, x0 + 4, y1 - 4, 1, -1); esquinero(g, x1 - 4, y1 - 4, -1, -1);
        for (int k = 0; k < 4; k++) {                                                           // el canto de las hojas, para que parezca grueso
            g.fill(PX - 4 + k, PY - 8 + k, PX - 3 + k, PY + PH + 4, k % 2 == 0 ? PAPEL3 : PAPEL2);
            g.fill(PX2 + PW + 3 - k, PY - 8 + k, PX2 + PW + 4 - k, PY + PH + 4, k % 2 == 0 ? PAPEL3 : PAPEL2);
        }
        g.fill(PX, PY - 8, PX + PW, PY + PH + 4, PAPEL);                                         // las dos paginas
        g.fill(PX2, PY - 8, PX2 + PW, PY + PH + 4, PAPEL);
        for (int k = 0; k < 18; k++) {                                                          // sombra de cada pagina junto al lomo
            int c = 0x00000000 | ((0x2A - k * 2) << 24) | 0x5A3A10;
            g.fill(PX + PW - 1 - k, PY - 8, PX + PW - k, PY + PH + 4, c);
            g.fill(PX2 + k, PY - 8, PX2 + k + 1, PY + PH + 4, c);
        }
        g.fill(PX + PW, PY - 8, PX2, PY + PH + 4, TAPA2);                                       // el lomo
        g.fill(PX + PW + 2, PY - 8, PX + PW + 4, PY + PH + 4, DORADO2);
        // el marcapaginas: una cinta roja que cuelga del lomo
        g.fill(PX + PW - 1, PY - 30, PX + PW + 7, PY + 50, TAPA3);
        g.fill(PX + PW - 1, PY + 50, PX + PW + 3, PY + 58, TAPA3); g.fill(PX + PW + 3, PY + 50, PX + PW + 7, PY + 54, TAPA3);
    }

    @Override
    protected void dibujar(DrawContext g, int mx, int my, float delta) {
        tapaYPaginas(g);
        // el titulo, en una cinta dorada sobre la tapa
        g.fill(PX + 2, PY - 29, PX + 190, PY - 11, TAPA2); g.fill(PX + 3, PY - 28, PX + 189, PY - 12, DORADO);
        g.fill(PX + 5, PY - 26, PX + 187, PY - 14, DORADO2); g.fill(PX + 6, PY - 25, PX + 186, PY - 15, TAPA2);
        texto(g, "LIBRO DE RECETAS", PX + 12, PY - 24, 0xFFFFE9A8, 1.2f);
        boton(g, PX2 + PW - 70, PY - 27, 70, 16, "CERRAR", 0xFF8A3A3A, dentro(mx, my, PX2 + PW - 70, PY - 27, 70, 16));

        // el indice (pagina izquierda)
        tinta(g, "Elige un plato", PX + 16, PY + 2, TINTA, 1.3f);
        adorno(g, PX + 14, PY + 22, PW - 28);
        int total = paginas();
        pag = Math.min(pag, total - 1);
        if (total > 1) {
            boton(g, PX + PW - 94, PY - 1, 20, 16, "◀", 0xFF7A4AC0, pag > 0 && dentro(mx, my, PX + PW - 94, PY - 1, 20, 16));
            tinta(g, (pag + 1) + "/" + total, PX + PW - 68, PY + 3, TINTA, 1.1f);
            boton(g, PX + PW - 36, PY - 1, 20, 16, "▶", 0xFF7A4AC0, pag < total - 1 && dentro(mx, my, PX + PW - 36, PY - 1, 20, 16));
        }
        for (int n = 0; n < POR_PAG; n++) {
            int i = pag * POR_PAG + n;
            if (i >= platos.size()) break;
            Plato p = platos.get(i);
            int x = GX + (n % COLS) * CELDA, y = GY + (n / COLS) * CELDA;
            boolean sobre = dentro(mx, my, x, y, CELDA - 4, CELDA - 4);
            g.fill(x, y, x + CELDA - 4, y + CELDA - 4, i == sel ? DORADO2 : sobre ? 0xFFB08B4A : PAPEL3);
            g.fill(x + 1, y + 1, x + CELDA - 5, y + CELDA - 5, i == sel ? 0xFFFFF0B8 : p.tengo() ? 0xFFFFFBEA : 0xFFD8CBA6);
            objeto(g, p.item(), x + 3, y + 3, 1.9f);
            if (!p.tengo()) g.fill(x + 1, y + 1, x + CELDA - 5, y + CELDA - 5, 0x88605038);          // sin aprender: apagado
        }
        tinta(g, "Los apagados aún no los sabes hacer", PX + 16, PY + PH - 12, TINTA2, 1.0f);

        // la receta (pagina derecha)
        if (platos.isEmpty()) { tinta(g, "Todavía no hay recetas.", PX2 + 16, PY + 20, TINTA, 1.2f); return; }
        Plato p = platos.get(Math.max(0, Math.min(sel, platos.size() - 1)));
        g.fill(PX2 + 14, PY + 6, PX2 + 62, PY + 54, DORADO2); g.fill(PX2 + 16, PY + 8, PX2 + 60, PY + 52, 0xFFFFFBEA);
        objeto(g, p.item(), PX2 + 20, PY + 14, 2.2f);
        float tamNombre = tr().getWidth(p.nombre()) * 1.7f > PW - 84 ? 1.25f : 1.7f;
        tinta(g, p.nombre(), PX2 + 72, PY + 8, TINTA, tamNombre);
        tinta(g, p.especial() ? "★ Visita especial" + (p.michi().isEmpty() ? "" : " · del " + p.michi()) : "Paga " + p.precio() + " monedas (más propina)", PX2 + 72, PY + 30, 0xFF7A4A00, 1.0f);
        if (p.tengo()) tinta(g, "✔ ¡Ya sabes hacerlo!", PX2 + 72, PY + 43, 0xFF1B6B2E, 1.1f);
        else if (!p.bloqueo().isEmpty()) tinta(g, "✘ Aún no (local " + p.nivelLocal() + ")", PX2 + 72, PY + 43, 0xFFA02020, 1.1f);
        else tinta(g, "✘ Cuesta " + p.coste() + " monedas en la TIENDA", PX2 + 72, PY + 43, 0xFFA02020, 1.0f);

        tinta(g, "CÓMO SE HACE", PX2 + 16, PY + 66, TINTA, 1.3f);
        adorno(g, PX2 + 14, PY + 84, PW - 28);
        int y = PY + 92, n = 1;
        for (String gr : p.receta().split(";\\s*")) {
            for (String paso : gr.split("\\s*→\\s*")) {
                if (paso.isBlank()) continue;
                disco(g, PX2 + 22, y + 5, 6, DORADO2); disco(g, PX2 + 22, y + 5, 5, DORADO);
                textoCentrado(g, String.valueOf(n), PX2 + 22, y + 1, TINTA, 1.0f);
                int lineas = parrafoTinta(g, paso.trim(), PX2 + 36, y, PW - 52, TINTA, 1.0f);
                y += Math.max(1, lineas) * 10 + 4;
                n++;
            }
        }
        if (!p.estaciones().isEmpty()) {
            int yb = Math.max(y + 2, PY + 172);
            tinta(g, "ESTACIONES", PX2 + 16, yb, TINTA2, 1.1f);
            for (int k = 0; k < p.estaciones().size() && k < 8; k++) {
                Est e = p.estaciones().get(k);
                int x = PX2 + 16 + (k % 2) * 136, yy = yb + 13 + (k / 2) * 20;
                g.fill(x, yy, x + 18, yy + 18, PAPEL3); g.fill(x + 1, yy + 1, x + 17, yy + 17, 0xFFFFFBEA);
                objeto(g, e.item(), x + 1, yy + 1, 1.0f);
                tinta(g, e.nombre(), x + 22, yy + 5, e.tiene() ? TINTA : 0xFFA02020, 0.95f);
                if (!e.tiene()) tinta(g, "✘", x + 11, yy - 2, 0xFFD02020, 1.2f);
            }
        }
        boolean puede = !p.especial() && !p.estaciones().isEmpty();
        boton(g, PX2 + 40, BY, PW - 80, 24, "¡MUÉSTRAMELO!", 0xFF2E8B57, puede && dentro(mx, my, PX2 + 40, BY, PW - 80, 24));
        if (!puede) g.fill(PX2 + 40, BY, PX2 + PW - 40, BY + 24, 0x88404040);
        boton(g, PX2 + 12, BY, 24, 24, "◀", 0xFF7A4AC0, sel > 0 && dentro(mx, my, PX2 + 12, BY, 24, 24));
        boton(g, PX2 + PW - 36, BY, 24, 24, "▶", 0xFF7A4AC0, sel < platos.size() - 1 && dentro(mx, my, PX2 + PW - 36, BY, 24, 24));
    }

    private void boton(DrawContext g, int x, int y, int w, int h, String t, int color, boolean sobre) {
        int c = sobre ? 0xFF000000 | (Math.min(255, ((color >> 16) & 255) + 40) << 16) | (Math.min(255, ((color >> 8) & 255) + 40) << 8) | Math.min(255, (color & 255) + 40) : color;
        caja(g, x, y, w, h, c, 0xFFFFFFFF);
        textoCentrado(g, t, x + w / 2, y + Math.max(2, (h - 10) / 2), BLANCO, h < 20 ? 0.9f : 1.2f);
    }

    @Override
    protected void clic(int mx, int my, int boton) {
        if (dentro(mx, my, PX2 + PW - 70, PY - 27, 70, 16)) { close(); return; }
        int total = paginas();
        if (total > 1) {
            if (pag > 0 && dentro(mx, my, PX + PW - 94, PY - 1, 20, 16)) { pag--; return; }
            if (pag < total - 1 && dentro(mx, my, PX + PW - 36, PY - 1, 20, 16)) { pag++; return; }
        }
        for (int n = 0; n < POR_PAG; n++) {
            int i = pag * POR_PAG + n;
            if (i >= platos.size()) break;
            int x = GX + (n % COLS) * CELDA, y = GY + (n / COLS) * CELDA;
            if (dentro(mx, my, x, y, CELDA - 4, CELDA - 4)) { sel = i; return; }
        }
        if (platos.isEmpty()) return;
        if (sel > 0 && dentro(mx, my, PX2 + 12, BY, 24, 24)) { sel--; pag = sel / POR_PAG; return; }
        if (sel < platos.size() - 1 && dentro(mx, my, PX2 + PW - 36, BY, 24, 24)) { sel++; pag = sel / POR_PAG; return; }
        Plato p = platos.get(sel);
        if (!p.especial() && !p.estaciones().isEmpty() && dentro(mx, my, PX2 + 40, BY, PW - 80, 24)) {
            ClientPlayNetworking.send(new AccionPayload("libro_mostrar", p.id(), ""));
            close();
        }
    }

    @Override public void close() { MinecraftClient.getInstance().setScreen(null); }
}
