package net.jr.client.runtime.input;

import java.util.List;
import javax.annotation.Nullable;
import net.jr.client.input.gamepad.GamepadIdentity;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.binding.GamepadBindingRegistry;
import net.jr.client.input.mode.InputMode;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.input.binding.KeyMappingState;
import net.jr.client.runtime.state.InputState;
import net.minecraft.client.KeyMapping;

public final class LocalInputContext {
    private LocalInputContext() {
    }

    public static InputMode mode() {
        return inputState().mode();
    }

    public static void markKeyboardMouseInput() {
        inputState(InputDeviceRouter.KEYBOARD_MOUSE_SLOT).markKeyboardMouseInput();
    }

    public static void markGamepadInput() {
        inputState().markGamepadInput();
    }

    public static long lastKeyboardMouseInputNanos() {
        return inputState().lastKeyboardMouseInputNanos();
    }

    public static long lastGamepadInputNanos() {
        return inputState().lastGamepadInputNanos();
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

    public static List<Device> devices() {
        return InputDeviceRouter.devices().stream()
            .map(device -> new Device(device.deviceId(), device.identity()))
            .toList();
    }

    public static void assignGamepad(long deviceId, int slotId) {
        InputDeviceRouter.assignGamepad(deviceId, slotId);
    }

    public static void unassignGamepad(long deviceId) {
        InputDeviceRouter.unassignGamepad(deviceId);
    }

    public static List<Long> selectedGamepadIds() {
        return InputDeviceRouter.gamepadsForSlot(currentSlotId());
    }

    public static boolean suppressForJoinChord(long deviceId, GamepadDigitalInput input) {
        return GamepadJoinTicker.suppressForJoinChord(deviceId, input);
    }

    public static int currentSlotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : InputDeviceRouter.KEYBOARD_MOUSE_SLOT;
    }

    private static KeyMappingState keyMappingState(KeyMapping keyMapping) {
        return inputState().state(keyMapping);
    }

    public static boolean isDown(KeyMapping keyMapping) {
        return keyMappingState(keyMapping).isDown();
    }

    public static void setDown(KeyMapping keyMapping, boolean down) {
        keyMappingState(keyMapping).setDown(down);
    }

    public static void click(KeyMapping keyMapping) {
        keyMappingState(keyMapping).incrementClickCount();
    }

    public static boolean consumeClick(KeyMapping keyMapping) {
        return keyMappingState(keyMapping).consumeClick();
    }

    public static void release(KeyMapping keyMapping) {
        keyMappingState(keyMapping).release();
    }

    public static int clickCount(KeyMapping keyMapping) {
        return keyMappingState(keyMapping).clickCount();
    }

    public static void setClickCount(KeyMapping keyMapping, int clickCount) {
        keyMappingState(keyMapping).setClickCount(clickCount);
    }

    public static void resetSlot(int slotId) {
        SlotScope.run(slotId, () -> {
            GamepadBindingRegistry.get().resetAppliedBindings();
            inputState(slotId).clear();
        });
    }

    private static InputState inputState() {
        return inputState(currentSlotId());
    }

    private static InputState inputState(int slotId) {
        return ClientRuntime.INSTANCE.slots().slot(slotId).inputState();
    }

    public record Device(long deviceId, @Nullable GamepadIdentity identity) {
    }
}
