package net.jr.client.runtime.ui;

import javax.annotation.Nullable;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.input.CursorVisibility;
import net.jr.api.client.input.InputMode;
import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.jr.experimental_features.pre_join.PreJoinWorldLoad;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.screens.MessageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;

public final class SlotScreenTransitionHandler {
    private SlotScreenTransitionHandler() {
    }

    public static boolean shouldCancelSetScreen(@Nullable Screen screen) {
        Integer slotId = SlotExecution.currentSlotIdOrNull();
        if (slotId == null || slotId == 0) {
            return false;
        }
        if (screen instanceof LevelLoadingScreen) {
            var session = ClientRuntime.INSTANCE.clients().sessionOrNull(slotId);
            if (session != null) {
                session.setJoiningInProgress(true);
            }
            return true;
        }
        return screen instanceof DisconnectedScreen;
    }

    public static void onSlotScreenChanged(Minecraft minecraft, int slotId, @Nullable Screen screen) {
        if (screen == null) {
            CursorVisibility.clearHiddenForSlot(slotId);
            CursorVisibility.clearReplacementHiddenForSlot(slotId);
            GamepadInputHandler.releaseFocusedItemSlotCursor(slotId);
        } else {
            if (shouldSeedControllerCursor(slotId)) {
                GamepadInputHandler.centerControllerCursorForScreen(slotId);
            }
            CursorVisibility.setReplacementHiddenForSlot(slotId, true);
        }
        CursorVisibility.sync();
    }

    public static void closeCompletedPrimaryJoiningScreen(Minecraft minecraft, @Nullable Screen screen) {
        if (!(screen instanceof LevelLoadingScreen)) {
            return;
        }
        if (PreJoinWorldLoad.shouldHoldLoadingScreen()) {
            return;
        }
        if (ReadinessPolicy.gameplayBound(0)) {
            ClientRuntime.INSTANCE.slots().primary().runForSlot(() -> minecraft.gui.setScreen(null));
        }
    }

    public static void closeCompletedPrimaryJoiningScreen(Minecraft minecraft) {
        closeCompletedPrimaryJoiningScreen(
                minecraft,
                ClientRuntime.INSTANCE.slots().primary().screenState().screen()
        );
    }

    public static void closeCompletedSecondaryJoiningScreens(Minecraft minecraft) {
        for (int slotId = 1; slotId < ClientSlots.MAX_SLOTS; slotId++) {
            Screen screen = ClientRuntime.INSTANCE.slots().slot(slotId).screenState().screen();
            if (screen instanceof MessageScreen messageScreen
                    && messageScreen.closesWhenGameplayReady()
                    && ReadinessPolicy.gameplayReady(slotId)) {
                ClientRuntime.INSTANCE.slots().slot(slotId).runForSlot(
                    () -> minecraft.gui.setScreen(null)
                );
            }
        }
    }

    public static void clearSecondaryJoiningScreen(Minecraft minecraft, int slotId) {
        Screen screen = ClientRuntime.INSTANCE.slots().slot(slotId).screenState().screen();
        if (screen instanceof MessageScreen messageScreen
                && messageScreen.closesWhenGameplayReady()) {
            ClientRuntime.INSTANCE.slots().slot(slotId).runForSlot(
                () -> minecraft.gui.setScreen(null)
            );
        }
    }

    private static boolean shouldSeedControllerCursor(int slotId) {
        return InputApi.hasGamepadForSlot(slotId)
            && (ClientRuntime.INSTANCE.slots().slot(slotId).inputState().mode() == InputMode.GAMEPAD
                || !InputApi.canPhysicalMouseDriveSlot(slotId));
    }
}
