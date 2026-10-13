package net.jr.client.viewport;

public final class Viewport {
    private final int index;
    private final Rect framebuffer;
    private final Rect window;
    private final int guiWidth;
    private final int guiHeight;
    private final int guiScale;

    Viewport(int index, Rect framebuffer, Rect window, int guiScale) {
        this.index = index;
        this.framebuffer = framebuffer;
        this.window = window;
        this.guiScale = guiScale;
        this.guiWidth = Math.max(1, (int) Math.ceil(framebuffer.width() / (double) guiScale));
        this.guiHeight = Math.max(1, (int) Math.ceil(framebuffer.height() / (double) guiScale));
    }

    public int index() { return this.index; }
    public Rect framebuffer() { return this.framebuffer; }
    public Rect window() { return this.window; }
    public int guiWidth() { return this.guiWidth; }
    public int guiHeight() { return this.guiHeight; }
    public int guiScale() { return this.guiScale; }
    public float aspectRatio() { return (float) this.framebuffer.width() / this.framebuffer.height(); }

    public boolean containsWindowPoint(double x, double y) {
        return this.window.contains(x, y);
    }

    public boolean containsFramebufferPoint(double x, double y) {
        return this.framebuffer.contains(x, y);
    }

    public double framebufferToLocalPixelX(double x) {
        return x - this.framebuffer.x();
    }

    public double framebufferToLocalPixelY(double y) {
        return y - this.framebuffer.y();
    }

    public double windowToLocalPixelX(double x) {
        return (x - this.window.x()) * this.framebuffer.width() / this.window.width();
    }

    public double windowToLocalPixelY(double y) {
        return (y - this.window.y()) * this.framebuffer.height() / this.window.height();
    }

    public double windowToLocalGuiX(double x) {
        return (x - this.window.x()) * this.guiWidth / this.window.width();
    }

    public double windowToLocalGuiY(double y) {
        return (y - this.window.y()) * this.guiHeight / this.window.height();
    }

    public double windowDeltaToGuiX(double delta) {
        return delta * this.guiWidth / this.window.width();
    }

    public double windowDeltaToGuiY(double delta) {
        return delta * this.guiHeight / this.window.height();
    }

    public record Rect(int x, int y, int width, int height) {
        public Rect {
            if (x < 0 || y < 0 || width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Viewport bounds must have nonnegative origin and positive size");
            }
        }

        public boolean contains(double pointX, double pointY) {
            return pointX >= x && pointX < (long) x + width && pointY >= y && pointY < (long) y + height;
        }
    }
}
