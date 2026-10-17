package emi.carreras;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;

/** El menu de la sala. (Fase C1: solo un aviso en el chat; la pantalla llega despues.) */
final class Menu {
    private Menu() {}

    static void abrir(ServerPlayerEntity p, String tab) { abrir(p, tab, null); }

    static void abrir(ServerPlayerEntity p, String tab, String resultadoJson) {
        p.sendMessage(Msg.say("Estás en la sala de carreras. Usa /carrera ayuda; con /carrera salir vuelves a tu sitio."), false);
    }

    static void cerrar(ServerPlayerEntity p) { }

    /** El resultado de la carrera que acaba de terminar, en JSON (para ensenarlo en el menu al volver a la sala). {@code ticks} = -1 si no la termino. */
    static String resultado(String pista, boolean entrenamiento, int puesto, int jugadores, int ticks, boolean record, Logros.Novedades n, int saldo) {
        JsonObject r = new JsonObject();
        r.addProperty("pista", pista);
        r.addProperty("entrenamiento", entrenamiento);
        r.addProperty("puesto", puesto);
        r.addProperty("jugadores", jugadores);
        r.addProperty("tiempo", ticks < 0 ? "" : Logros.tiempo(ticks));
        r.addProperty("record", record);
        r.addProperty("estrellas", n.total());
        r.addProperty("saldo", saldo);
        JsonArray l = new JsonArray();
        for (String x : n.lineas) l.add(x);
        r.add("lineas", l);
        return r.toString();
    }
}
