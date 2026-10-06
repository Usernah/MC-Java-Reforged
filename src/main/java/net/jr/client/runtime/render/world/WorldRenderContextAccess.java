package net.jr.client.runtime.render.world;

import java.util.Objects;
import javax.annotation.Nullable;
import net.jr.api.client.render.world.WorldRenderContext;

public final class WorldRenderContextAccess {
    private static final ThreadLocal<WorldRenderContext> CURRENT = new ThreadLocal<>();

    private WorldRenderContextAccess() {
    }

    public static Scope enter(WorldRenderContext context) {
        Objects.requireNonNull(context, "context");
        WorldRenderContext previous = CURRENT.get();
        CURRENT.set(context);
        return new Scope(context, previous);
    }

    public static @Nullable WorldRenderContext currentOrNull() {
        return CURRENT.get();
    }

    public static final class Scope implements AutoCloseable {
        private final WorldRenderContext installed;
        @Nullable
        private final WorldRenderContext previous;
        private boolean closed;

        private Scope(WorldRenderContext installed, @Nullable WorldRenderContext previous) {
            this.installed = installed;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            WorldRenderContext current = CURRENT.get();
            if (current != this.installed) {
                throw new IllegalStateException("Unbalanced world render context scope");
            }
            this.closed = true;
            if (this.previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(this.previous);
            }
        }
    }
}
