package net.jr.client.ui.render.composition;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.ArrayDeque;
import java.util.Deque;
import org.jetbrains.annotations.Nullable;


public final class GuiCompositionScope {
    private static final ThreadLocal<Deque<Extraction>> EXTRACTIONS =
        ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<Rendering>> RENDERINGS =
        ThreadLocal.withInitial(ArrayDeque::new);

    private GuiCompositionScope() {
    }

    public static Scope enterExtraction(int width, int height, int rasterScale) {
        Extraction extraction = new Extraction(width, height, rasterScale);
        EXTRACTIONS.get().push(extraction);
        return new Scope(EXTRACTIONS, extraction);
    }

    public static Scope enterRendering(
        RenderTarget target,
        int framebufferWidth,
        int framebufferHeight,
        int guiScale
    ) {
        Rendering rendering = new Rendering(
            target, framebufferWidth, framebufferHeight, guiScale
        );
        RENDERINGS.get().push(rendering);
        return new Scope(RENDERINGS, rendering);
    }

    public static boolean isExtracting() {
        return activeExtractionOrNull() != null;
    }

    @Nullable
    public static Extraction activeExtractionOrNull() {
        Deque<Extraction> scopes = EXTRACTIONS.get();
        return scopes.isEmpty() ? null : scopes.peek();
    }

    @Nullable
    public static Rendering activeRenderingOrNull() {
        Deque<Rendering> scopes = RENDERINGS.get();
        return scopes.isEmpty() ? null : scopes.peek();
    }

    @Nullable
    public static RenderTarget activeRenderTargetOrNull() {
        Rendering rendering = activeRenderingOrNull();
        return rendering == null ? null : rendering.target();
    }

    public record Extraction(int width, int height, int rasterScale) {
    }

    public record Rendering(
        RenderTarget target,
        int framebufferWidth,
        int framebufferHeight,
        int guiScale
    ) {
    }

    public static final class Scope implements AutoCloseable {
        private final ThreadLocal<? extends Deque<?>> owner;
        private final Object value;
        private boolean closed;

        private Scope(ThreadLocal<? extends Deque<?>> owner, Object value) {
            this.owner = owner;
            this.value = value;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            Deque<?> scopes = this.owner.get();
            if (scopes.isEmpty() || scopes.peek() != this.value) {
                throw new IllegalStateException("GUI composition scope is unbalanced");
            }
            scopes.pop();
            if (scopes.isEmpty()) {
                this.owner.remove();
            }
        }
    }
}
