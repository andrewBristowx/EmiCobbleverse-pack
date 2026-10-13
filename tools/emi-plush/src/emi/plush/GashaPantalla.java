package emi.plush;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.class_1109;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_437;
import net.minecraft.class_7923;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pantalla del Gasha Especial de Eventos. Todo se dibuja en un lienzo de 1536x1024 que se escala a la ventana; el fondo y los adornos son
 * texturas (`textures/gui/gasha/`), el resto (textos, ticket, Pokébola, animaciones, premio) se dibuja aquí. El servidor hace las tiradas y
 * manda los premios; la pantalla solo los enseña con una animación corta (normal) o larga (premio mayor).
 */
public class GashaPantalla extends class_437 {
    static final int W = 1536, H = 1024, BX = 768, BY = 400;
    static final int ORO = 0xFFFFCD2D, ORO_L = 0xFFFFEE96, BLANCO = 0xFFFAF6FF, LILA = 0xFFD2BEF5, ROSA = 0xFFFF78BE;

    static GashaPantalla abierta;

    // ---------------------------------------------------------------- estado
    private final int x, y, z;
    private int garantia;
    private String error = "";
    private Premio ultimo;
    private final List<Premio> cola = new ArrayList<>();
    private int idx = -1;                 // premio que se está enseñando; -1 = reposo
    private boolean fin;                  // terminó la cola: se queda el último premio hasta el siguiente clic
    private long inicio;                  // ns del arranque de la animación del premio actual
    private int sonidos;                  // etapas de sonido ya reproducidas del premio actual
    private boolean esperando;            // petición enviada, aún sin respuesta
    private final long t0 = System.nanoTime();
    private final float[][] lluvia = new float[70][6];   // x0, y0, velocidad, balanceo, tipo, color

    public GashaPantalla(int x, int y, int z, int garantia) {
        super(class_2561.method_43470("Gasha"));
        this.x = x;
        this.y = y;
        this.z = z;
        this.garantia = garantia;
        Random r = new Random(11);
        for (float[] p : lluvia) {
            p[0] = r.nextFloat() * W; p[1] = -r.nextFloat() * 600; p[2] = 260 + r.nextFloat() * 320; p[3] = r.nextFloat() * 6.28f; p[4] = r.nextInt(3); p[5] = r.nextInt(4);
        }
    }

    // ---------------------------------------------------------------- premios
    static final class Premio {
        String pool = "", nombre = "";
        boolean mayor;
        final List<class_1799> objetos = new ArrayList<>();
        final List<String> pokes = new ArrayList<>();
        final List<RenderablePokemon> renders = new ArrayList<>();
        final List<FloatingState> estados = new ArrayList<>();

        static Premio de(JsonObject o) {
            Premio p = new Premio();
            p.pool = o.get("pool").getAsString();
            p.nombre = o.get("n").getAsString();
            p.mayor = o.get("mayor").getAsBoolean();
            for (JsonElement e : o.getAsJsonArray("objetos")) {
                JsonObject it = e.getAsJsonObject();
                var item = class_7923.field_41178.method_10223(class_2960.method_12829(it.get("id").getAsString()));
                p.objetos.add(new class_1799(item, it.get("n").getAsInt()));
            }
            for (JsonElement e : o.getAsJsonArray("pokemon")) {
                String props = e.getAsString();
                p.pokes.add(props);
                RenderablePokemon rp = null;
                try {
                    var pk = PokemonProperties.Companion.parse(props).create();
                    pk.updateAspects();
                    RenderablePokemon base = pk.asRenderablePokemon();
                    // el Pokémon creado en el cliente no trae las propiedades propias como aspectos: se añaden a mano (emi → emi, gold → gold, accessory=X → X-accessory)
                    java.util.Set<String> aspectos = new java.util.HashSet<>(base.getAspects());
                    for (String t : props.split(" ")) {
                        if (t.equals("emi=true")) aspectos.add("emi");
                        else if (t.equals("gold=true")) aspectos.add("gold");
                        else if (t.startsWith("accessory=")) aspectos.add(t.substring(10) + "-accessory");
                    }
                    rp = new RenderablePokemon(base.getSpecies(), aspectos, class_1799.field_8037);
                } catch (RuntimeException | LinkageError ex) {
                    // sin modelo: se dibuja una Pokébola en su lugar
                }
                p.renders.add(rp);
                FloatingState st = new FloatingState();
                if (rp != null) st.setCurrentAspects(rp.getAspects());
                p.estados.add(st);
            }
            return p;
        }

