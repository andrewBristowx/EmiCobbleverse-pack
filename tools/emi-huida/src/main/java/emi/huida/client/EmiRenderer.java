package emi.huida.client;

import emi.huida.EmiEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Dibuja a Emi (la skin del dueno, {@code textures/entity/emi.png}) con el modelo de jugador, algo mas grande que un jugador (para que se vea entre los michis). */
@Environment(EnvType.CLIENT)
public final class EmiRenderer extends LivingEntityRenderer<EmiEntity, EmiRenderer.Modelo> {
    private static final Identifier TEXTURA = Identifier.of("emi_huida", "textures/entity/emi.png");
    /** Cuanto mas grande que un jugador se dibuja. */
    static final float ESCALA = 1.9f;

    public EmiRenderer(EntityRendererFactory.Context ctx) { super(ctx, new Modelo(ctx.getPart(EntityModelLayers.PLAYER)), 0.55f); }

    @Override public Identifier getTexture(EmiEntity e) { return TEXTURA; }
    @Override protected void scale(EmiEntity e, MatrixStack m, float f) { m.scale(ESCALA, ESCALA, ESCALA); }
    @Override protected boolean hasLabel(EmiEntity e) { return false; }

    /** El modelo de jugador (brazos anchos) con las capas de la skin siempre visibles y los gestos de {@link EmiEntity#ESTADO}. */
    public static final class Modelo extends PlayerEntityModel<EmiEntity> {
        Modelo(ModelPart raiz) {
            super(raiz, false);
            hat.visible = true; jacket.visible = true; leftSleeve.visible = true; rightSleeve.visible = true; leftPants.visible = true; rightPants.visible = true;
        }

        @Override
        public void setAngles(EmiEntity e, float limbAngle, float limbDistance, float t, float headYaw, float headPitch) {
            super.setAngles(e, limbAngle, limbDistance, t, headYaw, headPitch);
            switch (e.estado()) {
                case 1 -> {                       // saluda con la mano derecha
                    rightArm.pitch = -2.7f; rightArm.roll = 0.15f + 0.45f * (float) Math.sin(t * 0.45f); rightArm.yaw = 0f;
                }
                case 2 -> {                       // brazos arriba, de alegria
                    rightArm.pitch = -2.9f; leftArm.pitch = -2.9f;
                    rightArm.roll = 0.3f + 0.2f * (float) Math.sin(t * 0.5f); leftArm.roll = -0.3f - 0.2f * (float) Math.sin(t * 0.5f);
                }
                case 3 -> {                       // «¡ay!»: las manos a las mejillas, ladeando la cabeza
                    rightArm.pitch = -1.45f; leftArm.pitch = -1.45f; rightArm.yaw = -0.85f; leftArm.yaw = 0.85f; rightArm.roll = 0.1f; leftArm.roll = -0.1f;
                    head.roll = 0.22f * (float) Math.sin(t * 0.3f);
                }
                default -> {}
            }
            leftSleeve.copyTransform(leftArm); rightSleeve.copyTransform(rightArm); hat.copyTransform(head);
        }
    }
}
