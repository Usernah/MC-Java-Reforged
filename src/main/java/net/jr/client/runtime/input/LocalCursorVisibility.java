package net.jr.client.runtime.input;

import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.client.ui.cursor.SystemCursorController;

public final class LocalCursorVisibility {
    private static final boolean[] controllerHiddenBySlot = new boolean[LocalClientSlotRegistry.MAX_SLOTS];
    private static final boolean[] replacementHiddenBySlot = new boolean[LocalClientSlotRegistry.MAX_SLOTS];

    private LocalCursorVisibility() {
    }

    public static void setHidden(boolean hidden) {
        setHiddenForSlot(currentSlotId(), hidden);
    }

    public static void setHiddenForSlot(int slotId, boolean hidden) {
        int normalizedSlotId = normalizeSlotId(slotId);
        if (controllerHiddenBySlot[normalizedSlotId] == hidden) {
            return;
        }
        controllerHiddenBySlot[normalizedSlotId] = hidden;
        sync();
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
        sync();
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
        SystemCursorController.setHidden(isHidden());
    }

    private static int currentSlotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : 0;
    }

    private static int normalizeSlotId(int slotId) {
        return Math.max(0, Math.min(LocalClientSlotRegistry.MAX_SLOTS - 1, slotId));
    }
}
