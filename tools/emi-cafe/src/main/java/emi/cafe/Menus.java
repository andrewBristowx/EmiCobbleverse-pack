package emi.cafe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import emi.cafe.net.AccionPayload;
import emi.cafe.net.TextoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.ZoneId;

/**
 * El menu del cafe (la pantalla que se abre con la caja registradora, igual que la de Emi Fall y las demas): aqui estan los datos que se mandan al cliente (JSON) y lo que el servidor
 * hace con cada accion que manda el cliente. Siempre se comprueba todo: el cliente no es de fiar.
 */
final class Menus {
    private Menus() {}

    private static final java.util.Map<java.util.UUID, Long> ULTIMO_REFRESCO = new java.util.HashMap<>();

    // ------------------------------------------------------------ abrir, refrescar, cerrar

    static void principal(Cafe c, ServerPlayerEntity p) { abrir(c, p, "cafe", false); }
    static void iniciales(Cafe c, ServerPlayerEntity p) { abrir(c, p, "cafe", false); }
    /** El resumen del turno que acaba de terminar, encima del menu. */
    static void resumen(Cafe c, ServerPlayerEntity p) { abrir(c, p, "cafe", true); }

    static void abrir(Cafe c, ServerPlayerEntity p, String tab, boolean conResumen) {
        if (!ServerPlayNetworking.canSend(p, TextoPayload.ID)) {
            p.sendMessage(Msg.err("Tu juego no tiene el mod Café Michialien actualizado: actualiza el pack para ver el menú."), false);
            return;
        }
        ServerPlayNetworking.send(p, new TextoPayload("menu", json(c, p, tab, conResumen)));
    }

    static void actualizar(Cafe c, ServerPlayerEntity p) {
        if (!ServerPlayNetworking.canSend(p, TextoPayload.ID)) return;
        ServerPlayNetworking.send(p, new TextoPayload("menu_act", json(c, p, "cafe", false)));
    }

    static void cerrar(ServerPlayerEntity p) {
        if (ServerPlayNetworking.canSend(p, TextoPayload.ID)) ServerPlayNetworking.send(p, new TextoPayload("menu", ""));
    }

    // ------------------------------------------------------------ los datos

    private static String idItem(net.minecraft.item.Item i) { return CafeItems.id(i); }

