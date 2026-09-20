package net.jr.experimental_features.gamepad_keyboard;

/** Screen-level policy for controller text flows with native submit behavior. */
public interface GamepadKeyboardScreen {
    /** Lets the primary UI action activate the focused editor outside focus-navigation mode. */
    default boolean gamepadKeyboardActivateFocusedInputOnPrimary() {
        return false;
    }

    /** Runs the screen's native confirmation after the edited value is committed. */
    default boolean gamepadKeyboardSubmit() {
        return false;
    }
}
