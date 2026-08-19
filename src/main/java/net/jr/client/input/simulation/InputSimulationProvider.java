package net.jr.client.input.simulation;

import javax.annotation.Nullable;
import net.jr.client.input.gamepad.GamepadAxis;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.gamepad.RawGamepadInputPress;

public interface InputSimulationProvider {
    boolean isActive();

    boolean isConnected();

    void update();

    boolean isPressed(GamepadDigitalInput input);

    float axis(GamepadAxis axis, float deadzone);

    float rawAxis(GamepadAxis axis);

    boolean handleKey(long windowPointer, int key, int scanCode, int action, int modifiers);

    boolean handleMouseMove(long windowPointer, double x, double y);

    boolean handleMouseButton(long windowPointer, int button, int action, int modifiers);

    boolean handleMouseScroll(long windowPointer, double xOffset, double yOffset);

    void clearCalibrationPresses();

    @Nullable
    RawGamepadInputPress pollCalibrationPress();

    boolean hasActiveCalibrationInput();

    String debugStatus();
}

