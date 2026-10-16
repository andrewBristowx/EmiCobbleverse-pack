package emi.survivors.client;

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
import emi.survivors.net.AccionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * El menu del mundo del minijuego (se abre al entrar, con el objeto del menu o tocando el terminal de la sala): JUGAR (elegir uno de tus Pokemon y empezar),
 * MEJORAS (gastar michiestrellitas en mejoras permanentes de cada Pokemon), GRUPO (llega en la fase 2) y TOP (las mejores marcas por tiempo aguantado).
 * Los datos los manda el servidor (JSON); cada clic manda una accion al servidor, que lo comprueba todo y vuelve a mandar los datos.
 */
@Environment(EnvType.CLIENT)
public final class MenuPantalla extends Lienzo {
    private static final Identifier GEMA = Identifier.of("emi_survivors", "textures/gema.png");
    private static final int T_JUGAR = 0, T_MEJORAS = 1, T_GRUPO = 2, T_TOP = 3;
    private static final String[] TABS = {"JUGAR", "MEJORAS", "GRUPO", "TOP"};
    private static final String[] TABS_ID = {"jugar", "mejoras", "grupo", "top"};

    /** La pantalla abierta ahora (para actualizarla cuando llegan datos nuevos del servidor). */
    public static MenuPantalla abierta;

    // ------------------------------------------------------------ datos
    private static final class Poke {
        int slot, nivel, mejorTiempo, partidas;
        String id, especie, nombre, apodo, tipo, ataque, ataqueTexto, ataqueIcono;
        boolean brillante;
        List<String> aspectos = new ArrayList<>();
        Map<String, Integer> mejoras = new HashMap<>();
        RenderablePokemon rp;
        FloatingState st;
        boolean intentado;

        String titulo() { return apodo == null || apodo.isEmpty() ? nombre : apodo; }
    }

    private record Mejora(String id, String nombre, String texto, String icono, int[] costos) {}
    private record Marca(int pos, String nombre, boolean mio, String especie, int segundos, int derrotados, int nivel, boolean jefe) {}
    private record Resultado(String motivo, int segundos, int derrotados, int nivel, int monedas, int puesto, boolean jefe, boolean cuenta, String equipo) {}
    private record Miembro(String uuid, String nombre, boolean lider, boolean yo, boolean listo, String especie, int nivel) {}
    private record Abierto(String lider, String nombre, int n, String miembros) {}

    private final List<Poke> equipo = new ArrayList<>();
    private final List<Mejora> catalogo = new ArrayList<>();
    private final List<Marca> top = new ArrayList<>(), topGrupos = new ArrayList<>();
    private final List<Miembro> miembros = new ArrayList<>();
    private final List<Abierto> abiertos = new ArrayList<>();
    private Resultado resultado;
    private boolean enGrupo, soyLider, grupoJugando, topDeGrupos;
    private int maxGrupo = 4;
    private long ultimoRefresco = System.currentTimeMillis();
    private int monedas, partidas, maxPartidas, miPuesto, miMejor;
    private int tab, sel;
    /** Ya se ha pulsado JUGAR y se espera al servidor (la primera vez que se usa una arena tarda un par de segundos): no se vuelve a mandar. */
    private boolean preparando;
    private final long t0 = System.nanoTime();

    public MenuPantalla(String json) {
        super(Text.literal("Emi Survivors"));
        cargar(json, false);
        abierta = this;
    }

    @Override public void removed() { if (abierta == this) abierta = null; super.removed(); }

    /** Carga (o recarga) los datos del servidor, conservando la pestaña y el Pokemon elegido mientras se pueda. {@code soloDatos} = es un refresco: no se cambia de pestaña. */
    public void cargar(String json, boolean soloDatos) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        preparando = false;
        String idSel = sel >= 0 && sel < equipo.size() ? equipo.get(sel).id : null;
        String t = o.has("tab") ? o.get("tab").getAsString() : "jugar";
        if (!soloDatos) for (int i = 0; i < TABS_ID.length; i++) if (TABS_ID[i].equals(t)) tab = i;
        monedas = o.get("monedas").getAsInt();
        partidas = o.get("partidas").getAsInt(); maxPartidas = o.get("maxPartidas").getAsInt();
        miPuesto = o.get("miPuesto").getAsInt(); miMejor = o.get("miMejor").getAsInt();

