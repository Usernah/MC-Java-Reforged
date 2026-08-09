package net.jr.client.input.gamepad;

import java.util.Objects;

public record RawGamepadInputPress(long deviceId, GamepadIdentity identity, RawGamepadInput input, long timestampMs) {
    public RawGamepadInputPress(GamepadIdentity identity, RawGamepadInput input, long timestampMs) {
        this(-1L, identity, input, timestampMs);
    }

    public RawGamepadInputPress {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(input, "input");
    }
}

