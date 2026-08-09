package net.jr.client.input.simulation;

import javax.annotation.Nullable;
import net.jr.client.input.gamepad.GamepadAxis;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.gamepad.RawGamepadInputPress;

public final class InputSimulation {
    private static InputSimulationProvider provider = new KeyboardMouseGamepadSimulationProvider();

    private InputSimulation() {
    }

    public static void setProvider(InputSimulationProvider provider) {
        InputSimulation.provider = provider == null ? NoopInputSimulationProvider.INSTANCE : provider;
    }

    public static boolean isActive() {
        return provider.isActive();
    }

    public static boolean isConnected() {
        return provider.isConnected();
    }

    public static void update() {
        provider.update();
    }

    public static boolean isPressed(GamepadDigitalInput input) {
        return provider.isPressed(input);
    }

    public static float axis(GamepadAxis axis, float deadzone) {
        return provider.axis(axis, deadzone);
    }

    public static float rawAxis(GamepadAxis axis) {
        return provider.rawAxis(axis);
    }

    public static boolean handleKey(long windowPointer, int key, int scanCode, int action, int modifiers) {
        return provider.handleKey(windowPointer, key, scanCode, action, modifiers);
    }

    public static boolean handleMouseMove(long windowPointer, double x, double y) {
        return provider.handleMouseMove(windowPointer, x, y);
    }

    public static boolean handleMouseButton(long windowPointer, int button, int action, int modifiers) {
        return provider.handleMouseButton(windowPointer, button, action, modifiers);
    }

    public static boolean handleMouseScroll(long windowPointer, double xOffset, double yOffset) {
        return provider.handleMouseScroll(windowPointer, xOffset, yOffset);
    }

    public static void clearCalibrationPresses() {
        provider.clearCalibrationPresses();
    }

    @Nullable
    public static RawGamepadInputPress pollCalibrationPress() {
        return provider.pollCalibrationPress();
    }

    public static boolean hasActiveCalibrationInput() {
        return provider.hasActiveCalibrationInput();
    }

    public static String debugStatus() {
        return provider.debugStatus();
    }
}

