package net.jr.screens.crafting;

import net.jr.api.client.resource.Asset;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.ItemSlotGroup;
import net.jr.client.ui.container.slots.ItemSlotMaps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

public final class Crafting2x2Screen extends AbstractCraftingScreen<InventoryMenu> {
    private static final Asset BACKGROUND = Asset.MC("textures/gui/container/crafting_table_2x2.png");

    public Crafting2x2Screen(InventoryMenu menu, Inventory inventory, Component title, int slotId) {
        super(
                menu,
                inventory,
                title,
                slotId,
                BACKGROUND,
                270,
                151,
                2,
                2,
                new CraftingScreenLayout(
                        ItemSlotGroup.PLAYER_CRAFTING_GRID,
                        ItemSlotGroup.PLAYER_CRAFTING_RESULT,
                        13,
                        2,
                        18,
                        14,
                        70,
                        130,
                        70,
                        135,
                        7
                )
        );
    }

    @Override
    protected ItemSlotGrid createItemSlotGrid() {
        return new ItemSlotGrid(ItemSlotMaps.SURVIVAL_CRAFTING)
                .grid(
                        ItemSlotGroup.PLAYER_CRAFTING_GRID,
                        this.leftPos + 14,
                        this.topPos + 96,
                        2,
                        18,
                        18,
                        16
                )
                .point(
                        ItemSlotGroup.PLAYER_CRAFTING_RESULT,
                        0,
                        this.leftPos + 86,
                        this.topPos + 101,
                        26,
                        26,
                        24
                )
                .grid(
                        ItemSlotGroup.PLAYER_INVENTORY,
                        this.leftPos + 130,
                        this.topPos + 81,
                        9,
                        14,
                        14
                )
                .grid(
                        ItemSlotGroup.PLAYER_HOTBAR,
                        this.leftPos + 130,
                        this.topPos + 127,
                        9,
                        14,
                        14
                );
    }
}
