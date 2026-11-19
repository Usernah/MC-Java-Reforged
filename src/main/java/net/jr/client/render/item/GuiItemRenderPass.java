package net.jr.client.render.item;

import net.jr.mixin.accessors.GuiRendererAccessor;
import net.jr.client.ui.render.composition.GuiCompositionScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiItemAtlas;
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
        frameState.atlases(accessor.javaReforged$getFeatureRenderDispatcher()).prepare(
            accessor.javaReforged$getRenderState(),
            guiScale()
        );
    }

    public static GuiItemAtlas.SlotView getOrUpdate(
        GuiItemFrameState frameState,
        GuiItemRenderState state
    ) {
        return frameState.atlases().getOrUpdate(state);
    }

    public static GuiItemAtlas representative(
        GuiItemFrameState frameState,
        int vanillaPixelSize
    ) {
        return frameState.atlases().representative(vanillaPixelSize);
    }

    public static void readOpacity(GuiItemRenderState itemState, GuiItemFrameState frameState) {
        frameState.setItemColor(ItemRenderOpacity.consumeColor(itemState));
    }

    public static void clearFrame(GuiItemFrameState frameState) {
        frameState.atlases().endFrame();
        ItemRenderOpacity.clearFrame();
    }

    public static void close(GuiItemFrameState frameState) {
        frameState.close();
    }

    private static int guiScale() {
        GuiCompositionScope.Rendering rendering = GuiCompositionScope.activeRenderingOrNull();
        if (rendering != null) {
            return Math.max(1, rendering.guiScale());
        }
        return Math.max(
            1,
            Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.guiScale
        );
    }
}
