package emi.cafe;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/** Una mesa del comedor: el bloque, donde se pone el cliente, quien esta sentado y el plato que se le acaba de servir. */
final class Mesa {
    final BlockPos bloque;
    final double sx, sz;                  // donde se pone el cliente (justo al sur de la mesa)
    Cliente cliente;
    Entity plato;                         // el dibujo del plato sobre la mesa mientras come
    Entity sucio;                         // el plato sucio que deja el cliente al irse: hasta que alguien lo recoja la mesa no se puede usar

    Mesa(BlockPos bloque) { this.bloque = bloque; this.sx = bloque.getX() + 0.5; this.sz = bloque.getZ() + 1.8; }

    boolean libre() { return cliente == null && sucio == null; }
}
