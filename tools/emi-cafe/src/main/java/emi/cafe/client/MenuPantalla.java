package emi.cafe.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emi.cafe.net.AccionPayload;
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
 * El menu del Cafe Michialien (se abre con la caja registradora o con {@code /cafe menu}), con el mismo estilo que el de Emi Fall y los demas juegos: CAFE (elegir cocinero, abrir el
 * turno, invitar), RECETARIO, TIENDA (platos y mejoras, con monedas), ESTRELLAS (tickets y objetos con estrellas y el peluche del Michialien chef), MISIONES y LOGROS. Los datos los manda
 * el servidor (JSON); cada clic manda una accion al servidor, que lo comprueba todo y vuelve a mandar los datos.
 */
@Environment(EnvType.CLIENT)
public final class MenuPantalla extends Lienzo {
    private static final int T_CAFE = 0, T_RECETARIO = 1, T_TIENDA = 2, T_ESTRELLAS = 3, T_MISIONES = 4, T_LOGROS = 5, T_DECORAR = 6;
    private static final String[] TABS = {"CAFÉ", "RECETARIO", "TIENDA", "ESTRELLAS", "MISIONES", "LOGROS", "DECORAR"};
    private static final String[] TABS_ID = {"cafe", "recetario", "tienda", "estrellas", "misiones", "logros", "decorar"};
    private static final int FONDO = 0xFF1B1B30, BORDE = 0xFF55557A, ACTIVO = 0xFF2E2E58, SUAVE = 0xFF8888AA, MONEDA = 0xFFFFD34E, ESTRELLA = 0xFFFFE070;

    /** La pantalla abierta ahora (para actualizarla cuando llegan datos nuevos del servidor). */
    public static MenuPantalla abierta;

    // ------------------------------------------------------------ datos
    private record DecoC(String id, String item, String nombre, int cantidad, int precio, String grupo) {}
    private record InicialC(String id, String nombre, String item, String plato, String receta) {}
    private record PlatoC(String id, String item, String nombre, int precio, int coste, String receta, boolean tengo, boolean especial, String michi) {}
    private record MejoraC(String id, String item, String nombre, String texto, int nivel, int max, int precio) {}
    private record EstiloC(String parte, String nombre) {}
    private record GenteC(String nombre, boolean yo, boolean host) {}
    private record MisionC(String texto, int prog, int objetivo, int premio, boolean hecha) {}
    private record LogroC(String nombre, String texto, int nivel, int max, long valor, long meta, int premio) {}
    private record ArticuloC(String id, String item, String nombre, String texto, int precio, String grupo, int hoy, boolean unico, boolean comprado) {}

    private final List<InicialC> iniciales = new ArrayList<>();
    private final List<PlatoC> platos = new ArrayList<>();            // todos, para el recetario
    private final List<PlatoC> tiendaPlatos = new ArrayList<>();      // los que se compran (sin los de visitantes especiales)
    private final List<MejoraC> mejoras = new ArrayList<>();
    private final List<DecoC> deco = new ArrayList<>();
    private boolean construyendo;
    private int mesasUsadas, mesasMax, pagDeco;
    private final List<EstiloC> estilo = new ArrayList<>();
    private int estacionesEnMano;
    private final List<GenteC> gente = new ArrayList<>();
    private final List<String> invitables = new ArrayList<>();
    private final List<MisionC> dia = new ArrayList<>(), semana = new ArrayList<>();
    private final List<LogroC> logros = new ArrayList<>();
    private final List<ArticuloC> articulos = new ArrayList<>();
    private final Map<String, ItemStack> pilas = new HashMap<>();
    private JsonObject resumen;
    private String estado = "preparando", inicial = "", nombreDuenio = "";
    private boolean duenio, lleno;
    private int monedas, estrellas, clientes, turnos, mejorTurno, platosLimpios, limiteTienda = 10;
    private int tab;
    private int pagRec, pagTienda;                    // pagina del recetario (6 platos) y de los platos de la tienda (3)
    private long ultimoRefresco = System.currentTimeMillis();

    public MenuPantalla(String json) {
        super(Text.literal("Café Michialien"));
        cargar(json, false);
        abierta = this;
    }

    @Override public void removed() { if (abierta == this) abierta = null; super.removed(); }

    /** Carga (o recarga) los datos del servidor, conservando la pestaña mientras se pueda. {@code soloDatos} = es un refresco: no se cambia de pestaña. */
    public void cargar(String json, boolean soloDatos) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        String t = o.has("tab") ? o.get("tab").getAsString() : "cafe";
        if (!soloDatos) for (int i = 0; i < TABS_ID.length; i++) if (TABS_ID[i].equals(t)) tab = i;
        monedas = o.get("monedas").getAsInt(); estrellas = o.get("estrellas").getAsInt(); clientes = o.get("clientes").getAsInt();
        turnos = o.get("turnos").getAsInt(); mejorTurno = o.get("mejorTurno").getAsInt(); platosLimpios = o.get("platosLimpios").getAsInt();
        duenio = o.get("duenio").getAsBoolean(); lleno = o.get("lleno").getAsBoolean();
        nombreDuenio = o.get("nombreDuenio").getAsString(); estado = o.get("estado").getAsString(); inicial = o.get("inicial").getAsString();

