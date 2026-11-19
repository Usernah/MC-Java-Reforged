package net.jr.client.runtime.viewport;

public final class ViewportHudLayout {
    private static final ThreadLocal<Float> ACTIVE_BOTTOM_INSET =
        ThreadLocal.withInitial(() -> 0.0F);

    private ViewportHudLayout() {
    }

    public static int hotbarSelectionHeight() {
        return 24;
    }

    public static BottomInsetScope pushBottomInset(float bottomInset) {
        float previousInset = activeBottomInset();
        float safeInset = Float.isFinite(bottomInset)
            ? Math.max(0.0F, bottomInset)
            : 0.0F;
        ACTIVE_BOTTOM_INSET.set(safeInset);
        return new BottomInsetScope(previousInset);
    }

    public static int bottomAnchorY(int viewportHeight) {
        return Math.round(viewportHeight - activeBottomInset());
    }

    private static float activeBottomInset() {
        return ACTIVE_BOTTOM_INSET.get();
    }

    public static final class BottomInsetScope implements AutoCloseable {
        private final float previousInset;
        private boolean closed;

        private BottomInsetScope(float previousInset) {
            this.previousInset = previousInset;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            if (this.previousInset == 0.0F) {
                ACTIVE_BOTTOM_INSET.remove();
            } else {
                ACTIVE_BOTTOM_INSET.set(this.previousInset);
            }
            this.closed = true;
        }
    }
}
