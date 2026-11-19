package net.jr.client.render.item;

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

public final class GuiItemFrameState {
    private int itemColor = -1;
    private GuiItemAtlasSet atlases;

    public int itemColor() {
        return this.itemColor;
    }

    public void setItemColor(int itemColor) {
        this.itemColor = itemColor;
    }

    public GuiItemAtlasSet atlases(FeatureRenderDispatcher featureRenderDispatcher) {
        if (this.atlases == null) {
            this.atlases = new GuiItemAtlasSet(featureRenderDispatcher);
        }
        return this.atlases;
    }

    public GuiItemAtlasSet atlases() {
        if (this.atlases == null) {
            throw new IllegalStateException("Item atlases have not been initialized");
        }
        return this.atlases;
    }

    public void close() {
        if (this.atlases != null) {
            this.atlases.close();
            this.atlases = null;
        }
    }
}
