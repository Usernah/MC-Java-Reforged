package net.jr.client.render.glyph;

import net.jr.api.client.render.glyph.GlyphDefinition;
import net.jr.api.client.resource.Asset;

public record ResolvedGlyph(
    Asset asset,
    GlyphTextureBounds bounds,
    float drawWidth,
    float drawHeight,
    float visibleLeft,
    float visibleRight,
    float visibleTop,
    float visibleBottom,
    float offsetX,
    float offsetY,
    float sideAdvance,
    float advance
) {
    public static ResolvedGlyph from(GlyphDefinition definition) {
        GlyphTextureBounds bounds = GlyphTextureBoundsCache.get(definition.asset());
        float drawHeight = definition.height();
        float drawWidth = definition.width() == null
            ? bounds.drawWidthForHeight(drawHeight)
            : definition.width();
        float visibleLeft = bounds.visibleLeft(drawWidth);
        float visibleRight = bounds.visibleRight(drawWidth);
        float visibleTop = bounds.visibleTop(drawHeight);
        float visibleBottom = bounds.visibleBottom(drawHeight);
        float visibleWidth = visibleRight - visibleLeft;
        float sideAdvance = definition.advance() == null
            ? 0.0F
            : definition.advance();
        float advance = visibleWidth + sideAdvance * 2.0F;
        return new ResolvedGlyph(
            definition.asset(),
            bounds,
            drawWidth,
            drawHeight,
            visibleLeft,
            visibleRight,
            visibleTop,
            visibleBottom,
            definition.offsetX(),
            definition.offsetY(),
            sideAdvance,
            advance
        );
    }

    public float visibleWidth() {
        return this.visibleRight - this.visibleLeft;
    }

    public float visibleHeight() {
        return this.visibleBottom - this.visibleTop;
    }
}
