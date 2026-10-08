package emi.buildbattle;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/** Una parcela cuadrada de construccion. (ox, oz) es la esquina minima del interior. */
public final class Plot {
    public final int index;
    public final int ox, oz;
    public final int size, floorY, height;
    public final UUID owner;
    public final String ownerName;

    public Plot(int index, Cfg cfg, UUID owner, String ownerName) {
        this.index = index;
        this.ox = (index % cfg.plotsPerRow) * cfg.plotSpacing;
        this.oz = (index / cfg.plotsPerRow) * cfg.plotSpacing;
        this.size = cfg.plotSize;
        this.floorY = cfg.floorY;
        this.height = cfg.plotHeight;
        this.owner = owner;
        this.ownerName = ownerName;
    }

    /** Punto donde aparece el constructor: en el borde sur, mirando hacia dentro. */
    public Vec3d spawn() { return new Vec3d(ox + size / 2.0, floorY + 1, oz + 2.5); }
    public Vec3d npcPos() { return new Vec3d(ox + size / 2.0 + 3.5, floorY + 1, oz + 2.5); }
    public Vec3d labelPos() { return new Vec3d(ox + size / 2.0 + 3.5, floorY + 3.2, oz + 2.5); }

    /** Dentro del interior construible (el volumen sobre el suelo, sin el muro). */
    public boolean contains(BlockPos p) {
        return p.getX() >= ox && p.getX() < ox + size && p.getZ() >= oz && p.getZ() < oz + size
                && p.getY() > floorY - 3 && p.getY() <= floorY + height;
    }

    /** Se puede romper/colocar aqui: interior y por encima de la capa de base. */
    public boolean canBuildAt(BlockPos p) {
        return p.getX() >= ox && p.getX() < ox + size && p.getZ() >= oz && p.getZ() < oz + size
                && p.getY() > floorY - 3 && p.getY() <= floorY + height;
    }

    /** Ajusta una posicion para que quede dentro de los limites (margen extra opcional para ver desde fuera). */
    public Vec3d clamp(Vec3d v, int margin) {
        double minX = ox - margin + 0.3, maxX = ox + size + margin - 0.3;
        double minZ = oz - margin + 0.3, maxZ = oz + size + margin - 0.3;
        double minY = floorY - 1.5, maxY = floorY + height + (margin > 0 ? margin : 0) + 2;
        return new Vec3d(Math.max(minX, Math.min(maxX, v.x)), Math.max(minY, Math.min(maxY, v.y)), Math.max(minZ, Math.min(maxZ, v.z)));
    }

    public boolean outside(Vec3d v, int margin) {
        Vec3d c = clamp(v, margin);
        return c.x != v.x || c.y != v.y || c.z != v.z;
    }
}
