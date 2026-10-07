package emi.plush;

import dev.mrshawn.pokeblocks.block.entity.BlockEntityTypeRegistry;
import dev.mrshawn.pokeblocks.block.entity.PokedollBlockEntity;
import net.minecraft.class_2338;
import net.minecraft.class_2680;

public class MichiBlockEntity extends PokedollBlockEntity {
    public MichiBlockEntity(class_2338 pos, class_2680 state) {
        super(BlockEntityTypeRegistry.get(MichiBlockEntity.class), pos, state);
    }
}
