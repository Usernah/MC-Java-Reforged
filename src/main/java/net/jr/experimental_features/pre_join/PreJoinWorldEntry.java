package net.jr.experimental_features.pre_join;

import net.jr.client.runtime.ClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;


public final class PreJoinWorldEntry {
    private static boolean continuingWorldOpen;

    private PreJoinWorldEntry() {
    }

    public static boolean intercept(Runnable worldOpen) {
        if (continuingWorldOpen || PreJoinWorldLoad.isActive() || !PreJoinPlayersOverlay.shouldOpen()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Screen owner = minecraft.gui.screen();
        if (owner == null) {
            return false;
        }
        PreJoinPlayersOverlay overlay = new PreJoinPlayersOverlay(
            owner,
            () -> continueWorldOpen(worldOpen)
        );
        return ClientRuntime.INSTANCE.slots().primary().inputState().screenOverlays().open(owner, overlay);
    }

    private static void continueWorldOpen(Runnable worldOpen) {
        continuingWorldOpen = true;
        try {
            worldOpen.run();
        } finally {
            continuingWorldOpen = false;
        }
    }
}
