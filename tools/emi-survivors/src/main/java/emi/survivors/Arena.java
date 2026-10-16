package emi.survivors;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.Map;
import java.util.Random;

/**
 * La arena de una partida: una isla circular flotando en el vacio, con un muro alrededor (los enemigos y el jugador no pueden salir), una luna creciente en el centro y, fuera,
 * cuatro islotes con estatuas gigantes de los peluches del Michialien mirando a la arena. Hay una por bioma ({@link Biomas}), cada una en su dimension: la «Pradera Michialien»
 * ({@code emi_survivors:arena}: hierba, flores, rocas y un muro de amatista), el «Bosque Embrujado» ({@code arena_bosque}: noche, suelo de pizarra con musgo, lapidas, calabazas y circulos de runas) y el
 * «Cosmos Michialien» ({@code arena_cosmos}: un damero magenta y purpura con las orbitas y los planetas pintados en el suelo). Cada hueco de partida tiene su propia isla (a 1024 bloques
 * de la anterior) y se construye la primera vez que se usa; queda guardada en el mundo. Si se cambia el diseño se sube {@link #VERSION}: la isla nueva se construye en otro sitio
 * (otra fila de Z), asi no hace falta limpiar la antigua. Lo que se pone dentro de la arena es BAJO (hasta 2 bloques) para que la camara cenital no tape al jugador.
 */
final class Arena {
    private Arena() {}

    /** Altura de los pies de quien esta en el suelo (la capa de hierba esta en y=-61). */
    static final double Y0 = -60.0;
    static final int SUELO = -61;
    /** Radio jugable (hasta el muro) y radio de la isla. */
    static final int RADIO = 70, RADIO_ISLA = 74;
    /** Cuanto mas grande es la arena que el diseño original (de radio 46): las distancias de los adornos se multiplican por esto. */
    static final double ESC = RADIO / 46.0;

    /** Una distancia del diseño original (radio 46) en esta arena. */
    static int esc(double v) { return (int) Math.round(v * ESC); }
    /** Version del diseño de la arena: 1 fue la llanura de la fase 0; 2 la isla con muro de radio 46 de la fase 1; 3 la isla grande de radio 70 (0.11.0, pedido del dueño: «se hace muy pequeño»). */
    static final int VERSION = 3;

    /** Centro (x, z) de la arena del hueco {@code slot}. */
    static double[] origen(int slot) { return new double[]{slot * 1024.0, (VERSION - 1) * 1024.0}; }

    /** ¿Esta el punto (x, z) dentro de la arena de esta partida, con un margen respecto al muro? */
    static boolean dentro(Equipo r, double x, double z, double margen) {
        double dx = x - r.ox, dz = z - r.oz, lim = RADIO - margen;
        return dx * dx + dz * dz <= lim * lim;
    }

    /**
     * Quita lo que haya quedado de una partida anterior en la arena de ese hueco (Pokemon enemigos y marcas de aviso con la etiqueta {@code emi_survivors}). Si el servidor
     * se paro en pleno ataque, esas entidades se guardaron con el mundo y {@link Partida#init} no las encuentra porque sus trozos de mapa no estan cargados todavia.
     */
    static void limpiarRestos(ServerWorld w, int slot) {
        double[] o = origen(slot);
        Box caja = new Box(o[0] - 100, w.getBottomY(), o[1] - 100, o[0] + 100, w.getTopY(), o[1] + 100);
        java.util.List<Entity> restos = w.getEntitiesByClass(Entity.class, caja, e -> e.getCommandTags().contains("emi_survivors"));
        for (Entity e : restos) e.discard();
        if (!restos.isEmpty()) EmiSurvivors.LOG.info("Se han quitado {} restos de una partida anterior de la arena {}.", restos.size(), slot);
    }

    /** Construye la arena de ese hueco en el mundo {@code w} (la dimension del bioma) si todavia no existe. Devuelve true si la ha construido ahora (la primera vez tarda un segundo o dos). */
    static boolean preparar(ServerWorld w, int slot, Biomas.Bioma bioma) {
        double[] o = origen(slot);
        int cx = (int) o[0], cz = (int) o[1];
        if (w.getBlockState(new BlockPos(cx, -64, cz)).isOf(Blocks.CRYING_OBSIDIAN)) return false;
        long t0 = System.nanoTime();
        switch (bioma.id()) {
            case "bosque" -> construirBosque(w, cx, cz, slot);
            case "cosmos" -> construirCosmos(w, cx, cz, slot);
            default -> construirPradera(w, cx, cz, slot);
        }
        Obra.poner(w, cx, -64, cz, Blocks.CRYING_OBSIDIAN.getDefaultState());           // la marca de «ya esta construida»
        EmiSurvivors.LOG.info("Arena {} ({}) construida en {} ms", slot, bioma.id(), (System.nanoTime() - t0) / 1_000_000);
        return true;
    }

