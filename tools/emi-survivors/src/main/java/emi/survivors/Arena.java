package emi.survivors;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Random;

/**
 * La arena de una partida: la «Pradera Michialien», una isla circular de hierba flotando en el vacio de la dimension {@code emi_survivors:arena}, con un muro de cristal
 * de amatista y farolas de luz perlada alrededor (los enemigos y el jugador no pueden salir), una luna creciente en el centro, flores, manchas de musgo, unas rocas y, fuera,
 * cuatro islotes con estatuas gigantes de los peluches del Michialien mirando a la arena. Cada hueco de partida tiene su propia isla (a 1024 bloques de la anterior) y se
 * construye la primera vez que se usa; queda guardada en el mundo. Si se cambia el diseño se sube {@link #VERSION}: la isla nueva se construye en otro sitio (otra fila
 * de Z), asi no hace falta limpiar la antigua.
 */
final class Arena {
    private Arena() {}

    /** Altura de los pies de quien esta en el suelo (la capa de hierba esta en y=-61). */
    static final double Y0 = -60.0;
    static final int SUELO = -61;
    /** Radio jugable (hasta el muro) y radio de la isla. */
    static final int RADIO = 46, RADIO_ISLA = 50;
    /** Version del diseño de la arena: 1 fue la llanura de la fase 0; 2 es la isla con muro de la fase 1. */
    static final int VERSION = 2;

    /** Centro (x, z) de la arena del hueco {@code slot}. */
    static double[] origen(int slot) { return new double[]{slot * 1024.0, (VERSION - 1) * 1024.0}; }

    /** ¿Esta el punto (x, z) dentro de la arena de esta partida, con un margen respecto al muro? */
    static boolean dentro(Equipo r, double x, double z, double margen) {
        double dx = x - r.ox, dz = z - r.oz, lim = RADIO - margen;
        return dx * dx + dz * dz <= lim * lim;
    }

    /** Construye la arena de ese hueco si todavia no existe. Devuelve true si la ha construido ahora (la primera vez tarda un segundo o dos). */
    static boolean preparar(ServerWorld w, int slot) {
        double[] o = origen(slot);
        int cx = (int) o[0], cz = (int) o[1];
        if (w.getBlockState(new BlockPos(cx, -64, cz)).isOf(Blocks.CRYING_OBSIDIAN)) return false;
        long t0 = System.nanoTime();
        construir(w, cx, cz, slot);
        Obra.poner(w, cx, -64, cz, Blocks.CRYING_OBSIDIAN.getDefaultState());           // la marca de «ya esta construida»
        EmiSurvivors.LOG.info("Arena {} construida en {} ms", slot, (System.nanoTime() - t0) / 1_000_000);
        return true;
    }

