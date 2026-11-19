package net.jr.api.client.split;

import net.jr.api.client.local.Client;
import net.jr.api.client.ui.Viewport;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.viewport.ViewportLayout;

public final class SplitScreen {
    private static volatile boolean requireMultipleControllersForJoin = true;

    private SplitScreen() {
    }

    public static boolean isActive() {
        int viewportCount = ClientRuntime.INSTANCE.viewports().usesEditorLayout()
            ? 2
            : Viewport.count();
        return Client.count() >= 2 && viewportCount >= 2;
    }

    public static ViewportLayout layout() {
        return ClientRuntime.INSTANCE.viewports().layout();
    }

    public static boolean requiresMultipleControllersForJoin() {
        return requireMultipleControllersForJoin;
    }

    public static void setRequireMultipleControllersForJoin(boolean required) {
        requireMultipleControllersForJoin = required;
    }

    public static int requiredControllerCountForJoin() {
        return requireMultipleControllersForJoin
                ? Client.count() + 1
                : 1;
    }

    public static boolean hasRequiredControllerCountForJoin() {
        return ClientRuntime.INSTANCE.input().devices().gamepads().size()
            >= requiredControllerCountForJoin();
    }
}
