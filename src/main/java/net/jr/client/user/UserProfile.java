package net.jr.client.user;

import com.mojang.authlib.GameProfile;
import java.util.Objects;
import java.util.UUID;

public record UserProfile(
    UUID id,
    String displayName,
    UserIcon icon,
    UserType type,
    UserOptions options,
    long createdAt,
    long lastUsedAt
) {
    public UserProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(options, "options");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("User display name cannot be blank");
        }
    }

    public GameProfile gameProfile() {
        return new GameProfile(this.id, this.displayName);
    }

    public UserProfile usedAt(long timestamp) {
        return new UserProfile(
            this.id,
            this.displayName,
            this.icon,
            this.type,
            this.options,
            this.createdAt,
            timestamp
        );
    }
}
