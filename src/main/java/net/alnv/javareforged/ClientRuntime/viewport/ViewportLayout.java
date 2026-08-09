package net.alnv.javareforged.ClientRuntime.viewport;

public enum ViewportLayout {
    SINGLE(1),
    TWO_VERTICAL(2),
    TWO_HORIZONTAL(2),
    FOUR_GRID(4);

    private final int viewportCount;

    ViewportLayout(int viewportCount) {
        this.viewportCount = viewportCount;
    }

    public int viewportCount() {
        return this.viewportCount;
    }

    public boolean hasViewport(int id) {
        return id >= 0 && id < this.viewportCount;
    }

    public void requireViewport(int id) {
        if (!this.hasViewport(id)) {
            throw new IndexOutOfBoundsException("Viewport id " + id + " is not valid for layout " + this);
        }
    }
}
