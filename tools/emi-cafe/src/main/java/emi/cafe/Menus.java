package emi.cafe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.LinkedHashMap;
import java.util.Map;

/** Los menus concretos del cafe (principal, resumen del turno, tienda, cocineros iniciales, invitar, recetario). Se abren desde la caja registradora. */
final class Menus {
    private Menus() {}

    private static Menu.Entrada e(ItemStack st, Menu.Clic c) { return new Menu.Entrada(st, c); }

    private static void salir(Cafe c, ServerPlayerEntity p) { p.closeHandledScreen(); Cafes.salir(p); }

    /** Los botones de estrellas, misiones y logros (cada uno ve los suyos), en la fila de abajo del menu. */
    private static void botonesProgreso(Map<Integer, Menu.Entrada> m, Cafe c, Datos.Jugador d) {
        m.put(28, e(Menu.icono(CafeItems.MISION, "Misiones", Formatting.GREEN, "3 del día y 2 de la semana.", "Dan estrellas."), q -> misiones(c, q)));
        m.put(30, e(Menu.icono(CafeItems.TROFEO, "Logros", Formatting.GOLD, "Escalones que dan estrellas."), q -> logros(c, q)));
        m.put(32, e(Menu.icono(CafeItems.ESTRELLA, "Tienda de estrellas: " + d.estrellas, Formatting.YELLOW, "Tickets y objetos de Pokémon por /correo,", "y el peluche del Michialien chef."), q -> tiendaEstrellas(c, q)));
    }

    /** El menu de la caja registradora (segun quien seas y como este el cafe). */
    static void principal(Cafe c, ServerPlayerEntity p) {
        Datos.Jugador d = c.dj(p);
        Map<Integer, Menu.Entrada> m = new LinkedHashMap<>();
        m.put(4, e(Menu.icono(CafeItems.MONEDA, "Tus monedas: " + d.monedas, Formatting.GOLD, "Se ganan sirviendo a los clientes.", "Se gastan en platos y mejoras."), null));
        if (!c.esDuenio(p)) {
            m.put(13, e(Menu.icono(CafeItems.PUERTA, "Salir del café", Formatting.RED, "Vuelves a donde estabas."), q -> salir(c, q)));
            botonesProgreso(m, c, d);
            Menu.abrir(p, "Café de un amigo", 4, m);
            return;
        }
        if (d.inicial == null) { iniciales(c, p); return; }
        if (c.estado == Cafe.Estado.PREPARANDO) {
            m.put(10, e(Menu.icono(CafeItems.CAMPANA, "ABRIR EL CAFÉ", Formatting.GREEN, "Un turno de 4 minutos.", "Al acabar verás el resumen de ganancias."), q -> { q.closeHandledScreen(); String err = c.abrirTurno(); if (err != null) q.sendMessage(Msg.err(err), false); }));
            m.put(12, e(Menu.icono(CafeItems.LIBRO, "Recetario", Formatting.AQUA, "Cómo se hace cada plato que sabes."), q -> recetario(c, q)));
            m.put(14, e(Menu.icono(CafeItems.CORAZON, "Tienda: platos y mejoras", Formatting.LIGHT_PURPLE, "Gasta tus monedas en el café."), q -> tienda(c, q)));
            m.put(16, e(Menu.icono(CafeItems.INVITAR, "Invitar a un amigo", Formatting.YELLOW, "Cocinan contigo en tu café.", "Tienen que estar conectados."), q -> invitar(c, q)));
            botonesProgreso(m, c, d);
            m.put(34, e(Menu.icono(CafeItems.PUERTA, "Parar por ahora", Formatting.RED, "Salir del café."), q -> salir(c, q)));
            Menu.abrir(p, "Café Michialien", 4, m);
        } else if (c.estado == Cafe.Estado.ABIERTO) {
            m.put(11, e(Menu.icono(CafeItems.CERRADO, "Cerrar el turno ya", Formatting.RED, "Verás el resumen ahora."), q -> { q.closeHandledScreen(); c.cerrarTurno(); }));
            m.put(15, e(Menu.icono(CafeItems.CAMPANA, "Seguir sirviendo", Formatting.GREEN), q -> q.closeHandledScreen()));
            Menu.abrir(p, "Café abierto", 3, m);
        } else {
            m.put(13, e(Menu.icono(CafeItems.CERRADO, "Cerrando el local...", Formatting.YELLOW, "En un momento verás el resumen."), null));
            Menu.abrir(p, "Café Michialien", 3, m);
        }
    }

