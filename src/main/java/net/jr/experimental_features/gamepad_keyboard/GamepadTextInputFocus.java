package net.jr.experimental_features.gamepad_keyboard;

import java.util.function.BooleanSupplier;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.ui.container.actions.ContainerItemSlotFocusController;
import net.jr.client.ui.navigation.UiInputModeController;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;


public final class GamepadTextInputFocus {
    private GamepadTextInputFocus() {
    }

    public static boolean focus(Screen screen, GamepadTextInput input, Runnable prepareFocus) {
        if (input instanceof AbstractWidget widget && (!widget.active || !widget.visible)) {
            return false;
        }
        prepareFocus.run();
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            ContainerItemSlotFocusController.clearFocusedItemSlot(containerScreen);
        }
        screen.setFocused(input);
        input.setFocused(true);
        if (InputApi.isGamepadMode()) {
            GamepadInputHandler.syncVirtualCursorToFocus(screen);
        }
        return true;
    }

    public static boolean focusFromContentBoundary(
        Screen screen,
        GamepadTextInput input,
        UiAction action,
        Runnable prepareFocus
    ) {
        return action == UiAction.MOVE_UP && focus(screen, input, prepareFocus);
    }

    public static boolean handleFocusedNavigation(
        Screen screen,
        GamepadTextInput input,
        UiAction action,
        BooleanSupplier focusContent
    ) {
        if (UiInputModeController.focusedElement(screen) != input || !action.isDirectional()) {
            return false;
        }
        return action == UiAction.MOVE_DOWN ? focusContent.getAsBoolean() : true;
    }
}
