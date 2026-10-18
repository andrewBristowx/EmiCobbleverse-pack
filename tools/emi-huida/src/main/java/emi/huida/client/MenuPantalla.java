package emi.huida.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emi.huida.net.AccionPayload;
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
 * El menu de la sala de la huida (se abre al entrar, con el objeto del menu o tocando el terminal): JUGAR (correr solo, abrir una huida para todos o unirte a la que hay),
 * MISIONES (las del dia y las de la semana), LOGROS, TIENDA (con estrellas) y TOP (las mejores puntuaciones de cada nivel). Los datos los manda el servidor (JSON); cada clic manda una
 * accion al servidor, que lo comprueba todo y vuelve a mandar los datos.
 */
@Environment(EnvType.CLIENT)
public final class MenuPantalla extends Lienzo {
    private static final int T_JUGAR = 0, T_MISIONES = 1, T_LOGROS = 2, T_TIENDA = 3, T_TOP = 4;
    private static final String[] TABS = {"HUIDA", "MISIONES", "LOGROS", "TIENDA", "TOP"};
    private static final String[] TABS_ID = {"jugar", "misiones", "logros", "tienda", "top"};

    /** La pantalla abierta ahora (para actualizarla cuando llegan datos nuevos del servidor). */
    public static MenuPantalla abierta;

    // ------------------------------------------------------------ datos
    private record NivelC(String id, String nombre, String texto, int mejor, int escapes) {}
    private record JugadorC(String nombre, boolean yo, boolean host) {}
    private record MisionC(String texto, int prog, int objetivo, int premio, boolean hecha) {}
    private record LogroC(String nombre, String texto, int hechos, int total, long valor, long meta, String valorTxt, String metaTxt, int premio, boolean completo) {}
    private record ArticuloC(String id, String item, String nombre, String texto, int precio, String grupo, int hoy, boolean unico, boolean comprado) {}
    private record MarcaC(int pos, String nombre, int puntos, int errores, String tiempo, boolean mio) {}

    private final List<NivelC> niveles = new ArrayList<>();
    private final List<JugadorC> jugadores = new ArrayList<>();
    private final List<MisionC> dia = new ArrayList<>(), semana = new ArrayList<>();
    private final List<LogroC> logros = new ArrayList<>();
    private final List<ArticuloC> articulos = new ArrayList<>();
    private final Map<String, List<MarcaC>> tops = new HashMap<>();
    private final Map<String, ItemStack> pilas = new HashMap<>();
    private JsonObject resultado;
    private String fase = "libre", hNivel = "", hNivelNombre = "";
    private boolean apuntado, anfitrion;
    private int maxJugadores = 8, limiteTienda = 10;
    private int estrellas, huidas, escapes, victorias;
    private int tab, selNivel, selTop;
    private long ultimoRefresco = System.currentTimeMillis();

    public MenuPantalla(String json) {
        super(Text.literal("Emi Huida"));
        cargar(json, false);
        abierta = this;
    }

    @Override public void removed() { if (abierta == this) abierta = null; super.removed(); }

    /** Carga (o recarga) los datos del servidor, conservando la pestaña y la eleccion mientras se pueda. {@code soloDatos} = es un refresco: no se cambia de pestaña. */
    public void cargar(String json, boolean soloDatos) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        String t = o.has("tab") ? o.get("tab").getAsString() : "jugar";
        if (!soloDatos) for (int i = 0; i < TABS_ID.length; i++) if (TABS_ID[i].equals(t)) tab = i;
        estrellas = o.get("estrellas").getAsInt(); huidas = o.get("huidas").getAsInt(); escapes = o.get("escapes").getAsInt(); victorias = o.get("victorias").getAsInt();

        niveles.clear();
        for (JsonElement e : o.getAsJsonArray("niveles")) {
            JsonObject j = e.getAsJsonObject();
            niveles.add(new NivelC(j.get("id").getAsString(), j.get("nombre").getAsString(), j.get("texto").getAsString(), j.get("mejor").getAsInt(), j.get("escapes").getAsInt()));
        }
        if (selNivel >= niveles.size()) selNivel = 0;

