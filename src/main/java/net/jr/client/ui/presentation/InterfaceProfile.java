package net.jr.client.ui.presentation;

import net.minecraft.network.chat.Component;

public enum InterfaceProfile {
    STANDARD("standard"),
    PORTABLE("portable");

    private final String serializedName;

    InterfaceProfile(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return this.serializedName;
    }

    public Component caption() {
        return Component.translatable("menu.java_reforged.video.interface_profile." + this.serializedName);
    }
}
