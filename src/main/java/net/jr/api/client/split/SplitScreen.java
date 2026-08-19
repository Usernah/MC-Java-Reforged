package net.jr.api.client.split;

import net.jr.client.runtime.ClientRuntime;

public final class SplitScreen {
    private SplitScreen() {
    }

    public static boolean isActive() {
        return ClientRuntime.INSTANCE.viewports().presentedCount() >= 2;
    }
}
