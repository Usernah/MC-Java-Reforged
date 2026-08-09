package net.jr.screens.test;

import net.jr.api.client.ui.dsl.UiRenderLayer;
import net.jr.client.ui.UiElement;
import net.jr.client.ui.UiWidget;
import net.jr.client.ui.layout.UILayout;
import net.jr.registry.ModUi;

final class TestLayout extends UILayout {
    private int clickCount;

    TestLayout() {
        super(ModUi.TEST_SCREEN);
        this.fade(350);
    }

    @Override
    protected void initLayout() {
        new UiElement.Layer(UiRenderLayer.BACKGROUND, this);
        new UiElement.Layer(UiRenderLayer.CONTENT, this);
        new UiElement.Layer(UiRenderLayer.FOREGROUND, this);
        new UiWidget.Button("steam_y", this, button -> this.clickCount++);
        new UiWidget.Button("steam_a", this, button -> this.clickCount++);
        new UiWidget.Button("steam_b", this, button -> this.clickCount++);
    }

    int clickCount() {
        return this.clickCount;
    }
}
