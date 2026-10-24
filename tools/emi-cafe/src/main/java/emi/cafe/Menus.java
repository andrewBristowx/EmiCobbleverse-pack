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
    private static final java.util.Map<java.util.UUID, Decor.Vista> VISTAS = new java.util.HashMap<>();

    /** Lo que ve este jugador en la tienda DECORAR (fuente, grupo, pagina y busqueda). */
    static Decor.Vista vista(ServerPlayerEntity p) { return VISTAS.computeIfAbsent(p.getUuid(), k -> new Decor.Vista()); }

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

    /** El libro de recetas de la pared: abre su pantalla con todos los platos y sus pasos. */
    static void libro(Cafe c, ServerPlayerEntity p) {
        if (!ServerPlayNetworking.canSend(p, TextoPayload.ID)) {
            p.sendMessage(Msg.err("Tu juego no tiene el mod Café Michialien actualizado: actualiza el pack para ver el libro."), false);
            return;
        }
        Datos.Jugador dd = c.dj();
        JsonObject o = new JsonObject();
        JsonArray rec = new JsonArray();
        for (Recetas.Plato pl : Recetas.Plato.values()) {
            JsonObject e = new JsonObject();
            e.addProperty("id", pl.id); e.addProperty("item", idItem(pl.item)); e.addProperty("nombre", pl.nombre());
            e.addProperty("precio", pl.precio); e.addProperty("coste", pl.coste); e.addProperty("receta", pl.receta);
            e.addProperty("tengo", dd.platos.contains(pl.id));
            e.addProperty("nivelLocal", pl.nivelLocal());
            String blq = dd.platos.contains(pl.id) ? null : c.bloqueoPlato(pl);
            e.addProperty("bloqueo", blq == null ? "" : blq);
            JsonArray ests = new JsonArray();
            for (Estacion es : pl.estaciones) {
                JsonObject x = new JsonObject();
                x.addProperty("nombre", es.nombre); x.addProperty("item", net.minecraft.registry.Registries.BLOCK.getId(es.bloque).toString());
                x.addProperty("tiene", c.posDe(es) != null);
                ests.add(x);
            }
            e.add("estaciones", ests);
            rec.add(e);
        }
        for (Especial es : Especial.values()) {
            boolean sabe = dd.especiales.contains(es.id);
            if (!sabe) continue;                                             // los de visitantes especiales solo salen cuando ya los enseñaron
            JsonObject e = new JsonObject();
            e.addProperty("id", "ev_" + es.id); e.addProperty("item", idItem(es.plato)); e.addProperty("nombre", es.nombrePlato());
            e.addProperty("precio", es.precio); e.addProperty("coste", 0); e.addProperty("receta", es.receta());
            e.addProperty("tengo", true); e.addProperty("especial", true); e.addProperty("michi", es.michi);
            e.addProperty("nivelLocal", 1); e.addProperty("bloqueo", ""); e.add("estaciones", new JsonArray());
            rec.add(e);
        }
        o.add("platos", rec);
        o.addProperty("nivelLocal", c.nivelLocal());
        ServerPlayNetworking.send(p, new TextoPayload("libro", o.toString()));
        Msg.sound(p, SoundEvents.ITEM_BOOK_PAGE_TURN, 1f, 1f);
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
        o.addProperty("construyendo", c.construyendo);
        o.addProperty("mesasUsadas", c.mesasPos().size()); o.addProperty("mesasMax", c.numMesas());
        Decor.Vista vista = vista(p);
        vista.ponerFavoritos(dd.favoritos);
        java.util.List<Decor.Articulo> lista = vista.lista();
        JsonArray deco = new JsonArray();
        for (int i = vista.pag * Decor.POR_PAG; i < Math.min(lista.size(), (vista.pag + 1) * Decor.POR_PAG); i++) {
            Decor.Articulo a = lista.get(i);
            JsonObject e = new JsonObject();
            e.addProperty("id", a.id()); e.addProperty("item", a.item()); e.addProperty("nombre", a.nombre()); e.addProperty("cantidad", a.cantidad());
            e.addProperty("precio", a.precio()); e.addProperty("grupo", a.grupo()); e.addProperty("tienes", c.obras.stock(a.item()));
            e.addProperty("fav", dd.favoritos.contains(a.item()));
            deco.add(e);
        }
        o.add("deco", deco);
        java.util.List<Decor.Fuente> fuentes = Decor.fuentes();
        JsonArray fs = new JsonArray(), gs = new JsonArray();
        for (Decor.Fuente f : fuentes) fs.add(f.nombre());
        Decor.Fuente fAct = fuentes.get(Math.max(0, Math.min(fuentes.size() - 1, vista.fuente)));
        for (Decor.Grupo g : fAct.grupos()) gs.add(g.nombre() + " (" + g.arts().size() + ")");
        o.add("decoFuentes", fs); o.add("decoGrupos", gs);
        o.addProperty("decoFuente", vista.fuente); o.addProperty("decoGrupo", vista.grupo);
        o.addProperty("decoPag", vista.pag); o.addProperty("decoPags", vista.paginas());
        o.addProperty("decoTotal", lista.size()); o.addProperty("decoBuscar", vista.buscar); o.addProperty("decoFav", vista.verFav); o.addProperty("nFav", dd.favoritos.size());
        JsonArray baul = new JsonArray();                                      // el baul: todo lo que tiene guardado el dueño (desde 0.21.0), con filtro por fuente y busqueda (0.22.0)
        JsonArray bfs = new JsonArray();
        bfs.add("Todo");
        int bTotal = 0;
        if (duenio) {
            java.util.List<String> claves = new java.util.ArrayList<>(dd.almacen.keySet());
            claves.sort((x, y) -> Integer.compare(Obras.prioridad(x), Obras.prioridad(y)));
            java.util.List<String> fuentesBaul = new java.util.ArrayList<>();
            for (String k : claves) if (dd.almacen.get(k) > 0) { String f = fuenteDe(k); if (!fuentesBaul.contains(f)) fuentesBaul.add(f); }
            for (String f : fuentesBaul) bfs.add(f);
            vista.baulFuente = Math.max(0, Math.min(fuentesBaul.size(), vista.baulFuente));
            String filtro = vista.baulFuente == 0 ? null : fuentesBaul.get(vista.baulFuente - 1);
            String[] toks = vista.baulBuscar.isEmpty() ? new String[0] : vista.baulBuscar.toLowerCase(java.util.Locale.ROOT).split("\\s+");
            for (String k : claves) {
                int n = dd.almacen.get(k);
                if (n <= 0) continue;
                bTotal++;
                if (filtro != null && !filtro.equals(fuenteDe(k))) continue;
                boolean ok = true;
                for (String t : toks) if (!k.toLowerCase(java.util.Locale.ROOT).contains(t)) { ok = false; break; }
                if (!ok || baul.size() >= 600) continue;
                JsonObject e = new JsonObject();
                e.addProperty("item", k); e.addProperty("n", n);
                e.addProperty("fuera", dd.baul != null && dd.baul.contains(k));
                e.addProperty("mano", c.construyendo ? c.obras.enInventario(p, k) : 0);
                baul.add(e);
            }
        }
        o.add("baul", baul);
        o.add("baulFuentes", bfs);
        o.addProperty("baulFuente", vista.baulFuente); o.addProperty("baulBuscar", vista.baulBuscar); o.addProperty("baulTotal", bTotal);
        o.addProperty("deshacer", c.obras.deshacerDisponibles());
        JsonArray planos = new JsonArray();                                    // los planos guardados (0.22.0)
        if (duenio && dd.libre) {
            java.util.List<java.util.Map.Entry<String, Datos.Plano>> ps = new java.util.ArrayList<>(dd.planos.entrySet());
            ps.sort((x, y) -> {
                boolean ax = x.getKey().startsWith("auto"), ay = y.getKey().startsWith("auto");
                if (ax != ay) return ax ? 1 : -1;
                return ax ? x.getKey().compareTo(y.getKey()) : Long.compare(y.getValue().fecha, x.getValue().fecha);
            });
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm");
            for (var en : ps) {
                JsonObject e = new JsonObject();
                e.addProperty("clave", en.getKey()); e.addProperty("nombre", en.getValue().nombre);
                e.addProperty("auto", en.getKey().startsWith("auto"));
                e.addProperty("fecha", java.time.Instant.ofEpochMilli(en.getValue().fecha).atZone(ZoneId.systemDefault()).format(fmt));
                e.addProperty("bloques", en.getValue().decor.size());
                planos.add(e);
            }
        }
        o.add("planos", planos);
        o.addProperty("planosMax", Obras.MAX_PLANOS);
        o.addProperty("libre", dd.libre);
        StringBuilder guardado = new StringBuilder();                          // lo del cafe que se tiene guardado para poner (caja registradora, estaciones, cartel...)
        for (java.util.Map.Entry<String, Integer> g : dd.almacen.entrySet()) {
            if (!g.getKey().startsWith("emi_cafe:") || g.getValue() <= 0) continue;
            net.minecraft.item.Item it = net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.of(g.getKey()));
            guardado.append(guardado.length() > 0 ? ", " : "").append(it.getName().getString()).append(g.getValue() > 1 ? " x" + g.getValue() : "");
        }
        o.addProperty("guardado", guardado.toString());
        JsonArray est = new JsonArray();                                     // los materiales del local (paredes y suelos) que el dueño puede cambiar construyendo
        for (int i = 0; i < Estilo.PARTES.length && !dd.libre; i++) {
            JsonObject e = new JsonObject();
            String pint = Estilo.pintado(dd.pintura, i);
            String nom = pint == null ? Estilo.op(dd.estilo, i).nombre()
                    : "Pintado: " + net.minecraft.registry.Registries.BLOCK.get(net.minecraft.util.Identifier.of(pint.contains(":") ? pint : "minecraft:" + pint)).getName().getString();
            e.addProperty("parte", Estilo.PARTES[i]); e.addProperty("nombre", nom);
            e.addProperty("idx", Estilo.indice(dd.estilo, i)); e.addProperty("total", Estilo.total(i));
            est.add(e);
        }
        o.add("estilo", est);
        o.addProperty("nombreCafe", dd.nombreCafe == null ? "" : dd.nombreCafe);
        JsonArray pers = new JsonArray();                                    // los Michialien contratados
        if (dd.personal != null) for (int i = 0; i < dd.personal.size(); i++) {
            Datos.Empleado em = dd.personal.get(i);
            Personal.Rol rol = Personal.Rol.de(em.rol);
            if (rol == null) continue;
            JsonObject e = new JsonObject();
            e.addProperty("idx", i); e.addProperty("nombre", em.nombre); e.addProperty("rol", rol.nombre); e.addProperty("icono", rol.icono);
            e.addProperty("aspecto", Personal.ASPECTOS[Math.floorMod(em.aspecto, Personal.ASPECTOS.length)][1]);
            int nv = Personal.nivel(em);
            e.addProperty("nivel", nv);
            e.addProperty("costo", Personal.costeSubida(rol, nv));
            e.addProperty("tope", nv >= Personal.NIVEL_MAX ? "NIVEL MÁXIMO" : nv + 1 > c.nivelLocal() ? "NECESITA LOCAL NIVEL " + (nv + 1) : "");
            e.addProperty("efecto", "Anda +" + Math.round((Personal.velocidad(nv) - 1) * 100) + " % · trabaja +" + Math.round((Personal.trabajo(nv) - 1) * 100) + " %");
            pers.add(e);
        }
        o.add("personal", pers);
        o.addProperty("personalMax", 8);
        JsonArray contr = new JsonArray();                                   // a quien se puede contratar
        for (Personal.Rol rol : Personal.Rol.values()) {
            JsonObject e = new JsonObject();
            e.addProperty("rol", rol.id); e.addProperty("nombre", rol.nombre); e.addProperty("texto", rol.descripcion); e.addProperty("icono", rol.icono);
            e.addProperty("precio", rol.precio); e.addProperty("reqLocal", rol.reqLocal); e.addProperty("reqRep", rol.reqRep);
            int tienes = dd.personal == null ? 0 : Personal.cuantos(dd.personal, rol);
            e.addProperty("tienes", tienes); e.addProperty("max", rol.max);
            String blq = Personal.bloqueo(rol, tienes, c.nivelLocal());
            if (blq.isEmpty() && c.nivelRep() < rol.reqRep) blq = "Reputación nivel " + rol.reqRep;
            e.addProperty("bloqueo", blq);
            contr.add(e);
        }
        o.add("contratos", contr);
        o.addProperty("estacionesEnMano", c.estEnMano.size());
        o.addProperty("nivelLocal", c.nivelLocal()); o.addProperty("estUsadas", c.estacionesUsadas()); o.addProperty("estMax", c.maxEstaciones());
        o.addProperty("visitante", c.visitantes.contains(p.getUuid()));
        o.addProperty("visitas", dd.visitas);
        o.addProperty("megusta", dd.megusta);
        o.addProperty("yaMeGusta", dd.likesDe.contains(p.getUuid().toString()));
        o.addProperty("nVisitantes", c.visitantes.size());
        int nRep = Reputacion.nivel(dd.rep);
        o.addProperty("repValor", dd.rep); o.addProperty("repNivel", nRep); o.addProperty("repNombre", Reputacion.nombre(nRep)); o.addProperty("repSig", Reputacion.siguiente(dd.rep));
        int pts = Estilo.puntos(dd.decor, dd.estilo, dd.pintura), nEst = Estilo.nivelDe(pts);
        o.addProperty("estPuntos", pts); o.addProperty("estNivel", nEst); o.addProperty("estNombre", Estilo.NOMBRES[nEst]); o.addProperty("estSig", Estilo.siguiente(pts));
        o.addProperty("evento", c.evento == Evento.NINGUNO ? "" : c.evento.nombre + (c.platoDelDia != null ? ": " + c.platoDelDia.nombre() : ""));
        o.addProperty("eventoTexto", c.evento.descripcion);
        JsonArray cafes = new JsonArray();                                   // todos los vecinos de la ciudad (para ir a su café)
        for (Ciudad.Vecino v : Ciudad.todos()) {
            java.util.UUID uid = Ciudad.duenioDe(v.slot);
            if (uid == null || uid.equals(c.duenio) || uid.equals(p.getUuid())) continue;
            Datos.Jugador od = EmiCafe.datos.jugador(uid, null);
            Cafe otro = Cafes.deDuenio(v.nombre);
            JsonObject e = new JsonObject();
            e.addProperty("nombre", v.nombre); e.addProperty("titulo", od.nombreCafe == null ? "" : od.nombreCafe); e.addProperty("megusta", od.megusta);
            e.addProperty("estilo", Estilo.nivelDe(Estilo.puntos(od.decor, od.estilo, od.pintura))); e.addProperty("rep", Reputacion.nivel(od.rep));
            e.addProperty("abierto", otro != null && otro.estado == Cafe.Estado.ABIERTO); e.addProperty("visitas", otro != null && od.visitas); e.addProperty("gente", otro == null ? 0 : otro.visitantes.size());
            e.addProperty("dentro", otro != null);
            cafes.add(e);
        }
        o.add("cafes", cafes);
        JsonArray top = new JsonArray();
        for (Datos.Fila f : EmiCafe.datos.top(6)) {
            JsonObject e = new JsonObject();
            e.addProperty("nombre", f.nombre()); e.addProperty("megusta", f.megusta()); e.addProperty("rep", Reputacion.nivel(f.rep())); e.addProperty("estilo", Estilo.nivelDe(f.estilo()));
            top.add(e);
        }
        o.add("top", top);

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
            e.addProperty("nombre", q.getGameProfile().getName()); e.addProperty("yo", q == p); e.addProperty("host", c.esDuenio(q)); e.addProperty("visita", c.visitantes.contains(q.getUuid()));
            gente.add(e);
        }
        o.add("gente", gente);
        o.addProperty("lleno", c.trabajadores() >= Cafes.MAX_JUGADORES);
        JsonArray inv = new JsonArray();
        if (duenio) for (ServerPlayerEntity q : c.server.getPlayerManager().getPlayerList()) {
            if (q == p || inv.size() >= 6 || !Cafes.invitable(q)) continue;
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
            e.addProperty("nivelLocal", pl.nivelLocal());
            String blq = dd.platos.contains(pl.id) ? null : c.bloqueoPlato(pl);
            e.addProperty("bloqueo", blq == null ? "" : blq);
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
            String reqs = "";
            if (nv < m.max()) {
                int rl = m.reqLocal(nv), rr = m.reqRep(nv);
                if (rl > 1) reqs += " · local " + rl + (c.nivelLocal() >= rl ? " ✔" : " ✘");
                if (rr > 0) reqs += " · rep. " + rr + (nRep >= rr ? " ✔" : " ✘");
            }
            e.addProperty("id", m.id); e.addProperty("item", idItem(m.icono)); e.addProperty("nombre", m.nombre + reqs); e.addProperty("texto", m.descripcion);
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
                case "construir" -> {
                    String err = duenio ? c.entrarConstruccion(p) : "Solo el dueño construye.";
                    if (err != null) p.sendMessage(Msg.err(err), false);
                    actualizar(c, p);
                }
                case "terminarConstruir" -> { if (duenio) c.salirConstruccion(p); actualizar(c, p); }
                case "estilo" -> {                                    // a = la parte (0-3), b = "+1" o "-1"
                    if (duenio && c.construyendo && !c.dj().libre) {
                        int parte = Integer.parseInt(a.a()), delta = "-1".equals(a.b()) ? -1 : 1;
                        if (parte >= 0 && parte < Estilo.PARTES.length) {
                            Datos.Jugador dd = c.dj();
                            dd.estilo = Estilo.normalizar(dd.estilo);
                            dd.pintura = Estilo.normalizarPintura(dd.pintura); dd.pintura[parte] = null;
                            dd.estilo[parte] = ((Estilo.indice(dd.estilo, parte) + delta) % Estilo.total(parte) + Estilo.total(parte)) % Estilo.total(parte);
                            EmiCafe.datos.guardar(c.duenio);
                            c.reconstruir();
                        }
                    }
                    actualizar(c, p);
                }
                case "guia" -> { if (duenio) { c.iniciarTutorial(true); cerrar(p); } }
                case "megusta" -> { String err = Cafes.megusta(c, p); if (err != null) p.sendMessage(Msg.err(err), false); actualizar(c, p); }
                case "visitas" -> { if (duenio) { Datos.Jugador dd = c.dj(); dd.visitas = !dd.visitas; EmiCafe.datos.guardar(c.duenio); } actualizar(c, p); }
                case "visitar" -> {                                   // ir al café de un vecino: se va andando (aparece en su acera), no cambia nada de lo tuyo
                    cerrar(p);
                    String err = Cafes.irA(p, a.a());
                    if (err != null) p.sendMessage(Msg.err(err), false);
                }
                case "contratar" -> {
                    Personal.Rol rol = Personal.Rol.de(a.a());
                    String err = rol == null ? "Ese puesto no existe." : c.contratar(p, rol);
                    if (err != null) p.sendMessage(Msg.err(err), false);
                    actualizar(c, p);
                }
                case "despedir" -> { String err = c.despedir(p, parseIdx(a.a())); if (err != null) p.sendMessage(Msg.err(err), false); actualizar(c, p); }
                case "mejorar_emp" -> { String err = c.mejorarEmpleado(p, parseIdx(a.a())); if (err != null) { p.sendMessage(Msg.err(err), false); Msg.sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); } actualizar(c, p); }
                case "aspecto" -> { String err = c.cambiarAspecto(p, parseIdx(a.a())); if (err != null) p.sendMessage(Msg.err(err), false); actualizar(c, p); }
                case "libro_mostrar" -> { c.mostrarReceta(p, Recetas.Plato.de(a.a())); }
                case "comprarDeco" -> { comprarDeco(c, p, Decor.articulo(a.a())); actualizar(c, p); }
                case "decoFav" -> { if (duenio) { Datos.Jugador dj = c.dj(); if (!dj.favoritos.remove(a.a())) { if (Decor.articulo(a.a()) != null && dj.favoritos.size() < 300) dj.favoritos.add(a.a()); } EmiCafe.datos.guardar(c.duenio); } actualizar(c, p); }
                case "baulFiltro" -> { Decor.Vista v = vista(p); v.baulFuente = Math.max(0, v.baulFuente + parseIdx(a.a())); actualizar(c, p); }
                case "baulBuscar" -> { vista(p).baulBuscar = a.a().trim(); actualizar(c, p); }
                case "deshacer" -> { String err = duenio ? c.obras.deshacer(p) : "Solo el dueño construye."; if (err != null) { p.sendMessage(Msg.err(err), false); Msg.sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); } else p.sendMessage(Msg.say("Deshecho."), false); actualizar(c, p); }
                case "planoRestaurar" -> { String err = c.obras.restaurarPlano(p, a.a()); if (err != null) { p.sendMessage(Msg.err(err), false); Msg.sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); } else { p.sendMessage(Msg.say("Plano restaurado. Lo que tenías antes quedó en una copia automática."), false); Msg.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.2f); } actualizar(c, p); }
                case "planoSobrescribir" -> { Datos.Plano pl = c.dj().planos.get(a.a().toLowerCase(java.util.Locale.ROOT)); String err = pl == null || a.a().startsWith("auto") ? "Ese plano no se puede sobrescribir." : c.obras.guardarPlano(p, pl.nombre); if (err != null) p.sendMessage(Msg.err(err), false); else p.sendMessage(Msg.say("Plano «" + pl.nombre + "» actualizado con el diseño de ahora."), false); actualizar(c, p); }
                case "planoBorrar" -> { String err = c.obras.borrarPlano(p, a.a()); if (err != null) p.sendMessage(Msg.err(err), false); actualizar(c, p); }
                case "decoMover" -> { Decor.Vista v = vista(p); v.ponerFavoritos(c.dj().favoritos); v.mover(a.a(), parseIdx(a.b())); actualizar(c, p); }
                case "decoBuscar" -> { vista(p).buscar(a.a()); actualizar(c, p); }
                case "baulSacar" -> { if (duenio) { String err = c.obras.sacarDelBaul(p, a.a()); if (err != null) p.sendMessage(Msg.err(err), false); } actualizar(c, p); }
                case "baulGuardar" -> { if (duenio) { String err = c.obras.guardarEnBaul(p, a.a()); if (err != null) p.sendMessage(Msg.err(err), false); } actualizar(c, p); }
                case "baulSacarTodo" -> { if (duenio) c.obras.sacarTodoDelBaul(p); actualizar(c, p); }
                case "baulGuardarTodo" -> { if (duenio) c.obras.guardarTodoEnBaul(p); actualizar(c, p); }
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

    /** De donde viene un objeto del baul, para el filtro: Café, Minecraft o el nombre del mod. */
    static String fuenteDe(String item) {
        int i = item.indexOf(':');
        String ns = i < 0 ? "minecraft" : item.substring(0, i);
        if (ns.equals("emi_cafe")) return "Café";
        if (ns.equals("minecraft")) return "Minecraft";
        return Decor.MODS.getOrDefault(ns, ns);
    }

    private static int parseIdx(String s) { try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; } }

    private static void elegirInicial(Cafe c, ServerPlayerEntity p, Recetas.Inicial ini) {
        if (ini == null) return;
        Datos.Jugador d = c.dj(p);
        if (d.inicial != null) return;
        d.inicial = ini.id;
        if (!d.platos.contains(ini.plato.id)) d.platos.add(ini.plato.id);
        EmiCafe.datos.guardar(p.getUuid());
        c.reconstruir();
        c.iniciarTutorial(false);
        Msg.title(p, Text.literal("¡" + ini.nombre + "!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Tu cocinero te enseña: " + ini.plato.nombre()).formatted(Formatting.YELLOW), 5, 50, 10);
        p.sendMessage(Msg.say(ini.nombre + " te enseñó a hacer " + ini.plato.nombre() + ": " + ini.plato.receta + ". Clic derecho en la caja registradora para abrir el café."), false);
    }

    private static void comprarPlato(Cafe c, ServerPlayerEntity p, Recetas.Plato pl) {
        if (pl == null || !c.esDuenio(p)) return;
        Datos.Jugador d = c.dj(p);
        if (c.estado != Cafe.Estado.PREPARANDO) { p.sendMessage(Msg.err("Solo se compra con el café cerrado."), false); return; }
        if (d.platos.contains(pl.id)) return;
        String blq = c.bloqueoPlato(pl);
        if (blq != null) { p.sendMessage(Msg.err(blq + "."), false); Msg.sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); return; }
        if (d.monedas < pl.coste) { p.sendMessage(Msg.err("Te faltan " + (pl.coste - d.monedas) + " monedas."), false); Msg.sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); return; }
        d.monedas -= pl.coste;
        d.platos.add(pl.id);
        Progreso.revisarLogros(c, p, d);
        EmiCafe.datos.guardar(p.getUuid());
        c.reconstruir();
        Msg.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        p.sendMessage(Msg.say("Aprendiste " + pl.nombre() + ": " + pl.receta + ". Sus estaciones ya están en la cocina."), false);
    }

    private static void comprarDeco(Cafe c, ServerPlayerEntity p, Decor.Articulo art) {
        String err = c.obras.comprar(p, art);
        if (err != null) { p.sendMessage(Msg.err(err), false); Msg.sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); return; }
        Msg.sound(p, SoundEvents.ENTITY_ITEM_PICKUP, 1f, 1.2f);
        String nom = art.nombre().isEmpty() ? art.objeto().getName().getString() : art.nombre();
        String resto = !c.construyendo ? " Se queda guardado en tu baúl: lo tendrás en el inventario al entrar en el modo construcción."
                : c.obras.sobrante > 0 ? " Tu inventario está lleno: " + c.obras.sobrante + " se quedan en tu baúl (pestaña BAÚL) y entran solos cuando liberes sitio." : "";
        p.sendMessage(Msg.say("Compraste " + nom + " (" + art.cantidad() + ")." + resto), false);
    }

    private static void comprarMejora(Cafe c, ServerPlayerEntity p, Recetas.Mejora me) {
        if (me == null || !c.esDuenio(p)) return;
        Datos.Jugador d = c.dj(p);
        int nv = d.nivel(me.id);
        if (nv < me.max() && c.nivelLocal() < me.reqLocal(nv)) { p.sendMessage(Msg.err("Hace falta el local de nivel " + me.reqLocal(nv) + " (ahora tienes el " + c.nivelLocal() + "): amplía el local."), false); Msg.sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); return; }
        if (nv < me.max() && Reputacion.nivel(d.rep) < me.reqRep(nv)) { p.sendMessage(Msg.err("Hace falta reputación de nivel " + me.reqRep(nv) + " (" + Reputacion.nombre(me.reqRep(nv)) + ")."), false); Msg.sound(p, SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); return; }
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
