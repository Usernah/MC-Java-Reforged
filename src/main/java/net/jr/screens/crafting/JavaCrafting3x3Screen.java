package net.jr.screens.crafting;

import net.jr.api.client.resource.Asset;
import net.jr.client.ui.container.slots.SlotGrid;
import net.jr.client.ui.container.slots.SlotGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;

public final class JavaCrafting3x3Screen extends AbstractJavaCraftingScreen<CraftingMenu> {
    private static final Asset BACKGROUND = Asset.MC("textures/gui/container/crafting_table_3x3.png");

    public JavaCrafting3x3Screen(CraftingMenu menu, Inventory inventory, Component title, int slotId) {
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
                        SlotGroup.CRAFTING_GRID_3X3,
                        SlotGroup.CRAFTING_RESULT,
                        14,
                        3,
                        18,
                        14,
                        88,
                        148,
                        88,
                        144,
                        7
                )
        );
    }

    @Override
    protected SlotGrid createSlotGrid() {
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
