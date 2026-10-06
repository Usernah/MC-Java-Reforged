package net.jr.client.runtime.input;

final class InputFrameClock {
    private final double targetFps;
    private final double minimumScale;
    private final double maximumScale;
    private final double initialScale;
    private long lastFrameNanos;

    InputFrameClock(double targetFps, double minimumScale, double maximumScale, double initialScale) {
        this.targetFps = targetFps;
        this.minimumScale = minimumScale;
        this.maximumScale = maximumScale;
        this.initialScale = initialScale;
    }

    double sample() {
        long now = System.nanoTime();
        if (this.lastFrameNanos == 0L) {
            this.lastFrameNanos = now;
            return this.initialScale;
        }

        double elapsedSeconds = (now - this.lastFrameNanos) / 1_000_000_000.0D;
        this.lastFrameNanos = now;
        double scale = elapsedSeconds * this.targetFps;
        return Math.max(this.minimumScale, Math.min(this.maximumScale, scale));
    }

    void reset() {
        this.lastFrameNanos = 0L;
    }
}

