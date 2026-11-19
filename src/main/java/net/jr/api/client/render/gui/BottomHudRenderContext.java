package net.jr.api.client.render.gui;

import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;


public record BottomHudRenderContext(
    Slots slot,
    ViewportArea viewport,
    GuiGraphicsExtractor graphics,
    DeltaTracker deltaTracker,
    int availableHeight
) implements HudRenderContext {
    public BottomHudRenderContext {
        if (availableHeight < 0 || availableHeight > graphics.guiHeight()) {
            throw new IllegalArgumentException("availableHeight is outside the active GUI space");
        }
    }

    public static BottomHudRenderContext from(
        HudRenderContext context,
        int availableHeight
    ) {
        return new BottomHudRenderContext(
            context.slot(),
            context.viewport(),
            context.graphics(),
            context.deltaTracker(),
            availableHeight
        );
    }
}
