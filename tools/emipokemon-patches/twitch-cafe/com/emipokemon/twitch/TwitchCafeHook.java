package com.emipokemon.twitch;

import java.lang.reflect.Method;

/**
 * Pedidos del Cafe Michialien desde el chat de Twitch. Se llama desde {@link TwitchChatRelayService} (parche PatchTwitchCafe) y pasa a
 * {@code emi.cafe.TwitchPedidos} (mod emi-cafe) por reflexion; si emi-cafe no esta instalado no pasa nada. Siempre en el hilo del servidor.
 * <ul>
 *   <li>{@link #alChat}: cada mensaje del chat que empieza por «!cafeteria».</li>
 *   <li>{@link #escuchar}: el relé de Emipokemon solo lee el chat si hay conectado alguien de {@code chatViewerUuids} o un operador;
 *       con esto tambien lo lee cuando el cafe de la streamer esta abierto, aunque no sea el caso.</li>
 * </ul>
 */
final class TwitchCafeHook {
    private static final String COMANDO = "!cafeteria";
    private static Method pedido, escucha;
    private static boolean buscado;

    private TwitchCafeHook() {}

    private static void buscar() {
        if (buscado) return;
        buscado = true;
        try {
            Class<?> c = Class.forName("emi.cafe.TwitchPedidos");
            pedido = c.getMethod("alChat", String.class, String.class, String.class, boolean.class, boolean.class, boolean.class, boolean.class);
            escucha = c.getMethod("escuchando");
        } catch (Throwable t) {
            pedido = null; escucha = null;
            com.emipokemon.Emipokemon.LOGGER.info("Pedidos del cafe por Twitch: emi-cafe no esta disponible ({})", t.toString());
        }
    }

    static void alChat(TwitchBridgeClient.ChatMessage m) {
        try {
            String t = m.text();
            if (t == null || t.length() < COMANDO.length() || !t.regionMatches(true, 0, COMANDO, 0, COMANDO.length())) return;
            buscar();
            if (pedido != null) pedido.invoke(null, m.login(), m.displayName(), t, m.moderator(), m.vip(), m.subscriber(), m.broadcaster());
        } catch (Throwable ignorado) { /* un pedido que falla no puede romper el chat de Twitch */ }
    }

    static boolean escuchar() {
        try {
            buscar();
            return escucha != null && (Boolean) escucha.invoke(null);
        } catch (Throwable t) {
            if (!avisado) { avisado = true; com.emipokemon.Emipokemon.LOGGER.warn("Pedidos del cafe por Twitch: error al preguntar a emi-cafe", t); }
            return false;
        }
    }
    private static boolean avisado;
}
