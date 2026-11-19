package net.jr.client.ui.dsl;

import net.minecraft.client.gui.GuiGraphicsExtractor;


public interface DslElement {
    void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    );

    default void closeDsl() {
    }

    default Object dslValue(String property) {
        throw new IllegalArgumentException(
            "DSL element does not expose '" + property + "'"
        );
    }
}
