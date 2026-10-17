package com.emipokemon.raid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_2902;
import net.minecraft.class_3218;
import net.minecraft.class_3222;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sitio de los eventos del mundo de Emipokemon (jefes Mega, Gigantamax y Tera, encuentro shiny y entrenador legendario) y arreglos de rendimiento (parche PatchEventSites).
 *
 * Antes cada evento buscaba su lugar al azar en un radio de hasta 5000 bloques: cada intento pedia la altura del terreno de un punto lejano, y eso GENERA ese trozo de mapa en el
 * hilo principal (hasta 80 intentos seguidos: tirones de 2 a 5 segundos cada vez que salia un evento, incluso sin nadie conectado). Ahora:
 *  - sin jugadores conectados no se lanza ningun evento;
 *  - si hay un sitio marcado con /emipokemon sitio marcar, el evento sale ahi (o a pocos bloques, dentro de los trozos de mapa de al lado);
 *  - si no, la busqueda al azar solo mira trozos de mapa ya cargados, y la de «cualquier parte» pasa a ser en torno a un jugador conectado (300 bloques);
 *  - las especies de Emipokemon del catalogo (ids sin ':' ni '_', p. ej. «emipokemonfloragatoemi») se convierten en su id real antes de crear el jefe Tera.
 * Todo es tolerante a errores: si algo falla, se comporta como antes (menos el aviso en el registro).
 */
public final class EventSites {
    private EventSites() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Nombres de evento que entiende el comando, y el nombre de la clase de Emipokemon que lo lanza. */
    private static final Map<String, String> CLASES = new LinkedHashMap<>();
    static {
        CLASES.put("mega", "MegaBossService");
        CLASES.put("gigantamax", "GigantamaxBossService");
        CLASES.put("tera", "TeraBossService");
        CLASES.put("shiny", "ShinyEncounterService");
        CLASES.put("legendario", "LegendaryTrainerService");
    }
    private static final int RADIO_MAX = 16;

    private static final class Sitio { String dim = "minecraft:overworld"; double x, y, z; int radio = 6; }

    private static final Map<String, Sitio> sitios = new LinkedHashMap<>();
    private static boolean iniciado;

    // estado de la ultima llamada a prepare() (todo ocurre en el hilo del servidor)
    private static boolean abortar;
    private static double ncx, ncz, nr;
    private static long ultimoAviso;

    // ------------------------------------------------------------------ arranque, archivo y comandos