    /** El resumen de ganancias al cerrar el turno, con las opciones de comprar, seguir o parar. */
    static void resumen(Cafe c, ServerPlayerEntity p) {
        Datos.Jugador d = c.dj(p);
        Map<Integer, Menu.Entrada> m = new LinkedHashMap<>();
        int tuyo = c.ganadoPor.getOrDefault(p.getUuid(), 0);
        m.put(4, e(Menu.icono(CafeItems.MONEDA, "Ganancias del turno: " + c.ganado, Formatting.GOLD,
                "Clientes servidos: " + c.servidos, "Propinas: " + c.propinas, "Se fueron enfadados: " + c.enfadados,
                tuyo != c.ganado ? "Lo que ganaste tú: " + tuyo : "", "Tus monedas ahora: " + d.monedas), null));
        int est = c.estrellasTurno.getOrDefault(p.getUuid(), 0);
        m.put(3, e(Menu.icono(CafeItems.ESTRELLA, "Estrellas ganadas: " + est, Formatting.YELLOW, "Ahora tienes " + d.estrellas + ".", "Se gastan en la tienda de estrellas."), q -> tiendaEstrellas(c, q)));
        if (c.esDuenio(p)) {
            m.put(11, e(Menu.icono(CafeItems.CORAZON, "Comprar: platos y mejoras", Formatting.LIGHT_PURPLE, "Gasta tus monedas en el café."), q -> tienda(c, q)));
            m.put(13, e(Menu.icono(CafeItems.CAMPANA, "Otro turno", Formatting.GREEN, "Abrir otra vez, 4 minutos."), q -> { q.closeHandledScreen(); String err = c.abrirTurno(); if (err != null) q.sendMessage(Msg.err(err), false); }));
        }
        m.put(c.esDuenio(p) ? 15 : 13, e(Menu.icono(CafeItems.PUERTA, "Parar por ahora", Formatting.RED, "Salir del café."), q -> salir(c, q)));
        Menu.abrir(p, "Resumen del turno", 3, m);
    }

    /** Elegir el Pokemon cocinero con el que se empieza: te enseña tu primer plato. */
    static void iniciales(Cafe c, ServerPlayerEntity p) {
        Map<Integer, Menu.Entrada> m = new LinkedHashMap<>();
        int[] huecos = {11, 13, 15};
        int k = 0;
        for (Recetas.Inicial ini : Recetas.Inicial.values()) {
            m.put(huecos[k++], e(Menu.icono(ini.icono, ini.nombre, Formatting.GOLD, "Te enseña a hacer:", ini.plato.nombre(), ini.plato.receta, "(Los demás platos se compran en la tienda.)"), q -> elegirInicial(c, q, ini)));
        }
        m.put(4, e(Menu.icono(CafeItems.LIBRO, "Elige a tu Pokémon cocinero", Formatting.AQUA, "El que elijas te enseña tu primer plato."), null));
        Menu.abrir(p, "¿Quién será tu cocinero?", 3, m);
    }

    private static void elegirInicial(Cafe c, ServerPlayerEntity p, Recetas.Inicial ini) {
        Datos.Jugador d = c.dj(p);
        if (d.inicial != null) { p.closeHandledScreen(); return; }
        d.inicial = ini.id;
        if (!d.platos.contains(ini.plato.id)) d.platos.add(ini.plato.id);
        EmiCafe.datos.guardar(p.getUuid());
        p.closeHandledScreen();
        c.reconstruir();
        Msg.title(p, Text.literal("¡" + ini.nombre + "!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Tu cocinero te enseña: " + ini.plato.nombre()).formatted(Formatting.YELLOW), 5, 50, 10);
        p.sendMessage(Msg.say(ini.nombre + " te enseñó a hacer " + ini.plato.nombre() + ": " + ini.plato.receta + ". Clic derecho en la caja registradora para abrir el café."), false);
    }

