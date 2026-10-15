package com.emipokemon.twitch;

import com.emipokemon.Emipokemon;
import com.emipokemon.mail.MailboxService;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_7923;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Premios de Twitch (gift subs y bits) y donaciones de Streamlabs que llevan mas de 2 dias sin entregarse (pack 1.0.139): en vez de quedarse
 * esperando para siempre, sus tickets se REPARTEN entre todos los jugadores (los que hayan entrado alguna vez, como {@code /emipokemon correo enviartodos}).
 * El total se divide a partes iguales y lo que sobra se sortea de uno en uno entre jugadores distintos: los tickets que da un premio de N tickets son N,
 * ni mas ni menos. Los conectados lo reciben directo en el inventario y los demas (o si no les cabe) en el correo ({@code /correo}).
 *
 * Dos entradas: {@link #tick(Object)} (la llama {@code TwitchSupportRewardService.deliverOnePending}, gracias a PatchCommunityRewards, y revisa los premios de
 * subs/bits pendientes) y {@link #repartirTickets} (la llama por reflexion el mod emi-streamlabs con las donaciones vencidas).
 * Opcion de pruebas: {@code -Demi.reparto.segundos=N} cambia los 2 dias.
 */
public final class CommunityRewards {
    private CommunityRewards() {}

    /** Los seis tickets de las recompensas de Twitch (iguales a los de los loot tables emipokemon:twitch/random_tickets_N). */
    private static final String[] TICKETS = {"emipokemon:gacha_ticket", "emipokemon:emi_special_banner_ticket", "emipokemon:treasure_gacha_ticket",
            "emipokemon:sticker_gacha_ticket", "emipokemon:card_booster_ticket", "emipokemon:type_booster_ticket"};
    private static final Pattern LOOT = Pattern.compile("random_tickets_(\\d+)");
    private static long proximaRevision;

    /** Segundos que tiene que llevar un premio sin entregarse para repartirlo (2 dias). */
    static long segundos() {
        try { return Long.getLong("emi.reparto.segundos", 48L * 3600L); } catch (RuntimeException e) { return 48L * 3600L; }
    }

    /** Reparte {@code tickets} tickets aleatorios entre todos los jugadores. true = se repartieron. Hay que llamarlo desde el hilo del servidor. */
    public static boolean repartirTickets(MinecraftServer server, int tickets, String motivo) {
        return repartir(server, tickets, null, motivo);
    }

    /** {@code fijo} = id del ticket (si el premio era de un tipo concreto) o null para que cada ticket salga de un tipo al azar. */
    static boolean repartir(MinecraftServer server, int tickets, String fijo, String motivo) {
        if (server == null || tickets <= 0 || Emipokemon.playerDataManager() == null) return false;
        List<UUID> ids = new ArrayList<>(Emipokemon.playerDataManager().allSavedPlayerIds());
        if (ids.isEmpty()) return false;
        Collections.shuffle(ids, ThreadLocalRandom.current());
        int base = tickets / ids.size(), resto = tickets % ids.size();
        String limpio = motivo == null ? "" : motivo.replace("\"", "'").replace("\\", "");
        String razon = "Reparto de la comunidad: " + limpio + " llevaba 2 dias sin entregarse";
        int ganadores = 0;
        for (int i = 0; i < ids.size(); i++) {
            int n = base + (i < resto ? 1 : 0);
            if (n <= 0) break;                                             // la lista esta mezclada: los que no tocan van al final
            Map<String, Integer> porTipo = new LinkedHashMap<>();
            for (int k = 0; k < n; k++) porTipo.merge(fijo != null ? fijo : TICKETS[ThreadLocalRandom.current().nextInt(TICKETS.length)], 1, Integer::sum);
            for (Map.Entry<String, Integer> e : porTipo.entrySet()) {
                class_2960 id = class_2960.method_12829(e.getKey());
                class_1792 item = id == null ? null : (class_1792) class_7923.field_41178.method_10223(id);
                if (item == null) continue;
                int resta = e.getValue();
                while (resta > 0) {
                    class_1799 pila = new class_1799(item, 1);
                    int ahora = Math.min(resta, Math.max(1, pila.method_7914()));
                    pila.method_7939(ahora);
                    MailboxService.deliverOrMailByUuid(server, ids.get(i), pila, razon);
                    resta -= ahora;
                }
            }
            ganadores++;
            class_3222 online = server.method_3760().method_14602(ids.get(i));
            if (online != null) online.method_7353(class_2561.method_43470("✦ ¡Te " + (n == 1 ? "tocó 1 ticket" : "tocaron " + n + " tickets") + " del reparto de la comunidad!"), false);
        }
        server.method_3760().method_43514(class_2561.method_43470("✦ COMUNIDAD · " + limpio + " llevaba 2 días sin entregarse: se reparten sus " + tickets + " ticket(s) entre los "
                + ids.size() + " jugadores (" + ganadores + " " + (ganadores == 1 ? "recibe" : "reciben") + "). Los conectados los tienen en el inventario; el resto, en /correo."), false);
        Emipokemon.LOGGER.info("[Reparto] {} ticket(s) de \"{}\" repartidos entre {} jugador(es) ({} con premio)", new Object[]{tickets, limpio, ids.size(), ganadores});
        return true;
    }

    /** La llama {@code TwitchSupportRewardService.deliverOnePending} (cada ~15 s): reparte los premios de subs/bits para donador que llevan mas de 2 dias pendientes. */
    public static void tick(Object service) {
        long ahora = System.currentTimeMillis();
        if (ahora < proximaRevision) return;
        proximaRevision = ahora + 60_000L;
        try {
            Object estado = campo(service, "state");
            @SuppressWarnings("unchecked") List<Object> pendientes = (List<Object>) campo(estado, "pending");
            if (pendientes == null || pendientes.isEmpty()) return;
            MinecraftServer server = (MinecraftServer) campo(service, "server");
            if (server == null) return;
            long limite = Instant.now().getEpochSecond() - segundos();
            boolean cambio = false;
            for (Object p : new ArrayList<>(pendientes)) {
                if (!"donador".equals(campo(p, "destination")) || (Boolean) campo(p, "test") || (Long) campo(p, "createdAt") > limite) continue;
                String accion = String.valueOf(campo(p, "action"));
                int n = 0;
                String fijo = null;
                if ("command".equals(accion)) {
                    Matcher m = LOOT.matcher(String.valueOf(campo(p, "command")));
                    if (m.find()) n = Integer.parseInt(m.group(1));
                } else if ("tickets".equals(accion)) {
                    n = Math.max(1, (Integer) campo(p, "count"));
                    String tipo = String.valueOf(campo(p, "value"));
                    fijo = "emi".equals(tipo) ? "emipokemon:emi_special_banner_ticket" : ("tesoro".equals(tipo) || "treasure".equals(tipo)) ? "emipokemon:treasure_gacha_ticket" : "emipokemon:gacha_ticket";
                }
                if (n <= 0) continue;                                      // Pokemon, caos u otros comandos: no se reparten
                String quien = String.valueOf(campo(p, "twitchLogin"));
                if (repartir(server, n, fijo, "el premio de Twitch de " + (quien.isBlank() ? "un espectador" : quien))) {
                    pendientes.remove(p);
                    cambio = true;
                } else {
                    break;
                }
            }
            if (cambio) {
                Method guardar = service.getClass().getDeclaredMethod("save");
                guardar.setAccessible(true);
                guardar.invoke(service);
            }
        } catch (Throwable e) {
            Emipokemon.LOGGER.warn("[Reparto] no se pudieron repartir los premios de Twitch vencidos: {}", e.toString());
        }
    }

    private static Object campo(Object o, String nombre) throws ReflectiveOperationException {
        Field f = o.getClass().getDeclaredField(nombre);
        f.setAccessible(true);
        return f.get(o);
    }
}
