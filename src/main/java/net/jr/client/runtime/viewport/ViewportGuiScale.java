package net.jr.client.runtime.viewport;

import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;

public final class ViewportGuiScale {
    private static final ThreadLocal<Deque<Context>> CONTEXTS = ThreadLocal.withInitial(ArrayDeque::new);

    private ViewportGuiScale() {
    }

    public static Scope enter(ViewportArea viewport) {
        Context context = new Context(
            viewport,
            scale(viewport),
            logicalWidth(viewport),
            logicalHeight(viewport),
            screenScale(viewport),
            screenLogicalWidth(viewport),
            screenLogicalHeight(viewport)
        );
        CONTEXTS.get().push(context);
        return new Scope();
    }

    public static float scale(ViewportArea viewport) {
        return 1.0F;
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

    /** Scale applied only while extracting a screen. HUD rendering keeps the viewport scale. */
    public static float screenScale(ViewportArea viewport) {
        int viewportScale = Math.max(1, viewport.guiScale());
        int screenScale = usesReducedVerticalScreenScale(viewport)
            ? Math.max(1, viewportScale - 1)
            : viewportScale;
        return screenScale / (float)viewportScale;
    }

    public static int screenLogicalWidth(ViewportArea viewport) {
        return Math.max(1, (int)Math.ceil(logicalWidth(viewport) / screenScale(viewport)));
    }

    public static int screenLogicalHeight(ViewportArea viewport) {
        return Math.max(1, (int)Math.ceil(logicalHeight(viewport) / screenScale(viewport)));
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

    public static int screenLogicalMouseX(ViewportArea viewport, int localGuiX) {
        int logicalX = logicalMouseX(viewport, localGuiX);
        return logicalX == Integer.MIN_VALUE
            ? logicalX
            : (int)Math.floor(logicalX / screenScale(viewport));
    }

    public static int screenLogicalMouseY(ViewportArea viewport, int localGuiY) {
        int logicalY = logicalMouseY(viewport, localGuiY);
        return logicalY == Integer.MIN_VALUE
            ? logicalY
            : (int)Math.floor(logicalY / screenScale(viewport));
    }

    public static void beginScreen() {
        Context context = activeOrNull();
        if (context == null) {
            throw new IllegalStateException("Cannot begin screen scaling outside a viewport GUI scope");
        }
        context.screenDepth++;
    }

    public static void endScreen() {
        Context context = activeOrNull();
        if (context == null || context.screenDepth == 0) {
            throw new IllegalStateException("Screen GUI scale scope is unbalanced");
        }
        context.screenDepth--;
    }

    public static float activeScreenScale() {
        Context context = activeOrNull();
        return context != null && context.screenDepth > 0 ? context.screenScale : 1.0F;
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
        return context == null
            ? null
            : context.screenDepth > 0 ? context.screenLogicalWidth : context.logicalWidth;
    }

    @Nullable
    public static Integer activeGuiHeightOrNull() {
        Context context = activeOrNull();
        return context == null
            ? null
            : context.screenDepth > 0 ? context.screenLogicalHeight : context.logicalHeight;
    }

    private static boolean usesReducedVerticalScreenScale(ViewportArea viewport) {
        return viewport.layout() == ViewportLayout.TWO_VERTICAL;
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
        }
    }

    public static final class Context {
        private final ViewportArea viewport;
        private final float scale;
        private final int logicalWidth;
        private final int logicalHeight;
        private final float screenScale;
        private final int screenLogicalWidth;
        private final int screenLogicalHeight;
        private int screenDepth;

        private Context(
            ViewportArea viewport,
            float scale,
            int logicalWidth,
            int logicalHeight,
            float screenScale,
            int screenLogicalWidth,
            int screenLogicalHeight
        ) {
            this.viewport = viewport;
            this.scale = scale;
            this.logicalWidth = logicalWidth;
            this.logicalHeight = logicalHeight;
            this.screenScale = screenScale;
            this.screenLogicalWidth = screenLogicalWidth;
            this.screenLogicalHeight = screenLogicalHeight;
        }

        public ViewportArea viewport() {
            return this.viewport;
        }

        public float scale() {
            return this.scale;
        }

        public int logicalWidth() {
            return this.logicalWidth;
        }

        public int logicalHeight() {
            return this.logicalHeight;
        }
    }
}
