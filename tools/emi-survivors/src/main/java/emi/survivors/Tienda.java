package emi.survivors;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;

/**
 * La tienda de recompensas: se compran con coronas ({@link Retos}) objetos de Pokemon y tickets de los banners de Emipokemon, y con michiestrellitas el peluche de Ms. President (unico, una vez por jugador). Cada articulo tiene un maximo de
 * {@link #LIMITE_DIA} compras por jugador y por dia (decision del dueño) y se entrega al CORREO del jugador ({@code /correo}; ver {@link TiendaCorreo}). Los precios
 * son una primera estimacion del asistente: el dueño los ajusta aqui. Sin Minecraft, asi se prueba con un main.
 */
final class Tienda {
    private Tienda() {}

    /** Cuantas veces se puede comprar cada articulo por dia y jugador. */
    static final int LIMITE_DIA = 10;

    /** {@code item} = id del objeto de Minecraft; {@code grupo}: «banner» (tickets de Emipokemon) u «objeto» (de Cobblemon). */
    record Articulo(String id, String item, String nombre, String texto, int precio, String grupo, String moneda, boolean unico) {
        /** Un articulo normal: se paga con coronas y se puede comprar {@link #LIMITE_DIA} veces al dia. */
        Articulo(String id, String item, String nombre, String texto, int precio, String grupo) { this(id, item, nombre, texto, precio, grupo, "coronas", false); }
        boolean enEstrellas() { return "estrellas".equals(moneda); }
        String unidad() { return enEstrellas() ? "michiestrellitas" : precio == 1 ? "corona" : "coronas"; }
    }

    static final List<Articulo> ARTICULOS = List.of(
            new Articulo("gacha", "emipokemon:gacha_ticket", "Ticket Gacha", "Una tirada del banner normal.", 3, "banner"),
            new Articulo("pegatinas", "emipokemon:sticker_gacha_ticket", "Ticket de Cosméticos", "Una tirada del gasha de cosméticos.", 3, "banner"),
            new Articulo("cartas", "emipokemon:card_booster_ticket", "Ticket de Cartas", "Un sobre de cartas.", 3, "banner"),
            new Articulo("tipos", "emipokemon:type_booster_ticket", "Ticket de Tipos", "Un booster de tipos.", 4, "banner"),
            new Articulo("tesoros", "emipokemon:treasure_gacha_ticket", "Ticket de Tesoros", "Una tirada del banner de tesoros de Emi.", 4, "banner"),
            new Articulo("garra", "emipokemon:claw_ticket", "Ticket de Garra", "Una jugada de la máquina de la garra.", 4, "banner"),
            new Articulo("especial", "emipokemon:emi_special_banner_ticket", "Ticket Especial de Emi", "Una tirada del banner especial de Emi.", 8, "banner"),
            new Articulo("ultra", "cobblemon:ultra_ball", "Ultra Ball", "Una Ultra Ball.", 2, "objeto"),
            new Articulo("caramelo", "cobblemon:rare_candy", "Caramelo Raro", "Sube un nivel a un Pokémon.", 4, "objeto"),
            new Articulo("revivir", "cobblemon:max_revive", "Revivir Máximo", "Revive a un Pokémon con toda su vida.", 4, "objeto"),
            new Articulo("capsula", "cobblemon:ability_capsule", "Cápsula Habilidad", "Cambia la habilidad de un Pokémon.", 15, "objeto"),
            new Articulo("master", "cobblemon:master_ball", "Master Ball", "Captura cualquier Pokémon.", 40, "objeto"),
            // el peluche de Ms. President (emi-plush 1.16.0): objeto UNICO, se paga con michiestrellitas (no con coronas) y solo se puede comprar una vez por jugador
            new Articulo("peluche_ms", "emi_plush:gatitoalien_suit", "Peluche de Ms. President", "El michi con traje y maletín. Solo se compra una vez.", 1000, "unico", "estrellas", true));

    static Articulo articulo(String id) {
        for (Articulo a : ARTICULOS) if (a.id().equals(id)) return a;
        return null;
    }

    /** Quien entrega el articulo (al correo). true = entregado. */
    interface Entrega { boolean entregar(Articulo a); }

    /** Al cambiar el dia se vacian las compras del dia anterior. */
    static void refrescar(Datos.Jugador j, long ahoraMs, ZoneId zona) {
        String hoy = Retos.claveDia(ahoraMs, zona);
        if (j.tiendaCompras == null) j.tiendaCompras = new HashMap<>();
        if (!hoy.equals(j.tiendaDia)) { j.tiendaDia = hoy; j.tiendaCompras.clear(); }
    }

    /** ¿Ya compro este articulo unico? */
    static boolean yaLoTiene(Datos.Jugador j, String id) { return j.unicos != null && j.unicos.contains(id); }

    static int compradosHoy(Datos.Jugador j, String id) { return j.tiendaCompras == null ? 0 : j.tiendaCompras.getOrDefault(id, 0); }

    /**
     * Compra una unidad del articulo. Devuelve el motivo si no se puede, o null si se ha hecho. Solo se cobra y se cuenta la compra DESPUES de que el correo
     * haya aceptado el articulo: si no se puede entregar, el jugador no pierde coronas.
     */
    static String comprar(Datos.Jugador j, String id, long ahoraMs, ZoneId zona, Entrega entrega) {
        Articulo a = articulo(id);
        if (a == null) return "Ese artículo no existe.";
        refrescar(j, ahoraMs, zona);
        if (j.unicos == null) j.unicos = new java.util.ArrayList<>();
        if (a.unico()) { if (yaLoTiene(j, id)) return "Ya tienes «" + a.nombre() + "»: solo se puede comprar una vez."; }
        else if (compradosHoy(j, id) >= LIMITE_DIA) return "Hoy ya has comprado " + LIMITE_DIA + " de «" + a.nombre() + "». Mañana podrás comprar más.";
        int saldo = a.enEstrellas() ? j.monedas : j.coronas;
        if (saldo < a.precio()) return "Te faltan " + (a.precio() - saldo) + " " + (a.enEstrellas() ? "michiestrellitas." : a.precio() - saldo == 1 ? "corona." : "coronas.");
        boolean ok;
        try { ok = entrega.entregar(a); } catch (RuntimeException e) { ok = false; }
        if (!ok) return "No se ha podido enviar al correo ahora mismo (no has gastado nada). Inténtalo más tarde.";
        if (a.enEstrellas()) j.monedas -= a.precio(); else j.coronas -= a.precio();
        if (a.unico()) j.unicos.add(id); else j.tiendaCompras.merge(id, 1, Integer::sum);
        return null;
    }
}
