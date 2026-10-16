package emi.survivors;

import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** La arena de la fase 0: el mundo plano {@code emi_survivors:arena} (hierba infinita) con unas cuantas rocas puestas al empezar y quitadas al acabar. */
final class Arena {
    private Arena() {}

    /** Altura de los pies de quien esta en el suelo (la capa de hierba esta en y=-61). */
    static final double Y0 = -60.0;

    /**
     * Pone {@code n} rocas de 2x2x2 entre 10 y 42 bloques del centro (para probar colisiones); devuelve las posiciones de todos sus bloques.
     * Debajo de cada roca se pone piedra: la hierba con un bloque encima se convierte en tierra y se quedaria asi al quitar la roca.
     */
    static List<BlockPos> colocarRocas(ServerWorld w, Random rnd, int n, int ox, int oz) {
        List<BlockPos> bloques = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, r = 10 + rnd.nextDouble() * 32;
            int x = ox + (int) Math.round(Math.cos(ang) * r), z = oz + (int) Math.round(Math.sin(ang) * r);
            for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) {
                BlockPos suelo = new BlockPos(x + dx, (int) Y0 - 1, z + dz);
                w.setBlockState(suelo, Blocks.STONE.getDefaultState());
                bloques.add(suelo);
                for (int dy = 0; dy < 2; dy++) {
                    BlockPos p = new BlockPos(x + dx, (int) Y0 + dy, z + dz);
                    w.setBlockState(p, Blocks.MOSSY_COBBLESTONE.getDefaultState());
                    bloques.add(p);
                }
            }
        }
        return bloques;
    }

    /** ¿Esta el punto (x, z) dentro de la arena de esta partida, con un margen respecto al borde? (por ahora toda la llanura vale; la arena con muro llega despues) */
    static boolean dentro(Run r, double x, double z, double margen) { return true; }

    /** Quita las rocas y devuelve la hierba a su sitio (los bloques a la altura del suelo se vuelven hierba; los de encima, aire). */
    static void quitarRocas(ServerWorld w, List<BlockPos> bloques) {
        for (BlockPos p : bloques) w.setBlockState(p, p.getY() < (int) Y0 ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState());
        bloques.clear();
    }

    private static final Map<String, ParticleEffect> POR_TIPO = Map.ofEntries(
            Map.entry("fire", ParticleTypes.FLAME), Map.entry("water", ParticleTypes.BUBBLE), Map.entry("electric", ParticleTypes.ELECTRIC_SPARK),
            Map.entry("grass", ParticleTypes.HAPPY_VILLAGER), Map.entry("ice", ParticleTypes.SNOWFLAKE), Map.entry("fighting", ParticleTypes.CRIT),
            Map.entry("poison", ParticleTypes.WITCH), Map.entry("ground", ParticleTypes.SMOKE), Map.entry("flying", ParticleTypes.CLOUD),
            Map.entry("psychic", ParticleTypes.ENCHANT), Map.entry("bug", ParticleTypes.SPORE_BLOSSOM_AIR), Map.entry("rock", ParticleTypes.CRIT),
            Map.entry("ghost", ParticleTypes.SOUL_FIRE_FLAME), Map.entry("dragon", ParticleTypes.DRAGON_BREATH), Map.entry("dark", ParticleTypes.LARGE_SMOKE),
            Map.entry("steel", ParticleTypes.CRIT), Map.entry("fairy", ParticleTypes.END_ROD), Map.entry("normal", ParticleTypes.POOF));

    /** Las particulas del disparo segun el tipo del movimiento (por ahora el tipo principal del Pokemon del jugador). */
    static ParticleEffect particula(String tipo) { return POR_TIPO.getOrDefault(tipo, ParticleTypes.CRIT); }
}
