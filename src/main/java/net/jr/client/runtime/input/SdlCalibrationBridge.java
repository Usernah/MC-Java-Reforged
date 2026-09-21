package net.jr.client.runtime.input;

import javax.annotation.Nullable;
import net.jr.api.client.input.IGamepad;
import net.jr.client.input.gamepad.RawGamepadInputPress;
import net.jr.client.runtime.ClientRuntime;

/** Translates a backend calibration event into its device object. */
public final class SdlCalibrationBridge {
    private SdlCalibrationBridge() {
    }

    @Nullable
    public static IGamepad gamepad(RawGamepadInputPress press) {
        return press.deviceId() < 0L
            ? null
            : ClientRuntime.INSTANCE.input().deviceRegistry().sdlDevice(press.deviceId());
    }
}
