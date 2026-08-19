package net.jr.client.input.cursor;

import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class CursorHider {
    private static final boolean[] controllerHiddenBySlot = new boolean[LocalClientSlotRegistry.MAX_SLOTS];
    private static final boolean[] replacementHiddenBySlot = new boolean[LocalClientSlotRegistry.MAX_SLOTS];
    private static int appliedCursorMode = -1;

    public static void setHidden(boolean hidden) {
        setHiddenForSlot(currentSlotId(), hidden);
    }

    public static void setHiddenForSlot(int slotId, boolean hidden) {
        int normalizedSlotId = normalizeSlotId(slotId);
        if (controllerHiddenBySlot[normalizedSlotId] == hidden) {
            return;
        }
        controllerHiddenBySlot[normalizedSlotId] = hidden;
        applyCursorMode();
    }

    public static void clearHiddenForSlot(int slotId) {
        setHiddenForSlot(slotId, false);
    }

    public static void setReplacementHidden(boolean hidden) {
        setReplacementHiddenForSlot(currentSlotId(), hidden);
    }

    public static void setReplacementHiddenForSlot(int slotId, boolean hidden) {
        int normalizedSlotId = normalizeSlotId(slotId);
        if (replacementHiddenBySlot[normalizedSlotId] == hidden) {
            return;
        }
        replacementHiddenBySlot[normalizedSlotId] = hidden;
        applyCursorMode();
    }

    public static void clearReplacementHiddenForSlot(int slotId) {
        setReplacementHiddenForSlot(slotId, false);
    }

    public static boolean isHidden() {
        for (int slotId = 0; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            if (isHiddenForSlot(slotId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHiddenForSlot(int slotId) {
        int normalizedSlotId = normalizeSlotId(slotId);
        return controllerHiddenBySlot[normalizedSlotId] || replacementHiddenBySlot[normalizedSlotId];
    }

    public static boolean isControllerHidden() {
        for (boolean hidden : controllerHiddenBySlot) {
            if (hidden) {
                return true;
            }
        }
        return false;
    }

    public static boolean isControllerHiddenForSlot(int slotId) {
        return controllerHiddenBySlot[normalizeSlotId(slotId)];
    }

    public static void sync() {
        applyCursorMode();
    }

    private static void applyCursorMode() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        if (minecraft.mouseHandler != null && minecraft.mouseHandler.isMouseGrabbed()) {
            appliedCursorMode = -1;
            return;
        }

        long window = minecraft.getWindow().handle();
        int desiredMode = isHidden() ? GLFW.GLFW_CURSOR_HIDDEN : GLFW.GLFW_CURSOR_NORMAL;
        int actualMode = GLFW.glfwGetInputMode(window, GLFW.GLFW_CURSOR);

        if (appliedCursorMode == desiredMode && actualMode == desiredMode) {
            return;
        }

        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, desiredMode);
        appliedCursorMode = desiredMode;
    }

    private static int currentSlotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : 0;
    }

    private static int normalizeSlotId(int slotId) {
        return Math.max(0, Math.min(LocalClientSlotRegistry.MAX_SLOTS - 1, slotId));
    }
}