    private static void construir(ServerWorld w, int cx, int cz, int slot) {
        Random rnd = new Random(0xE31L * (slot + 1) + VERSION);
        BlockState hierba = Obra.b("grass_block"), tierra = Obra.b("dirt"), piedra = Obra.b("stone"), profundo = Obra.b("deepslate"), musgo = Obra.b("moss_block");
        BlockState luna = Obra.b("yellow_concrete");
        int R = RADIO_ISLA;
        boolean[][] cambiado = new boolean[2 * R + 1][2 * R + 1];            // celdas del suelo que no son hierba (luna, musgo): ahi no van flores

        // ---- el suelo: hierba sobre tierra, piedra y pizarra
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            if (dx * dx + dz * dz > R * R) continue;
            int x = cx + dx, z = cz + dz;
            Obra.poner(w, x, SUELO, z, hierba);
            Obra.poner(w, x, SUELO - 1, z, tierra);
            Obra.poner(w, x, SUELO - 2, z, piedra);
            Obra.poner(w, x, SUELO - 3, z, profundo);
        }
        // ---- dos anillos discontinuos de musgo (como las orbitas de la luna)
        for (int radio : new int[]{18, 34}) {
            for (double a = 0; a < Math.PI * 2; a += 0.012) {
                if (((int) (a * radio / 3.0)) % 3 == 2) continue;                    // tramos de 3 bloques con huecos
                int dx = (int) Math.round(Math.cos(a) * radio), dz = (int) Math.round(Math.sin(a) * radio);
                Obra.poner(w, cx + dx, SUELO, cz + dz, musgo);
                cambiado[dx + R][dz + R] = true;
            }
        }
        // ---- manchas de musgo
        for (int k = 0; k < 14; k++) {
            double ang = rnd.nextDouble() * Math.PI * 2, d = 8 + rnd.nextDouble() * 34, r = 2.0 + rnd.nextDouble() * 3.0;
            int px = (int) Math.round(Math.cos(ang) * d), pz = (int) Math.round(Math.sin(ang) * d);
            for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
                if (dx * dx + dz * dz > r * r || (px + dx) * (px + dx) + (pz + dz) * (pz + dz) > (RADIO - 2) * (RADIO - 2)) continue;
                Obra.poner(w, cx + px + dx, SUELO, cz + pz + dz, musgo);
                cambiado[px + dx + R][pz + dz + R] = true;
            }
        }
        // ---- la luna creciente del centro (la forma del dibujo de Emi): un disco al que se le quita otro desplazado
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
            boolean enDisco = dx * dx + dz * dz <= 6.4 * 6.4;
            boolean enHueco = (dx - 2.8) * (dx - 2.8) + (dz + 1.6) * (dz + 1.6) <= 5.3 * 5.3;
            if (enDisco && !enHueco) { Obra.poner(w, cx + dx, SUELO, cz + dz, luna); cambiado[dx + R][dz + R] = true; }
        }

        // ---- el muro de amatista con farolas de luz perlada
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < RADIO || d >= RADIO + 2.2) continue;
            Obra.poner(w, cx + dx, SUELO + 1, cz + dz, "amethyst_block");
            Obra.poner(w, cx + dx, SUELO + 2, cz + dz, "amethyst_block");
            Obra.poner(w, cx + dx, SUELO + 3, cz + dz, "pink_concrete");
        }
        for (int k = 0; k < 48; k++) {
            double a = k * Math.PI * 2 / 48;
            int dx = (int) Math.round(Math.cos(a) * (RADIO + 1)), dz = (int) Math.round(Math.sin(a) * (RADIO + 1));
            Obra.poner(w, cx + dx, SUELO + 4, cz + dz, "pearlescent_froglight");
            Obra.poner(w, cx + dx, SUELO + 5, cz + dz, "end_rod");
        }

        // ---- flores y hierba
        String[] flores = {"short_grass", "short_grass", "short_grass", "short_grass", "short_grass", "dandelion", "cornflower", "cornflower", "azure_bluet", "allium", "allium", "oxeye_daisy", "lily_of_the_valley", "pink_tulip"};
        for (int dx = -RADIO + 2; dx <= RADIO - 2; dx++) for (int dz = -RADIO + 2; dz <= RADIO - 2; dz++) {
            double d2 = dx * dx + dz * dz;
            if (d2 > (RADIO - 2) * (RADIO - 2) || d2 < 36 || cambiado[dx + R][dz + R]) continue;
            if (rnd.nextDouble() < 0.06) Obra.poner(w, cx + dx, SUELO + 1, cz + dz, flores[rnd.nextInt(flores.length)]);
        }
        // ---- unas rocas de 2x2x2 para esconderse y para que los enemigos tengan que rodearlas
        String[] rocas = {"mossy_cobblestone", "mossy_cobblestone", "andesite", "tuff"};
        for (int k = 0; k < 9; k++) {
            double ang = rnd.nextDouble() * Math.PI * 2, d = 12 + rnd.nextDouble() * 28;
            int px = cx + (int) Math.round(Math.cos(ang) * d), pz = cz + (int) Math.round(Math.sin(ang) * d);
            String roca = rocas[rnd.nextInt(rocas.length)];
            for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) for (int dy = 1; dy <= 2; dy++) Obra.poner(w, px + dx, SUELO + dy, pz + dz, roca);
        }

        // ---- cuatro islotes de cristal con los peluches gigantes, mirando a la arena
        String[] formas = {"michi_crown_20", "michi_halo_20", "michi_witch_hat_20", "michi_sunglasses_20"};
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            int sx = cx + (int) Math.round(Math.cos(a) * 86), sz = cz + (int) Math.round(Math.sin(a) * 86);
            for (int cap = 0; cap < 4; cap++) Obra.disco(w, sx, SUELO - cap, sz, 17 - cap * 3.0, Obra.b(cap < 2 ? "amethyst_block" : "calcite"));
            for (double b = 0; b < Math.PI * 2; b += 0.04) Obra.poner(w, sx + (int) Math.round(Math.cos(b) * 16), SUELO, sz + (int) Math.round(Math.sin(b) * 16), "pink_concrete");
            Obra.ponerEstatua(w, formas[k], sx, (int) Y0, sz, Obra.girarHacia(sx, sz, cx, cz));
        }
    }

    // ---------------------------------------------------------------- particulas por tipo

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