        JsonObject c = o.getAsJsonObject("huida");
        fase = c.get("fase").getAsString();
        hNivel = c.has("nivel") ? c.get("nivel").getAsString() : ""; hNivelNombre = c.has("nivelNombre") ? c.get("nivelNombre").getAsString() : "";
        maxJugadores = c.get("max").getAsInt();
        apuntado = c.get("apuntado").getAsBoolean(); anfitrion = c.get("anfitrion").getAsBoolean();
        jugadores.clear();
        for (JsonElement e : c.getAsJsonArray("jugadores")) {
            JsonObject j = e.getAsJsonObject();
            jugadores.add(new JugadorC(j.get("nombre").getAsString(), j.get("yo").getAsBoolean(), j.get("host").getAsBoolean()));
        }

        misiones(dia, o.getAsJsonArray("dia"));
        misiones(semana, o.getAsJsonArray("semana"));

        logros.clear();
        for (JsonElement e : o.getAsJsonArray("logros")) {
            JsonObject j = e.getAsJsonObject();
            logros.add(new LogroC(j.get("nombre").getAsString(), j.get("texto").getAsString(), j.get("hechos").getAsInt(), j.get("total").getAsInt(), j.get("valor").getAsLong(), j.get("meta").getAsLong(),
                    j.get("valorTxt").getAsString(), j.get("metaTxt").getAsString(), j.get("premio").getAsInt(), j.get("completo").getAsBoolean()));
        }
        articulos.clear();
        JsonObject ti = o.getAsJsonObject("tienda");
        limiteTienda = ti.get("limite").getAsInt();
        for (JsonElement e : ti.getAsJsonArray("articulos")) {
            JsonObject j = e.getAsJsonObject();
            articulos.add(new ArticuloC(j.get("id").getAsString(), j.get("item").getAsString(), j.get("nombre").getAsString(), j.get("texto").getAsString(), j.get("precio").getAsInt(),
                    j.get("grupo").getAsString(), j.get("hoy").getAsInt(), j.get("unico").getAsBoolean(), j.get("comprado").getAsBoolean()));
        }
        tops.clear();
        for (var en : o.getAsJsonObject("tops").entrySet()) {
            List<MarcaC> l = new ArrayList<>();
            for (JsonElement e : en.getValue().getAsJsonArray()) {
                JsonObject j = e.getAsJsonObject();
                l.add(new MarcaC(j.get("pos").getAsInt(), j.get("nombre").getAsString(), j.get("puntos").getAsInt(), j.get("errores").getAsInt(), j.get("tiempo").getAsString(), j.get("mio").getAsBoolean()));
            }
            tops.put(en.getKey(), l);
        }
        if (o.has("resultado")) resultado = o.getAsJsonObject("resultado");
        else if (!soloDatos) resultado = null;
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

    // ------------------------------------------------------------ dibujo

    private static final int LX = 20, LW = 226, RX = 258, RW = 362, CY = 78;

    @Override
    protected void dibujar(DrawContext g, int mx, int my, float delta) {
        // cada segundo y medio se pide al servidor que refresque (por si algo cambia sin avisar)
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoRefresco > 1500) { ultimoRefresco = ahora; enviar("refrescar", TABS_ID[tab], ""); }
        // cabecera
        texto(g, "EMI HUIDA", 20, 10, ORO, 2.2f);
        texto(g, "Corre, salta y no te dejes atrapar", 22, 32, GRIS, 0.9f);
        texto(g, "★ " + estrellas + " estrellas", 470, 10, 0xFFFFE070, 1.3f);
        texto(g, "Huidas: " + huidas + " · Escapes: " + escapes + " · Victorias: " + victorias, 470, 26, GRIS, 0.8f);
        texto(g, "Se ganan corriendo, con misiones y logros", 470, 36, LILA, 0.7f);

