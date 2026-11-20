package net.jr.client.runtime.viewport;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderPass;
import java.util.Objects;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class ViewportRenderScope {
    private static final ThreadLocal<RenderFrame> ACTIVE_RENDER = new ThreadLocal<>();

    private ViewportRenderScope() {
    }

    public static Scope enter(ViewportArea viewport) {
        Objects.requireNonNull(viewport, "viewport");
        RenderFrame previousRender = ACTIVE_RENDER.get();
        RenderFrame render = new RenderFrame(viewport);
        ACTIVE_RENDER.set(render);
        return new Scope(render, previousRender);
    }

    public static Scope enterGui(ViewportArea viewport) {
        return enter(viewport);
    }

    public static void run(ViewportArea viewport, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        try (Scope ignored = enter(viewport)) {
            runnable.run();
        }
    }

    public static Matrix4f projectionMatrix(double fovDegrees, float zoom, float zoomX, float zoomY, float depthFar) {
        ViewportArea viewport = activeViewportOrNull();
        if (viewport == null) {
            return null;
        }

        Matrix4f matrix = new Matrix4f();
        if (zoom != 1.0F) {
            matrix.translate(zoomX, -zoomY, 0.0F);
            matrix.scale(zoom, zoom, 1.0F);
        }
        return matrix.perspective((float)(fovDegrees * (Math.PI / 180.0D)), viewport.aspectRatio(), 0.05F, depthFar);
    }

    public static int shaderWidth(Window window) {
        ViewportArea viewport = activeViewportOrNull();
        return viewport != null ? viewport.width() : window.getWidth();
    }

    public static int shaderHeight(Window window) {
        ViewportArea viewport = activeViewportOrNull();
        return viewport != null ? viewport.height() : window.getHeight();
    }

    public static @Nullable ViewportArea activeViewportOrNull() {
        RenderFrame render = ACTIVE_RENDER.get();
        return render != null ? render.viewport() : null;
    }

    public static ViewportArea activeViewport() {
        ViewportArea viewport = activeViewportOrNull();
        if (viewport == null) {
            throw new IllegalStateException("No viewport render scope is active");
        }
        return viewport;
    }

    public static boolean hasActiveViewport() {
        return activeViewportOrNull() != null;
    }

    public static RenderPass.RenderArea areaFor(ViewportArea viewport, int textureWidth, int textureHeight) {
        int x = viewport.glX();
        int y = viewport.glY();
        x = Math.max(0, Math.min(x, textureWidth - 1));
        y = Math.max(0, Math.min(y, textureHeight - 1));
        return new RenderPass.RenderArea(
            x,
            y,
            Math.max(1, Math.min(viewport.width(), textureWidth - x)),
            Math.max(1, Math.min(viewport.height(), textureHeight - y))
        );
    }

    public static final class Scope implements AutoCloseable {
        private final RenderFrame render;
        private final RenderFrame previousRender;
        private boolean closed;

        private Scope(
            RenderFrame render,
            @Nullable RenderFrame previousRender
        ) {
            this.render = render;
            this.previousRender = previousRender;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            if (ACTIVE_RENDER.get() != this.render) {
                throw new IllegalStateException("Viewport render scope is unbalanced");
            }
            this.closed = true;
            restore(this.previousRender);
        }
    }

    private static void restore(@Nullable RenderFrame render) {
        if (render == null) {
            ACTIVE_RENDER.remove();
        } else {
            ACTIVE_RENDER.set(render);
        }
    }

    private record RenderFrame(ViewportArea viewport) {
    }
}
