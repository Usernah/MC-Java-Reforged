package net.jr.api.client.render.font;

import net.jr.api.client.resource.Asset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public final class FontHolder {
    private final String modId;
    private final String fontPath;
    private final Asset location;
    private final Asset loader;

    FontHolder(String modId, String fontPath) {
        this.modId = Objects.requireNonNull(modId, "modId");
        this.fontPath = normalizePath(fontPath);
        this.location = Asset.NamespaceAndPatch(this.modId, this.fontPath);
        this.loader = Asset.NamespaceAndPatch(this.modId, this.fontPath);
    }

    public String modId() {
        return modId;
    }

    public String path() {
        return fontPath;
    }

    public Asset location() {
        return location;
    }

    public Asset loader() {
        return loader;
    }

    public Component apply(Component text) {
        Objects.requireNonNull(text, "text");
        return text.copy().withStyle(style -> style.withFont(this.location.asFontDescription()));
    }

    public int width(String text) {
        return width(Component.literal(text == null ? "" : text));
    }

    public int width(Component text) {
        Font font = Minecraft.getInstance().font;
        return font.width(apply(text));
    }

    public int width(String text, float scale) {
        return Math.round(width(text) * scale);
    }

    public int width(Component text, float scale) {
        return Math.round(width(text) * scale);
    }

    public int lineHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    public int lineHeight(float scale) {
        return Math.round(lineHeight() * scale);
    }

    public String debugId() {
        return location.toString();
    }

    private static String normalizePath(String fontPath) {
        Objects.requireNonNull(fontPath, "fontPath");
        String normalized = fontPath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FontHolder that)) return false;
        return location.equals(that.location);
    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }

    @Override
    public String toString() {
        return "FontHolder[" + location + "]";
    }
}