    static String json(Cafe c, ServerPlayerEntity p, String tab, boolean conResumen) {
        Datos.Jugador d = c.dj(p);
        Tienda.refrescar(d, System.currentTimeMillis(), ZoneId.systemDefault());
        Progreso.refrescar(d);
        boolean duenio = c.esDuenio(p);
        Datos.Jugador dd = c.dj();                                           // lo del dueño: cocinero, platos y mejoras
        JsonObject o = new JsonObject();
        o.addProperty("tab", tab);
        o.addProperty("monedas", d.monedas);
        o.addProperty("estrellas", d.estrellas);
        o.addProperty("clientes", d.clientes);
        o.addProperty("turnos", d.turnos);
        o.addProperty("mejorTurno", d.mejorTurno);
        o.addProperty("duenio", duenio);
        ServerPlayerEntity host = c.jugador(c.duenio);
        o.addProperty("nombreDuenio", host == null ? "" : host.getGameProfile().getName());
        o.addProperty("estado", c.estado.name().toLowerCase());
        o.addProperty("inicial", dd.inicial == null ? "" : dd.inicial);
        o.addProperty("platosLimpios", c.platos);

        JsonArray ini = new JsonArray();
        for (Recetas.Inicial i : Recetas.Inicial.values()) {
            JsonObject e = new JsonObject();
            e.addProperty("id", i.id); e.addProperty("nombre", i.nombre); e.addProperty("item", idItem(i.icono));
            e.addProperty("plato", i.plato.nombre()); e.addProperty("receta", i.plato.receta);
            ini.add(e);
        }
        o.add("iniciales", ini);

        JsonArray gente = new JsonArray();
        for (ServerPlayerEntity q : c.presentes()) {
            JsonObject e = new JsonObject();
            e.addProperty("nombre", q.getGameProfile().getName()); e.addProperty("yo", q == p); e.addProperty("host", c.esDuenio(q));
            gente.add(e);
        }
        o.add("gente", gente);
        o.addProperty("lleno", c.jugadores.size() >= Cafes.MAX_JUGADORES);
        JsonArray inv = new JsonArray();
        if (duenio) for (ServerPlayerEntity q : c.server.getPlayerManager().getPlayerList()) {
            if (q == p || Cafes.de(q.getUuid()) != null || inv.size() >= 6) continue;
            if (q.getServerWorld().getRegistryKey().getValue().getNamespace().startsWith("emi_")) continue;
            JsonObject e = new JsonObject(); e.addProperty("nombre", q.getGameProfile().getName()); inv.add(e);
        }
        o.add("invitables", inv);

        // el recetario y la tienda de monedas
        JsonArray rec = new JsonArray();
        for (Recetas.Plato pl : Recetas.Plato.values()) {
            JsonObject e = new JsonObject();
            e.addProperty("id", pl.id); e.addProperty("item", idItem(pl.item)); e.addProperty("nombre", pl.nombre());
            e.addProperty("precio", pl.precio); e.addProperty("coste", pl.coste); e.addProperty("receta", pl.receta);
            e.addProperty("tengo", dd.platos.contains(pl.id));
            rec.add(e);
        }
        for (Especial es : Especial.values()) {                              // los platos de los visitantes especiales: solo se ven los que ya han enseñado
            boolean sabe = dd.especiales.contains(es.id);
            JsonObject e = new JsonObject();
            e.addProperty("id", "ev_" + es.id); e.addProperty("item", idItem(es.plato)); e.addProperty("nombre", sabe ? es.nombrePlato() : "???");
            e.addProperty("precio", es.precio); e.addProperty("coste", 0);
            e.addProperty("receta", sabe ? es.receta() : "Un Michialien especial vendrá a enseñártelo");
            e.addProperty("tengo", sabe); e.addProperty("especial", true); e.addProperty("michi", sabe ? es.michi : "???");
            rec.add(e);
        }
        o.add("platos", rec);
        JsonArray mej = new JsonArray();
        for (Recetas.Mejora m : Recetas.Mejora.values()) {
            int nv = dd.nivel(m.id);
            JsonObject e = new JsonObject();
            e.addProperty("id", m.id); e.addProperty("item", idItem(m.icono)); e.addProperty("nombre", m.nombre); e.addProperty("texto", m.descripcion);
            e.addProperty("nivel", nv); e.addProperty("max", m.max()); e.addProperty("precio", nv >= m.max() ? 0 : m.precios[nv]);
            mej.add(e);
        }
        o.add("mejoras", mej);

        // la tienda de estrellas
        JsonObject ti = new JsonObject();
        ti.addProperty("limite", Tienda.LIMITE_DIA);
        JsonArray arts = new JsonArray();
        for (Tienda.Articulo a : Tienda.ARTICULOS) {
            JsonObject e = new JsonObject();
            e.addProperty("id", a.id()); e.addProperty("item", a.item()); e.addProperty("nombre", a.nombre()); e.addProperty("texto", a.texto());
            e.addProperty("precio", a.precio()); e.addProperty("grupo", a.grupo()); e.addProperty("hoy", Tienda.compradosHoy(d, a.id()));
            e.addProperty("unico", a.unico()); e.addProperty("comprado", a.unico() && Tienda.yaLoTiene(d, a.id()));
            arts.add(e);
        }
        ti.add("articulos", arts);
        o.add("tienda", ti);

        o.add("dia", misiones(d, Progreso.deHoy(d)));
        o.add("semana", misiones(d, Progreso.deSemana(d)));
        JsonArray logros = new JsonArray();
        for (Progreso.Logro l : Progreso.LOGROS) {
            int nivel = d.logros.getOrDefault(l.id(), 0);
            boolean fin = nivel >= l.metas().length;
            JsonObject e = new JsonObject();
            e.addProperty("nombre", l.nombre()); e.addProperty("texto", l.descripcion()); e.addProperty("nivel", nivel); e.addProperty("max", l.metas().length);
            e.addProperty("valor", l.valor().applyAsLong(d)); e.addProperty("meta", fin ? l.metas()[l.metas().length - 1] : l.metas()[nivel]); e.addProperty("premio", fin ? 0 : l.premios()[nivel]);
            logros.add(e);
        }
        o.add("logros", logros);

        if (conResumen) {
            JsonObject r = new JsonObject();
            r.addProperty("ganado", c.ganado); r.addProperty("servidos", c.servidos); r.addProperty("propinas", c.propinas); r.addProperty("enfadados", c.enfadados);
            r.addProperty("tuyo", c.ganadoPor.getOrDefault(p.getUuid(), 0));
            r.addProperty("estrellasTurno", c.estrellasTurno.getOrDefault(p.getUuid(), 0));
            o.add("resumen", r);
        }
        return o.toString();
    }

    private static JsonArray misiones(Datos.Jugador d, java.util.List<Progreso.Mision> l) {
        JsonArray a = new JsonArray();
        for (Progreso.Mision m : l) {
            JsonObject e = new JsonObject();
            e.addProperty("texto", m.texto()); e.addProperty("prog", d.misProg.getOrDefault(m.id(), 0)); e.addProperty("objetivo", m.objetivo());
            e.addProperty("premio", m.premio()); e.addProperty("hecha", d.misHechas.contains(m.id()));
            a.add(e);
        }
        return a;
    }

    // ------------------------------------------------------------ las acciones del cliente

