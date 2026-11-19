package net.jr.api.client.input;

import net.jr.client.runtime.ClientRuntime;


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
