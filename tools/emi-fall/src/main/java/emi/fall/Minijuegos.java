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
        for (Mini m : new Mini[]{new MiniCarrera(), new MiniCamino(), new MiniBombas(), new MiniPlataformas()}) TODOS.put(m.id(), m);
    }

    static Mini porId(String id) { return TODOS.get(id.toLowerCase()); }
    static List<String> ids() { return List.copyOf(TODOS.keySet()); }
    static ServerWorld arena(MinecraftServer s) { return s.getWorld(DIM); }
}
