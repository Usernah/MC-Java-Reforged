package net.jr.client.input;

import java.util.List;
import javax.annotation.Nullable;
import net.jr.client.input.binding.BindingContext;
import net.jr.client.input.binding.GamepadBindingRegistry;
import net.jr.client.input.binding.GamepadInputChord;
import net.jr.client.input.gamepad.GamepadAxis;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.gamepad.GamepadIdentity;
import net.jr.client.input.gamepad.RawGamepadInput;
import net.jr.client.input.gamepad.RawGamepadInputPress;
import net.jr.client.input.mode.InputMode;
import net.jr.client.input.sdl.SdlGamepad;
import net.jr.client.input.simulation.InputSimulation;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.input.GamepadJoinTicker;
import net.jr.client.runtime.input.InputDeviceRouter;
import net.jr.client.runtime.input.binding.KeyMappingState;
import net.jr.client.runtime.state.InputState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class InputApi {
    public static final float DIGITAL_PRESS_THRESHOLD = 0.2F;
    private static volatile boolean splitTouchpadButtons = true;

    private InputApi() {
    }

    public static InputMode mode() {
        return inputState().mode();
    }

    public static boolean isGamepadMode() {
        return mode() == InputMode.GAMEPAD;
    }

    public static void markKeyboardMouseInput() {
        if (InputSimulation.isActive()) {
            return;
        }
        inputState(InputDeviceRouter.KEYBOARD_MOUSE_SLOT).markKeyboardMouseInput();
    }

    public static void markGamepadInput() {
        inputState().markGamepadInput();
    }

    public static void markMouseMove(double rawX, double rawY) {
        markKeyboardMouseInput();
    }

    public static long lastKeyboardMouseInputNanos() {
        return inputState().lastKeyboardMouseInputNanos();
    }

    public static long lastGamepadInputNanos() {
        return inputState().lastGamepadInputNanos();
    }

    public static boolean isGamepadConnected() {
        return hasGamepad();
    }

    public static int keyboardMouseSlotId() {
        return InputDeviceRouter.keyboardMouseSlotId();
    }

    public static boolean canPhysicalMouseDriveSlot(int slotId) {
        return InputDeviceRouter.canPhysicalMouseDriveSlot(slotId);
    }

    public static boolean canPhysicalKeyboardDriveSlot(int slotId) {
        return InputDeviceRouter.canPhysicalKeyboardDriveSlot(slotId);
    }

    public static boolean hasGamepadForSlot(int slotId) {
        return InputDeviceRouter.hasGamepadForSlot(slotId);
    }

    public static boolean splitTouchpadButtons() {
        return splitTouchpadButtons;
    }

    public static void setSplitTouchpadButtons(boolean enabled) {
        splitTouchpadButtons = enabled;
        SdlGamepad.setSplitTouchpad(enabled);
    }

    public static boolean canPhysicalMouseDrive() {
        return canPhysicalMouseDriveSlot(currentSlotId());
    }

    public static boolean canPhysicalKeyboardDrive() {
        return canPhysicalKeyboardDriveSlot(currentSlotId());
    }

    public static boolean hasGamepad() {
        return hasGamepadForSlot(currentSlotId());
    }

    public static List<GamepadDevice> gamepads() {
        return InputDeviceRouter.devices().stream()
            .map(device -> new GamepadDevice(device.deviceId(), device.identity()))
            .toList();
    }

    public static void assignGamepad(long deviceId, int slotId) {
        InputDeviceRouter.assignGamepad(deviceId, slotId);
    }

    public static void unassignGamepad(long deviceId) {
        InputDeviceRouter.unassignGamepad(deviceId);
    }

    @Nullable
    public static GamepadIdentity currentGamepadIdentity() {
        List<Long> ids = selectedGamepadIds();
        return ids.isEmpty() ? null : SdlGamepad.identity(ids.getFirst());
    }

    public static void initializeGamepads() {
        SdlGamepad.initIfNeeded();
    }

    public static void updateGamepads() {
        InputSimulation.update();
        SdlGamepad.update();
    }

    public static void maintainGamepads() {
        SdlGamepad.maintenanceTick();
    }

    public static void requestGamepadRescan() {
        SdlGamepad.requestDeviceRescan();
    }

    public static String gamepadDebugStatus() {
        if (InputSimulation.isActive()) {
            return InputSimulation.debugStatus();
        }
        return SdlGamepad.debugStatus();
    }

    public static boolean isPressed(GamepadDigitalInput input) {
        if (InputSimulation.isActive()) {
            return InputSimulation.isPressed(input);
        }
        for (long deviceId : selectedGamepadIds()) {
            if (GamepadJoinTicker.suppressForJoinChord(deviceId, input)) {
                continue;
            }
            if (SdlGamepad.input(deviceId, input, DIGITAL_PRESS_THRESHOLD)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPressed(long deviceId, GamepadDigitalInput input) {
        return SdlGamepad.input(deviceId, input, DIGITAL_PRESS_THRESHOLD);
    }

    public static float axis(GamepadAxis axis, float deadzone) {
        if (InputSimulation.isActive()) {
            return InputSimulation.axis(axis, deadzone);
        }
        float strongest = 0.0F;
        for (long deviceId : selectedGamepadIds()) {
            float value = SdlGamepad.axis01(deviceId, axis, deadzone);
            if (Math.abs(value) > Math.abs(strongest)) {
                strongest = value;
            }
        }
        return strongest;
    }

    public static float axis(long deviceId, GamepadAxis axis, float deadzone) {
        return SdlGamepad.axis01(deviceId, axis, deadzone);
    }

    public static float rawAxis(GamepadAxis axis) {
        if (InputSimulation.isActive()) {
            return InputSimulation.rawAxis(axis);
        }
        float strongest = 0.0F;
        for (long deviceId : selectedGamepadIds()) {
            float value = SdlGamepad.rawAxis(deviceId, axis);
            if (Math.abs(value) > Math.abs(strongest)) {
                strongest = value;
            }
        }
        return strongest;
    }

    public static float rawAxis(long deviceId, GamepadAxis axis) {
        return SdlGamepad.rawAxis(deviceId, axis);
    }

    public static void clearCalibrationPresses() {
        InputSimulation.clearCalibrationPresses();
        SdlGamepad.clearRawInputPresses();
    }

    @Nullable
    public static RawGamepadInputPress pollCalibrationPress() {
        RawGamepadInputPress simulatedPress = InputSimulation.pollCalibrationPress();
        if (simulatedPress != null) {
            return simulatedPress;
        }
        return SdlGamepad.pollRawInputPress();
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

    public static void ensureBindingsLoaded(Minecraft minecraft) {
        GamepadBindingRegistry.get().ensureLoaded(minecraft);
    }

    @Nullable
    public static GamepadInputChord binding(KeyMapping keyMapping) {
        return GamepadBindingRegistry.get().getBinding(keyMapping);
    }

    public static void applyBindings(BindingContext context) {
        GamepadBindingRegistry.get().applyMappedBindings(context);
    }

    public static void releaseBindings() {
        GamepadBindingRegistry.get().releaseAppliedBindings();
    }

    public static void suppressHeldBindings() {
        GamepadBindingRegistry.get().suppressHeldInputs();
    }

    public static boolean isDown(KeyMapping keyMapping) {
        return state(keyMapping).isDown();
    }

    public static void setDown(KeyMapping keyMapping, boolean down) {
        state(keyMapping).setDown(down);
    }

    public static void click(KeyMapping keyMapping) {
        state(keyMapping).incrementClickCount();
    }

    public static boolean consumeClick(KeyMapping keyMapping) {
        return state(keyMapping).consumeClick();
    }

    public static void release(KeyMapping keyMapping) {
        state(keyMapping).release();
    }

    public static int clickCount(KeyMapping keyMapping) {
        return state(keyMapping).clickCount();
    }

    public static void setClickCount(KeyMapping keyMapping, int clickCount) {
        state(keyMapping).setClickCount(clickCount);
    }

    public static int currentSlotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : InputDeviceRouter.KEYBOARD_MOUSE_SLOT;
    }

    private static KeyMappingState state(KeyMapping keyMapping) {
        return inputState().state(keyMapping);
    }

    private static InputState inputState() {
        return inputState(currentSlotId());
    }

    private static InputState inputState(int slotId) {
        return ClientRuntime.INSTANCE.slots().slot(slotId).inputState();
    }

    private static List<Long> selectedGamepadIds() {
        return InputDeviceRouter.gamepadsForSlot(currentSlotId());
    }

    public record GamepadDevice(long deviceId, @Nullable GamepadIdentity identity) {
    }
}
