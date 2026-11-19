package net.jr.client.runtime.render.state;


public final class HudCameraOverlayScope implements AutoCloseable {
    private static final ThreadLocal<Integer> SUPPRESSION_DEPTH =
            ThreadLocal.withInitial(() -> 0);

    private boolean closed;

    private HudCameraOverlayScope() {
        SUPPRESSION_DEPTH.set(SUPPRESSION_DEPTH.get() + 1);
    }

    public static HudCameraOverlayScope suppress() {
        return new HudCameraOverlayScope();
    }

    public static boolean isSuppressed() {
        return SUPPRESSION_DEPTH.get() > 0;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;

        int depth = SUPPRESSION_DEPTH.get() - 1;
        if (depth <= 0) {
            SUPPRESSION_DEPTH.remove();
        } else {
            SUPPRESSION_DEPTH.set(depth);
        }
    }
}
