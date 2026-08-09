package net.jr.client.components.interfaces;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Define el contrato para cualquier objeto que pueda renderizar un widget.
 * Esto permite desacoplar la lógica del widget de su apariencia visual.
 */
public interface IWidgetRenderer {
    /**
     * Dibuja la apariencia del widget.
     * @param gui El contexto de GuiGraphicsExtractor para dibujar.
     * @param widget El widget que se está renderizando, para poder consultar su estado (isHovered, isPressed, etc.).
     */
    void render(GuiGraphicsExtractor gui, AbstractWidget widget, int mouseX, int mouseY, float partialTick);
}
