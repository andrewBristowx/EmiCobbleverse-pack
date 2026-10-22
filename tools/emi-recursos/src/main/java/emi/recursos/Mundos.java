package emi.recursos;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.server.network.SpawnLocating;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Los tres mundos de recursos del ciclo actual (overworld, nether y end), creados en caliente con Fantasy. */
public final class Mundos {
    static final String NS = "emi_recursos";

    private static RuntimeWorldHandle overworld, nether, end;

    public static boolean esRecursos(World w) { return w != null && NS.equals(w.getRegistryKey().getValue().getNamespace()); }
    public static boolean esRecursos(RegistryKey<World> k) { return k != null && NS.equals(k.getValue().getNamespace()); }

    public static ServerWorld overworld() { return overworld == null ? null : overworld.asWorld(); }
    public static ServerWorld nether() { return nether == null ? null : nether.asWorld(); }
    public static ServerWorld end() { return end == null ? null : end.asWorld(); }
    static boolean abiertos() { return overworld != null; }

    /** El punto de aparicion del overworld de recursos (Fantasy no guarda el spawn de sus mundos: usan el del mundo principal, asi que lo llevamos nosotros). */
    public static BlockPos spawn() {
        Estado.Ciclo c = Estado.ciclo();
        return c == null || !c.hayspawn ? BlockPos.ORIGIN : new BlockPos(c.spawnX, c.spawnY, c.spawnZ);
    }

    private static Identifier id(String tipo, int epoca) { return Identifier.of(NS, tipo + "_" + epoca); }

    /** Abre (o reabre, si ya existian en disco) los tres mundos del ciclo. */
    static void abrir(MinecraftServer s, Estado.Ciclo c) {
        Fantasy f = Fantasy.get(s);
        overworld = f.getOrOpenPersistentWorld(id("overworld", c.epoca), config(s, DimensionTypes.OVERWORLD, generadorOverworld(s), c.semilla, 1000));
        nether = f.getOrOpenPersistentWorld(id("nether", c.epoca), config(s, DimensionTypes.THE_NETHER, s.getWorld(World.NETHER).getChunkManager().getChunkGenerator(), c.semilla, 18000));
        end = f.getOrOpenPersistentWorld(id("end", c.epoca), config(s, DimensionTypes.THE_END, s.getWorld(World.END).getChunkManager().getChunkGenerator(), c.semilla, 6000));
        ponerDragon(c);
        if (!c.hayspawn) {
            BlockPos p = buscarSpawn(overworld.asWorld());
            c.spawnX = p.getX(); c.spawnY = p.getY(); c.spawnZ = p.getZ(); c.hayspawn = true;
            Estado.guardar();
        }
    }

    private static RuntimeWorldConfig config(MinecraftServer s, RegistryKey<net.minecraft.world.dimension.DimensionType> tipo, ChunkGenerator generador, long semilla, long hora) {
        return new RuntimeWorldConfig()
                .setDimensionType(tipo)
                .setGenerator(generador)
                .setSeed(semilla)
                .setShouldTickTime(true)
                .setTimeOfDay(hora)
                .setMirrorOverworldGameRules(true)
                .setMirrorOverworldDifficulty(true);
    }