        int visuales() { return pokes.size() + objetos.size(); }

        String titulo() {
            if (mayor) return "¡¡PREMIO MAYOR!!";
            return switch (pool) {
                case "pareja" -> "¡¡PAREJA!!";
                case "mod", "pokemon" -> "¡Pokémon!";
                case "peluche" -> "¡Peluche!";
                default -> "¡Objeto!";
            };
        }

        String tipo() {
            return switch (pool) {
                case "comun" -> "Objeto común";
                case "bueno" -> "Objeto bueno";
                case "epico" -> "Objeto épico";
                case "mod" -> "Pokémon de Emi";
                case "peluche" -> "Peluche";
                case "pokemon" -> "Pokémon";
                default -> "Pokémon + su peluche";
            };
        }
    }

    /** Respuesta del servidor (se llama desde el hilo del cliente). */
    public static void recibir(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        String t = o.get("t").getAsString();
        var mc = class_310.method_1551();
        if (t.equals("abrir")) {
            mc.method_1507(abierta = new GashaPantalla(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt(), o.get("garantia").getAsInt()));
            return;
        }
        GashaPantalla p = abierta;
        if (p == null || mc.field_1755 != p) return;
        p.garantia = o.get("garantia").getAsInt();
        p.esperando = false;
        if (t.equals("error")) {
            p.error = "Necesitas más tickets";
        } else if (t.equals("resultado")) {
            p.error = "";
            p.cola.clear();
            for (JsonElement e : o.getAsJsonArray("premios")) p.cola.add(Premio.de(e.getAsJsonObject()));
            p.idx = 0;
            p.fin = false;
            p.empezar();
        }
    }

    private void empezar() {
        inicio = System.nanoTime();
        sonidos = 0;
    }

    private double t() { return (System.nanoTime() - inicio) / 1e9; }

    private double duracion(Premio p) { return p.mayor ? 4.0 : (cola.size() > 1 ? 1.15 : 1.6); }

    private void sonido(String id, float tono) {
        class_310.method_1551().method_1483().method_4873(class_1109.method_4758(class_3414.method_47908(class_2960.method_12829(id)), tono));
    }

    // ---------------------------------------------------------------- entrada
    private int tickets() {
        var mc = class_310.method_1551();
        if (mc.field_1724 == null) return 0;
        int n = 0;
        var inv = mc.field_1724.method_31548();
        for (int i = 0; i < inv.method_5439(); i++) {
            var st = inv.method_5438(i);
            if (st.method_31574(EmiPlush.TICKET_GASHA)) n += st.method_7947();
        }
        return n;
    }

    private boolean ocupado() { return idx >= 0 || esperando; }

    private float k() { return Math.min(field_22789 / (float) W, field_22790 / (float) H); }
    private float ox() { return (field_22789 - W * k()) / 2f; }
    private float oy() { return (field_22790 - H * k()) / 2f; }

