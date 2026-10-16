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
    record Resultado(String motivo, int segundos, int derrotados, int nivel, int monedas, int puesto, boolean jefe, boolean cuenta) {}

    static void enviar(ServerPlayerEntity p, String tab, Resultado res) {
        if (!ServerPlayNetworking.canSend(p, TextoPayload.ID)) {
            p.sendMessage(Msg.err("Tu juego no tiene el mod Emi Survivors: actualiza el pack para ver el menú."), false);
            return;
        }
        ServerPlayNetworking.send(p, new TextoPayload("menu", json(p, tab, res)));
    }

    /** Cierra la pantalla del menu en el cliente (al salir del mundo del minijuego). */
    static void cerrar(ServerPlayerEntity p) {
        if (ServerPlayNetworking.canSend(p, TextoPayload.ID)) ServerPlayNetworking.send(p, new TextoPayload("menu", ""));
    }

    static String json(ServerPlayerEntity p, String tab, Resultado res) {
        Datos d = Partida.datos;
        Datos.Jugador j = d.jugador(p.getUuid(), p.getGameProfile().getName());
        JsonObject o = new JsonObject();
        o.addProperty("tab", tab);
        o.addProperty("monedas", j.monedas);
        o.addProperty("partidas", Partida.runs.size());
        o.addProperty("maxPartidas", Partida.MAX_PARTIDAS);
        o.addProperty("miPuesto", d.puesto(p.getUuid()));
        o.addProperty("miMejor", j.mejorTiempo);

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
            e.addProperty("ataqueIcono", Catalogo.arma(ini.arma()).icono());
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
            for (int n = 0; n < Meta.MAX; n++) costos.add(Meta.costo(n));
            e.add("costos", costos);
            cat.add(e);
        }
        o.add("catalogo", cat);

        JsonArray top = new JsonArray();
        int pos = 1;
        for (Datos.Entrada en : d.top(10)) {
            JsonObject e = new JsonObject();
            e.addProperty("pos", pos++);
            e.addProperty("nombre", en.nombre);
            e.addProperty("mio", en.uuid.equals(p.getUuid().toString()));
            e.addProperty("especie", en.especie);
            e.addProperty("segundos", en.segundos);
            e.addProperty("derrotados", en.derrotados);
            e.addProperty("nivel", en.nivel);
            e.addProperty("jefe", en.jefe);
            top.add(e);
        }
        o.add("top", top);

        if (res != null) {
            JsonObject r = new JsonObject();
            r.addProperty("motivo", res.motivo());
            r.addProperty("segundos", res.segundos());
            r.addProperty("derrotados", res.derrotados());
            r.addProperty("nivel", res.nivel());
            r.addProperty("monedas", res.monedas());
            r.addProperty("puesto", res.puesto());
            r.addProperty("jefe", res.jefe());
            r.addProperty("cuenta", res.cuenta());
            o.add("resultado", r);
        }
        return o.toString();
    }
}
