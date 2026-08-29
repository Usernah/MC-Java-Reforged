package net.jr.client.ui.navigation;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.state.UiNavigationState;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.input.LocalCursorVisibility;
import net.jr.client.runtime.input.LocalGamepadInputHandler;
import net.jr.client.runtime.input.LocalInputActionHandler;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.jr.mixin.accessors.MouseHandlerAccessor;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class UiInputModeController {
    private static final double RAW_MOUSE_MOVE_EPSILON = 0.25D;

    private UiInputModeController() {
    }

    public static void updateCurrentClientFrame(Minecraft minecraft) {
        Screen screen = minecraft.gui.screen();
        UiNavigationState state = state();
        if (screen == null) {
            state.setFocusNavigationActive(false);
            state.setObservedScreen(null);
            observeCurrentRawMouse(minecraft);
            return;
        }

        if (screen != state.observedScreen()) {
            state.setObservedScreen(screen);
            observeCurrentRawMouse(minecraft);
            if (isFocusNavigationActive()) {
                if (!ScreenFocusController.hasNavigationFocus(screen)) {
                    ScreenFocusController.initializeFocusFromPointer(screen, minecraft);
                } else {
                    LocalGamepadInputHandler.syncVirtualCursorToFocus(screen);
                }
            }
        }

        if (isFocusNavigationActive()) {
            if (canPhysicalMouseDriveCurrentSlot() && hasPhysicalMouseMoved(minecraft)) {
                activatePointerModeFromPhysicalMouse(minecraft);
            } else if (!canPhysicalMouseDriveCurrentSlot()) {
                observeCurrentRawMouse(minecraft);
            }
            return;
        }

        observeCurrentRawMouse(minecraft);
    }

    public static boolean isFocusNavigationActive() {
        return hasCurrentSlot() && state().focusNavigationActive();
    }

    public static boolean shouldShowCursorDuringFocus() {
        if (!isFocusNavigationActive()) {
            return false;
        }

        Screen screen = currentScreen();
        if (screen instanceof FocusCursorVisibilityHost host
                && host.shouldShowCursorDuringFocus()) {
            return true;
        }

        return screen instanceof AbstractContainerScreen<?> containerScreen
                && ContainerSlotFocusController.isVanillaSlotFocusActive(containerScreen);
    }

    public static boolean shouldSuppressPointerHover() {
        return isFocusNavigationActive() && !shouldShowCursorDuringFocus();
    }

    public static void prepareForFocusNavigation(int keyCode) {
        if (!hasCurrentSlot()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        applyKeyboardNavigationInputType(keyCode);
        activateFocusNavigation();
        ScreenFocusController.completeFocusedContainerPath(minecraft.gui.screen());
    }

    public static void notifyJoystickPointerActivity() {
        if (!hasCurrentSlot()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() == null) {
            return;
        }
        if (isFocusNavigationActive()) {
            activatePointerModeFromJoystick();
            return;
        }
        applyPointerInputTypeAndClearFocus(minecraft, true);
    }

    public static void notifyPhysicalPointerActivity() {
        if (!hasCurrentSlot() || !canPhysicalMouseDriveCurrentSlot()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() == null) {
            return;
        }
        if (isFocusNavigationActive()) {
            activatePointerModeFromPhysicalMouse(minecraft);
            return;
        }
        LocalGamepadInputHandler.activatePhysicalMouseCursor(minecraft);

        if (minecraft.gui.screen() != null && minecraft.gui.screen().isDragging()) {
            minecraft.setLastInputType(InputType.MOUSE);
            return;
        }
        applyPointerInputTypeAndClearFocus(minecraft, false);
    }

    public static void activateFocusNavigation() {
        if (!hasCurrentSlot()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.gui.screen();
        if (screen == null || isFocusNavigationActive()) {
            return;
        }

        if (!ScreenFocusController.hasNavigationFocus(screen)) {
            ScreenFocusController.clearFocusState(screen);
            ScreenFocusController.initializeFocusFromPointer(screen, minecraft);
        }
        state().setFocusNavigationActive(true);
        LocalGamepadInputHandler.syncVirtualCursorToFocus(screen);
        LocalCursorVisibility.setHidden(true);
        LocalCursorVisibility.setReplacementHidden(true);
        LocalCursorVisibility.sync();
        observeCurrentRawMouse(minecraft);
    }

    public static void activatePointerModeFromJoystick() {
        if (!hasCurrentSlot()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() == null) {
            state().setFocusNavigationActive(false);
            return;
        }
        if (!isFocusNavigationActive()) {
            observeCurrentRawMouse(minecraft);
            return;
        }

        UiNavigationState state = state();
        state.setFocusNavigationActive(false);
        state.setLastObservedRawMouseX(getRawMouseX(minecraft));
        state.setLastObservedRawMouseY(getRawMouseY(minecraft));
        state.setHasObservedRawMousePosition(true);
        applyPointerInputTypeAndClearFocus(minecraft, true);
    }

    private static void activatePointerModeFromPhysicalMouse(Minecraft minecraft) {
        if (!canPhysicalMouseDriveCurrentSlot()) {
            return;
        }
        UiNavigationState state = state();
        state.setFocusNavigationActive(false);
        state.setLastObservedRawMouseX(getRawMouseX(minecraft));
        state.setLastObservedRawMouseY(getRawMouseY(minecraft));
        state.setHasObservedRawMousePosition(true);
        LocalGamepadInputHandler.activatePhysicalMouseCursor(minecraft);
        applyPointerInputTypeAndClearFocus(minecraft, false);
    }

    static void activatePointerModeFromMouseInteraction() {
        if (!canPhysicalMouseDriveCurrentSlot()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() == null) {
            state().setFocusNavigationActive(false);
            return;
        }
        activatePointerModeFromPhysicalMouse(minecraft);
    }

    private static boolean hasPhysicalMouseMoved(Minecraft minecraft) {
        if (!canPhysicalMouseDriveCurrentSlot()) {
            return false;
        }
        double rawMouseX = getRawMouseX(minecraft);
        double rawMouseY = getRawMouseY(minecraft);
        UiNavigationState state = state();
        if (!state.hasObservedRawMousePosition()) {
            state.setLastObservedRawMouseX(rawMouseX);
            state.setLastObservedRawMouseY(rawMouseY);
            state.setHasObservedRawMousePosition(true);
            return false;
        }

        boolean moved = Math.abs(rawMouseX - state.lastObservedRawMouseX()) >= RAW_MOUSE_MOVE_EPSILON
            || Math.abs(rawMouseY - state.lastObservedRawMouseY()) >= RAW_MOUSE_MOVE_EPSILON;
        state.setLastObservedRawMouseX(rawMouseX);
        state.setLastObservedRawMouseY(rawMouseY);
        return moved;
    }

    private static void observeCurrentRawMouse(Minecraft minecraft) {
        UiNavigationState state = state();
        state.setLastObservedRawMouseX(getRawMouseX(minecraft));
        state.setLastObservedRawMouseY(getRawMouseY(minecraft));
        state.setHasObservedRawMousePosition(true);
    }

    private static UiNavigationState state() {
        return ClientRuntime.INSTANCE.slots().slot(InputApi.currentSlotId()).inputState().uiNavigation();
    }

    private static double getRawMouseX(Minecraft minecraft) {
        return mouseAccessor(minecraft).javaReforged$getXpos();
    }

    private static double getRawMouseY(Minecraft minecraft) {
        return mouseAccessor(minecraft).javaReforged$getYpos();
    }

    private static Screen currentScreen() {
        return Minecraft.getInstance().gui.screen();
    }

    static boolean hasCurrentSlot() {
        return Minecraft.getInstance() != null;
    }

    @Nullable
    static Screen currentEventScreen(Screen eventScreen) {
        Screen currentScreen = Minecraft.getInstance().gui.screen();
        return currentScreen == eventScreen ? currentScreen : null;
    }

    static boolean canPhysicalMouseDriveCurrentSlot() {
        return hasCurrentSlot() && InputApi.canPhysicalMouseDrive();
    }

    private static MouseHandlerAccessor mouseAccessor(Minecraft minecraft) {
        return (MouseHandlerAccessor) minecraft.mouseHandler;
    }

    static boolean isFocusNavigationKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_TAB
            || keyCode == GLFW.GLFW_KEY_UP
            || keyCode == GLFW.GLFW_KEY_DOWN
            || keyCode == GLFW.GLFW_KEY_LEFT
            || keyCode == GLFW.GLFW_KEY_RIGHT;
    }

    static boolean isContainerSlotNavigationKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_UP
            || keyCode == GLFW.GLFW_KEY_DOWN
            || keyCode == GLFW.GLFW_KEY_LEFT
            || keyCode == GLFW.GLFW_KEY_RIGHT;
    }

    public static boolean applyContainerSlotFocusFallback(Screen screen, int keyCode) {
        return screen instanceof AbstractContainerScreen<?> containerScreen
            && ContainerSlotFocusController.moveFocus(containerScreen, keyCode);
    }

    public static boolean shouldPrioritizeContainerSlotFocus(Screen screen) {
        return screen instanceof AbstractContainerScreen<?>;
    }

    private static void applyPointerInputTypeAndClearFocus(
            Minecraft minecraft,
            boolean controllerPointer
    ) {
        minecraft.setLastInputType(InputType.MOUSE);
        Screen screen = minecraft.gui.screen();

        if (screen == null
            || (!screen.isDragging() && !LocalInputActionHandler.hasActivePointerGesture())) {
            ScreenFocusController.clearFocusState(screen);
        }
        if (controllerPointer) {
            LocalGamepadInputHandler.releaseFocusedSlotCursorToJoystick();
        } else {
            LocalGamepadInputHandler.releaseFocusedSlotCursor();
        }
        LocalCursorVisibility.setReplacementHidden(false);
    }

    static void applyKeyboardNavigationInputType(int keyCode) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.setLastInputType(keyCode == GLFW.GLFW_KEY_TAB
                ? InputType.KEYBOARD_TAB
                : InputType.KEYBOARD_ARROW);
        }
    }

}
