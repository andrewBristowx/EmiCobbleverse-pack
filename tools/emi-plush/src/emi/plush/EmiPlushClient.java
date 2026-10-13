package emi.plush;

import dev.mrshawn.pokeblocks.block.client.PokedollBlockRenderer;
import dev.mrshawn.pokeblocks.block.entity.PokedollBlockEntity;
import net.minecraft.class_5614;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public class EmiPlushClient implements ClientModInitializer {
    /** los gigantes de Pokeblocks se dibujan al doble */
    private static final float ESCALA_GIGANTE = 2.0f;

    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(EmiPlush.MAQUINA_GASHA_BE, GashaMachineRenderer::new);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(GashaRespuesta.ID, (p, ctx) -> ctx.client().execute(() -> GashaPantalla.recibir(p.json())));
        MichiBlockModel model = new MichiBlockModel();
        class_5614<PokedollBlockEntity> factory = ctx -> new PokedollBlockRenderer(ctx, model);
        BlockEntityRendererRegistry.register(EmiPlush.MICHI_BE, factory);
        emi.plush.gen.DollRegistry.TIPOS.forEach((nombre, tipo) -> {
            EmiDollModel m = new EmiDollModel(nombre);
            @SuppressWarnings({"unchecked", "rawtypes"})
            net.minecraft.class_2591<PokedollBlockEntity> t = (net.minecraft.class_2591) tipo;
            BlockEntityRendererRegistry.register(t, (class_5614<PokedollBlockEntity>) ctx -> new PokedollBlockRenderer(ctx, m));
        });
        // Accessories (opcional): los peluches se ven en la ranura de sombrero / cosmeticos
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("accessories")) PlushAccessoryRenderer.registrar();
        // peluches gigantes: el mismo modelo que el normal, dibujado al doble (renderizador escalado de Pokeblocks)
        emi.plush.gen.DollRegistry.TIPOS_GIGANTES.forEach((nombre, tipo) -> {
            EmiDollModel m = new EmiDollModel(emi.plush.gen.DollRegistry.MODELO_GIGANTE.get(nombre));
            @SuppressWarnings({"unchecked", "rawtypes"})
            net.minecraft.class_2591<PokedollBlockEntity> t = (net.minecraft.class_2591) tipo;
            BlockEntityRendererRegistry.register(t, (class_5614<PokedollBlockEntity>) ctx -> new dev.mrshawn.pokeblocks.block.client.PokedollScaledBlockRenderer(ctx, m, ESCALA_GIGANTE));
        });
    }
}
