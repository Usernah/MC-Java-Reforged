package net.jr.client.input.cursor;

import net.jr.client.input.runtime.GamepadInputProcessor;
import net.jr.client.runtime.context.SlotScope;

public final class ScreenMousePositionResolver {
    private ScreenMousePositionResolver() {
    }

    public static int x(int vanillaX) {
        return SlotScope.idOrNull() != null ? vanillaX : GamepadInputProcessor.resolveScreenMouseX(vanillaX);
    }

    public static int y(int vanillaY) {
        return SlotScope.idOrNull() != null ? vanillaY : GamepadInputProcessor.resolveScreenMouseY(vanillaY);
    }
}
