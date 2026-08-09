package net.jr.client.input.runtime;

import net.jr.Java_reforged;
import net.jr.client.input.gamepad.GamepadAxis;
import net.jr.client.input.InputApi;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionHandler;
import net.jr.mixin.controls.ClientInputAccessor;
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

    private static final FrameClock CAMERA_CLOCK = new FrameClock(CAMERA_TARGET_FPS, 0.0D, CAMERA_FRAME_SCALE_MAX, 0.0D);
    private static final FrameClock CURSOR_CLOCK = new FrameClock(CURSOR_TARGET_FPS, CURSOR_FRAME_SCALE_MIN, CURSOR_FRAME_SCALE_MAX, 1.0D);
    private static final CursorState CURSOR_STATE = new CursorState();
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (event.getEntity() != Minecraft.getInstance().player || !InputApi.isGamepadConnected()) {
            return;
        }
        applyMovementInput(event);
    }

    private static void applyMovementInput(MovementInputUpdateEvent event) {
        if (Minecraft.getInstance().gui.screen() != null) {
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
        UiInputModeController.updateCurrentClientFrame(minecraft);
        if (!InputApi.isGamepadConnected()) {
            CursorHider.setHidden(false);
            CURSOR_STATE.cursorInputSource = CursorInputSource.MOUSE;
            CURSOR_STATE.lastScreen = minecraft.gui.screen();
            resetMouseSourceObservation(minecraft, CURSOR_STATE);
            resetCameraTiming();
            CURSOR_CLOCK.reset();
            return;
        }

        processCurrentClientFrame(minecraft);
    }

    private static void processCurrentClientFrame(Minecraft minecraft) {
        Screen screen = minecraft.gui.screen();
        if (screen != null) {
            resetCameraTiming();
            CursorHider.setHidden(true);
            handleVirtualCursorMovement(minecraft, screen);
            return;
        }
        CursorHider.setHidden(false);
        if (CURSOR_STATE.cursorInputSource == CursorInputSource.FOCUS_SLOT) {
            CURSOR_STATE.cursorInputSource = InputApi.isGamepadMode()
                ? CursorInputSource.JOYSTICK
                : CursorInputSource.MOUSE;
        }
        resetFocusCursorAnimation(CURSOR_STATE.virtualCursorX, CURSOR_STATE.virtualCursorY);
        CURSOR_STATE.lastScreen = null;
        CURSOR_CLOCK.reset();
        LocalPlayer player = minecraft.player;
        if (!minecraft.isPaused() && player != null) {
            handleCameraMovement(player);
        } else {
            resetCameraTiming();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Device maintenance and joining are centralized in MappedActionProcessor.
    }

    private static void handleCameraMovement(LocalPlayer player) {
        float rightStickX = InputApi.axis(GamepadAxis.RIGHT_STICK_X, DEADZONE);
        float rightStickY = InputApi.axis(GamepadAxis.RIGHT_STICK_Y, DEADZONE);

        rightStickX = applyResponseCurve(rightStickX);
        rightStickY = applyResponseCurve(rightStickY);

        if (Math.abs(rightStickX) <= CAMERA_INPUT_EPSILON && Math.abs(rightStickY) <= CAMERA_INPUT_EPSILON) {
            resetCameraTiming();
            return;
        }

        InputApi.markGamepadInput();
        double frameScale = CAMERA_CLOCK.sample();

        float deltaX = (float) (rightStickY * MOUSE_SENSITIVITY_Y * frameScale);
        float deltaY = (float) (rightStickX * MOUSE_SENSITIVITY_X * frameScale);

        player.turn(deltaY, deltaX);
    }

    private static void handleVirtualCursorMovement(Minecraft mc, Screen screen) {
        CursorState state = CURSOR_STATE;
        if (screen != state.lastScreen) {
            resetMouseSourceObservation(mc, state);
            state.lastScreen = screen;
            if (InputApi.isGamepadMode()
                || state.cursorInputSource == CursorInputSource.JOYSTICK
                || !InputApi.canPhysicalMouseDrive()) {
                centerVirtualCursor(mc);
                markCursorAsJoystickDriven();
            } else {
                activatePhysicalMouseCursor(mc);
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
            syncVirtualCursorWithMouse(mc);
        }

        if (joystickMovedCursor) {
            if (!isJoystickCursorActive()) {
                centerVirtualCursor(mc);
            }
            double frameScale = CURSOR_CLOCK.sample();
            state.virtualCursorX += stickX * CURSOR_SPEED * frameScale;
            state.virtualCursorY += stickY * CURSOR_SPEED * frameScale;
            markCursorAsJoystickDriven();
            UiInputModeController.notifyJoystickPointerActivity();
        } else {
            CURSOR_CLOCK.sample();
        }

        int width = Math.max(1, screen.width);
        int height = Math.max(1, screen.height);
        state.virtualCursorX = Math.max(0, Math.min(width, state.virtualCursorX));
        state.virtualCursorY = Math.max(0, Math.min(height, state.virtualCursorY));
    }

    private static float applyResponseCurve(float input) {
        float sign = Math.signum(input);
        float abs = Math.abs(input);
        return sign * (abs * abs);
    }

    public static boolean isJoystickCursorActive() {
        return CURSOR_STATE.cursorInputSource == CursorInputSource.JOYSTICK;
    }

    public static boolean isVirtualCursorActive() {
        return CURSOR_STATE.cursorInputSource != CursorInputSource.MOUSE;
    }

    public static boolean isControllerCursorActive() {
        return isVirtualCursorActive();
    }

    public static boolean isPhysicalMouseCursorActive() {
        return CURSOR_STATE.cursorInputSource == CursorInputSource.MOUSE;
    }

    public static boolean isFocusSlotCursorActive() {
        return CURSOR_STATE.cursorInputSource == CursorInputSource.FOCUS_SLOT;
    }

    public static int resolveScreenMouseX(int vanillaMouseX) {
        if (UiInputModeController.shouldSuppressPointerHover()) {
            return Integer.MIN_VALUE;
        }

        return isVirtualCursorActive() ? (int) Math.round(CURSOR_STATE.virtualCursorX) : vanillaMouseX;
    }

    public static int resolveScreenMouseY(int vanillaMouseY) {
        if (UiInputModeController.shouldSuppressPointerHover()) {
            return Integer.MIN_VALUE;
        }

        return isVirtualCursorActive() ? (int) Math.round(CURSOR_STATE.virtualCursorY) : vanillaMouseY;
    }

    public static double visualCursorX() {
        return resolveFocusAnimatedCursorX();
    }

    public static double visualCursorY() {
        return resolveFocusAnimatedCursorY();
    }

    public static double cursorX() {
        return CURSOR_STATE.virtualCursorX;
    }

    public static double cursorY() {
        return CURSOR_STATE.virtualCursorY;
    }

    public static void moveVirtualCursorToFocusedSlot(double guiX, double guiY) {
        CursorState state = CURSOR_STATE;
        double visualX = visualCursorX();
        double visualY = visualCursorY();
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
        CursorState state = CURSOR_STATE;
        if (state.cursorInputSource == CursorInputSource.FOCUS_SLOT) {
            state.cursorInputSource = CursorInputSource.MOUSE;
        }
        resetFocusCursorAnimation(state.virtualCursorX, state.virtualCursorY);
    }

    public static void activatePhysicalMouseCursor(Minecraft minecraft) {
        if (minecraft == null || !InputApi.canPhysicalMouseDrive()) {
            return;
        }
        markCursorAsMouseDriven();
        resetMouseSourceObservation(minecraft, CURSOR_STATE);
        if (minecraft.gui.screen() != null) {
            syncVirtualCursorWithMouse(minecraft);
        }
    }

    public static void notifyPhysicalMouseMove(double rawMouseX, double rawMouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || !InputApi.canPhysicalMouseDrive()) {
            return;
        }

        CursorState state = CURSOR_STATE;
        Screen screen = minecraft.gui.screen();
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
        activatePhysicalMouseCursor(minecraft);
        UiInputModeController.notifyPhysicalPointerActivity();
    }

    public static void centerControllerCursorForScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        centerVirtualCursor(minecraft);
        markCursorAsJoystickDriven();
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

    private static void syncVirtualCursorWithMouse(Minecraft mc) {
        if (!InputApi.canPhysicalMouseDrive()) {
            return;
        }

        CursorState state = CURSOR_STATE;
        state.virtualCursorX = MouseCoordinates.rawMouseToGlobalGuiX(mc, mc.mouseHandler.xpos());
        state.virtualCursorY = MouseCoordinates.rawMouseToGlobalGuiY(mc, mc.mouseHandler.ypos());
        resetFocusCursorAnimation(state.virtualCursorX, state.virtualCursorY);
    }

    private static void centerVirtualCursor(Minecraft mc) {
        Screen screen = mc.gui.screen();
        int width = screen != null && screen.width > 1
            ? screen.width
            : mc.getWindow().getGuiScaledWidth();
        int height = screen != null && screen.height > 1
            ? screen.height
            : mc.getWindow().getGuiScaledHeight();
        CursorState state = CURSOR_STATE;
        state.virtualCursorX = width / 2.0D;
        state.virtualCursorY = height / 2.0D;
        resetFocusCursorAnimation(state.virtualCursorX, state.virtualCursorY);
    }

    private static void resetMouseSourceObservation(Minecraft mc, CursorState state) {
        state.lastObservedRawMouseX = mc.mouseHandler.xpos();
        state.lastObservedRawMouseY = mc.mouseHandler.ypos();
        state.hasObservedRawMousePosition = true;
    }

    private static void markCursorAsJoystickDriven() {
        InputApi.markGamepadInput();
        CursorState state = CURSOR_STATE;
        state.cursorInputSource = CursorInputSource.JOYSTICK;
        resetFocusCursorAnimation(state.virtualCursorX, state.virtualCursorY);
    }

    private static void markCursorAsMouseDriven() {
        InputApi.markKeyboardMouseInput();
        CursorState state = CURSOR_STATE;
        state.cursorInputSource = CursorInputSource.MOUSE;
        resetFocusCursorAnimation(state.virtualCursorX, state.virtualCursorY);
    }

    private static double resolveFocusAnimatedCursorX() {
        CursorState state = CURSOR_STATE;
        if (state.cursorInputSource != CursorInputSource.FOCUS_SLOT || !state.focusCursorAnimationActive) {
            return state.virtualCursorX;
        }

        return InputAnimator.value(FOCUS_CURSOR_ANIMATION_X)
            .fromTo((float) state.focusCursorAnimationStartX, (float) state.focusCursorAnimationTargetX)
            .time(FOCUS_CURSOR_ANIMATION_MS)
            .ease(2.0F, 2.0F)
            .getFloat();
    }

    private static double resolveFocusAnimatedCursorY() {
        CursorState state = CURSOR_STATE;
        if (state.cursorInputSource != CursorInputSource.FOCUS_SLOT || !state.focusCursorAnimationActive) {
            return state.virtualCursorY;
        }

        return InputAnimator.value(FOCUS_CURSOR_ANIMATION_Y)
            .fromTo((float) state.focusCursorAnimationStartY, (float) state.focusCursorAnimationTargetY)
            .time(FOCUS_CURSOR_ANIMATION_MS)
            .ease(2.0F, 2.0F)
            .getFloat();
    }

    private static void resetFocusCursorAnimation(double x, double y) {
        CursorState state = CURSOR_STATE;
        state.focusCursorAnimationActive = false;
        state.focusCursorAnimationStartX = x;
        state.focusCursorAnimationStartY = y;
        state.focusCursorAnimationTargetX = x;
        state.focusCursorAnimationTargetY = y;
        InputAnimator.value(FOCUS_CURSOR_ANIMATION_X).fromTo((float) x, (float) x).time(0).getFloat();
        InputAnimator.value(FOCUS_CURSOR_ANIMATION_Y).fromTo((float) y, (float) y).time(0).getFloat();
    }

    private static void resetCameraTiming() {
        CAMERA_CLOCK.reset();
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