    /**
     * El terreno del overworld de recursos es el del overworld de siempre (asi respeta los mods de biomas); si el mundo principal no es de ruido
     * (plano, depuracion...) se usa el terreno normal del juego, porque un mundo de recursos plano no tendria recursos.
     */
    private static ChunkGenerator generadorOverworld(MinecraftServer s) {
        ChunkGenerator principal = s.getOverworld().getChunkManager().getChunkGenerator();
        if (principal instanceof NoiseChunkGenerator) return principal;
        DynamicRegistryManager rm = s.getRegistryManager();
        return new NoiseChunkGenerator(
                MultiNoiseBiomeSource.create(rm.get(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).entryOf(MultiNoiseBiomeSourceParameterLists.OVERWORLD)),
                rm.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS).entryOf(ChunkGeneratorSettings.OVERWORLD));
    }

    /** Borra los tres mundos del ciclo que termina (Fantasy los descarga y borra sus archivos poco despues). */
    static void cerrar() {
        for (RuntimeWorldHandle h : new RuntimeWorldHandle[]{overworld, nether, end}) {
            if (h == null) continue;
            try { h.delete(); } catch (Throwable t) { EmiRecursos.LOG.error("No se pudo borrar un mundo de recursos", t); }
        }
        overworld = nether = end = null;
    }

    /** Un sitio de aparicion firme y en tierra, cerca del centro del mundo (como hace el juego al crear un mundo: espiral de chunks). */
    static BlockPos buscarSpawn(ServerWorld w) {
        ChunkPos centro = new ChunkPos(w.getChunkManager().getNoiseConfig().getMultiNoiseSampler().findBestSpawnPosition());
        for (int r = 0; r <= 8; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    BlockPos p = SpawnLocating.findServerSpawnPoint(w, new ChunkPos(centro.x + dx, centro.z + dz));
                    if (p != null) return p;
                }
            }
        }
        BlockPos base = centro.getStartPos().add(8, 0, 8);
        return w.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, base);
    }

    // ------------------------------------------------------------ la pelea del dragon del End de recursos

    /** El juego solo crea la pelea del dragon en el End de siempre; al de recursos se la ponemos nosotros (y la guardamos aparte, sin tocar la del End normal). */
    private static void ponerDragon(Estado.Ciclo c) {
        ServerWorld w = end.asWorld();
        EnderDragonFight.Data datos = EnderDragonFight.Data.DEFAULT;
        if (c.dragon != null) {
            try {
                NbtElement n = StringNbtReader.parse(c.dragon);
                datos = EnderDragonFight.Data.CODEC.parse(NbtOps.INSTANCE, n).resultOrPartial(m -> EmiRecursos.LOG.warn("Pelea del dragon guardada ilegible: {}", m)).orElse(EnderDragonFight.Data.DEFAULT);
            } catch (Exception e) { EmiRecursos.LOG.warn("No se pudo leer la pelea del dragon guardada", e); }
        }
        w.setEnderDragonFight(new EnderDragonFight(w, c.semilla, datos));
    }

    /** Apunta el estado de la pelea del dragon para no perderlo si se reinicia el servidor a mitad de ciclo. */
    static void guardarDragon() {
        Estado.Ciclo c = Estado.ciclo();
        if (c == null || end == null) return;
        EnderDragonFight f = end.asWorld().getEnderDragonFight();
        if (f == null) return;
        try {
            NbtElement n = EnderDragonFight.Data.CODEC.encodeStart(NbtOps.INSTANCE, f.toData()).getOrThrow();
            String s = n.toString();
            if (!s.equals(c.dragon)) { c.dragon = s; Estado.guardar(); }
        } catch (Exception e) { EmiRecursos.LOG.warn("No se pudo guardar la pelea del dragon", e); }
    }

    /** Apaga y borra a mano las carpetas de ciclos viejos (por si el servidor estuvo apagado en el reinicio o Fantasy no pudo borrarlas). */
    static void limpiarCarpetas(MinecraftServer s, int epocaActual) {
        Path base = s.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("dimensions").resolve(NS);
        if (!Files.isDirectory(base)) return;
        try (Stream<Path> l = Files.list(base)) {
            for (Path p : (Iterable<Path>) l::iterator) {
                String n = p.getFileName().toString();
                if (n.endsWith("_" + epocaActual)) continue;
                borrar(p);
            }
        } catch (IOException e) { EmiRecursos.LOG.warn("No se pudo revisar las carpetas viejas de recursos", e); }
    }

    static void borrar(Path p) {
        try (Stream<Path> w = Files.walk(p)) {
            w.sorted(Comparator.reverseOrder()).forEach(f -> { try { Files.deleteIfExists(f); } catch (IOException ignored) { } });
        } catch (IOException e) { EmiRecursos.LOG.warn("No se pudo borrar {}", p, e); }
    }

    /** Donde esta el spawn del mundo normal (el del overworld de siempre), con suelo bajo los pies. */
    static void alSpawnNormal(ServerPlayerEntity p) {
        ServerWorld w = p.getServer().getOverworld();
        BlockPos sp = w.getSpawnPos();
        BlockPos top = w.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, sp);
        p.teleport(w, top.getX() + 0.5, Math.max(top.getY(), sp.getY()), top.getZ() + 0.5, 0f, 0f);
    }
}
