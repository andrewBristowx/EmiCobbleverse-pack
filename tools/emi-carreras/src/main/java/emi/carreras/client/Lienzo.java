package emi.carreras.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Base de las pantallas del minijuego: se dibujan en un lienzo fijo de 640x360 que se escala a la ventana (asi se ven igual con cualquier tamano de interfaz).
 * Las pantallas hijas implementan {@link #dibujar} y {@link #clic} con coordenadas del lienzo.
 */
@Environment(EnvType.CLIENT)
public abstract class Lienzo extends Screen {
    public static final int W = 640, H = 360;
    public static final int BLANCO = 0xFFFFFFFF, GRIS = 0xFFB8B8C8, ORO = 0xFFFFD34E, ROSA = 0xFFFF9AD5, LILA = 0xFFC9B3FF, AZUL = 0xFF6CC8FF, VERDE = 0xFF6FE08A, ROJO = 0xFFFF6B5E;

    protected float escala = 1f, ox, oy;

    protected Lienzo(Text titulo) { super(titulo); }

    protected abstract void dibujar(DrawContext g, int mx, int my, float delta);

    protected void clic(int mx, int my, int boton) { }

    @Override public boolean shouldPause() { return false; }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        escala = Math.min(width / (float) W, height / (float) H);
        ox = (width - W * escala) / 2f; oy = (height - H * escala) / 2f;
        g.fill(0, 0, width, height, 0xC808081A);
        var m = g.getMatrices();
        m.push();
        m.translate(ox, oy, 0);
        m.scale(escala, escala, 1f);
        dibujar(g, (int) Math.floor((mouseX - ox) / escala), (int) Math.floor((mouseY - oy) / escala), delta);
        m.pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        clic((int) Math.floor((mouseX - ox) / escala), (int) Math.floor((mouseY - oy) / escala), button);
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.8f));
        return true;
    }

    // ------------------------------------------------------------ ayudas de dibujo

    protected static boolean dentro(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    protected TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }

    /** Texto con tamano (1 = normal), con sombra. */
    protected void texto(DrawContext g, String s, int x, int y, int color, float tam) {
        var m = g.getMatrices();
        m.push(); m.translate(x, y, 0); m.scale(tam, tam, 1f);
        g.drawTextWithShadow(tr(), s, 0, 0, color);
        m.pop();
    }

    protected void textoCentrado(DrawContext g, String s, int cx, int y, int color, float tam) {
        texto(g, s, cx - (int) (tr().getWidth(s) * tam / 2), y, color, tam);
    }

    /** Texto que se parte en lineas de {@code ancho} (pixeles del lienzo); devuelve cuantas lineas uso. */
    protected int parrafo(DrawContext g, String s, int x, int y, int ancho, int color, float tam) {
        List<OrderedText> l = tr().wrapLines(Text.literal(s), (int) (ancho / tam));
        var m = g.getMatrices();
        for (int i = 0; i < l.size(); i++) {
            m.push(); m.translate(x, y + i * 10 * tam, 0); m.scale(tam, tam, 1f);
            g.drawText(tr(), l.get(i), 0, 0, color, true);
            m.pop();
        }
        return l.size();
    }

    /** Un marco con relleno (borde de 1 px). */
    protected static void caja(DrawContext g, int x, int y, int w, int h, int relleno, int borde) {
        g.fill(x, y, x + w, y + h, borde);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, relleno);
    }
}
