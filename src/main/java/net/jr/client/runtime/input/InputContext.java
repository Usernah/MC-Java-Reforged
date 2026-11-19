package net.jr.client.runtime.input;

import net.jr.client.input.binding.GamepadBindingRegistry;
import net.jr.api.client.input.InputMode;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.input.binding.KeyMappingState;
import net.jr.client.runtime.state.InputState;
import net.minecraft.client.KeyMapping;

public final class InputContext {
    private InputContext() {
    }

    public static InputMode mode() {
        return inputState().mode();
    }

    public static void markKeyboardMouseInput() {
        inputState(InputAssignment.KEYBOARD_MOUSE_SLOT).markKeyboardMouseInput();
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
        return InputAssignment.KEYBOARD_MOUSE_SLOT;
    }

    public static boolean canPhysicalMouseDriveSlot(int slotId) {
        return ClientRuntime.INSTANCE.slots().slot(slotId).input().keyboardMouse() != null;
    }

    public static boolean canPhysicalKeyboardDriveSlot(int slotId) {
        return ClientRuntime.INSTANCE.slots().slot(slotId).input().keyboardMouse() != null;
    }

    public static boolean hasGamepadForSlot(int slotId) {
        return ClientRuntime.INSTANCE.slots().slot(slotId).input().hasGamepad();
    }

    public static int currentSlotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : InputAssignment.KEYBOARD_MOUSE_SLOT;
    }

    private static KeyMappingState keyMappingState(KeyMapping keyMapping) {
        return inputState().state(keyMapping);
    }

    public static boolean isDown(KeyMapping keyMapping) {
        return keyMappingState(keyMapping).isDown();
    }

    public static boolean isPhysicallyDown(KeyMapping keyMapping) {
        return keyMappingState(keyMapping).isPhysicallyDown();
    }

    public static void setDown(KeyMapping keyMapping, boolean down) {
        keyMappingState(keyMapping).setDown(down);
    }

    public static void setPhysicallyDown(KeyMapping keyMapping, boolean down) {
        keyMappingState(keyMapping).setPhysicallyDown(down);
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

}
