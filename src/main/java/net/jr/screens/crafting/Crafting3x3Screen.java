package net.jr.screens.crafting;

import net.jr.api.client.resource.Asset;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.ItemSlotGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;

public final class Crafting3x3Screen extends AbstractCraftingScreen<CraftingMenu> {
    private static final Asset BACKGROUND = Asset.MC("textures/gui/container/crafting_table_3x3.png");

    public Crafting3x3Screen(CraftingMenu menu, Inventory inventory, Component title, int slotId) {
        super(
                menu,
                inventory,
                title,
                slotId,
                BACKGROUND,
                354,
                203,
                3,
                3,
                new CraftingScreenLayout(
                        ItemSlotGroup.CRAFTING_GRID_3X3,
                        ItemSlotGroup.CRAFTING_RESULT,
                        13,
                        2,
                        24,
                        22,
                        94,
                        166,
                        94,
                        178,
                        12
                )
        );
    }

    @Override
    protected ItemSlotGrid createItemSlotGrid() {
        return this.craftingTableSlotGrid(
                this.leftPos + 21,
                this.topPos + 124,
                this.leftPos + 103,
                this.topPos + 135,
                this.leftPos + 166,
                this.topPos + 107,
                this.leftPos + 166,
                this.topPos + 167
        );
    }
}
