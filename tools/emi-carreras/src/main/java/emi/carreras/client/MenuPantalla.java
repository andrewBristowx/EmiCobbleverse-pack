package emi.carreras.client;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emi.carreras.net.AccionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * El menu de la sala de carreras (se abre al entrar, con el objeto del menu o tocando el terminal): JUGAR (abrir una carrera o unirte a la que hay, elegir Pokemon, estar listo),
 * ENTRENAR (correr solo en una pista), LOGROS, TIENDA (con estrellas) y TOP (los mejores tiempos de cada pista). Los datos los manda el servidor (JSON); cada clic manda una
 * accion al servidor, que lo comprueba todo y vuelve a mandar los datos.
 */
@Environment(EnvType.CLIENT)
public final class MenuPantalla extends Lienzo {
    private static final int T_JUGAR = 0, T_ENTRENAR = 1, T_LOGROS = 2, T_TIENDA = 3, T_TOP = 4;
    private static final String[] TABS = {"CARRERA", "ENTRENAR", "LOGROS", "TIENDA", "TOP"};
    private static final String[] TABS_ID = {"jugar", "entrenar", "logros", "tienda", "top"};

    /** La pantalla abierta ahora (para actualizarla cuando llegan datos nuevos del servidor). */
    public static MenuPantalla abierta;

    // ------------------------------------------------------------ datos
    private static final class Poke {
        int slot, nivel;
        String especie, nombre, apodo, tipo;
        boolean brillante, valido;
        List<String> aspectos = new ArrayList<>();
        RenderablePokemon rp;
        FloatingState st;
        boolean intentado;

        String titulo() { return apodo == null || apodo.isEmpty() ? nombre : apodo; }
    }

    private record PistaC(String id, String nombre, String texto, int vueltas, String mejor) {}
    private record CorredorC(String nombre, boolean listo, boolean yo, boolean host, String pokemon) {}
    private record LogroC(String nombre, String texto, int hechos, int total, long valor, long meta, String valorTxt, String metaTxt, int premio, boolean completo) {}
    private record ArticuloC(String id, String item, String nombre, String texto, int precio, String grupo, int hoy, boolean unico, boolean comprado) {}
    private record MarcaC(int pos, String nombre, String tiempo, String especie, boolean mio) {}

    private final List<Poke> equipo = new ArrayList<>();
    private final List<PistaC> pistas = new ArrayList<>();
    private final List<CorredorC> corredores = new ArrayList<>();
    private final List<LogroC> logros = new ArrayList<>();
    private final List<ArticuloC> articulos = new ArrayList<>();
    private final Map<String, List<MarcaC>> tops = new HashMap<>();
    private final Map<String, ItemStack> pilas = new HashMap<>();
    private JsonObject resultado;
    private String fase = "libre", cPista = "", cPistaNombre = "";
    private boolean cEntrenamiento, apuntado, listo, anfitrion;
    private int cuenta = -1, maxCorredores = 12, limiteTienda = 10;
    private int estrellas, carreras, victorias, podios, elegidoServidor;
    private int tab, sel = -1, selPista, selTop;
    private long ultimoRefresco = System.currentTimeMillis();
    private final long t0 = System.nanoTime();

    public MenuPantalla(String json) {
        super(Text.literal("Emi Carreras"));
        cargar(json, false);
        abierta = this;
    }

    @Override public void removed() { if (abierta == this) abierta = null; super.removed(); }

    /** Carga (o recarga) los datos del servidor, conservando la pestaña y la eleccion mientras se pueda. {@code soloDatos} = es un refresco: no se cambia de pestaña. */
    public void cargar(String json, boolean soloDatos) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        String t = o.has("tab") ? o.get("tab").getAsString() : "jugar";
        if (!soloDatos) for (int i = 0; i < TABS_ID.length; i++) if (TABS_ID[i].equals(t)) tab = i;
        estrellas = o.get("estrellas").getAsInt(); carreras = o.get("carreras").getAsInt(); victorias = o.get("victorias").getAsInt(); podios = o.get("podios").getAsInt();

