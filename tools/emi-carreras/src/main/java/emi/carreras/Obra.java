package emi.carreras;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ayudas para construir el mundo del minijuego desde el codigo (la arena y la sala de entrada): poner bloques sin avisar a los vecinos (mucho mas rapido) y colocar las
 * estatuas gigantes de los peluches del Michialien, que vienen de las pistas de emi-carreras como listas de bloques en {@code data/emi_survivors/estatuas/}.
 */
final class Obra {
    private Obra() {}

    /** Avisa a los clientes pero no a los bloques vecinos: para poner miles de bloques de golpe. */
    static final int BANDERAS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

    private static final Map<String, BlockState> ESTADOS = new HashMap<>();

    /** El estado por defecto de un bloque de Minecraft por su nombre (p. ej. {@code pink_concrete}). */
    static BlockState b(String nombre) {
        return ESTADOS.computeIfAbsent(nombre, n -> Registries.BLOCK.get(Identifier.of("minecraft", n)).getDefaultState());
    }

    static void poner(ServerWorld w, int x, int y, int z, BlockState s) { w.setBlockState(new BlockPos(x, y, z), s, BANDERAS); }

    static void poner(ServerWorld w, int x, int y, int z, String bloque) { poner(w, x, y, z, b(bloque)); }

    /** Un disco horizontal de radio {@code r} centrado en (cx, cz) a la altura {@code y}. */
    static void disco(ServerWorld w, int cx, int y, int cz, double r, BlockState s) {
        int ri = (int) Math.ceil(r);
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) if (dx * dx + dz * dz <= r * r) poner(w, cx + dx, y, cz + dz, s);
    }

    // ------------------------------------------------------------------------------------------------ estatuas

    /** Un bloque de una estatua, relativo al centro de sus pies. */
    record Voxel(int x, int y, int z, BlockState b) {}

    private static final Map<String, List<Voxel>> ESTATUAS = new HashMap<>();

    /** Los bloques de la estatua (p. ej. {@code michi_crown_20}); lista vacia si no se pudo leer. */
    static synchronized List<Voxel> estatua(String forma) {
        return ESTATUAS.computeIfAbsent(forma, f -> {
            List<Voxel> l = new ArrayList<>();
            try (InputStream in = Obra.class.getResourceAsStream("/data/emi_carreras/estatuas/" + f + ".json")) {
                if (in == null) { EmiCarreras.LOG.warn("Falta la estatua {}", f); return l; }
                JsonArray a = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
                for (JsonElement e : a) {
                    JsonArray v = e.getAsJsonArray();
                    l.add(new Voxel(v.get(0).getAsInt(), v.get(1).getAsInt(), v.get(2).getAsInt(), b(v.get(3).getAsString())));
                }
            } catch (Exception e) { EmiCarreras.LOG.warn("No se pudo leer la estatua {}: {}", f, e.toString()); }
            return l;
        });
    }

    /**
     * Pone una estatua con los pies en (cx, y0, cz) girada {@code veces} cuartos de vuelta (el frente de la estatua mira a -z con 0, a +x con 1, a +z con 2 y a -x con 3).
     * Devuelve el radio (en bloques) que ocupa.
     */
    static int ponerEstatua(ServerWorld w, String forma, int cx, int y0, int cz, int veces) {
        int radio = 0;
        for (Voxel v : estatua(forma)) {
            int x = v.x(), z = v.z();
            for (int k = 0; k < (veces & 3); k++) { int t = x; x = -z; z = t; }
            poner(w, cx + x, y0 + v.y(), cz + z, v.b());
            radio = Math.max(radio, Math.max(Math.abs(x), Math.abs(z)));
        }
        return radio;
    }

    /** Cuartos de vuelta para que una estatua colocada en (x, z) mire hacia (hx, hz). */
    static int girarHacia(double x, double z, double hx, double hz) {
        double fx = hx - x, fz = hz - z;
        return Math.abs(fz) >= Math.abs(fx) ? (fz < 0 ? 0 : 2) : (fx > 0 ? 1 : 3);
    }
}
