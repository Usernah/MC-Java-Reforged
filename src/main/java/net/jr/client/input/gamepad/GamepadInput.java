package net.jr.client.input.gamepad;

import java.util.List;
import javax.annotation.Nullable;
import net.jr.client.input.sdl.SdlGamepad;
import net.jr.client.input.simulation.InputSimulation;

public final class GamepadInput {
    public static final float DIGITAL_PRESS_THRESHOLD = 0.2F;
    private static volatile boolean splitTouchpadButtons = true;

    private GamepadInput() {
    }

    public static boolean splitTouchpadButtons() {
        return splitTouchpadButtons;
    }

    public static boolean isSimulationActive() {
        return InputSimulation.isActive();
    }

    public static void setSplitTouchpadButtons(boolean enabled) {
        splitTouchpadButtons = enabled;
        SdlGamepad.setSplitTouchpad(enabled);
    }

    public static void initialize() {
        SdlGamepad.initIfNeeded();
    }

    public static void update() {
        InputSimulation.update();
        SdlGamepad.update();
    }

    public static void maintain() {
        SdlGamepad.maintenanceTick();
    }

    public static void requestRescan() {
        SdlGamepad.requestDeviceRescan();
    }

    public static String debugStatus() {
        return InputSimulation.isActive() ? InputSimulation.debugStatus() : SdlGamepad.debugStatus();
    }

    public static boolean isPressed(List<Long> deviceIds, GamepadDigitalInput input) {
        if (InputSimulation.isActive()) {
            return InputSimulation.isPressed(input);
        }
        for (long deviceId : deviceIds) {
            if (SdlGamepad.input(deviceId, input, DIGITAL_PRESS_THRESHOLD)) return true;
        }
        return false;
    }

    public static boolean isPressed(long deviceId, GamepadDigitalInput input) {
        return SdlGamepad.input(deviceId, input, DIGITAL_PRESS_THRESHOLD);
    }

    public static float axis(List<Long> deviceIds, GamepadAxis axis, float deadzone) {
        if (InputSimulation.isActive()) {
            return InputSimulation.axis(axis, deadzone);
        }
        float strongest = 0.0F;
        for (long deviceId : deviceIds) {
            float value = SdlGamepad.axis01(deviceId, axis, deadzone);
            if (Math.abs(value) > Math.abs(strongest)) strongest = value;
        }
        return strongest;
    }

    public static float axis(long deviceId, GamepadAxis axis, float deadzone) {
        return SdlGamepad.axis01(deviceId, axis, deadzone);
    }

    public static float rawAxis(List<Long> deviceIds, GamepadAxis axis) {
        if (InputSimulation.isActive()) {
            return InputSimulation.rawAxis(axis);
        }
        float strongest = 0.0F;
        for (long deviceId : deviceIds) {
            float value = SdlGamepad.rawAxis(deviceId, axis);
            if (Math.abs(value) > Math.abs(strongest)) strongest = value;
        }
        return strongest;
    }

    public static float rawAxis(long deviceId, GamepadAxis axis) {
        return SdlGamepad.rawAxis(deviceId, axis);
    }

    @Nullable
    public static GamepadIdentity identity(long deviceId) {
        return SdlGamepad.identity(deviceId);
    }

    public static void clearCalibrationPresses() {
        InputSimulation.clearCalibrationPresses();
        SdlGamepad.clearRawInputPresses();
    }

    @Nullable
    public static RawGamepadInputPress pollCalibrationPress() {
        RawGamepadInputPress simulatedPress = InputSimulation.pollCalibrationPress();
        return simulatedPress != null ? simulatedPress : SdlGamepad.pollRawInputPress();
    }

    public static boolean hasActiveCalibrationInput() {
        return InputSimulation.hasActiveCalibrationInput() || SdlGamepad.anyCalibrationInputActive();
    }

    public static boolean hasActiveCalibrationInput(long deviceId) {
        return SdlGamepad.calibrationInputActive(deviceId);
    }

    public static boolean isRawInputActive(long deviceId, RawGamepadInput input) {
        return SdlGamepad.rawInputActive(deviceId, input);
    }
}
