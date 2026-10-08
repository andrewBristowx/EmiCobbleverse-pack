package emi.buildbattle;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Guarda la construccion de cada parcela como schematic de WorldEdit (formato Sponge v2, .schem) al terminar la partida:
 * config/worldedit/schematics/bb-<jugador>.schem. Se guardan los bloques (y los datos de cofres, carteles... ), sin el suelo
 * de partida que nadie toco, sin el muro y sin las barreras de la jaula. Las entidades (soportes de armadura, cuadros...) no.
 */
public final class Schem {
    private Schem() {}

    /** Encola un trabajo por parcela con construccion; hay que llamarlo ANTES de limpiar la arena (la cola es FIFO). */
    public static void exportAll(ServerWorld w, Cfg cfg, List<Plot> plots) {
        if (!cfg.saveSchematics || w == null) return;
        Path dir = FabricLoader.getInstance().getGameDir().resolve(cfg.schematicDir);
        List<String> saved = new ArrayList<>();
        int total = 0;
        for (Plot p : plots) {
            if (p.ownerName == null || p.ownerName.equals("?")) continue;
            total++;
            final int[] x = {0};
            final BlockState[][] grid = new BlockState[1][];
            final int top = p.floorY + p.height, y0 = p.floorY - 2, h = top - y0 + 1;
            PlotOps.enqueue(budget -> {
                if (grid[0] == null) grid[0] = new BlockState[p.size * h * p.size];
                BlockPos.Mutable m = new BlockPos.Mutable();
                int cols = Math.max(1, budget / (p.size * h));
                for (int c = 0; c < cols && x[0] < p.size; c++, x[0]++) {
                    for (int z = 0; z < p.size; z++) for (int yy = 0; yy < h; yy++) {
                        int y = y0 + yy;
                        BlockState s = w.getBlockState(m.set(p.ox + x[0], y, p.oz + z));
                        grid[0][((yy * p.size) + z) * p.size + x[0]] = untouched(s, p.floorY, y, cfg) ? null : s;
                    }
                }
                if (x[0] < p.size) return false;
                try {
                    String name = write(w, cfg, dir, p, grid[0], y0, h);
                    if (name != null) saved.add(name);
                } catch (Exception e) {
                    EmiBuildBattle.LOG.error("No se pudo guardar el schematic de {}", p.ownerName, e);
                }
                return true;
            });
        }
        final int expected = total;
        PlotOps.enqueue(budget -> {
            if (expected > 0) {
                var server = Game.server();
                if (server != null) Msg.broadcast(server, Msg.say(saved.isEmpty()
                        ? "No había construcciones que guardar como schematic."
                        : "Guardadas " + saved.size() + " construcciones como schematic de WorldEdit: " + String.join(", ", saved)
                        + "  (usa //schem load <nombre>)"));
            }
            return true;
        });
    }

    /** Bloques que no son del jugador: aire, barreras de la jaula y el suelo original de la parcela sin tocar. */
    private static boolean untouched(BlockState s, int floorY, int y, Cfg cfg) {
        if (s.isAir() || s.isOf(Blocks.BARRIER)) return true;
        if (y == floorY) return s.isOf(blockOf(cfg.floorBlock, Blocks.GRASS_BLOCK)) || s.isOf(Blocks.DIRT);   // el cesped tapado se convierte en tierra solo
        if (y == floorY - 1 || y == floorY - 2) return s.isOf(Blocks.DIRT);
        return false;
    }

    private static Block blockOf(String id, Block fallback) {
        var i = net.minecraft.util.Identifier.tryParse(id);
        if (i == null) return fallback;
        Block b = Registries.BLOCK.get(i);
        return b == Blocks.AIR ? fallback : b;
    }

    private static String write(ServerWorld w, Cfg cfg, Path dir, Plot p, BlockState[] grid, int y0, int h) throws IOException {
        int n = p.size;
        int minX = n, minY = h, minZ = n, maxX = -1, maxY = -1, maxZ = -1;
        for (int yy = 0; yy < h; yy++) for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
            if (grid[((yy * n) + z) * n + x] == null) continue;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, yy); maxY = Math.max(maxY, yy);
            minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
        }
        if (maxX < 0) return null;   // no construyo nada
        int W = maxX - minX + 1, H = maxY - minY + 1, L = maxZ - minZ + 1;

        Map<String, Integer> palette = new LinkedHashMap<>();
        palette.put("minecraft:air", 0);
        ByteArrayOutputStream data = new ByteArrayOutputStream(W * H * L);
        NbtList bes = new NbtList();
        for (int yy = minY; yy <= maxY; yy++) for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++) {
            BlockState s = grid[((yy * n) + z) * n + x];
            int id = 0;
            if (s != null) {
                String key = BlockArgumentParser.stringifyBlockState(s);
                Integer got = palette.get(key);
                if (got == null) { got = palette.size(); palette.put(key, got); }
                id = got;
                if (s.hasBlockEntity()) {
                    BlockEntity be = w.getBlockEntity(new BlockPos(p.ox + x, y0 + yy, p.oz + z));
                    if (be != null) {
                        NbtCompound raw = be.createNbtWithIdentifyingData(w.getRegistryManager());
                        NbtCompound out = new NbtCompound();
                        for (String k : raw.getKeys()) if (!k.equals("id") && !k.equals("x") && !k.equals("y") && !k.equals("z")) out.put(k, raw.get(k));
                        out.putString("Id", raw.getString("id"));
                        out.putIntArray("Pos", new int[]{x - minX, yy - minY, z - minZ});
                        bes.add(out);
                    }
                }
            }
            while ((id & -128) != 0) { data.write(id & 127 | 128); id >>>= 7; }
            data.write(id);
        }

        NbtCompound pal = new NbtCompound();
        for (var e : palette.entrySet()) pal.putInt(e.getKey(), e.getValue());
        NbtCompound root = new NbtCompound();
        root.putInt("Version", 2);
        root.putInt("DataVersion", SharedConstants.getGameVersion().getSaveVersion().getId());
        root.putShort("Width", (short) W);
        root.putShort("Height", (short) H);
        root.putShort("Length", (short) L);
        root.putIntArray("Offset", new int[]{0, 0, 0});
        root.putInt("PaletteMax", palette.size());
        root.put("Palette", pal);
        root.put("BlockData", new NbtByteArray(data.toByteArray()));
        if (!bes.isEmpty()) root.put("BlockEntities", bes);
        NbtCompound meta = new NbtCompound();
        meta.putString("Name", "bb-" + p.ownerName);
        meta.putString("Author", p.ownerName);
        meta.putLong("Date", System.currentTimeMillis());
        root.put("Metadata", meta);

        Files.createDirectories(dir);
        String base = cfg.schematicPrefix + p.ownerName.replaceAll("[^A-Za-z0-9_\\-]", "_");
        Path file = dir.resolve(base + ".schem");
        if (Files.exists(file)) {   // no pisar el de una partida anterior: pasa a llevar la fecha
            String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(LocalDateTime.now());
            Files.move(file, dir.resolve(base + "-" + stamp + ".schem"), StandardCopyOption.REPLACE_EXISTING);
        }
        Path tmp = dir.resolve(base + ".schem.tmp");
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(tmp)))) {
            out.writeByte(NbtElement.COMPOUND_TYPE);
            out.writeUTF("Schematic");
            root.write(out);
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        EmiBuildBattle.LOG.info("Schematic guardado: {} ({}x{}x{}, {} bloques distintos)", file, W, H, L, palette.size());
        return base;
    }
}
