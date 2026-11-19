package net.jr.api.client.ui;

import net.jr.client.ui.dsl.DslNode;
import net.jr.client.ui.dsl.UiDataContext;
import net.jr.client.ui.dsl.UiRenderLayer;
import net.jr.client.ui.screen.DslScreenBridge;
import net.jr.client.ui.screen.DslScreenContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public interface DslScreen extends FramebufferScreen {
    UiScreenFile dslFile();

    default DslScreenContext dslContext() {
        if (!(this instanceof Screen screen) || screen instanceof AbstractContainerScreen<?>) {
            throw new IllegalStateException(
                "DslScreen is only for regular Minecraft screens; containers must implement DslContainer"
            );
        }
        return ((DslScreenBridge)screen).javaReforged$dslContext(this.dslFile());
    }

    default DslNode dsl(String id) {
        return this.dslContext().element(id);
    }

    default UiDataContext dslData() {
        return this.dslContext().data();
    }

    default void extractDslBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.dslContext().extract(UiRenderLayer.BACKGROUND, graphics, mouseX, mouseY, partialTick);
    }

    default void extractDslContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.dslContext().extract(UiRenderLayer.CONTENT, graphics, mouseX, mouseY, partialTick);
    }

    default void extractDslForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.dslContext().extract(UiRenderLayer.FOREGROUND, graphics, mouseX, mouseY, partialTick);
    }
}
