package net.jr.experimental_features.gamepad_keyboard;

import net.jr.client.input.InputApi;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.ui.overlay.ScreenOverlayManager;
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
        ScreenOverlayManager overlays = ScreenOverlays.current();
        if (overlays.hasOverlay() || !InputApi.hasGamepad() || !InputApi.isGamepadMode()
                || !(listener instanceof GamepadTextInput input)
                || !input.gamepadKeyboardCanConsumeInput()) {
            return false;
        }
        GamepadKeyboardState state = state();
        state.open(input);
        if (overlays.open(screen, state)) {
            return true;
        }
        state.clear();
        return false;
    }

    public static GamepadKeyboardState state() {
        return ClientRuntime.INSTANCE.slots().slot(InputApi.currentSlotId()).inputState().gamepadKeyboard();
    }
}
