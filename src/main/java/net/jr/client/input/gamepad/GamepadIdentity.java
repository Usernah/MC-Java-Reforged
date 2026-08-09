package net.jr.client.input.gamepad;

import java.util.Objects;

public record GamepadIdentity(
    String key,
    String displayName,
    int vendor,
    int product,
    int productVersion,
    String guid
) {
    public GamepadIdentity {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(guid, "guid");
    }
}

