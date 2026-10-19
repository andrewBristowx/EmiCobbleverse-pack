package emi.fall.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emi.fall.net.AccionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
 * El menu de la sala de Emi Fall (se abre al entrar, con la estrella del menu o tocando el terminal): JUGAR (elegir el modo —con o sin eliminaciones— y los juegos, jugar solo o
 * abrir un torneo para todos o unirte al que hay), MISIONES (las del dia y las de la semana), TIENDA (con estrellas) y TOP. Los datos los manda el servidor (JSON); cada clic manda
 * una accion al servidor, que lo comprueba todo y vuelve a mandar los datos.
 */
@Environment(EnvType.CLIENT)
public final class MenuPantalla extends Lienzo {
    private static final int T_JUGAR = 0, T_MISIONES = 1, T_TIENDA = 2, T_TOP = 3;
    private static final String[] TABS = {"TORNEO", "MISIONES", "TIENDA", "TOP"};
    private static final String[] TABS_ID = {"jugar", "misiones", "tienda", "top"};
    private static final String[] PRESETS = {"todos", "clasicos", "nuevos"};
    private static final String[] PRESET_NOMBRE = {"LOS 8 JUEGOS", "LOS 4 CLÁSICOS", "LOS 4 DE HALLOWEEN"};
    private static final String[] PRESET_TEXTO = {"Todos, en orden", "Carrera, camino, bombas, plataformas", "Dulces, suelo, muro, calabaza"};

    /** La pantalla abierta ahora (para actualizarla cuando llegan datos nuevos del servidor). */
    public static MenuPantalla abierta;

    // ------------------------------------------------------------ datos
    private record JuegoC(String nombre, String texto) {}
    private record JugadorC(String nombre, boolean yo, boolean host) {}
    private record MisionC(String texto, int prog, int objetivo, int premio, boolean hecha) {}
    private record ArticuloC(String id, String item, String nombre, String texto, int precio, String grupo, int hoy, boolean unico, boolean comprado) {}
    private record MarcaC(int pos, String nombre, int valor, String extra, boolean mio) {}

    private final Map<String, List<JuegoC>> juegos = new HashMap<>();
    private final List<JugadorC> jugadores = new ArrayList<>();
    private final List<MisionC> dia = new ArrayList<>(), semana = new ArrayList<>();
    private final List<ArticuloC> articulos = new ArrayList<>();
    private final List<MarcaC> topGanados = new ArrayList<>(), topEstrellas = new ArrayList<>();
    private final Map<String, ItemStack> pilas = new HashMap<>();
    private JsonObject resultado;
    private String fase = "libre", ronda = "", presetServidor = "todos";
    private boolean apuntado, anfitrion, admin, sinServidor;
    private int maxJugadores = 16, limiteTienda = 10;
    private int estrellas, torneos, ganados, rondasGanadas;
    private int tab, selTop;
    private int selPreset;                    // lo que se ha elegido aqui (cuando no hay torneo abierto)
    private boolean selSin;
    private long ultimoRefresco = System.currentTimeMillis();

    public MenuPantalla(String json) {
        super(Text.literal("Emi Fall"));
        cargar(json, false);
        abierta = this;
    }

    @Override public void removed() { if (abierta == this) abierta = null; super.removed(); }

    /** Carga (o recarga) los datos del servidor, conservando la pestaña y la eleccion mientras se pueda. {@code soloDatos} = es un refresco: no se cambia de pestaña. */
    public void cargar(String json, boolean soloDatos) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        String t = o.has("tab") ? o.get("tab").getAsString() : "jugar";
        if (!soloDatos) for (int i = 0; i < TABS_ID.length; i++) if (TABS_ID[i].equals(t)) tab = i;
        estrellas = o.get("estrellas").getAsInt(); torneos = o.get("torneos").getAsInt(); ganados = o.get("ganados").getAsInt(); rondasGanadas = o.get("rondasGanadas").getAsInt();

