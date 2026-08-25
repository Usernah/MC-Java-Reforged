package net.jr.client.ui.layout;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class UILayoutScreen extends Screen {
    private final UILayoutRuntime layoutRuntime;
    private UILayout interactiveLayout;

    protected UILayoutScreen(Component title, UILayout initialLayout) {
        super(title);
        this.layoutRuntime = new UILayoutRuntime(
            this,
            () -> this.width,
            () -> this.height,
            this::setInteractiveLayout
        );
        this.layoutRuntime.setInitialLayout(initialLayout);
    }

    public final UILayoutRuntime layoutRuntime() {
        return this.layoutRuntime;
    }

    @Override
    protected void init() {
        this.layoutRuntime.resize();
        this.setInteractiveLayout(this.layoutRuntime.interactiveLayout());
    }

    @Override
    public void tick() {
        this.layoutRuntime.tick();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.layoutRuntime.extractRenderState(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {

    }

    @Override
    public void onClose() {
        if (this.layoutRuntime.activeOverlay() != null) {
            this.layoutRuntime.closeOverlay();
            return;
        }
        super.onClose();
    }

    @Override
    public void removed() {
        this.layoutRuntime.close();
        super.removed();
    }

    private void setInteractiveLayout(@Nullable UILayout layout) {
        boolean attachedToScreen = layout != null && this.children().contains(layout);
        if (this.interactiveLayout == layout && (layout == null || attachedToScreen)) {
            return;
        }
        if (this.interactiveLayout != null) {
            this.interactiveLayout.cancelInteractions();
            this.removeWidget(this.interactiveLayout);
        }
        this.interactiveLayout = layout;
        if (layout != null) {

            this.addWidget(layout);
        }
    }
}
