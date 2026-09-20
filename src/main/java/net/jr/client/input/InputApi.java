package net.jr.client.input;

import java.util.List;
import javax.annotation.Nullable;
import net.jr.client.input.binding.BindingContext;
import net.jr.client.input.binding.GamepadBindingRegistry;
import net.jr.client.input.binding.GamepadInputChord;
import net.jr.client.input.gamepad.GamepadAxis;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.gamepad.GamepadIdentity;
import net.jr.client.input.gamepad.GamepadInput;
import net.jr.client.input.gamepad.RawGamepadInput;
import net.jr.client.input.gamepad.RawGamepadInputPress;
import net.jr.client.input.mode.InputMode;
import net.jr.client.runtime.input.LocalInputContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class InputApi {
    public static final float DIGITAL_PRESS_THRESHOLD = GamepadInput.DIGITAL_PRESS_THRESHOLD;

    private InputApi() {
    }

    public static InputMode mode() {
        return LocalInputContext.mode();
    }

    public static boolean isGamepadMode() {
        return mode() == InputMode.GAMEPAD;
    }

    public static void markKeyboardMouseInput() {
        if (GamepadInput.isSimulationActive()) {
            return;
        }
        LocalInputContext.markKeyboardMouseInput();
    }

    public static void markGamepadInput() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.getFramerateLimitTracker().onInputReceived();
        }
        LocalInputContext.markGamepadInput();
    }

    public static void markMouseMove(double rawX, double rawY) {
        markKeyboardMouseInput();
    }

    public static long lastKeyboardMouseInputNanos() {
        return LocalInputContext.lastKeyboardMouseInputNanos();
    }

    public static long lastGamepadInputNanos() {
        return LocalInputContext.lastGamepadInputNanos();
    }

    public static boolean isGamepadConnected() {
        return hasGamepad();
    }

    public static int keyboardMouseSlotId() {
        return LocalInputContext.keyboardMouseSlotId();
    }

    public static boolean canPhysicalMouseDriveSlot(int slotId) {
        return LocalInputContext.canPhysicalMouseDriveSlot(slotId);
    }

    public static boolean canPhysicalKeyboardDriveSlot(int slotId) {
        return LocalInputContext.canPhysicalKeyboardDriveSlot(slotId);
    }

    public static boolean hasGamepadForSlot(int slotId) {
        return LocalInputContext.hasGamepadForSlot(slotId);
    }

    public static boolean splitTouchpadButtons() {
        return GamepadInput.splitTouchpadButtons();
    }

    public static void setSplitTouchpadButtons(boolean enabled) {
        GamepadInput.setSplitTouchpadButtons(enabled);
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
        return LocalInputContext.devices().stream()
            .map(device -> new GamepadDevice(device.deviceId(), device.identity()))
            .toList();
    }

    public static void assignGamepad(long deviceId, int slotId) {
        LocalInputContext.assignGamepad(deviceId, slotId);
    }

    public static void unassignGamepad(long deviceId) {
        LocalInputContext.unassignGamepad(deviceId);
    }

    @Nullable
    public static GamepadIdentity currentGamepadIdentity() {
        List<Long> ids = selectedGamepadIds();
        return ids.isEmpty() ? null : GamepadInput.identity(ids.getFirst());
    }

    public static void initializeGamepads() {
        GamepadInput.initialize();
    }

    public static void updateGamepads() {
        GamepadInput.update();
    }

    public static void maintainGamepads() {
        GamepadInput.maintain();
    }

    public static void requestGamepadRescan() {
        GamepadInput.requestRescan();
    }

    public static String gamepadDebugStatus() {
        return GamepadInput.debugStatus();
    }

    public static boolean isPressed(GamepadDigitalInput input) {
        if (GamepadInput.isSimulationActive()) {
            return GamepadInput.isPressed(List.of(), input);
        }
        List<Long> selected = selectedGamepadIds();
        for (long deviceId : selected) {
            if (LocalInputContext.suppressForJoinChord(deviceId, input)) {
                continue;
            }
            if (GamepadInput.isPressed(List.of(deviceId), input)) return true;
        }
        return false;
    }

    public static boolean isPressed(long deviceId, GamepadDigitalInput input) {
        return GamepadInput.isPressed(deviceId, input);
    }

    public static float axis(GamepadAxis axis, float deadzone) {
        return GamepadInput.axis(selectedGamepadIds(), axis, deadzone);
    }

    public static float axis(long deviceId, GamepadAxis axis, float deadzone) {
        return GamepadInput.axis(deviceId, axis, deadzone);
    }

    public static float rawAxis(GamepadAxis axis) {
        return GamepadInput.rawAxis(selectedGamepadIds(), axis);
    }

    public static float rawAxis(long deviceId, GamepadAxis axis) {
        return GamepadInput.rawAxis(deviceId, axis);
    }

    public static void clearCalibrationPresses() {
        GamepadInput.clearCalibrationPresses();
    }

    @Nullable
    public static RawGamepadInputPress pollCalibrationPress() {
        return GamepadInput.pollCalibrationPress();
    }

    public static boolean hasActiveCalibrationInput() {
        return GamepadInput.hasActiveCalibrationInput();
    }

    public static boolean hasActiveCalibrationInput(long deviceId) {
        return GamepadInput.hasActiveCalibrationInput(deviceId);
    }

    public static boolean isRawInputActive(long deviceId, RawGamepadInput input) {
        return GamepadInput.isRawInputActive(deviceId, input);
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

    public static boolean isBindingApplied(KeyMapping keyMapping) {
        return GamepadBindingRegistry.get().isApplied(keyMapping);
    }

    public static void releaseBindings() {
        GamepadBindingRegistry.get().releaseAppliedBindings();
    }

    public static void suppressHeldBindings() {
        GamepadBindingRegistry.get().suppressHeldInputs();
    }

    public static void resetSlotInputState(int slotId) {
        LocalInputContext.resetSlot(slotId);
    }

    public static boolean isDown(KeyMapping keyMapping) {
        return LocalInputContext.isDown(keyMapping);
    }

    public static void setDown(KeyMapping keyMapping, boolean down) {
        LocalInputContext.setDown(keyMapping, down);
    }

    public static void click(KeyMapping keyMapping) {
        LocalInputContext.click(keyMapping);
    }

    public static boolean consumeClick(KeyMapping keyMapping) {
        return LocalInputContext.consumeClick(keyMapping);
    }

    public static void release(KeyMapping keyMapping) {
        LocalInputContext.release(keyMapping);
    }

    public static int clickCount(KeyMapping keyMapping) {
        return LocalInputContext.clickCount(keyMapping);
    }

    public static void setClickCount(KeyMapping keyMapping, int clickCount) {
        LocalInputContext.setClickCount(keyMapping, clickCount);
    }

    public static int currentSlotId() {
        return LocalInputContext.currentSlotId();
    }

    private static List<Long> selectedGamepadIds() {
        return LocalInputContext.selectedGamepadIds();
    }

    public record GamepadDevice(long deviceId, @Nullable GamepadIdentity identity) {
    }
}
