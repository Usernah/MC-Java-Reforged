package net.jr.api.client.split;

import net.jr.client.runtime.context.LocalClientAcces;

/** Public, world-independent view of the local split-screen runtime. */
public final class SplitScreen {
    private SplitScreen() {
    }

    public static boolean isActive() {
        return localPlayerCount() >= 2;
    }

    public static int localPlayerCount() {
        try {
            return LocalClientAcces.connectedCount();
        } catch (RuntimeException ignored) {
            return 1;
        }
    }
}