        iniciales.clear();
        for (JsonElement e : o.getAsJsonArray("iniciales")) { JsonObject j = e.getAsJsonObject(); iniciales.add(new InicialC(s(j, "id"), s(j, "nombre"), s(j, "item"), s(j, "plato"), s(j, "receta"))); }
        platos.clear();
        for (JsonElement e : o.getAsJsonArray("platos")) {
            JsonObject j = e.getAsJsonObject();
            platos.add(new PlatoC(s(j, "id"), s(j, "item"), s(j, "nombre"), j.get("precio").getAsInt(), j.get("coste").getAsInt(), s(j, "receta"), j.get("tengo").getAsBoolean(), j.has("especial") && j.get("especial").getAsBoolean(), j.has("michi") ? s(j, "michi") : ""));
        }
        construyendo = o.has("construyendo") && o.get("construyendo").getAsBoolean();
        mesasUsadas = o.has("mesasUsadas") ? o.get("mesasUsadas").getAsInt() : 0; mesasMax = o.has("mesasMax") ? o.get("mesasMax").getAsInt() : 0;
        deco.clear();
        if (o.has("deco")) for (JsonElement e : o.getAsJsonArray("deco")) { JsonObject j = e.getAsJsonObject(); deco.add(new DecoC(s(j, "id"), s(j, "item"), s(j, "nombre"), j.get("cantidad").getAsInt(), j.get("precio").getAsInt(), s(j, "grupo"))); }
        estilo.clear();
        if (o.has("estilo")) for (JsonElement e : o.getAsJsonArray("estilo")) { JsonObject j = e.getAsJsonObject(); estilo.add(new EstiloC(s(j, "parte"), s(j, "nombre"))); }
        estacionesEnMano = o.has("estacionesEnMano") ? o.get("estacionesEnMano").getAsInt() : 0;
        tiendaPlatos.clear();
        for (PlatoC p : platos) if (!p.especial()) tiendaPlatos.add(p);
        mejoras.clear();
        for (JsonElement e : o.getAsJsonArray("mejoras")) {
            JsonObject j = e.getAsJsonObject();
            mejoras.add(new MejoraC(s(j, "id"), s(j, "item"), s(j, "nombre"), s(j, "texto"), j.get("nivel").getAsInt(), j.get("max").getAsInt(), j.get("precio").getAsInt()));
        }
        gente.clear();
        for (JsonElement e : o.getAsJsonArray("gente")) { JsonObject j = e.getAsJsonObject(); gente.add(new GenteC(s(j, "nombre"), j.get("yo").getAsBoolean(), j.get("host").getAsBoolean())); }
        invitables.clear();
        for (JsonElement e : o.getAsJsonArray("invitables")) invitables.add(s(e.getAsJsonObject(), "nombre"));

