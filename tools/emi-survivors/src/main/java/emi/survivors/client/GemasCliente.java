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
        double suelo = mc.player.getY();
        double t = System.nanoTime() / 1e9;
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, TEX);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i < n; i++) {
            double dx = x[i] - cam.x, dz = z[i] - cam.z;
            if (dx * dx + dz * dz > 80 * 80) continue;
            int tp = tipo[i];
            float s = tp == 2 ? 0.62f : tp == 1 ? 0.42f : 0.30f;                   // medio lado del dibujo
            int r = tp == 2 ? 255 : tp == 1 ? 120 : 140, g = tp == 2 ? 170 : tp == 1 ? 240 : 205, b = tp == 2 ? 235 : tp == 1 ? 150 : 255;
            double bob = Math.sin(t * 3.0 + i * 1.7) * 0.07;
            m.push();
            m.translate(dx, suelo + 0.5 + s * 0.5 + bob - cam.y, dz);
            m.multiply(ctx.camera().getRotation());
            Matrix4f mat = m.peek().getPositionMatrix();
            bb.vertex(mat, -s, -s, 0).texture(0, 1).color(r, g, b, 255);
            bb.vertex(mat, s, -s, 0).texture(1, 1).color(r, g, b, 255);
            bb.vertex(mat, s, s, 0).texture(1, 0).color(r, g, b, 255);
            bb.vertex(mat, -s, s, 0).texture(0, 0).color(r, g, b, 255);
            m.pop();
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
