package net.alnv.javareforged.ClientRuntime.runtime;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import net.alnv.javareforged.ClientRuntime.viewport.ViewportArea;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30C;

public final class ViewportPass {
    private static final ThreadLocal<ViewportArea> ACTIVE_VIEWPORT = new ThreadLocal<>();
    private static final ThreadLocal<Deque<ClearState>> CLEAR_STATES = ThreadLocal.withInitial(ArrayDeque::new);

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
        float clientFov = Client.fov();
        return matrix.perspective((float)(clientFov * (Math.PI / 180.0D)), viewport.aspectRatio(), 0.05F, depthFar);
    }

    public static int shaderWidth(Window window) {
        ViewportArea viewport = ACTIVE_VIEWPORT.get();
        return viewport != null ? viewport.glWidth() : window.getWidth();
    }

    public static int shaderHeight(Window window) {
        ViewportArea viewport = ACTIVE_VIEWPORT.get();
        return viewport != null ? viewport.glHeight() : window.getHeight();
    }

    public static void applyActiveViewport(RenderTarget target) {
        ViewportArea viewport = ACTIVE_VIEWPORT.get();
        if (viewport == null) {
            return;
        }
        if (usesWindowCoordinates(target)) {
            RenderSystem.viewport(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
            applyActiveScissor(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        } else {
            int width = Math.min(viewport.glWidth(), target.viewWidth);
            int height = Math.min(viewport.glHeight(), target.viewHeight);
            RenderSystem.viewport(0, 0, width, height);
            applyActiveScissor(0, 0, width, height);
        }
    }

    private static void applyActiveScissor(int x, int y, int width, int height) {
        if (GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)) {
            GL11.glScissor(x, y, width, height);
        }
    }

    public static ViewportArea activeViewportOrNull() {
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

    public static void beginClear(RenderTarget target) {
        ViewportArea viewport = ACTIVE_VIEWPORT.get();
        Deque<ClearState> states = CLEAR_STATES.get();
        if (viewport == null) {
            states.push(ClearState.inactive());
            return;
        }

        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] scissor = new int[4];
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);
        states.push(new ClearState(true, scissorEnabled, scissor));

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        if (usesWindowCoordinates(target)) {
            GL11.glScissor(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        } else {
            GL11.glScissor(0, 0, Math.min(viewport.glWidth(), target.viewWidth), Math.min(viewport.glHeight(), target.viewHeight));
        }
    }

    public static void endClear() {
        Deque<ClearState> states = CLEAR_STATES.get();
        if (states.isEmpty()) {
            throw new IllegalStateException("RenderTarget clear scope is unbalanced");
        }
        ClearState state = states.pop();
        if (states.isEmpty()) {
            CLEAR_STATES.remove();
        }
        if (!state.active()) {
            return;
        }
        if (state.scissorEnabled()) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(state.scissor()[0], state.scissor()[1], state.scissor()[2], state.scissor()[3]);
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private record ClearState(boolean active, boolean scissorEnabled, int[] scissor) {
        private static ClearState inactive() {
            return new ClearState(false, false, new int[0]);
        }
    }

    public static boolean usesWindowCoordinates(RenderTarget target) {
        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        return target == mainTarget
            || (target.viewWidth >= mainTarget.viewWidth && target.viewHeight >= mainTarget.viewHeight);
    }

    public static boolean usesViewportLocalCoordinates(RenderTarget target) {
        return !usesWindowCoordinates(target);
    }

    public static boolean copyDepthFrom(RenderTarget target, RenderTarget source) {
        ViewportArea viewport = ACTIVE_VIEWPORT.get();
        if (viewport == null || !usesViewportLocalCoordinates(target)) {
            return false;
        }

        int previousRead = GL11.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        try {
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, source.frameBufferId);
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
            GL30C.glBlitFramebuffer(
                viewport.glX(),
                viewport.glY(),
                viewport.glX() + viewport.glWidth(),
                viewport.glY() + viewport.glHeight(),
                0,
                0,
                Math.min(viewport.glWidth(), target.viewWidth),
                Math.min(viewport.glHeight(), target.viewHeight),
                GL11.GL_DEPTH_BUFFER_BIT,
                GL11.GL_NEAREST
            );
        } finally {
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previousRead);
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousDraw);
        }
        return true;
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
