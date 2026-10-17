package emi.carreras;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emi.carreras.net.TextoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Los datos que se mandan al cliente para dibujar el menu de la sala (estado de la carrera, tu equipo, pistas, logros, tienda y tiempos), en JSON. */
final class Menu {
    private Menu() {}

    private static final Map<String, String> TEXTOS = Map.of(
            "circuito_pokeball", "Fácil · ancha · 3 vueltas",
            "estacion_michialien", "Media · luna creciente · saltos y atajo",
            "volcan_cinnabar", "Difícil · lava, lodo y atajo con turbo");
    private static final Map<String, Pista> PISTAS = new HashMap<>();

    private static Pista pista(String id) {
        if (Carrera.pista != null && Carrera.pista.id.equals(id)) return Carrera.pista;
        return PISTAS.computeIfAbsent(id, Pista::cargar);
    }

    static void abrir(ServerPlayerEntity p, String tab) { abrir(p, tab, null); }

    static void abrir(ServerPlayerEntity p, String tab, String resultadoJson) {
        if (!ServerPlayNetworking.canSend(p, TextoPayload.ID)) {
            p.sendMessage(Msg.err("Tu juego no tiene el mod Emi Carreras actualizado: actualiza el pack para ver el menú. Mientras tanto: /carrera ayuda."), false);
            return;
        }
        ServerPlayNetworking.send(p, new TextoPayload("menu", json(p, tab, resultadoJson)));
    }

    /** Manda los datos del menu SOLO para refrescarlo si ya esta abierto (el cliente ignora el mensaje si no hay menu abierto). */
    static void actualizar(ServerPlayerEntity p) {
        if (!ServerPlayNetworking.canSend(p, TextoPayload.ID)) return;
        ServerPlayNetworking.send(p, new TextoPayload("menu_act", json(p, "jugar", null)));
    }

    /** Cierra la pantalla del menu en el cliente (al salir de la sala). */
    static void cerrar(ServerPlayerEntity p) {
        if (ServerPlayNetworking.canSend(p, TextoPayload.ID)) ServerPlayNetworking.send(p, new TextoPayload("menu", ""));
    }

    /** El resultado de la carrera que acaba de terminar, en JSON (para ensenarlo en el menu al volver a la sala). {@code ticks} = -1 si no la termino. */
    static String resultado(String pista, boolean entrenamiento, int puesto, int jugadores, int ticks, boolean record, Logros.Novedades n, int saldo) {
        JsonObject r = new JsonObject();
        r.addProperty("pista", pista);
        r.addProperty("entrenamiento", entrenamiento);
        r.addProperty("puesto", puesto);
        r.addProperty("jugadores", jugadores);
        r.addProperty("tiempo", ticks < 0 ? "" : Logros.tiempo(ticks));
        r.addProperty("record", record);
        r.addProperty("estrellas", n.total());
        r.addProperty("saldo", saldo);
        JsonArray l = new JsonArray();
        for (String x : n.lineas) l.add(x);
        r.add("lineas", l);
        return r.toString();
    }

