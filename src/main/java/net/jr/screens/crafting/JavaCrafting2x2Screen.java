package net.jr.screens.crafting;

import net.jr.api.client.resource.Asset;
import net.jr.client.ui.container.slots.SlotGrid;
import net.jr.client.ui.container.slots.SlotGroup;
import net.jr.client.ui.container.slots.SlotMaps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

public final class JavaCrafting2x2Screen extends AbstractJavaCraftingScreen<InventoryMenu> {
    private static final Asset BACKGROUND = Asset.MC("textures/gui/container/crafting_table_2x2.png");

    public JavaCrafting2x2Screen(InventoryMenu menu, Inventory inventory, Component title) {
        super(
                menu,
                inventory,
                title,
                BACKGROUND,
                270,
                151,
                2,
                2,
                new CraftingScreenLayout(
                        SlotGroup.PLAYER_CRAFTING_GRID,
                        SlotGroup.PLAYER_CRAFTING_RESULT,
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
    protected SlotGrid createSlotGrid() {
        return new SlotGrid(SlotMaps.SURVIVAL_CRAFTING)
                .grid(
                        SlotGroup.PLAYER_CRAFTING_GRID,
                        this.leftPos + 14,
                        this.topPos + 96,
                        2,
                        18,
                        18,
                        18,
                        18,
                        16
                )
                .point(
                        SlotGroup.PLAYER_CRAFTING_RESULT,
                        0,
                        this.leftPos + 86,
                        this.topPos + 101,
                        26,
                        26,
                        24
                )
                .grid(
                        SlotGroup.PLAYER_INVENTORY,
                        this.leftPos + 130,
                        this.topPos + 81,
                        9,
                        14,
                        14,
                        14,
                        14
                )
                .grid(
                        SlotGroup.PLAYER_HOTBAR,
                        this.leftPos + 130,
                        this.topPos + 127,
                        9,
                        14,
                        14,
                        14,
                        14
                );
    }
}
