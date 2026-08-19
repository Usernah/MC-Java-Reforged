package net.jr.client.ui.hint.model;

import net.jr.client.ui.hint.render.GlyphTextureBounds;
import net.jr.api.client.resource.Asset;

import java.util.Objects;

public record ResolvedControlHintIcon(
    Asset texture,
    GlyphTextureBounds metrics,
    float iconHeight
) {
    public ResolvedControlHintIcon {
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(metrics, "metrics");
        if (iconHeight <= 0.0F) {
            throw new IllegalArgumentException("iconHeight must be positive");
        }
    }

    public float iconWidth() {
        return this.drawWidth();
    }

    public float iconHeight() {
        return this.drawHeight();
    }

    public float drawWidth() {
        return this.metrics.drawWidthForHeight(this.drawHeight());
    }

    public float drawHeight() {
        return this.iconHeight;
    }

    public float visibleLeft() {
        return this.metrics.visibleLeft(this.drawWidth());
    }

    public float visibleRight() {
        return this.metrics.visibleRight(this.drawWidth());
    }

    public float visibleTop() {
        return this.metrics.visibleTop(this.drawHeight());
    }

    public float visibleBottom() {
        return this.metrics.visibleBottom(this.drawHeight());
    }
}
