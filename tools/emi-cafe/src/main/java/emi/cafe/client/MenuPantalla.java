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
    private static final int T_CAFE = 0, T_RECETARIO = 1, T_TIENDA = 2, T_ESTRELLAS = 3, T_MISIONES = 4, T_LOGROS = 5, T_DECORAR = 6, T_CAFES = 7, T_PERSONAL = 8, T_BAUL = 9, T_PLANOS = 10, T_HUERTOS = 11, T_GANADO = 12, T_DESPENSA = 13, T_CLIENTELA = 14, T_RESENAS = 15;
    private static final String[] TABS = {"CAFÉ", "RECETARIO", "TIENDA", "ESTRELLAS", "MISIONES", "LOGROS", "DECORAR", "CAFÉS", "PERSONAL", "BAÚL", "PLANOS", "HUERTOS", "GANADO", "DESPENSA", "CLIENTELA", "RESEÑAS"};
    private static final String[] TABS_ID = {"cafe", "recetario", "tienda", "estrellas", "misiones", "logros", "decorar", "cafes", "personal", "baul", "planos", "huertos", "ganado", "despensa", "clientela", "resenas"};
    // las 11 pantallas se reparten en 6 pestañas grandes; CONSTRUCCIÓN y PROGRESO llevan una fila de subpestañas debajo (desde 0.23.0)
    private static final String[] TOP = {"CAFÉ", "RECETARIO", "PERSONAL", "GRANJA", "CAFÉS", "CONSTRUCCIÓN", "PROGRESO"};
    private static final int[] TOP_REF = {T_CAFE, T_RECETARIO, T_PERSONAL, 102, T_CAFES, 100, 101};          // >= 100: un grupo de subpestañas (SUBS[ref - 100])
    private static final int[][] SUBS = {{T_DECORAR, T_BAUL, T_PLANOS}, {T_TIENDA, T_MISIONES, T_LOGROS, T_CLIENTELA, T_RESENAS, T_ESTRELLAS}, {T_HUERTOS, T_GANADO, T_DESPENSA}};
    private static final String[][] SUBN = {{"DECORAR", "BAÚL", "PLANOS"}, {"MEJORAS Y PLATOS", "MISIONES", "LOGROS", "CLIENTELA", "RESEÑAS", "RECOMPENSAS"}, {"HUERTOS", "GANADO", "DESPENSA"}};
    private static final int TOP_W = 72, SUBY = 20;
    private static int subW(int gr) { return SUBS[gr].length > 5 ? 84 : SUBS[gr].length > 4 ? 100 : 124; }          // con 5 subpestañas se estrechan para caber                    // ancho de cada pestaña grande y de cada subpestaña, y lo que baja el contenido para dejarles sitio
    private final int[] subActual = {T_DECORAR, T_TIENDA, T_HUERTOS};                           // la ultima subpestaña de cada grupo

    private static int grupoDe(int t) { for (int g = 0; g < SUBS.length; g++) for (int x : SUBS[g]) if (x == t) return g; return -1; }
    private static int topDe(int t) { for (int i = 0; i < TOP_REF.length; i++) if (TOP_REF[i] == t || (TOP_REF[i] >= 100 && grupoDe(t) == TOP_REF[i] - 100)) return i; return 0; }

    private void irATab(int t) {
        tab = t;
        int g = grupoDe(t);
        if (g >= 0) subActual[g] = t;
        enviar("refrescar", TABS_ID[t], "");
    }

    private static final int FONDO = 0xFF1B1B30, BORDE = 0xFF55557A, ACTIVO = 0xFF2E2E58, SUAVE = 0xFF8888AA, MONEDA = 0xFFFFD34E, ESTRELLA = 0xFFFFE070;

    /** La pantalla abierta ahora (para actualizarla cuando llegan datos nuevos del servidor). */
    public static MenuPantalla abierta;

    // ------------------------------------------------------------ datos
    private record DecoC(String id, String item, String nombre, int cantidad, int precio, String grupo, int tienes, boolean fav) {}
    private record PlanoC(String clave, String nombre, boolean auto, String fecha, int bloques) {}
    private record TemaC(String id, String nombre, String desc, int precio) {}
    private record BaulC(String item, int n, boolean fuera, int mano) {}
    private record InicialC(String id, String nombre, String item, String plato, String receta) {}
    private record PlatoC(String id, String item, String nombre, int precio, int coste, String receta, boolean tengo, boolean especial, String michi, int nivelLocal, String bloqueo, int servidos, int mnivel, int msig) {}
    private record MejoraC(String id, String item, String nombre, String texto, int nivel, int max, int precio) {}
    private record CafeC(String nombre, String titulo, int megusta, int estilo, int rep, boolean abierto, boolean visitas, int gente, boolean dentro, double valoracion, int nresenas) {}
    private record PersonaC(int idx, String nombre, String rol, String icono, String aspecto, int nivel, int costo, String tope, String efecto, int felicidad, String animo, int premio, String especial) {}
    private record ContratoC(String rol, String nombre, String texto, String icono, int precio, String bloqueo, int tienes, int max) {}
    private record TopC(String nombre, int megusta, int rep, int estilo) {}
    private record EstiloC(String parte, String nombre) {}
    private record GenteC(String nombre, boolean yo, boolean host, boolean visita) {}
    private record MisionC(String texto, int prog, int objetivo, int premio, boolean hecha) {}
    private record ResenaC(String esp, int est, String plato, String texto, boolean critico) {}
    private final List<ResenaC> resenas = new ArrayList<>();
    private double resMedia; private int resN, resTotal; private long resProveedor; private String resEfecto = ""; private final int[] resDist = new int[5];
    private int pagResenas;
    private record ClienteC(String id, String nombre, int visitas, int nivel, int sig, int pago) {}
    private final List<ClienteC> clientela = new ArrayList<>();
    private int pagClientela;
    private record LogroC(String nombre, String texto, int nivel, int max, long valor, long meta, int premio) {}
    private record ArticuloC(String id, String item, String nombre, String texto, int precio, String grupo, int hoy, boolean unico, boolean comprado) {}

    private record FincaC(int slot, String tipo, String nombre, String kind, String icono, String fuente, int nivel, int prog, long resta, String granjero, String aspecto, int coste, String tope, int rend, int bonus, String calidad, int animales, long cosechas) {}
    private record CatC(String id, String nombre, String kind, String icono, int precio, int ciclo, String fuente, String texto, String bloqueo) {}
    private record DespC(String item, String nombre, int total, int[] q) {}

    private final List<InicialC> iniciales = new ArrayList<>();
    private final List<FincaC> fincas = new ArrayList<>();
    private final List<CatC> catalogoGranja = new ArrayList<>();
    private final List<DespC> despensa = new ArrayList<>();
    private final List<String> calidadesGranja = new ArrayList<>();
    private final List<Integer> calidadesPct = new ArrayList<>();
    private int granjaMax, granjaTienes, granjaMinLocal = 5, granjaCap = 120, pagCatGranja, pagFincas;
    private long granjaCosechado, granjaExtra;
    private final List<PlatoC> platos = new ArrayList<>();            // todos, para el recetario
    private final List<PlatoC> tiendaPlatos = new ArrayList<>();      // los que se compran (sin los de visitantes especiales)
    private final List<MejoraC> mejoras = new ArrayList<>();
    private final List<DecoC> deco = new ArrayList<>();                        // la pagina que se ve (el servidor manda solo 12)
    private final List<String> decoFuentes = new ArrayList<>(), decoGrupos = new ArrayList<>();
    private int decoFuente, decoGrupo, decoPag, decoPags = 1, decoTotal;
    private String decoBuscar = "";
    private final List<BaulC> baul = new ArrayList<>();
    private int pagBaul;
    private boolean decoFav;                                                  // se ve la lista de favoritos
    private int nFav, baulFuente, baulGrupo, baulTotal, deshacerN, planosMax = 6;
    private final List<String> baulGrupos = new ArrayList<>();
    private String baulBuscar = "";
    private final List<String> baulFuentes = new ArrayList<>();
    private final List<PlanoC> planos = new ArrayList<>();
    private final List<TemaC> temas = new ArrayList<>();
    private int planosSub;                                                    // 0 = mis planos, 1 = cafes tematicos
    private String confirmaTema = "";                                         // id del tema que se va a comprar (segundo clic para confirmar)
    private String confirmaBorrar = "";                                       // clave del plano que se va a borrar (segundo clic para confirmar)
    private String tipItem = "", tipNombre = "", tipExtra = "";               // el objeto sobre el que esta el raton (vista ampliada)
    private boolean construyendo;
    private boolean libre;                                                      // parcela libre (desde 0.18.0): se construye comprando, sin materiales que cambiar
    private String guardado = "";                                           // lo del cafe que tiene guardado para poner
    private int mesasUsadas, mesasMax;
    private final List<EstiloC> estilo = new ArrayList<>();
    private final List<PersonaC> personal = new ArrayList<>();
    private final List<ContratoC> contratos = new ArrayList<>();
    private String funcional = "";
    private int personalMax, bonoPaciencia, bonoPropina, uniformePct, contratoPag;
    private int persPag;                                  // pagina de la lista de empleados (5 por pagina)
    private String nombreCafe = "";
    private final List<CafeC> cafesAbiertos = new ArrayList<>();
    private final List<TopC> topCafes = new ArrayList<>();
    private boolean visitante, visitasOn = true, yaMeGusta;
    private int nivelLocal = 1, estUsadas, estMax, megusta, nVisitantes, repNivel, repValor, repSig, estPuntos, estNivel, estSig;
    private String repNombre = "", estNombre = "", evento = "", eventoTexto = "";
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
    private int pagRec, pagTienda, pagCafes, pagMejoras;                    // pagina del recetario (6 platos) y de los platos de la tienda (3)
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
        if (!soloDatos) for (int i = 0; i < TABS_ID.length; i++) if (TABS_ID[i].equals(t)) { tab = i; int gg = grupoDe(i); if (gg >= 0) subActual[gg] = i; }
        monedas = o.get("monedas").getAsInt(); estrellas = o.get("estrellas").getAsInt(); clientes = o.get("clientes").getAsInt();
        turnos = o.get("turnos").getAsInt(); mejorTurno = o.get("mejorTurno").getAsInt(); platosLimpios = o.get("platosLimpios").getAsInt();
        duenio = o.get("duenio").getAsBoolean(); lleno = o.get("lleno").getAsBoolean();
        nombreDuenio = o.get("nombreDuenio").getAsString(); estado = o.get("estado").getAsString(); inicial = o.get("inicial").getAsString();

        iniciales.clear();
        for (JsonElement e : o.getAsJsonArray("iniciales")) { JsonObject j = e.getAsJsonObject(); iniciales.add(new InicialC(s(j, "id"), s(j, "nombre"), s(j, "item"), s(j, "plato"), s(j, "receta"))); }
        platos.clear();
        for (JsonElement e : o.getAsJsonArray("platos")) {
            JsonObject j = e.getAsJsonObject();
            platos.add(new PlatoC(s(j, "id"), s(j, "item"), s(j, "nombre"), j.get("precio").getAsInt(), j.get("coste").getAsInt(), s(j, "receta"), j.get("tengo").getAsBoolean(), j.has("especial") && j.get("especial").getAsBoolean(), j.has("michi") ? s(j, "michi") : "", j.has("nivelLocal") ? j.get("nivelLocal").getAsInt() : 1, j.has("bloqueo") ? s(j, "bloqueo") : "", j.has("servidos") ? j.get("servidos").getAsInt() : 0, j.has("mnivel") ? j.get("mnivel").getAsInt() : 0, j.has("msig") ? j.get("msig").getAsInt() : -1));
        }
        construyendo = o.has("construyendo") && o.get("construyendo").getAsBoolean();
        mesasUsadas = o.has("mesasUsadas") ? o.get("mesasUsadas").getAsInt() : 0; mesasMax = o.has("mesasMax") ? o.get("mesasMax").getAsInt() : 0;
        deco.clear();
        if (o.has("deco")) for (JsonElement e : o.getAsJsonArray("deco")) { JsonObject j = e.getAsJsonObject(); deco.add(new DecoC(s(j, "id"), s(j, "item"), s(j, "nombre"), j.get("cantidad").getAsInt(), j.get("precio").getAsInt(), s(j, "grupo"), j.has("tienes") ? j.get("tienes").getAsInt() : 0, j.has("fav") && j.get("fav").getAsBoolean())); }
        decoFuentes.clear(); decoGrupos.clear();
        if (o.has("decoFuentes")) for (JsonElement e : o.getAsJsonArray("decoFuentes")) decoFuentes.add(e.getAsString());
        if (o.has("decoGrupos")) for (JsonElement e : o.getAsJsonArray("decoGrupos")) decoGrupos.add(e.getAsString());
        decoFuente = o.has("decoFuente") ? o.get("decoFuente").getAsInt() : 0; decoGrupo = o.has("decoGrupo") ? o.get("decoGrupo").getAsInt() : 0;
        decoPag = o.has("decoPag") ? o.get("decoPag").getAsInt() : 0; decoPags = o.has("decoPags") ? o.get("decoPags").getAsInt() : 1;
        decoTotal = o.has("decoTotal") ? o.get("decoTotal").getAsInt() : deco.size(); decoBuscar = o.has("decoBuscar") ? o.get("decoBuscar").getAsString() : "";
        decoFav = o.has("decoFav") && o.get("decoFav").getAsBoolean(); nFav = o.has("nFav") ? o.get("nFav").getAsInt() : 0;
        baulFuentes.clear();
        if (o.has("baulFuentes")) for (JsonElement e : o.getAsJsonArray("baulFuentes")) baulFuentes.add(e.getAsString());
        baulGrupos.clear();
        if (o.has("baulGrupos")) for (JsonElement e : o.getAsJsonArray("baulGrupos")) baulGrupos.add(e.getAsString());
        baulGrupo = o.has("baulGrupo") ? o.get("baulGrupo").getAsInt() : 0;
        baulFuente = o.has("baulFuente") ? o.get("baulFuente").getAsInt() : 0; baulBuscar = o.has("baulBuscar") ? o.get("baulBuscar").getAsString() : "";
        baulTotal = o.has("baulTotal") ? o.get("baulTotal").getAsInt() : 0; deshacerN = o.has("deshacer") ? o.get("deshacer").getAsInt() : 0;
        planos.clear();
        if (o.has("planos")) for (JsonElement e : o.getAsJsonArray("planos")) { JsonObject j = e.getAsJsonObject(); planos.add(new PlanoC(s(j, "clave"), s(j, "nombre"), j.get("auto").getAsBoolean(), s(j, "fecha"), j.get("bloques").getAsInt())); }
        planosMax = o.has("planosMax") ? o.get("planosMax").getAsInt() : 6;
        temas.clear();
        if (o.has("temas")) for (JsonElement e : o.getAsJsonArray("temas")) { JsonObject j = e.getAsJsonObject(); temas.add(new TemaC(s(j, "id"), s(j, "nombre"), s(j, "desc"), j.get("precio").getAsInt())); }
        baul.clear();
        if (o.has("baul")) for (JsonElement e : o.getAsJsonArray("baul")) { JsonObject j = e.getAsJsonObject(); baul.add(new BaulC(s(j, "item"), j.get("n").getAsInt(), j.get("fuera").getAsBoolean(), j.get("mano").getAsInt())); }
        libre = o.has("libre") && o.get("libre").getAsBoolean();
        guardado = o.has("guardado") ? o.get("guardado").getAsString() : "";
        estilo.clear();
        if (o.has("estilo")) for (JsonElement e : o.getAsJsonArray("estilo")) { JsonObject j = e.getAsJsonObject(); estilo.add(new EstiloC(s(j, "parte"), s(j, "nombre"))); }
        nombreCafe = o.has("nombreCafe") ? o.get("nombreCafe").getAsString() : "";
        personal.clear();
        if (o.has("personal")) for (JsonElement e : o.getAsJsonArray("personal")) { JsonObject j = e.getAsJsonObject(); personal.add(new PersonaC(j.get("idx").getAsInt(), s(j, "nombre"), s(j, "rol"), s(j, "icono"), s(j, "aspecto"), j.has("nivel") ? j.get("nivel").getAsInt() : 1, j.has("costo") ? j.get("costo").getAsInt() : 0, s(j, "tope"), s(j, "efecto"), j.has("felicidad") ? j.get("felicidad").getAsInt() : 70, j.has("animo") ? s(j, "animo") : "", j.has("premio") ? j.get("premio").getAsInt() : 0, j.has("especial") ? s(j, "especial") : "")); }
        bonoPaciencia = o.has("bonoPaciencia") ? o.get("bonoPaciencia").getAsInt() : 0; bonoPropina = o.has("bonoPropina") ? o.get("bonoPropina").getAsInt() : 0; uniformePct = o.has("uniformePct") ? o.get("uniformePct").getAsInt() : 0;
        funcional = o.has("funcional") ? o.get("funcional").getAsString() : "";
        personalMax = o.has("personalMax") ? o.get("personalMax").getAsInt() : 1;
        contratos.clear();
        if (o.has("contratos")) for (JsonElement e : o.getAsJsonArray("contratos")) { JsonObject j = e.getAsJsonObject(); contratos.add(new ContratoC(s(j, "rol"), s(j, "nombre"), s(j, "texto"), s(j, "icono"), j.get("precio").getAsInt(), s(j, "bloqueo"), j.has("tienes") ? j.get("tienes").getAsInt() : 0, j.has("max") ? j.get("max").getAsInt() : 1)); }
        nivelLocal = o.has("nivelLocal") ? o.get("nivelLocal").getAsInt() : 1; estUsadas = o.has("estUsadas") ? o.get("estUsadas").getAsInt() : 0; estMax = o.has("estMax") ? o.get("estMax").getAsInt() : 0;
        visitante = o.has("visitante") && o.get("visitante").getAsBoolean();
        visitasOn = !o.has("visitas") || o.get("visitas").getAsBoolean();
        yaMeGusta = o.has("yaMeGusta") && o.get("yaMeGusta").getAsBoolean();
        megusta = o.has("megusta") ? o.get("megusta").getAsInt() : 0; nVisitantes = o.has("nVisitantes") ? o.get("nVisitantes").getAsInt() : 0;
        repNivel = o.has("repNivel") ? o.get("repNivel").getAsInt() : 1; repValor = o.has("repValor") ? o.get("repValor").getAsInt() : 0; repSig = o.has("repSig") ? o.get("repSig").getAsInt() : -1;
        repNombre = o.has("repNombre") ? o.get("repNombre").getAsString() : "";
        estPuntos = o.has("estPuntos") ? o.get("estPuntos").getAsInt() : 0; estNivel = o.has("estNivel") ? o.get("estNivel").getAsInt() : 0; estSig = o.has("estSig") ? o.get("estSig").getAsInt() : -1;
        estNombre = o.has("estNombre") ? o.get("estNombre").getAsString() : "";
        evento = o.has("evento") ? o.get("evento").getAsString() : ""; eventoTexto = o.has("eventoTexto") ? o.get("eventoTexto").getAsString() : "";
        cafesAbiertos.clear();
        if (o.has("cafes")) for (JsonElement e : o.getAsJsonArray("cafes")) { JsonObject j = e.getAsJsonObject(); cafesAbiertos.add(new CafeC(s(j, "nombre"), j.has("titulo") ? s(j, "titulo") : "", j.get("megusta").getAsInt(), j.get("estilo").getAsInt(), j.get("rep").getAsInt(), j.get("abierto").getAsBoolean(), j.get("visitas").getAsBoolean(), j.get("gente").getAsInt(), j.has("dentro") && j.get("dentro").getAsBoolean(), j.has("valoracion") ? j.get("valoracion").getAsDouble() : 0, j.has("nresenas") ? j.get("nresenas").getAsInt() : 0)); }
        topCafes.clear();
        if (o.has("top")) for (JsonElement e : o.getAsJsonArray("top")) { JsonObject j = e.getAsJsonObject(); topCafes.add(new TopC(s(j, "nombre"), j.get("megusta").getAsInt(), j.get("rep").getAsInt(), j.get("estilo").getAsInt())); }
        fincas.clear(); catalogoGranja.clear(); despensa.clear(); calidadesGranja.clear(); calidadesPct.clear();
        if (o.has("granja")) {
            JsonObject gj = o.getAsJsonObject("granja");
            granjaMax = gj.get("max").getAsInt(); granjaTienes = gj.get("tienes").getAsInt(); granjaMinLocal = gj.get("minLocal").getAsInt(); granjaCap = gj.get("capacidad").getAsInt();
            granjaCosechado = gj.get("cosechado").getAsLong(); granjaExtra = gj.get("extra").getAsLong();
            for (JsonElement e : gj.getAsJsonArray("calidades")) { JsonObject j = e.getAsJsonObject(); calidadesGranja.add(s(j, "nombre")); calidadesPct.add(j.get("pct").getAsInt()); }
            for (JsonElement e : gj.getAsJsonArray("fincas")) {
                JsonObject j = e.getAsJsonObject();
                fincas.add(new FincaC(j.get("slot").getAsInt(), s(j, "tipo"), s(j, "nombre"), s(j, "kind"), s(j, "icono"), s(j, "fuente"), j.get("nivel").getAsInt(), j.get("prog").getAsInt(), j.get("resta").getAsLong(),
                        s(j, "granjero"), s(j, "aspecto"), j.get("coste").getAsInt(), s(j, "tope"), j.get("rend").getAsInt(), j.get("bonus").getAsInt(), s(j, "calidad"), j.get("animales").getAsInt(), j.get("cosechas").getAsLong()));
            }
            for (JsonElement e : gj.getAsJsonArray("catalogo")) {
                JsonObject j = e.getAsJsonObject();
                catalogoGranja.add(new CatC(s(j, "id"), s(j, "nombre"), s(j, "kind"), s(j, "icono"), j.get("precio").getAsInt(), j.get("ciclo").getAsInt(), s(j, "fuente"), s(j, "texto"), s(j, "bloqueo")));
            }
            for (JsonElement e : gj.getAsJsonArray("despensa")) {
                JsonObject j = e.getAsJsonObject();
                int[] q = new int[5];
                for (int i = 0; i < 5 && i < j.getAsJsonArray("q").size(); i++) q[i] = j.getAsJsonArray("q").get(i).getAsInt();
                despensa.add(new DespC(s(j, "item"), s(j, "nombre"), j.get("total").getAsInt(), q));
            }
        }
        estacionesEnMano = o.has("estacionesEnMano") ? o.get("estacionesEnMano").getAsInt() : 0;
        tiendaPlatos.clear();
        for (PlatoC p : platos) if (!p.especial()) tiendaPlatos.add(p);
        mejoras.clear();
        for (JsonElement e : o.getAsJsonArray("mejoras")) {
            JsonObject j = e.getAsJsonObject();
            mejoras.add(new MejoraC(s(j, "id"), s(j, "item"), s(j, "nombre"), s(j, "texto"), j.get("nivel").getAsInt(), j.get("max").getAsInt(), j.get("precio").getAsInt()));
        }
        gente.clear();
        for (JsonElement e : o.getAsJsonArray("gente")) { JsonObject j = e.getAsJsonObject(); gente.add(new GenteC(s(j, "nombre"), j.get("yo").getAsBoolean(), j.get("host").getAsBoolean(), j.has("visita") && j.get("visita").getAsBoolean())); }
        invitables.clear();
        for (JsonElement e : o.getAsJsonArray("invitables")) invitables.add(s(e.getAsJsonObject(), "nombre"));

        misiones(dia, o.getAsJsonArray("dia"));
        misiones(semana, o.getAsJsonArray("semana"));
        logros.clear();
        for (JsonElement e : o.getAsJsonArray("logros")) {
            JsonObject j = e.getAsJsonObject();
            logros.add(new LogroC(s(j, "nombre"), s(j, "texto"), j.get("nivel").getAsInt(), j.get("max").getAsInt(), j.get("valor").getAsLong(), j.get("meta").getAsLong(), j.get("premio").getAsInt()));
        }
        resenas.clear(); resMedia = 0; resN = resTotal = 0; resProveedor = 0; resEfecto = ""; java.util.Arrays.fill(resDist, 0);
        if (o.has("resenas")) {
            JsonObject rj = o.getAsJsonObject("resenas");
            resMedia = rj.get("media").getAsDouble(); resN = rj.get("n").getAsInt(); resTotal = rj.get("total").getAsInt(); resProveedor = rj.get("proveedor").getAsLong(); resEfecto = s(rj, "efecto");
            int k = 0; for (JsonElement e : rj.getAsJsonArray("dist")) if (k < 5) resDist[k++] = e.getAsInt();
            for (JsonElement e : rj.getAsJsonArray("lista")) { JsonObject j = e.getAsJsonObject(); resenas.add(new ResenaC(s(j, "esp"), j.get("est").getAsInt(), s(j, "plato"), s(j, "texto"), j.has("critico") && j.get("critico").getAsBoolean())); }
        }
        clientela.clear();
        if (o.has("clientela")) for (JsonElement e : o.getAsJsonArray("clientela")) { JsonObject j = e.getAsJsonObject(); clientela.add(new ClienteC(s(j, "id"), s(j, "nombre"), j.get("visitas").getAsInt(), j.get("nivel").getAsInt(), j.get("sig").getAsInt(), j.get("pago").getAsInt())); }
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

    /** Barra de estrellas llenas/vacías; nunca negativa aunque el nivel pase del máximo (así se colgó el menú con empleados de nivel 6). */
    private static String estrellas(int nivel, int max) {
        int n = Math.max(0, Math.min(max, nivel));
        return "★".repeat(n) + "☆".repeat(max - n);
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

    private void ponerTip(String item, String nombre, String extra) { tipItem = item; tipNombre = nombre; tipExtra = extra; }

    /** La vista ampliada del objeto sobre el que esta el raton (se dibuja encima de todo). */
    private void dibujarTip(DrawContext g, int mx, int my) {
        if (tipItem.isEmpty()) return;
        int w = 150, h = 108;
        int x = Math.min(mx + 10, 640 - w - 4), y = Math.min(my + 10, 360 - h - 4);
        var m = g.getMatrices();
        m.push(); m.translate(0, 0, 450);                                    // por encima de todo lo demas (los textos de debajo no se ven a traves)
        caja(g, x, y, w, h, 0xFF101022, ORO);
        objeto(g, tipItem, x + (w - 64) / 2, y + 6, 4f);
        textoCentrado(g, recortar(tipNombre, 30), x + w / 2, y + 74, BLANCO, 0.85f);
        textoCentrado(g, recortar(tipExtra, 38), x + w / 2, y + 90, SUAVE, 0.6f);
        m.pop();
    }

    // ------------------------------------------------------------ dibujo

    private static final int LX = 20, LW = 226, RX = 258, RW = 362, CY = 78;

    @Override
    protected void dibujar(DrawContext g, int mx, int my, float delta) {
        tipItem = "";
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoRefresco > 1500) { ultimoRefresco = ahora; enviar("refrescar", TABS_ID[tab], ""); }
        String titulo = nombreCafe.isEmpty() ? "CAFÉ MICHIALIEN" : nombreCafe.toUpperCase();
        texto(g, titulo, 20, 10, ORO, titulo.length() > 18 ? 1.6f : 2.2f);
        texto(g, duenio ? "Sirve a los Pokémon y haz crecer tu café" : "Estás en el café de " + nombreDuenio, 22, 32, GRIS, 0.9f);
        texto(g, "● " + monedas + " monedas   ★ " + estrellas + " estrellas", 400, 10, MONEDA, 1.2f);
        texto(g, "Clientes: " + clientes + " · Jornadas: " + turnos + " · Mejor jornada: " + mejorTurno, 400, 26, GRIS, 0.8f);
        texto(g, "Las monedas son del café; las estrellas, de la tienda del servidor", 400, 36, LILA, 0.65f);

        for (int i = 0; i < TOP.length; i++) {
            int x = 20 + i * TOP_W;
            boolean act = i == topDe(tab), sobre = dentro(mx, my, x, 48, TOP_W - 4, 20);
            caja(g, x, 48, TOP_W - 4, 20, act ? 0xFF3A3A66 : sobre ? 0xFF2A2A4A : FONDO, act ? ORO : BORDE);
            textoCentrado(g, TOP[i], x + (TOP_W - 4) / 2, 55, act ? ORO : BLANCO, 0.78f);
        }
        boton(g, 546, 48, 74, 20, "SALIR", 0xFF8A3A3A, true, dentro(mx, my, 546, 48, 74, 20), 1f);
        int gr = grupoDe(tab);
        if (gr >= 0) {
            for (int k = 0; k < SUBS[gr].length; k++) {
                int x = 20 + k * (subW(gr) + 4);
                boolean act = SUBS[gr][k] == tab, sobre = dentro(mx, my, x, 70, subW(gr), 14);
                caja(g, x, 70, subW(gr), 14, act ? 0xFF3A3A66 : sobre ? 0xFF2A2A4A : 0xFF14142A, act ? ORO : BORDE);
                textoCentrado(g, SUBN[gr][k], x + subW(gr) / 2, 74, act ? ORO : sobre ? BLANCO : GRIS, 0.7f);
            }
            var m = g.getMatrices();
            m.push(); m.translate(0, SUBY, 0);
            contenido(g, mx, my - SUBY);
            m.pop();
        } else contenido(g, mx, my);
        dibujarTip(g, mx, my);
        if (resumen != null) resumenCaja(g, mx, my);
    }

    private void contenido(DrawContext g, int mx, int my) {
        switch (tab) {
            case T_CAFE -> cafe(g, mx, my);
            case T_RECETARIO -> recetario(g, mx, my);
            case T_TIENDA -> tienda(g, mx, my);
            case T_ESTRELLAS -> estrellasTab(g, mx, my);
            case T_MISIONES -> misionesTab(g);
            case T_DECORAR -> decorar(g, mx, my);
            case T_CAFES -> cafesTab(g, mx, my);
            case T_PERSONAL -> personalTab(g, mx, my);
            case T_BAUL -> baulTab(g, mx, my);
            case T_PLANOS -> planosTab(g, mx, my);
            case T_HUERTOS -> granjaTab(g, mx, my, "huerto");
            case T_GANADO -> granjaTab(g, mx, my, "corral");
            case T_DESPENSA -> despensaTab(g, mx, my);
            case T_CLIENTELA -> clientelaTab(g, mx, my);
            case T_RESENAS -> resenasTab(g, mx, my);
            default -> logrosTab(g);
        }
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
        if (visitante) {
            texto(g, "DE VISITA", LX, CY - 7, ORO, 1.1f);
            parrafo(g, "Estás en el café de " + nombreDuenio + ". Puedes mirar cómo lo decoró, pero no tocar nada. Si te gusta, dale «me gusta»: el dueño gana estrellas y reputación.", LX, CY + 10, LW, GRIS, 0.85f);
            infoCafe(g, CY + 62);
            boolean puede = !yaMeGusta;
            boton(g, LX, CY + 100, LW, 28, yaMeGusta ? "YA LE DISTE «ME GUSTA» ♥" : "ME GUSTA ♥", 0xFFB0407A, puede, puede && dentro(mx, my, LX, CY + 100, LW, 28), 1f);
            boton(g, LX, CY + 134, LW, 24, "VER OTROS CAFÉS", 0xFF7A4AC0, true, dentro(mx, my, LX, CY + 134, LW, 24), 0.9f);
        } else if (construyendo) {
            texto(g, "MODO CONSTRUCCIÓN", LX, CY - 7, ORO, 1.1f);
            parrafo(g, libre ? "Construyes con lo que tienes (tu inventario, E). Clic derecho = poner; clic izquierdo = quitar (te lo devuelve). Agachado + clic en el suelo = cambiar el suelo. Lo que falte se compra en DECORAR."
                    : "Compra bloques en DECORAR. Clic derecho = poner; clic izquierdo = recoger. Decora dentro y fuera (jardín, lados, acera). Mesas y estaciones: recógelas y ponlas en otro sitio.", LX, CY + 10, LW, GRIS, 0.8f);
            texto(g, "Mesas: " + mesasUsadas + "/" + (mesasMax >= 9999 ? "∞" : mesasMax) + " colocadas", LX, CY + 82, BLANCO, 0.9f);
            boton(g, LX, CY + 100, LW, 28, "IR A DECORAR (tienda)", 0xFF7A4AC0, true, dentro(mx, my, LX, CY + 100, LW, 28), 1f);
            boton(g, LX, CY + 134, LW, 28, "TERMINAR DE CONSTRUIR", 0xFF2E8B57, true, dentro(mx, my, LX, CY + 134, LW, 28), 1f);
            if (libre) boton(g, LX, CY + 166, LW, 24, deshacerN > 0 ? "↶ DESHACER (" + deshacerN + ")" : "↶ DESHACER", 0xFFB08A20, deshacerN > 0, deshacerN > 0 && dentro(mx, my, LX, CY + 166, LW, 24), 0.9f);
        } else switch (estado) {
            case "abierto" -> {
                texto(g, "CAFÉ ABIERTO", LX, CY - 7, VERDE, 1.1f);
                parrafo(g, "El café está abierto y no hay reloj: cierra cuando quieras desde aquí. Cocina llevando los ingredientes en las manos y sirve con clic derecho al cliente o a su mesa.", LX, CY + 10, LW, GRIS, 0.85f);
                texto(g, "Platos limpios: " + platosLimpios, LX, CY + 62, platosLimpios == 0 ? ROJO : BLANCO, 1f);
                if (!evento.isEmpty()) { texto(g, "HOY: " + recortar(evento, 52), LX, CY + 150, ORO, 0.75f); parrafo(g, eventoTexto, LX, CY + 161, LW, GRIS, 0.65f); }
                else infoCafe(g, CY + 156);
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
                infoCafe(g, CY + 160);
            }
        }
        if (construyendo && libre) {                                         // la parte de la derecha: lo que tiene guardado
            texto(g, "TU OBRA", RX, CY - 7, ORO, 0.95f);
            parrafo(g, "Puesto y por poner: pon la caja registradora, las mesas, las estaciones de tus platos, el cartel (pegado a la cara sur de una pared) y el libro de recetas donde quieras. Si algo se te acaba, cómpralo en DECORAR. Para subir de piso, construye escaleras: los clientes y los michis las suben solos.", RX, CY + 8, RW, SUAVE, 0.8f);
            if (!guardado.isEmpty()) parrafo(g, "Del café: " + guardado, RX, CY + 100, RW, 0xFF9AC8FF, 0.75f);
            return;
        }
        if (construyendo) {                                                  // la parte de la derecha: los materiales del local
            for (int i = 0; i < estilo.size(); i++) {
                int y = CY + 4 + i * 36;
                texto(g, estilo.get(i).parte(), RX, y, SUAVE, 0.8f);
                boton(g, RX, y + 11, 24, 20, "◀", 0xFF7A4AC0, duenio, duenio && dentro(mx, my, RX, y + 11, 24, 20), 1f);
                caja(g, RX + 28, y + 11, RW - 56, 20, FONDO, BORDE);
                textoCentrado(g, estilo.get(i).nombre(), RX + RW / 2, y + 17, BLANCO, 0.95f);
                boton(g, RX + RW - 24, y + 11, 24, 20, "▶", 0xFF7A4AC0, duenio, duenio && dentro(mx, my, RX + RW - 24, y + 11, 24, 20), 1f);
            }
            if (estacionesEnMano > 0) texto(g, "Llevas " + estacionesEnMano + " estación(es) en la mano: ponla(s) en la cocina", RX, CY + 200, 0xFFFFD27F, 0.8f);
            return;
        }
        // la parte de la derecha: quien esta en el cafe y a quien invitar
        texto(g, "EN EL CAFÉ (" + gente.size() + ")", RX, CY - 7, ORO, 0.95f);
        if (duenio && !abierto() && !estado.equals("cerrando")) boton(g, RX + 96, CY - 11, 76, 14, "✎ NOMBRE", 0xFF7A4AC0, true, dentro(mx, my, RX + 96, CY - 11, 76, 14), 0.8f);
        if (duenio) boton(g, RX + RW - 170, CY - 11, 54, 14, "GUÍA", 0xFF7A4AC0, true, dentro(mx, my, RX + RW - 170, CY - 11, 54, 14), 0.8f);
        if (duenio && !abierto() && !estado.equals("cerrando")) boton(g, RX + RW - 110, CY - 11, 110, 14, visitasOn ? "VISITAS: SÍ" : "VISITAS: NO", visitasOn ? 0xFF2E8B57 : 0xFF8A3A3A, true, dentro(mx, my, RX + RW - 110, CY - 11, 110, 14), 0.8f);
        for (int i = 0; i < gente.size() && i < 4; i++) {
            GenteC c = gente.get(i);
            int y = CY + 8 + i * 20;
            caja(g, RX, y, RW, 18, c.yo() ? ACTIVO : FONDO, c.yo() ? ORO : BORDE);
            texto(g, (c.host() ? "★ " : "") + recortar(c.nombre(), 24), RX + 8, y + 5, c.yo() ? ORO : BLANCO, 0.95f);
            texto(g, c.host() ? "dueño" : c.visita() ? "de visita" : "invitado", RX + 300, y + 5, c.host() ? ORO : SUAVE, 0.85f);
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

    /** Tres lineas con la reputacion, el estilo y los «me gusta» del cafe. */
    private void infoCafe(DrawContext g, int y) {
        String rep = "Reputación: " + repNombre + " (nivel " + repNivel + ")" + (repSig > 0 ? " · " + repValor + "/" + repSig : " · máximo");
        String est = "Estilo: " + estNombre + " · " + estPuntos + " pts" + (estSig > 0 ? "/" + estSig : "");
        texto(g, rep, LX, y, 0xFFFFD27F, 0.75f);
        texto(g, est, LX, y + 11, 0xFFFF9AD5, 0.75f);
        texto(g, "Local nivel " + nivelLocal + " · estaciones " + estUsadas + "/" + estMax + " · mesas " + mesasUsadas + "/" + (mesasMax >= 9999 ? "∞" : mesasMax), LX, y + 33, 0xFF8AE0A0, 0.75f);
        if (!funcional.isEmpty()) texto(g, "Decoración útil: " + recortar(funcional, 60), LX, y + 44, 0xFF9AC8FF, 0.7f);
        texto(g, "♥ " + megusta + " me gusta" + (nVisitantes > 0 ? " · " + nVisitantes + " de visita" : "") + (resN > 0 ? " · ★ " + String.format("%.1f", resMedia) + " (" + resN + " reseñas)" : ""), LX, y + 22, 0xFFFF7AA8, 0.75f);
    }

    // ------------------------------------------------------------ cafes de otros: visitar y top

    private static final int CAFES_POR_PAG = 6;

    private void cafesTab(DrawContext g, int mx, int my) {
        texto(g, "CAFÉS DE LA CIUDAD", LX, CY - 8, ORO, 1.0f);
        int total = paginas(cafesAbiertos.size(), CAFES_POR_PAG);
        pagCafes = Math.min(pagCafes, total - 1);
        if (total > 1) paginador(g, mx, my, LX + 288, CY - 12, pagCafes, total);
        if (cafesAbiertos.isEmpty()) parrafo(g, "Todavía no hay más vecinos. Cuando un amigo entre a su café tendrá su parcela en la calle y saldrá aquí.", LX, CY + 8, 340, GRIS, 0.85f);
        for (int n = 0; n < CAFES_POR_PAG; n++) {
            int i = pagCafes * CAFES_POR_PAG + n;
            if (i >= cafesAbiertos.size()) break;
            CafeC c = cafesAbiertos.get(i);
            int y = CY + 8 + n * 30;
            caja(g, LX, y, 360, 26, FONDO, c.dentro() ? 0xFF6FE08A : BORDE);
            texto(g, recortar(c.titulo().isEmpty() ? c.nombre() : c.titulo() + " · " + c.nombre(), 34), LX + 8, y + 4, BLANCO, 0.95f);
            texto(g, "♥ " + c.megusta() + (c.nresenas() > 0 ? " · ★ " + String.format("%.1f", c.valoracion()) : "") + " · estilo " + c.estilo() + " · rep " + c.rep() + (c.abierto() ? " · ABIERTO" : c.dentro() ? " · en su café" : " · no está") + (c.gente() > 0 ? " · " + c.gente() + " de visita" : ""), LX + 8, y + 15, SUAVE, 0.7f);
            boton(g, LX + 360 - 92, y + 4, 86, 18, "IR A SU CAFÉ", 0xFF7A4AC0, true, dentro(mx, my, LX + 360 - 92, y + 4, 86, 18), 0.8f);
        }
        texto(g, "LOS MÁS QUERIDOS", 400, CY - 8, ORO, 1.0f);
        if (topCafes.isEmpty()) texto(g, "Aún nadie tiene «me gusta».", 400, CY + 8, GRIS, 0.8f);
        for (int i = 0; i < topCafes.size(); i++) {
            TopC t = topCafes.get(i);
            int y = CY + 8 + i * 22;
            caja(g, 400, y, 220, 19, i == 0 ? ACTIVO : FONDO, i == 0 ? ORO : BORDE);
            texto(g, (i + 1) + ". " + recortar(t.nombre(), 13), 406, y + 5, i == 0 ? ORO : BLANCO, 0.85f);
            texto(g, "♥ " + t.megusta() + " · e" + t.estilo() + " · r" + t.rep(), 520, y + 5, SUAVE, 0.7f);
        }
        texto(g, "«IR A SU CAFÉ» te lleva a su acera: puedes entrar a mirar.", 400, CY + 138, SUAVE, 0.7f);
        texto(g, "Para cocinar con alguien, te tiene que invitar.", 400, CY + 148, SUAVE, 0.7f);
        texto(g, "«Me gusta»: toca la caja registradora de su café.", 400, CY + 158, SUAVE, 0.7f);
    }

    private void clicCafes(int mx, int my) {
        int total = paginas(cafesAbiertos.size(), CAFES_POR_PAG);
        if (total > 1) { int d = clicPaginador(mx, my, LX + 288, CY - 12); if (d != 0) { pagCafes = Math.max(0, Math.min(total - 1, pagCafes + d)); return; } }
        for (int n = 0; n < CAFES_POR_PAG; n++) {
            int i = pagCafes * CAFES_POR_PAG + n;
            if (i >= cafesAbiertos.size()) break;
            CafeC c = cafesAbiertos.get(i);
            int y = CY + 8 + n * 30;
            if (dentro(mx, my, LX + 360 - 92, y + 4, 86, 18)) { enviar("visitar", c.nombre(), ""); return; }
        }
    }

    // ------------------------------------------------------------ personal: contratar Michialien y ponerles nombre

    /** Cierra el menu y abre el chat con el comando ya escrito, para que el jugador solo escriba el nombre. */
    private void pedirTexto(String comando) {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        mc.setScreen(new net.minecraft.client.gui.screen.ChatScreen(comando));
    }

    private static final int PERS_H = 38;

    private void personalTab(DrawContext g, int mx, int my) {
        int pags = Math.max(1, (personal.size() + 4) / 5);
        persPag = Math.max(0, Math.min(pags - 1, persPag));
        texto(g, "TU PERSONAL (" + personal.size() + "/" + personalMax + ")", LX, CY - 8, ORO, 1.1f);
        texto(g, "Trabajan cuando el café está abierto.", LX + 130, CY - 5, SUAVE, 0.75f);
        if (pags > 1) {
            boton(g, LX + 290, CY - 11, 20, 14, "◀", 0xFF4A4A6A, persPag > 0, persPag > 0 && dentro(mx, my, LX + 290, CY - 11, 20, 14), 0.8f);
            texto(g, (persPag + 1) + "/" + pags, LX + 314, CY - 8, BLANCO, 0.8f);
            boton(g, LX + 340, CY - 11, 20, 14, "▶", 0xFF4A4A6A, persPag < pags - 1, persPag < pags - 1 && dentro(mx, my, LX + 340, CY - 11, 20, 14), 0.8f);
        }
        if (personal.isEmpty()) parrafo(g, "Todavía no tienes a nadie. Contrata a un mesero, un lavaplatos o un chef a la derecha: pueden llevar el nombre que quieras.", LX, CY + 10, 340, GRIS, 0.9f);
        boolean puede = duenio && estado.equals("preparando") && !construyendo;
        for (int i = 0; i < 5 && persPag * 5 + i < personal.size(); i++) {
            PersonaC p = personal.get(persPag * 5 + i);
            int y = CY + 8 + i * (PERS_H + 4);
            caja(g, LX, y, 360, PERS_H, FONDO, BORDE);
            objeto(g, p.icono(), LX + 8, y + 9, 1.3f);
            texto(g, recortar(p.nombre(), 16), LX + 38, y + 4, ORO, 1.1f);
            texto(g, p.rol() + " · " + (p.especial().isEmpty() ? recortar(p.aspecto(), 18) : "★ " + recortar(p.especial(), 16)), LX + 38, y + 17, p.especial().isEmpty() ? GRIS : ORO, 0.78f);
            texto(g, estrellas((p.nivel() + 1) / 2, 10) + "  Nv " + p.nivel(), LX + 38, y + 27, ORO, 0.75f);
            texto(g, "☺" + p.felicidad(), LX + 132, y + 27, p.felicidad() < 30 ? ROJO : p.felicidad() < 60 ? GRIS : p.felicidad() < 85 ? VERDE : ORO, 0.7f);
            int bx = LX + 360 - 3 * 66;
            boton(g, bx, y + 3, 62, 15, "✎ NOMBRE", 0xFF7A4AC0, duenio, duenio && dentro(mx, my, bx, y + 3, 62, 15), 0.7f);
            if (construyendo) boton(g, bx + 66, y + 3, 62, 15, "SACAR PELUCHE", 0xFFB06A2E, duenio, duenio && dentro(mx, my, bx + 66, y + 3, 62, 15), 0.62f);
            else boton(g, bx + 66, y + 3, 62, 15, "ASPECTO ▶", 0xFF4A7AC0, puede, puede && dentro(mx, my, bx + 66, y + 3, 62, 15), 0.7f);
            boton(g, bx + 132, y + 3, 62, 15, "DESPEDIR", 0xFF8A3A3A, puede, puede && dentro(mx, my, bx + 132, y + 3, 62, 15), 0.7f);
            boolean sube = puede && p.tope().isEmpty() && monedas >= p.costo();
            String txt = !p.tope().isEmpty() ? p.tope() : "▲ MEJORAR a Nv " + (p.nivel() + 1) + " · " + p.costo() + " monedas";
            boton(g, bx, y + 21, 128, 15, txt.length() > 24 ? txt.replace(" monedas", "") : txt, 0xFF2E8B57, sube, sube && dentro(mx, my, bx, y + 21, 128, 15), 0.62f);
            boolean prem = puede && p.felicidad() < 100 && monedas >= p.premio();
            boton(g, bx + 132, y + 21, 62, 15, "🎁 " + p.premio(), 0xFFB06A2E, prem, prem && dentro(mx, my, bx + 132, y + 21, 62, 15), 0.66f);
        }
        if (!personal.isEmpty()) {
            texto(g, "☺ = humor (rapidez 0,90 a 1,12). 🎁 = premio (+25).", LX, CY + 8 + 5 * (PERS_H + 4) + 2, SUAVE, 0.62f);
            boton(g, LX + 232, CY + 8 + 5 * (PERS_H + 4) - 1, 128, 13, "UNIFORME PARA TODOS", 0xFF4A7AC0, puede, puede && dentro(mx, my, LX + 232, CY + 8 + 5 * (PERS_H + 4) - 1, 128, 13), 0.62f);
            boolean ord = duenio && estado.equals("preparando") && personal.size() > 1;
            boton(g, LX + 232, CY + 8 + 5 * (PERS_H + 4) + 13, 128, 13, "ORDENAR POR PUESTO", 0xFF7A4AC0, ord, ord && dentro(mx, my, LX + 232, CY + 8 + 5 * (PERS_H + 4) + 13, 128, 13), 0.62f);
            texto(g, "Uniforme +" + uniformePct + " %" + (bonoPaciencia > 0 ? " · paciencia +" + bonoPaciencia + " %" : "") + (bonoPropina > 0 ? " · propinas +" + bonoPropina + " %" : ""), LX, CY + 8 + 5 * (PERS_H + 4) + 12, 0xFF9AC8FF, 0.62f);
        }
        if (construyendo) { peluchesPanel(g, mx, my); return; }
        texto(g, "CONTRATAR", 400, CY - 8, ORO, 1.1f);
        int cpags = Math.max(1, (contratos.size() + 2) / 3);
        contratoPag = Math.max(0, Math.min(cpags - 1, contratoPag));
        if (cpags > 1) paginador(g, mx, my, 548, CY - 12, contratoPag, cpags);
        for (int k = 0; k < 3 && contratoPag * 3 + k < contratos.size(); k++) {
            ContratoC c = contratos.get(contratoPag * 3 + k);
            int y = CY + 8 + k * 66;
            boolean libre = c.bloqueo().isEmpty();
            caja(g, 400, y, 220, 62, FONDO, libre ? 0xFF6FE08A : BORDE);
            objeto(g, c.icono(), 408, y + 6, 1.4f);
            texto(g, c.nombre(), 436, y + 5, BLANCO, 1.05f);
            texto(g, c.precio() + " monedas · tienes " + c.tienes() + "/" + c.max(), 436, y + 17, MONEDA, 0.72f);
            parrafo(g, c.texto(), 406, y + 29, 208, SUAVE, 0.7f);
            boolean ok = puede && libre && monedas >= c.precio();
            String txt = !libre ? c.bloqueo() : "CONTRATAR";
            boton(g, 400 + 220 - 86, y + 5, 80, 16, txt, 0xFF2E8B57, ok, ok && dentro(mx, my, 400 + 220 - 86, y + 5, 80, 16), 0.72f);
        }
        StringBuilder lim = new StringBuilder("Hasta ");
        for (int k = 0; k < contratos.size(); k++) lim.append(k > 0 ? (k == contratos.size() - 1 ? " y " : ", ") : "").append(contratos.get(k).max()).append(' ').append(contratos.get(k).nombre().toLowerCase()).append(contratos.get(k).max() == 1 ? "" : "s");
        parrafo(g, lim + "; cada uno de más pide un local un nivel más grande.", 400, CY + 8 + 3 * 66 + 2, 232, SUAVE, 0.7f);
        parrafo(g, "Cada nivel de un empleado: anda un 12 % y trabaja un 15 % más rápido (del 11 al 20, un 4 % y un 5 %). Máximo nivel 20; no puede pasar del nivel de tu local.", 400, CY + 8 + 3 * 66 + 26, 232, SUAVE, 0.7f);
        if (!duenio) texto(g, "Solo el dueño contrata y despide.", LX, CY + 190, GRIS, 0.9f);
    }

    private static final String[][] PUESTOS = {{"chef", "Chef"}, {"mesero", "Mesero"}, {"lavaplatos", "Lavaplatos"}, {"anfitrion", "Anfitrión"}, {"cajero", "Cajero"}, {"mascota", "Mascota"}, {"granjero", "Michigranjero"}};

    private int cuantosPuesto(String nombre) { int n = 0; for (PersonaC p : personal) if (p.rol().equals(nombre)) n++; return n; }

    /** Con el modo construccion activo, el panel de la derecha sustituye a «contratar»: botones para sacar los peluches de los empleados (por puesto) o guardarlos. */
    private void peluchesPanel(DrawContext g, int mx, int my) {
        texto(g, "PELUCHES DEL PERSONAL", 400, CY - 8, ORO, 1.1f);
        parrafo(g, "Cada empleado es un peluche: sácalo, haz clic derecho donde quieres que descanse (agachado = sitio automático) y guárdalo para no llenar el inventario.", 400, CY + 6, 232, SUAVE, 0.7f);
        int y = CY + 48;
        boolean hay = !personal.isEmpty();
        boton(g, 400, y, 220, 16, "SACAR TODOS (ordenados por puesto)", 0xFFB06A2E, hay && duenio, hay && duenio && dentro(mx, my, 400, y, 220, 16), 0.7f);
        int k = 0;
        for (String[] pu : PUESTOS) {
            int n = cuantosPuesto(pu[1]);
            if (n == 0) continue;
            int x = 400 + (k % 2) * 112, yy = y + 22 + (k / 2) * 20;
            boton(g, x, yy, 108, 16, pu[1] + " (" + n + ")", 0xFF7A4AC0, duenio, duenio && dentro(mx, my, x, yy, 108, 16), 0.68f);
            k++;
        }
        int yb = y + 22 + ((k + 1) / 2) * 20 + 4;
        boton(g, 400, yb, 108, 16, "GUARDAR PELUCHES", 0xFF4A7AC0, duenio, duenio && dentro(mx, my, 400, yb, 108, 16), 0.68f);
        boolean ord = duenio && personal.size() > 1;
        boton(g, 512, yb, 108, 16, "ORDENAR POR PUESTO", 0xFF2E8B57, ord, ord && dentro(mx, my, 512, yb, 108, 16), 0.62f);
        parrafo(g, "«Ordenar» deja la lista por puestos (chefs, meseros, lavaplatos…); los peluches que llevas se guardan y los sacas otra vez.", 400, yb + 22, 232, SUAVE, 0.65f);
    }

    private void clicPeluchesPanel(int mx, int my) {
        int y = CY + 48;
        if (!personal.isEmpty() && duenio && dentro(mx, my, 400, y, 220, 16)) { enviar("peluches", "todos", ""); return; }
        int k = 0;
        for (String[] pu : PUESTOS) {
            if (cuantosPuesto(pu[1]) == 0) continue;
            int x = 400 + (k % 2) * 112, yy = y + 22 + (k / 2) * 20;
            if (duenio && dentro(mx, my, x, yy, 108, 16)) { enviar("peluches", pu[0], ""); return; }
            k++;
        }
        int yb = y + 22 + ((k + 1) / 2) * 20 + 4;
        if (duenio && dentro(mx, my, 400, yb, 108, 16)) { enviar("guardar_peluches", "", ""); return; }
        if (duenio && personal.size() > 1 && dentro(mx, my, 512, yb, 108, 16)) { enviar("ordenar_personal", "", ""); return; }
    }

    private void clicPersonal(int mx, int my) {
        boolean puede = duenio && estado.equals("preparando") && !construyendo;
        int pags = Math.max(1, (personal.size() + 4) / 5);
        if (pags > 1) {
            if (persPag > 0 && dentro(mx, my, LX + 290, CY - 11, 20, 14)) { persPag--; return; }
            if (persPag < pags - 1 && dentro(mx, my, LX + 340, CY - 11, 20, 14)) { persPag++; return; }
        }
        for (int i = 0; i < 5 && persPag * 5 + i < personal.size(); i++) {
            PersonaC p = personal.get(persPag * 5 + i);
            int y = CY + 8 + i * (PERS_H + 4), bx = LX + 360 - 3 * 66;
            if (duenio && dentro(mx, my, bx, y + 3, 62, 15)) { pedirTexto("/cafe personal nombre " + (p.idx() + 1) + " "); return; }
            if (construyendo && duenio && dentro(mx, my, bx + 66, y + 3, 62, 15)) { enviar("peluche", String.valueOf(p.idx()), ""); return; }
            if (puede && dentro(mx, my, bx + 66, y + 3, 62, 15)) { enviar("aspecto", String.valueOf(p.idx()), ""); return; }
            if (puede && dentro(mx, my, bx + 132, y + 3, 62, 15)) { enviar("despedir", String.valueOf(p.idx()), ""); return; }
            if (puede && p.tope().isEmpty() && dentro(mx, my, bx, y + 21, 128, 15)) { enviar("mejorar_emp", String.valueOf(p.idx()), ""); return; }
            if (puede && dentro(mx, my, bx + 132, y + 21, 62, 15)) { enviar("premio", String.valueOf(p.idx()), ""); return; }
        }
        if (!personal.isEmpty() && puede && dentro(mx, my, LX + 232, CY + 8 + 5 * (PERS_H + 4) - 1, 128, 13)) { enviar("uniforme", "", ""); return; }
        if (personal.size() > 1 && duenio && estado.equals("preparando") && dentro(mx, my, LX + 232, CY + 8 + 5 * (PERS_H + 4) + 13, 128, 13)) { enviar("ordenar_personal", "", ""); return; }
        if (construyendo) { clicPeluchesPanel(mx, my); return; }
        int cp = clicPaginador(mx, my, 548, CY - 12);
        if (cp != 0) { contratoPag = Math.max(0, Math.min(Math.max(1, (contratos.size() + 2) / 3) - 1, contratoPag + cp)); return; }
        for (int k = 0; k < 3 && contratoPag * 3 + k < contratos.size(); k++) {
            int y = CY + 8 + k * 66;
            if (puede && dentro(mx, my, 400 + 220 - 86, y + 5, 80, 16)) { enviar("contratar", contratos.get(contratoPag * 3 + k).rol(), ""); return; }
        }
    }

    // ------------------------------------------------------------ recetario

    private static final int REC_POR_PAG = 6, REC_W = 296, REC_H = 62, TIENDA_POR_PAG = 3, MEJ_POR_PAG = 7;

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

    private static final String[] MAESTRIA = {"Novato", "Aprendiz", "Experto", "Maestro"};
    private static final int[] MAESTRIA_PCT = {0, 3, 6, 10};

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
            else if (!p.tengo()) texto(g, "Sin aprender", x + 46, y + REC_H - 10, SUAVE, 0.65f);
            else texto(g, "★".repeat(p.mnivel()) + (p.mnivel() > 0 ? " " : "") + MAESTRIA[p.mnivel()] + " · " + p.servidos() + (p.msig() < 0 ? " servidos" : " / " + p.msig() + " servidos") + (p.mnivel() > 0 ? " · +" + MAESTRIA_PCT[p.mnivel()] + " %" : ""), x + 46, y + REC_H - 10, p.mnivel() >= 3 ? ORO : p.mnivel() > 0 ? 0xFF9AFFC0 : VERDE, 0.65f);
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
            boolean libre = p.bloqueo().isEmpty();
            parrafo(g, libre || p.tengo() ? p.receta() : p.bloqueo(), LX + 8, y + 34, 200, libre || p.tengo() ? SUAVE : 0xFFFF8A8A, 0.65f);
            texto(g, "Local " + p.nivelLocal(), LX + PW - 92, y + 6, p.nivelLocal() <= nivelLocal ? 0xFF8AE0A0 : 0xFFFF8A8A, 0.75f);
            int bx = LX + PW - 92, bw = 86;
            boolean puede = !p.tengo() && libre && puedeComprar() && monedas >= p.coste();
            boton(g, bx, y + 36, bw, 20, p.tengo() ? "APRENDIDO ✔" : !libre ? "BLOQUEADO" : p.coste() + " ●", 0xFF7A4AC0, puede, puede && dentro(mx, my, bx, y + 36, bw, 20), 0.9f);
        }
        texto(g, "MEJORAS DEL LOCAL", LX + 308, CY + 8, 0xFFC9B3FF, 0.95f);
        int totalMej = paginas(mejoras.size(), MEJ_POR_PAG);
        pagMejoras = Math.min(pagMejoras, totalMej - 1);
        if (totalMej > 1) paginador(g, mx, my, LX + 308 + PW - 72, CY + 5, pagMejoras, totalMej);
        for (int n = 0; n < MEJ_POR_PAG; n++) {
            int i = pagMejoras * MEJ_POR_PAG + n;
            if (i >= mejoras.size()) break;
            MejoraC m = mejoras.get(i);
            int y = CY + 20 + n * 28, x = LX + 308;
            boolean max = m.nivel() >= m.max();
            caja(g, x, y, PW, 26, FONDO, max ? 0xFF6FE08A : BORDE);
            objeto(g, m.item(), x + 6, y + 5, 1f);
            texto(g, m.nombre(), x + 28, y + 1, BLANCO, 0.85f);
            texto(g, recortar(m.texto(), 56), x + 28, y + 10, GRIS, 0.55f);
            for (int k = 0; k < m.max(); k++) g.fill(x + 28 + k * 14, y + 19, x + 38 + k * 14, y + 22, k < m.nivel() ? ORO : 0xFF2A2A44);
            int bx = x + PW - 88, bw = 82;
            boolean puede = !max && puedeComprar() && monedas >= m.precio();
            boton(g, bx, y + 3, bw, 20, max ? "MÁXIMO" : m.precio() + " ●", 0xFF7A4AC0, puede, puede && dentro(mx, my, bx, y + 3, bw, 20), 0.9f);
        }
    }

    // ------------------------------------------------------------ decorar (tienda del modo construccion)

    private static final int DW = 196, DH = 46, DCOLS = 3, DFILAS = 4, DPOR_PAG = DCOLS * DFILAS, DY = CY + 28;

    /** El nombre que se ve de un objeto: el que manda el servidor o, si viene vacio, el del propio objeto (en el idioma del jugador). */
    private String nombreDe(String nombre, String item) {
        if (!nombre.isEmpty()) return nombre;
        ItemStack st = pila(item);
        return st.isEmpty() ? item : st.getName().getString();
    }

    /** Un selector «◀ nombre ▶» de ancho {@code w}. */
    private void selector(DrawContext g, int mx, int my, int x, int y, int w, String texto) {
        boton(g, x, y, 18, 16, "◀", 0xFF7A4AC0, true, dentro(mx, my, x, y, 18, 16), 0.9f);
        caja(g, x + 20, y, w - 40, 16, FONDO, BORDE);
        textoCentrado(g, recortar(texto, (w - 40) / 5), x + w / 2, y + 4, BLANCO, 0.75f);
        boton(g, x + w - 18, y, 18, 16, "▶", 0xFF7A4AC0, true, dentro(mx, my, x + w - 18, y, 18, 16), 0.9f);
    }

    /** -1 / +1 si se pulso una flecha del selector, 0 si no. */
    private int clicSelector(int mx, int my, int x, int y, int w) {
        if (dentro(mx, my, x, y, 18, 16)) return -1;
        if (dentro(mx, my, x + w - 18, y, 18, 16)) return 1;
        return 0;
    }

    private static final int SEL_F = LX, SEL_FW = 196, SEL_G = LX + 202, SEL_GW = 216, BUSCAR_X = LX + 424, BUSCAR_W = 82, FAV_X = LX + 512, FAV_W = 88, PAG_DX = LX + 500;

    private void decorar(DrawContext g, int mx, int my) {
        texto(g, "DECORAR", LX, CY - 8, ORO, 1.2f);
        texto(g, "Tus monedas: " + monedas, LX + 70, CY - 5, MONEDA, 0.9f);
        if (!construyendo && !libre) {
            parrafo(g, duenio ? "Aquí compras bloques y decoración para tu café, pero solo en el MODO CONSTRUCCIÓN: ciérralo si está abierto y pulsa el botón de la pestaña CAFÉ."
                    : "Solo el dueño decora el café.", LX, CY + 14, 400, GRIS, 0.9f);
            if (duenio && estado.equals("preparando")) boton(g, LX, CY + 60, 250, 30, "ENTRAR EN MODO CONSTRUCCIÓN", 0xFF7A4AC0, true, dentro(mx, my, LX, CY + 60, 250, 30), 1f);
            return;
        }
        if (!duenio) { parrafo(g, "Solo el dueño compra para construir.", LX, CY + 14, 400, GRIS, 0.9f); return; }
        texto(g, "Mesas: " + mesasUsadas + "/" + (mesasMax >= 9999 ? "∞" : mesasMax), LX + 170, CY - 5, SUAVE, 0.8f);
        boton(g, PAG_DX - 22, CY - 12, 18, 14, "«", 0xFF7A4AC0, decoPag > 0, decoPag > 0 && dentro(mx, my, PAG_DX - 22, CY - 12, 18, 14), 0.9f);
        paginador(g, mx, my, PAG_DX, CY - 12, decoPag, decoPags);
        boton(g, PAG_DX + 76, CY - 12, 18, 14, "»", 0xFF7A4AC0, decoPag < decoPags - 1, decoPag < decoPags - 1 && dentro(mx, my, PAG_DX + 76, CY - 12, 18, 14), 0.9f);
        if (decoBuscar.isEmpty()) {
            selector(g, mx, my, SEL_F, CY + 6, SEL_FW, decoFuente < decoFuentes.size() ? decoFuentes.get(decoFuente) : "");
            selector(g, mx, my, SEL_G, CY + 6, SEL_GW, decoGrupo < decoGrupos.size() ? decoGrupos.get(decoGrupo) : "");
            boton(g, BUSCAR_X, CY + 6, BUSCAR_W, 16, "BUSCAR…", 0xFF3A7A5A, true, dentro(mx, my, BUSCAR_X, CY + 6, BUSCAR_W, 16), 0.85f);
            boton(g, FAV_X, CY + 6, FAV_W, 16, decoFav ? "★ VOLVER" : "★ FAVORITOS (" + nFav + ")", decoFav ? 0xFF8A3A3A : 0xFFB08A20, true, dentro(mx, my, FAV_X, CY + 6, FAV_W, 16), 0.72f);
        } else {
            texto(g, "Búsqueda: «" + recortar(decoBuscar, 40) + "» · " + decoTotal + " resultados" + (decoTotal >= 600 ? " (los primeros)" : ""), LX, CY + 10, BLANCO, 0.85f);
            boton(g, BUSCAR_X, CY + 6, BUSCAR_W + 2, 16, "BUSCAR OTRA…", 0xFF3A7A5A, true, dentro(mx, my, BUSCAR_X, CY + 6, BUSCAR_W + 2, 16), 0.8f);
            boton(g, FAV_X, CY + 6, FAV_W, 16, "VOLVER", 0xFF8A3A3A, true, dentro(mx, my, FAV_X, CY + 6, FAV_W, 16), 0.85f);
        }
        if (deco.isEmpty()) parrafo(g, decoFav && decoBuscar.isEmpty() ? "Aún no tienes favoritos: pulsa la estrella ☆ de cualquier artículo para guardarlo aquí." : decoBuscar.isEmpty() ? "No hay nada en este grupo." : "No hay nada con ese nombre. Prueba con el nombre en inglés (por ejemplo «chair», «lamp», «sofa», «oak table»).", LX, DY + 10, 400, GRIS, 0.9f);
        for (int n = 0; n < deco.size() && n < DPOR_PAG; n++) {
            DecoC a = deco.get(n);
            int x = LX + (n % DCOLS) * (DW + 6), y = DY + (n / DCOLS) * (DH + 3);
            caja(g, x, y, DW, DH, FONDO, BORDE);
            objeto(g, a.item(), x + 6, y + 7, 1.5f);
            texto(g, recortar(nombreDe(a.nombre(), a.item()), 26), x + 32, y + 5, BLANCO, 0.8f);
            texto(g, (decoBuscar.isEmpty() ? "" : recortar(a.grupo(), 14) + " · ") + "x" + a.cantidad() + (a.tienes() > 0 ? " · tienes " + a.tienes() : ""), x + 32, y + 16, SUAVE, 0.68f);
            boolean puede = monedas >= a.precio() && estado.equals("preparando");
            boton(g, x + DW - 76, y + DH - 19, 70, 15, a.precio() + " ●", 0xFF7A4AC0, puede, puede && dentro(mx, my, x + DW - 76, y + DH - 19, 70, 15), 0.85f);
            boolean sobreEstrella = dentro(mx, my, x + DW - 18, y + 3, 14, 14);
            texto(g, a.fav() ? "★" : "☆", x + DW - 17, y + 4, a.fav() ? MONEDA : sobreEstrella ? BLANCO : SUAVE, 1.1f);
            if (dentro(mx, my, x, y, DW, DH) && !dentro(mx, my, x + DW - 76, y + DH - 19, 70, 15) && !sobreEstrella) ponerTip(a.item(), nombreDe(a.nombre(), a.item()), a.grupo() + " · " + a.item());
        }
    }

    private void clicDecorar(int mx, int my) {
        if (!construyendo && !libre) {
            if (duenio && estado.equals("preparando") && dentro(mx, my, LX, CY + 60, 250, 30)) enviar("construir", "", "");
            return;
        }
        if (!duenio) return;
        if (dentro(mx, my, PAG_DX - 22, CY - 12, 18, 14)) { enviar("decoMover", "pag", "-10"); return; }
        if (dentro(mx, my, PAG_DX + 76, CY - 12, 18, 14)) { enviar("decoMover", "pag", "10"); return; }
        int d = clicPaginador(mx, my, PAG_DX, CY - 12);
        if (d != 0) { enviar("decoMover", "pag", String.valueOf(d)); return; }
        if (decoBuscar.isEmpty()) {
            int f = clicSelector(mx, my, SEL_F, CY + 6, SEL_FW);
            if (f != 0) { enviar("decoMover", "fuente", String.valueOf(f)); return; }
            int gr = clicSelector(mx, my, SEL_G, CY + 6, SEL_GW);
            if (gr != 0) { enviar("decoMover", "grupo", String.valueOf(gr)); return; }
            if (dentro(mx, my, BUSCAR_X, CY + 6, BUSCAR_W, 16)) { pedirTexto("/cafe buscar "); return; }
            if (dentro(mx, my, FAV_X, CY + 6, FAV_W, 16)) { enviar("decoMover", "fav", "0"); return; }
        } else {
            if (dentro(mx, my, BUSCAR_X, CY + 6, BUSCAR_W + 2, 16)) { pedirTexto("/cafe buscar "); return; }
            if (dentro(mx, my, FAV_X, CY + 6, FAV_W, 16)) { enviar("decoBuscar", "", ""); return; }
        }
        for (int n = 0; n < deco.size() && n < DPOR_PAG; n++) {
            DecoC a = deco.get(n);
            int x = LX + (n % DCOLS) * (DW + 6), y = DY + (n / DCOLS) * (DH + 3);
            if (dentro(mx, my, x + DW - 18, y + 3, 14, 14)) { enviar("decoFav", a.item(), ""); return; }
            if (monedas >= a.precio() && estado.equals("preparando") && dentro(mx, my, x + DW - 76, y + DH - 19, 70, 15)) enviar("comprarDeco", a.id(), "");
        }
    }

    // ------------------------------------------------------------ el baul (todo lo que tienes guardado para construir, sin limite)

    // posiciones de la fila de filtros del baul: [SACAR TODO] [GUARDAR TODO] [fuente] [categoria] [BUSCAR]
    private static final int BSEL_W = 112, BBUSCAR_W = 76;
    private int baulSelX() { return construyendo ? LX + 182 : LX; }
    private int baulGrupoX() { return baulSelX() + BSEL_W + 4; }
    private int baulBuscarX() { return baulGrupoX() + BSEL_W + 4; }

    private void baulTab(DrawContext g, int mx, int my) {
        texto(g, "BAÚL", LX, CY - 8, ORO, 1.2f);
        texto(g, construyendo ? "Todo lo que compras para decorar se guarda aquí, sin límite, aunque no te quepa en el inventario."
                : "Para sacar o guardar cosas entra en el MODO CONSTRUCCIÓN (pestaña CAFÉ).", LX + 60, CY - 5, GRIS, 0.75f);
        if (!duenio) { parrafo(g, "Solo el dueño tiene baúl.", LX, CY + 14, 400, GRIS, 0.9f); return; }
        int total = paginas(baul.size(), DPOR_PAG);
        pagBaul = Math.max(0, Math.min(pagBaul, total - 1));
        paginador(g, mx, my, LX + 528, CY + 4, pagBaul, total);
        if (construyendo) {
            boton(g, LX, CY + 6, 78, 16, "SACAR TODO", 0xFF3A7A5A, true, dentro(mx, my, LX, CY + 6, 78, 16), 0.8f);
            boton(g, LX + 82, CY + 6, 96, 16, "GUARDAR TODO", 0xFF7A4AC0, true, dentro(mx, my, LX + 82, CY + 6, 96, 16), 0.8f);
        }
        selector(g, mx, my, baulSelX(), CY + 6, BSEL_W, baulFuente < baulFuentes.size() ? baulFuentes.get(baulFuente) : "Todo");
        selector(g, mx, my, baulGrupoX(), CY + 6, BSEL_W, baulGrupo < baulGrupos.size() ? baulGrupos.get(baulGrupo) : "Todas");
        boolean filtrando = !baulBuscar.isEmpty();
        boton(g, baulBuscarX(), CY + 6, BBUSCAR_W, 16, filtrando ? "✖ «" + recortar(baulBuscar, 6) + "»" : "BUSCAR…", filtrando ? 0xFF8A3A3A : 0xFF3A7A5A, true, dentro(mx, my, baulBuscarX(), CY + 6, BBUSCAR_W, 16), 0.75f);
        if (baul.isEmpty()) { parrafo(g, baulTotal > 0 ? "Nada coincide con el filtro." : "Tu baúl está vacío. Compra bloques y muebles en la pestaña DECORAR: se guardan aquí.", LX, DY + 10, 400, GRIS, 0.9f); return; }
        for (int n = 0; n < DPOR_PAG; n++) {
            int i = pagBaul * DPOR_PAG + n;
            if (i >= baul.size()) break;
            BaulC b = baul.get(i);
            int x = LX + (n % DCOLS) * (DW + 6), y = DY + (n / DCOLS) * (DH + 3);
            caja(g, x, y, DW, DH, FONDO, b.fuera() ? 0xFF8A6A3A : BORDE);
            objeto(g, b.item(), x + 6, y + 7, 1.5f);
            texto(g, recortar(nombreDe("", b.item()), 26), x + 32, y + 5, BLANCO, 0.8f);
            texto(g, "x" + b.n() + (construyendo ? (b.mano() > 0 ? " · llevas " + b.mano() : b.fuera() ? " · solo en el baúl" : " · sin sitio") : ""), x + 32, y + 16, SUAVE, 0.68f);
            boolean guardar = b.mano() > 0;
            boton(g, x + DW - 76, y + DH - 19, 70, 15, guardar ? "GUARDAR" : "SACAR", guardar ? 0xFF7A4AC0 : 0xFF3A7A5A, construyendo, construyendo && dentro(mx, my, x + DW - 76, y + DH - 19, 70, 15), 0.8f);
            if (dentro(mx, my, x, y, DW, DH) && !dentro(mx, my, x + DW - 76, y + DH - 19, 70, 15)) ponerTip(b.item(), nombreDe("", b.item()), "x" + b.n() + " · " + b.item());
        }
    }

    private void clicBaul(int mx, int my) {
        if (!duenio) return;
        int total = paginas(baul.size(), DPOR_PAG);
        int d = clicPaginador(mx, my, LX + 528, CY + 4);
        if (d != 0) { pagBaul = Math.max(0, Math.min(total - 1, pagBaul + d)); return; }
        int f = clicSelector(mx, my, baulSelX(), CY + 6, BSEL_W);
        if (f != 0) { pagBaul = 0; enviar("baulFiltro", String.valueOf(f), ""); return; }
        int gg = clicSelector(mx, my, baulGrupoX(), CY + 6, BSEL_W);
        if (gg != 0) { pagBaul = 0; enviar("baulGrupo", String.valueOf(gg), ""); return; }
        if (dentro(mx, my, baulBuscarX(), CY + 6, BBUSCAR_W, 16)) {
            if (baulBuscar.isEmpty()) pedirTexto("/cafe baul "); else { pagBaul = 0; enviar("baulBuscar", "", ""); }
            return;
        }
        if (!construyendo) return;
        if (dentro(mx, my, LX, CY + 6, 78, 16)) { enviar("baulSacarTodo", "", ""); return; }
        if (dentro(mx, my, LX + 82, CY + 6, 96, 16)) { enviar("baulGuardarTodo", "", ""); return; }
        for (int n = 0; n < DPOR_PAG; n++) {
            int i = pagBaul * DPOR_PAG + n;
            if (i >= baul.size()) break;
            BaulC b = baul.get(i);
            int x = LX + (n % DCOLS) * (DW + 6), y = DY + (n / DCOLS) * (DH + 3);
            if (dentro(mx, my, x + DW - 76, y + DH - 19, 70, 15)) enviar(b.mano() > 0 ? "baulGuardar" : "baulSacar", b.item(), "");
        }
    }

    // ------------------------------------------------------------ planos (diseños guardados del café)

    private static final int PL_H = 22;

    private void planosTab(DrawContext g, int mx, int my) {
        texto(g, "PLANOS", LX, CY - 8, ORO, 1.2f);
        texto(g, "Guarda tu diseño y recupéralo cuando quieras.", LX + 70, CY - 5, GRIS, 0.75f);
        if (!duenio) { parrafo(g, "Solo el dueño tiene planos.", LX, CY + 14, 400, GRIS, 0.9f); return; }
        if (!libre) { parrafo(g, "Los planos solo funcionan en los cafés de parcela libre (los nuevos).", LX, CY + 14, 400, GRIS, 0.9f); return; }
        boton(g, LX + 372, CY - 12, 100, 14, "MIS PLANOS", planosSub == 0 ? 0xFF7A4AC0 : 0xFF4A4A6A, true, dentro(mx, my, LX + 372, CY - 12, 100, 14), 0.75f);
        boton(g, LX + 476, CY - 12, 124, 14, "CAFÉS TEMÁTICOS", planosSub == 1 ? 0xFF7A4AC0 : 0xFF4A4A6A, true, dentro(mx, my, LX + 476, CY - 12, 124, 14), 0.75f);
        if (planosSub == 1) { temasTab(g, mx, my); return; }
        long manuales = planos.stream().filter(pl -> !pl.auto()).count();
        boolean cerrado = estado.equals("preparando");
        boton(g, LX, CY + 6, 170, 16, "GUARDAR PLANO NUEVO… (" + manuales + "/" + planosMax + ")", 0xFF3A7A5A, manuales < planosMax, manuales < planosMax && dentro(mx, my, LX, CY + 6, 170, 16), 0.78f);
        texto(g, cerrado ? "Restaurar cambia lo construido y deja una copia automática de lo de antes." : "Para restaurar un plano cierra el café.", LX + 178, CY + 10, SUAVE, 0.7f);
        if (planos.isEmpty()) { parrafo(g, "Todavía no hay ningún plano. Al terminar de construir se guarda sola una copia automática (las 3 últimas).", LX, CY + 40, 450, GRIS, 0.9f); return; }
        for (int i = 0; i < planos.size() && i < 9; i++) {
            PlanoC pl = planos.get(i);
            int y = CY + 28 + i * (PL_H + 2);
            caja(g, LX, y, 600, PL_H, FONDO, pl.auto() ? BORDE : 0xFF6A8A5A);
            texto(g, pl.auto() ? "⟲ " + pl.nombre() : "▣ " + pl.nombre(), LX + 8, y + 7, BLANCO, 0.9f);
            texto(g, pl.fecha() + " · " + pl.bloques() + " bloques", LX + 130, y + 8, SUAVE, 0.65f);
            boton(g, LX + 262, y + 3, 78, 16, "RESTAURAR", 0xFF3A7A5A, cerrado, cerrado && dentro(mx, my, LX + 262, y + 3, 78, 16), 0.8f);
            if (!pl.auto()) boton(g, LX + 344, y + 3, 92, 16, "SOBRESCRIBIR", 0xFF7A4AC0, true, dentro(mx, my, LX + 344, y + 3, 92, 16), 0.72f);
            if (!pl.auto()) boton(g, LX + 440, y + 3, 76, 16, "COMPARTIR", 0xFF3A6AA0, true, dentro(mx, my, LX + 440, y + 3, 76, 16), 0.75f);
            boolean conf = confirmaBorrar.equals(pl.clave());
            boton(g, LX + 520, y + 3, 74, 16, conf ? "¿SEGURO?" : "BORRAR", 0xFF8A3A3A, true, dentro(mx, my, LX + 520, y + 3, 74, 16), 0.75f);
        }
    }

    private static final int TEMA_H = 46;

    private void temasTab(DrawContext g, int mx, int my) {
        boolean cerrado = estado.equals("preparando");
        parrafo(g, "Cambia todo tu café por un edificio ya construido y decorado. Tus estaciones y mesas se colocan solas, los bloques que falten van incluidos en el precio y lo de antes queda en una copia automática (MIS PLANOS).", LX, CY + 6, 590, SUAVE, 0.72f);
        if (!cerrado) texto(g, "Cierra el café para comprar uno.", LX, CY + 30, 0xFFE0A050, 0.75f);
        for (int i = 0; i < temas.size() && i < 4; i++) {
            TemaC t = temas.get(i);
            int y = CY + 42 + i * (TEMA_H + 3);
            caja(g, LX, y, 600, TEMA_H, FONDO, 0xFF6A8A5A);
            texto(g, t.nombre(), LX + 10, y + 7, ORO, 1.0f);
            parrafo(g, t.desc(), LX + 10, y + 21, 390, GRIS, 0.72f);
            boolean puede = cerrado && monedas >= t.precio();
            boolean conf = confirmaTema.equals(t.id());
            boton(g, LX + 440, y + 8, 152, 28, conf ? "¿SEGURO? " + t.precio() + " monedas" : "COMPRAR · " + t.precio(), conf ? 0xFFB0602A : 0xFF3A7A5A, puede, puede && dentro(mx, my, LX + 440, y + 8, 152, 28), 0.82f);
        }
    }

    private void clicTemas(int mx, int my) {
        boolean cerrado = estado.equals("preparando");
        for (int i = 0; i < temas.size() && i < 4; i++) {
            TemaC t = temas.get(i);
            int y = CY + 42 + i * (TEMA_H + 3);
            if (cerrado && monedas >= t.precio() && dentro(mx, my, LX + 440, y + 8, 152, 28)) {
                if (confirmaTema.equals(t.id())) { confirmaTema = ""; enviar("temaComprar", t.id(), ""); }
                else confirmaTema = t.id();
                return;
            }
        }
        confirmaTema = "";
    }

    private void clicPlanos(int mx, int my) {
        if (!duenio || !libre) return;
        if (dentro(mx, my, LX + 372, CY - 12, 100, 14)) { planosSub = 0; confirmaTema = ""; return; }
        if (dentro(mx, my, LX + 476, CY - 12, 124, 14)) { planosSub = 1; confirmaBorrar = ""; return; }
        if (planosSub == 1) { clicTemas(mx, my); return; }
        long manuales = planos.stream().filter(pl -> !pl.auto()).count();
        if (manuales < planosMax && dentro(mx, my, LX, CY + 6, 170, 16)) { pedirTexto("/cafe plano guardar "); return; }
        for (int i = 0; i < planos.size() && i < 9; i++) {
            PlanoC pl = planos.get(i);
            int y = CY + 28 + i * (PL_H + 2);
            if (estado.equals("preparando") && dentro(mx, my, LX + 262, y + 3, 78, 16)) { confirmaBorrar = ""; enviar("planoRestaurar", pl.clave(), ""); return; }
            if (!pl.auto() && dentro(mx, my, LX + 344, y + 3, 92, 16)) { confirmaBorrar = ""; enviar("planoSobrescribir", pl.clave(), ""); return; }
            if (!pl.auto() && dentro(mx, my, LX + 440, y + 3, 76, 16)) { confirmaBorrar = ""; enviar("planoElegir", pl.clave(), ""); pedirTexto("/cafe plano compartir "); return; }
            if (dentro(mx, my, LX + 520, y + 3, 74, 16)) {
                if (confirmaBorrar.equals(pl.clave())) { confirmaBorrar = ""; enviar("planoBorrar", pl.clave(), ""); }
                else confirmaBorrar = pl.clave();
                return;
            }
        }
        confirmaBorrar = "";
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
        texto(g, "Lo que compras llega a tu correo (/correo). Las estrellas se ganan sirviendo, con misiones y con logros.", LX, 326, SUAVE, 0.75f);
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

    private static final int[] CL_COLOR = {0xFFB8B8CC, 0xFFE59BFF, 0xFFFFE070, 0xFFFFB02E};
    private static final String[] CL_NOMBRE = {"Nuevo", "Habitual", "Fiel", "VIP"};
    private static final String[] CL_ICONO = {"", "♥ ", "★ ", "♛ "};

    private void clientelaTab(DrawContext g, int mx, int my) {
        int hab = 0, fiel = 0, vip = 0;
        for (ClienteC c : clientela) { if (c.nivel() == 1) hab++; else if (c.nivel() == 2) fiel++; else if (c.nivel() >= 3) vip++; }
        texto(g, "CLIENTELA", LX, CY - 8, ORO, 1.1f);
        texto(g, "Habitual a las 4 visitas, Fiel a las 12 y VIP a las 25: vienen más, aguantan más y pagan +6 / +12 / +22 %.", LX + 66, CY - 6, SUAVE, 0.62f);
        texto(g, "Habituales: " + hab + "   Fieles: " + fiel + "   VIP: " + vip, LX, CY + 6, GRIS, 0.8f);
        int pags = paginas(clientela.size(), 12);
        pagClientela = Math.max(0, Math.min(pags - 1, pagClientela));
        if (pags > 1) paginador(g, mx, my, LX + 528, CY + 2, pagClientela, pags);
        for (int k = 0; k < 12 && pagClientela * 12 + k < clientela.size(); k++) {
            ClienteC c = clientela.get(pagClientela * 12 + k);
            int x = LX + (k % 2) * 308, y = CY + 20 + (k / 2) * 38;
            caja(g, x, y, 296, 34, FONDO, c.nivel() >= 3 ? ORO : c.nivel() > 0 ? CL_COLOR[c.nivel()] : BORDE);
            texto(g, c.nombre(), x + 8, y + 4, c.nivel() > 0 ? CL_COLOR[c.nivel()] : BLANCO, 0.95f);
            texto(g, CL_ICONO[c.nivel()] + CL_NOMBRE[c.nivel()] + (c.pago() > 0 ? " · +" + c.pago() + " % al pagar" : ""), x + 110, y + 5, c.nivel() > 0 ? CL_COLOR[c.nivel()] : SUAVE, 0.7f);
            int meta = c.sig() < 0 ? Math.max(1, c.visitas()) : c.sig();
            barra(g, x + 8, y + 19, 190, c.sig() < 0 ? 1 : c.visitas(), c.sig() < 0 ? 1 : meta, c.nivel() >= 3 ? ORO : 0xFF6CC8FF);
            texto(g, c.sig() < 0 ? c.visitas() + " visitas (máximo)" : c.visitas() + " / " + c.sig() + " visitas", x + 206, y + 18, GRIS, 0.75f);
        }
        if (clientela.isEmpty()) parrafo(g, "Todavía no has servido a nadie. Cada cliente servido cuenta para su especie.", LX, CY + 24, 340, GRIS, 0.9f);
    }

    private static String estrellas(int n) { return "★".repeat(Math.max(0, Math.min(5, n))) + "☆".repeat(5 - Math.max(0, Math.min(5, n))); }

    private void resenasTab(DrawContext g, int mx, int my) {
        texto(g, "RESEÑAS", LX, CY - 8, ORO, 1.1f);
        texto(g, "Cada cliente que se va deja de 1 a 5 estrellas: rápido y con su favorito, más; enfadado, 1.", LX + 62, CY - 6, SUAVE, 0.62f);
        int pags = paginas(resenas.size(), 7);
        pagResenas = Math.max(0, Math.min(pags - 1, pagResenas));
        if (pags > 1) paginador(g, mx, my, LX + 528, CY + 2, pagResenas, pags);
        caja(g, LX, CY + 8, 150, 120, FONDO, resN >= Resenas_MIN && resMedia >= 4.5 ? ORO : BORDE);
        if (resN == 0) { parrafo(g, "Todavía no hay reseñas. Sirve clientes y aparecerán aquí.", LX + 8, CY + 18, 134, GRIS, 0.8f); }
        else {
            texto(g, String.format("%.1f", resMedia), LX + 10, CY + 14, resMedia >= 4 ? ORO : resMedia >= 3 ? 0xFFE0E0E0 : 0xFFFF8080, 2.0f);
            texto(g, estrellas((int) Math.round(resMedia)), LX + 62, CY + 22, ORO, 0.9f);
            texto(g, resN + " recientes · " + resTotal + " en total", LX + 10, CY + 40, GRIS, 0.7f);
            for (int k = 4; k >= 0; k--) {
                int yy = CY + 54 + (4 - k) * 11;
                texto(g, (k + 1) + "★", LX + 10, yy, GRIS, 0.7f);
                barra(g, LX + 30, yy + 1, 100, resDist[k], Math.max(1, resN), k >= 3 ? ORO : k == 2 ? 0xFFB0B0C0 : 0xFFE06060);
            }
        }
        parrafo(g, resEfecto, LX, CY + 134, 150, SUAVE, 0.62f);
        if (resProveedor > 0) texto(g, "Proveedor: −" + resProveedor + " monedas", LX, CY + 168, 0xFFFFD27F, 0.62f);
        for (int k = 0; k < 7 && pagResenas * 7 + k < resenas.size(); k++) {
            ResenaC r = resenas.get(pagResenas * 7 + k);
            int x = LX + 160, y = CY + 8 + k * 24;
            caja(g, x, y, 380, 22, FONDO, r.est() >= 5 ? ORO : r.est() <= 2 ? 0xFF8A3A3A : BORDE);
            texto(g, estrellas(r.est()), x + 6, y + 3, r.est() >= 4 ? ORO : r.est() == 3 ? 0xFFB0B0C0 : 0xFFFF8080, 0.8f);
            texto(g, (r.critico() ? "📰 CRÍTICO · " : "") + r.esp(), x + 60, y + 3, r.critico() ? ORO : BLANCO, 0.75f);
            texto(g, recortar("«" + r.texto() + "»", 62), x + 60, y + 12, GRIS, 0.65f);
        }
    }

    private static final int Resenas_MIN = 10;

    private void logrosTab(DrawContext g) {
        texto(g, "LOGROS", LX, CY - 8, ORO, 1.1f);
        texto(g, "Escalones que se cobran solos en estrellas al alcanzarlos.", LX + 70, CY - 6, GRIS, 0.8f);
        for (int i = 0; i < logros.size(); i++) {
            LogroC l = logros.get(i);
            int x = LX + (i % 2) * 308, y = CY + 12 + (i / 2) * 48;
            boolean fin = l.nivel() >= l.max();
            caja(g, x, y, 296, 44, FONDO, fin ? VERDE : BORDE);
            texto(g, l.nombre(), x + 8, y + 4, fin ? VERDE : BLANCO, 0.95f);
            for (int k = 0; k < l.max(); k++) g.fill(x + 150 + k * 14, y + 6, x + 160 + k * 14, y + 12, k < l.nivel() ? ORO : 0xFF2A2A44);
            texto(g, fin ? "¡Completo!" : "+" + l.premio() + " ★", x + 240, y + 4, fin ? VERDE : ESTRELLA, 0.85f);
            barra(g, x + 8, y + 17, 200, l.valor(), l.meta(), fin ? VERDE : 0xFF6CC8FF);
            texto(g, Math.min(l.valor(), l.meta()) + " / " + l.meta(), x + 216, y + 16, GRIS, 0.85f);
            texto(g, l.texto(), x + 8, y + 31, SUAVE, 0.72f);
        }
    }


    // ------------------------------------------------------------ granja (0.24.0): huertos, ganado y despensa

    private static final int FH = 46;                                   // alto de cada huerto o corral en la lista

    private List<FincaC> fincasDe(String kind) { List<FincaC> l = new ArrayList<>(); for (FincaC f : fincas) if (f.kind().equals(kind)) l.add(f); return l; }

    private static String tiempo(long seg) { return (seg / 60) + ":" + (seg % 60 < 10 ? "0" : "") + (seg % 60); }

    private void granjaTab(DrawContext g, int mx, int my, String kind) {
        boolean huerto = kind.equals("huerto");
        List<FincaC> mias = fincasDe(kind);
        texto(g, huerto ? "TUS HUERTOS" : "TU GANADO", LX, CY - 8, ORO, 1.1f);
        texto(g, "Huertos y corrales: " + granjaTienes + "/" + granjaMax + (granjaMax < 16 ? " (más con cada nivel de local)" : ""), LX + 100, CY - 6, SUAVE, 0.75f);
        if (nivelLocal < granjaMinLocal) {
            parrafo(g, "La granja se desbloquea con un local de nivel " + granjaMinLocal + " (ahora tienes el " + nivelLocal + "). Cada huerto o corral viene con su michigranjero, que cosecha solo y lleva todo a la despensa.", LX, CY + 14, 340, GRIS, 0.9f);
        } else if (mias.isEmpty()) {
            parrafo(g, huerto ? "Todavía no tienes huertos. Compra uno a la derecha: viene con su michigranjero, que cosecha cuando está listo y deja los ingredientes en la despensa."
                    : "Todavía no tienes corrales. Compra uno a la derecha: viene con su michigranjero y sus Pokémon, y da leche, huevos, carne o miel.", LX, CY + 14, 340, GRIS, 0.9f);
        }
        boolean puede = duenio && !construyendo;
        int pf = paginas(mias.size(), 5);
        pagFincas = Math.max(0, Math.min(pf - 1, pagFincas));
        if (pf > 1) paginador(g, mx, my, 295, CY - 12, pagFincas, pf);
        for (int i = 0; i < 5 && pagFincas * 5 + i < mias.size(); i++) {
            FincaC f = mias.get(pagFincas * 5 + i);
            int y = CY + 8 + i * (FH + 3);
            caja(g, LX, y, 360, FH, FONDO, f.prog() >= 100 ? VERDE : BORDE);
            objeto(g, f.icono(), LX + 6, y + 6, 1.4f);
            texto(g, f.nombre(), LX + 34, y + 3, ORO, 0.95f);
            texto(g, estrellas(f.nivel(), 5) + " " + f.calidad() + " · +" + f.bonus() + " % al plato", LX + 34, y + 14, ESTRELLA, 0.68f);
            barra(g, LX + 34, y + 25, 96, f.prog(), 100, f.prog() >= 100 ? VERDE : AZUL);
            texto(g, f.prog() >= 100 ? "¡Lista!" : f.prog() + " % · " + tiempo(f.resta()), LX + 134, y + 24, f.prog() >= 100 ? VERDE : GRIS, 0.62f);
            texto(g, "Michigranjero: " + recortar(f.granjero(), 12) + " (" + recortar(f.aspecto(), 14) + ") · da " + f.rend() + " por cosecha" + (f.animales() > 0 ? " · " + f.animales() + " animales" : ""), LX + 34, y + 35, SUAVE, 0.62f);
            int bx = LX + 360 - 3 * 58;
            boton(g, bx, y + 3, 54, 14, "✎ NOMBRE", 0xFF7A4AC0, duenio, duenio && dentro(mx, my, bx, y + 3, 54, 14), 0.62f);
            boton(g, bx + 58, y + 3, 54, 14, "ASPECTO ▶", 0xFF4A7AC0, duenio, duenio && dentro(mx, my, bx + 58, y + 3, 54, 14), 0.62f);
            boolean sube = puede && f.tope().isEmpty() && monedas >= f.coste();
            String txt = !f.tope().isEmpty() ? f.tope() : "▲ NIVEL " + (f.nivel() + 1) + " · " + f.coste();
            boton(g, bx, y + 21, 170, 14, txt, 0xFF2E8B57, sube, sube && dentro(mx, my, bx, y + 21, 170, 14), 0.66f);
        }
        texto(g, "«NOMBRE»: escribe el nombre en el chat y pulsa Enter.", LX, CY + 8 + 5 * (FH + 3) + 2, SUAVE, 0.7f);
        // a la derecha, lo que se puede comprar
        texto(g, huerto ? "COMPRAR UN HUERTO" : "COMPRAR UN CORRAL", 400, CY - 8, ORO, 1.1f);
        List<CatC> cat = new ArrayList<>();
        for (CatC c : catalogoGranja) if (c.kind().equals(kind)) cat.add(c);
        int pags = paginas(cat.size(), 3);
        pagCatGranja = Math.max(0, Math.min(pags - 1, pagCatGranja));
        if (pags > 1) paginador(g, mx, my, 548, CY - 12, pagCatGranja, pags);
        for (int k = 0; k < 3 && pagCatGranja * 3 + k < cat.size(); k++) {
            CatC c = cat.get(pagCatGranja * 3 + k);
            int y = CY + 8 + k * 66;
            boolean libre = c.bloqueo().isEmpty();
            caja(g, 400, y, 220, 62, FONDO, libre ? VERDE : BORDE);
            objeto(g, c.icono(), 406, y + 6, 1.4f);
            texto(g, c.nombre(), 432, y + 4, BLANCO, 0.92f);
            texto(g, c.precio() + " monedas · cada " + (c.ciclo() / 60) + " min", 432, y + 15, MONEDA, 0.68f);
            texto(g, "Equivale a: " + c.fuente(), 432, y + 25, LILA, 0.62f);
            parrafo(g, c.texto(), 406, y + 37, 208, SUAVE, 0.66f);
            boolean ok = puede && libre && monedas >= c.precio();
            boton(g, 400 + 220 - 88, y + 4, 84, 15, libre ? "COMPRAR" : c.bloqueo(), 0xFF2E8B57, ok, ok && dentro(mx, my, 400 + 220 - 88, y + 4, 84, 15), 0.66f);
        }
        parrafo(g, "Cada nivel mejora la calidad de lo que dan (y, en los corrales, suma un animal). Los ingredientes de calidad hacen que el plato que los lleva pague más.", 400, CY + 8 + 3 * 66 + 2, 220, SUAVE, 0.66f);
    }

    private void clicGranja(int mx, int my, String kind) {
        List<FincaC> mias = fincasDe(kind);
        boolean puede = duenio && !construyendo;
        int dp = clicPaginador(mx, my, 295, CY - 12);
        if (dp != 0) { pagFincas = Math.max(0, Math.min(paginas(mias.size(), 5) - 1, pagFincas + dp)); return; }
        for (int i = 0; i < 5 && pagFincas * 5 + i < mias.size(); i++) {
            FincaC f = mias.get(pagFincas * 5 + i);
            int y = CY + 8 + i * (FH + 3), bx = LX + 360 - 3 * 58;
            if (duenio && dentro(mx, my, bx, y + 3, 54, 14)) { pedirTexto("/cafe granja nombre " + (f.slot() + 1) + " "); return; }
            if (duenio && dentro(mx, my, bx + 58, y + 3, 54, 14)) { enviar("granjaAspecto", String.valueOf(f.slot()), ""); return; }
            if (puede && dentro(mx, my, bx, y + 21, 170, 14)) { enviar("granjaMejorar", String.valueOf(f.slot()), ""); return; }
        }
        List<CatC> cat = new ArrayList<>();
        for (CatC c : catalogoGranja) if (c.kind().equals(kind)) cat.add(c);
        int d = clicPaginador(mx, my, 548, CY - 12);
        if (d != 0) { pagCatGranja = Math.max(0, Math.min(paginas(cat.size(), 3) - 1, pagCatGranja + d)); return; }
        for (int k = 0; k < 3 && pagCatGranja * 3 + k < cat.size(); k++) {
            int y = CY + 8 + k * 66;
            if (puede && dentro(mx, my, 400 + 220 - 88, y + 4, 84, 15)) { enviar("granjaComprar", cat.get(pagCatGranja * 3 + k).id(), ""); return; }
        }
    }

    private void despensaTab(DrawContext g, int mx, int my) {
        texto(g, "DESPENSA", LX, CY - 8, ORO, 1.1f);
        texto(g, "Cosecha de tus michigranjeros. Pon cada despensa en tu café (BAÚL): sin ella puesta no se usa.", LX + 70, CY - 6, SUAVE, 0.68f);
        if (despensa.isEmpty()) {
            parrafo(g, "La despensa está vacía. Compra un huerto o un corral en las pestañas HUERTOS y GANADO: cuando esté listo, su michigranjero traerá la cosecha aquí.", LX, CY + 14, 340, GRIS, 0.9f);
        }
        for (int i = 0; i < despensa.size() && i < 12; i++) {
            DespC d = despensa.get(i);
            int x = LX + (i % 2) * 308, y = CY + 8 + (i / 2) * 36;
            caja(g, x, y, 296, 33, FONDO, BORDE);
            objeto(g, d.item(), x + 5, y + 8, 1.0f);
            texto(g, d.nombre() + " · " + d.total() + "/" + granjaCap, x + 26, y + 3, BLANCO, 0.8f);
            for (int q = 0; q < 5; q++) {
                int cx = x + 26 + q * 54;
                int n = d.q()[q];
                g.fill(cx, y + 15, cx + 50, y + 29, n > 0 ? 0xFF2E2E58 : 0xFF1E1E34);
                texto(g, (q < calidadesGranja.size() ? calidadesGranja.get(q) : "") + (q < calidadesPct.size() ? "" : ""), cx + 2, y + 16, n > 0 ? ESTRELLA : 0xFF66667A, 0.5f);
                texto(g, n + " × +" + (q < calidadesPct.size() ? calidadesPct.get(q) : 0) + " %", cx + 2, y + 22, n > 0 ? BLANCO : 0xFF66667A, 0.55f);
            }
        }
        int ty = CY + 8 + 6 * 36 + 4;
        texto(g, "Cosechado en total: " + granjaCosechado + " · Extra ganado con la granja: " + granjaExtra + " monedas", LX, ty, MONEDA, 0.8f);
        StringBuilder sb = new StringBuilder("Calidad (nivel del huerto o corral): ");
        for (int q = 0; q < calidadesGranja.size(); q++) sb.append(q > 0 ? " · " : "").append(calidadesGranja.get(q)).append(" +").append(calidadesPct.get(q)).append(" %");
        texto(g, sb.toString(), LX, ty + 11, SUAVE, 0.62f);
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
        if (dentro(mx, my, 546, 48, 74, 20)) { enviar("salir", "", ""); return; }
        for (int i = 0; i < TOP.length; i++) if (dentro(mx, my, 20 + i * TOP_W, 48, TOP_W - 4, 20)) { irATab(TOP_REF[i] < 100 ? TOP_REF[i] : subActual[TOP_REF[i] - 100]); return; }
        int gr = grupoDe(tab);
        if (gr >= 0) {
            for (int k = 0; k < SUBS[gr].length; k++) if (dentro(mx, my, 20 + k * (subW(gr) + 4), 70, subW(gr), 14)) { irATab(SUBS[gr][k]); return; }
            my -= SUBY;
        }
        switch (tab) {
            case T_CAFE -> clicCafe(mx, my);
            case T_TIENDA -> clicTienda(mx, my);
            case T_RECETARIO -> { int d = clicPaginador(mx, my, LX + 528, CY - 12); if (d != 0) pagRec = Math.max(0, Math.min(paginas(platos.size(), REC_POR_PAG) - 1, pagRec + d)); }
            case T_ESTRELLAS -> clicEstrellas(mx, my);
            case T_DECORAR -> clicDecorar(mx, my);
            case T_CAFES -> clicCafes(mx, my);
            case T_PERSONAL -> clicPersonal(mx, my);
            case T_BAUL -> clicBaul(mx, my);
            case T_PLANOS -> clicPlanos(mx, my);
            case T_HUERTOS -> clicGranja(mx, my, "huerto");
            case T_GANADO -> clicGranja(mx, my, "corral");
            case T_RESENAS -> { int dc = clicPaginador(mx, my, LX + 528, CY + 2); if (dc != 0) pagResenas = Math.max(0, Math.min(paginas(resenas.size(), 7) - 1, pagResenas + dc)); }
            case T_CLIENTELA -> { int dc = clicPaginador(mx, my, LX + 528, CY + 2); if (dc != 0) pagClientela = Math.max(0, Math.min(paginas(clientela.size(), 12) - 1, pagClientela + dc)); }
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
        if (visitante) {
            if (!yaMeGusta && dentro(mx, my, LX, CY + 100, LW, 28)) enviar("megusta", "", "");
            else if (dentro(mx, my, LX, CY + 134, LW, 24)) { tab = T_CAFES; enviar("refrescar", "cafes", ""); }
            return;
        }
        if (duenio && !construyendo && !abierto() && !estado.equals("cerrando") && dentro(mx, my, RX + RW - 110, CY - 11, 110, 14)) { enviar("visitas", "", ""); return; }
        if (duenio && !construyendo && dentro(mx, my, RX + RW - 170, CY - 11, 54, 14)) { enviar("guia", "", ""); return; }
        if (duenio && !construyendo && !abierto() && !estado.equals("cerrando") && dentro(mx, my, RX + 96, CY - 11, 76, 14)) { pedirTexto("/cafe nombre "); return; }
        if (construyendo) {
            if (!duenio) return;
            if (dentro(mx, my, LX, CY + 100, LW, 28)) { tab = T_DECORAR; enviar("refrescar", "decorar", ""); return; }
            if (dentro(mx, my, LX, CY + 134, LW, 28)) { enviar("terminarConstruir", "", ""); return; }
            if (libre && deshacerN > 0 && dentro(mx, my, LX, CY + 166, LW, 24)) { enviar("deshacer", "", ""); return; }
            for (int i = 0; i < estilo.size(); i++) {
                int y = CY + 4 + i * 36;
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
        int dm = paginas(mejoras.size(), MEJ_POR_PAG) > 1 ? clicPaginador(mx, my, LX + 308 + PW - 72, CY + 5) : 0;
        if (dm != 0) { pagMejoras = Math.max(0, Math.min(paginas(mejoras.size(), MEJ_POR_PAG) - 1, pagMejoras + dm)); return; }
        if (!puedeComprar()) return;
        for (int n = 0; n < TIENDA_POR_PAG; n++) {
            int i = pagTienda * TIENDA_POR_PAG + n;
            if (i >= tiendaPlatos.size()) break;
            PlatoC p = tiendaPlatos.get(i);
            int y = CY + 22 + n * (PH + 4);
            if (!p.tengo() && p.bloqueo().isEmpty() && monedas >= p.coste() && dentro(mx, my, LX + PW - 92, y + 36, 86, 20)) enviar("comprarPlato", p.id(), "");
        }
        for (int n = 0; n < MEJ_POR_PAG; n++) {
            int i = pagMejoras * MEJ_POR_PAG + n;
            if (i >= mejoras.size()) break;
            MejoraC m = mejoras.get(i);
            int y = CY + 20 + n * 28, x = LX + 308;
            if (m.nivel() < m.max() && monedas >= m.precio() && dentro(mx, my, x + PW - 88, y + 3, 82, 20)) enviar("comprarMejora", m.id(), "");
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
