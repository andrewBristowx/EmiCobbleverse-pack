package emi.fall;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import emi.fall.net.TextoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/** Los datos que se mandan al cliente para dibujar el menu de la sala (torneo, modos, misiones, tienda y tablas), en JSON. */
final class Menu {
    private Menu() {}

    static void abrir(ServerPlayerEntity p, String tab) { abrir(p, tab, null); }

    static void abrir(ServerPlayerEntity p, String tab, String resultadoJson) {
        if (EmiFall.datos == null) return;
        if (!ServerPlayNetworking.canSend(p, TextoPayload.ID)) {
            p.sendMessage(Msg.err("Tu juego no tiene el mod Emi Fall actualizado: actualiza el pack para ver el menú. Mientras tanto: /fall unirse y /fall empezar."), false);
            return;
        }
        ServerPlayNetworking.send(p, new TextoPayload("menu", json(p, tab, resultadoJson)));
    }

    /** Manda los datos del menu SOLO para refrescarlo si ya esta abierto (el cliente ignora el mensaje si no hay menu abierto). */
    static void actualizar(ServerPlayerEntity p) {
        if (EmiFall.datos == null || !ServerPlayNetworking.canSend(p, TextoPayload.ID)) return;
        ServerPlayNetworking.send(p, new TextoPayload("menu_act", json(p, "jugar", null)));
    }

    /** Cierra la pantalla del menu en el cliente (al empezar el torneo o salir de la sala). */
    static void cerrar(ServerPlayerEntity p) {
        if (ServerPlayNetworking.canSend(p, TextoPayload.ID)) ServerPlayNetworking.send(p, new TextoPayload("menu", ""));
    }

    /** El resultado del torneo que acaba de terminar, para ensenarlo en el menu al volver a la sala. */
    static String resultado(Torneo q, Jug j) {
        JsonObject o = new JsonObject();
        o.addProperty("puesto", j.puestoFinal);
        o.addProperty("jugadores", q.jugs.size());
        o.addProperty("puntos", j.puntos);
        o.addProperty("sinElim", q.sinElim);
        o.addProperty("rondas", q.rondas.size());
        o.addProperty("estrellas", j.estrTorneo);
        o.addProperty("saldo", EmiFall.estrellas(j.id));
        JsonArray l = new JsonArray();
        for (String x : j.lineasTorneo) l.add(x);
        o.add("lineas", l);
        return o.toString();
    }

    private static JsonArray juegos(String preset) {
        String[] ids = preset.equals("clasicos") ? Torneo.CLASICOS : preset.equals("nuevos") ? Torneo.NUEVOS : Torneo.ORDEN;
        JsonArray a = new JsonArray();
        for (String id : ids) {
            Mini m = Minijuegos.porId(id);
            if (m == null) continue;
            JsonObject e = new JsonObject();
            e.addProperty("id", id); e.addProperty("nombre", m.nombre()); e.addProperty("texto", m.objetivo());
            a.add(e);
        }
        return a;
    }

    static String json(ServerPlayerEntity p, String tab, String resultadoJson) {
        Datos datos = EmiFall.datos;
        Datos.Jugador j = datos.jugador(p.getUuid(), p.getGameProfile().getName());
        long ahora = System.currentTimeMillis();
        java.time.ZoneId zona = java.time.ZoneId.systemDefault();
        Retos.refrescar(j, ahora, zona);
        JsonObject o = new JsonObject();
        o.addProperty("tab", tab);
        o.addProperty("estrellas", j.estrellas);
        o.addProperty("torneos", j.torneos);
        o.addProperty("ganados", j.torneosGanados);
        o.addProperty("rondasGanadas", j.rondasGanadas);

        // el torneo: libre, con la cola abierta o en marcha
        JsonObject c = new JsonObject();
        Torneo q = Torneo.actual;
        String fase = q == null ? "libre" : q.fase == Torneo.Fase.COLA ? "cola" : "curso";
        c.addProperty("fase", fase);
        c.addProperty("max", 16);
        boolean apuntado = q != null && q.jugs.containsKey(p.getUuid());
        c.addProperty("apuntado", apuntado);
        c.addProperty("anfitrion", q != null && p.getUuid().equals(q.anfitrion));
        c.addProperty("admin", Perm.admin(p.getCommandSource()));
        c.addProperty("preset", q == null ? "todos" : q.preset);
        c.addProperty("sinElim", q != null && q.sinElim);
        if (q != null && q.fase != Torneo.Fase.COLA && q.mini != null) c.addProperty("ronda", (q.idx + 1) + "/" + q.rondas.size() + " · " + q.mini.nombre());
        JsonArray js = new JsonArray();
        if (q != null) for (Jug x : q.jugs.values()) {
            JsonObject e = new JsonObject();
            e.addProperty("nombre", x.nombre); e.addProperty("yo", x.id.equals(p.getUuid())); e.addProperty("host", x.id.equals(q.anfitrion));
            js.add(e);
        }
        c.add("jugadores", js);
        o.add("torneo", c);
        JsonObject presets = new JsonObject();
        presets.add("todos", juegos("todos")); presets.add("clasicos", juegos("clasicos")); presets.add("nuevos", juegos("nuevos"));
        o.add("juegos", presets);

        // misiones
        o.add("dia", misiones(j.dia));
        o.add("semana", misiones(j.semana));

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

        // las tablas
        o.add("topGanados", tabla(datos.topGanadores(), p, true));
        o.add("topEstrellas", tabla(datos.topEstrellas(), p, false));

        if (resultadoJson != null && !resultadoJson.isEmpty()) o.add("resultado", com.google.gson.JsonParser.parseString(resultadoJson));
        return o.toString();
    }

    private static JsonArray tabla(List<Datos.Jugador> l, ServerPlayerEntity p, boolean ganados) {
        JsonArray t = new JsonArray();
        int pos = 1;
        for (Datos.Jugador m : l) {
            JsonObject e = new JsonObject();
            e.addProperty("pos", pos++); e.addProperty("nombre", m.nombre);
            e.addProperty("valor", ganados ? m.torneosGanados : m.estrellasGanadas);
            e.addProperty("extra", ganados ? m.torneos + " torneos" : m.torneosGanados + " victorias");
            e.addProperty("mio", m.nombre.equals(p.getGameProfile().getName()));
            t.add(e);
        }
        return t;
    }

    private static JsonArray misiones(List<Retos.Prog> lista) {
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
