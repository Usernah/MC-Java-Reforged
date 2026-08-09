package net.jr.ClientRuntime.runtime;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderPass;
import java.util.Objects;
import net.jr.ClientRuntime.viewport.ViewportArea;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Exposes the logical dimensions of the local player currently being rendered.
 * The vanilla passes themselves always use a complete viewport-local target and
 * are never resized or offset individually.
 */
public final class ViewportPass {
    private static final ThreadLocal<ViewportArea> ACTIVE_VIEWPORT = new ThreadLocal<>();

    private ViewportPass() {
    }

    public static Scope enter(ViewportArea viewport) {
        Objects.requireNonNull(viewport, "viewport");
        ViewportArea previousViewport = ACTIVE_VIEWPORT.get();
        ACTIVE_VIEWPORT.set(viewport);
        return new Scope(previousViewport);
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
        ViewportArea viewport = ACTIVE_VIEWPORT.get();
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
        ViewportArea viewport = ACTIVE_VIEWPORT.get();
        return viewport != null ? viewport.width() : window.getWidth();
    }

    public static int shaderHeight(Window window) {
        ViewportArea viewport = ACTIVE_VIEWPORT.get();
        return viewport != null ? viewport.height() : window.getHeight();
    }

    public static @Nullable ViewportArea activeViewportOrNull() {
        return ACTIVE_VIEWPORT.get();
    }

    public static ViewportArea activeViewport() {
        ViewportArea viewport = ACTIVE_VIEWPORT.get();
        if (viewport == null) {
            throw new IllegalStateException("No viewport is active");
        }
        return viewport;
    }

    public static boolean hasActiveViewport() {
        return ACTIVE_VIEWPORT.get() != null;
    }

    /** Computes the one destination region used when the completed slot is presented. */
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
        private final ViewportArea previousViewport;
        private boolean closed;

        private Scope(ViewportArea previousViewport) {
            this.previousViewport = previousViewport;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.previousViewport == null) {
                ACTIVE_VIEWPORT.remove();
            } else {
                ACTIVE_VIEWPORT.set(this.previousViewport);
            }
        }
    }
}
