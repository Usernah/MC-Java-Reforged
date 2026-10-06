package net.jr.client.runtime.viewport;

import net.jr.api.client.split.SplitScreen;
import net.jr.api.client.ui.Viewport;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.bytedeco.opencv.opencv_dnn.SplitLayer;

public final class ViewportHudLayout {
    private static final float BOTTOM_SCALE_BIG = 1.5F;
    private static final float BOTTOM_SCALE = 1.0F;
    private static final float BOTTOM_PIVOT_Y_OFFSET = 1.0F;
    private static final ThreadLocal<Float> ACTIVE_BOTTOM_INSET =
        ThreadLocal.withInitial(() -> 0.0F);

    private ViewportHudLayout() {
    }

    private static float hudScale() {
        return Viewport.current().layout() == ViewportLayout.TWO_VERTICAL
                ? BOTTOM_SCALE
                : BOTTOM_SCALE_BIG;
    }

    public static void pushBottomTransform(GuiGraphicsExtractor graphics) {
        float pivotX = graphics.guiWidth() * 0.5F;
        float pivotY = graphics.guiHeight() + BOTTOM_PIVOT_Y_OFFSET;
        float offset = -activeBottomInset();
        graphics.pose().pushMatrix();
        graphics.pose().translate(pivotX, pivotY + offset);
        graphics.pose().scale(hudScale());
        graphics.pose().translate(-pivotX, -pivotY);
    }

    public static void popBottomTransform(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    public static void pushBottomScaleTransform(
            GuiGraphicsExtractor graphics,
            float bottomY
    ) {
        float pivotX = graphics.guiWidth() * 0.5F;
        float pivotY = bottomY + BOTTOM_PIVOT_Y_OFFSET;
        graphics.pose().pushMatrix();
        graphics.pose().translate(pivotX, pivotY);
        graphics.pose().scale(hudScale());
        graphics.pose().translate(-pivotX, -pivotY);
    }

    public static int hotbarSelectionHeight() {
        return 24;
    }

    public static BottomInsetScope pushBottomInset(float bottomInset) {
        float previousInset = activeBottomInset();
        float safeInset = Float.isFinite(bottomInset)
            ? Math.max(0.0F, bottomInset)
            : 0.0F;
        ACTIVE_BOTTOM_INSET.set(safeInset);
        return new BottomInsetScope(previousInset);
    }

    public static int bottomAnchorY(int viewportHeight) {
        return Math.round(viewportHeight - activeBottomInset());
    }

    private static float activeBottomInset() {
        return ACTIVE_BOTTOM_INSET.get();
    }

    public static final class BottomInsetScope implements AutoCloseable {
        private final float previousInset;
        private boolean closed;

        private BottomInsetScope(float previousInset) {
            this.previousInset = previousInset;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            if (this.previousInset == 0.0F) {
                ACTIVE_BOTTOM_INSET.remove();
            } else {
                ACTIVE_BOTTOM_INSET.set(this.previousInset);
            }
            this.closed = true;
        }
    }
}
