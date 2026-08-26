package net.jr.client.runtime.input;

import net.jr.Java_reforged;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.input.gamepad.GamepadAxis;
import net.jr.client.input.gamepad.GamepadGameplayInputHandler;
import net.jr.client.input.gamepad.GamepadInputCurve;
import net.jr.client.input.InputApi;
import net.jr.client.ui.cursor.CursorMagnetTarget;
import net.jr.client.ui.cursor.CursorAnimator;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionFeedback;
import net.jr.client.components.navigation.UiActionHandler;
import net.jr.client.components.widgets.ScrollPanelElement;
import net.jr.mixin.accessors.MouseHandlerAccessor;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.jr.client.ui.container.actions.ContainerInventoryActionHandler;
import net.jr.client.ui.container.slots.CustomSlotLayout;
import net.jr.client.runtime.input.LocalCursorVisibility;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.client.ui.cursor.MouseCoordinates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class LocalGamepadInputHandler {

    private LocalGamepadInputHandler() {
    }

    private static final float DEADZONE = 0.2f;
    private static final float CURSOR_SPEED = 15.0f;
    private static final float CURSOR_INPUT_EPSILON = 0.0001f;
    private static final double RAW_MOUSE_MOVE_EPSILON = 0.25D;
    private static final double CAMERA_FRAME_SCALE_MAX = 2.0D;
    private static final double CAMERA_TARGET_FPS = 60.0D;
    private static final double CURSOR_FRAME_SCALE_MIN = 0.25D;
    private static final double CURSOR_FRAME_SCALE_MAX = 2.0D;
    private static final double CURSOR_TARGET_FPS = 60.0D;
    private static final int FOCUS_CURSOR_ANIMATION_MS = 50;
    private static final String FOCUS_CURSOR_ANIMATION_X = "java_reforged.focus_slot_cursor_x";
    private static final String FOCUS_CURSOR_ANIMATION_Y = "java_reforged.focus_slot_cursor_y";

    private static final LocalInputFrameClock[] CAMERA_CLOCKS = new LocalInputFrameClock[LocalClientSlotRegistry.MAX_SLOTS];
    private static final LocalInputFrameClock[] CURSOR_CLOCKS = new LocalInputFrameClock[LocalClientSlotRegistry.MAX_SLOTS];
    private static final LocalCursorState[] CURSOR_STATES = new LocalCursorState[LocalClientSlotRegistry.MAX_SLOTS];

    static {
        for (int slotId = 0; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            CAMERA_CLOCKS[slotId] = new LocalInputFrameClock(CAMERA_TARGET_FPS, 0.0D, CAMERA_FRAME_SCALE_MAX, 0.0D);
            CURSOR_CLOCKS[slotId] = new LocalInputFrameClock(CURSOR_TARGET_FPS, CURSOR_FRAME_SCALE_MIN, CURSOR_FRAME_SCALE_MAX, 1.0D);
            CURSOR_STATES[slotId] = new LocalCursorState();
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            int slotId = client.slotId();
            if (event.getEntity() != client.player() || !InputApi.hasGamepadForSlot(slotId)) {
                continue;
            }
            LocalClientExecution.runForClient(slotId, () -> {
                if (ClientRuntime.INSTANCE.slots().slot(slotId).screenState().screen() == null) {
                    GamepadGameplayInputHandler.applyMovement(event);
                }
            });
            return;
        }
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        InputApi.updateGamepads();
        Minecraft minecraft = Minecraft.getInstance();
        if (!hasAnyGamepadConnected()) {
            for (int slotId = 0; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
                LocalCursorVisibility.setHiddenForSlot(slotId, false);
                LocalCursorState state = cursorState(slotId);
                state.cursorInputSource = CursorInputSource.MOUSE;
                state.lastScreen = ClientRuntime.INSTANCE.slots().slot(slotId).screenState().screen();
                resetMouseSourceObservation(minecraft, state);
                resetCameraTiming(slotId);
                cursorClock(slotId).reset();
            }
            return;
        }

        ClientRuntime runtime = ClientRuntime.INSTANCE;
        for (LocalClientSlot slot : runtime.viewports().presentedSlots()) {
            int slotId = slot.id();
            if (!InputApi.hasGamepadForSlot(slotId)) {
                LocalCursorVisibility.setHiddenForSlot(slotId, false);
                resetCameraTiming(slotId);
                cursorClock(slotId).reset();
                continue;
            }

            Runnable frame = () -> {
                UiInputModeController.updateCurrentClientFrame(minecraft);
                processCurrentClientFrame(minecraft);
            };

            if (runtime.clients().hasClient(slotId)) {
                LocalClientExecution.runForClient(slotId, frame);
            } else {
                SlotExecution.runForSlot(minecraft, slotId, frame);
            }
        }
    }

    private static void processCurrentClientFrame(Minecraft minecraft) {
        int slotId = SlotScope.requireId();
        LocalCursorState state = cursorState(slotId);
        Screen screen = ClientRuntime.INSTANCE.slots().slot(slotId).screenState().screen();
        if (screen != null) {
            resetCameraTiming(slotId);
            LocalCursorVisibility.setHiddenForSlot(slotId, true);
            handleVirtualCursorMovement(minecraft, screen, slotId);
            return;
        }
        LocalCursorVisibility.setHiddenForSlot(slotId, false);
        if (state.cursorInputSource == CursorInputSource.FOCUS_SLOT) {
            state.cursorInputSource = InputApi.isGamepadMode()
                ? CursorInputSource.JOYSTICK
                : CursorInputSource.MOUSE;
        }
        resetFocusCursorAnimation(slotId, state.virtualCursorX, state.virtualCursorY);
        state.lastScreen = null;
        cursorClock(slotId).reset();
        LocalClient client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId);
        LocalPlayer player = client != null ? client.player() : null;
        if (!minecraft.isPaused() && player != null) {
            if (!GamepadGameplayInputHandler.turnCamera(player, () -> cameraClock(slotId).sample())) {
                resetCameraTiming(slotId);
            }
        } else {
            resetCameraTiming(slotId);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
    }

    private static void handleVirtualCursorMovement(Minecraft mc, Screen screen, int slotId) {
        LocalCursorState state = cursorState(slotId);
        if (screen != state.lastScreen) {
            resetMouseSourceObservation(mc, state);
            state.lastScreen = screen;
            if (InputApi.isGamepadMode()
                || state.cursorInputSource == CursorInputSource.JOYSTICK
                || state.cursorInputSource == CursorInputSource.MAGNET
                || !InputApi.canPhysicalMouseDrive()) {
                centerVirtualCursor(mc, slotId);
                markCursorAsJoystickDriven(slotId);
            } else {
                activatePhysicalMouseCursor(mc, slotId);
            }
        }

        float stickX = InputApi.axis(GamepadAxis.LEFT_STICK_X, DEADZONE);
        float stickY = InputApi.axis(GamepadAxis.LEFT_STICK_Y, DEADZONE);

        stickX = GamepadInputCurve.response(stickX);
        stickY = GamepadInputCurve.response(stickY);

        boolean joystickMovedCursor = Math.abs(stickX) > CURSOR_INPUT_EPSILON || Math.abs(stickY) > CURSOR_INPUT_EPSILON;
        if (joystickMovedCursor && UiInputModeController.isFocusNavigationActive()) {
            UiInputModeController.activatePointerModeFromJoystick();
        }

        if (state.cursorInputSource == CursorInputSource.MOUSE) {
            syncVirtualCursorWithMouse(mc, slotId);
        }

        if (joystickMovedCursor) {
            if (!isJoystickCursorActive(slotId)) {
                centerVirtualCursor(mc, slotId);
            }
            double frameScale = cursorClock(slotId).sample();
            state.virtualCursorX += stickX * CURSOR_SPEED * frameScale;
            state.virtualCursorY += stickY * CURSOR_SPEED * frameScale;
            markCursorAsJoystickDriven(slotId);
            UiInputModeController.notifyJoystickPointerActivity();
        } else {
            cursorClock(slotId).sample();
        }

        int width = cursorSpaceWidth(mc, slotId, screen);
        int height = cursorSpaceHeight(mc, slotId, screen);
        state.virtualCursorX = Math.max(0, Math.min(width, state.virtualCursorX));
        state.virtualCursorY = Math.max(0, Math.min(height, state.virtualCursorY));

        applyControllerCursorMagnet(screen, slotId, joystickMovedCursor);
    }

    private static void applyControllerCursorMagnet(
            Screen screen,
            int slotId,
            boolean joystickMovedCursor
    ) {
        LocalCursorState state = cursorState(slotId);
        if (joystickMovedCursor
                || UiInputModeController.isFocusNavigationActive()
                || screen.isDragging()
                || LocalInputActionHandler.hasActivePointerGesture()
                || !(screen instanceof CustomSlotLayout layout)
                || !isJoystickCursorActive(slotId)) {
            return;
        }

        CursorMagnetTarget target = layout.cursorMagnetTargetAt(
                state.virtualCursorX,
                state.virtualCursorY
        );
        if (target == null) {
            if (state.cursorInputSource == CursorInputSource.MAGNET) {
                state.cursorInputSource = CursorInputSource.JOYSTICK;
                resetFocusCursorAnimation(slotId, state.virtualCursorX, state.virtualCursorY);
            }
            return;
        }

        if (state.cursorInputSource == CursorInputSource.MAGNET
                && Math.abs(state.focusCursorAnimationTargetX - target.guiX()) < 0.01D
                && Math.abs(state.focusCursorAnimationTargetY - target.guiY()) < 0.01D) {
            return;
        }

        double visualX = visualCursorX(slotId);
        double visualY = visualCursorY(slotId);
        state.virtualCursorX = target.guiX();
        state.virtualCursorY = target.guiY();
        state.focusCursorAnimationStartX = visualX;
        state.focusCursorAnimationStartY = visualY;
        state.focusCursorAnimationTargetX = target.guiX();
        state.focusCursorAnimationTargetY = target.guiY();
        state.focusCursorAnimationActive = true;
        state.cursorInputSource = CursorInputSource.MAGNET;
    }

    public static boolean isJoystickCursorActive() {
        return isJoystickCursorActive(currentCursorSlotId());
    }

    public static boolean isJoystickCursorActive(int slotId) {
        CursorInputSource source = cursorState(slotId).cursorInputSource;
        return source == CursorInputSource.JOYSTICK || source == CursorInputSource.MAGNET;
    }

    public static boolean isVirtualCursorActive() {
        return isVirtualCursorActive(currentCursorSlotId());
    }

    public static boolean isVirtualCursorActive(int slotId) {
        return cursorState(slotId).cursorInputSource != CursorInputSource.MOUSE;
    }

    public static boolean isControllerCursorActive() {
        return isControllerCursorActive(currentCursorSlotId());
    }

    public static boolean isControllerCursorActive(int slotId) {
        return isVirtualCursorActive(slotId);
    }

    public static boolean isPhysicalMouseCursorActive() {
        return isPhysicalMouseCursorActive(currentCursorSlotId());
    }

    public static boolean isPhysicalMouseCursorActive(int slotId) {
        return cursorState(slotId).cursorInputSource == CursorInputSource.MOUSE;
    }

    public static boolean isFocusSlotCursorActive() {
        return isFocusSlotCursorActive(currentCursorSlotId());
    }

    public static boolean isFocusSlotCursorActive(int slotId) {
        return cursorState(slotId).cursorInputSource == CursorInputSource.FOCUS_SLOT;
    }

    public static int resolveScreenMouseX(int vanillaMouseX) {
        return resolveScreenMouseX(currentCursorSlotId(), vanillaMouseX);
    }

    public static int resolveScreenMouseX(int slotId, int vanillaMouseX) {
        if (UiInputModeController.shouldSuppressPointerHover()) {
            return Integer.MIN_VALUE;
        }

        return isVirtualCursorActive(slotId) ? (int) Math.round(cursorState(slotId).virtualCursorX) : vanillaMouseX;
    }

    public static int resolveScreenMouseY(int vanillaMouseY) {
        return resolveScreenMouseY(currentCursorSlotId(), vanillaMouseY);
    }

    public static int resolveScreenMouseY(int slotId, int vanillaMouseY) {
        if (UiInputModeController.shouldSuppressPointerHover()) {
            return Integer.MIN_VALUE;
        }

        return isVirtualCursorActive(slotId) ? (int) Math.round(cursorState(slotId).virtualCursorY) : vanillaMouseY;
    }

    public static double visualCursorX() {
        return visualCursorX(currentCursorSlotId());
    }

    public static double visualCursorX(int slotId) {
        return resolveFocusAnimatedCursorX(slotId);
    }

    public static double visualCursorY() {
        return visualCursorY(currentCursorSlotId());
    }

    public static double visualCursorY(int slotId) {
        return resolveFocusAnimatedCursorY(slotId);
    }

    public static double cursorX() {
        return cursorX(currentCursorSlotId());
    }

    public static double cursorX(int slotId) {
        return cursorState(slotId).virtualCursorX;
    }

    public static double cursorY() {
        return cursorY(currentCursorSlotId());
    }

    public static double cursorY(int slotId) {
        return cursorState(slotId).virtualCursorY;
    }

    public static void moveVirtualCursorToFocusedSlot(double guiX, double guiY) {
        moveVirtualCursorToFocusedSlot(currentCursorSlotId(), guiX, guiY);
    }

    public static void moveVirtualCursorToFocusedSlot(int slotId, double guiX, double guiY) {
        LocalCursorState state = cursorState(slotId);
        double visualX = visualCursorX(slotId);
        double visualY = visualCursorY(slotId);
        state.virtualCursorX = guiX;
        state.virtualCursorY = guiY;
        state.focusCursorAnimationStartX = visualX;
        state.focusCursorAnimationStartY = visualY;
        state.focusCursorAnimationTargetX = guiX;
        state.focusCursorAnimationTargetY = guiY;
        state.focusCursorAnimationActive = true;
        state.cursorInputSource = CursorInputSource.FOCUS_SLOT;
    }

    public static void releaseFocusedSlotCursor() {
        releaseFocusedSlotCursor(currentCursorSlotId());
    }

    public static void releaseFocusedSlotCursorToJoystick() {
        int slotId = currentCursorSlotId();
        LocalCursorState state = cursorState(slotId);
        if (state.cursorInputSource != CursorInputSource.FOCUS_SLOT) {
            return;
        }

        double visualX = visualCursorX(slotId);
        double visualY = visualCursorY(slotId);
        state.virtualCursorX = visualX;
        state.virtualCursorY = visualY;
        state.cursorInputSource = CursorInputSource.JOYSTICK;
        resetFocusCursorAnimation(slotId, visualX, visualY);
    }

    public static void releaseFocusedSlotCursor(int slotId) {
        LocalCursorState state = cursorState(slotId);
        if (state.cursorInputSource == CursorInputSource.FOCUS_SLOT) {
            state.cursorInputSource = CursorInputSource.MOUSE;
        }
        resetFocusCursorAnimation(slotId, state.virtualCursorX, state.virtualCursorY);
    }

    public static void activatePhysicalMouseCursor(Minecraft minecraft) {
        activatePhysicalMouseCursor(minecraft, currentCursorSlotId());
    }

    private static void activatePhysicalMouseCursor(Minecraft minecraft, int slotId) {
        if (minecraft == null || !InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return;
        }
        LocalCursorState state = cursorState(slotId);
        markCursorAsMouseDriven(slotId);
        resetMouseSourceObservation(minecraft, state);
        if (ClientRuntime.INSTANCE.slots().slot(slotId).screenState().screen() != null) {
            syncVirtualCursorWithMouse(minecraft, slotId);
        }
    }

    public static void notifyPhysicalMouseMove(double rawMouseX, double rawMouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        int slotId = InputApi.keyboardMouseSlotId();
        if (minecraft == null || !InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return;
        }

        LocalCursorState state = cursorState(slotId);
        Screen screen = ClientRuntime.INSTANCE.slots().slot(slotId).screenState().screen();
        if (screen != state.lastScreen || !state.hasObservedRawMousePosition) {
            state.lastObservedRawMouseX = rawMouseX;
            state.lastObservedRawMouseY = rawMouseY;
            state.hasObservedRawMousePosition = true;
            return;
        }

        boolean moved = Math.abs(rawMouseX - state.lastObservedRawMouseX) >= RAW_MOUSE_MOVE_EPSILON
            || Math.abs(rawMouseY - state.lastObservedRawMouseY) >= RAW_MOUSE_MOVE_EPSILON;
        state.lastObservedRawMouseX = rawMouseX;
        state.lastObservedRawMouseY = rawMouseY;
        if (!moved) {
            return;
        }

        InputApi.markMouseMove(rawMouseX, rawMouseY);
        activatePhysicalMouseCursor(minecraft, slotId);
        UiInputModeController.notifyPhysicalPointerActivity();
    }

    public static void centerControllerCursorForScreen() {
        centerControllerCursorForScreen(currentCursorSlotId());
    }

    public static void centerControllerCursorForScreen(int slotId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        centerVirtualCursor(minecraft, slotId);
        markCursorAsJoystickDriven(slotId);
    }

    public static void centerPhysicalCursorForScreen(int slotId) {
        Minecraft minecraft = Minecraft.getInstance();
        ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewportOrNull(slotId);
        if (minecraft == null || viewport == null || !InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return;
        }

        double windowX = viewport.windowX() + viewport.windowWidth() / 2.0D;
        double windowY = viewport.windowY() + viewport.windowHeight() / 2.0D;
        GLFW.glfwSetCursorPos(minecraft.getWindow().handle(), windowX, windowY);

        MouseHandlerAccessor mouse = (MouseHandlerAccessor)minecraft.mouseHandler;
        mouse.javaReforged$setXpos(windowX);
        mouse.javaReforged$setYpos(windowY);

        LocalCursorState state = cursorState(slotId);
        state.lastObservedRawMouseX = windowX;
        state.lastObservedRawMouseY = windowY;
        state.hasObservedRawMousePosition = true;
        if (state.cursorInputSource == CursorInputSource.MOUSE) {
            syncVirtualCursorWithMouse(minecraft, slotId);
        }
    }

    static void dispatchFocusNavigation(Screen screen, int keyCode) {
        UiInputModeController.prepareForFocusNavigation(keyCode);
        FocusSnapshot focusBefore = FocusSnapshot.capture(screen);
        UiAction directionalAction = switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> UiAction.MOVE_LEFT;
            case GLFW.GLFW_KEY_RIGHT -> UiAction.MOVE_RIGHT;
            case GLFW.GLFW_KEY_UP -> UiAction.MOVE_UP;
            case GLFW.GLFW_KEY_DOWN -> UiAction.MOVE_DOWN;
            default -> null;
        };

        if (directionalAction != null && dispatchActionToFocusedElement(screen, directionalAction)) {
            playFocusedUiActionFeedback(screen, directionalAction);
            syncVirtualCursorToFocusedComponent(screen);
            screen.afterKeyboardAction();
            return;
        }

        if (directionalAction != null
                && screen instanceof UiActionHandler screenHandler
                && screenHandler.handleUiAction(directionalAction)) {
            if (focusBefore.hasChanged(screen)) {
                InputActionSounds.playFocus();
            }
            syncVirtualCursorToFocusedComponent(screen);
            screen.afterKeyboardAction();
            return;
        }

        boolean handled;
        boolean fallbackHandled;
        if (UiInputModeController.shouldPrioritizeContainerSlotFocus(screen)) {
            fallbackHandled = UiInputModeController.applyContainerSlotFocusFallback(screen, keyCode);
            handled = fallbackHandled;
        } else {
            handled = screen.keyPressed(new net.minecraft.client.input.KeyEvent(keyCode, 0, 0));
            fallbackHandled = handled ? false : UiInputModeController.applyContainerSlotFocusFallback(screen, keyCode);
        }
        if (handled || fallbackHandled) {
            if (focusBefore.hasChanged(screen)) {
                InputActionSounds.playFocus();
            }
            syncVirtualCursorToFocusedComponent(screen);
        }
        screen.afterKeyboardAction();
    }

    private static boolean dispatchActionToFocusedElement(Screen screen, UiAction action) {
        GuiEventListener focused = focusedElement(screen);
        return focused instanceof UiActionHandler handler
            && handler.handleUiAction(action);
    }

    private record FocusSnapshot(GuiEventListener focusedLeaf, Integer focusedSlot) {
        private static FocusSnapshot capture(Screen screen) {
            GuiEventListener focused = LocalGamepadInputHandler.focusedElement(screen);
            Integer slot = screen instanceof AbstractContainerScreen<?> containerScreen
                    ? ContainerSlotFocusController.getFocusedSlotIndex(containerScreen)
                    : null;
            return new FocusSnapshot(focused, slot);
        }

        private boolean hasChanged(Screen screen) {
            FocusSnapshot current = capture(screen);
            return this.focusedLeaf != current.focusedLeaf
                    || !java.util.Objects.equals(this.focusedSlot, current.focusedSlot);
        }

    }

    static boolean focusedElementPrefersDirectAction(Screen screen) {
        GuiEventListener focused = focusedElement(screen);

        return focused instanceof UiActionHandler handler
            && handler.prefersDirectFocusAction();
    }

    static boolean activateFocusedElement(Screen screen, int mouseButton) {
        if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen
            && ContainerSlotFocusController.isVanillaSlotFocusActive(containerScreen)) {
            return ContainerInventoryActionHandler.pickupFocusedOrHoveredSlot(containerScreen, mouseButton);
        }

        if (mouseButton == 0 || mouseButton == 1) {
            GuiEventListener focused = focusedElement(screen);
            if (focused instanceof UiActionHandler handler) {
                boolean handled = handler.handleUiAction(
                        mouseButton == 0 ? UiAction.ACCEPT : UiAction.ALTERNATE
                );
                if (handled) {

                    syncVirtualCursorToFocusedComponent(screen);
                }
                return handled;
            }
        }

        return false;
    }

    static boolean dispatchFocusedUiAction(Screen screen, UiAction action) {
        if (dispatchActionToFocusedElement(screen, action)) {
            return true;
        }

        return screen instanceof UiActionHandler handler && handler.handleUiAction(action);
    }

    static void playFocusedUiActionFeedback(Screen screen, UiAction action) {
        GuiEventListener focused = focusedElement(screen);
        if (focused instanceof UiActionHandler handler) {
            InputActionSounds.play(handler.uiActionFeedback(action));
            return;
        }
        InputActionSounds.play(UiActionFeedback.CLICK);
    }

    static FocusedElementCenter focusedElementCenter(Screen screen) {
        GuiEventListener focused = focusedElement(screen);
        if (focused == null) {
            return null;
        }

        ScreenRectangle rectangle = focused.getRectangle();
        if (rectangle.width() <= 0 || rectangle.height() <= 0) {
            return null;
        }
        return new FocusedElementCenter(
            rectangle.left() + rectangle.width() / 2.0D,
            rectangle.top() + rectangle.height() / 2.0D
        );
    }

    static void dispatchBack(Screen screen) {
        screen.onClose();
        InputActionSounds.playBack();
    }

    static void dispatchUiAction(Screen screen, UiAction action) {
        if (action.isDirectional()) {
            int keyCode = switch (action) {
                case MOVE_LEFT -> GLFW.GLFW_KEY_LEFT;
                case MOVE_RIGHT -> GLFW.GLFW_KEY_RIGHT;
                case MOVE_UP -> GLFW.GLFW_KEY_UP;
                case MOVE_DOWN -> GLFW.GLFW_KEY_DOWN;
                default -> throw new IllegalStateException("Unexpected directional UI action: " + action);
            };
            UiInputModeController.prepareForFocusNavigation(keyCode);
        }
        if (screen instanceof UiActionHandler handler && handler.handleUiAction(action)) {
            InputActionSounds.play(handler.uiActionFeedback(action));
            syncVirtualCursorToFocusedComponent(screen);
        }
    }

    public static void trackFocusedCursorDuringScroll(Screen screen) {
        if (screen == null || !UiInputModeController.isFocusNavigationActive()) {
            return;
        }

        ScrollPanelElement viewport = focusedScrollViewport(screen);
        GuiEventListener focused = focusedElement(screen);
        if (viewport == null || focused == null || !viewport.hasActiveScrollTransition()) {
            return;
        }

        ScreenRectangle rectangle = focused.getRectangle();
        double centerX = rectangle.left() + rectangle.width() / 2.0D;
        double centerY = rectangle.top() + rectangle.height() / 2.0D;
        double viewportRight = viewport.getX() + viewport.getWidth();
        double viewportBottom = viewport.getY() + viewport.getHeight();

        trackVirtualCursorAtFocus(
                Math.max(viewport.getX(), Math.min(viewportRight, centerX)),
                Math.max(viewport.getY(), Math.min(viewportBottom, centerY))
        );
    }

    private static void syncVirtualCursorToFocusedComponent(Screen screen) {
        if (screen == null || !UiInputModeController.isFocusNavigationActive()) {
            return;
        }

        if (screen instanceof AbstractContainerScreen<?> containerScreen
                && ContainerSlotFocusController.isVanillaSlotFocusActive(containerScreen)) {

            ContainerSlotFocusController.FocusedSlotCenter center =
                    ContainerSlotFocusController.getFocusedSlotCenter(containerScreen);

            if (center != null) {
                moveVirtualCursorToFocusedSlot(center.guiX(), center.guiY());
            }

            return;
        }

        GuiEventListener focused = focusedElement(screen);
        if (focused == null) {
            return;
        }

        ScreenRectangle rectangle = focused.getRectangle();
        double centerX = rectangle.left() + rectangle.width() / 2.0D;
        double centerY = rectangle.top() + rectangle.height() / 2.0D;

        moveVirtualCursorToFocusedSlot(centerX, centerY);
    }

    public static void syncVirtualCursorToFocus(Screen screen) {
        syncVirtualCursorToFocusedComponent(screen);
    }

    private static GuiEventListener focusedElement(Screen screen) {
        GuiEventListener current = screen;
        while (current instanceof ContainerEventHandler container) {
            GuiEventListener child = container.getFocused();
            if (child == null || child == current) {
                break;
            }
            current = child;
        }
        if (current != screen) {
            return current;
        }

        GuiEventListener fromPath = focusedLeaf(screen.getCurrentFocusPath());
        return fromPath == screen ? null : fromPath;
    }

    private static ScrollPanelElement focusedScrollViewport(Screen screen) {
        GuiEventListener current = screen;
        ScrollPanelElement viewport = null;
        while (current instanceof ContainerEventHandler container) {
            if (current instanceof ScrollPanelElement panel) {
                viewport = panel;
            }
            GuiEventListener child = container.getFocused();
            if (child == null || child == current) {
                break;
            }
            current = child;
        }
        return current == screen ? null : viewport;
    }

    private static void trackVirtualCursorAtFocus(double guiX, double guiY) {
        int slotId = currentCursorSlotId();
        LocalCursorState state = cursorState(slotId);
        state.virtualCursorX = guiX;
        state.virtualCursorY = guiY;
        state.cursorInputSource = CursorInputSource.FOCUS_SLOT;

        resetFocusCursorAnimation(slotId, guiX, guiY);
    }

    private static GuiEventListener focusedLeaf(ComponentPath path) {
        if (path == null) {
            return null;
        }
        if (path instanceof ComponentPath.Leaf leaf) {
            return leaf.component();
        }
        if (path instanceof ComponentPath.Path branch) {
            return focusedLeaf(branch.childPath());
        }
        return path.component();
    }

    private static void syncVirtualCursorWithMouse(Minecraft mc, int slotId) {
        if (!InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return;
        }

        LocalCursorState state = cursorState(slotId);
        if (ClientRuntime.INSTANCE.viewports().hasViewport(slotId)) {
            ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewport(slotId);
            state.virtualCursorX = (mc.mouseHandler.xpos() - viewport.windowX())
                * ViewportGuiScale.logicalWidth(viewport) / viewport.windowWidth();
            state.virtualCursorY = (mc.mouseHandler.ypos() - viewport.windowY())
                * ViewportGuiScale.logicalHeight(viewport) / viewport.windowHeight();
        } else {
            state.virtualCursorX = MouseCoordinates.rawMouseToGlobalGuiX(mc, mc.mouseHandler.xpos());
            state.virtualCursorY = MouseCoordinates.rawMouseToGlobalGuiY(mc, mc.mouseHandler.ypos());
        }
        resetFocusCursorAnimation(slotId, state.virtualCursorX, state.virtualCursorY);
    }

    private static void centerVirtualCursor(Minecraft mc, int slotId) {
        Screen screen = ClientRuntime.INSTANCE.slots().slot(slotId).screenState().screen();
        int width = cursorSpaceWidth(mc, slotId, screen);
        int height = cursorSpaceHeight(mc, slotId, screen);
        LocalCursorState state = cursorState(slotId);
        state.virtualCursorX = width / 2.0D;
        state.virtualCursorY = height / 2.0D;
        resetFocusCursorAnimation(slotId, state.virtualCursorX, state.virtualCursorY);
    }

    private static int cursorSpaceWidth(Minecraft mc, int slotId, Screen screen) {
        ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewportOrNull(slotId);
        if (viewport != null) {
            return ViewportGuiScale.logicalWidth(viewport);
        }
        return screen != null && screen.width > 1 ? screen.width : Math.max(1, mc.getWindow().getGuiScaledWidth());
    }

    private static int cursorSpaceHeight(Minecraft mc, int slotId, Screen screen) {
        ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewportOrNull(slotId);
        if (viewport != null) {
            return ViewportGuiScale.logicalHeight(viewport);
        }
        return screen != null && screen.height > 1 ? screen.height : Math.max(1, mc.getWindow().getGuiScaledHeight());
    }

    private static void resetMouseSourceObservation(Minecraft mc, LocalCursorState state) {
        state.lastObservedRawMouseX = mc.mouseHandler.xpos();
        state.lastObservedRawMouseY = mc.mouseHandler.ypos();
        state.hasObservedRawMousePosition = true;
    }

    private static void markCursorAsJoystickDriven(int slotId) {
        ClientRuntime.INSTANCE.slots().slot(slotId).inputState().markGamepadInput();
        LocalCursorState state = cursorState(slotId);
        state.cursorInputSource = CursorInputSource.JOYSTICK;
        resetFocusCursorAnimation(slotId, state.virtualCursorX, state.virtualCursorY);
    }

    private static void markCursorAsMouseDriven(int slotId) {
        InputApi.markKeyboardMouseInput();
        LocalCursorState state = cursorState(slotId);
        state.cursorInputSource = CursorInputSource.MOUSE;
        resetFocusCursorAnimation(slotId, state.virtualCursorX, state.virtualCursorY);
    }

    private static double resolveFocusAnimatedCursorX(int slotId) {
        LocalCursorState state = cursorState(slotId);
        if (!isAnimatedCursorSource(state.cursorInputSource) || !state.focusCursorAnimationActive) {
            return state.virtualCursorX;
        }

        return CursorAnimator.value(FOCUS_CURSOR_ANIMATION_X + "." + normalizedSlotId(slotId))
            .fromTo((float) state.focusCursorAnimationStartX, (float) state.focusCursorAnimationTargetX)
            .time(FOCUS_CURSOR_ANIMATION_MS)
            .ease(2.0F, 2.0F)
            .getFloat();
    }

    private static double resolveFocusAnimatedCursorY(int slotId) {
        LocalCursorState state = cursorState(slotId);
        if (!isAnimatedCursorSource(state.cursorInputSource) || !state.focusCursorAnimationActive) {
            return state.virtualCursorY;
        }

        return CursorAnimator.value(FOCUS_CURSOR_ANIMATION_Y + "." + normalizedSlotId(slotId))
            .fromTo((float) state.focusCursorAnimationStartY, (float) state.focusCursorAnimationTargetY)
            .time(FOCUS_CURSOR_ANIMATION_MS)
            .ease(2.0F, 2.0F)
            .getFloat();
    }

    private static void resetFocusCursorAnimation(int slotId, double x, double y) {
        LocalCursorState state = cursorState(slotId);
        state.focusCursorAnimationActive = false;
        state.focusCursorAnimationStartX = x;
        state.focusCursorAnimationStartY = y;
        state.focusCursorAnimationTargetX = x;
        state.focusCursorAnimationTargetY = y;
        int normalizedSlotId = normalizedSlotId(slotId);
        CursorAnimator.value(FOCUS_CURSOR_ANIMATION_X + "." + normalizedSlotId).fromTo((float) x, (float) x).time(0).getFloat();
        CursorAnimator.value(FOCUS_CURSOR_ANIMATION_Y + "." + normalizedSlotId).fromTo((float) y, (float) y).time(0).getFloat();
    }

    private static boolean isAnimatedCursorSource(CursorInputSource source) {
        return source == CursorInputSource.FOCUS_SLOT || source == CursorInputSource.MAGNET;
    }

    private static int currentCursorSlotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : InputApi.keyboardMouseSlotId();
    }

    private static LocalCursorState cursorState(int slotId) {
        return CURSOR_STATES[normalizedSlotId(slotId)];
    }

    private static LocalInputFrameClock cameraClock(int slotId) {
        return CAMERA_CLOCKS[normalizedSlotId(slotId)];
    }

    private static LocalInputFrameClock cursorClock(int slotId) {
        return CURSOR_CLOCKS[normalizedSlotId(slotId)];
    }

    private static void resetCameraTiming(int slotId) {
        cameraClock(slotId).reset();
    }

    private static int normalizedSlotId(int slotId) {
        return Math.max(0, Math.min(LocalClientSlotRegistry.MAX_SLOTS - 1, slotId));
    }

    private static boolean hasAnyGamepadConnected() {
        for (int slotId = 0; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            if (InputApi.hasGamepadForSlot(slotId)) {
                return true;
            }
        }
        return false;
    }

    record FocusedElementCenter(double guiX, double guiY) {
    }
}
