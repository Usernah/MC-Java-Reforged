package net.jr.experimental_features.gamepad_keyboard;

import net.jr.client.input.InputApi;
import net.jr.api.client.input.IGamepad;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.systemui.keyboard.QwertyMenu;
import net.jr.api.client.systemui.SystemUI;
import net.jr.client.ui.overlay.ScreenOverlays;
import net.jr.client.ui.navigation.UiInputModeController;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

public final class GamepadKeyboardController {
    private GamepadKeyboardController() {
    }

    public static boolean activateFocused(Screen screen) {
        return activate(screen, UiInputModeController.focusedElement(screen));
    }

    public static boolean activateFocusedFromPrimaryAction(Screen screen) {
        boolean focusedActivation = UiInputModeController.isFocusNavigationActive()
            || screen instanceof GamepadKeyboardScreen keyboardScreen
                && keyboardScreen.gamepadKeyboardActivateFocusedInputOnPrimary();
        return focusedActivation && activateFocused(screen);
    }

    public static boolean activatePointed(Screen screen, double mouseX, double mouseY) {
        GuiEventListener focused = UiInputModeController.focusedElement(screen);
        return focused != null && focused.isMouseOver(mouseX, mouseY) && activate(screen, focused);
    }

    private static boolean activate(Screen screen, GuiEventListener listener) {
        if (ScreenOverlays.current().hasOverlay()
                || SystemUI.blocksSlot(InputApi.currentSlotId())
                || !InputApi.hasGamepad() || !InputApi.isGamepadMode()
                || !(listener instanceof GamepadTextInput input)
                || !input.gamepadKeyboardCanConsumeInput()) {
            return false;
        }
        IGamepad gamepad = InputApi.currentGamepad();
        if (gamepad == null) {
            return false;
        }
        QwertyMenu.builder(screen.getTitle(), input)
            .activator(gamepad)
            .show();
        return true;
    }

    public static GamepadKeyboardState state() {
        return ClientRuntime.INSTANCE.slots().slot(InputApi.currentSlotId()).inputState().gamepadKeyboard();
    }
}
