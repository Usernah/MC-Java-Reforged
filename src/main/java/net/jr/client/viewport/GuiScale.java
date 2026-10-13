package net.jr.client.viewport;

public enum GuiScale {
    STANDARD(320, 240),
    COMPACT(280, 200);

    private final int minimumWidth;
    private final int minimumHeight;

    GuiScale(int minimumWidth, int minimumHeight) {
        this.minimumWidth = minimumWidth;
        this.minimumHeight = minimumHeight;
    }

    public int resolve(int width, int height, int requested, boolean unicode) {
        if (width <= 0 || height <= 0 || requested < 0) {
            throw new IllegalArgumentException("Invalid GUI scale inputs");
        }
        int scale = 1;
        while (scale != requested && scale < width && scale < height
                && width / (scale + 1) >= this.minimumWidth
                && height / (scale + 1) >= this.minimumHeight) {
            scale++;
        }
        if (unicode && (scale & 1) != 0) {
            scale++;
        }
        return scale;
    }
}