        JsonObject c = o.getAsJsonObject("torneo");
        fase = c.get("fase").getAsString();
        maxJugadores = c.get("max").getAsInt();
        apuntado = c.get("apuntado").getAsBoolean(); anfitrion = c.get("anfitrion").getAsBoolean(); admin = c.get("admin").getAsBoolean();
        presetServidor = c.get("preset").getAsString(); sinServidor = c.get("sinElim").getAsBoolean();
        ronda = c.has("ronda") ? c.get("ronda").getAsString() : "";
        jugadores.clear();
        for (JsonElement e : c.getAsJsonArray("jugadores")) {
            JsonObject j = e.getAsJsonObject();
            jugadores.add(new JugadorC(j.get("nombre").getAsString(), j.get("yo").getAsBoolean(), j.get("host").getAsBoolean()));
        }
        juegos.clear();
        for (var en : o.getAsJsonObject("juegos").entrySet()) {
            List<JuegoC> l = new ArrayList<>();
            for (JsonElement e : en.getValue().getAsJsonArray()) { JsonObject j = e.getAsJsonObject(); l.add(new JuegoC(j.get("nombre").getAsString(), j.get("texto").getAsString())); }
            juegos.put(en.getKey(), l);
        }

        misiones(dia, o.getAsJsonArray("dia"));
        misiones(semana, o.getAsJsonArray("semana"));

