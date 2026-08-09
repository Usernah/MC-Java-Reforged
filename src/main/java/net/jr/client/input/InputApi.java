package net.jr.client.input;

import javax.annotation.Nullable;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Stable input surface for gameplay, UI and external integrations. */
public final class InputApi {
    public static final float DIGITAL_PRESS_THRESHOLD = 0.2F;
    private static final Map<KeyMapping, KeyState> KEY_STATES = new IdentityHashMap<>();
    private static InputMode inputMode = InputMode.KEYBOARD_MOUSE;
    /**
     * When enabled, a Sony-style touchpad exposes two virtual digital inputs
     * selected by the finger position at the start of the physical click. If
     * SDL cannot provide a valid position, the click remains available through
     * the unified TOUCHPAD_BUTTON input instead of being assigned to an arbitrary side.
     */
    private static volatile boolean splitTouchpadButtons = true;
    private static long lastKeyboardMouseInputNanos;
    private static long lastGamepadInputNanos;
    private static int missTime;
    private static int rightClickDelay;
    private static final Map<Long, Long> JOIN_STARTED_AT = new HashMap<>();
    private static final Set<Long> JOIN_LATCHED = new HashSet<>();
    private static final long JOIN_HOLD_MILLIS = 3_000L;
    private InputApi() {
    }

    public static InputMode mode() {
        return inputMode;
    }

    public static boolean isGamepadMode() {
        return mode() == InputMode.GAMEPAD;
    }

    public static void markKeyboardMouseInput() {
        if (InputSimulation.isActive()) {
            return;
        }
        lastKeyboardMouseInputNanos = System.nanoTime();
        inputMode = InputMode.KEYBOARD_MOUSE;
    }

    public static void markGamepadInput() {
        lastGamepadInputNanos = System.nanoTime();
        inputMode = InputMode.GAMEPAD;
    }

    public static void markMouseMove(double rawX, double rawY) {
        markKeyboardMouseInput();
    }

    public static long lastKeyboardMouseInputNanos() {
        return lastKeyboardMouseInputNanos;
    }

    public static long lastGamepadInputNanos() {
        return lastGamepadInputNanos;
    }

    public static boolean isGamepadConnected() {
        return hasGamepad();
    }

    public static boolean splitTouchpadButtons() {
        return splitTouchpadButtons;
    }

    public static void setSplitTouchpadButtons(boolean enabled) {
        splitTouchpadButtons = enabled;
        SdlGamepad.setSplitTouchpad(enabled);
    }

    public static boolean canPhysicalMouseDrive() {
        return true;
    }

    public static boolean canPhysicalKeyboardDrive() {
        return true;
    }

    public static boolean hasGamepad() {
        return InputSimulation.isConnected() || !selectedGamepadIds().isEmpty();
    }

    public static List<GamepadDevice> gamepads() {
        return SdlGamepad.deviceIds().stream()
            .map(deviceId -> new GamepadDevice(deviceId, SdlGamepad.identity(deviceId)))
            .toList();
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
        // The old implementation launched split-screen JVMs here. Controller
        // discovery remains in the input layer; player joining belongs to the
        // future split-screen integration.
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
        return missTime;
    }

    public static void setMissTime(int value) {
        missTime = Math.max(0, value);
    }

    public static int rightClickDelay() {
        return rightClickDelay;
    }

    public static void setRightClickDelay(int value) {
        rightClickDelay = Math.max(0, value);
    }

    public static void tickRightClickDelay() {
        rightClickDelay = Math.max(0, rightClickDelay - 1);
    }

    public static void tickMissTime() {
        missTime = Math.max(0, missTime - 1);
    }

    private static KeyState state(KeyMapping keyMapping) {
        return KEY_STATES.computeIfAbsent(keyMapping, ignored -> new KeyState());
    }

    private static List<Long> selectedGamepadIds() {
        List<Long> devices = SdlGamepad.deviceIds();
        if (devices.isEmpty()) {
            return List.of();
        }
        int index = processSlot();
        if (index >= devices.size()) {
            return List.of();
        }
        return List.of(devices.get(index));
    }

    /** Public read model used by controller-assignment UI and integrations. */
    public record GamepadDevice(long deviceId, @Nullable GamepadIdentity identity) {
    }

    private static int processSlot() {
        String value = System.getenv("JAVAREFORGED_SPLIT_SLOT");
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            int slot = Integer.parseInt(value);
            // Launcher slots are one-based: host/J1=1, child/J2=2, ...
            return Math.max(0, slot - 1);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static final class KeyState {
        private boolean down;
        private int clickCount;

        boolean isDown() { return down; }
        void setDown(boolean down) { this.down = down; }
        int clickCount() { return clickCount; }
        void setClickCount(int value) { clickCount = Math.max(0, value); }
        void incrementClickCount() { clickCount++; }
        boolean consumeClick() {
            if (clickCount <= 0) return false;
            clickCount--;
            return true;
        }
        void release() { down = false; clickCount = 0; }
    }
}

