package emi.buildbattle;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Menu de bloques gratis del NPC: categorias -> bloques (con paginas) y busqueda por nombre. */
public final class Menu {
    private static final int PAGE = 45;

    private enum Kind { CATEGORIES, ITEMS }

    private static final class Nav {
        Kind kind = Kind.CATEGORIES;
        int page = 0;
        int catPage = 0;
        Text title = Text.empty();
        List<Item> items = List.of();
    }

    public static void open(ServerPlayerEntity p) {
        Nav nav = new Nav();
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, pl) -> new BlockScreen(syncId, inv, (ServerPlayerEntity) pl, nav),
                Text.literal("Constructor: bloques gratis")));
    }

    public static void openSearch(ServerPlayerEntity p, String query) {
        List<Item> found = Catalog.search(query);
        Nav nav = new Nav();
        nav.kind = Kind.ITEMS;
        nav.title = Text.literal("Búsqueda: " + query);
        nav.items = found;
        if (found.isEmpty()) Msg.actionBar(p, Text.literal("No hay bloques que coincidan con \"" + query + "\"").formatted(Formatting.RED));
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, pl) -> new BlockScreen(syncId, inv, (ServerPlayerEntity) pl, nav),
                Text.literal("Búsqueda: " + query)));
    }

    public static void openAnvilSearch(ServerPlayerEntity p) {
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, pl) -> new SearchScreen(syncId, inv, (ServerPlayerEntity) pl),
                Text.literal("Buscar bloque: escribe y pulsa el resultado")));
    }

    // ---------------------------------------------------------------- helpers

    private static ItemStack named(Item item, String name, Formatting color, String... lore) {
        return named(item, Text.literal(name), color, lore);
    }

    private static ItemStack named(Item item, Text name, Formatting color, String... lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, name.copy().styled(st -> st.withColor(color).withItalic(false)));
        if (lore.length > 0) {
            List<Text> l = new ArrayList<>();
            for (String x : lore) l.add(Text.literal(x).styled(st -> st.withColor(Formatting.GRAY).withItalic(false)));
            s.set(DataComponentTypes.LORE, new LoreComponent(l));
        }
        return s;
    }

    private static final class BlockScreen extends GenericContainerScreenHandler {
        private final SimpleInventory inv;
        private final ServerPlayerEntity sp;
        private final Nav nav;

        BlockScreen(int syncId, PlayerInventory pi, ServerPlayerEntity sp, Nav nav) {
            this(syncId, pi, sp, nav, new SimpleInventory(54));
        }

        private BlockScreen(int syncId, PlayerInventory pi, ServerPlayerEntity sp, Nav nav, SimpleInventory inv) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, pi, inv, 6);
            this.inv = inv; this.sp = sp; this.nav = nav;
            render();
        }

        private int pages() {
            int n = nav.kind == Kind.CATEGORIES ? Catalog.categories().size() : nav.items.size();
            return Math.max(1, (n + PAGE - 1) / PAGE);
        }

        private int curPage() { return nav.kind == Kind.CATEGORIES ? nav.catPage : nav.page; }

        private void render() {
            for (int i = 0; i < 54; i++) inv.setStack(i, ItemStack.EMPTY);
            int page = Math.min(curPage(), pages() - 1);
            if (nav.kind == Kind.CATEGORIES) {
                List<Catalog.Category> cats = Catalog.categories();
                for (int i = 0; i < PAGE; i++) {
                    int idx = page * PAGE + i;
                    if (idx >= cats.size()) break;
                    Catalog.Category c = cats.get(idx);
                    inv.setStack(i, named(c.icon, c.name, Formatting.GOLD, c.items.size() + " bloques", "Clic para abrir"));
                }
            } else {
                for (int i = 0; i < PAGE; i++) {
                    int idx = page * PAGE + i;
                    if (idx >= nav.items.size()) break;
                    inv.setStack(i, new ItemStack(nav.items.get(idx)));
                }
            }
            ItemStack fill = named(Items.GRAY_STAINED_GLASS_PANE, " ", Formatting.GRAY);
            for (int i = 45; i < 54; i++) inv.setStack(i, fill.copy());
            inv.setStack(45, named(Items.ARROW, "< Anterior", Formatting.YELLOW));
            inv.setStack(46, named(Items.SPECTRAL_ARROW, "<< 10 páginas", Formatting.YELLOW));
            if (nav.kind == Kind.ITEMS) inv.setStack(47, named(Items.CHEST, "Categorías", Formatting.AQUA, "Volver a la lista de categorías"));
            Text where = nav.kind == Kind.CATEGORIES ? Text.literal("Categorías") : nav.title;
            inv.setStack(49, named(Items.PAPER, where, Formatting.WHITE, "Página " + (page + 1) + " de " + pages(),
                    nav.kind == Kind.ITEMS ? "Clic izq: un stack  |  Clic der: 1 bloque" : "Elige una categoría"));
            inv.setStack(51, named(Items.COMPASS, "Buscar", Formatting.GREEN, "Escribe el nombre de un bloque"));
            inv.setStack(52, named(Items.SPECTRAL_ARROW, "10 páginas >>", Formatting.YELLOW));
            inv.setStack(53, named(Items.ARROW, "Siguiente >", Formatting.YELLOW));
            sendContentUpdates();
        }

        private void setPage(int p) {
            int max = pages() - 1;
            p = Math.max(0, Math.min(max, p));
            if (nav.kind == Kind.CATEGORIES) nav.catPage = p; else nav.page = p;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType type, PlayerEntity player) {
            if (slotIndex >= 0 && slotIndex < 54 && (type == SlotActionType.PICKUP || type == SlotActionType.QUICK_MOVE)) {
                click(slotIndex, button);
            }
            // todo lo demas se ignora (no se pueden mover objetos del menu); se resincroniza para que el cursor no se quede raro
            setCursorStack(ItemStack.EMPTY);
            syncState();
        }

        private void click(int slot, int button) {
            if (slot < PAGE) {
                int idx = Math.min(curPage(), pages() - 1) * PAGE + slot;
                if (nav.kind == Kind.CATEGORIES) {
                    if (idx < Catalog.categories().size()) {
                        Catalog.Category c = Catalog.categories().get(idx);
                        nav.kind = Kind.ITEMS; nav.page = 0; nav.title = c.name; nav.items = c.items;
                        Msg.sound(sp, SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 1.2f);
                        render();
                    }
                } else if (idx < nav.items.size()) {
                    give(nav.items.get(idx), button == 1);
                }
                return;
            }
            switch (slot) {
                case 45 -> setPage(curPage() - 1);
                case 46 -> setPage(curPage() - 10);
                case 52 -> setPage(curPage() + 10);
                case 53 -> setPage(curPage() + 1);
                case 47 -> { if (nav.kind == Kind.ITEMS) { nav.kind = Kind.CATEGORIES; } }
                case 51 -> { sp.server.execute(() -> openAnvilSearch(sp)); return; }
                default -> { return; }
            }
            Msg.sound(sp, SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 1.0f);
            render();
        }

        private void give(Item item, boolean single) {
            int count = single ? 1 : Math.min(Game.cfg().stackSize, item.getMaxCount());
            ItemStack st = new ItemStack(item, count);
            int total = st.getCount();
            sp.getInventory().insertStack(st);
            if (st.getCount() == total) {
                Msg.actionBar(sp, Text.literal("Inventario lleno").formatted(Formatting.RED));
            } else {
                Msg.sound(sp, SoundEvents.ENTITY_ITEM_PICKUP, 0.5f, 1.4f);
                Msg.actionBar(sp, Text.literal("+" + (total - st.getCount()) + " ").formatted(Formatting.GREEN).append(item.getName()));
            }
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

        @Override
        public boolean canInsertIntoSlot(ItemStack stack, Slot slot) { return false; }

        @Override
        public boolean canUse(PlayerEntity player) { return Game.isBuilding(player); }
    }

    /** Yunque usado como cuadro de texto: lo que escribas se busca al pulsar el resultado. */
    private static final class SearchScreen extends AnvilScreenHandler {
        private final ServerPlayerEntity sp;
        private String query = "";

        SearchScreen(int syncId, PlayerInventory pi, ServerPlayerEntity sp) {
            super(syncId, pi, ScreenHandlerContext.EMPTY);
            this.sp = sp;
            this.input.setStack(0, named(Items.PAPER, " ", Formatting.WHITE));
        }

        @Override
        public boolean setNewItemName(String name) {
            this.query = name == null ? "" : name.trim();
            updateResult();
            sendContentUpdates();
            return true;
        }

        @Override
        public void updateResult() {
            this.levelCost.set(0);
            if (query.isEmpty()) { this.output.setStack(0, ItemStack.EMPTY); return; }
            this.output.setStack(0, named(Items.SPYGLASS, "Buscar: " + query, Formatting.GREEN, "Clic para ver los resultados", "(ignora el \"coste\": es gratis)"));
        }

        @Override
        protected boolean canTakeOutput(PlayerEntity player, boolean present) { return !query.isEmpty(); }

        @Override
        protected void onTakeOutput(PlayerEntity player, ItemStack stack) {
            String q = query;
            input.setStack(0, ItemStack.EMPTY);
            output.setStack(0, ItemStack.EMPTY);
            sp.server.execute(() -> {
                setCursorStack(ItemStack.EMPTY);
                openSearch(sp, q);
            });
        }

        @Override
        public void onClosed(PlayerEntity player) {
            input.clear();
            output.clear();
            super.onClosed(player);
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

        @Override
        public boolean canUse(PlayerEntity player) { return Game.isBuilding(player); }
    }
}
