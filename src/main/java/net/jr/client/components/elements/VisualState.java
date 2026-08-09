package net.jr.client.components.elements;

public final class VisualState {
    private float x;
    private float baseX;
    private float y;
    private float baseY;
    private float width;
    private float baseWidth;
    private float height;
    private float baseHeight;
    private float rotation;
    private float baseRotation;
    private float alpha = 1.0F;
    private float baseAlpha = 1.0F;
    private boolean visible = true;
    private boolean baseVisible = true;

    public VisualState(float x, float y, float width, float height) {
        this.setBounds(x, y, width, height);
        this.captureBaseState();
    }

    public float x() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float baseX() {
        return this.baseX;
    }

    public void setBaseX(float baseX) {
        this.baseX = baseX;
    }

    public float y() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float baseY() {
        return this.baseY;
    }

    public void setBaseY(float baseY) {
        this.baseY = baseY;
    }

    public float width() {
        return this.width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float baseWidth() {
        return this.baseWidth;
    }

    public void setBaseWidth(float baseWidth) {
        this.baseWidth = baseWidth;
    }

    public float height() {
        return this.height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float baseHeight() {
        return this.baseHeight;
    }

    public void setBaseHeight(float baseHeight) {
        this.baseHeight = baseHeight;
    }

    public float rotation() {
        return this.rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public float baseRotation() {
        return this.baseRotation;
    }

    public void setBaseRotation(float baseRotation) {
        this.baseRotation = baseRotation;
    }

    public float alpha() {
        return this.alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = clampAlpha(alpha);
    }

    public float baseAlpha() {
        return this.baseAlpha;
    }

    public void setBaseAlpha(float baseAlpha) {
        this.baseAlpha = clampAlpha(baseAlpha);
    }

    public boolean visible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean baseVisible() {
        return this.baseVisible;
    }

    public void setBaseVisible(boolean baseVisible) {
        this.baseVisible = baseVisible;
    }

    public void setPosition(float x, float y) {
        this.setX(x);
        this.setY(y);
    }

    public void setBasePosition(float x, float y) {
        this.setBaseX(x);
        this.setBaseY(y);
    }

    public void setSize(float width, float height) {
        this.setWidth(width);
        this.setHeight(height);
    }

    public void setBaseSize(float width, float height) {
        this.setBaseWidth(width);
        this.setBaseHeight(height);
    }

    public void setBounds(float x, float y, float width, float height) {
        this.setPosition(x, y);
        this.setSize(width, height);
    }

    public void setBaseBounds(float x, float y, float width, float height) {
        this.setBasePosition(x, y);
        this.setBaseSize(width, height);
    }

    public void resetVisualState() {
        this.setX(this.baseX());
        this.setY(this.baseY());
        this.setWidth(this.baseWidth());
        this.setHeight(this.baseHeight());
        this.setRotation(this.baseRotation());
        this.setAlpha(this.baseAlpha());
        this.setVisible(this.baseVisible());
    }

    public void captureBaseState() {
        this.setBaseX(this.x());
        this.setBaseY(this.y());
        this.setBaseWidth(this.width());
        this.setBaseHeight(this.height());
        this.setBaseRotation(this.rotation());
        this.setBaseAlpha(this.alpha());
        this.setBaseVisible(this.visible());
    }

    private static float clampAlpha(float alpha) {
        return Math.max(0.0F, Math.min(alpha, 1.0F));
    }
}
