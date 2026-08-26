package net.jr.client.ui.cursor;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class SystemCursorController {
    private static int appliedCursorMode = -1;

    private SystemCursorController() {
    }

    public static void setHidden(boolean hidden) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        if (minecraft.mouseHandler != null && minecraft.mouseHandler.isMouseGrabbed()) {
            appliedCursorMode = -1;
            return;
        }

        long window = minecraft.getWindow().handle();
        int desiredMode = hidden ? GLFW.GLFW_CURSOR_HIDDEN : GLFW.GLFW_CURSOR_NORMAL;
        int actualMode = GLFW.glfwGetInputMode(window, GLFW.GLFW_CURSOR);
        if (appliedCursorMode == desiredMode && actualMode == desiredMode) {
            return;
        }

        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, desiredMode);
        appliedCursorMode = desiredMode;
    }
}
