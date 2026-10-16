package emi.survivors.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emi.survivors.EmiSurvivors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Los iconos propios del minijuego (ataques, evoluciones, objetos y mejoras): un atlas de dibujos de 24x24 pixeles ({@code textures/iconos.png}) y su indice
 * ({@code iconos.json}: clave -> columna y fila). Los genera {@code tools/emi-survivors/assets/generar_iconos.py}; las claves son {@code a_<ataque>}, {@code e_<ataque>}
 * (evolucion), {@code i_<tipo>} (ataque inicial de ese tipo de Pokemon), {@code p_<objeto>}, {@code m_<mejora>} y {@code x_<otro>}.
 */
@Environment(EnvType.CLIENT)
public final class Iconos {
    private Iconos() {}

    /** Lado de cada dibujo del atlas (pixeles). */
    public static final int CELDA = 24;
    private static final Identifier TEX = Identifier.of("emi_survivors", "textures/iconos.png");
    private static final Identifier INDICE = Identifier.of("emi_survivors", "iconos.json");
    private static Map<String, int[]> indice;
    private static int ancho = 384, alto = 168;

    private static void cargar() {
        indice = new HashMap<>();
        try {
            var r = MinecraftClient.getInstance().getResourceManager().getResource(INDICE);
            if (r.isEmpty()) { EmiSurvivors.LOG.warn("No se encuentra {} (los iconos no se veran).", INDICE); return; }
            try (var in = new InputStreamReader(r.get().getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
                ancho = o.get("ancho").getAsInt();
                alto = o.get("alto").getAsInt();
                for (var e : o.getAsJsonObject("iconos").entrySet()) {
                    JsonArray a = e.getValue().getAsJsonArray();
                    indice.put(e.getKey(), new int[]{a.get(0).getAsInt(), a.get(1).getAsInt()});
                }
            }
        } catch (Exception e) {
            EmiSurvivors.LOG.warn("No se pudo leer {}: {}", INDICE, e.toString());
        }
    }

    /** Vuelve a leer el indice la proxima vez (al recargar los recursos). */
    public static void olvidar() { indice = null; }

    public static boolean existe(String clave) {
        if (indice == null) cargar();
        return clave != null && indice.containsKey(clave);
    }

    /** Dibuja el icono {@code clave} con su esquina de arriba a la izquierda en (x, y) y {@code tam} pixeles de lado (24 = tamano natural; mejor multiplos de 24). */
    public static void dibujar(DrawContext g, String clave, int x, int y, int tam) {
        if (indice == null) cargar();
        int[] c = clave == null ? null : indice.get(clave);
        if (c == null) return;
        g.drawTexture(TEX, x, y, tam, tam, c[0] * CELDA, c[1] * CELDA, CELDA, CELDA, ancho, alto);
    }
}
