package net.jr.client.components.widgets;

import net.jr.client.components.interfaces.IWidgetRenderer;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class TabElement extends JavaBaseWidget {

    public enum PositionRole {
        SINGLE,
        FIRST,
        MIDDLE,
        LAST
    }

    private boolean selected;
    private PositionRole positionRole =
            PositionRole.SINGLE;

    private IWidgetRenderer renderer;

    private Consumer<TabElement> selectionRequest =
            ignored -> {};

    public TabElement(
            int x,
            int y,
            int width,
            int height
    ) {
        super(x, y, width, height, Component.empty());
    }

    public boolean isSelected() {
        return this.selected;
    }

    void setSelected(boolean selected) {
        this.selected = selected;
    }

    public PositionRole positionRole() {
        return this.positionRole;
    }

    void setPositionRole(PositionRole role) {
        this.positionRole =
                role == null
                        ? PositionRole.SINGLE
                        : role;
    }

    public void setRenderer(IWidgetRenderer renderer) {
        this.renderer = renderer;
    }

    void setSelectionRequest(
            Consumer<TabElement> action
    ) {
        this.selectionRequest =
                action == null
                        ? ignored -> {}
                        : action;
    }

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractWidgetRenderState(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );

        if (this.renderer != null) {
            this.renderer.render(
                    guiGraphics,
                    this,
                    mouseX,
                    mouseY,
                    partialTick
            );
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.active) {
            this.selectionRequest.accept(this);
        }
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return false;
    }

    @Override
    public ComponentPath nextFocusPath(
            FocusNavigationEvent event
    ) {
        return null;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(false);
    }

    public static final class Builder {

        private final TabElement instance;

        public Builder(
                int x,
                int y,
                int width,
                int height
        ) {
            this.instance =
                    new TabElement(x, y, width, height);
        }

        public Builder withRenderer(
                IWidgetRenderer renderer
        ) {
            this.instance.setRenderer(renderer);
            return this;
        }

        public TabElement build() {
            return this.instance;
        }
    }
}
