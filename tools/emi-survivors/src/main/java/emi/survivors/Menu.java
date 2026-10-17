package emi.survivors;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import emi.survivors.net.TextoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;

/** Los datos que se mandan al cliente para dibujar el menu (equipo, mejoras, top, resultado de la ultima partida), en JSON. */
final class Menu {
    private Menu() {}

    /** El resultado de la partida que acaba de terminar (para enseñarlo en el menu). {@code puesto} = 0 si no entra al top. */
    record Resultado(String motivo, int segundos, int derrotados, int nivel, int monedas, int puesto, int jefes, boolean cuenta, String equipo, java.util.List<String> retos, int retosCoronas) {}

    static void enviar(ServerPlayerEntity p, String tab, Resultado res) {
        if (!ServerPlayNetworking.canSend(p, TextoPayload.ID)) {
            p.sendMessage(Msg.err("Tu juego no tiene el mod Emi Survivors: actualiza el pack para ver el menú."), false);
            return;
        }
        ServerPlayNetworking.send(p, new TextoPayload("menu", json(p, tab, res)));
    }

    /** Manda los datos del menu SOLO para refrescarlo si ya esta abierto (el cliente ignora el mensaje si no hay menu abierto): asi se actualiza el grupo sin abrirle la pantalla a nadie. */
    static void actualizar(ServerPlayerEntity p) {
        if (!ServerPlayNetworking.canSend(p, TextoPayload.ID)) return;
        ServerPlayNetworking.send(p, new TextoPayload("menu_act", json(p, "jugar", null)));
    }

    /** Cierra la pantalla del menu en el cliente (al salir del mundo del minijuego). */
    static void cerrar(ServerPlayerEntity p) {
        if (ServerPlayNetworking.canSend(p, TextoPayload.ID)) ServerPlayNetworking.send(p, new TextoPayload("menu", ""));
    }

    private static JsonArray topJson(java.util.List<Datos.Entrada> lista, ServerPlayerEntity p) {
        JsonArray top = new JsonArray();
        int pos = 1;
        String yo = p.getUuid().toString();
        for (Datos.Entrada en : lista) {
            boolean mio = en.uuid.equals(yo);
            for (Datos.MiembroTop m : en.miembros) if (m.uuid.equals(yo)) mio = true;
            JsonObject e = new JsonObject();
            e.addProperty("pos", pos++);
            e.addProperty("nombre", en.nombre);
            e.addProperty("mio", mio);
            e.addProperty("especie", en.especie);
            e.addProperty("segundos", en.segundos);
            e.addProperty("derrotados", en.derrotados);
            e.addProperty("nivel", en.nivel);
            e.addProperty("jefe", en.jefe);
            e.addProperty("sentencia", en.sentencia);
            e.addProperty("jefes", Math.max(en.jefes, en.jefe ? 1 : 0));
            top.add(e);
        }
        return top;
    }

    private static JsonArray misionesJson(java.util.List<Retos.Prog> lista) {
        JsonArray a = new JsonArray();
        for (Retos.Prog pr : lista) {
            Retos.Def d = Retos.def(pr.id);
            if (d == null) continue;
            JsonObject e = new JsonObject();
            e.addProperty("texto", d.texto()); e.addProperty("prog", pr.prog); e.addProperty("objetivo", d.objetivo());
            e.addProperty("premio", d.premio()); e.addProperty("hecha", pr.hecha);
            e.addProperty("segundos", d.metrica().equals("segundos"));
            a.add(e);
        }
        return a;
    }

