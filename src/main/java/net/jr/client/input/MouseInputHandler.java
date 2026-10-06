package net.jr.client.input;

import net.jr.client.input.simulation.InputSimulation;
import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.runtime.input.InputActionHandler;
import net.jr.client.ui.navigation.UiInputModeController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;

public final class MouseInputHandler {
    private MouseInputHandler() {
    }

    public static boolean handleMove(long windowPointer, double x, double y) {
        if (InputSimulation.handleMouseMove(windowPointer, x, y)) {
            InputApi.markGamepadInput();
            return true;
        }
        GamepadInputHandler.notifyPhysicalMouseMove(x, y);
        return false;
    }

    public static boolean handleButton(
            Minecraft minecraft,
            long windowPointer,
            MouseButtonInfo buttonInfo,
            int action
    ) {
        if (windowPointer != minecraft.getWindow().handle()) {
            return false;
        }

        int button = buttonInfo.button();
        if (InputSimulation.handleMouseButton(windowPointer, button, action, buttonInfo.modifiers())) {
            if (InputSimulation.isActive()) {
                InputApi.markGamepadInput();
            }
            return true;
        }
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE) {
            InputApi.markKeyboardMouseInput();
            UiInputModeController.notifyPhysicalPointerActivity();
        }

        Screen screen = minecraft.gui.screen();
        if (InputActionHandler.suppressConsumedScreenMouseButton(screen, button, action)) {
            if (screen != null) {
                screen.afterMouseAction();
            }
            return true;
        }
        if (minecraft.gui.overlay() != null || screen == null) {
            return false;
        }

        boolean handled = action == GLFW.GLFW_PRESS
                ? InputActionHandler.handleScreenMouseButton(screen, button)
                : action == GLFW.GLFW_RELEASE
                        && InputActionHandler.consumeHandledMouseRelease(screen, button);
        if (!handled) {
            return false;
        }
        if (action == GLFW.GLFW_PRESS) {
            InputActionHandler.rememberSuppressedScreenMouseButton(button);
        }
        screen.afterMouseAction();
        return true;
    }

    public static boolean handleScroll(
            Minecraft minecraft,
            long windowPointer,
            double xOffset,
            double yOffset
    ) {
        if (InputSimulation.handleMouseScroll(windowPointer, xOffset, yOffset)) {
            InputApi.markGamepadInput();
            return true;
        }
        if (yOffset != 0.0D || xOffset != 0.0D) {
            InputApi.markKeyboardMouseInput();
            UiInputModeController.notifyPhysicalPointerActivity();
        }
        if (windowPointer != minecraft.getWindow().handle()
                || minecraft.gui.overlay() != null
                || minecraft.gui.screen() == null) {
            return false;
        }

        Screen screen = minecraft.gui.screen();
        if (!InputActionHandler.handleScreenMouseScroll(screen, yOffset)) {
            return false;
        }
        screen.afterMouseAction();
        return true;
    }
}
