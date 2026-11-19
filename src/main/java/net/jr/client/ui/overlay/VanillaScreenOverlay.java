package net.jr.client.ui.overlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;


public interface VanillaScreenOverlay {
    default void opened(ScreenOverlayContext context) {
    }

    default void tick(ScreenOverlayContext context) {
    }

    void render(
        GuiGraphicsExtractor graphics,
        int width,
        int height,
        float partialTick
    );

    default boolean blocksScreenInput() {
        return true;
    }

    default boolean hidesCursor() {
        return this.blocksScreenInput();
    }

    default void closed(ScreenOverlayCloseReason reason) {
    }
}
