package net.jr.api.client.render.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Full-window GUI coordinates, independent of a local slot. */
public interface GlobalRenderContext {
    GuiGraphicsExtractor graphics();

    DeltaTracker deltaTracker();

    int guiWidth();

    int guiHeight();
}
