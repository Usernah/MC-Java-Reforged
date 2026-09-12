package net.jr.screens.inventory;

import net.jr.client.input.InputApi;
import net.jr.screens.InventoryScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class CreativeInventoryHandler {
    private CreativeInventoryHandler() {
    }

    public static boolean replacesVanilla(Minecraft minecraft, KeyMapping mapping) {
        return mapping == minecraft.options.keyInventory
                && minecraft.player != null
                && minecraft.player.hasInfiniteMaterials();
    }

    public static void openIfRequested(Minecraft minecraft, KeyMapping mapping) {
        if (!InputApi.consumeClick(mapping)) return;
        minecraft.gui.setScreen(new InventoryScreen(
                minecraft.player.inventoryMenu,
                minecraft.player.getInventory(),
                Component.translatable("container.inventory")
        ));
    }
}
