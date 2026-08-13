package net.jr.client.input.runtime;

import net.jr.Java_reforged;
import net.jr.ClientRuntime.runtime.Client;
import net.jr.ClientRuntime.runtime.ClientBoundary;
import net.jr.ClientRuntime.runtime.ScreenScale;
import net.jr.ClientRuntime.viewport.ViewportArea;
import net.jr.client.input.gamepad.GamepadAxis;
import net.jr.client.input.InputApi;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionHandler;
import net.jr.mixin.controls.ClientInputAccessor;
import net.jr.mixin.uifocus.MouseHandlerAccessor;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.jr.client.input.cursor.CursorHider;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.client.input.cursor.MouseCoordinates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public class GamepadInputProcessor {

    private static final float MOUSE_SENSITIVITY_X = 24.0f;
    private static final float MOUSE_SENSITIVITY_Y = 24.0f;
    private static final float DEADZONE = 0.2f;
    private static final float CURSOR_SPEED = 15.0f;
    private static final float SNEAK_MOVEMENT_SCALE = 0.3f;
    private static final float CURSOR_INPUT_EPSILON = 0.0001f;
    private static final float CAMERA_INPUT_EPSILON = 0.0001f;
    private static final double RAW_MOUSE_MOVE_EPSILON = 0.25D;
    private static final double CAMERA_FRAME_SCALE_MAX = 2.0D;
    private static final double CAMERA_TARGET_FPS = 60.0D;
    private static final double CURSOR_FRAME_SCALE_MIN = 0.25D;
    private static final double CURSOR_FRAME_SCALE_MAX = 2.0D;
    private static final double CURSOR_TARGET_FPS = 60.0D;
    private static final int FOCUS_CURSOR_ANIMATION_MS = 50;
    private static final String FOCUS_CURSOR_ANIMATION_X = "java_reforged.focus_slot_cursor_x";
    private static final String FOCUS_CURSOR_ANIMATION_Y = "java_reforged.focus_slot_cursor_y";

    private static final FrameClock[] CAMERA_CLOCKS = new FrameClock[Client.MAX_CLIENTS];
    private static final FrameClock[] CURSOR_CLOCKS = new FrameClock[Client.MAX_CLIENTS];
    private static final CursorState[] CURSOR_STATES = new CursorState[Client.MAX_CLIENTS];

    static {
        for (int slotId = 0; slotId < Client.MAX_CLIENTS; slotId++) {
            CAMERA_CLOCKS[slotId] = new FrameClock(CAMERA_TARGET_FPS, 0.0D, CAMERA_FRAME_SCALE_MAX, 0.0D);
            CURSOR_CLOCKS[slotId] = new FrameClock(CURSOR_TARGET_FPS, CURSOR_FRAME_SCALE_MIN, CURSOR_FRAME_SCALE_MAX, 1.0D);
            CURSOR_STATES[slotId] = new CursorState();
        }
    }
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        for (int clientId = 0; clientId < Client.MAX_CLIENTS; clientId++) {
            if (event.getEntity() != Client.player(clientId) || !InputApi.hasGamepadForClient(clientId)) {
                continue;
            }
            ClientBoundary.runForClient(clientId, () -> applyMovementInput(event));
            return;
        }
    }

    private static void applyMovementInput(MovementInputUpdateEvent event) {
        if (Client.screen() != null) {
            return;
        }
        float leftStickX = InputApi.axis(GamepadAxis.LEFT_STICK_X, DEADZONE);
        float leftStickY = InputApi.axis(GamepadAxis.LEFT_STICK_Y, DEADZONE);
        if (Math.abs(leftStickX) <= 0 && Math.abs(leftStickY) <= 0) {
            return;
        }
        InputApi.markGamepadInput();
        float forwardImpulse = -leftStickY;
        float leftImpulse = -leftStickX;
        if (event.getInput().keyPresses.shift()) {
            forwardImpulse *= SNEAK_MOVEMENT_SCALE;
            leftImpulse *= SNEAK_MOVEMENT_SCALE;
        }
        ((ClientInputAccessor) event.getInput()).javaReforged$setMoveVector(
            new net.minecraft.world.phys.Vec2(leftImpulse, forwardImpulse)
        );
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        InputApi.updateGamepads();
        Minecraft minecraft = Minecraft.getInstance();
        if (!hasAnyGamepadConnected()) {
            for (int slotId = 0; slotId < Client.MAX_CLIENTS; slotId++) {
                CursorHider.setHiddenForSlot(slotId, false);
                CursorState state = cursorState(slotId);
                state.cursorInputSource = CursorInputSource.MOUSE;
                state.lastScreen = Client.screen(slotId);
                resetMouseSourceObservation(minecraft, state);
                resetCameraTiming(slotId);
                cursorClock(slotId).reset();
            }
            return;
        }

        for (int clientId = 0; clientId < Client.MAX_CLIENTS; clientId++) {
            if (!Client.connected(clientId) || !InputApi.hasGamepadForClient(clientId)) {
                CursorHider.setHiddenForSlot(clientId, false);
                resetCameraTiming(clientId);
                cursorClock(clientId).reset();
                continue;
            }
            ClientBoundary.runForClient(clientId, () -> {
                UiInputModeController.updateCurrentClientFrame(minecraft);
                processCurrentClientFrame(minecraft);
            });
        }
    }

    private static void processCurrentClientFrame(Minecraft minecraft) {
        int clientId = Client.slotId();
        CursorState state = cursorState(clientId);
        Screen screen = Client.screen();
        if (screen != null) {
            resetCameraTiming(clientId);
            CursorHider.setHiddenForSlot(clientId, true);
            handleVirtualCursorMovement(minecraft, screen, clientId);
            return;
        }
        CursorHider.setHiddenForSlot(clientId, false);
        if (state.cursorInputSource == CursorInputSource.FOCUS_SLOT) {
            state.cursorInputSource = InputApi.isGamepadMode()
                ? CursorInputSource.JOYSTICK
                : CursorInputSource.MOUSE;
        }
        resetFocusCursorAnimation(clientId, state.virtualCursorX, state.virtualCursorY);
        state.lastScreen = null;
        cursorClock(clientId).reset();
        LocalPlayer player = Client.player();
        if (!minecraft.isPaused() && player != null) {
            handleCameraMovement(player, clientId);
        } else {
            resetCameraTiming(clientId);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Device maintenance and joining are centralized in MappedActionProcessor.
    }

    private static void handleCameraMovement(LocalPlayer player, int clientId) {
        float rightStickX = InputApi.axis(GamepadAxis.RIGHT_STICK_X, DEADZONE);
        float rightStickY = InputApi.axis(GamepadAxis.RIGHT_STICK_Y, DEADZONE);

        rightStickX = applyResponseCurve(rightStickX);
        rightStickY = applyResponseCurve(rightStickY);

        if (Math.abs(rightStickX) <= CAMERA_INPUT_EPSILON && Math.abs(rightStickY) <= CAMERA_INPUT_EPSILON) {
            resetCameraTiming(clientId);
            return;
        }

        InputApi.markGamepadInput();
        double frameScale = cameraClock(clientId).sample();

        float deltaX = (float) (rightStickY * MOUSE_SENSITIVITY_Y * frameScale);
        float deltaY = (float) (rightStickX * MOUSE_SENSITIVITY_X * frameScale);

        player.turn(deltaY, deltaX);
    }

    private static void handleVirtualCursorMovement(Minecraft mc, Screen screen, int slotId) {
        CursorState state = cursorState(slotId);
        if (screen != state.lastScreen) {
            resetMouseSourceObservation(mc, state);
            state.lastScreen = screen;
            if (InputApi.isGamepadMode()
                || state.cursorInputSource == CursorInputSource.JOYSTICK
                || !InputApi.canPhysicalMouseDrive()) {
                centerVirtualCursor(mc, slotId);
                markCursorAsJoystickDriven(slotId);
            } else {
                activatePhysicalMouseCursor(mc, slotId);
            }
        }

        float stickX = InputApi.axis(GamepadAxis.LEFT_STICK_X, DEADZONE);
        float stickY = InputApi.axis(GamepadAxis.LEFT_STICK_Y, DEADZONE);

        stickX = applyResponseCurve(stickX);
        stickY = applyResponseCurve(stickY);

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
    }

    private static float applyResponseCurve(float input) {
        float sign = Math.signum(input);
        float abs = Math.abs(input);
        return sign * (abs * abs);
    }

    public static boolean isJoystickCursorActive() {
        return isJoystickCursorActive(currentCursorSlotId());
    }

    public static boolean isJoystickCursorActive(int slotId) {
        return cursorState(slotId).cursorInputSource == CursorInputSource.JOYSTICK;
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
        CursorState state = cursorState(slotId);
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

    public static void releaseFocusedSlotCursor(int slotId) {
        CursorState state = cursorState(slotId);
        if (state.cursorInputSource == CursorInputSource.FOCUS_SLOT) {
            state.cursorInputSource = CursorInputSource.MOUSE;
        }
        resetFocusCursorAnimation(slotId, state.virtualCursorX, state.virtualCursorY);
    }

    public static void activatePhysicalMouseCursor(Minecraft minecraft) {
        activatePhysicalMouseCursor(minecraft, currentCursorSlotId());
    }

    private static void activatePhysicalMouseCursor(Minecraft minecraft, int slotId) {
        if (minecraft == null || !InputApi.canPhysicalMouseDriveClient(slotId)) {
            return;
        }
        CursorState state = cursorState(slotId);
        markCursorAsMouseDriven(slotId);
        resetMouseSourceObservation(minecraft, state);
        if (Client.screen(slotId) != null) {
            syncVirtualCursorWithMouse(minecraft, slotId);
        }
    }

    public static void notifyPhysicalMouseMove(double rawMouseX, double rawMouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        int slotId = InputApi.keyboardMouseClientId();
        if (minecraft == null || !InputApi.canPhysicalMouseDriveClient(slotId)) {
            return;
        }

        CursorState state = cursorState(slotId);
        Screen screen = Client.screen(slotId);
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

    /** Places the one physical mouse at the center of the viewport that owns it. */
    public static void centerPhysicalCursorForScreen(int slotId) {
        Minecraft minecraft = Minecraft.getInstance();
        ViewportArea viewport = Client.viewportOrNull(slotId);
        if (minecraft == null || viewport == null || !InputApi.canPhysicalMouseDriveClient(slotId)) {
            return;
        }

        double windowX = viewport.windowX() + viewport.windowWidth() / 2.0D;
        double windowY = viewport.windowY() + viewport.windowHeight() / 2.0D;
        GLFW.glfwSetCursorPos(minecraft.getWindow().handle(), windowX, windowY);

        MouseHandlerAccessor mouse = (MouseHandlerAccessor)minecraft.mouseHandler;
        mouse.javareforged$setXpos(windowX);
        mouse.javareforged$setYpos(windowY);

        CursorState state = cursorState(slotId);
        state.lastObservedRawMouseX = windowX;
        state.lastObservedRawMouseY = windowY;
        state.hasObservedRawMousePosition = true;
        if (state.cursorInputSource == CursorInputSource.MOUSE) {
            syncVirtualCursorWithMouse(minecraft, slotId);
        }
    }

    static void dispatchFocusNavigation(Screen screen, int keyCode) {
        UiInputModeController.prepareForFocusNavigation(keyCode);
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
            InputActionSounds.playHover();
        }
        screen.afterKeyboardAction();
    }

    static boolean activateFocusedElement(Screen screen, int mouseButton) {
        if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen
            && ContainerSlotFocusController.isVanillaSlotFocusActive(containerScreen)) {
            return ContainerSlotFocusController.pickupFocusedOrHoveredSlot(containerScreen, mouseButton);
        }

        if (mouseButton == 0) {
            GuiEventListener focused = focusedLeaf(screen.getCurrentFocusPath());
            if (focused == null || focused == screen) {
                focused = screen.getFocused();
            }
            if (focused instanceof UiActionHandler handler) {
                return handler.handleUiAction(UiAction.ACCEPT);
            }
        }

        return false;
    }

    static FocusedElementCenter focusedElementCenter(Screen screen) {
        GuiEventListener focused = focusedLeaf(screen.getCurrentFocusPath());
        if (focused == null || focused == screen) {
            focused = screen.getFocused();
        }
        if (focused == null || focused == screen) {
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
        if (!InputApi.canPhysicalMouseDriveClient(slotId)) {
            return;
        }

        CursorState state = cursorState(slotId);
        if (Client.hasViewport(slotId)) {
            ViewportArea viewport = Client.viewport(slotId);
            state.virtualCursorX = (mc.mouseHandler.xpos() - viewport.windowX())
                * ScreenScale.logicalWidth(viewport) / viewport.windowWidth();
            state.virtualCursorY = (mc.mouseHandler.ypos() - viewport.windowY())
                * ScreenScale.logicalHeight(viewport) / viewport.windowHeight();
        } else {
            state.virtualCursorX = MouseCoordinates.rawMouseToGlobalGuiX(mc, mc.mouseHandler.xpos());
            state.virtualCursorY = MouseCoordinates.rawMouseToGlobalGuiY(mc, mc.mouseHandler.ypos());
        }
        resetFocusCursorAnimation(slotId, state.virtualCursorX, state.virtualCursorY);
    }

    private static void centerVirtualCursor(Minecraft mc, int slotId) {
        Screen screen = Client.screen(slotId);
        int width = cursorSpaceWidth(mc, slotId, screen);
        int height = cursorSpaceHeight(mc, slotId, screen);
        CursorState state = cursorState(slotId);
        state.virtualCursorX = width / 2.0D;
        state.virtualCursorY = height / 2.0D;
        resetFocusCursorAnimation(slotId, state.virtualCursorX, state.virtualCursorY);
    }

    private static int cursorSpaceWidth(Minecraft mc, int slotId, Screen screen) {
        ViewportArea viewport = Client.viewportOrNull(slotId);
        if (viewport != null) {
            return ScreenScale.logicalWidth(viewport);
        }
        return screen != null && screen.width > 1 ? screen.width : Math.max(1, mc.getWindow().getGuiScaledWidth());
    }

    private static int cursorSpaceHeight(Minecraft mc, int slotId, Screen screen) {
        ViewportArea viewport = Client.viewportOrNull(slotId);
        if (viewport != null) {
            return ScreenScale.logicalHeight(viewport);
        }
        return screen != null && screen.height > 1 ? screen.height : Math.max(1, mc.getWindow().getGuiScaledHeight());
    }

    private static void resetMouseSourceObservation(Minecraft mc, CursorState state) {
        state.lastObservedRawMouseX = mc.mouseHandler.xpos();
        state.lastObservedRawMouseY = mc.mouseHandler.ypos();
        state.hasObservedRawMousePosition = true;
    }

    private static void markCursorAsJoystickDriven(int slotId) {
        Client.input(slotId).markGamepadInput();
        CursorState state = cursorState(slotId);
        state.cursorInputSource = CursorInputSource.JOYSTICK;
        resetFocusCursorAnimation(slotId, state.virtualCursorX, state.virtualCursorY);
    }

    private static void markCursorAsMouseDriven(int slotId) {
        InputApi.markKeyboardMouseInput();
        CursorState state = cursorState(slotId);
        state.cursorInputSource = CursorInputSource.MOUSE;
        resetFocusCursorAnimation(slotId, state.virtualCursorX, state.virtualCursorY);
    }

    private static double resolveFocusAnimatedCursorX(int slotId) {
        CursorState state = cursorState(slotId);
        if (state.cursorInputSource != CursorInputSource.FOCUS_SLOT || !state.focusCursorAnimationActive) {
            return state.virtualCursorX;
        }

        return InputAnimator.value(FOCUS_CURSOR_ANIMATION_X + "." + normalizedSlotId(slotId))
            .fromTo((float) state.focusCursorAnimationStartX, (float) state.focusCursorAnimationTargetX)
            .time(FOCUS_CURSOR_ANIMATION_MS)
            .ease(2.0F, 2.0F)
            .getFloat();
    }

    private static double resolveFocusAnimatedCursorY(int slotId) {
        CursorState state = cursorState(slotId);
        if (state.cursorInputSource != CursorInputSource.FOCUS_SLOT || !state.focusCursorAnimationActive) {
            return state.virtualCursorY;
        }

        return InputAnimator.value(FOCUS_CURSOR_ANIMATION_Y + "." + normalizedSlotId(slotId))
            .fromTo((float) state.focusCursorAnimationStartY, (float) state.focusCursorAnimationTargetY)
            .time(FOCUS_CURSOR_ANIMATION_MS)
            .ease(2.0F, 2.0F)
            .getFloat();
    }

    private static void resetFocusCursorAnimation(int slotId, double x, double y) {
        CursorState state = cursorState(slotId);
        state.focusCursorAnimationActive = false;
        state.focusCursorAnimationStartX = x;
        state.focusCursorAnimationStartY = y;
        state.focusCursorAnimationTargetX = x;
        state.focusCursorAnimationTargetY = y;
        int normalizedSlotId = normalizedSlotId(slotId);
        InputAnimator.value(FOCUS_CURSOR_ANIMATION_X + "." + normalizedSlotId).fromTo((float) x, (float) x).time(0).getFloat();
        InputAnimator.value(FOCUS_CURSOR_ANIMATION_Y + "." + normalizedSlotId).fromTo((float) y, (float) y).time(0).getFloat();
    }

    private static int currentCursorSlotId() {
        return Client.currentOrNull() == null ? 0 : Client.slotId();
    }

    private static CursorState cursorState(int slotId) {
        return CURSOR_STATES[normalizedSlotId(slotId)];
    }

    private static FrameClock cameraClock(int slotId) {
        return CAMERA_CLOCKS[normalizedSlotId(slotId)];
    }

    private static FrameClock cursorClock(int slotId) {
        return CURSOR_CLOCKS[normalizedSlotId(slotId)];
    }

    private static void resetCameraTiming(int slotId) {
        cameraClock(slotId).reset();
    }

    private static int normalizedSlotId(int slotId) {
        return Math.max(0, Math.min(Client.MAX_CLIENTS - 1, slotId));
    }

    private static boolean hasAnyGamepadConnected() {
        for (int slotId = 0; slotId < Client.MAX_CLIENTS; slotId++) {
            if (InputApi.hasGamepadForClient(slotId)) {
                return true;
            }
        }
        return false;
    }

    private static final class CursorState {
        private double virtualCursorX = 0.0D;
        private double virtualCursorY = 0.0D;
        private Screen lastScreen = null;
        private CursorInputSource cursorInputSource = CursorInputSource.MOUSE;
        private boolean hasObservedRawMousePosition = false;
        private double lastObservedRawMouseX = 0.0D;
        private double lastObservedRawMouseY = 0.0D;
        private boolean focusCursorAnimationActive = false;
        private double focusCursorAnimationStartX = 0.0D;
        private double focusCursorAnimationStartY = 0.0D;
        private double focusCursorAnimationTargetX = 0.0D;
        private double focusCursorAnimationTargetY = 0.0D;
    }

    private enum CursorInputSource {
        MOUSE,
        JOYSTICK,
        FOCUS_SLOT
    }

    record FocusedElementCenter(double guiX, double guiY) {
    }

}

