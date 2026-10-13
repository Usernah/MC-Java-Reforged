package net.jr.screens.crafting;

import net.jr.api.client.resource.Asset;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.ItemSlotGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;

/** Preserves the previous 3x3 crafting screen while the active design evolves. */
public final class Crafting3x3ScreenOld extends AbstractCraftingScreen<CraftingMenu> {
    private static final Asset BACKGROUND = Asset.MC("textures/gui/container/crafting_table_3x3.png");

    public Crafting3x3ScreenOld(CraftingMenu menu, Inventory inventory, Component title, int slotId) {
        super(
                menu,
                inventory,
                title,
                slotId,
                BACKGROUND,
                288,
                168,
                3,
                3,
                new CraftingScreenLayout(
                        ItemSlotGroup.CRAFTING_GRID_3X3,
                        ItemSlotGroup.CRAFTING_RESULT,
                        14,
                        3,
                        18,
                        14,
                        87,
                        148,
                        87,
                        144,
                        7
                )
        );
    }

    @Override
    protected ItemSlotGrid createItemSlotGrid() {
        return this.craftingTableSlotGrid(
                this.leftPos + 14,
                this.topPos + 104,
                this.leftPos + 104,
                this.topPos + 118,
                this.leftPos + 148,
                this.topPos + 98,
                this.leftPos + 148,
                this.topPos + 144
        );
    }
}
