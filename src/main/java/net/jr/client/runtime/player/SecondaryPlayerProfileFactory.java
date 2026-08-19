package net.jr.client.runtime.player;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;

/** Keeps the network-safe identity of a split player separate from its visible name. */
public final class SecondaryPlayerProfileFactory {
    private static final int FIRST_SECONDARY_ORDINAL = 2;
    private static final int LAST_SECONDARY_ORDINAL = 4;

    private SecondaryPlayerProfileFactory() {
    }

    public static GameProfile create(GameProfile primaryProfile, int ordinal) {
        validateOrdinal(ordinal);
        String internalName = primaryProfile.name() + "_" + ordinal;
        UUID uuid = UUID.nameUUIDFromBytes(internalName.getBytes(StandardCharsets.UTF_8));
        return new GameProfile(uuid, internalName);
    }

    public static int ordinal(GameProfile profile) {
        String internalName = profile.name();
        int separator = internalName.lastIndexOf('_');
        if (separator <= 0 || separator == internalName.length() - 1) {
            return -1;
        }

        int ordinal;
        try {
            ordinal = Integer.parseInt(internalName.substring(separator + 1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
        if (ordinal < FIRST_SECONDARY_ORDINAL || ordinal > LAST_SECONDARY_ORDINAL) {
            return -1;
        }

        return ordinal;
    }

    @Nullable
    public static Component visibleName(GameProfile profile) {
        int ordinal = ordinal(profile);
        if (ordinal < 0) {
            return null;
        }
        String internalName = profile.name();
        String originalName = internalName.substring(0, internalName.lastIndexOf('_'));
        return Component.literal(originalName + " (" + ordinal + ")");
    }

    private static void validateOrdinal(int ordinal) {
        if (ordinal < FIRST_SECONDARY_ORDINAL || ordinal > LAST_SECONDARY_ORDINAL) {
            throw new IllegalArgumentException("Split player ordinal must be between 2 and 4: " + ordinal);
        }
    }
}
