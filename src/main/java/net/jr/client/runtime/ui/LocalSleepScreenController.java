package net.jr.client.runtime.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;

public final class LocalSleepScreenController {
    private LocalSleepScreenController() {
    }

    public static void synchronize(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        Screen screen = minecraft.gui.screen();
        if (screen instanceof InBedChatScreen inBedScreen && player != null && !player.isSleeping()) {
            inBedScreen.onPlayerWokeUp();
            return;
        }
        if (screen != null || player == null) {
            return;
        }

        if (player.isSleeping() && minecraft.level != null) {
            minecraft.gui.hud.getChat().openScreen(ChatComponent.ChatMethod.MESSAGE, InBedChatScreen::new);
        }
    }
}
