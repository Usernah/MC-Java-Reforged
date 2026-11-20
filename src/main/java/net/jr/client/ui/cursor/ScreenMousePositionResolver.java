package net.jr.client.ui.cursor;

import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.runtime.context.SlotExecution;

public final class ScreenMousePositionResolver {
    private ScreenMousePositionResolver() {
    }

    public static int x(int vanillaX) {
        return SlotExecution.currentSlotIdOrNull() != null ? vanillaX : GamepadInputHandler.resolveScreenMouseX(vanillaX);
    }

    public static int y(int vanillaY) {
        return SlotExecution.currentSlotIdOrNull() != null ? vanillaY : GamepadInputHandler.resolveScreenMouseY(vanillaY);
    }
}
