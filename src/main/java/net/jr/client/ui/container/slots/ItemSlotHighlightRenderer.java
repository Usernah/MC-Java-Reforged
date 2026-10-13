package net.jr.client.ui.container.slots;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ItemSlotHighlightRenderer {
    private static final int OVERLAY_COLOR = 0x4DFFFFFF;
    private static final int BORDER_COLOR = 0xFFFFFFFF;

    private ItemSlotHighlightRenderer() {
    }

    public static void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int right = x + width;
        int bottom = y + height;
        graphics.nextStratum();
        graphics.fill(x, y, right, bottom, OVERLAY_COLOR);
        graphics.fill(x, y, right, y + 1, BORDER_COLOR);
        graphics.fill(x, bottom - 1, right, bottom, BORDER_COLOR);
        graphics.fill(x, y + 1, x + 1, bottom - 1, BORDER_COLOR);
        graphics.fill(right - 1, y + 1, right, bottom - 1, BORDER_COLOR);
    }
}
