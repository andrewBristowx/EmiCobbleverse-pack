package emi.plush;

import dev.mrshawn.pokeblocks.block.client.PokedollBlockRenderer;
import dev.mrshawn.pokeblocks.block.entity.PokedollBlockEntity;
import net.minecraft.class_5614;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public class EmiPlushClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MichiBlockModel model = new MichiBlockModel();
        class_5614<PokedollBlockEntity> factory = ctx -> new PokedollBlockRenderer(ctx, model);
        BlockEntityRendererRegistry.register(EmiPlush.MICHI_BE, factory);
        emi.plush.gen.DollRegistry.TIPOS.forEach((nombre, tipo) -> {
            EmiDollModel m = new EmiDollModel(nombre);
            @SuppressWarnings({"unchecked", "rawtypes"})
            net.minecraft.class_2591<PokedollBlockEntity> t = (net.minecraft.class_2591) tipo;
            BlockEntityRendererRegistry.register(t, (class_5614<PokedollBlockEntity>) ctx -> new PokedollBlockRenderer(ctx, m));
        });
    }
}