        Map<Integer, Poke> antes = new HashMap<>();
        for (Poke p : equipo) antes.put(p.slot, p);
        equipo.clear();
        for (JsonElement e : o.getAsJsonArray("equipo")) {
            JsonObject j = e.getAsJsonObject();
            Poke p = new Poke();
            p.slot = j.get("slot").getAsInt(); p.valido = j.get("valido").getAsBoolean(); p.especie = j.get("especie").getAsString();
            p.nombre = j.get("nombre").getAsString(); p.apodo = j.get("apodo").getAsString(); p.nivel = j.get("nivel").getAsInt();
            p.tipo = j.get("tipo").getAsString(); p.brillante = j.get("brillante").getAsBoolean();
            for (JsonElement a : j.getAsJsonArray("aspectos")) p.aspectos.add(a.getAsString());
            Poke previo = antes.get(p.slot);
            if (previo != null && previo.especie.equals(p.especie) && previo.aspectos.equals(p.aspectos)) { p.rp = previo.rp; p.st = previo.st; p.intentado = previo.intentado; }     // el modelo ya creado se reutiliza
            equipo.add(p);
        }
        elegidoServidor = o.get("elegido").getAsInt();
        if (elegidoServidor > 0) sel = elegidoServidor;                       // apuntado a la carrera: el servidor manda
        else if (!equipo.stream().anyMatch(p -> p.slot == sel && p.valido)) { sel = -1; for (Poke p : equipo) if (p.valido) { sel = p.slot; break; } }

        pistas.clear();
        for (JsonElement e : o.getAsJsonArray("pistas")) {
            JsonObject j = e.getAsJsonObject();
            pistas.add(new PistaC(j.get("id").getAsString(), j.get("nombre").getAsString(), j.get("texto").getAsString(), j.get("vueltas").getAsInt(), j.get("mejor").getAsString()));
        }
        if (selPista >= pistas.size()) selPista = 0;

