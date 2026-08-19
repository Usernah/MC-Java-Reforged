package net.jr.client.ui.hint.render;

public record GlyphTextureBounds(int sourceWidth, int sourceHeight, int minX, int maxX, int minY, int maxY) {
    public static GlyphTextureBounds full(int sourceWidth, int sourceHeight) {
        int safeWidth = Math.max(1, sourceWidth);
        int safeHeight = Math.max(1, sourceHeight);
        return new GlyphTextureBounds(safeWidth, safeHeight, 0, safeWidth - 1, 0, safeHeight - 1);
    }

    public float visibleLeft(float drawWidth) {
        return (float) this.minX * drawWidth / (float) this.sourceWidth;
    }

    public float visibleRight(float drawWidth) {
        return (float) (this.maxX + 1) * drawWidth / (float) this.sourceWidth;
    }

    public float visibleTop(float drawHeight) {
        return (float) this.minY * drawHeight / (float) this.sourceHeight;
    }

    public float visibleBottom(float drawHeight) {
        return (float) (this.maxY + 1) * drawHeight / (float) this.sourceHeight;
    }

    public float drawWidthForHeight(float drawHeight) {
        return drawHeight * (float) this.sourceWidth / (float) this.sourceHeight;
    }
}