    /** Solo para probar (/survivor dev reconstruir): borra toda la arena del hueco (isla, muro, islotes con estatuas y la marca) y la vuelve a construir; tarda unos segundos. */
    static void reconstruir(ServerWorld w, int slot, Biomas.Bioma bioma) {
        double[] o = origen(slot);
        int cx = (int) o[0], cz = (int) o[1], R = 160;
        BlockState aire = Blocks.AIR.getDefaultState();
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) for (int y = -68; y <= -16; y++) {
            p.set(cx + dx, y, cz + dz);
            if (!w.getBlockState(p).isAir()) w.setBlockState(p, aire, Obra.BANDERAS);
        }
        preparar(w, slot, bioma);
    }

    // ---------------------------------------------------------------- pradera (el diseño de siempre)

    private static void construirPradera(ServerWorld w, int cx, int cz, int slot) {
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
        for (int radio : new int[]{esc(18), esc(34), esc(44)}) {
            for (double a = 0; a < Math.PI * 2; a += 0.012) {
                if (((int) (a * radio / 3.0)) % 3 == 2) continue;                    // tramos de 3 bloques con huecos
                int dx = (int) Math.round(Math.cos(a) * radio), dz = (int) Math.round(Math.sin(a) * radio);
                Obra.poner(w, cx + dx, SUELO, cz + dz, musgo);
                cambiado[dx + R][dz + R] = true;
            }
        }
        // ---- manchas de musgo
        for (int k = 0; k < 32; k++) {
            double ang = rnd.nextDouble() * Math.PI * 2, d = esc(8) + rnd.nextDouble() * esc(34), r = 2.0 + rnd.nextDouble() * 3.0;
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
        for (int k = 0; k < 72; k++) {
            double a = k * Math.PI * 2 / 72;
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
        for (int k = 0; k < 21; k++) {
            double ang = rnd.nextDouble() * Math.PI * 2, d = esc(12) + rnd.nextDouble() * esc(28);
            int px = cx + (int) Math.round(Math.cos(ang) * d), pz = cz + (int) Math.round(Math.sin(ang) * d);
            String roca = rocas[rnd.nextInt(rocas.length)];
            for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) for (int dy = 1; dy <= 2; dy++) Obra.poner(w, px + dx, SUELO + dy, pz + dz, roca);
        }

        // ---- cuatro islotes de cristal con los peluches gigantes, mirando a la arena
        String[] formas = {"michi_crown_20", "michi_halo_20", "michi_witch_hat_20", "michi_sunglasses_20"};
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            int sx = cx + (int) Math.round(Math.cos(a) * esc(86)), sz = cz + (int) Math.round(Math.sin(a) * esc(86));
            for (int cap = 0; cap < 4; cap++) Obra.disco(w, sx, SUELO - cap, sz, 17 - cap * 3.0, Obra.b(cap < 2 ? "amethyst_block" : "calcite"));
            for (double b = 0; b < Math.PI * 2; b += 0.04) Obra.poner(w, sx + (int) Math.round(Math.cos(b) * 16), SUELO, sz + (int) Math.round(Math.sin(b) * 16), "pink_concrete");
            Obra.ponerEstatua(w, formas[k], sx, (int) Y0, sz, Obra.girarHacia(sx, sz, cx, cz));
        }
    }

    // ---------------------------------------------------------------- piezas comunes de los biomas nuevos

    /** La isla redonda: una capa de arriba (el bloque de cada celda lo elige la funcion) y debajo tres capas de relleno. */
    private static void suelo(ServerWorld w, int cx, int cz, java.util.function.BiFunction<Integer, Integer, BlockState> arriba, BlockState b1, BlockState b2, BlockState b3) {
        int R = RADIO_ISLA;
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            if (dx * dx + dz * dz > R * R) continue;
            int x = cx + dx, z = cz + dz;
            Obra.poner(w, x, SUELO, z, arriba.apply(dx, dz));
            Obra.poner(w, x, SUELO - 1, z, b1);
            Obra.poner(w, x, SUELO - 2, z, b2);
            Obra.poner(w, x, SUELO - 3, z, b3);
        }
    }

    /** Un anillo de bloques a ras de suelo, de {@code radio}: continuo o a tramos con huecos (como las orbitas de la luna). Marca las celdas para que no se ponga nada encima. */
    private static void anillo(ServerWorld w, int cx, int cz, int radio, BlockState s, boolean[][] usado, boolean continuo) {
        int R = RADIO_ISLA;
        for (double a = 0; a < Math.PI * 2; a += 0.012) {
            if (!continuo && ((int) (a * radio / 3.0)) % 3 == 2) continue;
            int dx = (int) Math.round(Math.cos(a) * radio), dz = (int) Math.round(Math.sin(a) * radio);
            Obra.poner(w, cx + dx, SUELO, cz + dz, s);
            usado[dx + R][dz + R] = true;
        }
    }

    /** La luna creciente del centro (la forma del dibujo de Emi): un disco al que se le quita otro desplazado. */
    private static void luna(ServerWorld w, int cx, int cz, BlockState s, boolean[][] usado) {
        int R = RADIO_ISLA;
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
            boolean enDisco = dx * dx + dz * dz <= 6.4 * 6.4;
            boolean enHueco = (dx - 2.8) * (dx - 2.8) + (dz + 1.6) * (dz + 1.6) <= 5.3 * 5.3;
            if (enDisco && !enHueco) { Obra.poner(w, cx + dx, SUELO, cz + dz, s); usado[dx + R][dz + R] = true; }
        }
    }

    /** Un disco plano en el suelo (un planeta pintado, una mancha de tierra) con el centro a (px, pz) del centro de la arena. */
    private static void mancha(ServerWorld w, int cx, int cz, int px, int pz, double r, BlockState s, boolean[][] usado) {
        int R = RADIO_ISLA, ri = (int) Math.ceil(r);
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
            if (dx * dx + dz * dz > r * r) continue;
            int gx = px + dx, gz = pz + dz;
            if (gx * gx + gz * gz > (RADIO - 1) * (RADIO - 1)) continue;
            Obra.poner(w, cx + gx, SUELO, cz + gz, s);
            usado[gx + R][gz + R] = true;
        }
    }

    /** Los cuatro islotes de fuera con las estatuas gigantes de los peluches, mirando a la arena. */
    private static void islotes(ServerWorld w, int cx, int cz, String[] formas, String capa1, String capa2, String borde) {
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            int sx = cx + (int) Math.round(Math.cos(a) * esc(86)), sz = cz + (int) Math.round(Math.sin(a) * esc(86));
            for (int cap = 0; cap < 4; cap++) Obra.disco(w, sx, SUELO - cap, sz, 17 - cap * 3.0, Obra.b(cap < 2 ? capa1 : capa2));
            for (double b = 0; b < Math.PI * 2; b += 0.04) Obra.poner(w, sx + (int) Math.round(Math.cos(b) * 16), SUELO, sz + (int) Math.round(Math.sin(b) * 16), borde);
            Obra.ponerEstatua(w, formas[k], sx, (int) Y0, sz, Obra.girarHacia(sx, sz, cx, cz));
        }
    }

    /** El muro que no se puede cruzar (dos bloques de {@code bajo} y uno de {@code alto}) con 72 luces encima ({@code luz} y, si no es null, un {@code remate}). */
    private static void muro(ServerWorld w, int cx, int cz, String bajo, String alto, String luz, String remate) {
        int R = RADIO_ISLA;
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < RADIO || d >= RADIO + 2.2) continue;
            Obra.poner(w, cx + dx, SUELO + 1, cz + dz, bajo);
            Obra.poner(w, cx + dx, SUELO + 2, cz + dz, bajo);
            Obra.poner(w, cx + dx, SUELO + 3, cz + dz, alto);
        }
        for (int k = 0; k < 72; k++) {
            double a = k * Math.PI * 2 / 72;
            int dx = (int) Math.round(Math.cos(a) * (RADIO + 1)), dz = (int) Math.round(Math.sin(a) * (RADIO + 1));
            Obra.poner(w, cx + dx, SUELO + 4, cz + dz, luz);
            if (remate != null) Obra.poner(w, cx + dx, SUELO + 5, cz + dz, remate);
        }
    }

    /** El bloque girado hacia {@code d} si tiene orientacion (una calabaza tallada); si no (una calabaza lisa), igual. */
    private static BlockState girado(BlockState s, Direction d) { return s.contains(HorizontalFacingBlock.FACING) ? s.with(HorizontalFacingBlock.FACING, d) : s; }

    /** Un sitio al azar dentro de la arena, entre {@code dMin} y {@code dMax} del centro, que no este ya usado; null si no lo encuentra en 30 intentos. */
    private static int[] sitioLibre(Random rnd, boolean[][] usado, double dMin, double dMax) {
        int R = RADIO_ISLA;
        for (int i = 0; i < 30; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, d = dMin + rnd.nextDouble() * (dMax - dMin);
            int dx = (int) Math.round(Math.cos(ang) * d), dz = (int) Math.round(Math.sin(ang) * d);
            if (!usado[dx + R][dz + R]) { usado[dx + R][dz + R] = true; return new int[]{dx, dz}; }
        }
        return null;
    }

    // ---------------------------------------------------------------- Bosque Embrujado (de noche)

    private static void construirBosque(ServerWorld w, int cx, int cz, int slot) {
        Random rnd = new Random(0xB05C0L * (slot + 1) + VERSION);
        int R = RADIO_ISLA;
        boolean[][] usado = new boolean[2 * R + 1][2 * R + 1];
        // el suelo es de pizarra oscura (asi los Pokemon de colores se ven bien desde arriba) con manchas de musgo
        BlockState pizarra = Obra.b("deepslate_tiles"), musgo = Obra.b("moss_block");
        suelo(w, cx, cz, (dx, dz) -> pizarra, Obra.b("deepslate"), Obra.b("deepslate"), Obra.b("deepslate"));
        for (int k = 0; k < 42; k++) {
            double ang = rnd.nextDouble() * Math.PI * 2, d = esc(6) + rnd.nextDouble() * esc(38), r = 2.0 + rnd.nextDouble() * 3.5;
            mancha(w, cx, cz, (int) Math.round(Math.cos(ang) * d), (int) Math.round(Math.sin(ang) * d), r, musgo, usado);
        }
        // ---- los circulos de runas (dos anillos a tramos de obsidiana llorona, que brilla) y la luna creciente de Emi, blanca
        anillo(w, cx, cz, esc(16), Obra.b("crying_obsidian"), usado, false);
        anillo(w, cx, cz, esc(32), Obra.b("crying_obsidian"), usado, false);
        anillo(w, cx, cz, esc(42), Obra.b("crying_obsidian"), usado, false);
        luna(w, cx, cz, Obra.b("calcite"), usado);
        // ---- ocho calabazas encendidas en el circulo de runas, mirando al centro
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            int dx = (int) Math.round(Math.cos(a) * esc(16)), dz = (int) Math.round(Math.sin(a) * esc(16));
            Direction mira = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
            Obra.poner(w, cx + dx, SUELO + 1, cz + dz, girado(Obra.b("jack_o_lantern"), mira));
        }
        // ---- lapidas (un pilar de dos bloques, a veces con una calavera), tumbas bajas, calabazas sueltas y farolas
        for (int k = 0; k < 36; k++) {
            int[] s = sitioLibre(rnd, usado, esc(9), esc(42));
            if (s == null) continue;
            Obra.poner(w, cx + s[0], SUELO + 1, cz + s[1], "stone_brick_wall");
            Obra.poner(w, cx + s[0], SUELO + 2, cz + s[1], "stone_brick_wall");
            if (rnd.nextDouble() < 0.4) Obra.poner(w, cx + s[0], SUELO + 3, cz + s[1], "skeleton_skull");
        }
        for (int k = 0; k < 11; k++) {                                             // filas de tumbas bajas (una losa de 3)
            int[] s = sitioLibre(rnd, usado, esc(9), esc(40));
            if (s == null) continue;
            boolean alX = rnd.nextBoolean();
            for (int t = -1; t <= 1; t++) {
                int gx = s[0] + (alX ? t : 0), gz = s[1] + (alX ? 0 : t);
                if (gx * gx + gz * gz > (RADIO - 3) * (RADIO - 3)) continue;
                Obra.poner(w, cx + gx, SUELO + 1, cz + gz, "stone_brick_slab");
            }
        }
        String[] calabazas = {"pumpkin", "carved_pumpkin", "jack_o_lantern", "jack_o_lantern"};
        for (int k = 0; k < 32; k++) {
            int[] s = sitioLibre(rnd, usado, esc(8), esc(42));
            if (s == null) continue;
            Obra.poner(w, cx + s[0], SUELO + 1, cz + s[1], girado(Obra.b(calabazas[rnd.nextInt(calabazas.length)]), Direction.fromHorizontal(rnd.nextInt(4))));
        }
        for (int k = 0; k < 12; k++) {                                             // doce farolas de alma azul en el anillo de fuera
            double a = k * Math.PI / 6 + 0.3;
            int dx = (int) Math.round(Math.cos(a) * esc(32)), dz = (int) Math.round(Math.sin(a) * esc(32));
            Obra.poner(w, cx + dx, SUELO + 1, cz + dz, "dark_oak_fence");
            Obra.poner(w, cx + dx, SUELO + 2, cz + dz, "dark_oak_fence");
            Obra.poner(w, cx + dx, SUELO + 3, cz + dz, "soul_lantern");
        }
        // ---- arbustos secos
        for (int dx = -RADIO + 2; dx <= RADIO - 2; dx++) for (int dz = -RADIO + 2; dz <= RADIO - 2; dz++) {
            double d2 = dx * dx + dz * dz;
            if (d2 > (RADIO - 2) * (RADIO - 2) || d2 < 36 || usado[dx + R][dz + R]) continue;
            if (rnd.nextDouble() < 0.04) Obra.poner(w, cx + dx, SUELO + 1, cz + dz, "dead_bush");
        }
        // ---- el muro de pizarra y piedra negra con calabazas encendidas, y los islotes con las estatuas (brujas y angelitos)
        muro(w, cx, cz, "deepslate_bricks", "blackstone", "jack_o_lantern", null);
        islotes(w, cx, cz, new String[]{"michi_witch_hat_20", "michi_halo_20", "michi_witch_hat_20", "michi_crown_20"}, "deepslate", "blackstone", "orange_concrete");
    }

    // ---------------------------------------------------------------- Cosmos Michialien (en el espacio)

    private static void construirCosmos(ServerWorld w, int cx, int cz, int slot) {
        Random rnd = new Random(0xC05C05L * (slot + 1) + VERSION);
        int R = RADIO_ISLA;
        boolean[][] usado = new boolean[2 * R + 1][2 * R + 1];
        BlockState magenta = Obra.b("magenta_concrete"), purpura = Obra.b("purple_concrete");
        suelo(w, cx, cz, (dx, dz) -> ((Math.floorDiv(dx, 4) + Math.floorDiv(dz, 4)) & 1) == 0 ? magenta : purpura, Obra.b("black_concrete"), Obra.b("obsidian"), Obra.b("obsidian"));
        // ---- tres orbitas pintadas en el suelo y, sobre ellas, planetas con su lunita (el sistema solar de Emi)
        for (int radio : new int[]{esc(14), esc(24), esc(36), esc(42)}) anillo(w, cx, cz, radio, Obra.b("white_concrete"), usado, true);
        record Planeta(int orbita, double grados, double r, String color, String luna) {}
        Planeta[] planetas = {
                new Planeta(esc(14), 35, 3.4, "orange_concrete", null), new Planeta(esc(24), 150, 4.4, "blue_concrete", "light_gray_concrete"), new Planeta(esc(24), 300, 3.2, "light_blue_concrete", null),
                new Planeta(esc(36), 70, 5.0, "yellow_terracotta", "white_concrete"), new Planeta(esc(36), 210, 3.8, "red_concrete", null), new Planeta(esc(36), 330, 3.0, "lime_concrete", null),
                new Planeta(esc(42), 120, 3.6, "purple_concrete", null), new Planeta(esc(42), 250, 2.8, "cyan_concrete", "white_concrete")};
        for (Planeta p : planetas) {
            double a = Math.toRadians(p.grados());
            int px = (int) Math.round(Math.cos(a) * p.orbita()), pz = (int) Math.round(Math.sin(a) * p.orbita());
            mancha(w, cx, cz, px, pz, p.r(), Obra.b(p.color()), usado);
            if (p.luna() != null) mancha(w, cx, cz, px + (int) Math.round(p.r() + 2.5), pz - (int) Math.round(p.r() * 0.6), 1.2, Obra.b(p.luna()), usado);
        }
        luna(w, cx, cz, Obra.b("yellow_concrete"), usado);
        // ---- estrellas: celdas sueltas de luz de mar a ras de suelo
        for (int k = 0; k < 160; k++) {
            int[] s = sitioLibre(rnd, usado, 4, RADIO - 2);
            if (s != null) Obra.poner(w, cx + s[0], SUELO, cz + s[1], "sea_lantern");
        }
        // ---- el muro de cristal morado con luz de mar y varas del End, y los islotes con las estatuas
        muro(w, cx, cz, "purple_stained_glass", "magenta_stained_glass", "sea_lantern", "end_rod");
        islotes(w, cx, cz, new String[]{"michi_crown_20", "michi_halo_20", "michi_witch_hat_20", "michi_sunglasses_20"}, "crying_obsidian", "obsidian", "sea_lantern");
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
