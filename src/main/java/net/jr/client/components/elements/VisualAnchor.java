package net.jr.client.components.elements;

import java.util.Locale;


public enum VisualAnchor {
    NONE(Horizontal.NONE, Vertical.NONE),
    TOP_LEFT(Horizontal.LEFT, Vertical.TOP),
    TOP_CENTER(Horizontal.CENTER, Vertical.TOP),
    TOP_RIGHT(Horizontal.RIGHT, Vertical.TOP),
    CENTER_LEFT(Horizontal.LEFT, Vertical.CENTER),
    CENTER(Horizontal.CENTER, Vertical.CENTER),
    CENTER_RIGHT(Horizontal.RIGHT, Vertical.CENTER),
    BOTTOM_LEFT(Horizontal.LEFT, Vertical.BOTTOM),
    BOTTOM_CENTER(Horizontal.CENTER, Vertical.BOTTOM),
    BOTTOM_RIGHT(Horizontal.RIGHT, Vertical.BOTTOM);

    private final Horizontal horizontal;
    private final Vertical vertical;

    VisualAnchor(Horizontal horizontal, Vertical vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public float resolveX(float offset, float width, float viewportWidth) {
        return this.resolveX(offset, width, 0.0F, viewportWidth);
    }

    public float resolveX(float offset, float width, float referenceX, float referenceWidth) {
        return switch (this.horizontal) {
            case NONE, LEFT -> referenceX + offset;
            case CENTER -> referenceX + referenceWidth / 2.0F + offset - width / 2.0F;
            case RIGHT -> referenceX + referenceWidth + offset - width;
        };
    }

    public float resolveY(float offset, float height, float viewportHeight) {
        return this.resolveY(offset, height, 0.0F, viewportHeight);
    }

    public float resolveY(float offset, float height, float referenceY, float referenceHeight) {
        return switch (this.vertical) {
            case NONE, TOP -> referenceY + offset;
            case CENTER -> referenceY + referenceHeight / 2.0F + offset - height / 2.0F;
            case BOTTOM -> referenceY + referenceHeight + offset - height;
        };
    }

    public boolean isExplicit() {
        return this != NONE;
    }

    public static VisualAnchor parse(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        String normalized = value.trim()
            .toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported visual anchor '" + value + "'");
        }
    }

    private enum Horizontal {
        NONE,
        LEFT,
        CENTER,
        RIGHT
    }

    private enum Vertical {
        NONE,
        TOP,
        CENTER,
        BOTTOM
    }
}
