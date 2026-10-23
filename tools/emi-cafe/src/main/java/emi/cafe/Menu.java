package emi.cafe;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Los menus del cafe: cofres de Minecraft con objetos propios (con su textura) como botones. Solo hace falta el mod en el servidor y en el cliente (por las texturas);
 * cada clic lo recibe el servidor y no se puede mover ni sacar nada.
 */
final class Menu {
    private Menu() {}

    interface Clic { void hacer(ServerPlayerEntity p); }
    record Entrada(ItemStack icono, Clic clic) {}

    /** Un boton: el objeto con un nombre de color y unas lineas de texto debajo. */
    static ItemStack icono(Item item, String nombre, Formatting color, String... lineas) {
        ItemStack st = new ItemStack(item);
        st.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(nombre).styled(s -> s.withColor(color).withBold(true).withItalic(false)));
        if (lineas.length > 0) {
            List<Text> lore = new ArrayList<>();
            for (String l : lineas) lore.add(Text.literal(l).styled(s -> s.withColor(Formatting.GRAY).withItalic(false)));
            st.set(net.minecraft.component.DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(lore));
        }
        return st;
    }

    /** Abre un menu de {@code filas} filas; {@code entradas} dice que hay en cada hueco (0 a filas*9-1). */
    static void abrir(ServerPlayerEntity p, String titulo, int filas, Map<Integer, Entrada> entradas) {
        SimpleInventory inv = new SimpleInventory(filas * 9);
        for (Map.Entry<Integer, Entrada> e : entradas.entrySet()) inv.setStack(e.getKey(), e.getValue().icono());
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory((id, pinv, pl) -> new Pantalla(id, pinv, inv, filas, entradas), Text.literal(titulo)));
    }

    private static ScreenHandlerType<GenericContainerScreenHandler> tipo(int filas) {
        return switch (filas) { case 1 -> ScreenHandlerType.GENERIC_9X1; case 2 -> ScreenHandlerType.GENERIC_9X2; case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4; case 5 -> ScreenHandlerType.GENERIC_9X5; default -> ScreenHandlerType.GENERIC_9X6; };
    }

    static final class Pantalla extends GenericContainerScreenHandler {
        private final Inventory inv;
        private final Map<Integer, Entrada> entradas;

        Pantalla(int id, PlayerInventory pinv, Inventory inv, int filas, Map<Integer, Entrada> entradas) {
            super(tipo(filas), id, pinv, inv, filas);
            this.inv = inv; this.entradas = entradas;
        }

        @Override
        public void onSlotClick(int slot, int button, SlotActionType type, PlayerEntity player) {
            if (slot >= 0 && slot < inv.size() && player instanceof ServerPlayerEntity sp && (type == SlotActionType.PICKUP || type == SlotActionType.QUICK_MOVE)) {
                Entrada e = entradas.get(slot);
                if (e != null && e.clic() != null) {
                    try { e.clic().hacer(sp); } catch (Throwable t) { EmiCafe.LOG.error("Error en un boton del menu", t); }
                }
            }
            if (!(player instanceof ServerPlayerEntity sp2 && sp2.currentScreenHandler != this)) { setCursorStack(ItemStack.EMPTY); syncState(); }
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canInsertIntoSlot(net.minecraft.screen.slot.Slot slot) { return false; }
    }
}
