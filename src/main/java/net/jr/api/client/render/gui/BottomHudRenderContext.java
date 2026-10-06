package net.jr.api.client.render.gui;

import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A local HUD viewport whose available height excludes reserved bottom content. */
public record BottomHudRenderContext(
    Slots slot,
    ViewportArea viewport,
    GuiGraphicsExtractor graphics,
    DeltaTracker deltaTracker,
    int availableHeight
) implements HudRenderContext {
    public BottomHudRenderContext {
        if (availableHeight < 0 || availableHeight > viewport.guiHeight()) {
            throw new IllegalArgumentException("availableHeight is outside the viewport");
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
