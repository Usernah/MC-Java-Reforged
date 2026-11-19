package net.jr.screens.crafting;

import net.jr.client.ui.container.slots.ItemSlotGroup;
import net.jr.registry.ModUi;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

public final class Crafting2x2Screen extends AbstractCraftingScreen<InventoryMenu> {
    public Crafting2x2Screen(InventoryMenu menu, Inventory inventory, Component title, int slotId) {
        super(
                menu,
                inventory,
                title,
                slotId,
                ModUi.CRAFTING_2X2,
                ModUi.CRAFTING_2X2_DAT,
                270,
                151,
                2,
                2,
                ItemSlotGroup.PLAYER_CRAFTING_GRID,
                ItemSlotGroup.PLAYER_CRAFTING_RESULT
        );
    }
}
