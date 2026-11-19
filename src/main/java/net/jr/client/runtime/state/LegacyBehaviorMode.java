package net.jr.client.runtime.state;

import net.minecraft.network.chat.Component;

public enum LegacyBehaviorMode {
    CLASSIC("classic"),
    LEGACY("legacy");

    private final String serializedName;

    LegacyBehaviorMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return this.serializedName;
    }

    public Component caption() {
        return Component.translatable("options.java_reforged.behavior_mode." + this.serializedName);
    }

    public static LegacyBehaviorMode byName(String name) {
        for (LegacyBehaviorMode mode : values()) {
            if (mode.serializedName.equals(name)) {
                return mode;
            }
        }
        return LEGACY;
    }
}
