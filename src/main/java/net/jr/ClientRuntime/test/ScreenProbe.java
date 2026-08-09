package net.jr.ClientRuntime.test;

import javax.annotation.Nullable;

import net.jr.ClientRuntime.runtime.ActiveSlot;
import net.jr.ClientRuntime.runtime.LocalPlayers;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class ScreenProbe {
    private ScreenProbe() {
    }

    public static void onSetScreen(@Nullable Screen screen, CallbackInfo ci) {
        Integer slotId = ActiveSlot.idOrNull();
        if (slotId == null || slotId == 0) {
            return;
        }
        if (screen instanceof LevelLoadingScreen) {
            LocalPlayers.INSTANCE.sessions().session(slotId).setJoiningInProgress(true);
            ci.cancel();
        } else if (screen instanceof DisconnectedScreen) {
            ci.cancel();
        }
    }

    public static void closeCompletedPrimaryJoiningScreen(Minecraft minecraft, @Nullable Screen screen) {
        if (!(screen instanceof LevelLoadingScreen)) {
            return;
        }
        if (primaryReady()) {
            minecraft.gui.setScreen(null);
        }
    }

    private static boolean primaryReady() {
        PlayerSlot primary = LocalPlayers.INSTANCE.primarySlot();
        return primary.renderState().level() != null
            && primary.gameplayState().player() != null
            && primary.gameplayState().gameMode() != null;
    }
}
