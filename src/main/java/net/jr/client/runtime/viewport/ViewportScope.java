package net.jr.client.runtime.viewport;

import javax.annotation.Nullable;


public final class ViewportScope {
    private static final ThreadLocal<Frame> ACTIVE = new ThreadLocal<>();

    private ViewportScope() {
    }

    public static Scope enter(@Nullable ViewportArea viewport) {
        Frame previous = ACTIVE.get();
        Frame installed = new Frame(viewport);
        ACTIVE.set(installed);
        return new Scope(installed, previous);
    }

    public static boolean isBound() {
        return ACTIVE.get() != null;
    }

    @Nullable
    public static ViewportArea currentOrNull() {
        Frame frame = ACTIVE.get();
        return frame != null ? frame.viewport() : null;
    }

    public static ViewportArea current() {
        ViewportArea viewport = currentOrNull();
        if (viewport == null) {
            throw new IllegalStateException("No resolved viewport is bound to this execution path");
        }
        return viewport;
    }

    private record Frame(@Nullable ViewportArea viewport) {
    }

    public static final class Scope implements AutoCloseable {
        private final Frame installed;
        @Nullable
        private final Frame previous;
        private boolean closed;

        private Scope(Frame installed, @Nullable Frame previous) {
            this.installed = installed;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            if (ACTIVE.get() != this.installed) {
                throw new IllegalStateException("Viewport execution scope is unbalanced");
            }
            this.closed = true;
            if (this.previous == null) {
                ACTIVE.remove();
            } else {
                ACTIVE.set(this.previous);
            }
        }
    }
}
