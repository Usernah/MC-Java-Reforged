package net.jr.api.client.render.glyph;

import net.jr.api.client.resource.Asset;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record GlyphDefinition(
    Asset asset,
    float height,
    @Nullable Float width,
    float offsetX,
    float offsetY,
    @Nullable Float advance
) {
    public GlyphDefinition {
        Objects.requireNonNull(asset, "asset");
        requirePositive("height", height);
        if (width != null) {
            requirePositive("width", width);
        }
        requireFinite("offsetX", offsetX);
        requireFinite("offsetY", offsetY);
        if (advance != null) {
            requireNonNegative("advance", advance);
        }
    }

    private static void requirePositive(String name, float value) {
        requireFinite(name, value);
        if (value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonNegative(String name, float value) {
        requireFinite(name, value);
        if (value < 0.0F) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    private static void requireFinite(String name, float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
