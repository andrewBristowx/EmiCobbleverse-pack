package emi.cafe;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * La tienda de ESTRELLAS del cafe: los mismos articulos que las de Survivors, Carreras, la Huida y Emi Fall (objetos de Pokemon y tickets de los banners de Emipokemon) pagados con estrellas,
 * y un articulo exclusivo: el peluche del Michialien chef (unico, una vez por jugador). Cada articulo normal tiene un maximo de {@link #LIMITE_DIA} compras por jugador y por dia y se entrega al
 * CORREO del jugador ({@code /correo}; ver {@link TiendaCorreo}). Los precios son una primera estimacion del asistente: el dueño los ajusta aqui.
 */
final class Tienda {
    private Tienda() {}

    static final int LIMITE_DIA = 10;

    /** {@code item} = id del objeto de Minecraft; {@code grupo}: «banner» (tickets de Emipokemon), «objeto» (de Cobblemon) o «unico». */
    record Articulo(String id, String item, String nombre, String texto, int precio, String grupo, boolean unico) {
        Articulo(String id, String item, String nombre, String texto, int precio, String grupo) { this(id, item, nombre, texto, precio, grupo, false); }
    }

    static final List<Articulo> ARTICULOS = List.of(
            new Articulo("gacha", "emipokemon:gacha_ticket", "Ticket Gacha", "Una tirada del banner normal.", 60, "banner"),
            new Articulo("pegatinas", "emipokemon:sticker_gacha_ticket", "Ticket de Cosméticos", "Una tirada del gasha de cosméticos.", 60, "banner"),
            new Articulo("cartas", "emipokemon:card_booster_ticket", "Ticket de Cartas", "Un sobre de cartas.", 60, "banner"),
            new Articulo("tipos", "emipokemon:type_booster_ticket", "Ticket de Tipos", "Un booster de tipos.", 80, "banner"),
            new Articulo("tesoros", "emipokemon:treasure_gacha_ticket", "Ticket de Tesoros", "Una tirada del banner de tesoros de Emi.", 80, "banner"),
            new Articulo("garra", "emipokemon:claw_ticket", "Ticket de Garra", "Una jugada de la máquina de la garra.", 80, "banner"),
            new Articulo("especial", "emipokemon:emi_special_banner_ticket", "Ticket Especial de Emi", "Una tirada del banner especial de Emi.", 160, "banner"),
            new Articulo("ultra", "cobblemon:ultra_ball", "Ultra Ball", "Una Ultra Ball.", 40, "objeto"),
            new Articulo("caramelo", "cobblemon:rare_candy", "Caramelo Raro", "Sube un nivel a un Pokémon.", 80, "objeto"),
            new Articulo("revivir", "cobblemon:max_revive", "Revivir Máximo", "Revive a un Pokémon con toda su vida.", 80, "objeto"),
            new Articulo("capsula", "cobblemon:ability_capsule", "Cápsula Habilidad", "Cambia la habilidad de un Pokémon.", 300, "objeto"),
            new Articulo("master", "cobblemon:master_ball", "Master Ball", "Captura cualquier Pokémon.", 800, "objeto"),
            // el peluche exclusivo del Cafe Michialien (emi-plush 1.20.0): el Michialien chef; objeto UNICO, una vez por jugador
            new Articulo("peluche_chef", "emi_plush:gatitoalien_chef", "Peluche Michialien Chef", "El michi con gorro y delantal de cocinero. Solo se compra una vez.", 1000, "unico", true));

    static Articulo articulo(String id) {
        for (Articulo a : ARTICULOS) if (a.id().equals(id)) return a;
        return null;
    }

    /** Quien entrega el articulo (al correo). true = entregado. */
    interface Entrega { boolean entregar(Articulo a); }

    static String claveDia(long ms, ZoneId zona) { return Instant.ofEpochMilli(ms).atZone(zona).toLocalDate().toString(); }

    /** Al cambiar el dia se vacian las compras del dia anterior. */
    static void refrescar(Datos.Jugador j, long ahoraMs, ZoneId zona) {
        String hoy = claveDia(ahoraMs, zona);
        if (!hoy.equals(j.tiendaDia)) { j.tiendaDia = hoy; j.tiendaCompras.clear(); }
    }

    static boolean yaLoTiene(Datos.Jugador j, String id) { return j.unicos.contains(id); }

    static int compradosHoy(Datos.Jugador j, String id) { return j.tiendaCompras.getOrDefault(id, 0); }

    /** Compra una unidad. Devuelve el motivo si no se puede, o null si se ha hecho. Solo se cobra DESPUES de que el correo haya aceptado el articulo. */
    static String comprar(Datos.Jugador j, String id, long ahoraMs, ZoneId zona, Entrega entrega) {
        Articulo a = articulo(id);
        if (a == null) return "Ese artículo no existe.";
        refrescar(j, ahoraMs, zona);
        if (a.unico()) { if (yaLoTiene(j, id)) return "Ya tienes «" + a.nombre() + "»: solo se puede comprar una vez."; }
        else if (compradosHoy(j, id) >= LIMITE_DIA) return "Hoy ya has comprado " + LIMITE_DIA + " de «" + a.nombre() + "». Mañana podrás comprar más.";
        if (j.estrellas < a.precio()) return "Te faltan " + (a.precio() - j.estrellas) + " estrellas.";
        boolean ok;
        try { ok = entrega.entregar(a); } catch (RuntimeException e) { ok = false; }
        if (!ok) return "No se ha podido enviar al correo ahora mismo (no has gastado nada). Inténtalo más tarde.";
        j.estrellas -= a.precio();
        if (a.unico()) j.unicos.add(id); else j.tiendaCompras.merge(id, 1, Integer::sum);
        return null;
    }
}
