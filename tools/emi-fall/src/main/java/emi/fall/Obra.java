package emi.fall;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Ayudas para construir la sala de entrada desde el codigo: poner bloques sin avisar a los vecinos (mucho mas rapido). */
final class Obra {
    private Obra() {}

    /** Avisa a los clientes pero no a los bloques vecinos: para poner miles de bloques de golpe. */
    static final int BANDERAS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

    private static final Map<String, BlockState> ESTADOS = new HashMap<>();

    /** El estado por defecto de un bloque de Minecraft por su nombre (p. ej. {@code mossy_stone_bricks}). */
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
}
