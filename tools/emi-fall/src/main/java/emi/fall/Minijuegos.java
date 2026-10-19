package emi.fall;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** La lista de minijuegos del torneo y la dimension donde estan todas las arenas ({@code emi_fall:arena}). Cada arena esta en su sitio (z distinto) para no mezclarse. */
final class Minijuegos {
    private Minijuegos() {}

    static final RegistryKey<World> DIM = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("emi_fall", "arena"));
    /** Altura del suelo de todas las arenas (los pies de los jugadores estan en SUELO + 1). */
    static final int SUELO = 63;

    private static final Map<String, Mini> TODOS = new LinkedHashMap<>();
    static {
        for (Mini m : new Mini[]{new MiniCarrera(), new MiniCaramelos(), new MiniCamino(), new MiniSuelo(), new MiniBombas(), new MiniMuro(), new MiniPlataformas(), new MiniPilla()}) TODOS.put(m.id(), m);
    }

    static Mini porId(String id) { return TODOS.get(id.toLowerCase()); }
    static List<String> ids() { return List.copyOf(TODOS.keySet()); }
    static ServerWorld arena(MinecraftServer s) { return s.getWorld(DIM); }

    /** Quita todos los chunks forzados de la dimension de las arenas (nadie mas los usa): se llama al arrancar el servidor por si se cerro en plena ronda. */
    static void soltarTodoForzado(MinecraftServer s) {
        ServerWorld w = arena(s);
        if (w == null) return;
        for (long l : new java.util.ArrayList<>(w.getForcedChunks())) w.setChunkForced(net.minecraft.util.math.ChunkPos.getPackedX(l), net.minecraft.util.math.ChunkPos.getPackedZ(l), false);
    }
}
