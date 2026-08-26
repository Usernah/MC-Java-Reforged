package net.jr.client.runtime.viewport;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ViewportHudLayout {
    private static final float BOTTOM_SCALE = 1.0F;
    private static final float BOTTOM_Y_OFFSET = -22.0F;
    private static final float BOTTOM_PIVOT_Y_OFFSET = 1.0F;

    private ViewportHudLayout() {
    }

    public static void pushBottomTransform(GuiGraphicsExtractor graphics) {
        float pivotX = graphics.guiWidth() * 0.5F;
        float pivotY = graphics.guiHeight() + BOTTOM_PIVOT_Y_OFFSET;
        graphics.pose().pushMatrix();
        graphics.pose().translate(pivotX, pivotY + BOTTOM_Y_OFFSET);
        graphics.pose().scale(BOTTOM_SCALE);
        graphics.pose().translate(-pivotX, -pivotY);
    }

    public static void popBottomTransform(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    public static int hotbarSelectionHeight() {
        return 24;
    }
}
