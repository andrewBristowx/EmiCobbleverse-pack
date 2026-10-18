package emi.huida;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emi.huida.net.TextoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

/** Los datos que se mandan al cliente para dibujar el menu de la sala (estado de la huida, niveles, misiones, logros, tienda y tablas), en JSON. */
final class Menu {
    private Menu() {}

    private static final Map<String, String> TEXTOS = Map.of(
            "tyrantrum", "Ruinas de la selva · un Tyrantrum gigante · 274 michiestrellas",
            "calabaza", "Cementerio de Halloween · el Pumpkaboo · aspas, islas y calabazas rodantes",
            "cosmos", "El espacio · una horda de michialiens · el más difícil");

    static void abrir(ServerPlayerEntity p, String tab) { abrir(p, tab, null); }

    static void abrir(ServerPlayerEntity p, String tab, String resultadoJson) {
        if (!ServerPlayNetworking.canSend(p, TextoPayload.ID)) {
            p.sendMessage(Msg.err("Tu juego no tiene el mod Emi Huida actualizado: actualiza el pack para ver el menú. Mientras tanto: /huida ayuda."), false);
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

    /** El resultado de la huida que acaba de terminar, en JSON (para ensenarlo en el menu al volver a la sala). */
    static String resultado(Retos.Partida r, Retos.Novedades n, int saldo) {
        JsonObject o = new JsonObject();
        o.addProperty("nivel", titulo(r.nivel()));
        o.addProperty("escapo", r.escapo());
        o.addProperty("puntos", r.puntos());
        o.addProperty("errores", r.errores());
        o.addProperty("tiempo", Retos.tiempo(r.tiempo()));
        o.addProperty("puesto", r.puesto());
        o.addProperty("jugadores", r.jugadores());
        o.addProperty("record", r.escapo() && r.veredicto() == 1);
        o.addProperty("estrellas", n.total());
        o.addProperty("saldo", saldo);
        JsonArray l = new JsonArray();
        for (String x : n.lineas) l.add(x);
        o.add("lineas", l);
        return o.toString();
    }

    static String titulo(String nivelId) {
        Niveles.Def d = Niveles.porNombre(nivelId);
        if (d == null) return nivelId;
        String t = d.titulo();
        return t.substring(0, 1).toUpperCase() + t.substring(1);
    }

    static String json(ServerPlayerEntity p, String tab, String resultadoJson) {
        Datos datos = Huida.datos;
        Datos.Jugador j = datos.jugador(p.getUuid(), p.getGameProfile().getName());
        long ahora = System.currentTimeMillis();
        java.time.ZoneId zona = java.time.ZoneId.systemDefault();
        Retos.refrescar(j, ahora, zona);
        JsonObject o = new JsonObject();
        o.addProperty("tab", tab);
        o.addProperty("estrellas", j.estrellas);
        o.addProperty("huidas", j.huidas);
        o.addProperty("escapes", j.escapes);
        o.addProperty("victorias", j.victorias);

        // los niveles con tu mejor puntuacion
        JsonArray niveles = new JsonArray();
        for (Niveles.Def d : Niveles.TODOS) {
            JsonObject e = new JsonObject();
            e.addProperty("id", d.id()); e.addProperty("nombre", titulo(d.id())); e.addProperty("texto", TEXTOS.getOrDefault(d.id(), ""));
            Integer mejor = j.mejores.get(d.id());
            e.addProperty("mejor", mejor == null ? -1 : mejor);
            e.addProperty("escapes", j.escapesNivel.getOrDefault(d.id(), 0));
            niveles.add(e);
        }
        o.add("niveles", niveles);

        // la huida: libre, con la cola abierta o en marcha
        JsonObject c = new JsonObject();
        String fase = switch (Huida.fase()) { case LIBRE -> "libre"; case COLA -> "cola"; default -> "curso"; };
        c.addProperty("fase", fase);
        if (!fase.equals("libre")) { c.addProperty("nivel", Nivel.DEF.id()); c.addProperty("nivelNombre", titulo(Nivel.DEF.id())); }
        c.addProperty("max", Nivel.MAX_HUECOS);
        boolean apuntado = Huida.jug.containsKey(p.getUuid());
        c.addProperty("apuntado", apuntado);
        c.addProperty("anfitrion", Huida.esAnfitrion(p));
        JsonArray js = new JsonArray();
        for (Huida.J x : Huida.jug.values()) {
            JsonObject e = new JsonObject();
            e.addProperty("nombre", x.nombre); e.addProperty("yo", x.id.equals(p.getUuid())); e.addProperty("host", x.id.equals(Huida.anfitrion));
            js.add(e);
        }
        c.add("jugadores", js);
        o.add("huida", c);

        // misiones
        o.add("dia", misiones(j.dia));
        o.add("semana", misiones(j.semana));

        // logros
        JsonArray logros = new JsonArray();
        for (Retos.Familia f : Retos.FAMILIAS) {
            int hechos = Math.min(j.logros.getOrDefault(f.id(), 0), f.escalones().length);
            boolean completo = hechos >= f.escalones().length;
            long meta = f.escalones()[completo ? f.escalones().length - 1 : hechos];
            long valor = completo ? meta : Math.min(f.valor().applyAsLong(j), meta);
            JsonObject e = new JsonObject();
            e.addProperty("id", f.id()); e.addProperty("nombre", f.nombre());
            e.addProperty("texto", Retos.texto(f, meta));
            e.addProperty("hechos", hechos); e.addProperty("total", f.escalones().length);
            e.addProperty("valor", valor); e.addProperty("meta", meta);
            e.addProperty("valorTxt", Retos.formato(valor, f.unidad())); e.addProperty("metaTxt", Retos.formato(meta, f.unidad()));
            e.addProperty("premio", f.premios()[completo ? f.premios().length - 1 : hechos]);
            e.addProperty("completo", completo);
            logros.add(e);
        }
        o.add("logros", logros);

        // tienda
        Tienda.refrescar(j, ahora, zona);
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

        // las mejores puntuaciones de cada nivel
        JsonObject tops = new JsonObject();
        for (Niveles.Def d : Niveles.TODOS) {
            JsonArray t = new JsonArray();
            int pos = 1;
            for (Datos.Marca m : datos.top(d.id())) {
                JsonObject e = new JsonObject();
                e.addProperty("pos", pos++); e.addProperty("nombre", m.nombre); e.addProperty("puntos", m.puntos); e.addProperty("errores", m.errores); e.addProperty("tiempo", Retos.tiempo(m.tiempo));
                e.addProperty("mio", m.uuid.equals(p.getUuid().toString()));
                t.add(e);
            }
            tops.add(d.id(), t);
        }
        o.add("tops", tops);

        if (resultadoJson != null && !resultadoJson.isEmpty()) o.add("resultado", JsonParser.parseString(resultadoJson));
        return o.toString();
    }

    private static JsonArray misiones(java.util.List<Retos.Prog> lista) {
        JsonArray a = new JsonArray();
        for (Retos.Prog pr : lista) {
            Retos.Def d = Retos.def(pr.id);
            if (d == null) continue;
            JsonObject e = new JsonObject();
            e.addProperty("texto", d.texto()); e.addProperty("prog", pr.prog); e.addProperty("objetivo", d.objetivo()); e.addProperty("premio", d.premio()); e.addProperty("hecha", pr.hecha);
            a.add(e);
        }
        return a;
    }
}
