package emi.cafe;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * Un panel de cristal de color para la puerta del local (desde 0.18.1). Cerrado es un bloque de cristal macizo; abierto es una placa fina pegada al borde y se puede pasar. No lo abre
 * ningun clic: lo abre {@link Obras#tickPuertas} cuando se acerca alguien (el dueño siempre; clientes y empleados con el cafe abierto) y suena una campanilla de tienda.
 */
public final class PuertaCristalBlock extends Block {
    public static final BooleanProperty ABIERTA = BooleanProperty.of("abierta");
    private static final VoxelShape PLACA = VoxelShapes.cuboid(0, 0, 0, 1, 1, 1 / 16.0);

    PuertaCristalBlock() {
        super(AbstractBlock.Settings.create().strength(1.5f, 6.0f).nonOpaque().sounds(BlockSoundGroup.GLASS));
        setDefaultState(getStateManager().getDefaultState().with(ABIERTA, false));
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(ABIERTA); }

    @Override protected VoxelShape getCollisionShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return s.get(ABIERTA) ? VoxelShapes.empty() : VoxelShapes.fullCube(); }

    @Override protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return s.get(ABIERTA) ? PLACA : VoxelShapes.fullCube(); }

    @Override protected float getAmbientOcclusionLightLevel(BlockState s, BlockView w, BlockPos p) { return 1.0f; }

    /** Entre dos paneles cerrados del mismo color no se dibuja la cara de en medio (si no, se ven rayas dentro del cristal). */
    @Override protected boolean isSideInvisible(BlockState s, BlockState from, Direction d) { return from.isOf(this) && !s.get(ABIERTA) && !from.get(ABIERTA); }
}
