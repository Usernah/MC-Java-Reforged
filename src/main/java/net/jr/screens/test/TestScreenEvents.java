package net.jr.screens.test;

import net.jr.Java_reforged;
import net.jr.api.client.local.Client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.jr.screens.crafting.Crafting3x3Screen;
import net.jr.screens.inventory.CreativeInventoryScreen;
import net.jr.screens.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class TestScreenEvents {

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {

        if (event.getScreen() instanceof CraftingScreen craftingScreen) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                event.setNewScreen(new Crafting3x3Screen(
                        craftingScreen.getMenu(),
                        minecraft.player.getInventory(),
                        craftingScreen.getTitle(),
                        Client.slotId(minecraft.player)
                ));
            }
            return;
        }

        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inventoryScreen) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && !minecraft.player.hasInfiniteMaterials()) {
                event.setNewScreen(new InventoryScreen(
                        inventoryScreen.getMenu(),
                        minecraft.player.getInventory(),
                        inventoryScreen.getTitle()
                ));
            }
            return;
        }

        if (event.getScreen() instanceof CreativeModeInventoryScreen creativeScreen) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }

            event.setNewScreen(new CreativeInventoryScreen(
                    minecraft.player,
                    creativeScreen.getTitle()
            ));
            return;
        }

        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.StonecutterScreen stonecutterScreen) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                event.setNewScreen(new net.jr.screens.StonecutterScreen(
                        stonecutterScreen.getMenu(),
                        minecraft.player.getInventory(),
                        stonecutterScreen.getTitle()
                ));
            }
        }
    }
}
