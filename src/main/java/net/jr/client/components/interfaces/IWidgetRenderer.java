package net.jr.client.components.interfaces;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;

public interface IWidgetRenderer {

    void render(GuiGraphicsExtractor gui, AbstractWidget widget, int mouseX, int mouseY, float partialTick);
}
