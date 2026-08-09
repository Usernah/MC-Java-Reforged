package net.jr.ClientRuntime.runtime;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Objects;
import net.jr.ClientRuntime.viewport.ViewportArea;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Defines the framebuffer region owned by the local player currently being rendered.
 *
 * <p>The scope itself is backend neutral. Render-pass mixins consume it through
 * {@link #constrain(RenderPassDescriptor)} and Mojang's {@code RenderPassBackend};
 * this class must never manipulate OpenGL or Vulkan state directly.</p>
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

    /** Restricts a newly-created Mojang render pass to the active player's region. */
    public static void constrain(RenderPassDescriptor descriptor) {
        ViewportArea viewport = ACTIVE_VIEWPORT.get();
        if (viewport == null) {
            return;
        }

        GpuTextureView attachment = firstAttachment(descriptor);
        if (attachment == null) {
            return;
        }

        RenderPass.RenderArea viewportArea = areaFor(viewport, attachment.getWidth(0), attachment.getHeight(0));
        RenderPass.RenderArea requestedArea = descriptor.renderArea;
        descriptor.renderArea = requestedArea == null ? viewportArea : intersection(requestedArea, viewportArea);
    }

    /**
     * Computes the viewport used by a pass. Full-window attachments use the slot's
     * framebuffer coordinates; viewport-local attachments use their own origin.
     */
    public static RenderPass.RenderArea areaFor(ViewportArea viewport, int textureWidth, int textureHeight) {
        RenderTarget mainTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        boolean windowSized = mainTarget != null
            && textureWidth >= mainTarget.width
            && textureHeight >= mainTarget.height;

        if (!windowSized) {
            return new RenderPass.RenderArea(
                0,
                0,
                Math.max(1, Math.min(viewport.width(), textureWidth)),
                Math.max(1, Math.min(viewport.height(), textureHeight))
            );
        }

        int x = isVulkanBackend() ? viewport.x() : viewport.glX();
        int y = isVulkanBackend() ? viewport.y() : viewport.glY();
        return new RenderPass.RenderArea(
            x,
            y,
            Math.min(viewport.width(), textureWidth - x),
            Math.min(viewport.height(), textureHeight - y)
        );
    }

    private static boolean isVulkanBackend() {
        return "Vulkan".equalsIgnoreCase(RenderSystem.getDevice().getDeviceInfo().backendName());
    }

    private static @Nullable GpuTextureView firstAttachment(RenderPassDescriptor descriptor) {
        for (RenderPassDescriptor.Attachment<?> attachment : descriptor.colorAttachments) {
            if (attachment != null) {
                return attachment.textureView();
            }
        }
        return descriptor.depthAttachment == null ? null : descriptor.depthAttachment.textureView();
    }

    private static RenderPass.RenderArea intersection(RenderPass.RenderArea first, RenderPass.RenderArea second) {
        int x = Math.max(first.x(), second.x());
        int y = Math.max(first.y(), second.y());
        int right = Math.min(first.x() + first.width(), second.x() + second.width());
        int bottom = Math.min(first.y() + first.height(), second.y() + second.height());
        if (right <= x || bottom <= y) {
            // A pass that intentionally targets a disjoint subregion cannot draw into
            // this player viewport. Keep a valid one-pixel area inside the viewport.
            return new RenderPass.RenderArea(second.x(), second.y(), 1, 1);
        }
        return new RenderPass.RenderArea(x, y, right - x, bottom - y);
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