    static void recetario(Cafe c, ServerPlayerEntity p) {
        Datos.Jugador d = c.dj(p);
        Map<Integer, Menu.Entrada> m = new LinkedHashMap<>();
        int k = 10;
        for (String id : d.platos) {
            Recetas.Plato pl = Recetas.Plato.de(id);
            if (pl == null) continue;
            m.put(k++, e(Menu.icono(pl.item, pl.nombre(), Formatting.GOLD, "Pagan " + pl.precio + " monedas (más propina).", "Receta:", pl.receta, "Clic derecho en el cliente o en su mesa para servirlo."), null));
        }
        m.put(22, e(Menu.icono(CafeItems.PUERTA, "Volver", Formatting.GRAY), q -> principal(c, q)));
        Menu.abrir(p, "Recetario", 3, m);
    }

    /** Comprar platos nuevos y mejorar las instalaciones con las monedas. */
    static void tienda(Cafe c, ServerPlayerEntity p) {
        Datos.Jugador d = c.dj(p);
        Map<Integer, Menu.Entrada> m = new LinkedHashMap<>();
        m.put(4, e(Menu.icono(CafeItems.MONEDA, "Tus monedas: " + d.monedas, Formatting.GOLD), null));
        int k = 11;
        for (Recetas.Plato pl : Recetas.Plato.values()) {
            if (d.platos.contains(pl.id)) continue;
            boolean puede = d.monedas >= pl.coste;
            m.put(k, e(Menu.icono(pl.item, "Aprender: " + pl.nombre(), puede ? Formatting.GREEN : Formatting.RED, "Cuesta " + pl.coste + " monedas.", "Paga " + pl.precio + " monedas cada uno.", "Receta: " + pl.receta, "Desbloquea sus estaciones en la cocina."), q -> comprarPlato(c, q, pl)));
            k += 2;
        }
        if (k == 11) m.put(13, e(Menu.icono(CafeItems.LIBRO, "¡Ya sabes todos los platos!", Formatting.GREEN), null));
        int j = 20;
        for (Recetas.Mejora me : Recetas.Mejora.values()) {
            int nv = d.nivel(me.id);
            if (nv >= me.max()) m.put(j++, e(Menu.icono(me.icono, me.nombre + " (máximo)", Formatting.GREEN, me.descripcion, "Nivel " + nv + " de " + me.max()), null));
            else {
                int precio = me.precios[nv];
                boolean puede = d.monedas >= precio;
                m.put(j++, e(Menu.icono(me.icono, me.nombre, puede ? Formatting.GREEN : Formatting.RED, me.descripcion, "Nivel " + nv + " de " + me.max(), "Subir cuesta " + precio + " monedas."), q -> comprarMejora(c, q, me)));
            }
        }
        m.put(40, e(Menu.icono(CafeItems.PUERTA, "Volver", Formatting.GRAY), q -> principal(c, q)));
        Menu.abrir(p, "Tienda del café", 5, m);
    }

