package emi.huida.client;

import com.mojang.blaze3d.systems.RenderSystem;
import emi.huida.net.EstrellasPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Las michiestrellas (las del Michialien de emi-carreras): las dibuja el cliente, asi giran y suben y bajan sin gastar entidades en el servidor.
 * El servidor manda la lista de la copia del jugador y avisa de cada una que se coge.
 */
@Environment(EnvType.CLIENT)
public final class EstrellasCliente {
    private EstrellasCliente() {}

    private static final Identifier TEX = Identifier.of("emi_huida", "textures/estrella.png");
    private static float[] pos = new float[0];      // x, y, z, puntos
    private static boolean[] cogida = new boolean[0];

    public static void recibir(EstrellasPayload p) {
        switch (p.accion()) {
            case 0 -> { pos = p.datos(); cogida = new boolean[pos.length / 4]; if (Boolean.getBoolean("emi.huida.debug")) emi.huida.EmiHuida.LOG.info("estrellas: recibidas {}", cogida.length); }
            case 1 -> { if (p.idx() >= 0 && p.idx() < cogida.length) cogida[p.idx()] = true; }
            default -> { pos = new float[0]; cogida = new boolean[0]; }
        }
    }

    private static int modo = Integer.getInteger("emi.huida.dibujo", 0);
    private static long modoComprobado, ultimoLog;

    private static void leerModo() {
        String f = System.getProperty("emi.huida.dbgdibujo");
        long t = System.currentTimeMillis();
        if (f == null || t - modoComprobado < 1000) return;
        modoComprobado = t;
        try { java.nio.file.Path ph = java.nio.file.Path.of(f); if (java.nio.file.Files.exists(ph)) modo = Integer.parseInt(java.nio.file.Files.readString(ph).trim()); } catch (Exception ignored) {}
    }

    /** Variante A (modo 0, la normal): se dibuja despues de lo translucido con el programa de texturas y color. */
    public static void dibujar(WorldRenderContext ctx) { leerModo(); if (modo == 0 || modo == 1) dibujarDirecto(ctx, modo == 1); }

    /** Variante B (modo 2): con el proveedor de vertices de las entidades, justo despues de dibujarlas. */
    public static void dibujarConProveedor(WorldRenderContext ctx) {
        if (modo != 2 || pos.length == 0 || !CamaraHuida.activa()) return;
        Vec3d cam = ctx.camera().getPos();
        MatrixStack m = ctx.matrixStack();
        VertexConsumerProvider cons = ctx.consumers();
        if (cons == null || m == null) return;
        VertexConsumer vc = cons.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
        double t = System.nanoTime() / 1e9;
        for (int i = 0; i < cogida.length; i++) {
            if (cogida[i]) continue;
            double x = pos[i * 4] - cam.x, y = pos[i * 4 + 1] - cam.y, z = pos[i * 4 + 2] - cam.z;
            if (x * x + y * y + z * z > 70 * 70) continue;
            boolean grande = pos[i * 4 + 3] >= 5;
            float tam = grande ? 0.95f : 0.62f;
            m.push();
            m.translate(x, y + Math.sin(t * 2.4 + i * 1.7) * 0.12, z);
            m.multiply(RotationAxis.POSITIVE_Y.rotation((float) Math.toRadians((t * 150 + i * 53) % 360)));
            MatrixStack.Entry e = m.peek();
            int g = grande ? 235 : 255, b = grande ? 140 : 255;
            vc.vertex(e, -tam, -tam, 0).color(255, g, b, 255).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(e, 0, 0, 1);
            vc.vertex(e, tam, -tam, 0).color(255, g, b, 255).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(e, 0, 0, 1);
            vc.vertex(e, tam, tam, 0).color(255, g, b, 255).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(e, 0, 0, 1);
            vc.vertex(e, -tam, tam, 0).color(255, g, b, 255).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(e, 0, 0, 1);
            m.pop();
        }
    }

    private static void dibujarDirecto(WorldRenderContext ctx, boolean conMatrizPosicion) {
        if (pos.length == 0 || !CamaraHuida.activa()) return;
        Vec3d cam = ctx.camera().getPos();
        MatrixStack m = ctx.matrixStack();
        double t = System.nanoTime() / 1e9;
        net.minecraft.client.util.math.MatrixStack propia = new MatrixStack();
        if (conMatrizPosicion) propia.multiplyPositionMatrix(ctx.positionMatrix());
        MatrixStack base = conMatrizPosicion || m == null ? propia : m;
        org.joml.Matrix4fStack mv = RenderSystem.getModelViewStack();
        if (conMatrizPosicion) { mv.pushMatrix(); mv.identity(); RenderSystem.applyModelViewMatrix(); }
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, TEX);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int n = 0; double mejor = 1e9;
        for (int i = 0; i < cogida.length; i++) {
            if (cogida[i]) continue;
            double x = pos[i * 4] - cam.x, y = pos[i * 4 + 1] - cam.y, z = pos[i * 4 + 2] - cam.z;
            double d2 = x * x + y * y + z * z;
            if (d2 > 70 * 70) continue;
            mejor = Math.min(mejor, d2);
            boolean grande = pos[i * 4 + 3] >= 5;
            float tam = grande ? 0.95f : 0.62f;                     // medio lado del dibujo
            double giro = Math.toRadians((t * 150 + i * 53) % 360);
            double bob = Math.sin(t * 2.4 + i * 1.7) * 0.12;
            base.push();
            base.translate(x, y + bob, z);
            base.multiply(RotationAxis.POSITIVE_Y.rotation((float) giro));
            Matrix4f mat = base.peek().getPositionMatrix();
            int r = 255, g = grande ? 235 : 255, b = grande ? 140 : 255;
            bb.vertex(mat, -tam, -tam, 0).texture(0, 1).color(r, g, b, 255);
            bb.vertex(mat, tam, -tam, 0).texture(1, 1).color(r, g, b, 255);
            bb.vertex(mat, tam, tam, 0).texture(1, 0).color(r, g, b, 255);
            bb.vertex(mat, -tam, tam, 0).texture(0, 0).color(r, g, b, 255);
            base.pop();
            n++;
        }
        if (n > 0) BufferRenderer.drawWithGlobalProgram(bb.end());
        if (conMatrizPosicion) { mv.popMatrix(); RenderSystem.applyModelViewMatrix(); }
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        long ahora = System.currentTimeMillis();
        if (Boolean.getBoolean("emi.huida.debug") && ahora - ultimoLog > 3000) { ultimoLog = ahora; emi.huida.EmiHuida.LOG.info("estrellas: modo {} dibujadas {} de {}, la mas cercana a {} bloques", modo, n, cogida.length, Math.sqrt(mejor)); }
    }
}
