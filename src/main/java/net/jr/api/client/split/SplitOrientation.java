package net.jr.api.client.split;

import net.minecraft.network.chat.Component;

public enum SplitOrientation {
    VERTICAL("vertical"),
    HORIZONTAL("horizontal");

    private final String serializedName;

    SplitOrientation(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return this.serializedName;
    }

    public Component caption() {
        return Component.translatable("menu.java_reforged.video.split_orientation." + this.serializedName);
    }
}
