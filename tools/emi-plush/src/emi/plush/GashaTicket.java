package emi.plush;

import java.util.List;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1271;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1836;
import net.minecraft.class_1937;
import net.minecraft.class_2168;
import net.minecraft.class_2561;

/**
 * Ticket del Gasha Michi. Clic derecho = 1 tirada; agachado = hasta 10 tiradas de golpe. Todo el reparto de premios (objetos, Pokémon
 * y peluches, con la garantía de la tirada 10) está en las funciones de datos `emi_plush:gasha/*`, que se pueden editar sin recompilar.
 * Nombres intermediary: class_1792 = Item, class_1799 = ItemStack, class_1657 = PlayerEntity, class_1271 = TypedActionResult,
 * class_2168 = ServerCommandSource.
 */
public class GashaTicket extends class_1792 {
    public static final int MAX_TIRADAS = 10;

    public GashaTicket(class_1792.class_1793 settings) {
        super(settings);
    }

    @Override
    public class_1271<class_1799> method_7836(class_1937 world, class_1657 player, class_1268 hand) {
        class_1799 stack = player.method_5998(hand);
        if (world.method_8608()) return new class_1271<>(class_1269.field_5812, stack);
        int n = player.method_5715() ? Math.min(MAX_TIRADAS, stack.method_7947()) : 1;
        stack.method_7934(n);
        class_2168 fuente = player.method_5671().method_9206(4);
        var comandos = world.method_8503().method_3734();
        for (int i = 0; i < n; i++) comandos.method_44252(fuente, "function emi_plush:gasha/tirar");
        return new class_1271<>(class_1269.field_5812, stack);
    }

    @Override
    public boolean method_7886(class_1799 stack) {
        return true;
    }

    @Override
    public void method_7851(class_1799 stack, class_1792.class_9635 contexto, List<class_2561> lineas, class_1836 tipo) {
        lineas.add(class_2561.method_43471("item.emi_plush.ticket_gasha_michi.desc"));
        lineas.add(class_2561.method_43471("item.emi_plush.ticket_gasha_michi.desc2"));
    }
}
