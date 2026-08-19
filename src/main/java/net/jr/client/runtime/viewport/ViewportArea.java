package net.jr.client.runtime.viewport;

public final class ViewportArea {
    private final ViewportLayout layout;
    private final int id;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int glX;
    private final int glY;
    private final int glWidth;
    private final int glHeight;
    private final int windowX;
    private final int windowY;
    private final int windowWidth;
    private final int windowHeight;
    private final int guiX;
    private final int guiY;
    private final int guiWidth;
    private final int guiHeight;
    private final int guiScale;
    private final double effectiveGuiScale;
    private final float aspectRatio;

    ViewportArea(
        ViewportLayout layout,
        int id,
        int x,
        int y,
        int width,
        int height,
        int framebufferHeight,
        int windowX,
        int windowY,
        int windowWidth,
        int windowHeight,
        int guiX,
        int guiY,
        int guiWidth,
        int guiHeight,
        int guiScale,
        double effectiveGuiScale
    ) {
        this.layout = layout;
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.glX = x;
        this.glY = framebufferHeight - y - height;
        this.glWidth = width;
        this.glHeight = height;
        this.windowX = windowX;
        this.windowY = windowY;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.guiX = guiX;
        this.guiY = guiY;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
        this.guiScale = guiScale;
        this.effectiveGuiScale = effectiveGuiScale;
        this.aspectRatio = height > 0 ? (float)width / (float)height : 1.0F;
    }

    public boolean containsFramebufferPoint(double pointX, double pointY) {
        return pointX >= this.x && pointX < this.x + this.width && pointY >= this.y && pointY < this.y + this.height;
    }

    public boolean containsWindowMouse(double mouseX, double mouseY) {
        return mouseX >= this.windowX
            && mouseX < this.windowX + this.windowWidth
            && mouseY >= this.windowY
            && mouseY < this.windowY + this.windowHeight;
    }

    public double windowMouseToLocalPixelX(double mouseX) {
        return (mouseX - this.windowX) * this.width / (double)this.windowWidth;
    }

    public double windowMouseToLocalPixelY(double mouseY) {
        return (mouseY - this.windowY) * this.height / (double)this.windowHeight;
    }

    public double windowMouseToLocalGuiX(double mouseX) {
        return (mouseX - this.windowX) * this.guiWidth / (double)this.windowWidth;
    }

    public double windowMouseToLocalGuiY(double mouseY) {
        return (mouseY - this.windowY) * this.guiHeight / (double)this.windowHeight;
    }

    public double windowMouseToGuiX(double mouseX) {
        return this.guiX + this.windowMouseToLocalGuiX(mouseX);
    }

    public double windowMouseToGuiY(double mouseY) {
        return this.guiY + this.windowMouseToLocalGuiY(mouseY);
    }

    public double framebufferToLocalPixelX(double pointX) {
        return pointX - this.x;
    }

    public double framebufferToLocalPixelY(double pointY) {
        return pointY - this.y;
    }

    public ViewportLayout layout() {
        return this.layout;
    }

    public int id() {
        return this.id;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public int glX() {
        return this.glX;
    }

    public int glY() {
        return this.glY;
    }

    public int glWidth() {
        return this.glWidth;
    }

    public int glHeight() {
        return this.glHeight;
    }

    public int windowX() {
        return this.windowX;
    }

    public int windowY() {
        return this.windowY;
    }

    public int windowWidth() {
        return this.windowWidth;
    }

    public int windowHeight() {
        return this.windowHeight;
    }

    public int guiX() {
        return this.guiX;
    }

    public int guiY() {
        return this.guiY;
    }

    public int guiWidth() {
        return this.guiWidth;
    }

    public int guiHeight() {
        return this.guiHeight;
    }

    public int guiScale() {
        return this.guiScale;
    }

    public double effectiveGuiScale() {
        return this.effectiveGuiScale;
    }

    public float aspectRatio() {
        return this.aspectRatio;
    }
}
