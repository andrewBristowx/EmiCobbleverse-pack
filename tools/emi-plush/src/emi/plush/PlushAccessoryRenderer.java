package emi.plush;

import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_572;
import net.minecraft.class_583;
import net.minecraft.class_7833;
import net.minecraft.class_811;

/**
 * Dibuja el peluche en la cabeza cuando va en la ranura «hat» (sombrero) del mod Accessories (o su capa de cosmeticos), igual que lo dibuja el juego
 * con un objeto en el casco (mismo recorrido que HeadFeatureRenderer). Esta clase solo se carga si Accessories esta instalado (ver EmiPlushClient).
 * Nombres intermediary: class_572 = BipedEntityModel, class_583 = EntityModel, class_811 = ModelTransformationMode.
 */
final class PlushAccessoryRenderer implements AccessoryRenderer {
    @Override
    public <M extends class_1309> void render(class_1799 stack, SlotReference ref, class_4587 ms, class_583<M> model, class_4597 buffers, int light,
                                              float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(model instanceof class_572<?> biped)) return;
        ms.method_22903();
        biped.field_3398.method_22703(ms);              // ModelPart.rotate: va al pivote de la cabeza y la gira como ella (igual que HeadFeatureRenderer)
        ms.method_46416(0.0f, -0.25f, 0.0f);
        ms.method_22907(class_7833.field_40716.rotationDegrees(180.0f));
        ms.method_22905(0.625f, -0.625f, -0.625f);
        class_1309 e = ref.entity();
        class_310.method_1551().method_1480().method_23178(stack, class_811.field_4316, light, net.minecraft.class_4608.field_21444, ms, buffers, e.method_37908(), e.method_5628());
        ms.method_22909();
    }

    /** Registra el renderizador para todos los peluches de emi_plush. */
    static void registrar() {
        emi.plush.gen.DollRegistry.OBJETOS.values().forEach(i -> AccessoriesRendererRegistry.registerRenderer(i, PlushAccessoryRenderer::new));
        AccessoriesRendererRegistry.registerRenderer(EmiPlush.MICHI_ITEM, PlushAccessoryRenderer::new);
    }
}
