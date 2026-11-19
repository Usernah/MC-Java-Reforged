package net.jr.client.components.widgets;

import java.util.Objects;


public final class GridDirection {
    public enum Axis {
        HORIZONTAL,
        VERTICAL
    }

    public enum VerticalAlignment {
        TOP,
        CENTER,
        BOTTOM
    }

    public enum HorizontalAlignment {
        LEFT,
        CENTER,
        RIGHT
    }

    private final Axis axis;
    private final VerticalAlignment verticalAlignment;
    private final HorizontalAlignment horizontalAlignment;

    private GridDirection(
        Axis axis,
        VerticalAlignment verticalAlignment,
        HorizontalAlignment horizontalAlignment
    ) {
        this.axis = Objects.requireNonNull(axis, "axis");
        this.verticalAlignment = verticalAlignment;
        this.horizontalAlignment = horizontalAlignment;
    }

    public static GridDirection horizontal(VerticalAlignment alignment) {
        return new GridDirection(
            Axis.HORIZONTAL,
            Objects.requireNonNull(alignment, "alignment"),
            null
        );
    }

    public static GridDirection vertical(HorizontalAlignment alignment) {
        return new GridDirection(
            Axis.VERTICAL,
            null,
            Objects.requireNonNull(alignment, "alignment")
        );
    }

    public Axis axis() {
        return this.axis;
    }

    float crossOffset(float available, float occupied) {
        float remaining = Math.max(0.0F, available - occupied);
        if (this.axis == Axis.HORIZONTAL) {
            return switch (this.verticalAlignment) {
                case TOP -> 0.0F;
                case CENTER -> remaining / 2.0F;
                case BOTTOM -> remaining;
            };
        }
        return switch (this.horizontalAlignment) {
            case LEFT -> 0.0F;
            case CENTER -> remaining / 2.0F;
            case RIGHT -> remaining;
        };
    }
}
