package emi.cafe;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Pedidos desde el chat de Twitch: «!cafeteria» enseña la carta y «!cafeteria papasfritas quiero bien fritas» hace que entre al cafe
 * de la streamer un Pokemon con el nombre del espectador, con su plato y su mensaje en el globo.
 *
 * El chat llega de Emipokemon (que lo lee del puente de Twitch del servidor): un parche suyo llama a {@link #alChat} con cada mensaje.
 * Aqui no hay ninguna conexion a Twitch ni ninguna clave. {@code alChat} puede llamarse desde cualquier hilo: solo encola; todo lo demas pasa en el tick del servidor.
 */
public final class TwitchPedidos {
    private TwitchPedidos() {}

    static final String COMANDO = "!cafeteria";
    /** De quien son los cafes donde salen los pedidos (nombres de Minecraft separados por comas; si hay varios abiertos, sale en el primero de la lista). */
    static final String DUENIA = System.getProperty("emi.cafe.twitch.dueno", "EmilyExtacy,somitadepony");
    static final String[] DUENIOS = DUENIA.split("\\s*,\\s*");

    /** ¿Es ese jugador uno de los que reciben pedidos? */
    static boolean esDuenio(String nombre) { for (String d : DUENIOS) if (d.equalsIgnoreCase(nombre)) return true; return false; }
    static boolean activo = !"false".equalsIgnoreCase(System.getProperty("emi.cafe.twitch", "true"));
    /** Segundos que tiene que esperar cada espectador entre pedidos (moderadores y la streamer: ESPERA_MOD). */
    static final int ESPERA = Integer.getInteger("emi.cafe.twitch.espera", 90), ESPERA_MOD = 15;
    /** Espectadores a la vez dentro del cafe, pedidos en cola y segundos que aguanta un pedido en cola esperando mesa. */
    static final int MAX_DENTRO = Integer.getInteger("emi.cafe.twitch.max", 4), MAX_COLA = 6, COLA_SEG = 120;

    /** Un mensaje del chat que empieza por el comando, tal como lo manda Emipokemon. */
    private record Entrada(String login, String nombre, String texto, boolean mod) {}
    /** Un pedido ya entendido que espera mesa. */
    private record Pendiente(String nombre, Recetas.Plato plato, String mensaje, long caduca) {}

    private static final ConcurrentLinkedQueue<Entrada> ENTRADAS = new ConcurrentLinkedQueue<>();
    private static final ArrayDeque<Pendiente> COLA = new ArrayDeque<>();
    private static final Map<String, Long> ULTIMO = new HashMap<>();             // login -> ms del ultimo pedido
    private static long ultimaLista;                                              // ms: la carta no se repite en el chat del juego mas de una vez cada 20 s
    private static long tick;
    private static final boolean DEBUG = Boolean.getBoolean("emi.cafe.debug");
    private static void dbg(String f, Object... a) { if (DEBUG) EmiCafe.LOG.info("[twitch] " + f, a); }

    /** Lo llama el parche de Emipokemon por cada mensaje del chat de Twitch (cualquier hilo). Lo que no empiece por el comando se tira. */
    public static void alChat(String login, String nombre, String texto, boolean moderador, boolean vip, boolean suscriptor, boolean streamer) {
        if (!activo || texto == null) return;
        String t = texto.strip();
        if (t.length() < COMANDO.length() || !t.regionMatches(true, 0, COMANDO, 0, COMANDO.length())) return;
        if (t.length() > COMANDO.length() && !Character.isWhitespace(t.charAt(COMANDO.length()))) return;     // «!cafeteriaxyz» no es el comando
        if (ENTRADAS.size() > 200) return;
        dbg("llega «{}» de {}", t, login);
        ENTRADAS.add(new Entrada(login == null ? "" : login.toLowerCase(java.util.Locale.ROOT), nombre == null || nombre.isBlank() ? login : nombre, t.substring(COMANDO.length()).strip(), moderador || streamer));
    }

    // ------------------------------------------------------------ el tick

    static void tick(MinecraftServer s) {
        tick++;
        Entrada e;
        int n = 0;
        while (n++ < 10 && (e = ENTRADAS.poll()) != null) {
            try { atender(e); } catch (Throwable t) { EmiCafe.LOG.error("Pedido de Twitch con error", t); }
        }
        if (tick % 20 == 0 && !COLA.isEmpty()) {
            long ahora = System.currentTimeMillis();
            COLA.removeIf(p -> p.caduca < ahora);
            Cafe c = cafe();
            Pendiente p;
            while (c != null && (p = COLA.peek()) != null) {
                String r = c.twitchCliente(p.nombre, p.plato, p.mensaje, MAX_DENTRO);
                if (r != null) break;                      // sigue sin haber sitio
                COLA.poll();
            }
        }
    }

    /** Lo pregunta Emipokemon (por reflexion) antes de decidir si lee el chat: solo hace falta mientras el cafe de la streamer esta abierto. */
    public static boolean escuchando() { return activo && (SIEMPRE || cafe() != null); }
    /** Solo pruebas ({@code -Demi.cafe.twitch.siempre=true}): Emipokemon lee el chat aunque el cafe este cerrado. */
    private static final boolean SIEMPRE = Boolean.getBoolean("emi.cafe.twitch.siempre");

    /** El cafe de la streamer si esta dentro de el y lo tiene abierto; null si no. */
    static Cafe cafe() {
        for (String d : DUENIOS) { Cafe c = Cafes.deDuenio(d); if (c != null && c.abierto()) return c; }
        return null;
    }

    private static void atender(Entrada e) {
        Cafe c = cafe();
        if (c == null) { dbg("ignorado: no hay café abierto de {}", DUENIA); return; }                             // sin cafe abierto no hay nada que hacer (y no se dice nada: el chat es muy rapido)
        List<Recetas.Plato> carta = carta(c);
        String resto = e.texto;
        if (resto.isEmpty()) {                             // «!cafeteria» solo: la carta, en el chat del juego
            long ahora = System.currentTimeMillis();
            if (ahora - ultimaLista < 20_000) return;
            ultimaLista = ahora;
            c.aTodos(p -> p.sendMessage(Msg.say("Twitch · carta del café: " + listaTexto(carta) + "  (se pide con " + COMANDO + " plato mensaje)"), false));
            return;
        }
        long ahora = System.currentTimeMillis();
        Long antes = ULTIMO.get(e.login);
        long espera = (e.mod ? ESPERA_MOD : ESPERA) * 1000L;
        if (antes != null && ahora - antes < espera) return;

        String[] tok = resto.split("\\s+");
        Recetas.Plato pl = null;
        int usados = 0;
        for (int k = Math.min(4, tok.length); k >= 1 && pl == null; k--) {          // «papas fritas», «papasfritas»...: se prueba con 4, 3, 2 y 1 palabras
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < k; i++) sb.append(tok[i]);
            pl = buscar(carta, TwitchTexto.compacto(sb.toString()));
            if (pl != null) usados = k;
        }
        if (pl == null) {
            String intento = TwitchTexto.compacto(tok[0]);
            if (intento.length() >= 4) {                   // un trozo que solo encaja con un plato: «limon» → limonada
                Recetas.Plato unico = null; int cuantos = 0;
                for (Recetas.Plato p : carta) if (TwitchTexto.compacto(p.id).startsWith(intento) || TwitchTexto.compacto(p.nombre()).startsWith(intento)) { unico = p; cuantos++; }
                if (cuantos == 1) { pl = unico; usados = 1; }
            }
        }
        if (pl == null) {
            if (ahora - ultimaLista >= 20_000) {
                ultimaLista = ahora;
                String que = TwitchTexto.limpiar(tok[0]);
                c.aTodos(p -> p.sendMessage(Msg.say("Twitch · " + TwitchTexto.nombreSeguro(e.nombre, e.login) + " pidió «" + (que.isEmpty() ? "?" : que) + "», que no está en la carta. Hoy hay: " + listaTexto(carta)), false));
            }
            return;
        }
        StringBuilder m = new StringBuilder();
        for (int i = usados; i < tok.length; i++) m.append(i > usados ? " " : "").append(tok[i]);
        String mensaje = TwitchTexto.limpiarMensaje(m.toString());
        String nombre = TwitchTexto.nombreSeguro(e.nombre, e.login);

        ULTIMO.put(e.login, ahora);
        if (ULTIMO.size() > 500) ULTIMO.values().removeIf(v -> ahora - v > 600_000);
        String r = c.twitchCliente(nombre, pl, mensaje, MAX_DENTRO);
        dbg("pedido de {}: {} «{}» → {}", nombre, pl.id, mensaje, r == null ? "entra" : "en cola (" + r + ")");
        if (r != null) {
            if (COLA.size() < MAX_COLA) COLA.add(new Pendiente(nombre, pl, mensaje, ahora + COLA_SEG * 1000L));
        }
    }

    // ------------------------------------------------------------ la carta

    /** Los platos que ahora mismo se pueden pedir: los que el cafe sabe hacer y tiene las estaciones puestas para ello. */
    static List<Recetas.Plato> carta(Cafe c) {
        List<Recetas.Plato> l = new ArrayList<>();
        for (String id : c.idsServibles()) { Recetas.Plato p = Recetas.Plato.de(id); if (p != null) l.add(p); }
        return l;
    }

    /** «papasfritas, limonada, ensalada»: lo que el espectador tiene que escribir. */
    static String listaTexto(List<Recetas.Plato> carta) {
        if (carta.isEmpty()) return "(todavía nada)";
        StringBuilder sb = new StringBuilder();
        for (Recetas.Plato p : carta) sb.append(sb.length() > 0 ? ", " : "").append(TwitchTexto.compacto(p.nombre()));
        return sb.toString();
    }

    private static Recetas.Plato buscar(List<Recetas.Plato> carta, String clave) {
        if (clave.isEmpty()) return null;
        for (Recetas.Plato p : carta) if (TwitchTexto.compacto(p.id).equals(clave) || TwitchTexto.compacto(p.nombre()).equals(clave)) return p;
        return null;
    }

    /** Solo pruebas: borra las esperas y la cola. */
    static void reiniciar() { ULTIMO.clear(); COLA.clear(); ENTRADAS.clear(); ultimaLista = 0; }

}
