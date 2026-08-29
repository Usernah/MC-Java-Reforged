package net.jr.api.client.render.font;

import net.jr.api.client.resource.Asset;
import net.jr.client.render.font.FontResource;
import net.minecraft.network.chat.Component;

public final class FontHolder {
    private final FontResource resource;

    FontHolder(FontResource resource) {
        this.resource = resource;
    }

    public String modId() {
        return this.resource.modId();
    }

    public String path() {
        return this.resource.path();
    }

    public Asset location() {
        return this.resource.location();
    }

    public Asset loader() {
        return this.resource.loader();
    }

    public Component apply(Component text) {
        return this.resource.apply(text);
    }

    public int width(String text) {
        return this.resource.width(text);
    }

    public int width(Component text) {
        return this.resource.width(text);
    }

    public int width(String text, float scale) {
        return this.resource.width(text, scale);
    }

    public int width(Component text, float scale) {
        return this.resource.width(text, scale);
    }

    public int lineHeight() {
        return this.resource.lineHeight();
    }

    public int lineHeight(float scale) {
        return this.resource.lineHeight(scale);
    }

    public String debugId() {
        return this.resource.debugId();
    }

    @Override
    public boolean equals(Object object) {
        return this == object
                || object instanceof FontHolder that && this.resource.equals(that.resource);
    }

    @Override
    public int hashCode() {
        return this.resource.hashCode();
    }

    @Override
    public String toString() {
        return "FontHolder[" + this.location() + "]";
    }
}
