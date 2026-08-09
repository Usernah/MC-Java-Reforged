package net.alnv.javareforged.ClientRuntime.test;

import javax.annotation.Nullable;

import net.alnv.javareforged.ClientRuntime.runtime.ActiveSlot;
import net.alnv.javareforged.ClientRuntime.runtime.LocalPlayers;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
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
        if (screen instanceof ReceivingLevelScreen) {
            LocalPlayers.INSTANCE.sessions().session(slotId).setJoiningInProgress(true);
            ci.cancel();
        } else if (screen instanceof DisconnectedScreen) {
            ci.cancel();
        }
    }

    public static void closeCompletedPrimaryJoiningScreen(Minecraft minecraft, @Nullable Screen screen) {
        if (!(screen instanceof ReceivingLevelScreen)) {
            return;
        }
        if (primaryReady()) {
            minecraft.setScreen(null);
        }
    }

    private static boolean primaryReady() {
        PlayerSlot primary = LocalPlayers.INSTANCE.primarySlot();
        return primary.renderState().level() != null
            && primary.gameplayState().player() != null
            && primary.gameplayState().gameMode() != null;
    }
}