    static void recibir(ServerPlayerEntity p, AccionPayload a) {
        try {
            Cafe c = Cafes.de(p.getUuid());
            if (c == null || EmiCafe.datos == null) return;
            boolean duenio = c.esDuenio(p);
            switch (a.accion()) {
                case "refrescar" -> {
                    long ahora = System.currentTimeMillis();
                    Long antes = ULTIMO_REFRESCO.put(p.getUuid(), ahora);
                    if (antes != null && ahora - antes < 400) return;
                    actualizar(c, p);
                }
                case "menu" -> abrir(c, p, a.a().isEmpty() ? "cafe" : a.a(), false);
                case "inicial" -> { if (duenio) elegirInicial(c, p, Recetas.Inicial.de(a.a())); actualizar(c, p); }
                case "abrir" -> {
                    String err = duenio ? c.abrirTurno() : "Solo el dueño del café abre el turno.";
                    if (err != null) { p.sendMessage(Msg.err(err), false); actualizar(c, p); } else cerrar(p);
                }
                case "cerrarTurno" -> { if (duenio) c.cerrarTurno(); cerrar(p); }
                case "salir" -> { cerrar(p); Cafes.salir(p); }
                case "invitar" -> {
                    ServerPlayerEntity t = c.server.getPlayerManager().getPlayer(a.a());
                    String err = t == null ? "Ese jugador ya no está conectado." : Cafes.invitar(c, p, t);
                    if (err != null) p.sendMessage(Msg.err(err), false);
                    actualizar(c, p);
                }
                case "comprarPlato" -> { comprarPlato(c, p, Recetas.Plato.de(a.a())); actualizar(c, p); }
                case "comprarMejora" -> { comprarMejora(c, p, Recetas.Mejora.de(a.a())); actualizar(c, p); }
                case "tienda_comprar" -> {
                    Datos.Jugador d = c.dj(p);
                    String err = Tienda.comprar(d, a.a(), System.currentTimeMillis(), ZoneId.systemDefault(), art -> TiendaCorreo.enviar(c.server, p.getUuid(), art));
                    if (err != null) { p.sendMessage(Msg.err(err), false); Msg.sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); }
                    else {
                        EmiCafe.datos.guardar(p.getUuid());
                        Tienda.Articulo art = Tienda.articulo(a.a());
                        p.sendMessage(Msg.say("Comprado: " + art.nombre() + " (" + art.precio() + " estrellas). Lo tienes en /correo."), false);
                        Msg.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    }
                    actualizar(c, p);
                }
                default -> { }
            }
        } catch (Throwable t) {
            EmiCafe.LOG.error("Error en una accion del menu del cafe", t);
        }
    }

    private static void elegirInicial(Cafe c, ServerPlayerEntity p, Recetas.Inicial ini) {
        if (ini == null) return;
        Datos.Jugador d = c.dj(p);
        if (d.inicial != null) return;
        d.inicial = ini.id;
        if (!d.platos.contains(ini.plato.id)) d.platos.add(ini.plato.id);
        EmiCafe.datos.guardar(p.getUuid());
        c.reconstruir();
        Msg.title(p, Text.literal("¡" + ini.nombre + "!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Tu cocinero te enseña: " + ini.plato.nombre()).formatted(Formatting.YELLOW), 5, 50, 10);
        p.sendMessage(Msg.say(ini.nombre + " te enseñó a hacer " + ini.plato.nombre() + ": " + ini.plato.receta + ". Clic derecho en la caja registradora para abrir el café."), false);
    }

    private static void comprarPlato(Cafe c, ServerPlayerEntity p, Recetas.Plato pl) {
        if (pl == null || !c.esDuenio(p)) return;
        Datos.Jugador d = c.dj(p);
        if (c.estado != Cafe.Estado.PREPARANDO) { p.sendMessage(Msg.err("Solo se compra con el café cerrado."), false); return; }
        if (d.platos.contains(pl.id)) return;
        if (d.monedas < pl.coste) { p.sendMessage(Msg.err("Te faltan " + (pl.coste - d.monedas) + " monedas."), false); Msg.sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); return; }
        d.monedas -= pl.coste;
        d.platos.add(pl.id);
        Progreso.revisarLogros(c, p, d);
        EmiCafe.datos.guardar(p.getUuid());
        c.reconstruir();
        Msg.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        p.sendMessage(Msg.say("Aprendiste " + pl.nombre() + ": " + pl.receta + ". Sus estaciones ya están en la cocina."), false);
    }

    private static void comprarMejora(Cafe c, ServerPlayerEntity p, Recetas.Mejora me) {
        if (me == null || !c.esDuenio(p)) return;
        Datos.Jugador d = c.dj(p);
        int nv = d.nivel(me.id);
        if (c.estado != Cafe.Estado.PREPARANDO) { p.sendMessage(Msg.err("Solo se compra con el café cerrado."), false); return; }
        if (nv >= me.max()) return;
        int precio = me.precios[nv];
        if (d.monedas < precio) { p.sendMessage(Msg.err("Te faltan " + (precio - d.monedas) + " monedas."), false); Msg.sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); return; }
        d.monedas -= precio;
        d.mejoras.put(me.id, nv + 1);
        Progreso.revisarLogros(c, p, d);
        EmiCafe.datos.guardar(p.getUuid());
        c.reconstruir();
        Msg.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        p.sendMessage(Msg.say(me.nombre + ": nivel " + (nv + 1) + "."), false);
    }
}
