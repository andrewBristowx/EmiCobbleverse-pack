package emi.survivors.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emi.survivors.net.AccionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * La pantalla de las 3 cartas que sale al subir de nivel (la partida esta en pausa): se elige con un clic o con las teclas 1, 2 y 3. Si pasan 30 segundos, el
 * servidor elige una al azar y cierra la pantalla. Lo que dice cada carta lo manda el servidor (JSON), asi no hay que repetirlo aqui.
 */
@Environment(EnvType.CLIENT)
public final class CartasPantalla extends Lienzo {
    private record Carta(String tipo, boolean nueva, boolean evo, int nivel, String titulo, String texto, String icono) {}

    private final List<Carta> cartas = new ArrayList<>();
    private final int nivelJugador, segundos;
    private final long t0 = System.currentTimeMillis();
    private boolean enviada;

    public CartasPantalla(String json) {
        super(Text.literal("Nivel nuevo"));
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        nivelJugador = o.get("nivel").getAsInt();
        segundos = o.get("seg").getAsInt();
        JsonArray a = o.getAsJsonArray("cartas");
        for (var e : a) {
            JsonObject c = e.getAsJsonObject();
            cartas.add(new Carta(c.get("tipo").getAsString(), c.get("nueva").getAsBoolean(), c.has("evo") && c.get("evo").getAsBoolean(), c.get("nivel").getAsInt(), c.get("titulo").getAsString(), c.get("texto").getAsString(), c.get("icono").getAsString()));
        }
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override protected boolean sonidoClic() { return false; }

    private static final int CW = 170, CH = 230, SEP = 22;

    private int x0() { return (W - (cartas.size() * CW + (cartas.size() - 1) * SEP)) / 2; }

    private static int color(Carta c) {
        if (c.evo()) return 0xFFFFD34E;
        return switch (c.tipo()) { case "arma" -> 0xFFFF8A4D; case "pasiva" -> AZUL; case "curar" -> VERDE; default -> ORO; };
    }

    @Override
    protected void dibujar(DrawContext g, int mx, int my, float delta) {
        textoCentrado(g, "¡NIVEL " + nivelJugador + "!", W / 2, 22, ORO, 2.2f);
        textoCentrado(g, "Elige una mejora (clic o tecla 1, 2, 3)", W / 2, 52, GRIS, 1f);
        int restante = Math.max(0, segundos - (int) ((System.currentTimeMillis() - t0) / 1000));
        for (int i = 0; i < cartas.size(); i++) {
            Carta c = cartas.get(i);
            int x = x0() + i * (CW + SEP), y = 72;
            boolean sobre = dentro(mx, my, x, y, CW, CH);
            int col = color(c);
            if (sobre) g.fill(x - 3, y - 3, x + CW + 3, y + CH + 3, 0x80FFFFFF);
            if (c.evo()) {                                              // la carta de evolucion brilla: marco dorado doble que parpadea
                int a = 0x60 + (int) (0x50 * (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 220.0)));
                g.fill(x - 5, y - 5, x + CW + 5, y + CH + 5, (a << 24) | 0xFFD34E);
            }
            caja(g, x, y, CW, CH, c.evo() ? (sobre ? 0xFF3A3220 : 0xFF2A2418) : sobre ? 0xFF2A2A45 : 0xFF1B1B30, col);
            g.fill(x + 1, y + 1, x + CW - 1, y + 26, col & 0x00FFFFFF | 0x66000000);
            textoCentrado(g, "[" + (i + 1) + "]", x + CW / 2, y + 9, BLANCO, 1.1f);
            icono(g, c.icono(), x + CW / 2 - 24, y + 36, 2f);
            parrafoCentrado(g, c.titulo(), x + CW / 2, y + 96, CW - 16, BLANCO, 1.25f);
            String etiqueta = c.evo() ? "¡EVOLUCIÓN!" : c.tipo().equals("arma") || c.tipo().equals("pasiva") ? (c.nueva() ? "¡NUEVO!" : "Nivel " + c.nivel()) : "";
            if (!etiqueta.isEmpty()) textoCentrado(g, etiqueta, x + CW / 2, y + 128, c.nueva() || c.evo() ? ORO : col, 1.1f);
            parrafo(g, c.texto(), x + 12, y + 152, CW - 24, GRIS, 1f);
        }
        // tiempo que queda
        int bw = 260, bx = (W - bw) / 2, by = 322;
        g.fill(bx, by, bx + bw, by + 6, 0xFF101020);
        g.fill(bx, by, bx + (int) (bw * (restante / (float) Math.max(1, segundos))), by + 6, restante > 8 ? AZUL : ROJO);
        textoCentrado(g, "Si no eliges, se elige una al azar (" + restante + " s)", W / 2, by + 10, GRIS, 0.9f);
    }

    private void parrafoCentrado(DrawContext g, String s, int cx, int y, int ancho, int color, float tam) {
        var lineas = tr().wrapLines(Text.literal(s), (int) (ancho / tam));
        var m = g.getMatrices();
        for (int i = 0; i < lineas.size(); i++) {
            int w = (int) (tr().getWidth(lineas.get(i)) * tam);
            m.push(); m.translate(cx - w / 2f, y + i * 11 * tam, 0); m.scale(tam, tam, 1f);
            g.drawText(tr(), lineas.get(i), 0, 0, color, true);
            m.pop();
        }
    }

    @Override
    protected void clic(int mx, int my, int boton) {
        for (int i = 0; i < cartas.size(); i++) if (dentro(mx, my, x0() + i * (CW + SEP), 72, CW, CH)) { elegir(i); return; }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int idx = keyCode >= 49 && keyCode <= 51 ? keyCode - 49 : keyCode >= 321 && keyCode <= 323 ? keyCode - 321 : -1;      // 1-3 y teclado numerico
        if (idx >= 0 && idx < cartas.size()) { elegir(idx); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void elegir(int idx) {
        if (enviada) return;
        enviada = true;
        ClientPlayNetworking.send(new AccionPayload("carta", String.valueOf(idx), ""));
        MinecraftClient.getInstance().setScreen(null);
    }
}
