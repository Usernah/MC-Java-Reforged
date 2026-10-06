package net.jr.api.client.render.gui;

import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A valid local GUI viewport. A player is not required. */
public interface GuiRenderContext {
    Slots slot();

    ViewportArea viewport();

    GuiGraphicsExtractor graphics();

    DeltaTracker deltaTracker();
}
