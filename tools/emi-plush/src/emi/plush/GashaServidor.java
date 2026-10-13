package emi.plush;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_1661;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_9015;

/**
 * Lado servidor de la pantalla del Gasha: abre la pantalla, valida las peticiones (cerca de la máquina, tickets suficientes) y hace las tiradas
 * ejecutando `emi_plush:gasha/tirada`, igual que el ticket suelto. Qué premio salió se lee del almacenamiento `emi_plush:gasha` → `res`.
 */
public final class GashaServidor {
    private static final class_2960 ALMACEN = class_2960.method_60655(EmiPlush.ID, "gasha");

    private GashaServidor() {}

    public static void registrar() {
        ServerPlayNetworking.registerGlobalReceiver(GashaPeticion.ID, (p, ctx) -> manejar(ctx.player(), p.json()));
    }

    /** Clic derecho en la máquina: el cliente abre la pantalla. */
    public static void abrir(class_3222 jugador, class_2338 pos) {
        JsonObject o = estado(jugador, pos);
        o.addProperty("t", "abrir");
        ServerPlayNetworking.send(jugador, new GashaRespuesta(o.toString()));
    }

    private static JsonObject estado(class_3222 jugador, class_2338 pos) {
        JsonObject o = new JsonObject();
        o.addProperty("x", pos.method_10263());
        o.addProperty("y", pos.method_10264());
        o.addProperty("z", pos.method_10260());
        o.addProperty("garantia", garantia(jugador));
        o.addProperty("tickets", tickets(jugador.method_31548()));
        return o;
    }

    static int garantia(class_3222 jugador) {
        var tablero = jugador.method_5682().method_3845();
        var objetivo = tablero.method_1170("emi_gasha");
        if (objetivo == null) return 0;
        return tablero.method_1180(class_9015.method_55422(jugador.method_5820()), objetivo).method_55409();
    }

    static int tickets(class_1661 inv) {
        int n = 0;
        for (int i = 0; i < inv.method_5439(); i++) {
            var st = inv.method_5438(i);
            if (st.method_31574(EmiPlush.TICKET_GASHA)) n += st.method_7947();
        }
        return n;
    }

    private static void gastar(class_1661 inv, int n) {
        for (int i = 0; i < inv.method_5439() && n > 0; i++) {
            var st = inv.method_5438(i);
            if (!st.method_31574(EmiPlush.TICKET_GASHA)) continue;
            int k = Math.min(n, st.method_7947());
            st.method_7934(k);
            n -= k;
        }
    }

    static void manejar(class_3222 jugador, String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            class_2338 pos = new class_2338(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt());
            var mundo = jugador.method_37908();
            double dx = jugador.method_23317() - (pos.method_10263() + 0.5), dy = jugador.method_23318() - pos.method_10264(), dz = jugador.method_23321() - (pos.method_10260() + 0.5);
            if (!mundo.method_8320(pos).method_27852(EmiPlush.MAQUINA_GASHA) || dx * dx + dy * dy + dz * dz > 100) return;
            String op = o.get("op").getAsString();
            if (op.equals("estado")) {
                JsonObject r = estado(jugador, pos);
                r.addProperty("t", "estado");
                ServerPlayNetworking.send(jugador, new GashaRespuesta(r.toString()));
            } else if (op.equals("tirar")) {
                int n = o.get("n").getAsInt() == 10 ? 10 : 1;
                tirar(jugador, pos, n);
            }
        } catch (RuntimeException e) {
            // petición mal formada: se ignora
        }
    }

    private static void tirar(class_3222 jugador, class_2338 pos, int n) {
        var inv = jugador.method_31548();
        if (tickets(inv) < n) {
            JsonObject r = estado(jugador, pos);
            r.addProperty("t", "error");
            r.addProperty("error", "sin_tickets");
            ServerPlayNetworking.send(jugador, new GashaRespuesta(r.toString()));
            return;
        }
        gastar(inv, n);
        var servidor = jugador.method_5682();
        var fuente = jugador.method_5671().method_9206(4);
        JsonArray premios = new JsonArray();
        for (int i = 0; i < n; i++) {
            servidor.method_3734().method_44252(fuente, "function emi_plush:gasha/tirada");
            class_2487 almacen = servidor.method_22827().method_22546(ALMACEN);
            premios.add(premio(almacen.method_10562("res")));
        }
        if (mundoMaquina(jugador, pos) instanceof GashaMachineBlockEntity maquina) maquina.efecto();
        JsonObject r = estado(jugador, pos);
        r.addProperty("t", "resultado");
        r.add("premios", premios);
        ServerPlayNetworking.send(jugador, new GashaRespuesta(r.toString()));
    }

    private static net.minecraft.class_2586 mundoMaquina(class_3222 jugador, class_2338 pos) {
        return jugador.method_37908().method_8321(pos);
    }

    /** Convierte el premio guardado por las funciones ({pool, sel:{n,c,cmd1,cmd2}}) en lo que necesita dibujar la pantalla. */
    static JsonObject premio(class_2487 res) {
        String pool = res.method_10558("pool");
        class_2487 sel = res.method_10562("sel");
        JsonObject o = new JsonObject();
        o.addProperty("pool", pool);
        o.addProperty("n", sel.method_10558("n"));
        JsonArray objetos = new JsonArray(), pokemon = new JsonArray();
        boolean mayor = pool.equals("pareja") || sel.method_10558("n").contains("Dorado");
        for (String cmd : new String[]{sel.method_10558("cmd1"), sel.method_10558("cmd2")}) {
            if (cmd.startsWith("give @s ")) {
                String[] p = cmd.substring(8).trim().split(" ");
                JsonObject it = new JsonObject();
                it.addProperty("id", p[0]);
                it.addProperty("n", p.length > 1 ? Integer.parseInt(p[1]) : 1);
                objetos.add(it);
                if (p[0].equals("cobblemon:master_ball") || p[0].equals("emipokemon:shiny_amulet")) mayor = true;
            } else if (cmd.startsWith("givepokemon @s ")) {
                pokemon.add(cmd.substring(15).trim());
            }
        }
        o.add("objetos", objetos);
        o.add("pokemon", pokemon);
        o.addProperty("mayor", mayor);
        return o;
    }
}
