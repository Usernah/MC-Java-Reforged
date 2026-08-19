package net.jr.client.input.simulation;

import javax.annotation.Nullable;
import net.jr.client.input.gamepad.GamepadAxis;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.gamepad.RawGamepadInputPress;

public final class NoopInputSimulationProvider implements InputSimulationProvider {
    public static final NoopInputSimulationProvider INSTANCE = new NoopInputSimulationProvider();

    private NoopInputSimulationProvider() {
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void update() {
    }

    @Override
    public boolean isPressed(GamepadDigitalInput input) {
        return false;
    }

    @Override
    public float axis(GamepadAxis axis, float deadzone) {
        return 0.0F;
    }

    @Override
    public float rawAxis(GamepadAxis axis) {
        return 0.0F;
    }

    @Override
    public boolean handleKey(long windowPointer, int key, int scanCode, int action, int modifiers) {
        return false;
    }

    @Override
    public boolean handleMouseMove(long windowPointer, double x, double y) {
        return false;
    }

    @Override
    public boolean handleMouseButton(long windowPointer, int button, int action, int modifiers) {
        return false;
    }

    @Override
    public boolean handleMouseScroll(long windowPointer, double xOffset, double yOffset) {
        return false;
    }

    @Override
    public void clearCalibrationPresses() {
    }

    @Nullable
    @Override
    public RawGamepadInputPress pollCalibrationPress() {
        return null;
    }

    @Override
    public boolean hasActiveCalibrationInput() {
        return false;
    }

    @Override
    public String debugStatus() {
        return "Input simulation: disabled";
    }
}