    /** Lo llama el inicializador de TeraBossService: carga el archivo y registra el comando (una sola vez). */
    public static synchronized void init() {
        if (iniciado) return;
        iniciado = true;
        try { leer(); } catch (Throwable t) { System.err.println("[eventos] no se pudo leer event_sites.json: " + t); }
        try {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registrar(dispatcher));
        } catch (Throwable t) { System.err.println("[eventos] no se pudo registrar el comando /emipokemon sitio: " + t); }
    }

    private static Path archivo() { return FabricLoader.getInstance().getConfigDir().resolve("emipokemon").resolve("event_sites.json"); }

    private static void leer() throws Exception {
        Path f = archivo();
        sitios.clear();
        if (!Files.exists(f)) return;
        JsonObject o = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
        if (o == null) return;
        for (var e : o.entrySet()) {
            try { sitios.put(e.getKey(), GSON.fromJson(e.getValue(), Sitio.class)); } catch (Throwable t) { /* una entrada rota no tumba las demas */ }
        }
    }

    private static void guardar() throws Exception {
        Path f = archivo();
        Files.createDirectories(f.getParent());
        Path tmp = f.resolveSibling("event_sites.json.tmp");
        Files.writeString(tmp, GSON.toJson(sitios), StandardCharsets.UTF_8);
        Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static void registrar(CommandDispatcher<class_2168> d) {
        var marcar = class_2170.method_9247("marcar").requires(s -> s.method_9259(2))
                .executes(c -> marcar(c.getSource(), "todos", 6))
                .then(class_2170.method_9244("evento", StringArgumentType.word()).executes(c -> marcar(c.getSource(), StringArgumentType.getString(c, "evento"), 6))
                        .then(class_2170.method_9244("radio", IntegerArgumentType.integer(0, RADIO_MAX))
                                .executes(c -> marcar(c.getSource(), StringArgumentType.getString(c, "evento"), IntegerArgumentType.getInteger(c, "radio")))));
        var quitar = class_2170.method_9247("quitar").requires(s -> s.method_9259(2))
                .executes(c -> quitar(c.getSource(), "todos"))
                .then(class_2170.method_9244("evento", StringArgumentType.word()).executes(c -> quitar(c.getSource(), StringArgumentType.getString(c, "evento"))));
        var ver = class_2170.method_9247("ver").requires(s -> s.method_9259(2)).executes(c -> ver(c.getSource()));
        var especie = class_2170.method_9247("_especie").requires(s -> s.method_9259(2))      // solo pruebas: ¿en que id real se convierte uno del catalogo?
                .then(class_2170.method_9244("id", StringArgumentType.word()).executes(c -> {
                    String id = StringArgumentType.getString(c, "id");
                    decir(c.getSource(), "\u00a7e" + id + " -> " + fixSpecies(id));
                    return 1;
                }));
        d.register(class_2170.method_9247("emipokemon").then(class_2170.method_9247("sitio").requires(s -> s.method_9259(2)).then(marcar).then(quitar).then(ver).then(especie)));
    }

    private static void decir(class_2168 s, String t) { s.method_9226(() -> class_2561.method_43470(t), false); }

    private static String clave(String evento) {
        String e = evento.toLowerCase(Locale.ROOT);
        if (e.equals("todos") || CLASES.containsKey(e)) return e;
        if (e.equals("legendary") || e.equals("entrenador")) return "legendario";
        return null;
    }

    private static int marcar(class_2168 s, String evento, int radio) {
        try {
            String k = clave(evento);
            if (k == null) { decir(s, "§cEvento desconocido. Usa: todos, mega, gigantamax, tera, shiny o legendario."); return 0; }
            class_3222 p = s.method_44023();
            if (p == null) { decir(s, "§cEjecuta este comando como jugador, en el sitio que quieres marcar."); return 0; }
            class_3218 w = p.method_51469();
            if (!w.method_27983().method_29177().toString().equals("minecraft:overworld")) {
                decir(s, "§cLos eventos salen siempre en el Overworld: marca el sitio ahi.");
                return 0;
            }
            Sitio st = new Sitio();
            st.dim = "minecraft:overworld"; st.x = p.method_23317(); st.y = p.method_23318(); st.z = p.method_23321(); st.radio = radio;
            sitios.put(k, st);
            guardar();
            decir(s, "§aSitio de eventos (" + nombre(k) + ") marcado en " + (int) Math.floor(st.x) + " " + (int) Math.floor(st.y) + " " + (int) Math.floor(st.z)
                    + " §7(salen a menos de " + radio + " bloques; sin jugadores conectados no sale ninguno)");
            return 1;
        } catch (Throwable t) {
            decir(s, "§cNo se pudo guardar el sitio: " + t);
            return 0;
        }
    }

    private static int quitar(class_2168 s, String evento) {
        try {
            String k = clave(evento);
            if (k == null) { decir(s, "§cEvento desconocido. Usa: todos, mega, gigantamax, tera, shiny o legendario."); return 0; }
            if (sitios.remove(k) == null) { decir(s, "§eNo hay sitio marcado para " + nombre(k) + "."); return 0; }
            guardar();
            decir(s, "§aSitio de " + nombre(k) + " quitado: " + (k.equals("todos") ? "los eventos sin sitio propio vuelven a salir cerca de un jugador." : "vuelve a usar el sitio de «todos» o, si no hay, uno cerca de un jugador."));
            return 1;
        } catch (Throwable t) {
            decir(s, "§cNo se pudo guardar: " + t);
            return 0;
        }
    }

    private static int ver(class_2168 s) {
        if (sitios.isEmpty()) { decir(s, "§eNo hay ningun sitio marcado: los eventos salen cerca de un jugador conectado."); return 1; }
        for (var e : sitios.entrySet()) {
            Sitio st = e.getValue();
            decir(s, "§6" + nombre(e.getKey()) + "§r: " + (int) Math.floor(st.x) + " " + (int) Math.floor(st.y) + " " + (int) Math.floor(st.z) + " (radio " + st.radio + ")");
        }
        return 1;
    }

    private static String nombre(String k) { return k.equals("todos") ? "todos los eventos" : k; }

    // ------------------------------------------------------------------ busqueda de lugar (la llaman MegaBossService.searchAround y LegendaryTrainerService.searchAround)

    /** Evento que esta buscando sitio: la clase que llamo a searchAround. */
    private static String eventoActual() {
        try {
            String clase = StackWalker.getInstance().walk(fr -> {
                boolean visto = false;
                for (var f : (Iterable<StackWalker.StackFrame>) fr.toList()) {
                    if (visto) return f.getClassName();
                    if (f.getMethodName().equals("searchAround")) visto = true;
                }
                return "";
            });
            for (var e : CLASES.entrySet()) if (clase.endsWith("." + e.getValue())) return e.getKey();
        } catch (Throwable t) { /* nada */ }
        return "";
    }

    /**
     * Primer paso de searchAround. Devuelve el sitio marcado (y ya esta) o null para seguir con la busqueda normal; en ese caso {@link #abortar()} dice si no hay que buscar
     * (nadie conectado) y {@link #ncx()}, {@link #ncz()} y {@link #nr()} son el centro y el radio con los que seguir.
     */
    public static class_2338 prepare(class_3218 w, double cx, double cz, double r, int intentos) {
        abortar = false; ncx = cx; ncz = cz; nr = r;
        try {
            List<class_3222> jugadores = w.method_18456();
            if (jugadores.isEmpty()) {
                abortar = true;
                long t = System.currentTimeMillis();
                if (t - ultimoAviso > 600_000L) { ultimoAviso = t; System.out.println("[eventos] sin jugadores conectados: no se lanza ningun evento del mundo"); }
                return null;
            }
            String ev = eventoActual();
            Sitio st = sitios.get(ev);
            if (st == null) st = sitios.get("todos");
            if (st != null && st.dim.equals(w.method_27983().method_29177().toString())) {
                class_2338 pos = enSitio(w, st);
                if (pos != null) return pos;
            }
            if (r > 400.0) {                                    // «cualquier parte del mundo»: ahora, en torno a un jugador (los trozos de mapa de alli ya estan cargados)
                class_3222 p = jugadores.get(ThreadLocalRandom.current().nextInt(jugadores.size()));
                ncx = p.method_23317(); ncz = p.method_23321(); nr = 300.0;
            }
        } catch (Throwable t) {
            System.err.println("[eventos] error al elegir el lugar (se usa la busqueda normal): " + t);
        }
        return null;
    }

    private static class_2338 enSitio(class_3218 w, Sitio st) {
        int sx = (int) Math.floor(st.x), sz = (int) Math.floor(st.z), sy = (int) Math.floor(st.y);
        if (st.radio > 0) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 0; i < 12; i++) {
                double a = rnd.nextDouble() * Math.PI * 2, dist = rnd.nextDouble() * st.radio;
                int x = (int) Math.round(st.x + Math.cos(a) * dist), z = (int) Math.round(st.z + Math.sin(a) * dist);
                // solo en los trozos de al lado del sitio (el jugador que lo marco los tenia generados) o en los ya cargados: nunca se genera mapa nuevo
                boolean cerca = Math.abs((x >> 4) - (sx >> 4)) <= 1 && Math.abs((z >> 4) - (sz >> 4)) <= 1;
                if (!cerca && !cargado(w, x, z)) continue;
                int y = w.method_8624(class_2902.class_2903.field_13197, x, z);
                if (Math.abs(y - sy) > 8) continue;           // un tejado, un risco...: ahi no
                return new class_2338(x, y, z);
            }
        }
        return new class_2338(sx, sy, sz);
    }

    public static boolean abortar() { return abortar; }
    public static double ncx() { return ncx; }
    public static double ncz() { return ncz; }
    public static double nr() { return nr; }

    /** ¿Esta ya cargado el trozo de mapa de ese bloque? (preguntar la altura de uno que no lo esta lo GENERA en el hilo principal) */
    public static boolean cargado(class_3218 w, int x, int z) {
        try { return w.method_14178().method_21730(x >> 4, z >> 4) != null; } catch (Throwable t) { return false; }
    }

    // ------------------------------------------------------------------ especies del catalogo

    private static Map<String, String> especies;

    /** El catalogo de Emipokemon guarda las especies de Emipokemon como «emipokemonfloragatoemi» (sin ':' ni '_') y Cobblemon no las reconoce: se devuelve el id real. */
    public static String fixSpecies(String id) {
        if (id == null) return null;
        try {
            if (com.cobblemon.mod.common.api.pokemon.PokemonSpecies.INSTANCE.getByName(id) != null) return id;
            if (especies == null) {
                Map<String, String> m = new java.util.HashMap<>();
                for (var sp : com.cobblemon.mod.common.api.pokemon.PokemonSpecies.INSTANCE.getSpecies()) {
                    String real = sp.getResourceIdentifier().toString();
                    m.put(real.replaceAll("[^a-z0-9]", ""), real);
                }
                especies = m;
            }
            String real = especies.get(id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""));
            return real != null ? real : id;
        } catch (Throwable t) {
            return id;
        }
    }
}
