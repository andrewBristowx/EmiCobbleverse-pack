package emi.plush;

import dev.mrshawn.pokeblocks.item.ModItemGroups;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

/** Pone el peluche en la pestaña "Pokeblocks - Misc" del inventario creativo. */
final class EmiPlushTab {
    private EmiPlushTab() {}

    static void register() {
        ItemGroupEvents.MODIFY_ENTRIES_ALL.register((group, entries) -> {
            if (group == ModItemGroups.MISC) {
                entries.method_45421(EmiPlush.MICHI_ITEM);
                emi.plush.gen.DollRegistry.OBJETOS.values().forEach(entries::method_45421);
            }
        });
    }
}
