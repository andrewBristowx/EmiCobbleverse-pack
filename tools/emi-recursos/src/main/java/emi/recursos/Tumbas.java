package emi.recursos;

import eu.pb4.graves.other.GraveUtils;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/** Tumbas de los que mueren en los mundos de recursos: aparecen en su punto de cama (o en el spawn del mundo normal si no tienen). */
public final class Tumbas {
    private Tumbas() { }

    /** Devuelve true si la tumba se creo aqui (el codigo original de Universal Graves no debe hacer nada mas). */
    public static boolean crearEnLaCama(ServerPlayerEntity player, ServerWorld world, DamageSource source) {
        if (!Mundos.esRecursos(world)) return false;
        ServerWorld destino = player.getServer().getOverworld();
        BlockPos pos = null;
        BlockPos cama = player.getSpawnPointPosition();
        RegistryKey<World> dimCama = player.getSpawnPointDimension();
        if (cama != null && dimCama != null) {
            ServerWorld w = player.getServer().getWorld(dimCama);
            if (w != null && !Mundos.esRecursos(w)) { destino = w; pos = cama.up(); }
        }
        if (pos == null) {
            BlockPos sp = destino.getSpawnPos();
            pos = destino.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, sp);
        }
        World mundoOriginal = player.getWorld();
        Vec3d posOriginal = player.getPos();
        try {
            // Universal Graves mira el mundo y la posicion del jugador: se los cambiamos solo durante la llamada
            ((emi.recursos.mixin.EntityAccessor) player).emi$setWorld(destino);
            player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            GraveUtils.createGrave(player, destino, source);
        } catch (Throwable t) {
            EmiRecursos.LOG.error("No se pudo crear la tumba en la cama de {}", player.getGameProfile().getName(), t);
            return false;
        } finally {
            ((emi.recursos.mixin.EntityAccessor) player).emi$setWorld(mundoOriginal);
            player.setPosition(posOriginal);
        }
        player.sendMessage(Text.literal("[Recursos] ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("Moriste en el mundo de recursos: tu tumba esta en tu punto de cama del mundo normal.").formatted(Formatting.YELLOW)), false);
        return true;
    }
}
