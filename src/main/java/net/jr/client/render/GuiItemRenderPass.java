package net.jr.client.render;

import net.jr.mixin.accessors.GuiRendererAccessor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;

public final class GuiItemRenderPass {
    private GuiItemRenderPass() {
    }

    public static GuiItemRenderState capture(GuiItemRenderState state) {
        return ItemRenderResolution.capture(ItemRenderOpacity.capture(state));
    }

    public static void prepareFrame(GuiRenderer renderer, GuiItemFrameState frameState) {
        GuiRendererAccessor accessor = (GuiRendererAccessor) renderer;
        ItemRenderResolution.prepareFrame(accessor.javaReforged$getRenderState());
        int requestedSize = ItemRenderResolution.preparedItemSize();
        if (requestedSize > frameState.largestAtlasItemSize()) {
            accessor.javaReforged$invalidateItemAtlas();
            frameState.setLargestAtlasItemSize(requestedSize);
        }
    }

    public static int atlasSlotSize(int vanillaSlotSize) {
        return ItemRenderResolution.atlasSlotSize(vanillaSlotSize);
    }

    public static void readOpacity(GuiItemRenderState itemState, GuiItemFrameState frameState) {
        frameState.setItemColor(ItemRenderOpacity.consumeColor(itemState));
    }

    public static void clearFrame() {
        ItemRenderResolution.clearFrame();
    }
}
