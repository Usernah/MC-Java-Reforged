package net.jr.client.input.gamepad;

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

}