        misiones(dia, o.getAsJsonArray("dia"));
        misiones(semana, o.getAsJsonArray("semana"));
        logros.clear();
        for (JsonElement e : o.getAsJsonArray("logros")) {
            JsonObject j = e.getAsJsonObject();
            logros.add(new LogroC(s(j, "nombre"), s(j, "texto"), j.get("nivel").getAsInt(), j.get("max").getAsInt(), j.get("valor").getAsLong(), j.get("meta").getAsLong(), j.get("premio").getAsInt()));
        }
        articulos.clear();
        JsonObject ti = o.getAsJsonObject("tienda");
        limiteTienda = ti.get("limite").getAsInt();
        for (JsonElement e : ti.getAsJsonArray("articulos")) {
            JsonObject j = e.getAsJsonObject();
            articulos.add(new ArticuloC(s(j, "id"), s(j, "item"), s(j, "nombre"), s(j, "texto"), j.get("precio").getAsInt(), s(j, "grupo"), j.get("hoy").getAsInt(), j.get("unico").getAsBoolean(), j.get("comprado").getAsBoolean()));
        }
        if (o.has("resumen")) resumen = o.getAsJsonObject("resumen");
        else if (!soloDatos) resumen = null;
    }

    private static String s(JsonObject j, String k) { return j.get(k).getAsString(); }

    private static void misiones(List<MisionC> destino, JsonArray a) {
        destino.clear();
        for (JsonElement e : a) {
            JsonObject j = e.getAsJsonObject();
            destino.add(new MisionC(s(j, "texto"), j.get("prog").getAsInt(), j.get("objetivo").getAsInt(), j.get("premio").getAsInt(), j.get("hecha").getAsBoolean()));
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

    /** Un objeto dibujado de tamaño {@code k} veces el normal (16 px). */
    private void objeto(DrawContext g, String item, int x, int y, float k) {
        ItemStack st = pila(item);
        if (st.isEmpty()) return;
        var m = g.getMatrices();
        m.push(); m.translate(x, y, 0); m.scale(k, k, 1f);
        g.drawItem(st, 0, 0);
        m.pop();
    }

    private boolean abierto() { return estado.equals("abierto"); }

    // ------------------------------------------------------------ dibujo

    private static final int LX = 20, LW = 226, RX = 258, RW = 362, CY = 78;

    @Override
    protected void dibujar(DrawContext g, int mx, int my, float delta) {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoRefresco > 1500) { ultimoRefresco = ahora; enviar("refrescar", TABS_ID[tab], ""); }
        texto(g, "CAFÉ MICHIALIEN", 20, 10, ORO, 2.2f);
        texto(g, duenio ? "Sirve a los Pokémon y haz crecer tu café" : "Estás en el café de " + nombreDuenio, 22, 32, GRIS, 0.9f);
        texto(g, "● " + monedas + " monedas   ★ " + estrellas + " estrellas", 400, 10, MONEDA, 1.2f);
        texto(g, "Clientes: " + clientes + " · Jornadas: " + turnos + " · Mejor jornada: " + mejorTurno, 400, 26, GRIS, 0.8f);
        texto(g, "Las monedas son del café; las estrellas, de la tienda del servidor", 400, 36, LILA, 0.65f);

        for (int i = 0; i < TABS.length; i++) {
            int x = 20 + i * 72;
            boolean act = i == tab, sobre = dentro(mx, my, x, 48, 68, 20);
            caja(g, x, 48, 68, 20, act ? 0xFF3A3A66 : sobre ? 0xFF2A2A4A : FONDO, act ? ORO : BORDE);
            textoCentrado(g, TABS[i], x + 34, 54, act ? ORO : BLANCO, 0.9f);
        }
        boton(g, 538, 48, 82, 20, "SALIR", 0xFF8A3A3A, true, dentro(mx, my, 538, 48, 82, 20), 1f);

        switch (tab) {
            case T_CAFE -> cafe(g, mx, my);
            case T_RECETARIO -> recetario(g, mx, my);
            case T_TIENDA -> tienda(g, mx, my);
            case T_ESTRELLAS -> estrellasTab(g, mx, my);
            case T_MISIONES -> misionesTab(g);
            case T_DECORAR -> decorar(g, mx, my);
            default -> logrosTab(g);
        }
        if (resumen != null) resumenCaja(g, mx, my);
    }

    // ------------------------------------------------------------ cafe

    private static final int CW = 190, CGAP = 11, CH = 232;

    private void cafe(DrawContext g, int mx, int my) {
        if (inicial.isEmpty()) {
            if (!duenio) { texto(g, "El dueño todavía no ha elegido a su cocinero.", LX, CY + 10, GRIS, 1f); return; }
            texto(g, "ELIGE A TU POKÉMON COCINERO", LX, CY - 8, ORO, 1.1f);
            texto(g, "El que elijas te enseña tu primer plato. Los demás se compran en la tienda con las monedas que ganes.", LX, CY + 6, GRIS, 0.8f);
            for (int i = 0; i < iniciales.size(); i++) {
                InicialC c = iniciales.get(i);
                int x = LX + i * (CW + CGAP), y = CY + 20;
                boolean sobre = dentro(mx, my, x, y, CW, CH);
                caja(g, x, y, CW, CH, sobre ? 0xFF25254A : FONDO, sobre ? ORO : BORDE);
                objeto(g, c.item(), x + CW / 2 - 24, y + 10, 3f);
                textoCentrado(g, c.nombre(), x + CW / 2, y + 66, ORO, 1.4f);
                textoCentrado(g, "Te enseña a hacer:", x + CW / 2, y + 90, GRIS, 0.8f);
                textoCentrado(g, c.plato(), x + CW / 2, y + 102, BLANCO, 1.1f);
                parrafo(g, c.receta(), x + 10, y + 124, CW - 20, 0xFF9AC8FF, 0.8f);
                boton(g, x + 20, y + CH - 34, CW - 40, 24, "ELEGIR", 0xFF2E8B57, true, sobre, 1f);
            }
            return;
        }
        // la parte de la izquierda: el estado del cafe y sus botones
        if (construyendo) {
            texto(g, "MODO CONSTRUCCIÓN", LX, CY - 7, ORO, 1.1f);
            parrafo(g, "Compra bloques en la pestaña DECORAR. Clic derecho = colocar; clic izquierdo = recoger lo que pusiste. Para mover una estación o una mesa: recógela con clic izquierdo y ponla con clic derecho en otro sitio (las estaciones, solo en la cocina). Los materiales los eliges a la derecha.", LX, CY + 10, LW, GRIS, 0.8f);
            texto(g, "Mesas: " + mesasUsadas + "/" + mesasMax + " colocadas", LX, CY + 82, BLANCO, 0.9f);
            boton(g, LX, CY + 100, LW, 28, "IR A DECORAR (tienda)", 0xFF7A4AC0, true, dentro(mx, my, LX, CY + 100, LW, 28), 1f);
            boton(g, LX, CY + 134, LW, 28, "TERMINAR DE CONSTRUIR", 0xFF2E8B57, true, dentro(mx, my, LX, CY + 134, LW, 28), 1f);
        } else switch (estado) {
            case "abierto" -> {
                texto(g, "CAFÉ ABIERTO", LX, CY - 7, VERDE, 1.1f);
                parrafo(g, "El café está abierto y no hay reloj: cierra cuando quieras desde aquí. Cocina llevando los ingredientes en las manos y sirve con clic derecho al cliente o a su mesa.", LX, CY + 10, LW, GRIS, 0.85f);
                texto(g, "Platos limpios: " + platosLimpios, LX, CY + 62, platosLimpios == 0 ? ROJO : BLANCO, 1f);
                if (duenio) {
                    boton(g, LX, CY + 84, LW, 28, "SEGUIR SIRVIENDO", 0xFF2E8B57, true, dentro(mx, my, LX, CY + 84, LW, 28), 1f);
                    boton(g, LX, CY + 118, LW, 28, "CERRAR EL CAFÉ", 0xFF8A3A3A, true, dentro(mx, my, LX, CY + 118, LW, 28), 1f);
                } else boton(g, LX, CY + 84, LW, 28, "SEGUIR SIRVIENDO", 0xFF2E8B57, true, dentro(mx, my, LX, CY + 84, LW, 28), 1f);
            }
            case "cerrando" -> {
                texto(g, "CERRANDO EL LOCAL", LX, CY - 7, ROJO, 1.1f);
                parrafo(g, "Ya no entran clientes. En un momento verás el resumen de ganancias.", LX, CY + 10, LW, GRIS, 0.9f);
            }
            default -> {
                texto(g, duenio ? "TU CAFÉ ESTÁ CERRADO" : "CAFÉ CERRADO", LX, CY - 7, ORO, 1.1f);
                parrafo(g, duenio ? "Ábrelo cuando quieras: llegan clientes, cocinas y sirves. Al cerrarlo verás el resumen de ganancias. Con el café cerrado compras platos y mejoras."
                        : "El dueño abrirá el café. Mientras tanto mira la tienda de estrellas, las misiones y los logros.", LX, CY + 10, LW, GRIS, 0.85f);
                if (duenio) {
                    boton(g, LX, CY + 66, LW, 30, "ABRIR EL CAFÉ", 0xFF2E8B57, true, dentro(mx, my, LX, CY + 66, LW, 30), 0.95f);
                    boton(g, LX, CY + 102, LW, 26, "PARAR POR AHORA (salir)", 0xFF8A3A3A, true, dentro(mx, my, LX, CY + 102, LW, 26), 0.95f);
                    boton(g, LX, CY + 132, LW, 24, "MODO CONSTRUCCIÓN (decorar)", 0xFF7A4AC0, true, dentro(mx, my, LX, CY + 132, LW, 24), 0.9f);
                } else boton(g, LX, CY + 66, LW, 28, "SALIR DEL CAFÉ", 0xFF8A3A3A, true, dentro(mx, my, LX, CY + 66, LW, 28), 1f);
                texto(g, "Estrellas: +2 por cliente, +5 por jornada", LX, CY + 164, SUAVE, 0.7f);
                texto(g, "con 3 o más clientes, +5 sin enfados y +5 por récord.", LX, CY + 174, SUAVE, 0.7f);
            }
        }
        if (construyendo) {                                                  // la parte de la derecha: los materiales del local
            texto(g, "MATERIALES DEL LOCAL", RX, CY - 7, ORO, 0.95f);
            for (int i = 0; i < estilo.size(); i++) {
                int y = CY + 8 + i * 36;
                texto(g, estilo.get(i).parte(), RX, y, SUAVE, 0.8f);
                boton(g, RX, y + 11, 24, 20, "◀", 0xFF7A4AC0, duenio, duenio && dentro(mx, my, RX, y + 11, 24, 20), 1f);
                caja(g, RX + 28, y + 11, RW - 56, 20, FONDO, BORDE);
                textoCentrado(g, estilo.get(i).nombre(), RX + RW / 2, y + 17, BLANCO, 0.95f);
                boton(g, RX + RW - 24, y + 11, 24, 20, "▶", 0xFF7A4AC0, duenio, duenio && dentro(mx, my, RX + RW - 24, y + 11, 24, 20), 1f);
            }
            if (estacionesEnMano > 0) texto(g, "Llevas " + estacionesEnMano + " estación(es) en la mano: ponla(s) en la cocina", RX, CY + 154, 0xFFFFD27F, 0.8f);
            return;
        }
        // la parte de la derecha: quien esta en el cafe y a quien invitar
        texto(g, "EN EL CAFÉ (" + gente.size() + "/4)", RX, CY - 7, ORO, 0.95f);
        for (int i = 0; i < gente.size() && i < 4; i++) {
            GenteC c = gente.get(i);
            int y = CY + 8 + i * 20;
            caja(g, RX, y, RW, 18, c.yo() ? ACTIVO : FONDO, c.yo() ? ORO : BORDE);
            texto(g, (c.host() ? "★ " : "") + recortar(c.nombre(), 24), RX + 8, y + 5, c.yo() ? ORO : BLANCO, 0.95f);
            texto(g, c.host() ? "dueño" : "invitado", RX + 300, y + 5, c.host() ? ORO : SUAVE, 0.85f);
        }
        if (duenio && !abierto() && !estado.equals("cerrando")) {
            int y0 = CY + 98;
            texto(g, "INVITAR A UN AMIGO (conectado y libre)", RX, y0 - 4, ORO, 0.9f);
            if (invitables.isEmpty()) texto(g, "Ahora mismo no hay nadie a quien invitar.", RX, y0 + 12, GRIS, 0.85f);
            for (int i = 0; i < invitables.size(); i++) {
                int y = y0 + 8 + i * 22;
                caja(g, RX, y, RW, 20, FONDO, BORDE);
                texto(g, recortar(invitables.get(i), 26), RX + 8, y + 6, BLANCO, 0.95f);
                boolean puede = !lleno;
                boton(g, RX + RW - 92, y + 2, 86, 16, lleno ? "LLENO" : "INVITAR", 0xFF7A4AC0, puede, puede && dentro(mx, my, RX + RW - 92, y + 2, 86, 16), 0.85f);
            }
        }
    }

    // ------------------------------------------------------------ recetario

    private static final int REC_POR_PAG = 6, REC_W = 296, REC_H = 62, TIENDA_POR_PAG = 3;

    private static int paginas(int total, int porPag) { return Math.max(1, (total + porPag - 1) / porPag); }

    /** Dibuja «◀ 1/2 ▶» con su esquina superior izquierda en (x, y). */
    private void paginador(DrawContext g, int mx, int my, int x, int y, int pag, int total) {
        boton(g, x, y, 18, 14, "◀", 0xFF7A4AC0, pag > 0, pag > 0 && dentro(mx, my, x, y, 18, 14), 0.9f);
        texto(g, (pag + 1) + "/" + total, x + 24, y + 3, BLANCO, 0.85f);
        boton(g, x + 54, y, 18, 14, "▶", 0xFF7A4AC0, pag < total - 1, pag < total - 1 && dentro(mx, my, x + 54, y, 18, 14), 0.9f);
    }

    /** -1 / +1 si se pulso una flecha del paginador dibujado en (x, y), 0 si no. */
    private int clicPaginador(int mx, int my, int x, int y) {
        if (dentro(mx, my, x, y, 18, 14)) return -1;
        if (dentro(mx, my, x + 54, y, 18, 14)) return 1;
        return 0;
    }

    private void recetario(DrawContext g, int mx, int my) {
        texto(g, "RECETARIO", LX, CY - 8, ORO, 1.1f);
        texto(g, "Cómo se hace cada plato. Clic derecho en el cliente o en su mesa con el plato en las manos.", LX + 90, CY - 6, GRIS, 0.8f);
        int total = paginas(platos.size(), REC_POR_PAG);
        pagRec = Math.min(pagRec, total - 1);
        paginador(g, mx, my, LX + 528, CY - 12, pagRec, total);
        for (int n = 0; n < REC_POR_PAG; n++) {
            int i = pagRec * REC_POR_PAG + n;
            if (i >= platos.size()) break;
            PlatoC p = platos.get(i);
            int x = LX + (n % 2) * (REC_W + 8), y = CY + 14 + (n / 2) * (REC_H + 4);
            caja(g, x, y, REC_W, REC_H, FONDO, p.especial() ? (p.tengo() ? ORO : 0xFF444455) : p.tengo() ? 0xFF6FE08A : 0xFF444455);
            objeto(g, p.item(), x + 8, y + 10, 2f);
            texto(g, p.nombre(), x + 46, y + 5, p.tengo() ? BLANCO : 0xFF888899, 1f);
            if (p.especial()) texto(g, p.tengo() ? "★ Visita especial · paga " + p.precio() + " monedas" : "★ Visita especial", x + 46, y + 17, p.tengo() ? ORO : 0xFF888899, 0.75f);
            else texto(g, p.tengo() ? "Paga " + p.precio() + " monedas (+ propina)" : "Cuesta " + p.coste() + " monedas en la TIENDA", x + 46, y + 17, p.tengo() ? MONEDA : ROJO, 0.75f);
            parrafo(g, p.receta(), x + 46, y + 29, REC_W - 54, 0xFF9AC8FF, 0.7f);
            if (p.especial()) texto(g, p.tengo() ? "Lo enseñó el " + p.michi() : "Aún no ha venido", x + 46, y + REC_H - 10, p.tengo() ? VERDE : SUAVE, 0.65f);
            else texto(g, p.tengo() ? "¡Ya sabes hacerlo!" : "Sin aprender", x + 46, y + REC_H - 10, p.tengo() ? VERDE : SUAVE, 0.65f);
        }
    }

    // ------------------------------------------------------------ tienda de monedas

    private static final int PW = 296, PH = 62;

    private boolean puedeComprar() { return duenio && estado.equals("preparando"); }

    private void tienda(DrawContext g, int mx, int my) {
        texto(g, "TIENDA DEL CAFÉ", LX, CY - 8, ORO, 1.2f);
        texto(g, "Tus monedas: " + monedas, LX + 140, CY - 5, MONEDA, 0.9f);
        texto(g, !duenio ? "Solo el dueño compra aquí." : abierto() || estado.equals("cerrando") ? "Se compra con el café cerrado." : "Platos nuevos y mejoras para tu café.", LX + 250, CY - 5, SUAVE, 0.8f);
        texto(g, "PLATOS NUEVOS", LX, CY + 8, 0xFF6CC8FF, 0.95f);
        int totalPag = paginas(tiendaPlatos.size(), TIENDA_POR_PAG);
        pagTienda = Math.min(pagTienda, totalPag - 1);
        paginador(g, mx, my, LX + PW - 72, CY + 5, pagTienda, totalPag);
        for (int n = 0; n < TIENDA_POR_PAG; n++) {
            int i = pagTienda * TIENDA_POR_PAG + n;
            if (i >= tiendaPlatos.size()) break;
            PlatoC p = tiendaPlatos.get(i);
            int y = CY + 22 + n * (PH + 4);
            caja(g, LX, y, PW, PH, FONDO, p.tengo() ? 0xFF6FE08A : BORDE);
            objeto(g, p.item(), LX + 8, y + 8, 1.5f);
            texto(g, p.nombre(), LX + 40, y + 6, BLANCO, 1f);
            texto(g, "Paga " + p.precio() + " monedas", LX + 40, y + 18, MONEDA, 0.75f);
            parrafo(g, p.receta(), LX + 8, y + 34, 200, SUAVE, 0.65f);
            int bx = LX + PW - 92, bw = 86;
            boolean puede = !p.tengo() && puedeComprar() && monedas >= p.coste();
            boton(g, bx, y + 36, bw, 20, p.tengo() ? "APRENDIDO ✔" : p.coste() + " ●", 0xFF7A4AC0, puede, puede && dentro(mx, my, bx, y + 36, bw, 20), 0.9f);
        }
        texto(g, "MEJORAS DEL LOCAL", LX + 308, CY + 8, 0xFFC9B3FF, 0.95f);
        for (int i = 0; i < mejoras.size(); i++) {
            MejoraC m = mejoras.get(i);
            int y = CY + 22 + i * 40, x = LX + 308;
            boolean max = m.nivel() >= m.max();
            caja(g, x, y, PW, 36, FONDO, max ? 0xFF6FE08A : BORDE);
            objeto(g, m.item(), x + 6, y + 10, 1f);
            texto(g, m.nombre(), x + 28, y + 4, BLANCO, 0.9f);
            texto(g, m.texto(), x + 28, y + 14, GRIS, 0.6f);
            for (int k = 0; k < m.max(); k++) g.fill(x + 28 + k * 14, y + 25, x + 38 + k * 14, y + 31, k < m.nivel() ? ORO : 0xFF2A2A44);
            int bx = x + PW - 88, bw = 82;
            boolean puede = !max && puedeComprar() && monedas >= m.precio();
            boton(g, bx, y + 9, bw, 20, max ? "MÁXIMO" : m.precio() + " ●", 0xFF7A4AC0, puede, puede && dentro(mx, my, bx, y + 9, bw, 20), 0.9f);
        }
    }

    // ------------------------------------------------------------ decorar (tienda del modo construccion)

    private static final int DW = 196, DH = 52, DCOLS = 3, DFILAS = 4, DPOR_PAG = DCOLS * DFILAS;

    private void decorar(DrawContext g, int mx, int my) {
        texto(g, "DECORAR", LX, CY - 8, ORO, 1.2f);
        texto(g, "Tus monedas: " + monedas, LX + 70, CY - 5, MONEDA, 0.9f);
        if (!construyendo) {
            parrafo(g, duenio ? "Aquí compras bloques y decoración para tu café, pero solo en el MODO CONSTRUCCIÓN: ciérralo si está abierto y pulsa el botón de la pestaña CAFÉ."
                    : "Solo el dueño decora el café.", LX, CY + 14, 400, GRIS, 0.9f);
            if (duenio && estado.equals("preparando")) boton(g, LX, CY + 60, 250, 30, "ENTRAR EN MODO CONSTRUCCIÓN", 0xFF7A4AC0, true, dentro(mx, my, LX, CY + 60, 250, 30), 1f);
            return;
        }
        texto(g, "Mesas colocadas: " + mesasUsadas + "/" + mesasMax, LX + 190, CY - 5, SUAVE, 0.8f);
        int total = paginas(deco.size(), DPOR_PAG);
        pagDeco = Math.min(pagDeco, total - 1);
        paginador(g, mx, my, LX + 528, CY - 12, pagDeco, total);
        for (int n = 0; n < DPOR_PAG; n++) {
            int i = pagDeco * DPOR_PAG + n;
            if (i >= deco.size()) break;
            DecoC a = deco.get(i);
            int x = LX + (n % DCOLS) * (DW + 6), y = CY + 10 + (n / DCOLS) * (DH + 4);
            caja(g, x, y, DW, DH, FONDO, BORDE);
            objeto(g, a.item(), x + 6, y + 8, 1.5f);
            texto(g, recortar(a.nombre(), 22), x + 32, y + 5, BLANCO, 0.85f);
            texto(g, a.grupo() + " · x" + a.cantidad(), x + 32, y + 17, SUAVE, 0.7f);
            boolean puede = monedas >= a.precio();
            boton(g, x + DW - 76, y + DH - 20, 70, 16, a.precio() + " ●", 0xFF7A4AC0, puede, puede && dentro(mx, my, x + DW - 76, y + DH - 20, 70, 16), 0.85f);
        }
    }

    private void clicDecorar(int mx, int my) {
        if (!construyendo) {
            if (duenio && estado.equals("preparando") && dentro(mx, my, LX, CY + 60, 250, 30)) enviar("construir", "", "");
            return;
        }
        int d = clicPaginador(mx, my, LX + 528, CY - 12);
        if (d != 0) { pagDeco = Math.max(0, Math.min(paginas(deco.size(), DPOR_PAG) - 1, pagDeco + d)); return; }
        for (int n = 0; n < DPOR_PAG; n++) {
            int i = pagDeco * DPOR_PAG + n;
            if (i >= deco.size()) break;
            DecoC a = deco.get(i);
            int x = LX + (n % DCOLS) * (DW + 6), y = CY + 10 + (n / DCOLS) * (DH + 4);
            if (monedas >= a.precio() && dentro(mx, my, x + DW - 76, y + DH - 20, 70, 16)) enviar("comprarDeco", a.id(), "");
        }
    }

    // ------------------------------------------------------------ tienda de estrellas

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

    private void estrellasTab(DrawContext g, int mx, int my) {
        texto(g, "TIENDA DE ESTRELLAS", LX, CY - 2, ORO, 1.2f);
        texto(g, "Tus estrellas: " + estrellas + "   ·   Máximo " + limiteTienda + " de cada artículo al día", LX + 170, CY + 1, ESTRELLA, 0.85f);
        for (int i = 0; i < articulos.size(); i++) {
            ArticuloC a = articulos.get(i);
            int[] p = posArticulo(i);
            int x = p[0], y = p[1];
            if (a.unico()) { articuloUnico(g, mx, my, a, x, y); continue; }
            boolean tope = a.hoy() >= limiteTienda, puede = !tope && estrellas >= a.precio();
            caja(g, x, y, TW, TH, FONDO, a.grupo().equals("banner") ? 0xFF8A5AB0 : BORDE);
            ItemStack s = pila(a.item());
            if (!s.isEmpty()) g.drawItem(s, x + 8, y + 7);
            texto(g, a.nombre(), x + 32, y + 4, BLANCO, 0.95f);
            texto(g, a.texto(), x + 32, y + 14, GRIS, 0.7f);
            texto(g, "Hoy: " + a.hoy() + "/" + limiteTienda, x + 32, y + 22, tope ? ROJO : SUAVE, 0.7f);
            int bx = x + TW - 92, bw = 86;
            boton(g, bx, y + 4, bw, 22, tope ? "AGOTADO HOY" : a.precio() + " ★", 0xFF7A4AC0, puede, puede && dentro(mx, my, bx, y + 4, bw, 22), 0.9f);
        }
        texto(g, "Lo que compras llega a tu correo (/correo). Las estrellas se ganan sirviendo, con misiones y con logros.", LX, 336, SUAVE, 0.75f);
    }

    /** El articulo exclusivo (el Michialien chef): fila ancha dorada, se paga con estrellas y solo se compra una vez. */
    private void articuloUnico(DrawContext g, int mx, int my, ArticuloC a, int x, int y) {
        int w = 2 * TW + 8, h = 36;
        boolean puede = !a.comprado() && estrellas >= a.precio();
        caja(g, x, y, w, h, 0xFF2A2410, ORO);
        ItemStack s = pila(a.item());
        if (!s.isEmpty()) g.drawItem(s, x + 10, y + 10);
        texto(g, a.nombre() + "  ★ EXCLUSIVO", x + 36, y + 5, ORO, 1.0f);
        texto(g, a.texto(), x + 36, y + 17, GRIS, 0.75f);
        texto(g, a.comprado() ? "¡Ya lo tienes! Mira /correo si aún no lo has recogido." : "Tus estrellas: " + estrellas, x + 36, y + 26, a.comprado() ? VERDE : SUAVE, 0.7f);
        int bw = 150, bx = x + w - bw - 8;
        boton(g, bx, y + 7, bw, 22, a.comprado() ? "COMPRADO ✔" : a.precio() + " estrellas", 0xFFB8860B, puede, puede && dentro(mx, my, bx, y + 7, bw, 22), 0.9f);
    }

    // ------------------------------------------------------------ misiones y logros

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
            caja(g, x, y, 296, 60, FONDO, m.hecha() ? VERDE : BORDE);
            parrafo(g, m.texto(), x + 8, y + 7, 224, m.hecha() ? VERDE : BLANCO, 0.9f);
            texto(g, m.hecha() ? "¡Hecha!" : "+" + m.premio() + " ★", x + 240, y + 7, m.hecha() ? VERDE : ESTRELLA, 0.95f);
            barra(g, x + 8, y + 36, 200, m.prog(), m.objetivo(), m.hecha() ? VERDE : color);
            texto(g, m.prog() + " / " + m.objetivo(), x + 216, y + 35, GRIS, 0.9f);
            texto(g, m.hecha() ? "Premio ya cobrado" : "El premio se cobra solo al cumplirla", x + 8, y + 48, SUAVE, 0.65f);
        }
    }

    private void barra(DrawContext g, int x, int y, int w, long v, long meta, int color) {
        g.fill(x, y, x + w, y + 7, 0xFF2A2A44);
        g.fill(x, y, x + (int) (w * Math.min(1.0, meta <= 0 ? 1 : v / (double) meta)), y + 7, color);
    }

    private void logrosTab(DrawContext g) {
        texto(g, "LOGROS", LX, CY - 8, ORO, 1.1f);
        texto(g, "Escalones que se cobran solos en estrellas al alcanzarlos.", LX + 70, CY - 6, GRIS, 0.8f);
        for (int i = 0; i < logros.size(); i++) {
            LogroC l = logros.get(i);
            int x = LX + (i % 2) * 308, y = CY + 12 + (i / 2) * 66;
            boolean fin = l.nivel() >= l.max();
            caja(g, x, y, 296, 60, FONDO, fin ? VERDE : BORDE);
            texto(g, l.nombre(), x + 8, y + 6, fin ? VERDE : BLANCO, 1f);
            for (int k = 0; k < l.max(); k++) g.fill(x + 150 + k * 14, y + 8, x + 160 + k * 14, y + 14, k < l.nivel() ? ORO : 0xFF2A2A44);
            texto(g, fin ? "¡Completo!" : "+" + l.premio() + " ★", x + 240, y + 6, fin ? VERDE : ESTRELLA, 0.9f);
            barra(g, x + 8, y + 26, 200, l.valor(), l.meta(), fin ? VERDE : 0xFF6CC8FF);
            texto(g, Math.min(l.valor(), l.meta()) + " / " + l.meta(), x + 216, y + 25, GRIS, 0.9f);
            texto(g, l.texto(), x + 8, y + 40, SUAVE, 0.75f);
        }
    }

    // ------------------------------------------------------------ resumen del turno

    private static final int RXB = 130, RYB = 80, RWB = 380, RHB = 220;

    private void resumenCaja(DrawContext g, int mx, int my) {
        int x = RXB, y = RYB, w = RWB, h = RHB;
        g.draw();
        g.getMatrices().push();
        g.getMatrices().translate(0, 0, 400);
        g.fill(0, 0, W, H, 0xA0000010);
        caja(g, x, y, w, h, 0xFF14142A, ORO);
        textoCentrado(g, "¡CERRAMOS! RESUMEN DE LA JORNADA", x + w / 2, y + 10, ORO, 1.4f);
        int tuyo = resumen.get("tuyo").getAsInt(), total = resumen.get("ganado").getAsInt();
        textoCentrado(g, "Ganancias del café: " + total + " monedas", x + w / 2, y + 34, MONEDA, 1.2f);
        if (tuyo != total) textoCentrado(g, "De ellas, tuyas: " + tuyo, x + w / 2, y + 48, BLANCO, 0.9f);
        textoCentrado(g, "Clientes servidos: " + resumen.get("servidos").getAsInt() + " · Propinas: " + resumen.get("propinas").getAsInt() + " · Se fueron enfadados: " + resumen.get("enfadados").getAsInt(), x + w / 2, y + 64, GRIS, 0.85f);
        textoCentrado(g, "Estrellas ganadas: +" + resumen.get("estrellasTurno").getAsInt() + "  (ahora tienes " + estrellas + ")", x + w / 2, y + 80, ESTRELLA, 1f);
        textoCentrado(g, "Tus monedas ahora: " + monedas, x + w / 2, y + 96, MONEDA, 0.9f);
        if (duenio) {
            textoCentrado(g, "¿Quieres comprar algo, abrir otra vez o parar por ahora?", x + w / 2, y + 122, BLANCO, 0.9f);
            boton(g, x + 20, y + 144, 164, 24, "COMPRAR", 0xFF7A4AC0, true, dentro(mx, my, x + 20, y + 144, 164, 24), 1f);
            boton(g, x + w - 184, y + 144, 164, 24, "ABRIR OTRA VEZ", 0xFF2E8B57, true, dentro(mx, my, x + w - 184, y + 144, 164, 24), 1f);
            boton(g, x + 20, y + 176, 164, 24, "PARAR POR AHORA", 0xFF8A3A3A, true, dentro(mx, my, x + 20, y + 176, 164, 24), 1f);
            boton(g, x + w - 184, y + 176, 164, 24, "SEGUIR EN EL MENÚ", 0xFF444466, true, dentro(mx, my, x + w - 184, y + 176, 164, 24), 1f);
        } else {
            boton(g, x + w / 2 - 60, y + h - 34, 120, 24, "ACEPTAR", 0xFF2E8B57, true, dentro(mx, my, x + w / 2 - 60, y + h - 34, 120, 24), 1f);
        }
        g.getMatrices().pop();
    }

    // ------------------------------------------------------------ clics

    @Override
    protected void clic(int mx, int my, int boton) {
        if (boton != 0) return;
        if (resumen != null) {
            int x = RXB, y = RYB, w = RWB, h = RHB;
            if (duenio) {
                if (dentro(mx, my, x + 20, y + 144, 164, 24)) { resumen = null; tab = T_TIENDA; enviar("refrescar", "tienda", ""); }
                else if (dentro(mx, my, x + w - 184, y + 144, 164, 24)) enviar("abrir", "", "");
                else if (dentro(mx, my, x + 20, y + 176, 164, 24)) enviar("salir", "", "");
                else if (dentro(mx, my, x + w - 184, y + 176, 164, 24)) resumen = null;
            } else if (dentro(mx, my, x + w / 2 - 60, y + h - 34, 120, 24)) resumen = null;
            return;
        }
        if (dentro(mx, my, 538, 48, 82, 20)) { enviar("salir", "", ""); return; }
        for (int i = 0; i < TABS.length; i++) if (dentro(mx, my, 20 + i * 72, 48, 68, 20)) { tab = i; enviar("refrescar", TABS_ID[i], ""); return; }
        switch (tab) {
            case T_CAFE -> clicCafe(mx, my);
            case T_TIENDA -> clicTienda(mx, my);
            case T_RECETARIO -> { int d = clicPaginador(mx, my, LX + 528, CY - 12); if (d != 0) pagRec = Math.max(0, Math.min(paginas(platos.size(), REC_POR_PAG) - 1, pagRec + d)); }
            case T_ESTRELLAS -> clicEstrellas(mx, my);
            case T_DECORAR -> clicDecorar(mx, my);
            default -> { }
        }
    }

    private void clicCafe(int mx, int my) {
        if (inicial.isEmpty()) {
            if (!duenio) return;
            for (int i = 0; i < iniciales.size(); i++) {
                int x = LX + i * (CW + CGAP), y = CY + 20;
                if (dentro(mx, my, x, y, CW, CH)) { enviar("inicial", iniciales.get(i).id(), ""); return; }
            }
            return;
        }
        if (construyendo) {
            if (!duenio) return;
            if (dentro(mx, my, LX, CY + 100, LW, 28)) { tab = T_DECORAR; enviar("refrescar", "decorar", ""); return; }
            if (dentro(mx, my, LX, CY + 134, LW, 28)) { enviar("terminarConstruir", "", ""); return; }
            for (int i = 0; i < estilo.size(); i++) {
                int y = CY + 8 + i * 36;
                if (dentro(mx, my, RX, y + 11, 24, 20)) { enviar("estilo", String.valueOf(i), "-1"); return; }
                if (dentro(mx, my, RX + RW - 24, y + 11, 24, 20)) { enviar("estilo", String.valueOf(i), "+1"); return; }
            }
            return;
        }
        switch (estado) {
            case "abierto" -> {
                if (dentro(mx, my, LX, CY + 84, LW, 28)) { net.minecraft.client.MinecraftClient.getInstance().setScreen(null); return; }
                if (duenio && dentro(mx, my, LX, CY + 118, LW, 28)) enviar("cerrarTurno", "", "");
            }
            case "cerrando" -> { }
            default -> {
                if (duenio) {
                    if (dentro(mx, my, LX, CY + 66, LW, 30)) { enviar("abrir", "", ""); return; }
                    if (dentro(mx, my, LX, CY + 102, LW, 26)) { enviar("salir", "", ""); return; }
                    if (dentro(mx, my, LX, CY + 132, LW, 24)) { enviar("construir", "", ""); return; }
                    if (!lleno) for (int i = 0; i < invitables.size(); i++) {
                        int y = CY + 98 + 8 + i * 22;
                        if (dentro(mx, my, RX + RW - 92, y + 2, 86, 16)) { enviar("invitar", invitables.get(i), ""); return; }
                    }
                } else if (dentro(mx, my, LX, CY + 66, LW, 28)) enviar("salir", "", "");
            }
        }
    }

    private void clicTienda(int mx, int my) {
        int d = clicPaginador(mx, my, LX + PW - 72, CY + 5);
        if (d != 0) { pagTienda = Math.max(0, Math.min(paginas(tiendaPlatos.size(), TIENDA_POR_PAG) - 1, pagTienda + d)); return; }
        if (!puedeComprar()) return;
        for (int n = 0; n < TIENDA_POR_PAG; n++) {
            int i = pagTienda * TIENDA_POR_PAG + n;
            if (i >= tiendaPlatos.size()) break;
            PlatoC p = tiendaPlatos.get(i);
            int y = CY + 22 + n * (PH + 4);
            if (!p.tengo() && monedas >= p.coste() && dentro(mx, my, LX + PW - 92, y + 36, 86, 20)) enviar("comprarPlato", p.id(), "");
        }
        for (int i = 0; i < mejoras.size(); i++) {
            MejoraC m = mejoras.get(i);
            int y = CY + 22 + i * 40, x = LX + 308;
            if (m.nivel() < m.max() && monedas >= m.precio() && dentro(mx, my, x + PW - 88, y + 9, 82, 20)) enviar("comprarMejora", m.id(), "");
        }
    }

    private void clicEstrellas(int mx, int my) {
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