    private static void comprarPlato(Cafe c, ServerPlayerEntity p, Recetas.Plato pl) {
        Datos.Jugador d = c.dj(p);
        if (c.estado != Cafe.Estado.PREPARANDO || d.platos.contains(pl.id)) { tienda(c, p); return; }
        if (d.monedas < pl.coste) { p.sendMessage(Msg.err("Te faltan " + (pl.coste - d.monedas) + " monedas."), false); Msg.sound(p, net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); return; }
        d.monedas -= pl.coste;
        d.platos.add(pl.id);
        Progreso.revisarLogros(c, p, d);
        EmiCafe.datos.guardar(p.getUuid());
        c.reconstruir();
        Msg.sound(p, net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        p.sendMessage(Msg.say("Aprendiste " + pl.nombre() + ": " + pl.receta + ". Sus estaciones ya están en la cocina."), false);
        tienda(c, p);
    }

    private static void comprarMejora(Cafe c, ServerPlayerEntity p, Recetas.Mejora me) {
        Datos.Jugador d = c.dj(p);
        int nv = d.nivel(me.id);
        if (c.estado != Cafe.Estado.PREPARANDO || nv >= me.max()) { tienda(c, p); return; }
        int precio = me.precios[nv];
        if (d.monedas < precio) { p.sendMessage(Msg.err("Te faltan " + (precio - d.monedas) + " monedas."), false); Msg.sound(p, net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); return; }
        d.monedas -= precio;
        d.mejoras.put(me.id, nv + 1);
        Progreso.revisarLogros(c, p, d);
        EmiCafe.datos.guardar(p.getUuid());
        c.reconstruir();
        Msg.sound(p, net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        p.sendMessage(Msg.say(me.nombre + ": nivel " + (nv + 1) + "."), false);
        tienda(c, p);
    }

    /** Lista a los jugadores conectados a los que se puede invitar. */
    static void invitar(Cafe c, ServerPlayerEntity p) {
        Map<Integer, Menu.Entrada> m = new LinkedHashMap<>();
        int k = 0;
        for (ServerPlayerEntity o : c.server.getPlayerManager().getPlayerList()) {
            if (o == p || Cafes.de(o.getUuid()) != null || k >= 36) continue;
            ItemStack cabeza = new ItemStack(Items.PLAYER_HEAD);
            cabeza.set(net.minecraft.component.DataComponentTypes.PROFILE, new net.minecraft.component.type.ProfileComponent(o.getGameProfile()));
            cabeza.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(o.getGameProfile().getName()).styled(s -> s.withColor(Formatting.YELLOW).withBold(true).withItalic(false)));
            cabeza.set(net.minecraft.component.DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(Text.literal("Clic para invitarle a tu café").styled(s -> s.withColor(Formatting.GRAY).withItalic(false)))));
            m.put(k++, e(cabeza, q -> { q.closeHandledScreen(); String err = Cafes.invitar(c, q, o); if (err != null) q.sendMessage(Msg.err(err), false); }));
        }
        if (k == 0) m.put(22, e(Menu.icono(CafeItems.INVITAR, "No hay nadie a quien invitar", Formatting.GRAY, "Solo puedes invitar a jugadores conectados", "que no estén ya en un café."), null));
        m.put(49, e(Menu.icono(CafeItems.PUERTA, "Volver", Formatting.GRAY), q -> principal(c, q)));
        Menu.abrir(p, "Invitar a tu café", 6, m);
    }

    // ------------------------------------------------------------ misiones, logros y tienda de estrellas

    static void misiones(Cafe c, ServerPlayerEntity p) {
        Datos.Jugador d = c.dj(p);
        Map<Integer, Menu.Entrada> m = new LinkedHashMap<>();
        m.put(4, e(Menu.icono(CafeItems.ESTRELLA, "Tus estrellas: " + d.estrellas, Formatting.YELLOW), null));
        int k = 10;
        for (Progreso.Mision mi : Progreso.deHoy(d)) m.put(k++, e(mision(mi, d, "Del día"), null));
        k = 19;
        for (Progreso.Mision mi : Progreso.deSemana(d)) m.put(k++, e(mision(mi, d, "De la semana"), null));
        m.put(22, e(Menu.icono(CafeItems.PUERTA, "Volver", Formatting.GRAY), q -> principal(c, q)));
        Menu.abrir(p, "Misiones", 3, m);
    }

    private static ItemStack mision(Progreso.Mision mi, Datos.Jugador d, String periodo) {
        boolean hecha = d.misHechas.contains(mi.id());
        int prog = d.misProg.getOrDefault(mi.id(), 0);
        return Menu.icono(CafeItems.MISION, (hecha ? "✔ " : "") + mi.texto(), hecha ? Formatting.GREEN : Formatting.WHITE, periodo + " · premio " + mi.premio() + " estrellas", hecha ? "¡Cumplida!" : "Llevas " + prog + " de " + mi.objetivo());
    }