        Map<String, Poke> antes = new HashMap<>();
        for (Poke p : equipo) antes.put(p.id, p);
        equipo.clear();
        for (JsonElement e : o.getAsJsonArray("equipo")) {
            JsonObject j = e.getAsJsonObject();
            Poke p = new Poke();
            p.id = j.get("id").getAsString(); p.slot = j.get("slot").getAsInt(); p.especie = j.get("especie").getAsString();
            p.nombre = j.get("nombre").getAsString(); p.apodo = j.get("apodo").getAsString(); p.nivel = j.get("nivel").getAsInt();
            p.tipo = j.get("tipo").getAsString(); p.brillante = j.get("brillante").getAsBoolean();
            for (JsonElement a : j.getAsJsonArray("aspectos")) p.aspectos.add(a.getAsString());
            p.ataque = j.get("ataque").getAsString(); p.ataqueTexto = j.get("ataqueTexto").getAsString(); p.ataqueIcono = j.get("ataqueIcono").getAsString();
            for (var en : j.getAsJsonObject("mejoras").entrySet()) p.mejoras.put(en.getKey(), en.getValue().getAsInt());
            p.mejorTiempo = j.get("mejorTiempo").getAsInt(); p.partidas = j.get("partidas").getAsInt();
            Poke previo = antes.get(p.id);
            if (previo != null) { p.rp = previo.rp; p.st = previo.st; p.intentado = previo.intentado; }     // el modelo ya creado se reutiliza
            equipo.add(p);
        }
        sel = 0;
        if (idSel != null) for (int i = 0; i < equipo.size(); i++) if (equipo.get(i).id.equals(idSel)) sel = i;

