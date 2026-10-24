package emi.cafe;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.DoorBlock;
import net.minecraft.sound.BlockSoundGroup;

/**
 * Una puerta de verdad (2 bloques de alto, con marco y cristal de color) para el local (desde 0.18.2). Es una {@link DoorBlock} de vainilla, asi que gira sobre su bisagra, y hay
 * 2 estilos (marco de madera y marco blanco) por 12 colores. Ademas de poder abrirse a mano, la abre sola {@link Obras#tickPuertas} cuando se acerca alguien (el dueño siempre;
 * clientes y empleados con el cafe abierto) y suena una campanilla de tienda.
 */
public final class PuertaCristalBlock extends DoorBlock {
    PuertaCristalBlock() {
        super(BlockSetType.IRON, AbstractBlock.Settings.create().strength(1.5f, 6.0f).nonOpaque().sounds(BlockSoundGroup.GLASS).pistonBehavior(net.minecraft.block.piston.PistonBehavior.DESTROY));
    }
}
