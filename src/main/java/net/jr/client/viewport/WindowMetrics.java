package net.jr.client.viewport;

public record WindowMetrics(int windowWidth, int windowHeight, int framebufferWidth, int framebufferHeight) {
    public WindowMetrics {
        if (windowWidth <= 0 || windowHeight <= 0 || framebufferWidth <= 0 || framebufferHeight <= 0) {
            throw new IllegalArgumentException("Window and framebuffer dimensions must be positive");
        }
    }
}
