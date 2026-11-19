package net.jr.experimental_features.gamepad_keyboard;


public interface GamepadKeyboardScreen {

    default boolean gamepadKeyboardActivateFocusedInputOnPrimary() {
        return false;
    }


    default boolean gamepadKeyboardSubmit() {
        return false;
    }
}
