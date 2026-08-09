package net.jr.client.ui.navigation;

import net.jr.Java_reforged;
import net.jr.client.input.InputApi;
import net.jr.client.input.cursor.CursorHider;
import net.jr.client.input.cursor.MouseCoordinates;
import net.jr.client.input.runtime.GamepadInputProcessor;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.jr.mixin.uifocus.MouseHandlerAccessor;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Owns the pointer/focus transition rules used by the original controller
 * implementation. The old custom MainScreen/UILayout branches intentionally
 * remain outside this class until that UI framework itself is ported; every
 * vanilla Screen and AbstractContainerScreen path is preserved here.
 */
@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class UiInputModeController {
    private static final class UiInputState {
        UiInputMode inputMode = UiInputMode.POINTER;
        Screen observedScreen;
        boolean hasObservedRawMousePosition;
        double lastObservedRawMouseX;
        double lastObservedRawMouseY;
    }

    private static final double RAW_MOUSE_MOVE_EPSILON = 0.25D;
    private static final UiInputState STATE = new UiInputState();

    private UiInputModeController() {
    }

    /** Called once per frame for this JVM's vanilla client. */
    public static void updateCurrentClientFrame(Minecraft minecraft) {
        Screen screen = minecraft.gui.screen();
        UiInputState state = STATE;
        if (screen == null) {
            state.inputMode = UiInputMode.POINTER;
            state.observedScreen = null;
            observeCurrentRawMouse(minecraft);
            return;
        }

        if (screen != state.observedScreen) {
            state.observedScreen = screen;
            observeCurrentRawMouse(minecraft);
            if (isFocusNavigationActive()) {
                initializeFocusForNavigation(screen, minecraft);
            }
        }

        if (isFocusNavigationActive()) {
            if (canPhysicalMouseDriveCurrentClient() && hasPhysicalMouseMoved(minecraft)) {
                activatePointerModeFromPhysicalMouse(minecraft);
            } else if (!canPhysicalMouseDriveCurrentClient()) {
                observeCurrentRawMouse(minecraft);
            }
            return;
        }

        observeCurrentRawMouse(minecraft);
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!hasCurrentClient()) {
            return;
        }
        Screen screen = currentEventScreen(event.getScreen());
        if (screen == null) {
            return;
        }
        if (isFocusNavigationKey(event.getKeyCode())) {
            applyKeyboardNavigationInputType(Minecraft.getInstance(), event.getKeyCode());
            activateFocusNavigation();
        }
        if (isContainerSlotNavigationKey(event.getKeyCode())
            && screen instanceof AbstractContainerScreen<?>) {
            boolean handled = applyContainerSlotFocusFallback(screen, event.getKeyCode());
            event.setCanceled(true);
            if (handled) {
                InputActionSounds.playHover();
                screen.afterKeyboardAction();
            }
        }
    }

    @SubscribeEvent
    public static void onScreenKeyPressedPost(ScreenEvent.KeyPressed.Post event) {
        if (!hasCurrentClient() || !isFocusNavigationKey(event.getKeyCode())) {
            return;
        }

        Screen screen = currentEventScreen(event.getScreen());
        if (screen == null) {
            return;
        }

        if (screen instanceof AbstractContainerScreen<?>
            && isContainerSlotNavigationKey(event.getKeyCode())) {
            return;
        }

        if (applyContainerSlotFocusFallback(screen, event.getKeyCode())) {
            screen.afterKeyboardAction();
        }
    }

    @SubscribeEvent
    public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (hasCurrentClient() && canPhysicalMouseDriveCurrentClient() && isFocusNavigationActive()) {
            activatePointerModeFromMouseInteraction();
        }
    }

    @SubscribeEvent
    public static void onMouseButtonReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (hasCurrentClient() && canPhysicalMouseDriveCurrentClient() && isFocusNavigationActive()) {
            activatePointerModeFromMouseInteraction();
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (hasCurrentClient() && canPhysicalMouseDriveCurrentClient() && isFocusNavigationActive()) {
            activatePointerModeFromMouseInteraction();
        }
    }

    public static boolean isFocusNavigationActive() {
        return hasCurrentClient() && STATE.inputMode == UiInputMode.FOCUS;
    }

    public static boolean shouldShowCursorDuringFocus() {
        return isFocusNavigationActive()
            && currentScreen() instanceof AbstractContainerScreen<?> containerScreen
            && ContainerSlotFocusController.isVanillaSlotFocusActive(containerScreen);
    }

    public static boolean shouldSuppressPointerHover() {
        return isFocusNavigationActive() && !shouldShowCursorDuringFocus();
    }

    public static void prepareForFocusNavigation(int keyCode) {
        if (!hasCurrentClient()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        applyKeyboardNavigationInputType(minecraft, keyCode);
        activateFocusNavigation();
    }

    public static void notifyJoystickPointerActivity() {
        if (!hasCurrentClient()) {
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
        applyPointerInputTypeAndClearFocus(minecraft);
    }

    public static void notifyPhysicalPointerActivity() {
        if (!hasCurrentClient() || !canPhysicalMouseDriveCurrentClient()) {
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
        GamepadInputProcessor.activatePhysicalMouseCursor(minecraft);
        // Do not clear the focus path while a mouse press is being dragged.
        // Screen/UILayout use that path to deliver mouseReleased to the
        // component that captured mouseClicked.
        if (minecraft.gui.screen() != null && minecraft.gui.screen().isDragging()) {
            minecraft.setLastInputType(InputType.MOUSE);
            return;
        }
        applyPointerInputTypeAndClearFocus(minecraft);
    }

    public static void activateFocusNavigation() {
        if (!hasCurrentClient()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.gui.screen();
        if (screen == null || isFocusNavigationActive()) {
            return;
        }

        initializeContainerFocusFromPointer(screen, minecraft);
        STATE.inputMode = UiInputMode.FOCUS;
        CursorHider.setHidden(true);
        CursorHider.setReplacementHidden(true);
        CursorHider.sync();
        observeCurrentRawMouse(minecraft);
    }

    public static void activatePointerModeFromJoystick() {
        if (!hasCurrentClient()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() == null) {
            STATE.inputMode = UiInputMode.POINTER;
            return;
        }
        if (!isFocusNavigationActive()) {
            observeCurrentRawMouse(minecraft);
            return;
        }

        STATE.inputMode = UiInputMode.POINTER;
        STATE.lastObservedRawMouseX = getRawMouseX(minecraft);
        STATE.lastObservedRawMouseY = getRawMouseY(minecraft);
        STATE.hasObservedRawMousePosition = true;
        applyPointerInputTypeAndClearFocus(minecraft);
    }

    private static void activatePointerModeFromPhysicalMouse(Minecraft minecraft) {
        if (!canPhysicalMouseDriveCurrentClient()) {
            return;
        }
        STATE.inputMode = UiInputMode.POINTER;
        STATE.lastObservedRawMouseX = getRawMouseX(minecraft);
        STATE.lastObservedRawMouseY = getRawMouseY(minecraft);
        STATE.hasObservedRawMousePosition = true;
        GamepadInputProcessor.activatePhysicalMouseCursor(minecraft);
        applyPointerInputTypeAndClearFocus(minecraft);
    }

    private static void activatePointerModeFromMouseInteraction() {
        if (!canPhysicalMouseDriveCurrentClient()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() == null) {
            STATE.inputMode = UiInputMode.POINTER;
            return;
        }
        activatePointerModeFromPhysicalMouse(minecraft);
    }

    private static boolean hasPhysicalMouseMoved(Minecraft minecraft) {
        if (!canPhysicalMouseDriveCurrentClient()) {
            return false;
        }
        double rawMouseX = getRawMouseX(minecraft);
        double rawMouseY = getRawMouseY(minecraft);
        if (!STATE.hasObservedRawMousePosition) {
            STATE.lastObservedRawMouseX = rawMouseX;
            STATE.lastObservedRawMouseY = rawMouseY;
            STATE.hasObservedRawMousePosition = true;
            return false;
        }

        boolean moved = Math.abs(rawMouseX - STATE.lastObservedRawMouseX) >= RAW_MOUSE_MOVE_EPSILON
            || Math.abs(rawMouseY - STATE.lastObservedRawMouseY) >= RAW_MOUSE_MOVE_EPSILON;
        STATE.lastObservedRawMouseX = rawMouseX;
        STATE.lastObservedRawMouseY = rawMouseY;
        return moved;
    }

    private static void observeCurrentRawMouse(Minecraft minecraft) {
        STATE.lastObservedRawMouseX = getRawMouseX(minecraft);
        STATE.lastObservedRawMouseY = getRawMouseY(minecraft);
        STATE.hasObservedRawMousePosition = true;
    }

    private static double getRawMouseX(Minecraft minecraft) {
        return mouseAccessor(minecraft).javareforged$getXpos();
    }

    private static double getRawMouseY(Minecraft minecraft) {
        return mouseAccessor(minecraft).javareforged$getYpos();
    }

    private static Screen currentScreen() {
        return Minecraft.getInstance().gui.screen();
    }

    private static boolean hasCurrentClient() {
        return Minecraft.getInstance() != null;
    }

    @Nullable
    private static Screen currentEventScreen(Screen eventScreen) {
        Screen currentScreen = Minecraft.getInstance().gui.screen();
        return currentScreen == eventScreen ? currentScreen : null;
    }

    private static boolean canPhysicalMouseDriveCurrentClient() {
        return hasCurrentClient() && InputApi.canPhysicalMouseDrive();
    }

    private static MouseHandlerAccessor mouseAccessor(Minecraft minecraft) {
        return (MouseHandlerAccessor) minecraft.mouseHandler;
    }

    private static boolean isFocusNavigationKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_TAB
            || keyCode == GLFW.GLFW_KEY_UP
            || keyCode == GLFW.GLFW_KEY_DOWN
            || keyCode == GLFW.GLFW_KEY_LEFT
            || keyCode == GLFW.GLFW_KEY_RIGHT;
    }

    private static boolean isContainerSlotNavigationKey(int keyCode) {
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

    private static void applyPointerInputTypeAndClearFocus(Minecraft minecraft) {
        minecraft.setLastInputType(InputType.MOUSE);
        clearFocusState(minecraft.gui.screen());
        GamepadInputProcessor.releaseFocusedSlotCursor();
        CursorHider.setReplacementHidden(false);
    }

    private static void applyKeyboardNavigationInputType(Minecraft minecraft, int keyCode) {
        if (minecraft != null) {
            minecraft.setLastInputType(keyCode == GLFW.GLFW_KEY_TAB
                ? InputType.KEYBOARD_TAB
                : InputType.KEYBOARD_ARROW);
        }
    }

    private static void clearFocusState(Screen screen) {
        if (screen == null) {
            return;
        }
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            ContainerSlotFocusController.clearFocusedSlot(containerScreen);
            return;
        }
        screen.clearFocus();
    }

    private static void initializeContainerFocusFromPointer(Screen screen, Minecraft minecraft) {
        double mouseGuiX = resolveFocusSeedGuiX(minecraft);
        double mouseGuiY = resolveFocusSeedGuiY(minecraft);
        if (screen instanceof AbstractContainerScreen<?> containerScreen
            && ContainerSlotFocusController.initializeFocusFromPointer(containerScreen, mouseGuiX, mouseGuiY)) {
            return;
        }
        initializeListenerFocusFromPointer(screen, mouseGuiX, mouseGuiY);
    }

    private static void initializeFocusForNavigation(Screen screen, Minecraft minecraft) {
        initializeContainerFocusFromPointer(screen, minecraft);
    }

    private static double resolveFocusSeedGuiX(Minecraft minecraft) {
        return GamepadInputProcessor.isJoystickCursorActive()
            ? GamepadInputProcessor.cursorX()
            : MouseCoordinates.rawMouseToGlobalGuiX(minecraft, minecraft.mouseHandler.xpos());
    }

    private static double resolveFocusSeedGuiY(Minecraft minecraft) {
        return GamepadInputProcessor.isJoystickCursorActive()
            ? GamepadInputProcessor.cursorY()
            : MouseCoordinates.rawMouseToGlobalGuiY(minecraft, minecraft.mouseHandler.ypos());
    }

    private static void initializeListenerFocusFromPointer(Screen screen, double mouseGuiX, double mouseGuiY) {
        if (screen == null || resolveFocusedListener(screen) != null) {
            return;
        }
        ComponentPath hoveredPath = findHoveredFocusPath(screen, mouseGuiX, mouseGuiY);
        if (hoveredPath != null) {
            applyFocusPath(screen, hoveredPath);
            return;
        }
        GuiEventListener target = findNearestListener(screen, mouseGuiX, mouseGuiY);
        if (target != null) {
            applyFocusedListener(screen, target);
        }
    }

    @Nullable
    private static GuiEventListener resolveFocusedListener(Screen screen) {
        GuiEventListener focusedFromPath = extractFocusedLeaf(screen.getCurrentFocusPath());
        return focusedFromPath != null ? focusedFromPath : screen.getFocused();
    }

    private static void applyFocusedListener(Screen screen, GuiEventListener listener) {
        applyFocusPath(screen, buildInitialFocusPath(screen, listener));
    }

    @Nullable
    private static ComponentPath findHoveredFocusPath(Screen screen, double mouseGuiX, double mouseGuiY) {
        for (GuiEventListener listener : resolveTopLevelFocusableListeners(screen)) {
            ComponentPath childPath = findHoveredFocusPath(listener, mouseGuiX, mouseGuiY);
            if (childPath != null) {
                return ComponentPath.path(screen, childPath);
            }
        }
        return null;
    }

    @Nullable
    private static ComponentPath findHoveredFocusPath(
        GuiEventListener listener,
        double mouseGuiX,
        double mouseGuiY
    ) {
        if (!isFocusableListener(listener) || !listener.isMouseOver(mouseGuiX, mouseGuiY)) {
            return null;
        }
        if (listener instanceof ContainerEventHandler container) {
            var hoveredChild = container.getChildAt(mouseGuiX, mouseGuiY);
            if (hoveredChild.isPresent()) {
                ComponentPath childPath = findHoveredFocusPath(hoveredChild.get(), mouseGuiX, mouseGuiY);
                if (childPath != null) {
                    return ComponentPath.path(container, childPath);
                }
            }
        }
        ComponentPath initialPath = listener.nextFocusPath(new FocusNavigationEvent.InitialFocus());
        return initialPath != null ? initialPath : ComponentPath.leaf(listener);
    }

    @Nullable
    private static GuiEventListener findNearestListener(Screen screen, double mouseGuiX, double mouseGuiY) {
        GuiEventListener bestListener = null;
        double bestDistance = Double.MAX_VALUE;
        for (GuiEventListener listener : resolveTopLevelFocusableListeners(screen)) {
            if (!isFocusableListener(listener)) {
                continue;
            }
            ScreenRectangle rectangle = resolveListenerRectangle(listener);
            if (isRectangleEmpty(rectangle)) {
                continue;
            }
            double centerX = rectangle.getCenterInAxis(ScreenAxis.HORIZONTAL);
            double centerY = rectangle.getCenterInAxis(ScreenAxis.VERTICAL);
            double distance = squaredDistance(centerX, centerY, mouseGuiX, mouseGuiY);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestListener = listener;
            }
        }
        return bestListener;
    }

    private static List<GuiEventListener> resolveTopLevelFocusableListeners(Screen screen) {
        LinkedHashSet<GuiEventListener> listeners = new LinkedHashSet<>();
        listeners.addAll(screen.children());
        return new ArrayList<>(listeners);
    }

    private static boolean isFocusableListener(GuiEventListener listener) {
        return !(listener instanceof AbstractWidget widget) || (widget.visible && widget.active);
    }

    private static ScreenRectangle resolveListenerRectangle(GuiEventListener listener) {
        if (listener instanceof AbstractWidget widget) {
            return new ScreenRectangle(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
        }
        return listener.getRectangle();
    }

    private static boolean isRectangleEmpty(ScreenRectangle rectangle) {
        return rectangle.width() <= 0 || rectangle.height() <= 0;
    }

    @Nullable
    private static GuiEventListener extractFocusedLeaf(@Nullable ComponentPath path) {
        if (path == null) {
            return null;
        }
        if (path instanceof ComponentPath.Leaf leaf) {
            return leaf.component();
        }
        if (path instanceof ComponentPath.Path branch) {
            return extractFocusedLeaf(branch.childPath());
        }
        return path.component();
    }

    private static void applyFocusPath(Screen screen, @Nullable ComponentPath path) {
        if (path != null) {
            screen.clearFocus();
            path.applyFocus(true);
        }
    }

    @Nullable
    private static ComponentPath buildInitialFocusPath(Screen screen, GuiEventListener listener) {
        ComponentPath childPath = listener.nextFocusPath(new FocusNavigationEvent.InitialFocus());
        if (childPath == null) {
            childPath = ComponentPath.leaf(listener);
        }
        return ComponentPath.path(screen, childPath);
    }

    private static double squaredDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private enum UiInputMode {
        POINTER,
        FOCUS
    }
}
