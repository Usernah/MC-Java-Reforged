package net.alnv.javareforged.ClientRuntime.runtime;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportArea;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportLayout;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Matrix4f;

public final class ScreenScale {
    private static final float SPLIT_SCREEN_SCALE = 1.5F;
    private static final ThreadLocal<Deque<Context>> CONTEXTS = ThreadLocal.withInitial(ArrayDeque::new);

    private ScreenScale() {
    }

    public static Scope enter(ViewportArea viewport) {
        Context context = new Context(viewport, scale(viewport), logicalWidth(viewport), logicalHeight(viewport));
        CONTEXTS.get().push(context);

        RenderSystem.backupProjectionMatrix();
        float farPlane = ClientHooks.getGuiFarPlane();
        Matrix4f projection = new Matrix4f().setOrtho(
                0.0F,
                (float)context.logicalWidth(),
                (float)context.logicalHeight(),
                0.0F,
                1000.0F,
                farPlane
        );
        RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);

        return new Scope();
    }

    public static float scale(ViewportArea viewport) {
        return viewport.layout() == ViewportLayout.FOUR_GRID ? SPLIT_SCREEN_SCALE : 1.0F;
    }

    public static double effectiveGuiScale(ViewportArea viewport) {
        return viewport.effectiveGuiScale() / scale(viewport);
    }

    public static int logicalWidth(ViewportArea viewport) {
        return Math.max(1, (int)Math.ceil(viewport.glWidth() / effectiveGuiScale(viewport)));
    }

    public static int logicalHeight(ViewportArea viewport) {
        return Math.max(1, (int)Math.ceil(viewport.glHeight() / effectiveGuiScale(viewport)));
    }

    public static int logicalMouseX(ViewportArea viewport, int localGuiX) {
        if (localGuiX == Integer.MIN_VALUE) {
            return localGuiX;
        }
        return (int)Math.floor(localGuiX * logicalWidth(viewport) / (double)viewport.guiWidth());
    }

    public static int logicalMouseY(ViewportArea viewport, int localGuiY) {
        if (localGuiY == Integer.MIN_VALUE) {
            return localGuiY;
        }
        return (int)Math.floor(localGuiY * logicalHeight(viewport) / (double)viewport.guiHeight());
    }

    @Nullable
    public static Context activeOrNull() {
        Deque<Context> contexts = CONTEXTS.get();
        return contexts.isEmpty() ? null : contexts.peek();
    }

    public static boolean isActive() {
        return activeOrNull() != null;
    }

    @Nullable
    public static Integer activeGuiWidthOrNull() {
        Context context = activeOrNull();
        return context == null ? null : context.logicalWidth();
    }

    @Nullable
    public static Integer activeGuiHeightOrNull() {
        Context context = activeOrNull();
        return context == null ? null : context.logicalHeight();
    }

    public static final class Scope implements AutoCloseable {
        private boolean closed;

        private Scope() {
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;

            Deque<Context> contexts = CONTEXTS.get();
            if (!contexts.isEmpty()) {
                contexts.pop();
            }
            if (contexts.isEmpty()) {
                CONTEXTS.remove();
            }
            RenderSystem.restoreProjectionMatrix();
        }
    }

    public record Context(ViewportArea viewport, float scale, int logicalWidth, int logicalHeight) {
    }
}