        catalogo.clear();
        for (JsonElement e : o.getAsJsonArray("catalogo")) {
            JsonObject j = e.getAsJsonObject();
            JsonArray c = j.getAsJsonArray("costos");
            int[] costos = new int[c.size()];
            for (int i = 0; i < costos.length; i++) costos[i] = c.get(i).getAsInt();
            catalogo.add(new Mejora(j.get("id").getAsString(), j.get("nombre").getAsString(), j.get("texto").getAsString(), j.get("icono").getAsString(), costos));
        }
        top.clear();
        for (JsonElement e : o.getAsJsonArray("top")) {
            JsonObject j = e.getAsJsonObject();
            top.add(new Marca(j.get("pos").getAsInt(), j.get("nombre").getAsString(), j.get("mio").getAsBoolean(), j.get("especie").getAsString(), j.get("segundos").getAsInt(),
                    j.get("derrotados").getAsInt(), j.get("nivel").getAsInt(), j.get("jefe").getAsBoolean()));
        }
        topGrupos.clear();
        if (o.has("topGrupos")) for (JsonElement e : o.getAsJsonArray("topGrupos")) {
            JsonObject j = e.getAsJsonObject();
            topGrupos.add(new Marca(j.get("pos").getAsInt(), j.get("nombre").getAsString(), j.get("mio").getAsBoolean(), j.get("especie").getAsString(), j.get("segundos").getAsInt(),
                    j.get("derrotados").getAsInt(), j.get("nivel").getAsInt(), j.get("jefe").getAsBoolean()));
        }
        miembros.clear(); abiertos.clear();
        enGrupo = false; soyLider = false; grupoJugando = false;
        if (o.has("grupo")) {
            JsonObject g = o.getAsJsonObject("grupo");
            enGrupo = g.get("enGrupo").getAsBoolean();
            if (enGrupo) {
                soyLider = g.get("soyLider").getAsBoolean(); grupoJugando = g.get("enPartida").getAsBoolean(); maxGrupo = g.get("max").getAsInt();
                for (JsonElement e : g.getAsJsonArray("miembros")) {
                    JsonObject j = e.getAsJsonObject();
                    miembros.add(new Miembro(j.get("uuid").getAsString(), j.get("nombre").getAsString(), j.get("lider").getAsBoolean(), j.get("yo").getAsBoolean(), j.get("listo").getAsBoolean(),
                            j.get("especie").getAsString(), j.get("nivel").getAsInt()));
                }
            }
        }
        if (o.has("abiertos")) for (JsonElement e : o.getAsJsonArray("abiertos")) {
            JsonObject j = e.getAsJsonObject();
            abiertos.add(new Abierto(j.get("lider").getAsString(), j.get("nombre").getAsString(), j.get("n").getAsInt(), j.get("miembros").getAsString()));
        }
        if (o.has("resultado")) {
            JsonObject r = o.getAsJsonObject("resultado");
            resultado = new Resultado(r.get("motivo").getAsString(), r.get("segundos").getAsInt(), r.get("derrotados").getAsInt(), r.get("nivel").getAsInt(), r.get("monedas").getAsInt(),
                    r.get("puesto").getAsInt(), r.get("jefe").getAsBoolean(), r.get("cuenta").getAsBoolean(), r.has("equipo") ? r.get("equipo").getAsString() : "");
        }
    }

    private Miembro yo() { for (Miembro m : miembros) if (m.yo()) return m; return null; }

    private int listosSinLider() { int n = 0; for (Miembro m : miembros) if (!m.lider() && m.listo()) n++; return n; }

    private int otros() { return Math.max(0, miembros.size() - 1); }

    // ------------------------------------------------------------ utilidades

    private static String tiempo(int s) { return (s / 60) + ":" + String.format("%02d", s % 60); }

    private static final Map<String, String> TIPOS = Map.ofEntries(
            Map.entry("fire", "Fuego"), Map.entry("water", "Agua"), Map.entry("electric", "Eléctrico"), Map.entry("grass", "Planta"), Map.entry("ice", "Hielo"), Map.entry("fighting", "Lucha"),
            Map.entry("poison", "Veneno"), Map.entry("ground", "Tierra"), Map.entry("flying", "Volador"), Map.entry("psychic", "Psíquico"), Map.entry("bug", "Bicho"), Map.entry("rock", "Roca"),
            Map.entry("ghost", "Fantasma"), Map.entry("dragon", "Dragón"), Map.entry("dark", "Siniestro"), Map.entry("steel", "Acero"), Map.entry("fairy", "Hada"), Map.entry("normal", "Normal"));
    private static final Map<String, Integer> COLORES = Map.ofEntries(
            Map.entry("fire", 0xFFEE8130), Map.entry("water", 0xFF6390F0), Map.entry("electric", 0xFFE0B800), Map.entry("grass", 0xFF7AC74C), Map.entry("ice", 0xFF7FCFCB), Map.entry("fighting", 0xFFC22E28),
            Map.entry("poison", 0xFFA33EA1), Map.entry("ground", 0xFFC9A44A), Map.entry("flying", 0xFFA98FF3), Map.entry("psychic", 0xFFF95587), Map.entry("bug", 0xFFA6B91A), Map.entry("rock", 0xFFB6A136),
            Map.entry("ghost", 0xFF735797), Map.entry("dragon", 0xFF6F35FC), Map.entry("dark", 0xFF705746), Map.entry("steel", 0xFF9A9AB5), Map.entry("fairy", 0xFFD685AD), Map.entry("normal", 0xFFA8A77A));

    private void enviar(String accion, String a, String b) { ClientPlayNetworking.send(new AccionPayload(accion, a, b)); }

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

    private void modelo(DrawContext g, Poke p, float cx, float cy, float escala, float delta) {
        preparar(p);
        if (p.rp == null) return;
        var m = g.getMatrices();
        m.push();
        m.translate(cx, cy - 0.935f * escala, 0);     // el modelo de Cobblemon cuelga ~0,35*escala por debajo del punto y mide ~1,17*escala
        p.st.updatePartialTicks(delta);
        float giro = (float) Math.toRadians(35 + (System.nanoTime() - t0) / 1e9 * 45);
        try {
            PokemonGuiUtilsKt.drawProfilePokemon(p.rp, m, new Quaternionf().rotationXYZ((float) Math.toRadians(13), giro, 0f), PoseType.PROFILE, p.st, delta, escala, true, false, 1f, 1f, 1f, 1f, 0f, 0f);
        } catch (Throwable t) { p.rp = null; }
        m.pop();
    }

    private void boton(DrawContext g, int x, int y, int w, int h, String texto, int color, boolean activo, boolean sobre, float tam) {
        int c = activo ? color : 0xFF444455;
        if (activo && sobre) c = (c & 0x00FFFFFF) | 0xFF000000 | ((Math.min(255, ((c >> 16) & 255) + 40) << 16) | (Math.min(255, ((c >> 8) & 255) + 40) << 8) | Math.min(255, (c & 255) + 40));
        caja(g, x, y, w, h, c, activo ? 0xFFFFFFFF : 0xFF666677);
        textoCentrado(g, texto, x + w / 2, y + (int) ((h - 8 * tam) / 2), activo ? BLANCO : 0xFF9A9AAA, tam);
    }

    private Poke elegido() { return sel >= 0 && sel < equipo.size() ? equipo.get(sel) : null; }

    // ------------------------------------------------------------ dibujo

    private static final int LX = 20, LW = 226, RX = 258, RW = 362, CY = 78;

    @Override
    protected void dibujar(DrawContext g, int mx, int my, float delta) {
        // mientras se mira el grupo (o se espera a los demas), se pide al servidor cada segundo y medio que lo refresque
        long ahora = System.currentTimeMillis();
        if ((tab == T_GRUPO || (tab == T_JUGAR && enGrupo)) && ahora - ultimoRefresco > 1500) { ultimoRefresco = ahora; enviar("refrescar", TABS_ID[tab], ""); }
        // cabecera
        texto(g, "EMI SURVIVORS", 20, 10, ORO, 2.2f);
        texto(g, "Sobrevive a la horda con tus Pokémon y los Michialiens", 22, 32, GRIS, 0.9f);
        g.drawTexture(GEMA, 488, 10, 16, 16, 0f, 0f, 128, 128, 128, 128);
        texto(g, monedas + " michiestrellitas", 508, 14, 0xFFFFE070, 1f);
        texto(g, "Con ellas mejoras a tus Pokémon", 488, 28, GRIS, 0.8f);

        // pestañas y salir
        for (int i = 0; i < TABS.length; i++) {
            int x = 20 + i * 98;
            boolean act = i == tab, sobre = dentro(mx, my, x, 48, 92, 20);
            caja(g, x, 48, 92, 20, act ? 0xFF3A3A66 : sobre ? 0xFF2A2A4A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
            textoCentrado(g, TABS[i], x + 46, 54, act ? ORO : BLANCO, 1f);
        }
        boton(g, 538, 48, 82, 20, "SALIR", 0xFF8A3A3A, true, dentro(mx, my, 538, 48, 82, 20), 1f);

        switch (tab) {
            case T_JUGAR -> { lista(g, mx, my, delta); jugar(g, mx, my, delta); }
            case T_MEJORAS -> { lista(g, mx, my, delta); mejoras(g, mx, my); }
            case T_GRUPO -> grupo(g, mx, my);
            default -> clasificacion(g, mx, my);
        }
        textoCentrado(g, "Inspirado en PokéSurvivor de JPTER78 · pokesurvivor.com", W / 2, 346, 0xFF77778A, 0.8f);
    }

    /** La lista de los Pokemon del equipo (a la izquierda en JUGAR y MEJORAS). */
    private void lista(DrawContext g, int mx, int my, float delta) {
        if (equipo.isEmpty()) {
            caja(g, LX, CY, LW, 120, 0xFF1B1B30, 0xFF55557A);
            parrafo(g, "No tienes ningún Pokémon en tu equipo. Captura alguno, ponlo en tu equipo y vuelve a abrir el menú.", LX + 10, CY + 12, LW - 20, GRIS, 1f);
            return;
        }
        for (int i = 0; i < equipo.size(); i++) {
            Poke p = equipo.get(i);
            int y = CY + i * 43;
            boolean act = i == sel, sobre = dentro(mx, my, LX, y, LW, 40);
            caja(g, LX, y, LW, 40, act ? 0xFF2E2E58 : sobre ? 0xFF25254A : 0xFF1B1B30, act ? ORO : 0xFF55557A);
            modelo(g, p, LX + 26, y + 20, 24f, delta);
            texto(g, p.titulo() + (p.brillante ? " ★" : ""), LX + 52, y + 6, BLANCO, 1.05f);
            texto(g, "Nv. " + p.nivel, LX + 52, y + 18, GRIS, 0.9f);
            int col = COLORES.getOrDefault(p.tipo, 0xFFA8A77A);
            g.fill(LX + 52, y + 29, LX + 52 + 52, y + 37, col);
            textoCentrado(g, TIPOS.getOrDefault(p.tipo, p.tipo), LX + 52 + 26, y + 29, BLANCO, 0.7f);
            int total = 0; for (int v : p.mejoras.values()) total += v;
            if (total > 0) texto(g, "Mejoras: " + total, LX + 130, y + 29, 0xFFFFE070, 0.8f);
        }
    }

    private void jugar(DrawContext g, int mx, int my, float delta) {
        int y = CY;
        if (resultado != null) {
            Resultado r = resultado;
            int h = 46;
            caja(g, RX, y, RW, h, 0xFF22223C, r.puesto() > 0 ? ORO : 0xFF55557A);
            texto(g, "Tu última partida: " + tiempo(r.segundos()) + " · " + r.derrotados() + " derrotados · nivel " + r.nivel() + (r.jefe() ? " · ¡jefe derrotado!" : ""), RX + 8, y + 6, BLANCO, 1f);
            String l2 = !r.cuenta() ? "Esta partida no cuenta para el top ni da monedas (modo de prueba)." : "+" + r.monedas() + " michiestrellitas" + (r.puesto() > 0 ? " · ¡Puesto " + r.puesto() + (r.equipo().isEmpty() ? " del top!" : " del top de grupos!") : " · no mejora la marca");
            texto(g, l2, RX + 8, y + 20, r.puesto() > 0 ? ORO : 0xFFFFE070, 1f);
            texto(g, r.motivo() + (r.equipo().isEmpty() ? "" : "  Jugaste con: " + r.equipo()), RX + 8, y + 33, GRIS, 0.8f);
            y += h + 6;
        }
        Poke p = elegido();
        if (p == null) { parrafo(g, "Cuando tengas un Pokémon en tu equipo podrás jugar con él.", RX, y + 20, RW, GRIS, 1f); return; }
        int altoPanel = 330 - y - 40;
        caja(g, RX, y, RW, altoPanel, 0xFF1B1B30, 0xFF55557A);
        modelo(g, p, RX + 66, y + 104, 56f, delta);
        int tx = RX + 140;
        texto(g, p.titulo() + (p.brillante ? " ★" : ""), tx, y + 8, ORO, 1.5f);
        texto(g, "Nv. " + p.nivel + " · Tipo " + TIPOS.getOrDefault(p.tipo, p.tipo), tx, y + 26, COLORES.getOrDefault(p.tipo, GRIS) | 0xFF000000, 1f);
        icono(g, p.ataqueIcono, tx, y + 42, 1f);
        texto(g, "Ataque: " + p.ataque, tx + 22, y + 42, BLANCO, 1f);
        parrafo(g, p.ataqueTexto, tx + 22, y + 54, RW - 140 - 30, GRIS, 0.9f);
        StringBuilder sb = new StringBuilder();
        for (Mejora m : catalogo) { int n = p.mejoras.getOrDefault(m.id(), 0); if (n > 0) sb.append(m.nombre()).append(' ').append(n).append("  "); }
        texto(g, sb.length() == 0 ? "Sin mejoras permanentes todavía (mira la pestaña MEJORAS)." : "Mejoras: " + sb, tx, y + 84, 0xFFFFE070, 0.85f);
        texto(g, p.partidas == 0 ? "Aún no has jugado con él." : "Mejor tiempo con él: " + tiempo(p.mejorTiempo) + " (" + p.partidas + " partidas)", tx, y + 98, GRIS, 0.85f);
        boolean lleno = partidas >= maxPartidas;
        boolean sobre = dentro(mx, my, RX + RW / 2 - 100, 296, 200, 32);
        if (!enGrupo) {
            boton(g, RX + RW / 2 - 100, 296, 200, 32, lleno ? "MUNDO LLENO" : preparando ? "PREPARANDO..." : "¡A JUGAR!", 0xFF2E9B4E, !lleno && !preparando, sobre, 1.4f);
            textoCentrado(g, "Partidas en curso: " + partidas + "/" + maxPartidas + "  ·  ¿Con amigos? Mira la pestaña GRUPO", RX + RW / 2, 332, GRIS, 0.8f);
        } else if (grupoJugando) {
            boton(g, RX + RW / 2 - 100, 296, 200, 32, "TU GRUPO ESTÁ JUGANDO", 0xFF555566, false, false, 1.0f);
            textoCentrado(g, "Cuando acabe la partida podréis volver a jugar juntos.", RX + RW / 2, 332, GRIS, 0.8f);
        } else if (soyLider) {
            int faltan = otros() - listosSinLider();
            String t = lleno ? "MUNDO LLENO" : preparando ? "PREPARANDO..." : faltan > 0 ? "¡EMPEZAR! (faltan " + faltan + ")" : "¡EMPEZAR!";
            boton(g, RX + RW / 2 - 100, 296, 200, 32, t, 0xFF2E9B4E, !lleno && !preparando && faltan <= 0, sobre, faltan > 0 ? 1.0f : 1.4f);
            textoCentrado(g, "Grupo de " + miembros.size() + "/" + maxGrupo + (faltan > 0 ? ": esperando a que todos estén listos" : ": ¡todos listos!") + "  ·  Partidas: " + partidas + "/" + maxPartidas, RX + RW / 2, 332, GRIS, 0.8f);
        } else {
            Miembro m = yo();
            boolean listo = m != null && m.listo();
            boton(g, RX + RW / 2 - 100, 296, 200, 32, preparando ? "..." : listo ? "LISTO ✓  (pulsa para cambiar)" : "¡LISTO!", listo ? 0xFF3A6EC8 : 0xFF2E9B4E, !preparando, sobre, listo ? 0.95f : 1.4f);
            textoCentrado(g, listo ? "Esperando a que el líder empiece la partida..." : "Elige tu Pokémon y pulsa ¡LISTO!", RX + RW / 2, 332, GRIS, 0.8f);
        }
    }

    private void mejoras(DrawContext g, int mx, int my) {
        Poke p = elegido();
        if (p == null) { parrafo(g, "Cuando tengas un Pokémon en tu equipo podrás mejorarlo.", RX, CY + 20, RW, GRIS, 1f); return; }
        texto(g, "Mejoras permanentes de " + p.titulo(), RX, CY - 2, ORO, 1.2f);
        texto(g, "Se compran con michiestrellitas y cuentan en todas sus partidas.", RX, CY + 12, GRIS, 0.85f);
        for (int i = 0; i < catalogo.size(); i++) {
            Mejora m = catalogo.get(i);
            int y = CY + 28 + i * 34;
            int nivel = p.mejoras.getOrDefault(m.id(), 0);
            caja(g, RX, y, RW, 31, 0xFF1B1B30, 0xFF55557A);
            icono(g, m.icono(), RX + 6, y + 7, 1.1f);
            texto(g, m.nombre(), RX + 30, y + 5, BLANCO, 1f);
            texto(g, m.texto(), RX + 30, y + 17, GRIS, 0.75f);
            for (int k = 0; k < m.costos().length; k++) g.fill(RX + 212 + k * 9, y + 5, RX + 219 + k * 9, y + 11, k < nivel ? 0xFFFFD34E : 0xFF555566);
            int bx = RX + 266, bw = 88;
            if (nivel >= m.costos().length) boton(g, bx, y + 4, bw, 22, "AL MÁXIMO", 0xFF555566, false, false, 0.9f);
            else {
                int costo = m.costos()[nivel];
                boolean puede = monedas >= costo;
                boton(g, bx, y + 4, bw, 22, "Mejorar  " + costo, 0xFF2E9B4E, puede, puede && dentro(mx, my, bx, y + 4, bw, 22), 0.9f);
            }
        }
    }

    private static final int BX_CREAR = 20, BY_CREAR = CY + 20, BW_CREAR = 210;

    private void grupo(DrawContext g, int mx, int my) {
        if (!enGrupo) {
            // sin grupo: crear uno, o unirse a uno de los abiertos
            caja(g, 20, CY, 220, 236, 0xFF1B1B30, 0xFF55557A);
            texto(g, "CREAR UN GRUPO", 32, CY + 10, ORO, 1.3f);
            parrafo(g, "Juega con amigos: hasta 4 jugadores en la misma arena, cada uno con su Pokémon. Si alguien cae, los demás pueden revivirlo. El top cuenta lo que aguante TODO el equipo.", 32, CY + 30, 196, GRIS, 0.9f);
            boton(g, 30, CY + 160, 200, 30, "CREAR GRUPO", 0xFF2E9B4E, true, dentro(mx, my, 30, CY + 160, 200, 30), 1.2f);
            parrafo(g, "Tú serás el líder y empezarás la partida cuando todos estén listos.", 32, CY + 198, 196, GRIS, 0.8f);
            texto(g, "GRUPOS ABIERTOS", 258, CY + 2, ORO, 1.3f);
            if (abiertos.isEmpty()) parrafo(g, "Ahora mismo no hay ningún grupo al que unirse. Crea el tuyo y dile a tus amigos que se unan desde aquí.", 258, CY + 26, 350, GRIS, 1f);
            for (int i = 0; i < Math.min(5, abiertos.size()); i++) {
                Abierto a = abiertos.get(i);
                int y = CY + 24 + i * 44;
                caja(g, 258, y, 362, 40, 0xFF1B1B30, 0xFF55557A);
                texto(g, "Grupo de " + a.nombre() + "  (" + a.n() + "/" + maxGrupo + ")", 266, y + 6, BLANCO, 1.05f);
                texto(g, recortar(a.miembros(), 44), 266, y + 21, GRIS, 0.85f);
                boolean sobre = dentro(mx, my, 536, y + 8, 76, 24);
                boton(g, 536, y + 8, 76, 24, "UNIRME", 0xFF2E9B4E, true, sobre, 0.95f);
            }
            return;
        }
        // en un grupo: los 4 sitios, y salir
        texto(g, "TU GRUPO  (" + miembros.size() + "/" + maxGrupo + ")", 20, CY - 2, ORO, 1.3f);
        texto(g, soyLider ? "Eres el líder: empieza la partida desde la pestaña JUGAR cuando todos estén listos." : "Elige tu Pokémon en la pestaña JUGAR y pulsa ¡LISTO!. El líder empieza la partida.", 20, CY + 14, GRIS, 0.85f);
        for (int i = 0; i < maxGrupo; i++) {
            int y = CY + 30 + i * 44;
            Miembro m = i < miembros.size() ? miembros.get(i) : null;
            caja(g, 20, y, 440, 40, m == null ? 0xFF14141F : 0xFF1B1B30, m != null && m.yo() ? ORO : 0xFF55557A);
            if (m == null) { texto(g, "Sitio libre", 34, y + 14, 0xFF666677, 1f); continue; }
            texto(g, (m.lider() ? "♛ " : "") + m.nombre() + (m.yo() ? " (tú)" : ""), 34, y + 7, m.lider() ? ORO : BLANCO, 1.1f);
            if (m.listo()) {
                texto(g, "LISTO ✓", 34, y + 23, VERDE, 0.95f);
                texto(g, m.especie() + " · Nv. " + m.nivel(), 100, y + 23, GRIS, 0.95f);
            } else texto(g, "Eligiendo su Pokémon...", 34, y + 23, 0xFF888899, 0.9f);
            if (soyLider && !m.yo() && !grupoJugando) {
                boolean sobre = dentro(mx, my, 392, y + 8, 60, 24);
                boton(g, 392, y + 8, 60, 24, "EXPULSAR", 0xFF8A3A3A, true, sobre, 0.7f);
            }
        }
        boton(g, 480, CY + 30, 140, 28, grupoJugando ? "JUGANDO..." : "SALIR DEL GRUPO", 0xFF8A3A3A, !grupoJugando, dentro(mx, my, 480, CY + 30, 140, 28), 0.95f);
        parrafo(g, "Si un compañero cae, quédate a su lado unos 3 segundos para revivirlo. La partida acaba cuando caen todos.", 480, CY + 70, 140, GRIS, 0.85f);
    }

    private static String recortar(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "…"; }

    private void clasificacion(DrawContext g, int mx, int my) {
        texto(g, topDeGrupos ? "Las mejores marcas de grupos: lo que ha aguantado todo el equipo" : "Las mejores marcas en solitario: el tiempo que has aguantado vivo", 20, CY - 2, ORO, 1.1f);
        boolean sobreS = dentro(mx, my, 392, CY - 6, 108, 16), sobreG = dentro(mx, my, 506, CY - 6, 114, 16);
        caja(g, 392, CY - 6, 108, 16, !topDeGrupos ? 0xFF3A3A66 : sobreS ? 0xFF2A2A4A : 0xFF1B1B30, !topDeGrupos ? ORO : 0xFF55557A);
        textoCentrado(g, "EN SOLITARIO", 446, CY - 2, !topDeGrupos ? ORO : BLANCO, 0.85f);
        caja(g, 506, CY - 6, 114, 16, topDeGrupos ? 0xFF3A3A66 : sobreG ? 0xFF2A2A4A : 0xFF1B1B30, topDeGrupos ? ORO : 0xFF55557A);
        textoCentrado(g, "EN GRUPO", 563, CY - 2, topDeGrupos ? ORO : BLANCO, 0.85f);
        List<Marca> lista = topDeGrupos ? topGrupos : top;
        int y = CY + 20;
        g.fill(20, y, 620, y + 16, 0xFF2A2A48);
        String[] cab = topDeGrupos ? new String[]{"#", "Equipo", "", "Tiempo", "Derrotados", "Nivel"} : new String[]{"#", "Jugador", "Pokémon", "Tiempo", "Derrotados", "Nivel"};
        int[] xs = {28, 60, 220, 340, 420, 520};
        for (int i = 0; i < cab.length; i++) texto(g, cab[i], xs[i], y + 4, GRIS, 0.9f);
        y += 18;
        if (lista.isEmpty()) texto(g, topDeGrupos ? "Todavía no hay marcas de grupos. ¡Sed los primeros!" : "Todavía no hay marcas. ¡Sé el primero!", 28, y + 10, BLANCO, 1.1f);
        for (Marca m : lista) {
            g.fill(20, y, 620, y + 20, m.mio() ? 0x663A3A10 : ((m.pos() & 1) == 0 ? 0x22FFFFFF : 0x11FFFFFF));
            int col = m.mio() ? ORO : m.pos() == 1 ? 0xFFFFE070 : BLANCO;
            texto(g, String.valueOf(m.pos()), xs[0], y + 6, col, 1f);
            if (topDeGrupos) texto(g, recortar(m.nombre(), 46) + (m.jefe() ? " ★" : ""), xs[1], y + 6, col, 1f);
            else {
                texto(g, m.nombre() + (m.jefe() ? " ★" : ""), xs[1], y + 6, col, 1f);
                texto(g, m.especie(), xs[2], y + 6, col, 1f);
            }
            texto(g, tiempo(m.segundos()), xs[3], y + 6, col, 1f);
            texto(g, String.valueOf(m.derrotados()), xs[4], y + 6, col, 1f);
            texto(g, String.valueOf(m.nivel()), xs[5], y + 6, col, 1f);
            y += 21;
        }
        if (!topDeGrupos) texto(g, miMejor > 0 ? "Tu mejor marca: " + tiempo(miMejor) + (miPuesto > 0 ? " (puesto " + miPuesto + ")" : "") : "Todavía no tienes marca: ¡juega una partida!", 20, 328, ORO, 1f);
    }

    // ------------------------------------------------------------ clics

    @Override
    protected void clic(int mx, int my, int boton) {
        for (int i = 0; i < TABS.length; i++) if (dentro(mx, my, 20 + i * 98, 48, 92, 20)) {
            tab = i; ultimoRefresco = 0;
            if (i == T_TOP || i == T_GRUPO) enviar("refrescar", TABS_ID[i], "");
            return;
        }
        if (dentro(mx, my, 538, 48, 82, 20)) { enviar("salir", "", ""); return; }
        if (tab == T_JUGAR || tab == T_MEJORAS) {
            for (int i = 0; i < equipo.size(); i++) if (dentro(mx, my, LX, CY + i * 43, LW, 40)) { sel = i; return; }
        }
        Poke p = elegido();
        if (tab == T_JUGAR && p != null && dentro(mx, my, RX + RW / 2 - 100, 296, 200, 32)) { botonJugar(p); return; }
        if (tab == T_MEJORAS && p != null) {
            for (int i = 0; i < catalogo.size(); i++) {
                Mejora m = catalogo.get(i);
                int y = CY + 28 + i * 34, nivel = p.mejoras.getOrDefault(m.id(), 0);
                if (nivel < m.costos().length && monedas >= m.costos()[nivel] && dentro(mx, my, RX + 266, y + 4, 88, 22)) { enviar("comprar", p.id, m.id()); return; }
            }
        }
        if (tab == T_GRUPO) clicGrupo(mx, my);
        if (tab == T_TOP) {
            if (dentro(mx, my, 392, CY - 6, 108, 16)) topDeGrupos = false;
            else if (dentro(mx, my, 506, CY - 6, 114, 16)) topDeGrupos = true;
        }
    }

    /** El boton grande de la pestaña JUGAR: jugar solo, estar listo (miembro del grupo) o empezar (lider). */
    private void botonJugar(Poke p) {
        boolean lleno = partidas >= maxPartidas;
        if (!enGrupo) { if (!lleno && !preparando) { preparando = true; enviar("jugar", String.valueOf(p.slot), ""); } return; }
        if (grupoJugando) return;
        if (soyLider) {
            if (!lleno && !preparando && otros() - listosSinLider() <= 0) { preparando = true; enviar("grupo_empezar", String.valueOf(p.slot), ""); }
            return;
        }
        Miembro m = yo();
        enviar("grupo_listo", m != null && m.listo() ? "0" : String.valueOf(p.slot), "");
    }

    private void clicGrupo(int mx, int my) {
        if (!enGrupo) {
            if (dentro(mx, my, 30, CY + 160, 200, 30)) { enviar("grupo_crear", "", ""); return; }
            for (int i = 0; i < Math.min(5, abiertos.size()); i++) {
                int y = CY + 24 + i * 44;
                if (dentro(mx, my, 536, y + 8, 76, 24)) { enviar("grupo_unirse", abiertos.get(i).lider(), ""); return; }
            }
            return;
        }
        if (grupoJugando) return;
        if (dentro(mx, my, 480, CY + 30, 140, 28)) { enviar("grupo_salir", "", ""); return; }
        if (soyLider) for (int i = 0; i < miembros.size(); i++) {
            Miembro m = miembros.get(i);
            if (m.yo()) continue;
            int y = CY + 30 + i * 44;
            if (dentro(mx, my, 392, y + 8, 60, 24)) { enviar("grupo_expulsar", m.uuid(), ""); return; }
        }
    }
}
