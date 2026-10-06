package net.jr.api.client.input;

import net.jr.client.runtime.ClientRuntime;

/** Entry point for mods that consume local input without SDL or slot scopes. */
public final class Inputs {
    private Inputs() {
    }

    public static IInputDeviceRegistry devices() {
        return ClientRuntime.INSTANCE.input().devices();
    }

    public static IInputAssignment assignments() {
        return ClientRuntime.INSTANCE.input().assignments();
    }
}
