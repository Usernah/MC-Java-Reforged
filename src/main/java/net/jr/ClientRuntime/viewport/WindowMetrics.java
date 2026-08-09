package net.jr.ClientRuntime.viewport;

import java.util.Objects;

public final class WindowMetrics {
    private final int windowWidth;
    private final int windowHeight;
    private final int framebufferWidth;
    private final int framebufferHeight;
    private final int guiWidth;
    private final int guiHeight;
    private final int guiScale;
    private final double effectiveGuiScale;

    public WindowMetrics(
        int windowWidth,
        int windowHeight,
        int framebufferWidth,
        int framebufferHeight,
        int guiWidth,
        int guiHeight,
        int guiScale,
        double effectiveGuiScale
    ) {
        this.windowWidth = requirePositive("windowWidth", windowWidth);
        this.windowHeight = requirePositive("windowHeight", windowHeight);
        this.framebufferWidth = requirePositive("framebufferWidth", framebufferWidth);
        this.framebufferHeight = requirePositive("framebufferHeight", framebufferHeight);
        this.guiWidth = requirePositive("guiWidth", guiWidth);
        this.guiHeight = requirePositive("guiHeight", guiHeight);
        this.guiScale = requirePositive("guiScale", guiScale);
        if (effectiveGuiScale <= 0.0D || Double.isNaN(effectiveGuiScale) || Double.isInfinite(effectiveGuiScale)) {
            throw new IllegalArgumentException("effectiveGuiScale must be a positive finite number");
        }
        this.effectiveGuiScale = effectiveGuiScale;
    }

    public static WindowMetrics framebufferOnly(
        int framebufferWidth,
        int framebufferHeight,
        int guiWidth,
        int guiHeight,
        int guiScale,
        double effectiveGuiScale
    ) {
        return new WindowMetrics(
            framebufferWidth,
            framebufferHeight,
            framebufferWidth,
            framebufferHeight,
            guiWidth,
            guiHeight,
            guiScale,
            effectiveGuiScale
        );
    }

    private static int requirePositive(String name, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    public int windowWidth() {
        return this.windowWidth;
    }

    public int windowHeight() {
        return this.windowHeight;
    }

    public int framebufferWidth() {
        return this.framebufferWidth;
    }

    public int framebufferHeight() {
        return this.framebufferHeight;
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof WindowMetrics other)) {
            return false;
        }
        return this.windowWidth == other.windowWidth
            && this.windowHeight == other.windowHeight
            && this.framebufferWidth == other.framebufferWidth
            && this.framebufferHeight == other.framebufferHeight
            && this.guiWidth == other.guiWidth
            && this.guiHeight == other.guiHeight
            && this.guiScale == other.guiScale
            && Double.compare(this.effectiveGuiScale, other.effectiveGuiScale) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.windowWidth,
            this.windowHeight,
            this.framebufferWidth,
            this.framebufferHeight,
            this.guiWidth,
            this.guiHeight,
            this.guiScale,
            this.effectiveGuiScale
        );
    }
}
