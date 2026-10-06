package net.jr.client.ui.cursor;

import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.runtime.context.SlotScope;

public final class ScreenMousePositionResolver {
    private ScreenMousePositionResolver() {
    }

    public static int x(int vanillaX) {
        return SlotScope.idOrNull() != null ? vanillaX : GamepadInputHandler.resolveScreenMouseX(vanillaX);
    }

    public static int y(int vanillaY) {
        return SlotScope.idOrNull() != null ? vanillaY : GamepadInputHandler.resolveScreenMouseY(vanillaY);
    }
}
