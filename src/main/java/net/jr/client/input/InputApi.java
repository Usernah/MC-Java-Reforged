package net.jr.client.input;

import java.util.List;
import javax.annotation.Nullable;
import net.jr.client.input.binding.BindingContext;
import net.jr.client.input.binding.GamepadBindingRegistry;
import net.jr.client.input.binding.GamepadInputChord;
import net.jr.api.client.input.GamepadAxis;
import net.jr.api.client.input.GamepadDigitalInput;
import net.jr.api.client.input.GamepadIdentity;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.ISlotInput;
import net.jr.client.input.gamepad.GamepadInput;
import net.jr.client.input.gamepad.RawGamepadInput;
import net.jr.client.input.gamepad.RawGamepadInputPress;
import net.jr.api.client.input.InputMode;
import net.jr.client.runtime.input.LocalInputContext;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.input.SdlCalibrationBridge;
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

    public static List<IGamepad> gamepads() {
        return ClientRuntime.INSTANCE.input().devices().gamepads();
    }

    @Nullable
    public static IGamepad currentGamepad() {
        List<IGamepad> selected = slotInput().gamepads();
        return selected.isEmpty() ? null : selected.getFirst();
    }

    public static void assignGamepad(IGamepad gamepad, int slotId) {
        ClientRuntime.INSTANCE.input().mutableAssignments().assign(gamepad, slotId);
        ClientRuntime.INSTANCE.slots().slot(slotId).inputState().markGamepadInput();
    }

    public static void unassignGamepad(IGamepad gamepad) {
        ClientRuntime.INSTANCE.input().mutableAssignments().unassign(gamepad);
    }

    @Nullable
    public static GamepadIdentity currentGamepadIdentity() {
        IGamepad gamepad = currentGamepad();
        return gamepad == null ? null : gamepad.identity();
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
        return slotInput().pressed(input);
    }

    public static boolean isPressed(IGamepad gamepad, GamepadDigitalInput input) {
        return gamepad.input().pressed(input);
    }

    public static float axis(GamepadAxis axis, float deadzone) {
        return slotInput().axis(axis, deadzone);
    }

    public static float axis(IGamepad gamepad, GamepadAxis axis, float deadzone) {
        return gamepad.input().axis(axis, deadzone);
    }

    public static float rawAxis(GamepadAxis axis) {
        return slotInput().rawAxis(axis);
    }

    public static float rawAxis(IGamepad gamepad, GamepadAxis axis) {
        return gamepad.input().rawAxis(axis);
    }

    public static void clearCalibrationPresses() {
        GamepadInput.clearCalibrationPresses();
    }

    @Nullable
    public static CalibrationPress pollCalibrationPress() {
        RawGamepadInputPress press = GamepadInput.pollCalibrationPress();
        if (press == null) {
            return null;
        }
        IGamepad gamepad = SdlCalibrationBridge.gamepad(press);
        return new CalibrationPress(gamepad, press.identity(), press.input(), press.timestampMs());
    }

    public static boolean hasActiveCalibrationInput() {
        return GamepadInput.hasActiveCalibrationInput();
    }

    public static boolean hasActiveCalibrationInput(IGamepad gamepad) {
        return gamepad.input().calibrationInputActive();
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

    private static ISlotInput slotInput() {
        return ClientRuntime.INSTANCE.slots().slot(currentSlotId()).input();
    }

    public record CalibrationPress(
        @Nullable IGamepad gamepad,
        GamepadIdentity identity,
        RawGamepadInput input,
        long timestampMs
    ) {
    }
}
