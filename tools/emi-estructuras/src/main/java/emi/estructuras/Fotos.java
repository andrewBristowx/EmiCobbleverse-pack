package emi.estructuras;

import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/** Diagnostico: dibuja una vista aerea y una vista lateral de una estructura colocada (colores del mapa), para revisarla sin entrar al juego. */
public final class Fotos {
    public static String hacer(ServerWorld w, Generador.Hecha h, int margen) throws Exception {
        int x0 = h.minX - margen, x1 = h.maxX + margen, z0 = h.minZ - margen, z1 = h.maxZ + margen;
        int y0 = Math.max(w.getBottomY(), h.minY - 12), y1 = Math.min(w.getTopY() - 1, h.maxY + 6);
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) w.getChunk(cx, cz);
        BlockPos.Mutable m = new BlockPos.Mutable();
        int W = x1 - x0 + 1, Dd = z1 - z0 + 1, H = y1 - y0 + 1;
        BufferedImage top = new BufferedImage(W, Dd, BufferedImage.TYPE_INT_RGB);
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            int rgb = 0x202020;
            for (int y = y1; y >= y0; y--) {
                BlockState s = w.getBlockState(m.set(x, y, z));
                if (s.isAir()) continue;
                rgb = color(w, m, s, y - y0, H);
                break;
            }
            top.setRGB(x - x0, z - z0, rgb);
        }
        BufferedImage side = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) {
            int rgb = 0x87CEEB;
            for (int z = z0; z <= z1; z++) {   // mirando hacia el norte desde el sur: primer bloque que se ve
                BlockState s = w.getBlockState(m.set(x, y, z));
                if (s.isAir()) continue;
                rgb = color(w, m, s, y - y0, H); rgb = shade(rgb, 1.0 - (z - z0) / (double) Dd * 0.35);
                break;
            }
            side.setRGB(x - x0, H - 1 - (y - y0), rgb);
        }
        // corte oeste-este por la z de la entrada: se ve la rampa/foso/plataforma
        int cx0 = Math.min(x0, h.entradaX - 8);
        for (int cx = cx0 >> 4; cx <= x1 >> 4; cx++) w.getChunk(cx, h.entradaZ >> 4);
        int cy0 = Math.max(w.getBottomY(), Math.min(y0, Generador.SUELO - 6)), cy1 = Math.max(y1, h.entradaY + 6);
        cy1 = Math.min(w.getTopY() - 1, cy1);
        BufferedImage cut = new BufferedImage(x1 - cx0 + 1, cy1 - cy0 + 1, BufferedImage.TYPE_INT_RGB);
        for (int x = cx0; x <= x1; x++) for (int y = cy0; y <= cy1; y++) {
            BlockState s = w.getBlockState(m.set(x, y, h.entradaZ));
            int rgb = s.isAir() ? 0x87CEEB : color(w, m, s, y - cy0, cy1 - cy0 + 1);
            if (x == h.entradaX && y == h.entradaY) rgb = 0xFF00FF;
            cut.setRGB(x - cx0, cy1 - y, rgb);
        }
        for (int k = 0; k < 5; k++) {   // cinco cortes a lo largo de la estructura (diagnostico de enterradas)
            int zz = h.minZ + (h.maxZ - h.minZ) * k / 4;
            BufferedImage c2 = new BufferedImage(x1 - x0 + 1, y1 - y0 + 1, BufferedImage.TYPE_INT_RGB);
            for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) {
                BlockState s = w.getBlockState(m.set(x, y, zz));
                c2.setRGB(x - x0, y1 - y, s.isAir() ? 0x87CEEB : color(w, m, s, y - y0, y1 - y0 + 1));
            }
            ImageIO.write(scale(c2, 3), "png", new File(w.getServer().getSavePath(WorldSavePath.ROOT).resolve("emi_estructuras").resolve("fotos").toFile(), h.id.replace(':', '_').replace('/', '_') + "_cz" + k + ".png"));
        }
        Path dir = w.getServer().getSavePath(WorldSavePath.ROOT).resolve("emi_estructuras").resolve("fotos");
        Files.createDirectories(dir);
        String base = h.id.replace(':', '_').replace('/', '_');
        ImageIO.write(scale(top, 3), "png", new File(dir.toFile(), base + "_aerea.png"));
        ImageIO.write(scale(side, 3), "png", new File(dir.toFile(), base + "_lateral.png"));
        ImageIO.write(scale(cut, 3), "png", new File(dir.toFile(), base + "_corte.png"));
        // fugas: liquidos fuera de la caja de la estructura por encima del suelo
        int fugas = 0;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = Generador.SUELO + 1; y <= y1; y++) {
            boolean dentro = x >= h.minX && x <= h.maxX && z >= h.minZ && z <= h.maxZ;
            if (dentro) continue;
            if (!w.getFluidState(m.set(x, y, z)).isEmpty()) fugas++;
        }
        return base + " fugas=" + fugas;
    }

    private static int color(ServerWorld w, BlockPos p, BlockState s, int yRel, int H) {
        if (!s.getFluidState().isEmpty() && s.getFluidState().getFluid() != Fluids.EMPTY) {
            return s.getFluidState().isIn(net.minecraft.registry.tag.FluidTags.LAVA) ? 0xFF6A00 : 0x3F76E4;
        }
        MapColor mc = s.getMapColor(w, p);
        int rgb = mc == MapColor.CLEAR ? 0x808080 : mc.color;
        return shade(rgb, 0.65 + 0.35 * yRel / Math.max(1, H));
    }

    private static int shade(int rgb, double f) {
        int r = (int) Math.min(255, ((rgb >> 16) & 255) * f), g = (int) Math.min(255, ((rgb >> 8) & 255) * f), b = (int) Math.min(255, (rgb & 255) * f);
        return (r << 16) | (g << 8) | b;
    }

    private static BufferedImage scale(BufferedImage src, int k) {
        BufferedImage out = new BufferedImage(src.getWidth() * k, src.getHeight() * k, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < out.getWidth(); x++) for (int y = 0; y < out.getHeight(); y++) out.setRGB(x, y, src.getRGB(x / k, y / k));
        return out;
    }
}
