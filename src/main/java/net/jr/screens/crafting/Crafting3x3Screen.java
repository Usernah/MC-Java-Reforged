package net.jr.screens.crafting;

import net.jr.client.ui.container.slots.ItemSlotGroup;
import net.jr.registry.ModUi;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;

public final class Crafting3x3Screen extends AbstractCraftingScreen<CraftingMenu> {
    public Crafting3x3Screen(CraftingMenu menu, Inventory inventory, Component title, int slotId) {
        super(
                menu,
                inventory,
                title,
                slotId,
                ModUi.CRAFTING_3X3,
                ModUi.CRAFTING_3X3_DAT,
                354,
                203,
                3,
                3,
                ItemSlotGroup.CRAFTING_GRID_3X3,
                ItemSlotGroup.CRAFTING_RESULT
        );
    }
}
