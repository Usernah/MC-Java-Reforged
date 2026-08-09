package net.jr.client.input.cursor;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class CursorHider {

    private static boolean controllerHidden;
    private static boolean replacementHidden;
    private static int appliedCursorMode = -1;

    public static void setHidden(boolean hidden) {
        if (controllerHidden != hidden) {
            controllerHidden = hidden;
            applyCursorMode();
        }
    }

    public static void setReplacementHidden(boolean hidden) {
        if (replacementHidden != hidden) {
            replacementHidden = hidden;
            applyCursorMode();
        }
    }

    public static boolean isHidden() {
        return controllerHidden || replacementHidden;
    }

    public static boolean isControllerHidden() {
        return controllerHidden;
    }

    public static void sync() {
        applyCursorMode();
    }

    private static void applyCursorMode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }

        if (mc.mouseHandler != null && mc.mouseHandler.isMouseGrabbed()) {
            appliedCursorMode = -1;
            return;
        }

        long window = mc.getWindow().handle();
        int desiredMode = isHidden() ? GLFW.GLFW_CURSOR_HIDDEN : GLFW.GLFW_CURSOR_NORMAL;
        int actualMode = GLFW.glfwGetInputMode(window, GLFW.GLFW_CURSOR);

        if (appliedCursorMode == desiredMode && actualMode == desiredMode) {
            return;
        }

        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, desiredMode);
        appliedCursorMode = desiredMode;
    }

}

