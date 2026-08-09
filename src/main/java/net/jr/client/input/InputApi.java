package net.jr.client.input;

import javax.annotation.Nullable;
import net.jr.ClientRuntime.input.InputDeviceRouter;
import net.jr.ClientRuntime.input.binding.KeyMappingState;
import net.jr.ClientRuntime.runtime.Client;
import net.jr.client.input.binding.BindingContext;
import net.jr.client.input.binding.GamepadBindingRegistry;
import net.jr.client.input.binding.GamepadInputChord;
import net.jr.client.input.gamepad.GamepadAxis;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.gamepad.GamepadIdentity;
import net.jr.client.input.gamepad.RawGamepadInputPress;
import net.jr.client.input.gamepad.RawGamepadInput;
import net.jr.client.input.mode.InputMode;
import net.jr.client.input.sdl.SdlGamepad;
import net.jr.client.input.simulation.InputSimulation;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import java.util.List;

/** Stable input surface for gameplay, UI and external integrations. */
public final class InputApi {
    public static final float DIGITAL_PRESS_THRESHOLD = 0.2F;
    /**
     * When enabled, a Sony-style touchpad exposes two virtual digital inputs
     * selected by the finger position at the start of the physical click. If
     * SDL cannot provide a valid position, the click remains available through
     * the unified TOUCHPAD_BUTTON input instead of being assigned to an arbitrary side.
     */
    private static volatile boolean splitTouchpadButtons = true;
    private InputApi() {
    }

    public static InputMode mode() {
        return Client.input(currentClientId()).mode();
    }

    public static boolean isGamepadMode() {
        return mode() == InputMode.GAMEPAD;
    }

    public static void markKeyboardMouseInput() {
        if (InputSimulation.isActive()) {
            return;
        }
        Client.input(InputDeviceRouter.KEYBOARD_MOUSE_CLIENT).markKeyboardMouseInput();
    }

    public static void markGamepadInput() {
        Client.input(currentClientId()).markGamepadInput();
    }

    public static void markMouseMove(double rawX, double rawY) {
        markKeyboardMouseInput();
    }

    public static long lastKeyboardMouseInputNanos() {
        return Client.input(currentClientId()).lastKeyboardMouseInputNanos();
    }

    public static long lastGamepadInputNanos() {
        return Client.input(currentClientId()).lastGamepadInputNanos();
    }

    public static boolean isGamepadConnected() {
        return hasGamepad();
    }

    public static int keyboardMouseClientId() {
        return InputDeviceRouter.KEYBOARD_MOUSE_CLIENT;
    }

    public static boolean canPhysicalMouseDriveClient(int clientId) {
        return InputDeviceRouter.canPhysicalMouseDriveClient(clientId);
    }

    public static boolean canPhysicalKeyboardDriveClient(int clientId) {
        return InputDeviceRouter.canPhysicalKeyboardDriveClient(clientId);
    }

    public static boolean hasGamepadForClient(int clientId) {
        return InputDeviceRouter.hasGamepadForClient(clientId);
    }

    public static boolean splitTouchpadButtons() {
        return splitTouchpadButtons;
    }

    public static void setSplitTouchpadButtons(boolean enabled) {
        splitTouchpadButtons = enabled;
        SdlGamepad.setSplitTouchpad(enabled);
    }

    public static boolean canPhysicalMouseDrive() {
        return canPhysicalMouseDriveClient(currentClientId());
    }

    public static boolean canPhysicalKeyboardDrive() {
        return canPhysicalKeyboardDriveClient(currentClientId());
    }

    public static boolean hasGamepad() {
        return hasGamepadForClient(currentClientId());
    }

    public static List<GamepadDevice> gamepads() {
        return InputDeviceRouter.devices().stream()
            .map(device -> new GamepadDevice(device.deviceId(), device.identity()))
            .toList();
    }

    public static void assignGamepad(long deviceId, int clientId) {
        InputDeviceRouter.assignGamepad(deviceId, clientId);
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

    /**
     * Host-only physical join gesture. The state belongs to a device, not to a
     * player slot; the launcher assigns the new JVM its process slot.
     */
    public static void tickGamepadJoin(Minecraft minecraft) {
        InputDeviceRouter.tickGamepadJoin(minecraft);
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

    public static int missTime() {
        return Client.input(currentClientId()).missTime();
    }

    public static void setMissTime(int value) {
        Client.input(currentClientId()).setMissTime(value);
    }

    public static int rightClickDelay() {
        return Client.input(currentClientId()).rightClickDelay();
    }

    public static void setRightClickDelay(int value) {
        Client.input(currentClientId()).setRightClickDelay(value);
    }

    public static void tickRightClickDelay() {
        Client.input(currentClientId()).tickRightClickDelay();
    }

    public static void tickMissTime() {
        Client.input(currentClientId()).tickMissTime();
    }

    private static KeyMappingState state(KeyMapping keyMapping) {
        return Client.input(currentClientId()).state(keyMapping);
    }

    private static List<Long> selectedGamepadIds() {
        return InputDeviceRouter.gamepadsForClient(currentClientId());
    }

    /** Public read model used by controller-assignment UI and integrations. */
    public record GamepadDevice(long deviceId, @Nullable GamepadIdentity identity) {
    }

    private static int currentClientId() {
        return Client.currentOrNull() == null
            ? InputDeviceRouter.KEYBOARD_MOUSE_CLIENT
            : Client.slotId();
    }
}