    static void logros(Cafe c, ServerPlayerEntity p) {
        Datos.Jugador d = c.dj(p);
        Map<Integer, Menu.Entrada> m = new LinkedHashMap<>();
        int k = 9;
        for (Progreso.Logro l : Progreso.LOGROS) {
            int nivel = d.logros.getOrDefault(l.id(), 0);
            long v = l.valor().applyAsLong(d);
            boolean fin = nivel >= l.metas().length;
            String sig = fin ? "¡Completo!" : "Siguiente: " + l.metas()[nivel] + " (+" + l.premios()[nivel] + " estrellas)";
            m.put(k++, e(Menu.icono(CafeItems.TROFEO, l.nombre() + " " + nivel + "/" + l.metas().length, fin ? Formatting.GREEN : Formatting.GOLD, "Llevas " + v + " " + l.descripcion(), sig), null));
        }
        m.put(22, e(Menu.icono(CafeItems.PUERTA, "Volver", Formatting.GRAY), q -> principal(c, q)));
        Menu.abrir(p, "Logros", 3, m);
    }

    static void tiendaEstrellas(Cafe c, ServerPlayerEntity p) {
        Datos.Jugador d = c.dj(p);
        Tienda.refrescar(d, System.currentTimeMillis(), java.time.ZoneId.systemDefault());
        Map<Integer, Menu.Entrada> m = new LinkedHashMap<>();
        m.put(4, e(Menu.icono(CafeItems.ESTRELLA, "Tus estrellas: " + d.estrellas, Formatting.YELLOW, "Se ganan sirviendo, con misiones y logros.", "Máximo " + Tienda.LIMITE_DIA + " de cada artículo al día.", "Todo llega a tu /correo."), null));
        int[] huecos = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 31};
        int k = 0;
        for (Tienda.Articulo a : Tienda.ARTICULOS) {
            Item item = net.minecraft.registry.Registries.ITEM.getOrEmpty(net.minecraft.util.Identifier.of(a.item())).orElse(CafeItems.ESTRELLA);
            boolean puede = d.estrellas >= a.precio() && (!a.unico() || !Tienda.yaLoTiene(d, a.id())) && (a.unico() || Tienda.compradosHoy(d, a.id()) < Tienda.LIMITE_DIA);
            String estado = a.unico() ? (Tienda.yaLoTiene(d, a.id()) ? "COMPRADO ✔" : "Solo una vez") : "Hoy: " + Tienda.compradosHoy(d, a.id()) + "/" + Tienda.LIMITE_DIA;
            ItemStack st = Menu.icono(item, a.nombre() + " · " + a.precio() + " ★", a.unico() ? Formatting.LIGHT_PURPLE : puede ? Formatting.GREEN : Formatting.RED, a.texto(), estado);
            if (item != CafeItems.ESTRELLA && item.getMaxCount() > 1) st.setCount(1);
            m.put(huecos[k++], e(st, q -> comprarEstrellas(c, q, a)));
        }
        m.put(40, e(Menu.icono(CafeItems.PUERTA, "Volver", Formatting.GRAY), q -> principal(c, q)));
        Menu.abrir(p, "Tienda de estrellas", 5, m);
    }

    private static void comprarEstrellas(Cafe c, ServerPlayerEntity p, Tienda.Articulo a) {
        Datos.Jugador d = c.dj(p);
        String err = Tienda.comprar(d, a.id(), System.currentTimeMillis(), java.time.ZoneId.systemDefault(), art -> TiendaCorreo.enviar(c.server, p.getUuid(), art));
        if (err != null) { p.sendMessage(Msg.err(err), false); Msg.sound(p, net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_NO, 1f, 1f); }
        else {
            EmiCafe.datos.guardar(p.getUuid());
            Msg.sound(p, net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            p.sendMessage(Msg.say("Compraste «" + a.nombre() + "». Lo tienes en tu /correo."), false);
        }
        tiendaEstrellas(c, p);
    }
}
