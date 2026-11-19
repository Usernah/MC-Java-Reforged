package net.jr.api.client.systemui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public record SystemUiRenderContext(
    GuiGraphicsExtractor graphics,
    DeltaTracker deltaTracker,
    SystemUiTarget target,
    int canvasWidth,
    int canvasHeight
) {
}
