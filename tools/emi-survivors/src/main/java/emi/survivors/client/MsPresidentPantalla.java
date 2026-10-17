package emi.survivors.client;

import emi.survivors.net.AccionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * La conversacion con «Ms. President», la michi con traje y maletin que patrocina los minijuegos: su retrato (parpadea, 4 fotogramas en {@code textures/ms_president.png})
 * y un cuadro de dialogo con el texto escribiendose letra a letra. Clic, espacio o intro pasan a la siguiente frase; la ultima ofrece unirse al juego o dejarlo para luego.
 */
@Environment(EnvType.CLIENT)
public final class MsPresidentPantalla extends Lienzo {
    private static final Identifier TEX = Identifier.of("emi_survivors", "textures/ms_president.png");
    /** Lo que dice (cada elemento es una pagina). Para cambiar el discurso basta con editar estas frases. */
    private static final String[] PAGINAS = {
            "¡Muy buenas! Soy Ms. President, presidenta de Michialien S.A., la empresa más adorable de todo el universo.",
            "Este mundo va a estar lleno de minijuegos: carreras de Pokémon, huidas de monstruos enormes, hordas que sobrevivir... ¡y yo los patrocino todos!",
            "¿Te animas a probar uno? Ahora mismo puedes unirte a «Emi Survivors»: sobrevive a la horda con tus Pokémon, pasa de mundo en mundo y vence al Michi Gigante."
    };
    /** Duracion (ms) de cada fotograma del retrato: ojos abiertos, cerrados, abiertos, cerrados (el parpadeo doble del GIF original). */
    private static final int[] DURACION = {1400, 200, 160, 200};

    private final long t0 = System.currentTimeMillis();
    private long inicioPagina = t0;
    private int pagina;
    private boolean enviado;

    public MsPresidentPantalla() { super(Text.literal("Ms. President")); }

    @Override public boolean shouldCloseOnEsc() { return true; }

    private int visibles() { return (int) ((System.currentTimeMillis() - inicioPagina) / 22); }          // letras ya escritas

    private boolean terminoDeEscribir() { return visibles() >= PAGINAS[pagina].length(); }

    private boolean ultima() { return pagina == PAGINAS.length - 1; }

    private int fotograma() {
        long t = (System.currentTimeMillis() - t0) % 1960;
        int acum = 0;
        for (int i = 0; i < DURACION.length; i++) { acum += DURACION[i]; if (t < acum) return i; }
        return 0;
    }

    private void avanzar() {
        if (!terminoDeEscribir()) { inicioPagina = System.currentTimeMillis() - PAGINAS[pagina].length() * 22L - 1; return; }      // primero se escribe lo que falta
        if (!ultima()) { pagina++; inicioPagina = System.currentTimeMillis(); }
    }

    private void unirse() {
        if (enviado) return;
        enviado = true;
        ClientPlayNetworking.send(new AccionPayload("ms_unirse", "", ""));
        close();
    }

    @Override
    protected void dibujar(DrawContext g, int mx, int my, float delta) {
        // el retrato, grande, a la izquierda y con un fondo suave
        int px = 40, py = 42, tam = 192;
        caja(g, px - 6, py - 6, tam + 12, tam + 12, 0xFF1B1B30, 0xFFFFD34E);
        g.drawTexture(TEX, px, py, tam, tam, fotograma() * 128f, 0f, 128, 128, 512, 128);
        // la placa con el nombre y el patrocinio
        textoCentrado(g, "Ms. President", px + tam / 2, py + tam + 12, ORO, 1.4f);
        textoCentrado(g, "Presidenta de Michialien S.A.", px + tam / 2, py + tam + 26, GRIS, 0.8f);
        // el cuadro de dialogo
        int bx = 262, by = 62, bw = 338, bh = 168;
        caja(g, bx, by, bw, bh, 0xFF14142A, 0xFF8F78D8);
        String frase = PAGINAS[pagina];
        String visible = frase.substring(0, Math.min(frase.length(), visibles()));
        parrafo(g, visible, bx + 14, by + 14, bw - 28, BLANCO, 1.15f);
        boolean fin = terminoDeEscribir();
        if (!ultima()) {
            if (fin && (System.currentTimeMillis() / 400) % 2 == 0) texto(g, "Siguiente ▶", bx + bw - 76, by + bh - 16, ORO, 0.9f);
            texto(g, "(clic o espacio)", bx + 14, by + bh - 16, GRIS, 0.8f);
        } else if (fin) {
            // las dos opciones
            int w = 150, h = 26, y = by + bh - 40;
            boolean sobre1 = dentro(mx, my, bx + 14, y, w, h), sobre2 = dentro(mx, my, bx + bw - 14 - w, y, w, h);
            caja(g, bx + 14, y, w, h, sobre1 ? 0xFF3CBF63 : 0xFF2E9B4E, sobre1 ? BLANCO : 0xFF6FE08A);
            textoCentrado(g, "¡Unirme al juego!", bx + 14 + w / 2, y + 9, BLANCO, 1.0f);
            caja(g, bx + bw - 14 - w, y, w, h, sobre2 ? 0xFF4A4A6A : 0xFF33334F, sobre2 ? BLANCO : 0xFF6A6A8A);
            textoCentrado(g, "Ahora no, gracias", bx + bw - 14 - w / 2, y + 9, GRIS, 1.0f);
        }
        textoCentrado(g, "Michialien S.A. · patrocinador oficial de los minijuegos", W / 2, H - 22, 0xFF8F78D8, 0.8f);
    }

    @Override
    protected void clic(int mx, int my, int boton) {
        if (ultima() && terminoDeEscribir()) {
            int bx = 262, by = 62, bw = 338, bh = 168, w = 150, h = 26, y = by + bh - 40;
            if (dentro(mx, my, bx + 14, y, w, h)) { unirse(); return; }
            if (dentro(mx, my, bx + bw - 14 - w, y, w, h)) { close(); return; }
            return;
        }
        avanzar();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (ultima() && terminoDeEscribir()) unirse(); else avanzar();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