        // pestañas y salir
        for (int i = 0; i < TABS.length; i++) {
            int x = 20 + i * 82;
            boolean act = i == tab, sobre = dentro(mx, my, x, 48, 78, 20);
            caja(g, x, 48, 78, 20, act ? 0xFF3A3A66 : sobre ? 0xFF2A2A4A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
            textoCentrado(g, TABS[i], x + 39, 54, act ? ORO : BLANCO, 1f);
        }
        boton(g, 538, 48, 82, 20, "SALIR", 0xFF8A3A3A, true, dentro(mx, my, 538, 48, 82, 20), 1f);

        switch (tab) {
            case T_JUGAR -> { niveles(g, mx, my); jugar(g, mx, my); }
            case T_MISIONES -> misionesTab(g);
            case T_LOGROS -> logrosTab(g);
            case T_TIENDA -> tienda(g, mx, my);
            default -> top(g, mx, my);
        }
        if (resultado != null) resultadoCaja(g, mx, my);
    }

    // ------------------------------------------------------------ huida

    private static final int NH = 78;

    /** Los tres niveles (a la izquierda en HUIDA): se elige uno para abrir o correr. */
    private void niveles(DrawContext g, int mx, int my) {
        texto(g, "ELIGE EL NIVEL", LX, CY - 7, ORO, 0.9f);
        for (int i = 0; i < niveles.size(); i++) {
            NivelC n = niveles.get(i);
            int y = CY + i * (NH + 4);
            boolean act = i == selNivel, sobre = dentro(mx, my, LX, y, LW, NH);
            caja(g, LX, y, LW, NH, act ? 0xFF2E2E58 : sobre ? 0xFF25254A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
            texto(g, (i + 1) + ". " + n.nombre(), LX + 8, y + 6, act ? ORO : BLANCO, 1.0f);
            parrafo(g, n.texto(), LX + 8, y + 20, LW - 16, GRIS, 0.75f);
            texto(g, n.mejor() < 0 ? "Aún no has escapado" : "Tu mejor: ★ " + n.mejor() + " puntos", LX + 8, y + 52, n.mejor() < 0 ? 0xFF8888AA : 0xFFFFE070, 0.85f);
            texto(g, "Escapes: " + n.escapes(), LX + 8, y + 64, GRIS, 0.75f);
        }
    }

    private static final int PY = CY + 12;

    private void jugar(DrawContext g, int mx, int my) {
        NivelC sel = niveles.isEmpty() ? null : niveles.get(selNivel);
        switch (fase) {
            case "libre" -> {
                texto(g, "NO HAY NINGUNA HUIDA ABIERTA", RX, CY - 7, ORO, 0.9f);
                caja(g, RX, PY, RW, 92, 0xFF1B1B30, 0xFF55557A);
                parrafo(g, "Corres por tu propia copia del nivel mientras algo enorme te persigue: salta, agáchate (shift) y esquiva. Recoge michiestrellas (dan puntos) y llega a la meta sin que te alcance.", RX + 10, PY + 8, RW - 20, GRIS, 0.85f);
                parrafo(g, "Cada tropiezo o caída cuenta como un error y con el tercero te atrapan. ¡Sin errores consigues la medalla de oro!", RX + 10, PY + 54, RW - 20, 0xFFFFE070, 0.85f);
                int by = PY + 100;
                String nom = sel == null ? "—" : sel.nombre().toUpperCase();
                boton(g, RX, by, RW, 26, "CORRER SOLO · " + recortar(nom, 34), 0xFF2E8B57, sel != null, sel != null && dentro(mx, my, RX, by, RW, 26), 1f);
                boton(g, RX, by + 32, RW, 26, "ABRIR HUIDA PARA TODOS (otros se pueden unir)", 0xFFB8860B, sel != null, sel != null && dentro(mx, my, RX, by + 32, RW, 26), 0.95f);
                texto(g, "Premio: 10 ★ por escapar (3 si te alcanzan) + medalla (oro 10, plata 5) + 1 ★ por cada 5 puntos", RX, by + 66, 0xFF8888AA, 0.7f);
                texto(g, "con otros jugadores: +20/12/8/4 ★ según tu puesto · +5 ★ por batir tu mejor puntuación", RX, by + 76, 0xFF8888AA, 0.7f);
            }
            case "cola" -> {
                texto(g, "HUIDA ABIERTA · " + hNivelNombre.toUpperCase(), RX, CY - 7, ORO, 0.9f);
                texto(g, "Jugadores " + jugadores.size() + "/" + maxJugadores + " · el anfitrión (★) la empieza; cada uno corre por su pista", RX, PY - 2, GRIS, 0.8f);
                for (int i = 0; i < jugadores.size() && i < 8; i++) {
                    JugadorC c = jugadores.get(i);
                    int y = PY + 10 + i * 20;
                    caja(g, RX, y, RW, 18, c.yo() ? 0xFF2E2E58 : 0xFF1B1B30, c.yo() ? ORO : 0xFF55557A);
                    texto(g, (c.host() ? "★ " : "") + recortar(c.nombre(), 24), RX + 8, y + 5, c.yo() ? ORO : BLANCO, 0.95f);
                    texto(g, c.host() ? "anfitrión" : "apuntado", RX + 290, y + 5, c.host() ? ORO : 0xFF8888AA, 0.9f);
                }
                int by = 262;
                if (!apuntado) {
                    boton(g, RX, by, RW, 26, "UNIRME A LA HUIDA", 0xFF2E8B57, jugadores.size() < maxJugadores, dentro(mx, my, RX, by, RW, 26), 1f);
                } else {
                    boton(g, RX, by, 178, 26, "SALIR DE LA COLA", 0xFF8A3A3A, true, dentro(mx, my, RX, by, 178, 26), 1f);
                    if (anfitrion) {
                        boton(g, RX + 184, by, 178, 26, "EMPEZAR LA HUIDA", 0xFFB8860B, true, dentro(mx, my, RX + 184, by, 178, 26), 1f);
                        boton(g, RX, by + 30, 178, 24, "CANCELAR HUIDA", 0xFF6A2E2E, true, dentro(mx, my, RX, by + 30, 178, 24), 1f);
                    } else texto(g, "Esperando a que el anfitrión (★) empiece.", RX + 184, by + 9, 0xFF8888AA, 0.8f);
                }
                texto(g, "Al empezar vas a tu pista: confirma «listo» con la tecla R.", RX, 336, 0xFF8888AA, 0.75f);
            }
            default -> {
                texto(g, "HUIDA EN MARCHA", RX, CY - 7, ORO, 0.9f);
                caja(g, RX, PY, RW, 70, 0xFF1B1B30, 0xFF55557A);
                parrafo(g, "Hay una huida en «" + hNivelNombre + "» con " + jugadores.size() + " jugador" + (jugadores.size() == 1 ? "" : "es") + ". Espera a que termine para abrir otra.", RX + 12, PY + 14, RW - 24, GRIS, 1f);
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
            texto(g, recortar(m.texto(), 38), x + 8, y + 7, m.hecha() ? VERDE : BLANCO, 1.0f);
            texto(g, m.hecha() ? "¡Hecha!" : "+" + m.premio() + " ★", x + 240, y + 7, m.hecha() ? VERDE : 0xFFFFE070, 0.95f);
            int bw = 200;
            g.fill(x + 8, y + 30, x + 8 + bw, y + 38, 0xFF2A2A44);
            g.fill(x + 8, y + 30, x + 8 + (int) (bw * Math.min(1.0, m.objetivo() == 0 ? 1 : m.prog() / (double) m.objetivo())), y + 38, m.hecha() ? VERDE : color);
            texto(g, m.prog() + " / " + m.objetivo(), x + 216, y + 30, GRIS, 0.9f);
            texto(g, m.hecha() ? "Premio ya cobrado" : "Se cobra al acabar la huida en la que la cumplas", x + 8, y + 44, 0xFF8888AA, 0.7f);
        }
    }

    // ------------------------------------------------------------ logros

    private void logrosTab(DrawContext g) {
        texto(g, "LOGROS", LX, CY - 8, ORO, 1.1f);
        texto(g, "Cada escalón da estrellas. Los logros se consiguen solos al jugar.", LX + 70, CY - 6, GRIS, 0.8f);
        for (int i = 0; i < logros.size(); i++) {
            LogroC l = logros.get(i);
            int x = LX + (i / 6) * 308, y = CY + 6 + (i % 6) * 40;
            caja(g, x, y, 296, 37, 0xFF1B1B30, l.completo() ? ORO : 0xFF55557A);
            texto(g, l.nombre() + "  " + l.hechos() + "/" + l.total(), x + 8, y + 4, l.completo() ? ORO : BLANCO, 0.95f);
            texto(g, recortar(l.texto(), 56), x + 8, y + 15, GRIS, 0.75f);
            int bw = 190;
            g.fill(x + 8, y + 27, x + 8 + bw, y + 32, 0xFF2A2A44);
            g.fill(x + 8, y + 27, x + 8 + (int) (bw * Math.min(1.0, l.meta() == 0 ? 1 : l.valor() / (double) l.meta())), y + 32, l.completo() ? ORO : VERDE);
            texto(g, l.valorTxt() + " / " + l.metaTxt(), x + 204, y + 26, GRIS, 0.75f);
            texto(g, l.completo() ? "¡Completo!" : "+" + l.premio() + " ★", x + 244, y + 4, l.completo() ? VERDE : 0xFFFFE070, 0.9f);
        }
    }

    // ------------------------------------------------------------ tienda

    private static final int TW = 296, TH = 30, TY = CY + 14, TGAP = 2;

    /** Donde va el articulo numero {@code i}: los normales en dos columnas de seis filas; el unico (el peluche) en una fila ancha debajo. */
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
        texto(g, "TIENDA DE LA HUIDA", LX, CY - 2, ORO, 1.2f);
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
        if (articulos.isEmpty()) parrafo(g, "La tienda no está disponible.", LX, CY + 30, 400, GRIS, 1f);
        texto(g, "Lo que compras llega a tu correo (/correo). Las estrellas se ganan corriendo, con misiones y con logros.", LX, 336, 0xFF8888AA, 0.75f);
    }

    /** El articulo exclusivo (el Michialien con pijama de dinosaurio): fila ancha dorada, se paga con estrellas y solo se compra una vez. */
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
        texto(g, "MEJORES PUNTUACIONES", LX, CY - 8, ORO, 1.1f);
        texto(g, "Solo cuentan las huidas en las que escapas. Gana quien más michiestrellas consigue.", LX + 150, CY - 6, GRIS, 0.75f);
        for (int i = 0; i < niveles.size(); i++) {
            int x = LX + i * 200;
            boolean act = i == selTop, sobre = dentro(mx, my, x, CY + 6, 194, 20);
            caja(g, x, CY + 6, 194, 20, act ? 0xFF3A3A66 : sobre ? 0xFF2A2A4A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
            textoCentrado(g, recortar((i + 1) + ". " + niveles.get(i).nombre(), 30), x + 97, CY + 12, act ? ORO : BLANCO, 0.8f);
        }
        if (niveles.isEmpty()) return;
        List<MarcaC> l = tops.getOrDefault(niveles.get(Math.min(selTop, niveles.size() - 1)).id(), List.of());
        if (l.isEmpty()) { texto(g, "Aún nadie ha escapado de este nivel. ¡Sé el primero!", LX, CY + 40, GRIS, 1f); return; }
        for (int i = 0; i < l.size(); i++) {
            MarcaC m = l.get(i);
            int y = CY + 32 + i * 21;
            caja(g, LX, y, 600, 19, m.mio() ? 0xFF2E2E58 : 0xFF1B1B30, m.pos() == 1 ? ORO : m.mio() ? ORO : 0xFF55557A);
            texto(g, "#" + m.pos(), LX + 8, y + 6, m.pos() == 1 ? ORO : m.pos() == 2 ? 0xFFC0C0D0 : m.pos() == 3 ? 0xFFCD7F32 : BLANCO, 1f);
            texto(g, recortar(m.nombre(), 24), LX + 50, y + 6, m.mio() ? ORO : BLANCO, 1f);
            texto(g, m.errores() + " errores · " + m.tiempo(), LX + 330, y + 6, GRIS, 0.9f);
            texto(g, "★ " + m.puntos(), LX + 520, y + 6, 0xFFFFE070, 1.05f);
        }
    }

    // ------------------------------------------------------------ resultado de la huida

    private void resultadoCaja(DrawContext g, int mx, int my) {
        int x = 130, y = 90, w = 380, h = 200;
        g.draw();                                   // el texto de debajo se dibuja al final: se vacia antes para que la caja lo tape
        g.getMatrices().push();
        g.getMatrices().translate(0, 0, 400);       // y la caja va por delante en profundidad (si no, el texto de debajo se cuela por encima)
        g.fill(0, 0, W, H, 0xA0000010);
        caja(g, x, y, w, h, 0xFF14142A, ORO);
        boolean escapo = resultado.get("escapo").getAsBoolean();
        int puesto = resultado.get("puesto").getAsInt(), jug = resultado.get("jugadores").getAsInt();
        textoCentrado(g, escapo ? (jug > 1 && puesto == 1 ? "¡HAS GANADO!" : "¡ESCAPASTE!") : "TE ALCANZARON", x + w / 2, y + 10, escapo ? ORO : ROJO, 1.6f);
        textoCentrado(g, resultado.get("nivel").getAsString() + " · " + resultado.get("tiempo").getAsString(), x + w / 2, y + 30, BLANCO, 0.95f);
        textoCentrado(g, "★ " + resultado.get("puntos").getAsInt() + " puntos · " + resultado.get("errores").getAsInt() + " errores", x + w / 2, y + 42, LILA, 0.95f);
        if (jug > 1) textoCentrado(g, "Puesto " + puesto + " de " + jug, x + w / 2, y + 54, LILA, 1f);
        if (resultado.get("record").getAsBoolean()) textoCentrado(g, "¡Nueva mejor puntuación!", x + w / 2, y + 66, VERDE, 1f);
        textoCentrado(g, "+" + resultado.get("estrellas").getAsInt() + " ★  (ahora tienes " + resultado.get("saldo").getAsInt() + ")", x + w / 2, y + 80, 0xFFFFE070, 1.2f);
        JsonArray l = resultado.getAsJsonArray("lineas");
        for (int i = 0; i < l.size() && i < 6; i++) textoCentrado(g, recortar(l.get(i).getAsString(), 78), x + w / 2, y + 100 + i * 11, GRIS, 0.75f);
        boton(g, x + w / 2 - 60, y + h - 30, 120, 22, "ACEPTAR", 0xFF2E8B57, true, dentro(mx, my, x + w / 2 - 60, y + h - 30, 120, 22), 1f);
        g.getMatrices().pop();
    }

    // ------------------------------------------------------------ clics

    @Override
    protected void clic(int mx, int my, int boton) {
        if (boton != 0) return;
        if (resultado != null) {
            if (dentro(mx, my, 130 + 190 - 60, 90 + 200 - 30, 120, 22)) resultado = null;
            return;
        }
        if (dentro(mx, my, 538, 48, 82, 20)) { enviar("salir", "", ""); return; }
        for (int i = 0; i < TABS.length; i++) if (dentro(mx, my, 20 + i * 82, 48, 78, 20)) { tab = i; enviar("refrescar", TABS_ID[i], ""); return; }
        switch (tab) {
            case T_JUGAR -> { if (clicNiveles(mx, my)) return; clicJugar(mx, my); }
            case T_TIENDA -> clicTienda(mx, my);
            case T_TOP -> { for (int i = 0; i < niveles.size(); i++) if (dentro(mx, my, LX + i * 200, CY + 6, 194, 20)) selTop = i; }
            default -> { }
        }
    }

    private boolean clicNiveles(int mx, int my) {
        for (int i = 0; i < niveles.size(); i++) if (dentro(mx, my, LX, CY + i * (NH + 4), LW, NH)) { selNivel = i; return true; }
        return false;
    }

    private void clicJugar(int mx, int my) {
        if (fase.equals("libre")) {
            int by = PY + 100;
            if (niveles.isEmpty()) return;
            String id = niveles.get(selNivel).id();
            if (dentro(mx, my, RX, by, RW, 26)) enviar("solo", id, "");
            else if (dentro(mx, my, RX, by + 32, RW, 26)) enviar("crear", id, "");
        } else if (fase.equals("cola")) {
            int by = 262;
            if (!apuntado) { if (dentro(mx, my, RX, by, RW, 26) && jugadores.size() < maxJugadores) enviar("unirse", "", ""); return; }
            if (dentro(mx, my, RX, by, 178, 26)) enviar("salirCola", "", "");
            else if (anfitrion && dentro(mx, my, RX + 184, by, 178, 26)) enviar("lanzar", "", "");
            else if (anfitrion && dentro(mx, my, RX, by + 30, 178, 24)) enviar("cancelar", "", "");
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