    static String json(ServerPlayerEntity p, String tab, Resultado res) {
        Datos d = Partida.datos;
        Datos.Jugador j = d.jugador(p.getUuid(), p.getGameProfile().getName());
        JsonObject o = new JsonObject();
        o.addProperty("tab", tab);
        o.addProperty("monedas", j.monedas);
        o.addProperty("coronas", j.coronas);
        o.addProperty("partidas", Partida.runs.size());
        o.addProperty("maxPartidas", Partida.MAX_PARTIDAS);
        o.addProperty("miPuesto", d.puesto(p.getUuid()));
        o.addProperty("miMejor", j.mejorTiempo);
        JsonArray bio = new JsonArray();
        for (Biomas.Bioma b : Biomas.TODOS) {
            JsonObject e = new JsonObject();
            e.addProperty("id", b.id()); e.addProperty("nombre", b.nombre()); e.addProperty("texto", b.texto()); e.addProperty("color", b.color());
            JsonArray tp = new JsonArray();
            for (String t : b.tipos()) tp.add(t);
            e.add("tipos", tp);
            bio.add(e);
        }
        o.add("biomas", bio);

        JsonArray eq = new JsonArray();
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(p);
        for (int i = 0; i < 6; i++) {
            Pokemon pk = party.get(i);
            if (pk == null) continue;
            JsonObject e = new JsonObject();
            String id = pk.getUuid().toString();
            String tipo = pk.getPrimaryType().getName().toLowerCase(Locale.ROOT);
            Catalogo.Inicial ini = Catalogo.inicial(tipo);
            e.addProperty("slot", i + 1);
            e.addProperty("id", id);
            e.addProperty("especie", pk.getSpecies().getResourceIdentifier().toString());
            e.addProperty("nombre", pk.getSpecies().getName());
            Text nick = pk.getNickname();
            e.addProperty("apodo", nick == null ? "" : nick.getString());
            e.addProperty("nivel", pk.getLevel());
            e.addProperty("tipo", tipo);
            e.addProperty("brillante", pk.getShiny());
            JsonArray asp = new JsonArray();
            for (String a : pk.getAspects()) asp.add(a);
            e.add("aspectos", asp);
            e.addProperty("ataque", ini.nombre());
            e.addProperty("ataqueTexto", Catalogo.arma(ini.arma()).textos()[0]);
            e.addProperty("ataqueIcono", Catalogo.iconoInicial(tipo));
            Datos.PokeMeta m = j.pokemon.get(id);
            JsonObject mej = new JsonObject();
            if (m != null) for (var en : m.mejoras.entrySet()) mej.addProperty(en.getKey(), en.getValue());
            e.add("mejoras", mej);
            e.addProperty("mejorTiempo", m == null ? 0 : m.mejorTiempo);
            e.addProperty("partidas", m == null ? 0 : m.partidas);
            eq.add(e);
        }
        o.add("equipo", eq);

        JsonArray cat = new JsonArray();
        for (Meta.Mejora m : Meta.MEJORAS) {
            JsonObject e = new JsonObject();
            e.addProperty("id", m.id()); e.addProperty("nombre", m.nombre()); e.addProperty("texto", m.texto()); e.addProperty("icono", m.icono());
            JsonArray costos = new JsonArray();
            for (int n = 0; n < Meta.MAX; n++) costos.add(Meta.costo(m, n));
            e.add("costos", costos);
            cat.add(e);
        }
        o.add("catalogo", cat);

        o.add("top", topJson(d.top(10), p));
        o.add("topGrupos", topJson(d.topGrupos(10), p));
        // los tops por periodo (hoy, semana, mes y siempre), en solitario y de grupos
        JsonObject tops = new JsonObject();
        long ahora = System.currentTimeMillis();
        for (String modo : new String[]{Datos.SOLO, Datos.GRUPO}) {
            JsonObject por = new JsonObject();
            for (String per : Periodos.TODOS) por.add(per, topJson(d.topPeriodo(modo, per, 10, ahora, java.time.ZoneId.systemDefault()), p));
            tops.add(modo, por);
        }
        o.add("tops", tops);

        // misiones del dia y de la semana, y logros
        Retos.refrescar(j, ahora, java.time.ZoneId.systemDefault());
        JsonObject retos = new JsonObject();
        retos.add("dia", misionesJson(j.dia));
        retos.add("semana", misionesJson(j.semana));
        JsonArray logros = new JsonArray();
        for (Retos.Familia f : Retos.FAMILIAS) {
            int hechos = Math.min(j.logros.getOrDefault(f.id(), 0), f.escalones().length);
            boolean completo = hechos >= f.escalones().length;
            long meta = f.escalones()[completo ? f.escalones().length - 1 : hechos];
            JsonObject e = new JsonObject();
            e.addProperty("id", f.id()); e.addProperty("nombre", f.nombre());
            e.addProperty("texto", Retos.texto(f, meta));
            e.addProperty("hechos", hechos); e.addProperty("total", f.escalones().length);
            e.addProperty("valor", completo ? meta : Math.min(f.valor().applyAsLong(j), meta));
            e.addProperty("meta", meta);
            e.addProperty("valorTxt", Retos.formato(completo ? meta : Math.min(f.valor().applyAsLong(j), meta), f.unidad()));
            e.addProperty("metaTxt", Retos.formato(meta, f.unidad()));
            e.addProperty("premio", f.premios()[completo ? f.premios().length - 1 : hechos]);
            e.addProperty("completo", completo);
            logros.add(e);
        }
        retos.add("logros", logros);
        o.add("retos", retos);

        // tienda de recompensas (con las coronas)
        Tienda.refrescar(j, ahora, java.time.ZoneId.systemDefault());
        JsonObject tienda = new JsonObject();
        tienda.addProperty("limite", Tienda.LIMITE_DIA);
        JsonArray arts = new JsonArray();
        for (Tienda.Articulo a : Tienda.ARTICULOS) {
            JsonObject e = new JsonObject();
            e.addProperty("id", a.id()); e.addProperty("item", a.item()); e.addProperty("nombre", a.nombre()); e.addProperty("texto", a.texto());
            e.addProperty("precio", a.precio()); e.addProperty("grupo", a.grupo()); e.addProperty("hoy", Tienda.compradosHoy(j, a.id()));
            arts.add(e);
        }
        tienda.add("articulos", arts);
        o.add("tienda", tienda);

        // el grupo del jugador (si esta en uno) y los grupos a los que se puede unir
        Grupos.Grupo g = Partida.grupos.de(p.getUuid());
        JsonObject gr = new JsonObject();
        gr.addProperty("enGrupo", g != null);
        if (g != null) {
            gr.addProperty("soyLider", g.lider.equals(p.getUuid()));
            gr.addProperty("enPartida", g.enPartida);
            gr.addProperty("bioma", g.bioma);
            gr.addProperty("max", Grupos.MAX);
            JsonArray ms = new JsonArray();
            for (Grupos.Miembro m : g.miembros) {
                JsonObject e = new JsonObject();
                e.addProperty("uuid", m.id.toString()); e.addProperty("nombre", m.nombre); e.addProperty("lider", m.id.equals(g.lider)); e.addProperty("yo", m.id.equals(p.getUuid()));
                e.addProperty("listo", m.listo); e.addProperty("especie", m.especie); e.addProperty("nivel", m.nivel);
                ms.add(e);
            }
            gr.add("miembros", ms);
        }
        o.add("grupo", gr);
        JsonArray ab = new JsonArray();
        for (Grupos.Grupo x : Partida.grupos.abiertos()) {
            if (x == g) continue;
            JsonObject e = new JsonObject();
            StringBuilder nombres = new StringBuilder();
            for (Grupos.Miembro m : x.miembros) { if (nombres.length() > 0) nombres.append(", "); nombres.append(m.nombre); }
            e.addProperty("lider", x.lider.toString()); e.addProperty("nombre", x.lider().nombre); e.addProperty("n", x.miembros.size()); e.addProperty("miembros", nombres.toString());
            ab.add(e);
        }
        o.add("abiertos", ab);

        if (res != null) {
            JsonObject r = new JsonObject();
            r.addProperty("motivo", res.motivo());
            r.addProperty("segundos", res.segundos());
            r.addProperty("derrotados", res.derrotados());
            r.addProperty("nivel", res.nivel());
            r.addProperty("monedas", res.monedas());
            r.addProperty("puesto", res.puesto());
            r.addProperty("jefe", res.jefes() > 0);
            r.addProperty("jefes", res.jefes());
            r.addProperty("cuenta", res.cuenta());
            r.addProperty("equipo", res.equipo());
            r.addProperty("retosCoronas", res.retosCoronas());
            JsonArray rl = new JsonArray();
            for (String l : res.retos()) rl.add(l);
            r.add("retos", rl);
            o.add("resultado", r);
        }
        return o.toString();
    }
}
