package net.jr.client.ui.presentation;

import net.minecraft.network.chat.Component;

public enum UiSizeMode {
    NORMAL("normal"),
    SMALL("compact");

    private final String serializedName;

    UiSizeMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public boolean isSmall() {
        return this == SMALL;
    }

    public String serializedName() {
        return this.serializedName;
    }

    public Component caption() {
        return Component.translatable("options.java_reforged.interface_mode." + this.serializedName);
    }

    public static UiSizeMode byName(String value) {
        for (UiSizeMode mode : values()) {
            if (mode.serializedName.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return NORMAL;
    }
}
