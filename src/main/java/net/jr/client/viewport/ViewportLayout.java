package net.jr.client.viewport;

public enum ViewportLayout {
    SINGLE,
    TWO_VERTICAL,
    TWO_HORIZONTAL,
    GRID;

    public static ViewportLayout forCount(int count, boolean horizontalTwoPlayerSplit) {
        return switch (count) {
            case 1 -> SINGLE;
            case 2 -> horizontalTwoPlayerSplit ? TWO_HORIZONTAL : TWO_VERTICAL;
            case 3, 4 -> GRID;
            default -> throw new IllegalArgumentException("Viewport count must be between 1 and 4: " + count);
        };
    }

    void requireCount(int count) {
        if (count < 1 || count > 4 || (this == SINGLE && count != 1)
                || ((this == TWO_VERTICAL || this == TWO_HORIZONTAL) && count != 2)
                || (this == GRID && count < 3)) {
            throw new IllegalArgumentException("Layout " + this + " cannot contain " + count + " viewports");
        }
    }
}
