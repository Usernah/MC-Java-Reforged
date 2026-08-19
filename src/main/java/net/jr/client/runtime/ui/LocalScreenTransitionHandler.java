package net.jr.client.runtime.ui;

import javax.annotation.Nullable;
import net.jr.client.input.InputApi;
import net.jr.client.input.cursor.CursorHider;
import net.jr.client.input.mode.InputMode;
import net.jr.client.input.runtime.GamepadInputProcessor;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.context.SlotScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class LocalScreenTransitionHandler {
    private LocalScreenTransitionHandler() {
    }

    public static void onSetScreen(@Nullable Screen screen, CallbackInfo callback) {
        Integer slotId = SlotScope.idOrNull();
        if (slotId == null || slotId == 0) {
            return;
        }
        if (screen instanceof LevelLoadingScreen) {
            var session = ClientRuntime.INSTANCE.clients().sessionOrNull(slotId);
            if (session != null) {
                session.setJoiningInProgress(true);
            }
            callback.cancel();
        } else if (screen instanceof DisconnectedScreen) {
            callback.cancel();
        }
    }

    public static void onSlotScreenChanged(Minecraft minecraft, int slotId, @Nullable Screen screen) {
        if (screen == null) {
            CursorHider.clearHiddenForSlot(slotId);
            CursorHider.clearReplacementHiddenForSlot(slotId);
            GamepadInputProcessor.releaseFocusedSlotCursor(slotId);
        } else {
            if (shouldSeedControllerCursor(slotId)) {
                GamepadInputProcessor.centerControllerCursorForScreen(slotId);
            }
            CursorHider.setReplacementHiddenForSlot(slotId, true);
        }
        CursorHider.sync();
    }

    public static void closeCompletedPrimaryJoiningScreen(Minecraft minecraft, @Nullable Screen screen) {
        if (!(screen instanceof LevelLoadingScreen)) {
            return;
        }
        if (LocalClientReadinessPolicy.gameplayBound(0)) {
            SlotExecution.runPrimary(minecraft, () -> minecraft.gui.setScreen(null));
        }
    }

    private static boolean shouldSeedControllerCursor(int slotId) {
        return InputApi.hasGamepadForSlot(slotId)
            && (ClientRuntime.INSTANCE.slots().slot(slotId).inputState().mode() == InputMode.GAMEPAD
                || !InputApi.canPhysicalMouseDriveSlot(slotId));
    }
}
