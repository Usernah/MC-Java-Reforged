package net.jr.api.client.split;

import net.jr.api.client.local.LocalClients;
import net.jr.client.runtime.ClientRuntime;

public final class SplitScreen {
    private static volatile boolean requireMultipleControllersForJoin = true;

    private SplitScreen() {
    }

    public static boolean isActive() {
        return LocalClients.count() >= 2;
    }

    public static boolean requiresMultipleControllersForJoin() {
        return requireMultipleControllersForJoin;
    }

    public static void setRequireMultipleControllersForJoin(boolean required) {
        requireMultipleControllersForJoin = required;
    }

    public static int requiredControllerCountForJoin() {
        return requireMultipleControllersForJoin
                ? LocalClients.count() + 1
                : 1;
    }

    public static boolean hasRequiredControllerCountForJoin() {
        return ClientRuntime.INSTANCE.input().devices().gamepads().size()
            >= requiredControllerCountForJoin();
    }
}