    static String json(ServerPlayerEntity p, String tab, String resultadoJson) {
        Datos d = Carrera.datos;
        Datos.Jugador j = d.jugador(p.getUuid(), p.getGameProfile().getName());
        long ahora = System.currentTimeMillis();
        JsonObject o = new JsonObject();
        o.addProperty("tab", tab);
        o.addProperty("estrellas", j.estrellas);
        o.addProperty("carreras", j.carreras);
        o.addProperty("victorias", j.victorias);
        o.addProperty("podios", j.podios);

        // los Pokemon del equipo que valen para correr (terrestres)
        JsonArray eq = new JsonArray();
        int elegido = Carrera.corr.containsKey(p.getUuid()) ? Carrera.corr.get(p.getUuid()).slot : 0;
        for (var x : Montura.candidatos(p)) {
            var pk = x.pokemon();
            JsonObject e = new JsonObject();
            e.addProperty("slot", x.slot());
            e.addProperty("valido", x.valido());
            e.addProperty("especie", pk.getSpecies().getResourceIdentifier().toString());
            e.addProperty("nombre", pk.getSpecies().getName());
            Text nick = pk.getNickname();
            e.addProperty("apodo", nick == null ? "" : nick.getString());
            e.addProperty("nivel", pk.getLevel());
            e.addProperty("tipo", pk.getPrimaryType().getName().toLowerCase(java.util.Locale.ROOT));
            e.addProperty("brillante", pk.getShiny());
            JsonArray asp = new JsonArray();
            for (String a : pk.getAspects()) asp.add(a);
            e.add("aspectos", asp);
            eq.add(e);
        }
        o.add("equipo", eq);
        o.addProperty("elegido", elegido);

        // las pistas con tu mejor tiempo
        JsonArray pistas = new JsonArray();
        for (String id : Carrera.ids()) {
            Pista pi = pista(id);
            if (pi == null) continue;
            JsonObject e = new JsonObject();
            e.addProperty("id", id); e.addProperty("nombre", pi.nombre); e.addProperty("texto", TEXTOS.getOrDefault(id, "")); e.addProperty("vueltas", Carrera.cfg.vueltas > 0 ? Carrera.cfg.vueltas : pi.vueltas);
            Integer mejor = j.mejores.get(id);
            e.addProperty("mejor", mejor == null ? "" : Logros.tiempo(mejor));
            pistas.add(e);
        }
        o.add("pistas", pistas);

        // la carrera: libre, con la cola abierta o en marcha
        JsonObject c = new JsonObject();
        String fase = switch (Carrera.fase()) { case LIBRE -> "libre"; case COLA -> "cola"; default -> "curso"; };
        c.addProperty("fase", fase);
        if (!fase.equals("libre") && Carrera.pista != null) { c.addProperty("pista", Carrera.pista.id); c.addProperty("pistaNombre", Carrera.pista.nombre); }
        c.addProperty("entrenamiento", Carrera.entrenamiento);
        c.addProperty("max", Carrera.cfg.maxPlayers);
        Corredor yo = Carrera.corr.get(p.getUuid());
        c.addProperty("apuntado", yo != null);
        c.addProperty("listo", yo != null && yo.listo);
        c.addProperty("anfitrion", Carrera.esAnfitrion(p));
        JsonArray cs = new JsonArray();
        for (Corredor x : Carrera.corr.values()) {
            JsonObject e = new JsonObject();
            e.addProperty("nombre", x.nombre); e.addProperty("listo", x.listo); e.addProperty("yo", x.id.equals(p.getUuid())); e.addProperty("host", x.id.equals(Carrera.anfitrion));
            e.addProperty("pokemon", x.pokemon == null ? "" : x.pokemon.getSpecies().getName());
            cs.add(e);
        }
        c.add("corredores", cs);
        c.addProperty("cuenta", Carrera.listosDesde < 0 ? -1 : Math.max(0, (Carrera.ESPERA_LISTOS - (Carrera.ahora() - Carrera.listosDesde) + 19) / 20));
        o.add("carrera", c);

        // logros
        JsonArray logros = new JsonArray();
        for (Logros.Familia f : Logros.FAMILIAS) {
            int hechos = Math.min(j.logros.getOrDefault(f.id(), 0), f.escalones().length);
            boolean completo = hechos >= f.escalones().length;
            long meta = f.escalones()[completo ? f.escalones().length - 1 : hechos];
            long valor = completo ? meta : Math.min(f.valor().applyAsLong(j), meta);
            JsonObject e = new JsonObject();
            e.addProperty("id", f.id()); e.addProperty("nombre", f.nombre());
            e.addProperty("texto", Logros.texto(f, meta));
            e.addProperty("hechos", hechos); e.addProperty("total", f.escalones().length);
            e.addProperty("valor", valor); e.addProperty("meta", meta);
            e.addProperty("valorTxt", Logros.formato(valor, f.unidad())); e.addProperty("metaTxt", Logros.formato(meta, f.unidad()));
            e.addProperty("premio", f.premios()[completo ? f.premios().length - 1 : hechos]);
            e.addProperty("completo", completo);
            logros.add(e);
        }
        o.add("logros", logros);

        // tienda
        Tienda.refrescar(j, ahora, java.time.ZoneId.systemDefault());
        JsonObject tienda = new JsonObject();
        tienda.addProperty("limite", Tienda.LIMITE_DIA);
        JsonArray arts = new JsonArray();
        for (Tienda.Articulo a : Tienda.ARTICULOS) {
            JsonObject e = new JsonObject();
            e.addProperty("id", a.id()); e.addProperty("item", a.item()); e.addProperty("nombre", a.nombre()); e.addProperty("texto", a.texto());
            e.addProperty("precio", a.precio()); e.addProperty("grupo", a.grupo()); e.addProperty("hoy", Tienda.compradosHoy(j, a.id()));
            e.addProperty("unico", a.unico()); e.addProperty("comprado", a.unico() && Tienda.yaLoTiene(j, a.id()));
            arts.add(e);
        }
        tienda.add("articulos", arts);
        o.add("tienda", tienda);

        // los mejores tiempos de cada pista
        JsonObject tops = new JsonObject();
        for (String id : Carrera.ids()) {
            JsonArray t = new JsonArray();
            int pos = 1;
            for (Datos.Marca m : d.top(id)) {
                JsonObject e = new JsonObject();
                e.addProperty("pos", pos++); e.addProperty("nombre", m.nombre); e.addProperty("tiempo", Logros.tiempo(m.ticks)); e.addProperty("especie", m.especie);
                e.addProperty("mio", m.uuid.equals(p.getUuid().toString()));
                t.add(e);
            }
            tops.add(id, t);
        }
        o.add("tops", tops);

        if (resultadoJson != null && !resultadoJson.isEmpty()) o.add("resultado", JsonParser.parseString(resultadoJson));
        return o.toString();
    }
}
