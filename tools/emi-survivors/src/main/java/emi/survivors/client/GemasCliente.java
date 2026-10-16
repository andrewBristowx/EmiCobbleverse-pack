package emi.survivors.client;

import com.mojang.blaze3d.systems.RenderSystem;
import emi.survivors.net.GemasPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Las gemas de experiencia (michiestrellitas) de la partida: no son entidades, las manda el servidor como una lista y las dibuja el cliente (carteles que miran a la
 * camara, con un balanceo suave). Pequena = azul, mediana = verde, grande = rosa dorado.
 */
@Environment(EnvType.CLIENT)
public final class GemasCliente {
    private GemasCliente() {}

    private static final Identifier TEX = Identifier.of("emi_survivors", "textures/gema.png");
    private static final Identifier OBJ = Identifier.of("emi_survivors", "textures/objetos.png");
    private static double[] x = new double[0], z = new double[0];
    private static byte[] tipo = new byte[0];

    public static void recibir(GemasPayload p) {
        int n = p.dx().length;
        double[] nx = new double[n], nz = new double[n];
        for (int i = 0; i < n; i++) { nx[i] = p.px() + p.dx()[i] / 16.0; nz[i] = p.pz() + p.dz()[i] / 16.0; }
        x = nx; z = nz; tipo = p.tipo();
    }

    public static void borrar() { x = new double[0]; z = new double[0]; tipo = new byte[0]; }

    public static int cuantas() { return x.length; }

    public static void dibujar(WorldRenderContext ctx) {
        int n = x.length;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (n == 0 || mc.player == null || !CamaraSurvivors.activa()) return;
        Vec3d cam = ctx.camera().getPos();
        MatrixStack m = ctx.matrixStack();
        if (m == null) return;
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        pasada(ctx, cam, m, mc.player.getY(), false);       // gemas
        pasada(ctx, cam, m, mc.player.getY(), true);        // objetos del suelo (baya, iman, bomba, caramelo)
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Dibuja o las gemas (tipo 0-2, con la textura de la michiestrellita) o los objetos (tipo 10-13, un dibujo de {@code objetos.png} cada uno, con un halo que late). */
    private static void pasada(WorldRenderContext ctx, Vec3d cam, MatrixStack m, double suelo, boolean objetos) {
        double t = System.nanoTime() / 1e9;
        RenderSystem.setShaderTexture(0, objetos ? OBJ : TEX);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int dibujados = 0;
        for (int i = 0; i < x.length; i++) {
            int tp = tipo[i];
            if ((tp >= 10) != objetos) continue;
            double dx = x[i] - cam.x, dz = z[i] - cam.z;
            if (dx * dx + dz * dz > 80 * 80) continue;
            float s = objetos ? 0.62f : tp == 2 ? 0.62f : tp == 1 ? 0.42f : 0.30f;                 // medio lado del dibujo
            int r = tp == 2 ? 255 : tp == 1 ? 120 : 140, g = tp == 2 ? 170 : tp == 1 ? 240 : 205, b = tp == 2 ? 235 : tp == 1 ? 150 : 255;
            if (objetos) { r = 255; g = 255; b = 255; }
            double bob = Math.sin(t * (objetos ? 4.0 : 3.0) + i * 1.7) * (objetos ? 0.14 : 0.07);
            float u0 = objetos ? (tp - 10) / 4f : 0f, u1 = objetos ? (tp - 9) / 4f : 1f;
            m.push();
            m.translate(dx, suelo + 0.5 + s * 0.5 + bob - cam.y + (objetos ? 0.25 : 0), dz);
            m.multiply(ctx.camera().getRotation());
            Matrix4f mat = m.peek().getPositionMatrix();
            if (objetos) {                                                              // el halo: el mismo dibujo, mas grande y casi transparente, que late
                float h = s * (1.25f + 0.12f * (float) Math.sin(t * 5.0 + i));
                int a = 70 + (int) (40 * Math.sin(t * 5.0 + i));
                bb.vertex(mat, -h, -h, 0).texture(u0, 1).color(255, 240, 160, a);
                bb.vertex(mat, h, -h, 0).texture(u1, 1).color(255, 240, 160, a);
                bb.vertex(mat, h, h, 0).texture(u1, 0).color(255, 240, 160, a);
                bb.vertex(mat, -h, h, 0).texture(u0, 0).color(255, 240, 160, a);
                dibujados++;
            }
            bb.vertex(mat, -s, -s, 0).texture(u0, 1).color(r, g, b, 255);
            bb.vertex(mat, s, -s, 0).texture(u1, 1).color(r, g, b, 255);
            bb.vertex(mat, s, s, 0).texture(u1, 0).color(r, g, b, 255);
            bb.vertex(mat, -s, s, 0).texture(u0, 0).color(r, g, b, 255);
            m.pop();
            dibujados++;
        }
        if (dibujados > 0) BufferRenderer.drawWithGlobalProgram(bb.end());
    }
}
