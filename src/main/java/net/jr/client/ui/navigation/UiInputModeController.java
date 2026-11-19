package net.jr.client.ui.navigation;

import net.jr.client.components.widgets.EditBoxElement;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.runtime.input.InputActionHandler;
import net.jr.client.runtime.state.UiNavigationState;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.input.CursorVisibility;
import net.jr.client.ui.container.actions.ContainerItemSlotFocusController;
import net.jr.mixin.accessors.MouseHandlerAccessor;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

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
                    GamepadInputHandler.syncVirtualCursorToFocus(screen);
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
                && ContainerItemSlotFocusController.isVanillaItemSlotFocusActive(containerScreen);
    }

    public static boolean shouldSuppressPointerHover() {
        return isFocusNavigationActive() && !shouldShowCursorDuringFocus();
    }

    @Nullable
    public static GuiEventListener focusedElement(Screen screen) {
        return ScreenFocusController.resolveFocusedListener(screen);
    }

    public static boolean prepareForFocusNavigation(int keyCode) {
        if (!hasCurrentSlot()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.gui.screen();
        GuiEventListener focusedBefore = screen == null
            ? null
            : ScreenFocusController.resolveFocusedListener(screen);
        Integer focusedSlotBefore = screen instanceof AbstractContainerScreen<?> containerScreen
            ? ContainerItemSlotFocusController.getFocusedItemSlotIndex(containerScreen)
            : null;
        applyKeyboardNavigationInputType(keyCode);
        activateFocusNavigation();
        ScreenFocusController.completeFocusedContainerPath(screen);

        GuiEventListener focusedAfter = screen == null
            ? null
            : ScreenFocusController.resolveFocusedListener(screen);
        Integer focusedSlotAfter = screen instanceof AbstractContainerScreen<?> containerScreen
            ? ContainerItemSlotFocusController.getFocusedItemSlotIndex(containerScreen)
            : null;
        return focusedBefore != focusedAfter
            || !Objects.equals(focusedSlotBefore, focusedSlotAfter);
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
        GamepadInputHandler.activatePhysicalMouseCursor(minecraft);

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
        GamepadInputHandler.syncVirtualCursorToFocus(screen);
        CursorVisibility.setHidden(true);
        CursorVisibility.setReplacementHidden(true);
        CursorVisibility.sync();
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
        GamepadInputHandler.activatePhysicalMouseCursor(minecraft);
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

    public static boolean applyContainerItemSlotFocusFallback(Screen screen, int keyCode) {
        return screen instanceof AbstractContainerScreen<?> containerScreen
            && ContainerItemSlotFocusController.moveFocus(containerScreen, keyCode);
    }

    public static boolean applyUnifiedContainerFocusNavigation(Screen screen, int keyCode) {
        return ScreenFocusController.moveUnifiedContainerFocus(screen, keyCode);
    }

    private static void applyPointerInputTypeAndClearFocus(
            Minecraft minecraft,
            boolean controllerPointer
    ) {
        minecraft.setLastInputType(InputType.MOUSE);
        Screen screen = minecraft.gui.screen();

        boolean preserveFocus = screen != null
                && focusedElement(screen) instanceof EditBoxElement || focusedElement(screen) instanceof EditBox;

        if (screen != null
                && !preserveFocus
                && !screen.isDragging()
                && !InputActionHandler.hasActivePointerGesture()) {
            ScreenFocusController.clearFocusState(screen);
        }
        if (controllerPointer) {
            GamepadInputHandler.releaseFocusedItemSlotCursorToJoystick();
        } else {
            GamepadInputHandler.releaseFocusedItemSlotCursor();
        }
        CursorVisibility.setReplacementHidden(false);
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
