package net.jr.client.runtime.ui;

import javax.annotation.Nullable;

import net.jr.client.runtime.context.ActiveClientSlot;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class LocalScreenTransitionHandler {
    private LocalScreenTransitionHandler() {
    }

    public static void onSetScreen(@Nullable Screen screen, CallbackInfo ci) {
        Integer slotId = ActiveClientSlot.idOrNull();
        if (slotId == null || slotId == 0) {
            return;
        }
        if (screen instanceof LevelLoadingScreen) {
            ClientRuntime.INSTANCE.sessions().session(slotId).setJoiningInProgress(true);
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
            LocalClientExecution.runPrimary(minecraft, () -> minecraft.gui.setScreen(null));
        }
    }

    private static boolean primaryReady() {
        LocalClientSlot primary = ClientRuntime.INSTANCE.primarySlot();
        return primary.renderState().level() != null
            && primary.gameplayState().player() != null
            && primary.gameplayState().gameMode() != null;
    }
}