        articulos.clear();
        JsonObject ti = o.getAsJsonObject("tienda");
        limiteTienda = ti.get("limite").getAsInt();
        for (JsonElement e : ti.getAsJsonArray("articulos")) {
            JsonObject j = e.getAsJsonObject();
            articulos.add(new ArticuloC(j.get("id").getAsString(), j.get("item").getAsString(), j.get("nombre").getAsString(), j.get("texto").getAsString(), j.get("precio").getAsInt(),
                    j.get("grupo").getAsString(), j.get("hoy").getAsInt(), j.get("unico").getAsBoolean(), j.get("comprado").getAsBoolean()));
        }
        tabla(topGanados, o.getAsJsonArray("topGanados"));
        tabla(topEstrellas, o.getAsJsonArray("topEstrellas"));
        if (o.has("resultado")) resultado = o.getAsJsonObject("resultado");
        else if (!soloDatos) resultado = null;
    }

    private static void tabla(List<MarcaC> destino, JsonArray a) {
        destino.clear();
        for (JsonElement e : a) {
            JsonObject j = e.getAsJsonObject();
            destino.add(new MarcaC(j.get("pos").getAsInt(), j.get("nombre").getAsString(), j.get("valor").getAsInt(), j.get("extra").getAsString(), j.get("mio").getAsBoolean()));
        }
    }

    private static void misiones(List<MisionC> destino, JsonArray a) {
        destino.clear();
        for (JsonElement e : a) {
            JsonObject j = e.getAsJsonObject();
            destino.add(new MisionC(j.get("texto").getAsString(), j.get("prog").getAsInt(), j.get("objetivo").getAsInt(), j.get("premio").getAsInt(), j.get("hecha").getAsBoolean()));
        }
    }

    // ------------------------------------------------------------ utilidades

    private void enviar(String accion, String a, String b) { ClientPlayNetworking.send(new AccionPayload(accion, a, b)); }

    private void boton(DrawContext g, int x, int y, int w, int h, String texto, int color, boolean activo, boolean sobre, float tam) {
        int c = activo ? color : 0xFF444455;
        if (activo && sobre) c = 0xFF000000 | (Math.min(255, ((c >> 16) & 255) + 40) << 16) | (Math.min(255, ((c >> 8) & 255) + 40) << 8) | Math.min(255, (c & 255) + 40);
        caja(g, x, y, w, h, c, activo ? 0xFFFFFFFF : 0xFF666677);
        textoCentrado(g, texto, x + w / 2, y + (int) ((h - 8 * tam) / 2), activo ? BLANCO : 0xFF9A9AAA, tam);
    }

    private static String recortar(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "…"; }

    private ItemStack pila(String item) {
        return pilas.computeIfAbsent(item, k -> {
            var it = Registries.ITEM.getOrEmpty(Identifier.of(k));
            return it.isPresent() ? new ItemStack(it.get()) : ItemStack.EMPTY;
        });
    }

    /** El modo y los juegos que se ven ahora: los del torneo abierto si lo hay; si no, lo que has elegido aqui. */
    private boolean modoSin() { return fase.equals("libre") ? selSin : sinServidor; }
    private int presetIdx() {
        if (fase.equals("libre")) return selPreset;
        for (int i = 0; i < PRESETS.length; i++) if (PRESETS[i].equals(presetServidor)) return i;
        return 0;
    }
    /** ¿Se pueden tocar las tarjetas de la izquierda? (sin torneo, o siendo el anfitrion de la cola) */
    private boolean editable() { return fase.equals("libre") || (fase.equals("cola") && (anfitrion || admin)); }

    // ------------------------------------------------------------ dibujo

    private static final int LX = 20, LW = 226, RX = 258, RW = 362, CY = 78;

    @Override
    protected void dibujar(DrawContext g, int mx, int my, float delta) {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoRefresco > 1500) { ultimoRefresco = ahora; enviar("refrescar", TABS_ID[tab], ""); }
        texto(g, "EMI FALL", 20, 10, ORO, 2.2f);
        texto(g, "Minijuegos de Halloween con el Michialien", 22, 32, GRIS, 0.9f);
        texto(g, "★ " + estrellas + " estrellas", 470, 10, 0xFFFFE070, 1.3f);
        texto(g, "Torneos: " + torneos + " · Ganados: " + ganados + " · Rondas ganadas: " + rondasGanadas, 400, 26, GRIS, 0.8f);
        texto(g, "Se ganan jugando y con misiones", 470, 36, LILA, 0.7f);

        for (int i = 0; i < TABS.length; i++) {
            int x = 20 + i * 82;
            boolean act = i == tab, sobre = dentro(mx, my, x, 48, 78, 20);
            caja(g, x, 48, 78, 20, act ? 0xFF3A3A66 : sobre ? 0xFF2A2A4A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
            textoCentrado(g, TABS[i], x + 39, 54, act ? ORO : BLANCO, 1f);
        }
        boton(g, 538, 48, 82, 20, "SALIR", 0xFF8A3A3A, true, dentro(mx, my, 538, 48, 82, 20), 1f);

        switch (tab) {
            case T_JUGAR -> { modos(g, mx, my); jugar(g, mx, my); }
            case T_MISIONES -> misionesTab(g);
            case T_TIENDA -> tienda(g, mx, my);
            default -> top(g, mx, my);
        }
        if (resultado != null) resultadoCaja(g, mx, my);
    }

    // ------------------------------------------------------------ torneo

    private static final int YM = CY, HM = 56, YJ = CY + 134, HJ = 40;

    /** A la izquierda: el modo (con o sin eliminaciones) y los juegos (los 8, los 4 clasicos o los 4 de Halloween). */
    private void modos(DrawContext g, int mx, int my) {
        boolean ed = editable();
        texto(g, "MODO DE JUEGO" + (ed ? "" : "  (lo elige el anfitrión)"), LX, CY - 7, ORO, 0.9f);
        String[][] m = {{"CON ELIMINACIONES", "Si caes en un juego que elimina, miras desde arriba hasta que acabe."},
                {"SIN ELIMINACIONES", "Nadie queda fuera: si caes o te golpean, vuelves a jugar y cuenta como penalización."}};
        for (int i = 0; i < 2; i++) {
            int y = YM + i * (HM + 4);
            boolean act = (i == 1) == modoSin(), sobre = ed && dentro(mx, my, LX, y, LW, HM);
            caja(g, LX, y, LW, HM, act ? 0xFF2E2E58 : sobre ? 0xFF25254A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
            texto(g, (act ? "● " : "○ ") + m[i][0], LX + 8, y + 6, act ? ORO : BLANCO, 1.0f);
            parrafo(g, m[i][1], LX + 8, y + 20, LW - 16, GRIS, 0.75f);
        }
        texto(g, "JUEGOS", LX, YJ - 12, ORO, 0.9f);
        for (int i = 0; i < PRESETS.length; i++) {
            int y = YJ + i * (HJ + 4);
            boolean act = i == presetIdx(), sobre = ed && dentro(mx, my, LX, y, LW, HJ);
            caja(g, LX, y, LW, HJ, act ? 0xFF2E2E58 : sobre ? 0xFF25254A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
            texto(g, (act ? "● " : "○ ") + PRESET_NOMBRE[i], LX + 8, y + 7, act ? ORO : BLANCO, 0.95f);
            texto(g, PRESET_TEXTO[i], LX + 8, y + 21, GRIS, 0.75f);
        }
    }

    private static final int PY = CY + 12;

    private void listaJuegos(DrawContext g, int y) {
        List<JuegoC> l = juegos.getOrDefault(PRESETS[presetIdx()], List.of());
        caja(g, RX, y, RW, 12 + l.size() * 12, 0xFF1B1B30, 0xFF55557A);
        for (int i = 0; i < l.size(); i++) {
            texto(g, (i + 1) + ". " + l.get(i).nombre(), RX + 8, y + 6 + i * 12, BLANCO, 0.85f);
            texto(g, recortar(l.get(i).texto(), 36), RX + 150, y + 6 + i * 12, GRIS, 0.75f);
        }
    }

    private void jugar(DrawContext g, int mx, int my) {
        switch (fase) {
            case "libre" -> {
                texto(g, "NO HAY NINGÚN TORNEO ABIERTO", RX, CY - 7, ORO, 0.9f);
                listaJuegos(g, PY);
                int by = PY + 12 + juegos.getOrDefault(PRESETS[presetIdx()], List.of()).size() * 12 + 10;
                boton(g, RX, by, RW, 26, "JUGAR SOLO (práctica)", 0xFF2E8B57, true, dentro(mx, my, RX, by, RW, 26), 1f);
                boton(g, RX, by + 32, RW, 26, "ABRIR TORNEO PARA TODOS (otros se pueden unir)", 0xFFB8860B, true, dentro(mx, my, RX, by + 32, RW, 26), 0.95f);
                texto(g, "Estrellas: 3 por ronda + 10/6/4/2 según tu puesto; al acabar el torneo +10 y +30/18/10/5 por el puesto final.", RX, by + 66, 0xFF8888AA, 0.65f);
                texto(g, "Practicando solo ganas las estrellas base, sin las del puesto. Las misiones dan estrellas extra.", RX, by + 76, 0xFF8888AA, 0.65f);
            }
            case "cola" -> {
                texto(g, "TORNEO ABIERTO · " + (sinServidor ? "SIN ELIMINACIONES" : "CON ELIMINACIONES"), RX, CY - 7, ORO, 0.9f);
                texto(g, "Jugadores " + jugadores.size() + "/" + maxJugadores + " · el anfitrión (★) lo empieza. Elige el modo a la izquierda.", RX, PY - 2, GRIS, 0.75f);
                for (int i = 0; i < jugadores.size() && i < 8; i++) {
                    JugadorC c = jugadores.get(i);
                    int y = PY + 10 + i * 20;
                    caja(g, RX, y, RW, 18, c.yo() ? 0xFF2E2E58 : 0xFF1B1B30, c.yo() ? ORO : 0xFF55557A);
                    texto(g, (c.host() ? "★ " : "") + recortar(c.nombre(), 24), RX + 8, y + 5, c.yo() ? ORO : BLANCO, 0.95f);
                    texto(g, c.host() ? "anfitrión" : "apuntado", RX + 290, y + 5, c.host() ? ORO : 0xFF8888AA, 0.9f);
                }
                int by = 262;
                if (!apuntado) {
                    boton(g, RX, by, RW, 26, "UNIRME AL TORNEO", 0xFF2E8B57, jugadores.size() < maxJugadores, dentro(mx, my, RX, by, RW, 26), 1f);
                } else {
                    boton(g, RX, by, 178, 26, "SALIR DE LA COLA", 0xFF8A3A3A, true, dentro(mx, my, RX, by, 178, 26), 1f);
                    if (anfitrion || admin) {
                        boton(g, RX + 184, by, 178, 26, "EMPEZAR EL TORNEO", 0xFFB8860B, true, dentro(mx, my, RX + 184, by, 178, 26), 1f);
                        boton(g, RX, by + 30, 178, 24, "CANCELAR TORNEO", 0xFF6A2E2E, true, dentro(mx, my, RX, by + 30, 178, 24), 1f);
                    } else texto(g, "Esperando a que el anfitrión (★) empiece.", RX + 184, by + 9, 0xFF8888AA, 0.8f);
                }
                texto(g, "Al empezar, cada ronda explica en el chat qué hay que hacer.", RX, 336, 0xFF8888AA, 0.75f);
            }
            default -> {
                texto(g, "TORNEO EN MARCHA", RX, CY - 7, ORO, 0.9f);
                caja(g, RX, PY, RW, 70, 0xFF1B1B30, 0xFF55557A);
                parrafo(g, "Hay un torneo en marcha (" + ronda + ") con " + jugadores.size() + " jugador" + (jugadores.size() == 1 ? "" : "es") + ". Espera a que termine para abrir otro.", RX + 12, PY + 14, RW - 24, GRIS, 1f);
            }
        }
    }

    // ------------------------------------------------------------ misiones

    private void misionesTab(DrawContext g) {
        texto(g, "MISIONES", LX, CY - 8, ORO, 1.1f);
        texto(g, "Las mismas para todos. Se renuevan cada día y cada lunes. Dan estrellas al cumplirlas.", LX + 90, CY - 6, GRIS, 0.8f);
        columnaMisiones(g, "DEL DÍA", dia, LX, 0xFF6CC8FF);
        columnaMisiones(g, "DE LA SEMANA", semana, LX + 308, 0xFFC9B3FF);
    }

    private void columnaMisiones(DrawContext g, String titulo, List<MisionC> l, int x, int color) {
        texto(g, titulo, x, CY + 8, color, 0.95f);
        for (int i = 0; i < l.size(); i++) {
            MisionC m = l.get(i);
            int y = CY + 22 + i * 66;
            caja(g, x, y, 296, 60, 0xFF1B1B30, m.hecha() ? VERDE : 0xFF55557A);
            parrafo(g, m.texto(), x + 8, y + 7, 224, m.hecha() ? VERDE : BLANCO, 0.9f);
            texto(g, m.hecha() ? "¡Hecha!" : "+" + m.premio() + " ★", x + 240, y + 7, m.hecha() ? VERDE : 0xFFFFE070, 0.95f);
            int bw = 200;
            g.fill(x + 8, y + 36, x + 8 + bw, y + 43, 0xFF2A2A44);
            g.fill(x + 8, y + 36, x + 8 + (int) (bw * Math.min(1.0, m.objetivo() == 0 ? 1 : m.prog() / (double) m.objetivo())), y + 43, m.hecha() ? VERDE : color);
            texto(g, m.prog() + " / " + m.objetivo(), x + 216, y + 35, GRIS, 0.9f);
            texto(g, m.hecha() ? "Premio ya cobrado" : "Se cobra al acabar la ronda o el torneo en que la cumplas", x + 8, y + 48, 0xFF8888AA, 0.65f);
        }
    }

    // ------------------------------------------------------------ tienda

    private static final int TW = 296, TH = 30, TY = CY + 14, TGAP = 2;

    private int[] posArticulo(int i) {
        if (articulos.get(i).unico()) {
            int k = 0;
            for (int q = 0; q < i; q++) if (articulos.get(q).unico()) k++;
            return new int[]{LX, TY + 6 * (TH + TGAP) + 4 + k * 40};
        }
        int n = 0;
        for (int q = 0; q < i; q++) if (!articulos.get(q).unico()) n++;
        return new int[]{LX + (n / 6) * (TW + 8), TY + (n % 6) * (TH + TGAP)};
    }

    private void tienda(DrawContext g, int mx, int my) {
        texto(g, "TIENDA DE EMI FALL", LX, CY - 2, ORO, 1.2f);
        texto(g, "Tus estrellas: " + estrellas + "   ·   Máximo " + limiteTienda + " de cada artículo al día", LX + 160, CY + 1, 0xFFFFE070, 0.85f);
        for (int i = 0; i < articulos.size(); i++) {
            ArticuloC a = articulos.get(i);
            int[] p = posArticulo(i);
            int x = p[0], y = p[1];
            if (a.unico()) { articuloUnico(g, mx, my, a, x, y); continue; }
            boolean tope = a.hoy() >= limiteTienda, puede = !tope && estrellas >= a.precio();
            caja(g, x, y, TW, TH, 0xFF1B1B30, a.grupo().equals("banner") ? 0xFF8A5AB0 : 0xFF55557A);
            ItemStack s = pila(a.item());
            if (!s.isEmpty()) g.drawItem(s, x + 8, y + 7);
            texto(g, a.nombre(), x + 32, y + 4, BLANCO, 0.95f);
            texto(g, a.texto(), x + 32, y + 14, GRIS, 0.7f);
            texto(g, "Hoy: " + a.hoy() + "/" + limiteTienda, x + 32, y + 22, tope ? ROJO : 0xFF8888AA, 0.7f);
            int bx = x + TW - 92, bw = 86;
            boton(g, bx, y + 4, bw, 22, tope ? "AGOTADO HOY" : a.precio() + " ★", 0xFF7A4AC0, puede, puede && dentro(mx, my, bx, y + 4, bw, 22), 0.9f);
        }
        texto(g, "Lo que compras llega a tu correo (/correo). Las estrellas se ganan jugando torneos y con misiones.", LX, 336, 0xFF8888AA, 0.75f);
    }

    /** El articulo exclusivo (el Michialien disfrazado de calabaza): fila ancha dorada, se paga con estrellas y solo se compra una vez. */
    private void articuloUnico(DrawContext g, int mx, int my, ArticuloC a, int x, int y) {
        int w = 2 * TW + 8, h = 36;
        boolean puede = !a.comprado() && estrellas >= a.precio();
        caja(g, x, y, w, h, 0xFF2A2410, ORO);
        ItemStack s = pila(a.item());
        if (!s.isEmpty()) g.drawItem(s, x + 10, y + 10);
        texto(g, a.nombre() + "  ★ EXCLUSIVO", x + 36, y + 5, ORO, 1.0f);
        texto(g, a.texto(), x + 36, y + 17, GRIS, 0.75f);
        texto(g, a.comprado() ? "¡Ya lo tienes! Mira /correo si aún no lo has recogido." : "Tus estrellas: " + estrellas, x + 36, y + 26, a.comprado() ? VERDE : 0xFF8888AA, 0.7f);
        int bw = 150, bx = x + w - bw - 8;
        boton(g, bx, y + 7, bw, 22, a.comprado() ? "COMPRADO ✔" : a.precio() + " estrellas", 0xFFB8860B, puede, puede && dentro(mx, my, bx, y + 7, bw, 22), 0.9f);
    }

    // ------------------------------------------------------------ top

    private void top(DrawContext g, int mx, int my) {
        texto(g, "LOS MEJORES", LX, CY - 8, ORO, 1.1f);
        String[] nombres = {"TORNEOS GANADOS", "ESTRELLAS GANADAS"};
        for (int i = 0; i < 2; i++) {
            int x = LX + i * 200;
            boolean act = i == selTop, sobre = dentro(mx, my, x, CY + 6, 194, 20);
            caja(g, x, CY + 6, 194, 20, act ? 0xFF3A3A66 : sobre ? 0xFF2A2A4A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
            textoCentrado(g, nombres[i], x + 97, CY + 12, act ? ORO : BLANCO, 0.9f);
        }
        List<MarcaC> l = selTop == 0 ? topGanados : topEstrellas;
        if (l.isEmpty()) { texto(g, selTop == 0 ? "Aún nadie ha ganado un torneo con otros jugadores. ¡Sé el primero!" : "Aún nadie ha ganado estrellas.", LX, CY + 40, GRIS, 1f); return; }
        for (int i = 0; i < l.size(); i++) {
            MarcaC m = l.get(i);
            int y = CY + 32 + i * 21;
            caja(g, LX, y, 600, 19, m.mio() ? 0xFF2E2E58 : 0xFF1B1B30, m.pos() == 1 ? ORO : m.mio() ? ORO : 0xFF55557A);
            texto(g, "#" + m.pos(), LX + 8, y + 6, m.pos() == 1 ? ORO : m.pos() == 2 ? 0xFFC0C0D0 : m.pos() == 3 ? 0xFFCD7F32 : BLANCO, 1f);
            texto(g, recortar(m.nombre(), 24), LX + 50, y + 6, m.mio() ? ORO : BLANCO, 1f);
            texto(g, m.extra(), LX + 330, y + 6, GRIS, 0.9f);
            texto(g, (selTop == 0 ? "♛ " : "★ ") + m.valor(), LX + 520, y + 6, 0xFFFFE070, 1.05f);
        }
    }

    // ------------------------------------------------------------ resultado del torneo

    private void resultadoCaja(DrawContext g, int mx, int my) {
        int x = 130, y = 80, w = 380, h = 220;
        g.draw();
        g.getMatrices().push();
        g.getMatrices().translate(0, 0, 400);
        g.fill(0, 0, W, H, 0xA0000010);
        caja(g, x, y, w, h, 0xFF14142A, ORO);
        int puesto = resultado.get("puesto").getAsInt(), jug = resultado.get("jugadores").getAsInt();
        textoCentrado(g, jug > 1 && puesto == 1 ? "¡HAS GANADO EL TORNEO!" : "TORNEO TERMINADO", x + w / 2, y + 10, ORO, 1.5f);
        textoCentrado(g, (resultado.get("sinElim").getAsBoolean() ? "Sin eliminaciones" : "Con eliminaciones") + " · " + resultado.get("rondas").getAsInt() + " rondas", x + w / 2, y + 30, BLANCO, 0.9f);
        textoCentrado(g, "Puesto " + puesto + " de " + jug + " · " + resultado.get("puntos").getAsInt() + " puntos", x + w / 2, y + 44, LILA, 1f);
        textoCentrado(g, "+" + resultado.get("estrellas").getAsInt() + " ★  (ahora tienes " + resultado.get("saldo").getAsInt() + ")", x + w / 2, y + 60, 0xFFFFE070, 1.2f);
        JsonArray l = resultado.getAsJsonArray("lineas");
        int primera = Math.max(0, l.size() - 8);
        for (int i = primera; i < l.size(); i++) textoCentrado(g, recortar(l.get(i).getAsString(), 78), x + w / 2, y + 82 + (i - primera) * 11, GRIS, 0.75f);
        boton(g, x + w / 2 - 60, y + h - 30, 120, 22, "ACEPTAR", 0xFF2E8B57, true, dentro(mx, my, x + w / 2 - 60, y + h - 30, 120, 22), 1f);
        g.getMatrices().pop();
    }

    // ------------------------------------------------------------ clics

    @Override
    protected void clic(int mx, int my, int boton) {
        if (boton != 0) return;
        if (resultado != null) {
            if (dentro(mx, my, 130 + 190 - 60, 80 + 220 - 30, 120, 22)) resultado = null;
            return;
        }
        if (dentro(mx, my, 538, 48, 82, 20)) { enviar("salir", "", ""); return; }
        for (int i = 0; i < TABS.length; i++) if (dentro(mx, my, 20 + i * 82, 48, 78, 20)) { tab = i; enviar("refrescar", TABS_ID[i], ""); return; }
        switch (tab) {
            case T_JUGAR -> { if (clicModos(mx, my)) return; clicJugar(mx, my); }
            case T_TIENDA -> clicTienda(mx, my);
            case T_TOP -> { for (int i = 0; i < 2; i++) if (dentro(mx, my, LX + i * 200, CY + 6, 194, 20)) selTop = i; }
            default -> { }
        }
    }

    /** Clic en las tarjetas de la izquierda: sin torneo cambia lo elegido aqui; en la cola, el anfitrion se lo cambia al servidor. */
    private boolean clicModos(int mx, int my) {
        if (!editable()) return false;
        for (int i = 0; i < 2; i++) if (dentro(mx, my, LX, YM + i * (HM + 4), LW, HM)) {
            if (fase.equals("libre")) selSin = i == 1; else enviar("config", presetServidor, i == 1 ? "sin" : "con");
            return true;
        }
        for (int i = 0; i < PRESETS.length; i++) if (dentro(mx, my, LX, YJ + i * (HJ + 4), LW, HJ)) {
            if (fase.equals("libre")) selPreset = i; else enviar("config", PRESETS[i], sinServidor ? "sin" : "con");
            return true;
        }
        return false;
    }

    private void clicJugar(int mx, int my) {
        if (fase.equals("libre")) {
            int by = PY + 12 + juegos.getOrDefault(PRESETS[presetIdx()], List.of()).size() * 12 + 10;
            String modo = selSin ? "sin" : "con";
            if (dentro(mx, my, RX, by, RW, 26)) enviar("solo", PRESETS[selPreset], modo);
            else if (dentro(mx, my, RX, by + 32, RW, 26)) enviar("crear", PRESETS[selPreset], modo);
        } else if (fase.equals("cola")) {
            int by = 262;
            if (!apuntado) { if (dentro(mx, my, RX, by, RW, 26) && jugadores.size() < maxJugadores) enviar("unirse", "", ""); return; }
            if (dentro(mx, my, RX, by, 178, 26)) enviar("salirCola", "", "");
            else if ((anfitrion || admin) && dentro(mx, my, RX + 184, by, 178, 26)) enviar("lanzar", "", "");
            else if ((anfitrion || admin) && dentro(mx, my, RX, by + 30, 178, 24)) enviar("cancelar", "", "");
        }
    }

    private void clicTienda(int mx, int my) {
        for (int i = 0; i < articulos.size(); i++) {
            ArticuloC a = articulos.get(i);
            int[] p = posArticulo(i);
            if (a.unico()) {
                int w = 2 * TW + 8, bw = 150, bx = p[0] + w - bw - 8;
                if (dentro(mx, my, bx, p[1] + 7, bw, 22) && !a.comprado() && estrellas >= a.precio()) enviar("tienda_comprar", a.id(), "");
            } else {
                int bx = p[0] + TW - 92;
                if (dentro(mx, my, bx, p[1] + 4, 86, 22) && a.hoy() < limiteTienda && estrellas >= a.precio()) enviar("tienda_comprar", a.id(), "");
            }
        }
    }
}
