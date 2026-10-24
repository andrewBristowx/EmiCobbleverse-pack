package emi.cafe;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * Una vitrina de cristal de color (desde 0.19.0): una caja con base de madera donde se expone un ingrediente o un plato. Lo que hay dentro no es del bloque: lo guarda
 * {@code Datos.Jugador.vitrina} y lo dibuja {@link Obras#actualizarVitrinas} con un objeto flotante. Se maneja con clic derecho ({@link Cafe#usarVitrina}).
 */
public final class VitrinaBlock extends Block {
    private static final VoxelShape FORMA = VoxelShapes.cuboid(0, 0, 0, 1, 15 / 16.0, 1);

    VitrinaBlock() { super(AbstractBlock.Settings.create().strength(1.5f, 6.0f).nonOpaque().sounds(BlockSoundGroup.GLASS)); }

    @Override protected VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return FORMA; }

    @Override protected VoxelShape getCollisionShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return FORMA; }

    @Override protected float getAmbientOcclusionLightLevel(BlockState s, BlockView w, BlockPos p) { return 1.0f; }
}