    @Override
    public boolean method_25402(double mx, double my, int boton) {
        double dx = (mx - ox()) / k(), dy = (my - oy()) / k();
        if (boton != 0) return true;
        if (dx >= 1450 && dx <= 1500 && dy >= 900 && dy <= 950 && !ocupado()) { method_25419(); return true; }
        if (idx >= 0) {                                   // clic durante la animación: saltar
            Premio p = cola.get(idx);
            double u = t() / duracion(p);
            if (u < revelar(p)) { inicio = System.nanoTime() - (long) (revelar(p) * duracion(p) * 1e9); sonidos = 9; }
            else siguiente();
            return true;
        }
        if (fin) { fin = false; cola.clear(); return true; }
        if (esperando) return true;
        for (int n : new int[]{1, 10}) {
            int bx0 = n == 1 ? 330 : 790;
            if (dx >= bx0 && dx <= bx0 + 410 && dy >= 860 && dy <= 975) {
                if (tickets() < n) { error = "Necesitas " + n + (n == 1 ? " ticket" : " tickets") + " en el inventario"; return true; }
                error = "";
                esperando = true;
                ClientPlayNetworking.send(new GashaPeticion("{\"op\":\"tirar\",\"x\":" + x + ",\"y\":" + y + ",\"z\":" + z + ",\"n\":" + n + "}"));
                sonido("minecraft:entity.experience_orb.pickup", 0.8f);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean method_25404(int tecla, int esc, int mods) {
        if (tecla == 256 && idx < 0 && !esperando) { method_25419(); return true; }          // ESC
        if ((tecla == 32 || tecla == 257) && idx >= 0) return method_25402(ox() + 10, oy() + 10, 0);   // espacio/intro: saltar
        return super.method_25404(tecla, esc, mods);
    }

    @Override
    public boolean method_25421() { return false; }

    @Override
    public void method_25432() {
        if (abierta == this) abierta = null;
    }

    private void siguiente() {
        if (idx + 1 < cola.size()) {
            idx++;
            empezar();
        } else {
            ultimo = cola.get(cola.size() - 1);
            idx = -1;
            fin = true;
        }
    }

    private double revelar(Premio p) { return p.mayor ? 0.55 : 0.55; }

    // ---------------------------------------------------------------- dibujo
    private static class_2960 tex(String n) { return class_2960.method_60655(EmiPlush.ID, "textures/gui/gasha/" + n + ".png"); }

    private void img(class_332 c, String n, float cx, float cy, float w, float h, float giro, float alfa, int rgb) {
        var m = c.method_51448();
        m.method_22903();
        m.method_46416(cx, cy, 0);
        if (giro != 0) m.method_22907(new Quaternionf().rotateZ(giro));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, Math.max(0, Math.min(1, alfa)));
        int tw = n.equals("fondo") ? W : (n.equals("rayos") ? 512 : (n.equals("emi") ? 128 : (n.equals("estrella") || n.equals("pata") ? 32 : 256)));
        int th = n.equals("fondo") ? H : tw;
        c.method_25293(tex(n), Math.round(-w / 2), Math.round(-h / 2), Math.round(w), Math.round(h), 0f, 0f, tw, th, tw, th);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        m.method_22909();
    }

    private void txt(class_332 c, String s, float cx, float cy, float tam, int col, boolean centro) {
        var m = c.method_51448();
        m.method_22903();
        m.method_46416(cx, cy, 0);
        float sc = tam / 9f;
        m.method_22905(sc, sc, 1f);
        int w = field_22793.method_1727(s);
        c.method_51433(field_22793, s, centro ? -w / 2 : 0, -4, col, true);
        m.method_22909();
    }

    /** Como txt, pero si el texto no cabe en `ancho` (unidades del lienzo) lo encoge. */
    private void txtAncho(class_332 c, String s, float cx, float cy, float tam, int col, float ancho) {
        float w = field_22793.method_1727(s) * tam / 9f;
        txt(c, s, cx, cy, w > ancho ? tam * ancho / w : tam, col, true);
    }

    private void rect(class_332 c, float x0, float y0, float x1, float y1, int argb) {
        c.method_25294(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1), argb);
    }

    private void objeto(class_332 c, class_1799 st, float cx, float cy, float tam) {
        var m = c.method_51448();
        m.method_22903();
        m.method_46416(cx - tam / 2, cy - tam / 2, 0);
        float sc = tam / 16f;
        m.method_22905(sc, sc, 1f);
        c.method_51427(st, 0, 0);
        m.method_22909();
    }

    private void pokemon(class_332 c, Premio p, int i, float cx, float cy, float escala, float delta) {
        RenderablePokemon rp = p.renders.get(i);
        if (rp == null) { img(c, "ball", cx, cy, escala * 2.2f, escala * 2.2f, 0, 1f, 0xFFFFFF); return; }
        var m = c.method_51448();
        m.method_22903();
        m.method_46416(cx, cy - 0.935f * escala, 0);   // el modelo de Cobblemon cuelga ~0,35*escala por debajo del punto y mide ~1,17*escala
        var st = p.estados.get(i);
        st.updatePartialTicks(delta);
        float giro = (float) Math.toRadians(35 + (System.nanoTime() - t0) / 1e9 * 45);
        PokemonGuiUtilsKt.drawProfilePokemon(rp, m, new Quaternionf().rotationXYZ((float) Math.toRadians(13), giro, 0f), PoseType.PROFILE, st, delta, escala, true, false, 1f, 1f, 1f, 1f, 0f, 0f);
        m.method_22909();
    }

    /** Dibuja los elementos (Pokémon y objetos) de un premio centrados en (cx, cy); `esc` = 1 tamaño completo. */
    private void visuales(class_332 c, Premio p, float cx, float cy, float esc, float delta) {
        int n = p.visuales();
        float sep = n > 1 ? 320 : 0;
        float x0 = cx - sep * (n - 1) / 2f;
        for (int i = 0; i < n; i++) {
            float vx = x0 + i * sep;
            if (i < p.pokes.size()) pokemon(c, p, i, vx, cy, (n == 1 ? 165 : 130) * esc, delta);
            else objeto(c, p.objetos.get(i - p.pokes.size()), vx, cy, 230 * esc);
            if (i > 0) txt(c, "+", vx - sep / 2f, cy, 64 * esc, ORO_L, true);
        }
    }

    private String nombreVisual(Premio p) {
        return p.nombre.replaceAll(" x(\\d+)$", "  x$1");
    }

    @Override
    public void method_25394(class_332 c, int mx, int my, float delta) {
        double seg = (System.nanoTime() - t0) / 1e9;
        var m = c.method_51448();
        c.method_25294(0, 0, field_22789, field_22790, 0xFF120822);
        m.method_22903();
        m.method_46416(ox(), oy(), 0);
        m.method_22905(k(), k(), 1f);
        double dx = (mx - ox()) / k(), dy = (my - oy()) / k();

        img(c, "fondo", W / 2f, H / 2f, W, H, 0, 1f, 0xFFFFFF);
        dibujarInterfaz(c, dx, dy, delta);

        // ---- animación
        Premio p = idx >= 0 ? cola.get(idx) : null;
        double dur = p == null ? 1 : duracion(p), u = p == null ? 0 : Math.min(1, t() / dur);
        boolean mayor = p != null && p.mayor;
        double rev = 0.55;
        float osc = (float) Math.sin(seg * 2.2);

        if (p != null && mayor) {                                                      // oscurecer fuera del domo
            float osc2 = (float) Math.min(1, u / 0.15) * 0.6f * (u > 0.85 ? (float) ((1 - u) / 0.15) : 1f);
            rect(c, 0, 0, W, H, ((int) (osc2 * 255) << 24));
        }
        if (p == null) {                                                               // reposo (o premio mostrado tras terminar)
            img(c, "halo", BX, BY, 520, 520, 0, 0.35f + 0.08f * osc, 0xC890FF);
            img(c, "rayos", BX, BY, 560, 560, (float) (seg * 0.15), 0.12f, 0xFFE3A0);
            if (!fin) img(c, "ball", BX, BY + 6 * osc, 250, 250, 0.04f * osc, 1f, 0xFFFFFF);
        } else {
            animar(c, p, u, mayor, seg, delta);
        }
        if (fin && ultimo != null) {
            panelPremio(c, ultimo, 1f, delta);
        }
        m.method_22909();
    }

    private void animar(class_332 c, Premio p, double u, boolean mayor, double seg, float delta) {
        double t = t();
        double rev = 0.55;
        if (!mayor) {
            if (u < 0.40) {                                                            // sacudida
                float a = (float) (Math.sin(t * 38) * (0.12 + 0.2 * u / 0.4));
                img(c, "halo", BX, BY, 520, 520, 0, 0.45f, 0xC890FF);
                img(c, "ball", BX + a * 70, BY, 250, 250, a, 1f, 0xFFFFFF);
                if (sonidos < 1) { sonidos = 1; sonido("minecraft:block.note_block.hat", 1.4f); }
            } else if (u < rev) {                                                      // destello
                float f = (float) ((u - 0.40) / (rev - 0.40));
                img(c, "halo", BX, BY, 520 + 700 * f, 520 + 700 * f, 0, 0.95f * (1 - f * 0.4f), 0xFFE6FF);
                img(c, "rayos", BX, BY, 400 + 520 * f, 400 + 520 * f, f * 0.6f, 0.95f * (1 - f * 0.3f), 0xFFF0B0);
                img(c, "ball", BX, BY, 250 * (1 + 0.15f * f), 250 * (1 + 0.15f * f), 0, 1f - f * 0.5f, 0xFFFFFF);
                chispas(c, f, 8, 190 + 160 * f);
                if (sonidos < 2) { sonidos = 2; sonido("minecraft:block.amethyst_block.chime", 1.2f); }
            } else {                                                                   // premio
                float f = (float) ((u - rev) / (1 - rev));
                img(c, "halo", BX, BY, 640, 640, 0, 0.5f * (1 - f * 0.4f), 0xE6C8FF);
                if (sonidos < 3) { sonidos = 3; sonido("minecraft:entity.player.levelup", 1.5f); }
                panelPremio(c, p, (float) Math.min(1, (u - rev) / 0.12), delta);
            }
        } else {
            if (u < 0.15) {                                                            // la bola se vuelve dorada
                float f = (float) (u / 0.15);
                img(c, "halo", BX, BY, 520 + 160 * f, 520 + 160 * f, 0, 0.5f + 0.4f * f, 0xFFC83C);
                img(c, "ball", BX, BY, 250, 250, 0, 1f - f, 0xFFFFFF);
                img(c, "ball_oro", BX, BY, 250, 250, 0, f, 0xFFFFFF);
                if (sonidos < 1) { sonidos = 1; sonido("minecraft:block.beacon.power_select", 1.3f); }
            } else if (u < 0.35) {                                                     // vibra y salen anillos
                float f = (float) ((u - 0.15) / 0.20);
                float a = (float) (Math.sin(t * 55) * (0.18 + 0.2 * f));
                img(c, "halo", BX, BY, 700, 700, 0, 0.9f, 0xFFC83C);
                for (int r = 0; r < 3; r++) {
                    float g = (float) ((f * 1.5 + r * 0.33) % 1.0);
                    img(c, "anillo", BX, BY, 260 + 700 * g, 260 + 700 * g, 0, 0.9f * (1 - g), 0xFFE070);
                }
                img(c, "ball_oro", BX + a * 90, BY, 250, 250, a, 1f, 0xFFFFFF);
                if (sonidos < 2) { sonidos = 2; sonido("minecraft:block.beacon.ambient", 1.6f); }
            } else if (u < rev) {                                                      // columna de luz
                float f = (float) ((u - 0.35) / (rev - 0.35));
                img(c, "halo", BX, BY - 120, 220 + 120 * f, 1500, 0, 0.8f, 0xFFE9A0);
                img(c, "rayos", BX, BY, 520 + 560 * f, 520 + 560 * f, (float) (t * 0.7), 0.9f, 0xFFF0B0);
                img(c, "ball_oro", BX, BY - 40 * f, 250 * (1 + 0.2f * f), 250 * (1 + 0.2f * f), 0, 1f, 0xFFFFFF);
                chispas(c, f, 12, 200 + 200 * f);
                if (sonidos < 3) { sonidos = 3; sonido("minecraft:block.beacon.activate", 1.0f); }
            } else {                                                                   // premio mayor
                float f = (float) ((u - rev) / (1 - rev));
                float flash = (float) Math.max(0, 1 - f / 0.15);
                img(c, "rayos", BX, BY, 1100, 1100, (float) (t * 0.5), 0.55f * (1 - f * 0.3f), 0xFFE070);
                img(c, "halo", BX, BY, 900, 900, 0, 0.7f, 0xFFC83C);
                if (sonidos < 4) { sonidos = 4; sonido("minecraft:ui.toast.challenge_complete", 1.0f); sonido("minecraft:entity.firework_rocket.twinkle", 1.0f); }
                panelPremio(c, p, (float) Math.min(1, f / 0.18), delta);
                lluvia(c, f * 3.0f);
                if (flash > 0) rect(c, 0, 0, W, H, ((int) (flash * 230) << 24) | 0xFFFFFF);
            }
        }
        if (u >= 0.999) siguiente();
    }

    private void chispas(class_332 c, float f, int n, float radio) {
        for (int i = 0; i < n; i++) {
            double a = i * 6.2832 / n + f * 0.8;
            float tam = 34 * (1 - Math.abs(f - 0.5f));
            img(c, "estrella", (float) (BX + Math.cos(a) * radio), (float) (BY + Math.sin(a) * radio * 0.85), tam, tam, (float) a, 1f, 0xFFF0A0);
        }
    }

    private void lluvia(class_332 c, float seg) {
        int[] cols = {0xFFEB96, 0xFF96D2, 0xBE96FF, 0x82E6F0};
        for (float[] p : lluvia) {
            float py = p[1] + p[2] * seg;
            if (py < -30 || py > H + 30) continue;
            float px = p[0] + (float) Math.sin(seg * 3 + p[3]) * 24;
            float tam = 26 + (p[3] * 3) % 14;
            img(c, p[4] == 0 ? "pata" : "estrella", px, py, tam, tam, seg * 2 + p[3], 1f, cols[(int) p[5]]);
        }
    }

    /** Carta con el premio: título, elementos (objeto/Pokémon/peluche) y nombre. */
    private void panelPremio(class_332 c, Premio p, float escala, float delta) {
        float e = 1 - (float) Math.pow(1 - escala, 3);
        e = e * (1 + 0.06f * (float) Math.sin(e * 3.14));
        var m = c.method_51448();
        m.method_22903();
        m.method_46416(BX, BY - 10, 0);
        m.method_22905(Math.max(0.01f, e), Math.max(0.01f, e), 1f);
        int w = p.visuales() > 1 ? 760 : 560, h = 400;
        int borde = p.mayor ? 0xFFFFCD2D : 0xFFFFB8E6;
        rect(c, -w / 2f - 8, -h / 2f - 8, w / 2f + 8, h / 2f + 8, 0xFFB0700A);
        rect(c, -w / 2f - 4, -h / 2f - 4, w / 2f + 4, h / 2f + 4, borde);
        rect(c, -w / 2f, -h / 2f, w / 2f, h / 2f, 0xF4240E44);
        rect(c, -w / 2f + 12, -h / 2f + 12, w / 2f - 12, h / 2f - 12, 0xF42E1556);
        rect(c, -w / 2f + 12, -h / 2f + 12, w / 2f - 12, -h / 2f + 14, borde);
        rect(c, -w / 2f + 12, h / 2f - 14, w / 2f - 12, h / 2f - 12, borde);
        txt(c, p.titulo(), 0, -h / 2f + 40, p.mayor ? 46 : 38, p.mayor ? ORO : ORO_L, true);
        visuales(c, p, 0, -10, 1f, delta);
        txtAncho(c, nombreVisual(p), 0, h / 2f - 62, 28, BLANCO, w - 50);
        txt(c, p.tipo(), 0, h / 2f - 26, 19, p.mayor ? ORO : LILA, true);
        m.method_22909();
    }

    private void interfazBoton(class_332 c, double dx, double dy, int n, boolean activo) {
        int x0 = n == 1 ? 330 : 790;
        boolean sobre = activo && dx >= x0 && dx <= x0 + 410 && dy >= 860 && dy <= 975;
        if (sobre) rect(c, x0 + 10, 870, x0 + 400, 965, 0x38FFFFFF);
        if (!activo) rect(c, x0 + 10, 870, x0 + 400, 965, 0x90100620);
        int col = activo ? BLANCO : 0xFF9C8FB8;
        txt(c, n == 1 ? "TIRAR  x1" : "TIRAR  x10", x0 + 205, 903, 36, col, true);
        txt(c, n == 1 ? "1 ticket" : "10 tickets · 1 clic", x0 + 205, 939, 18, activo ? 0xFFFFEBA0 : 0xFF7F7499, true);
    }

    private void dibujarInterfaz(class_332 c, double dx, double dy, float delta) {
        int tk = tickets();
        boolean libre = !ocupado() && !fin;
        txt(c, "GASHA ESPECIAL DE EVENTOS", BX, 144, 27, ORO_L, true);
        txt(c, "Michialien · Evento", 1285, 80, 28, ORO_L, true);
        // ticket
        txt(c, "TU TICKET", 200, 362, 24, ORO_L, true);
        objeto(c, new class_1799(EmiPlush.TICKET_GASHA), 200, 470, 176);
        txt(c, "x " + tk, 200, 580, 40, tk > 0 ? BLANCO : 0xFFFF8080, true);
        txt(c, "en tu inventario", 200, 616, 17, LILA, true);
        // premios
        txt(c, "PREMIOS", 1285, 182, 22, ORO_L, true);
        String[][] filas = {{"Pokémon Emi", "10%"}, {"Peluches", "7%"}, {"GatitoAlien", "7%"}, {"¡Pareja!", "2%"}, {"Objetos útiles", "74%"}};
        for (int i = 0; i < filas.length; i++) {
            txt(c, filas[i][0], 1154, 222 + i * 38, 22, BLANCO, false);
            txt(c, filas[i][1], 1452 - field_22793.method_1727(filas[i][1]) * 22 / 9f, 222 + i * 38, 22, ORO_L, false);
        }
        // garantía
        txt(c, "GARANTÍA", 1285, 482, 22, ORO_L, true);
        for (int i = 0; i < 10; i++) img(c, "pata", 1128 + i * 35, 553, 30, 30, 0, 1f, i < garantia ? 0xFFD750 : 0x5F3C82);
        txt(c, garantia + " / 10  ·  a la 10ª, pareja segura", 1285, 600, 18, LILA, true);
        // último premio
        txt(c, "ÚLTIMO PREMIO", 1285, 702, 22, ORO_L, true);
        if (ultimo == null) {
            img(c, "emi", 1160, 782, 94, 94, 0, 1f, 0xFFFFFF);
            txt(c, "Todavía no has tirado", 1368, 782, 16, LILA, true);
        } else {
            if (ultimo.visuales() > 0) {
                if (!ultimo.pokes.isEmpty()) pokemon(c, ultimo, 0, 1160, 782, 44, delta);
                else objeto(c, ultimo.objetos.get(0), 1160, 782, 72);
            }
            txtAncho(c, nombreVisual(ultimo).replace(" + su peluche", ""), 1368, 755, 22, BLANCO, 190);
            txtAncho(c, ultimo.tipo(), 1368, 786, 17, ultimo.mayor ? ORO : LILA, 190);
            if (ultimo.mayor) txt(c, "¡Premio mayor!", 1368, 812, 17, ROSA, true);
        }
        interfazBoton(c, dx, dy, 1, libre && tk >= 1);
        interfazBoton(c, dx, dy, 10, libre && tk >= 10);
        txt(c, "X", 1475, 925, 30, BLANCO, true);
        if (fin) txt(c, "Haz clic para continuar", 768, 835, 26, ORO_L, true);
        else if (!error.isEmpty()) txt(c, error, 768, 835, 22, 0xFFFF8080, true);
        else if (esperando) txt(c, "...", 768, 835, 22, LILA, true);
        txt(c, "Cada 10 tiradas sin pareja te aseguro un Pokémon con su peluche", 768, 1000, 17, LILA, true);
    }
}
