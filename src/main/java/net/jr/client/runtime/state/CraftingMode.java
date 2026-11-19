package net.jr.client.runtime.state;

import net.minecraft.network.chat.Component;

public enum CraftingMode {
    CLASSIC("classic"),
    LEGACY("legacy");

    private final String serializedName;

    CraftingMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return this.serializedName;
    }

    public Component caption() {
        return Component.translatable("options.java_reforged.crafting_mode." + this.serializedName);
    }

    public static CraftingMode byName(String name) {
        for (CraftingMode mode : values()) {
            if (mode.serializedName.equals(name)) {
                return mode;
            }
        }
        return LEGACY;
    }
}
