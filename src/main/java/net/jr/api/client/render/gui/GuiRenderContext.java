package net.jr.api.client.render.gui;

import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;


public interface GuiRenderContext {
    Slots slot();

    ViewportArea viewport();

    GuiGraphicsExtractor graphics();

    DeltaTracker deltaTracker();
}
