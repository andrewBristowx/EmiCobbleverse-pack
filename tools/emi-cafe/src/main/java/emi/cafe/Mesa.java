package emi.cafe;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/** Una mesa del comedor: el bloque, donde se sienta el cliente (la casilla libre pegada a la mesa a la que llega andando), quien esta sentado y el plato que se le acaba de servir. */
final class Mesa {
    final BlockPos bloque;
    double sx, sz;                        // donde se pone el cliente (cada vez que llega uno se elige una casilla libre junto a la mesa; por defecto, al sur)
    Cliente cliente;
    Entity plato;                         // el dibujo del plato sobre la mesa mientras come
    Entity sucio;                         // el plato sucio que deja el cliente al irse: hasta que alguien lo recoja la mesa no se puede usar
    long sucioDesde;                      // tick en que quedo sucia (el que lleva mas tiempo se limpia antes)

    Mesa(BlockPos bloque) { this.bloque = bloque; this.sx = bloque.getX() + 0.5; this.sz = bloque.getZ() + 1.5; }

    boolean libre() { return cliente == null && sucio == null; }
}
