package net.jr.api.client.ui;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.runtime.slot.ClientSlots;
import net.minecraft.client.Minecraft;

public final class Cursor {
    private Cursor() {
    }

    public static void moveTo(double x, double y) {
        GamepadInputHandler.moveCursorTo(x, y);
    }

    public static void center() {
        GamepadInputHandler.centerCursor();
    }

    public static double x() {
        return GamepadInputHandler.visualCursorX();
    }

    public static double y() {
        return GamepadInputHandler.visualCursorY();
    }

    public static double localWindowX() {
        Minecraft minecraft = Minecraft.getInstance();
        var client = Client.currentOrNull();
        return client != null
                ? client.input().screen().localWindowX(minecraft.mouseHandler)
                : minecraft.mouseHandler.xpos();
    }

    public static double localWindowY() {
        Minecraft minecraft = Minecraft.getInstance();
        var client = Client.currentOrNull();
        return client != null
                ? client.input().screen().localWindowY(minecraft.mouseHandler)
                : minecraft.mouseHandler.ypos();
    }

    public static void moveTo(int slotId, double x, double y) {
        requireValidSlot(slotId);
        requireFiniteCoordinate(x, "x");
        requireFiniteCoordinate(y, "y");
        GamepadInputHandler.moveCursorTo(slotId, x, y);
    }

    public static void center(int slotId) {
        requireValidSlot(slotId);
        GamepadInputHandler.centerCursor(slotId);
    }

    public static double x(int slotId) {
        requireValidSlot(slotId);
        return GamepadInputHandler.visualCursorX(slotId);
    }

    public static double y(int slotId) {
        requireValidSlot(slotId);
        return GamepadInputHandler.visualCursorY(slotId);
    }

    private static void requireValidSlot(int slotId) {
        ClientSlots.requireSlotId(slotId);
    }

    private static void requireFiniteCoordinate(double coordinate, String name) {
        if (!Double.isFinite(coordinate)) {
            throw new IllegalArgumentException(name + " must be a finite GUI coordinate");
        }
    }
}