        JsonObject c = o.getAsJsonObject("carrera");
        fase = c.get("fase").getAsString();
        cPista = c.has("pista") ? c.get("pista").getAsString() : ""; cPistaNombre = c.has("pistaNombre") ? c.get("pistaNombre").getAsString() : "";
        cEntrenamiento = c.get("entrenamiento").getAsBoolean(); maxCorredores = c.get("max").getAsInt();
        apuntado = c.get("apuntado").getAsBoolean(); listo = c.get("listo").getAsBoolean(); anfitrion = c.get("anfitrion").getAsBoolean();
        cuenta = c.get("cuenta").getAsInt();
        corredores.clear();
        for (JsonElement e : c.getAsJsonArray("corredores")) {
            JsonObject j = e.getAsJsonObject();
            corredores.add(new CorredorC(j.get("nombre").getAsString(), j.get("listo").getAsBoolean(), j.get("yo").getAsBoolean(), j.get("host").getAsBoolean(), j.get("pokemon").getAsString()));
        }

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
                l.add(new MarcaC(j.get("pos").getAsInt(), j.get("nombre").getAsString(), j.get("tiempo").getAsString(), j.get("especie").getAsString(), j.get("mio").getAsBoolean()));
            }
            tops.put(en.getKey(), l);
        }
        if (o.has("resultado")) resultado = o.getAsJsonObject("resultado");
        else if (!soloDatos) resultado = null;
    }

    // ------------------------------------------------------------ utilidades

    private static final Map<String, String> TIPOS = Map.ofEntries(
            Map.entry("fire", "Fuego"), Map.entry("water", "Agua"), Map.entry("electric", "Eléctrico"), Map.entry("grass", "Planta"), Map.entry("ice", "Hielo"), Map.entry("fighting", "Lucha"),
            Map.entry("poison", "Veneno"), Map.entry("ground", "Tierra"), Map.entry("flying", "Volador"), Map.entry("psychic", "Psíquico"), Map.entry("bug", "Bicho"), Map.entry("rock", "Roca"),
            Map.entry("ghost", "Fantasma"), Map.entry("dragon", "Dragón"), Map.entry("dark", "Siniestro"), Map.entry("steel", "Acero"), Map.entry("fairy", "Hada"), Map.entry("normal", "Normal"));
    private static final Map<String, Integer> COLORES = Map.ofEntries(
            Map.entry("fire", 0xFFEE8130), Map.entry("water", 0xFF6390F0), Map.entry("electric", 0xFFE0B800), Map.entry("grass", 0xFF7AC74C), Map.entry("ice", 0xFF7FCFCB), Map.entry("fighting", 0xFFC22E28),
            Map.entry("poison", 0xFFA33EA1), Map.entry("ground", 0xFFC9A44A), Map.entry("flying", 0xFFA98FF3), Map.entry("psychic", 0xFFF95587), Map.entry("bug", 0xFFA6B91A), Map.entry("rock", 0xFFB6A136),
            Map.entry("ghost", 0xFF735797), Map.entry("dragon", 0xFF6F35FC), Map.entry("dark", 0xFF705746), Map.entry("steel", 0xFF9A9AB5), Map.entry("fairy", 0xFFD685AD), Map.entry("normal", 0xFFA8A77A));

    private void enviar(String accion, String a, String b) { ClientPlayNetworking.send(new AccionPayload(accion, a, b)); }

    private Poke elegido() { for (Poke p : equipo) if (p.slot == sel) return p; return null; }

    /** Crea (una sola vez) lo necesario para dibujar el modelo 3D del Pokemon; si no se puede, se dibuja sin modelo. */
    private void preparar(Poke p) {
        if (p.intentado) return;
        p.intentado = true;
        try {
            Species sp = PokemonSpecies.getByIdentifier(Identifier.of(p.especie));
            if (sp == null) return;
            p.rp = new RenderablePokemon(sp, new HashSet<>(p.aspectos), ItemStack.EMPTY);
            p.st = new FloatingState();
            p.st.setCurrentAspects(p.rp.getAspects());
        } catch (Throwable t) { p.rp = null; }
    }

    private void modelo(DrawContext g, Poke p, float cx, float cy, float escalaM, float delta) {
        preparar(p);
        if (p.rp == null) return;
        var m = g.getMatrices();
        m.push();
        m.translate(cx, cy - 0.935f * escalaM, 0);     // el modelo de Cobblemon cuelga ~0,35*escala por debajo del punto y mide ~1,17*escala
        p.st.updatePartialTicks(delta);
        float giro = (float) Math.toRadians(35 + (System.nanoTime() - t0) / 1e9 * 45);
        try {
            PokemonGuiUtilsKt.drawProfilePokemon(p.rp, m, new Quaternionf().rotationXYZ((float) Math.toRadians(13), giro, 0f), PoseType.PROFILE, p.st, delta, escalaM, true, false, 1f, 1f, 1f, 1f, 0f, 0f);
        } catch (Throwable t) { p.rp = null; }
        m.pop();
    }

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
        texto(g, "EMI CARRERAS", 20, 10, ORO, 2.2f);
        texto(g, "Corre con tus Pokémon y los Michialiens", 22, 32, GRIS, 0.9f);
        texto(g, "★ " + estrellas + " estrellas", 470, 10, 0xFFFFE070, 1.3f);
        texto(g, "Carreras: " + carreras + " · Victorias: " + victorias + " · Podios: " + podios, 470, 26, GRIS, 0.8f);
        texto(g, "Se ganan corriendo y con los logros", 470, 36, LILA, 0.7f);

        // pestañas y salir
        for (int i = 0; i < TABS.length; i++) {
            int x = 20 + i * 82;
            boolean act = i == tab, sobre = dentro(mx, my, x, 48, 78, 20);
            caja(g, x, 48, 78, 20, act ? 0xFF3A3A66 : sobre ? 0xFF2A2A4A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
            textoCentrado(g, TABS[i], x + 39, 54, act ? ORO : BLANCO, 1f);
        }
        boton(g, 538, 48, 82, 20, "SALIR", 0xFF8A3A3A, true, dentro(mx, my, 538, 48, 82, 20), 1f);

        switch (tab) {
            case T_JUGAR -> { lista(g, mx, my, delta); jugar(g, mx, my); }
            case T_ENTRENAR -> { lista(g, mx, my, delta); entrenar(g, mx, my); }
            case T_LOGROS -> logrosTab(g);
            case T_TIENDA -> tienda(g, mx, my);
            default -> top(g, mx, my);
        }
        if (resultado != null) resultadoCaja(g, mx, my);
    }

    /** La lista de los Pokemon del equipo (a la izquierda en CARRERA y ENTRENAR); solo valen los terrestres. */
    private void lista(DrawContext g, int mx, int my, float delta) {
        texto(g, "TU POKÉMON DE CARRERA", LX, CY - 7, ORO, 0.9f);
        if (equipo.isEmpty()) {
            caja(g, LX, CY, LW, 90, 0xFF1B1B30, 0xFF55557A);
            parrafo(g, "No tienes ningún Pokémon en tu equipo: te prestaremos uno para correr.", LX + 10, CY + 12, LW - 20, GRIS, 1f);
            return;
        }
        for (int i = 0; i < equipo.size(); i++) {
            Poke p = equipo.get(i);
            int y = CY + i * 43;
            boolean act = p.slot == sel, sobre = p.valido && dentro(mx, my, LX, y, LW, 40);
            caja(g, LX, y, LW, 40, !p.valido ? 0xFF15151F : act ? 0xFF2E2E58 : sobre ? 0xFF25254A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
            modelo(g, p, LX + 26, y + 20, 24f, delta);
            texto(g, p.titulo() + (p.brillante ? " ★" : ""), LX + 52, y + 6, p.valido ? BLANCO : 0xFF77778A, 1.05f);
            texto(g, "Nv. " + p.nivel, LX + 52, y + 18, GRIS, 0.9f);
            if (p.valido) {
                int col = COLORES.getOrDefault(p.tipo, 0xFFA8A77A);
                g.fill(LX + 52, y + 29, LX + 52 + 52, y + 37, col);
                textoCentrado(g, TIPOS.getOrDefault(p.tipo, p.tipo), LX + 52 + 26, y + 29, BLANCO, 0.7f);
                if (act && elegidoServidor == p.slot) texto(g, "ELEGIDO ✔", LX + 150, y + 29, VERDE, 0.85f);
            } else texto(g, "no puede correr: no es terrestre", LX + 52, y + 29, 0xFF77778A, 0.75f);
        }
    }

    private void tarjetaPista(DrawContext g, int mx, int my, PistaC p, int x, int y, int w, boolean act, boolean clicable) {
        boolean sobre = clicable && dentro(mx, my, x, y, w, 44);
        caja(g, x, y, w, 44, act ? 0xFF2E2E58 : sobre ? 0xFF25254A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
        texto(g, p.nombre(), x + 10, y + 6, BLANCO, 1.15f);
        texto(g, p.texto(), x + 10, y + 19, GRIS, 0.85f);
        texto(g, p.mejor().isEmpty() ? "Aún sin tiempo" : "Tu mejor tiempo: " + p.mejor(), x + 10, y + 31, p.mejor().isEmpty() ? 0xFF8888AA : 0xFFFFE070, 0.85f);
    }

    private static final int PY = CY + 12;

    private void jugar(DrawContext g, int mx, int my) {
        switch (fase) {
            case "libre" -> {
                texto(g, "NO HAY NINGUNA CARRERA ABIERTA", RX, CY - 7, ORO, 0.9f);
                texto(g, "Elige pista y ábrela: los demás jugadores de la sala podrán unirse.", RX, PY - 2, GRIS, 0.8f);
                for (int i = 0; i < pistas.size(); i++) tarjetaPista(g, mx, my, pistas.get(i), RX, PY + 10 + i * 48, RW, i == selPista, true);
                int by = PY + 10 + pistas.size() * 48 + 4;
                boolean puede = !pistas.isEmpty();
                boton(g, RX, by, RW, 24, "ABRIR CARRERA EN " + (puede ? pistas.get(selPista).nombre().toUpperCase() : "—"), 0xFF2E8B57, puede, puede && dentro(mx, my, RX, by, RW, 24), 1f);
            }
            case "cola" -> {
                texto(g, (cEntrenamiento ? "ENTRENAMIENTO" : "CARRERA ABIERTA") + " · " + cPistaNombre.toUpperCase(), RX, CY - 7, ORO, 0.9f);
                texto(g, "Corredores " + corredores.size() + "/" + maxCorredores + " · cuando todos estén listos (mínimo 2) sale sola", RX, PY - 2, GRIS, 0.8f);
                for (int i = 0; i < corredores.size() && i < 8; i++) {
                    CorredorC c = corredores.get(i);
                    int y = PY + 10 + i * 20;
                    caja(g, RX, y, RW, 18, c.yo() ? 0xFF2E2E58 : 0xFF1B1B30, c.yo() ? ORO : 0xFF55557A);
                    texto(g, (c.host() ? "★ " : "") + recortar(c.nombre(), 18), RX + 8, y + 5, c.yo() ? ORO : BLANCO, 0.95f);
                    texto(g, c.pokemon().isEmpty() ? "(elegirá uno)" : c.pokemon(), RX + 150, y + 5, GRIS, 0.9f);
                    texto(g, c.listo() ? "LISTO ✔" : "esperando", RX + 290, y + 5, c.listo() ? VERDE : 0xFF8888AA, 0.9f);
                }
                int by = 262;
                if (cuenta >= 0) textoCentrado(g, "¡Todos listos! Sale la carrera en " + cuenta + " s", RX + RW / 2, by - 14, VERDE, 1f);
                if (!apuntado) {
                    boton(g, RX, by, RW, 26, "UNIRME A LA CARRERA", 0xFF2E8B57, corredores.size() < maxCorredores, dentro(mx, my, RX, by, RW, 26), 1f);
                } else {
                    boton(g, RX, by, 178, 26, listo ? "NO ESTOY LISTO" : "¡ESTOY LISTO!", listo ? 0xFF8A6A2E : 0xFF2E8B57, true, dentro(mx, my, RX, by, 178, 26), 1f);
                    boton(g, RX + 184, by, 178, 26, "SALIR DE LA CARRERA", 0xFF8A3A3A, true, dentro(mx, my, RX + 184, by, 178, 26), 1f);
                    if (anfitrion) {
                        boton(g, RX, by + 30, 178, 24, "EMPEZAR YA", 0xFFB8860B, true, dentro(mx, my, RX, by + 30, 178, 24), 1f);
                        boton(g, RX + 184, by + 30, 178, 24, "CANCELAR CARRERA", 0xFF6A2E2E, true, dentro(mx, my, RX + 184, by + 30, 178, 24), 1f);
                    } else texto(g, "El anfitrión (★) empieza la carrera.", RX, by + 36, 0xFF8888AA, 0.85f);
                }
                texto(g, "Elige tu Pokémon en la lista de la izquierda.", RX, 336, 0xFF8888AA, 0.75f);
            }
            default -> {
                texto(g, "CARRERA EN MARCHA", RX, CY - 7, ORO, 0.9f);
                caja(g, RX, PY, RW, 70, 0xFF1B1B30, 0xFF55557A);
                parrafo(g, "Hay una carrera en «" + cPistaNombre + "» con " + corredores.size() + " corredor" + (corredores.size() == 1 ? "" : "es") + ". Espera a que termine para abrir otra o entrenar.", RX + 12, PY + 14, RW - 24, GRIS, 1f);
            }
        }
    }

    private void entrenar(DrawContext g, int mx, int my) {
        texto(g, "ENTRENAMIENTO EN SOLITARIO", RX, CY - 7, ORO, 0.9f);
        texto(g, "Corres solo, sin esperar a nadie. Guardas tu mejor tiempo y cuenta para los logros.", RX, PY - 2, GRIS, 0.8f);
        for (int i = 0; i < pistas.size(); i++) tarjetaPista(g, mx, my, pistas.get(i), RX, PY + 10 + i * 48, RW, i == selPista, true);
        int by = PY + 10 + pistas.size() * 48 + 4;
        boolean libre = fase.equals("libre"), puede = libre && !pistas.isEmpty() && elegido() != null || libre && equipo.isEmpty();
        boton(g, RX, by, RW, 24, libre ? "ENTRENAR EN " + (pistas.isEmpty() ? "—" : pistas.get(selPista).nombre().toUpperCase()) : "HAY UNA CARRERA ABIERTA O EN MARCHA", 0xFF2E8B57, puede, puede && dentro(mx, my, RX, by, RW, 24), 1f);
        texto(g, "Premio: 3 estrellas por terminar + la mitad de las michiestrellitas que recojas.", RX, by + 30, 0xFF8888AA, 0.75f);
        texto(g, "En carreras con rivales: 5 + 25/15/10/5 según tu puesto + todas las michiestrellitas.", RX, by + 40, 0xFF8888AA, 0.75f);
    }

    // ------------------------------------------------------------ logros

    private void logrosTab(DrawContext g) {
        texto(g, "LOGROS", LX, CY - 8, ORO, 1.1f);
        texto(g, "Cada escalón da estrellas. Los logros se consiguen solos al correr.", LX + 70, CY - 6, GRIS, 0.8f);
        for (int i = 0; i < logros.size(); i++) {
            LogroC l = logros.get(i);
            int x = LX + (i / 6) * 308, y = CY + 6 + (i % 6) * 40;
            caja(g, x, y, 296, 37, 0xFF1B1B30, l.completo() ? ORO : 0xFF55557A);
            texto(g, l.nombre() + "  " + l.hechos() + "/" + l.total(), x + 8, y + 4, l.completo() ? ORO : BLANCO, 0.95f);
            texto(g, l.texto(), x + 8, y + 15, GRIS, 0.75f);
            int bw = 190;
            g.fill(x + 8, y + 27, x + 8 + bw, y + 32, 0xFF2A2A44);
            g.fill(x + 8, y + 27, x + 8 + (int) (bw * Math.min(1.0, l.meta() == 0 ? 1 : l.valor() / (double) l.meta())), y + 32, l.completo() ? ORO : VERDE);
            texto(g, l.valorTxt() + " / " + l.metaTxt(), x + 204, y + 26, GRIS, 0.75f);
            texto(g, l.completo() ? "¡Completo!" : "+" + l.premio() + " ★", x + 244, y + 4, l.completo() ? VERDE : 0xFFFFE070, 0.9f);
        }
    }

    // ------------------------------------------------------------ tienda

    private static final int TW = 296, TH = 30, TY = CY + 14, TGAP = 2;

    /** Donde va el articulo numero {@code i}: los normales en dos columnas de seis filas; el unico (de estrellas) en una fila ancha debajo. */
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
        texto(g, "TIENDA DE CARRERAS", LX, CY - 2, ORO, 1.2f);
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
        texto(g, "Lo que compras llega a tu correo (/correo). Las estrellas se ganan corriendo y con los logros.", LX, 336, 0xFF8888AA, 0.75f);
    }

    /** El articulo exclusivo (el Michialien en un carrito de juguete): fila ancha dorada, se paga con estrellas y solo se compra una vez. */
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
        texto(g, "MEJORES TIEMPOS", LX, CY - 8, ORO, 1.1f);
        for (int i = 0; i < pistas.size(); i++) {
            int x = LX + i * 160;
            boolean act = i == selTop, sobre = dentro(mx, my, x, CY + 6, 154, 20);
            caja(g, x, CY + 6, 154, 20, act ? 0xFF3A3A66 : sobre ? 0xFF2A2A4A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
            textoCentrado(g, pistas.get(i).nombre(), x + 77, CY + 12, act ? ORO : BLANCO, 0.9f);
        }
        if (pistas.isEmpty()) return;
        List<MarcaC> l = tops.getOrDefault(pistas.get(Math.min(selTop, pistas.size() - 1)).id(), List.of());
        if (l.isEmpty()) { texto(g, "Aún nadie ha terminado esta pista. ¡Sé el primero!", LX, CY + 40, GRIS, 1f); return; }
        for (int i = 0; i < l.size(); i++) {
            MarcaC m = l.get(i);
            int y = CY + 32 + i * 21;
            caja(g, LX, y, 600, 19, m.mio() ? 0xFF2E2E58 : 0xFF1B1B30, m.pos() == 1 ? ORO : m.mio() ? ORO : 0xFF55557A);
            texto(g, "#" + m.pos(), LX + 8, y + 6, m.pos() == 1 ? ORO : m.pos() == 2 ? 0xFFC0C0D0 : m.pos() == 3 ? 0xFFCD7F32 : BLANCO, 1f);
            texto(g, recortar(m.nombre(), 24), LX + 50, y + 6, m.mio() ? ORO : BLANCO, 1f);
            texto(g, m.especie(), LX + 260, y + 6, GRIS, 0.9f);
            texto(g, m.tiempo(), LX + 500, y + 6, 0xFFFFE070, 1.05f);
        }
    }

    // ------------------------------------------------------------ resultado de la carrera

    private void resultadoCaja(DrawContext g, int mx, int my) {
        int x = 130, y = 90, w = 380, h = 190;
        g.draw();                                   // el texto de debajo se dibuja al final: se vacia antes para que la caja lo tape
        g.getMatrices().push();
        g.getMatrices().translate(0, 0, 400);       // y la caja va por delante en profundidad (si no, el texto de debajo se cuela por encima)
        g.fill(0, 0, W, H, 0xA0000010);
        caja(g, x, y, w, h, 0xFF14142A, ORO);
        boolean ent = resultado.get("entrenamiento").getAsBoolean();
        int puesto = resultado.get("puesto").getAsInt();
        textoCentrado(g, ent ? "¡ENTRENAMIENTO TERMINADO!" : puesto == 1 ? "¡HAS GANADO!" : puesto > 0 ? "¡FIN DE LA CARRERA!" : "CARRERA TERMINADA", x + w / 2, y + 10, ORO, 1.6f);
        textoCentrado(g, resultado.get("pista").getAsString() + (resultado.get("tiempo").getAsString().isEmpty() ? "" : " · " + resultado.get("tiempo").getAsString()), x + w / 2, y + 30, BLANCO, 1f);
        if (puesto > 0 && !ent && resultado.get("jugadores").getAsInt() > 1) textoCentrado(g, "Puesto " + puesto + " de " + resultado.get("jugadores").getAsInt(), x + w / 2, y + 42, LILA, 1f);
        else if (puesto == 0) textoCentrado(g, "No has llegado a la meta", x + w / 2, y + 42, ROJO, 1f);
        if (resultado.get("record").getAsBoolean()) textoCentrado(g, "¡Nuevo récord personal!", x + w / 2, y + 54, VERDE, 1f);
        textoCentrado(g, "+" + resultado.get("estrellas").getAsInt() + " ★  (ahora tienes " + resultado.get("saldo").getAsInt() + ")", x + w / 2, y + 68, 0xFFFFE070, 1.2f);
        JsonArray l = resultado.getAsJsonArray("lineas");
        for (int i = 0; i < l.size() && i < 5; i++) textoCentrado(g, recortar(l.get(i).getAsString(), 70), x + w / 2, y + 88 + i * 11, GRIS, 0.8f);
        boton(g, x + w / 2 - 60, y + h - 30, 120, 22, "ACEPTAR", 0xFF2E8B57, true, dentro(mx, my, x + w / 2 - 60, y + h - 30, 120, 22), 1f);
        g.getMatrices().pop();
    }

    // ------------------------------------------------------------ clics

    @Override
    protected void clic(int mx, int my, int boton) {
        if (boton != 0) return;
        if (resultado != null) {
            if (dentro(mx, my, 130 + 190 - 60, 90 + 190 - 30, 120, 22)) resultado = null;
            return;
        }
        if (dentro(mx, my, 538, 48, 82, 20)) { enviar("salir", "", ""); return; }
        for (int i = 0; i < TABS.length; i++) if (dentro(mx, my, 20 + i * 82, 48, 78, 20)) { tab = i; enviar("refrescar", TABS_ID[i], ""); return; }
        switch (tab) {
            case T_JUGAR -> { if (clicLista(mx, my)) return; clicJugar(mx, my); }
            case T_ENTRENAR -> { if (clicLista(mx, my)) return; clicEntrenar(mx, my); }
            case T_TIENDA -> clicTienda(mx, my);
            case T_TOP -> { for (int i = 0; i < pistas.size(); i++) if (dentro(mx, my, LX + i * 160, CY + 6, 154, 20)) selTop = i; }
            default -> { }
        }
    }

    /** Elegir un Pokemon de la lista; si ya estas apuntado a la carrera, se le dice al servidor. */
    private boolean clicLista(int mx, int my) {
        for (int i = 0; i < equipo.size(); i++) {
            Poke p = equipo.get(i);
            if (!p.valido || !dentro(mx, my, LX, CY + i * 43, LW, 40)) continue;
            sel = p.slot;
            if (tab == T_JUGAR && apuntado && fase.equals("cola")) enviar("elegir", String.valueOf(p.slot), "");
            return true;
        }
        return false;
    }

    private boolean pulsaPista(int mx, int my) {
        for (int i = 0; i < pistas.size(); i++) if (dentro(mx, my, RX, PY + 10 + i * 48, RW, 44)) { selPista = i; return true; }
        return false;
    }

    private void clicJugar(int mx, int my) {
        if (fase.equals("libre")) {
            if (pulsaPista(mx, my)) return;
            int by = PY + 10 + pistas.size() * 48 + 4;
            if (!pistas.isEmpty() && dentro(mx, my, RX, by, RW, 24)) enviar("crear", pistas.get(selPista).id(), "");
        } else if (fase.equals("cola")) {
            int by = 262;
            if (!apuntado) { if (dentro(mx, my, RX, by, RW, 26) && corredores.size() < maxCorredores) enviar("unirse", "", ""); return; }
            if (dentro(mx, my, RX, by, 178, 26)) enviar(listo ? "nolisto" : "listo", "", "");
            else if (dentro(mx, my, RX + 184, by, 178, 26)) enviar("salirCola", "", "");
            else if (anfitrion && dentro(mx, my, RX, by + 30, 178, 24)) enviar("lanzar", "", "");
            else if (anfitrion && dentro(mx, my, RX + 184, by + 30, 178, 24)) enviar("cancelar", "", "");
        }
    }

    private void clicEntrenar(int mx, int my) {
        if (pulsaPista(mx, my)) return;
        int by = PY + 10 + pistas.size() * 48 + 4;
        if (fase.equals("libre") && !pistas.isEmpty() && dentro(mx, my, RX, by, RW, 24)) enviar("entrenar", pistas.get(selPista).id(), sel > 0 ? String.valueOf(sel) : "");
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
