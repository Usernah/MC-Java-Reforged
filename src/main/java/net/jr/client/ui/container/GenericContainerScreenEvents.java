package net.jr.client.ui.container;

import net.jr.Java_reforged;
import net.jr.screens.GenericContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class GenericContainerScreenEvents {
    private GenericContainerScreenEvents() {
    }

    @SubscribeEvent
    public static void onOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() == null
            || event.getNewScreen().getClass() != ContainerScreen.class) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ContainerScreen vanillaScreen = (ContainerScreen) event.getNewScreen();
        if (minecraft.player == null
            || !GenericContainerScreen.hasCompiledDsl(vanillaScreen.getMenu())) {
            return;
        }
        event.setNewScreen(new GenericContainerScreen(
            vanillaScreen.getMenu(),
            minecraft.player.getInventory(),
            vanillaScreen.getTitle()
        ));
    }
}
