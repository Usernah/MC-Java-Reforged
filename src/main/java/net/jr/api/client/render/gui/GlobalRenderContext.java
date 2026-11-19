package net.jr.api.client.render.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;


public interface GlobalRenderContext {
    GuiGraphicsExtractor graphics();

    DeltaTracker deltaTracker();

    int guiWidth();

    int guiHeight();
}
