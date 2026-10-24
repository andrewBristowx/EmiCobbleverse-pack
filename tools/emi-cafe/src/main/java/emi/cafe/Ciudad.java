package emi.cafe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * La ciudad de los cafes: cada jugador tiene una parcela fija (su «slot», que no cambia nunca) en una calle larga de la dimension {@code emi_cafe:cafe}. Las parcelas miden 64 de ancho
 * (x de {@code slot*64 - 4} a {@code slot*64 + 59}) y se van pegando una a otra. Se guarda en {@code <mundo>/emi_cafe/ciudad.json}. Sin Minecraft (solo Gson).
 */
final class Ciudad {
    private Ciudad() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Ancho de una parcela. */
    static final int ANCHO = 64;
    /** x relativo (al origen del local) del primer y del ultimo bloque de la parcela. */
    static final int X0 = -4, X1 = 59;

    static final class Vecino { int slot; String nombre = ""; }

    private static Path archivo;
    private static final Map<String, Vecino> VECINOS = new TreeMap<>();                 // uuid -> parcela

    static void cargar(Path raiz) {
        archivo = raiz.resolve("ciudad.json");
        VECINOS.clear();
        try {
            if (Files.exists(archivo)) {
                Map<String, Vecino> m = GSON.fromJson(Files.readString(archivo, StandardCharsets.UTF_8), new TypeToken<Map<String, Vecino>>() {}.getType());
                if (m != null) VECINOS.putAll(m);
            }
        } catch (Exception e) { EmiCafe.LOG.error("No se pudo leer la ciudad del cafe", e); }
    }

    private static void guardar() {
        if (archivo == null) return;
        try {
            Files.createDirectories(archivo.getParent());
            Path tmp = archivo.resolveSibling("ciudad.json.tmp");
            Files.writeString(tmp, GSON.toJson(VECINOS), StandardCharsets.UTF_8);
            Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { EmiCafe.LOG.error("No se pudo guardar la ciudad del cafe", e); }
    }

    /** La parcela de este jugador (la primera vez se le da la primera libre; despues siempre la misma). Actualiza su nombre. */
    static int slotDe(UUID id, String nombre) {
        Vecino v = VECINOS.get(id.toString());
        if (v == null) {
            v = new Vecino();
            outer:
            for (int i = 0; ; i++) { for (Vecino o : VECINOS.values()) if (o.slot == i) continue outer; v.slot = i; break; }
            VECINOS.put(id.toString(), v);
        }
        if (nombre != null && !nombre.isEmpty() && !nombre.equals(v.nombre)) v.nombre = nombre;
        guardar();
        return v.slot;
    }

    static boolean hay(int slot) { for (Vecino v : VECINOS.values()) if (v.slot == slot) return true; return false; }

    /** El nombre del dueño de esa parcela (null si nadie la tiene). */
    static String nombreDe(int slot) { for (Vecino v : VECINOS.values()) if (v.slot == slot) return v.nombre; return null; }

    static UUID duenioDe(int slot) {
        for (Map.Entry<String, Vecino> e : VECINOS.entrySet()) if (e.getValue().slot == slot) { try { return UUID.fromString(e.getKey()); } catch (Exception x) { return null; } }
        return null;
    }

    /** La parcela a la que pertenece una coordenada x del mundo (puede salir un slot que nadie tiene). */
    static int slotEn(int x) { return Math.floorDiv(x - X0, ANCHO); }

    /** El origen (x del bloque 0 del local) de una parcela. */
    static int origen(int slot) { return slot * ANCHO; }

    /** Todos los vecinos por orden de parcela (nombre y slot), para listados. */
    static java.util.List<Vecino> todos() {
        java.util.List<Vecino> l = new java.util.ArrayList<>(VECINOS.values());
        l.sort((a, b) -> Integer.compare(a.slot, b.slot));
        return l;
    }

    static Map<String, Vecino> copia() { return new HashMap<>(VECINOS); }
}
