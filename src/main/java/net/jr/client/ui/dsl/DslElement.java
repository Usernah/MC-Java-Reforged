package net.jr.client.ui.dsl;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Runtime element constructed and owned by Java from one declared DSL node. */
public interface DslElement {
    void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    );

    default void closeDsl() {
    }
}
