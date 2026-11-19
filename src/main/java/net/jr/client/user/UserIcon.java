package net.jr.client.user;

import java.util.Objects;

public record UserIcon(String texture) {
    public UserIcon {
        Objects.requireNonNull(texture, "texture");
    }
}
