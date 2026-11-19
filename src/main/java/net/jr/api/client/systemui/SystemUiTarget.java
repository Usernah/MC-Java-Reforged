package net.jr.api.client.systemui;

import javax.annotation.Nullable;
import net.jr.api.client.input.IGamepad;

public record SystemUiTarget(
    SystemUiScope scope,
    int ownerSlot,
    @Nullable IGamepad activator
) {
    public SystemUiTarget {
        if (scope == null) {
            throw new IllegalArgumentException("System UI scope cannot be null");
        }
        if (ownerSlot < 0) {
            throw new IllegalArgumentException("System UI owner slot cannot be negative");
        }
    }

    public static SystemUiTarget fullWindow() {
        return fullWindow(0);
    }

    public static SystemUiTarget fullWindow(int ownerSlot) {
        return new SystemUiTarget(SystemUiScope.FULL_WINDOW, ownerSlot, null);
    }

    public static SystemUiTarget fullWindow(int ownerSlot, IGamepad activator) {
        return new SystemUiTarget(SystemUiScope.FULL_WINDOW, ownerSlot, activator);
    }

    public static SystemUiTarget viewport(int ownerSlot) {
        return new SystemUiTarget(SystemUiScope.VIEWPORT, ownerSlot, null);
    }
}
