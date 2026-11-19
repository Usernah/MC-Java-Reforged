package net.jr.client.ui.presentation;

import java.util.Objects;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.ClientRuntime;

public final class UiPresentationPolicy {
    private static final ThreadLocal<UiSizeMode> OVERRIDE = new ThreadLocal<>();

    private UiPresentationPolicy() {
    }

    public static UiSizeMode sizeMode() {
        UiSizeMode override = OVERRIDE.get();
        if (override != null) {
            return override;
        }
        if (SplitScreen.isActive()) {
            return UiSizeMode.SMALL;
        }
        return ClientRuntime.INSTANCE
                .slots()
                .primary()
                .optionsState()
                .interfaceMode()
                .get();
    }

    public static boolean isSmall() {
        return sizeMode().isSmall();
    }

    public static Scope push(UiSizeMode mode) {
        Objects.requireNonNull(mode, "mode");
        UiSizeMode previous = OVERRIDE.get();
        OVERRIDE.set(mode);
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {
        private final UiSizeMode previous;
        private boolean closed;

        private Scope(UiSizeMode previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.previous == null) {
                OVERRIDE.remove();
            } else {
                OVERRIDE.set(this.previous);
            }
        }
    }
}
