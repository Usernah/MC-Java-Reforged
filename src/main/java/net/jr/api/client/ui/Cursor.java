package net.jr.api.client.ui;

import net.jr.client.runtime.input.LocalGamepadInputHandler;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;

public final class Cursor {
    private Cursor() {
    }

    public static void moveTo(int slotId, double x, double y) {
        requireValidSlot(slotId);
        requireFiniteCoordinate(x, "x");
        requireFiniteCoordinate(y, "y");
        LocalGamepadInputHandler.moveCursorTo(slotId, x, y);
    }

    public static void center(int slotId) {
        requireValidSlot(slotId);
        LocalGamepadInputHandler.centerCursor(slotId);
    }

    public static double x(int slotId) {
        requireValidSlot(slotId);
        return LocalGamepadInputHandler.visualCursorX(slotId);
    }

    public static double y(int slotId) {
        requireValidSlot(slotId);
        return LocalGamepadInputHandler.visualCursorY(slotId);
    }

    private static void requireValidSlot(int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
    }

    private static void requireFiniteCoordinate(double coordinate, String name) {
        if (!Double.isFinite(coordinate)) {
            throw new IllegalArgumentException(name + " must be a finite GUI coordinate");
        }
    }
}
