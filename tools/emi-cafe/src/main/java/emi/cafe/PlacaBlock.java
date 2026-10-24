package emi.cafe;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Una placa fina pegada a la cara norte de su casilla (contra la pared de detras): una imagen. La usan el letrero de la puerta («cartel»: dos casillas, mitad izquierda y derecha,
 * con las propiedades {@code abierto} y {@code der}) y el libro de recetas de la cocina («libro_pared»). No tiene colision: solo se puede apuntar a ella para hacer clic.
 */
public final class PlacaBlock extends Block {
    public static final BooleanProperty ABIERTO = BooleanProperty.of("abierto");
    public static final BooleanProperty DER = BooleanProperty.of("der");

    private final VoxelShape forma;

    PlacaBlock(VoxelShape forma) {
        super(AbstractBlock.Settings.create().strength(1.5f, 6.0f).nonOpaque().noCollision().sounds(BlockSoundGroup.WOOD));
        this.forma = forma;
        setDefaultState(getStateManager().getDefaultState().with(ABIERTO, false).with(DER, false));
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(ABIERTO, DER); }

    @Override protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return forma; }

    @Override protected VoxelShape getCollisionShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return net.minecraft.util.shape.VoxelShapes.empty(); }
}
