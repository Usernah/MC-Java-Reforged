package net.jr.screens.test;

import net.jr.api.client.ui.DslScreen;
import net.jr.api.client.ui.UiScreenFile;
import net.jr.client.components.widgets.Button;
import net.jr.registry.ModUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Small concrete screen used to exercise the Java-owned DSL widget flow. */
public final class DslTestScreen extends Screen implements DslScreen {
    private int clickCount;

    public DslTestScreen() {
        super(Component.literal("Java Reforged DSL Test"));
    }

    @Override
    public UiScreenFile dslFile() {
        return ModUi.TEST_SCREEN;
    }

    @Override
    protected void init() {
        this.dslData().exposeText("test.status", () -> "DSL clicks: " + this.clickCount);

        new Button(this.dsl("steam_y"))
            .onPress(button -> this.clickCount++);

        new Button(this.dsl("steam_a"))
            .onPress(button -> this.clickCount = 0);

        new Button(this.dsl("steam_b"))
            .onPress(button -> this.onClose());
    }

    @Override
    public void extractBackground(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        this.extractDslBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        this.extractDslContent(graphics, mouseX, mouseY, partialTick);
        this.extractDslForeground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
