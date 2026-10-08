package emi.buildbattle;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Guarda en disco lo que el jugador tenia antes de entrar al minijuego (inventario, posicion, modo de juego y punto de
 * reaparicion) y se lo devuelve despues. Esta en disco para sobrevivir a un reinicio del servidor.
 */
public final class Saved {
    private static Path file(MinecraftServer server, UUID id) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("emi_buildbattle").resolve("saved").resolve(id + ".nbt");
    }

    public static boolean exists(MinecraftServer server, UUID id) { return Files.exists(file(server, id)); }

    public static void save(MinecraftServer server, ServerPlayerEntity p) {
        NbtCompound t = new NbtCompound();
        NbtList inv = new NbtList();
        p.getInventory().writeNbt(inv);
        t.put("inv", inv);
        t.putString("dim", p.getServerWorld().getRegistryKey().getValue().toString());
        t.putDouble("x", p.getX()); t.putDouble("y", p.getY()); t.putDouble("z", p.getZ());
        t.putFloat("yaw", p.getYaw()); t.putFloat("pitch", p.getPitch());
        t.putInt("mode", p.interactionManager.getGameMode().getId());
        BlockPos sp = p.getSpawnPointPosition();
        if (sp != null) {
            t.putLong("spawnPos", sp.asLong());
            t.putString("spawnDim", p.getSpawnPointDimension().getValue().toString());
            t.putFloat("spawnAngle", p.getSpawnAngle());
            t.putBoolean("spawnForced", p.isSpawnForced());
        }
        try {
            Path f = file(server, p.getUuid());
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            NbtIo.writeCompressed(t, tmp);
            Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            EmiBuildBattle.LOG.error("No se pudo guardar el inventario de {}", p.getGameProfile().getName(), e);
            throw new RuntimeException(e);
        }
    }

    /** Devuelve todo al jugador y borra el archivo. false si no habia nada guardado. */
    public static boolean restore(MinecraftServer server, ServerPlayerEntity p) {
        Path f = file(server, p.getUuid());
        if (!Files.exists(f)) return false;
        NbtCompound t;
        try {
            t = NbtIo.readCompressed(f, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
        } catch (IOException e) {
            EmiBuildBattle.LOG.error("No se pudo leer el inventario guardado de {}", p.getGameProfile().getName(), e);
            return false;
        }
        p.getInventory().readNbt(t.getList("inv", NbtElement.COMPOUND_TYPE));
        p.currentScreenHandler.sendContentUpdates();
        p.playerScreenHandler.sendContentUpdates();
        p.getInventory().markDirty();

        RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(t.getString("dim")));
        ServerWorld w = server.getWorld(dimKey);
        double x = t.getDouble("x"), y = t.getDouble("y"), z = t.getDouble("z");
        float yaw = t.getFloat("yaw"), pitch = t.getFloat("pitch");
        if (w == null) {
            w = server.getOverworld();
            BlockPos s = w.getSpawnPos();
            x = s.getX() + 0.5; y = s.getY(); z = s.getZ() + 0.5;
        }
        p.teleport(w, x, y, z, yaw, pitch);
        p.changeGameMode(GameMode.byId(t.getInt("mode")));
        p.getAbilities().flying = false;
        if (t.contains("spawnPos")) {
            RegistryKey<World> sd = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(t.getString("spawnDim")));
            p.setSpawnPoint(sd, BlockPos.fromLong(t.getLong("spawnPos")), t.getFloat("spawnAngle"), t.getBoolean("spawnForced"), false);
        } else {
            p.setSpawnPoint(World.OVERWORLD, null, 0f, false, false);
        }
        p.sendAbilitiesUpdate();
        try { Files.deleteIfExists(f); } catch (IOException e) { EmiBuildBattle.LOG.warn("No se pudo borrar {}", f, e); }
        return true;
    }
}
