package net.jr.api.client.split;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;

public final class SplitScreen {
    private static volatile boolean requireMultipleControllersForJoin = true;

    private SplitScreen() {
    }

    public static boolean isActive() {
        return Client.count() >= 2;
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
