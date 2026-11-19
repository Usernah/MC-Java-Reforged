package net.jr.client.ui.render.composition;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.GuiBannerResultRenderer;
import net.minecraft.client.gui.render.pip.GuiBookModelRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer;
import net.minecraft.client.gui.render.pip.GuiSkinRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiBannerResultRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiBookModelRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiProfilerChartRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiSkinRenderState;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;


final class GuiCompositionRenderer implements AutoCloseable {
    private final GuiRenderer renderer;

    GuiCompositionRenderer(GuiRenderState state) {
        Minecraft minecraft = Minecraft.getInstance();
        this.renderer = new GuiRenderer(
            state,
            minecraft.gameRenderer.featureRenderDispatcher(),
            ClientHooks.gatherPictureInPictureRenderers(List.of(
                new PictureInPictureRendererRegistration<>(
                    GuiEntityRenderState.class,
                    () -> new GuiEntityRenderer(minecraft.getEntityRenderDispatcher())
                ),
                new PictureInPictureRendererRegistration<>(
                    GuiSkinRenderState.class,
                    GuiSkinRenderer::new
                ),
                new PictureInPictureRendererRegistration<>(
                    GuiBookModelRenderState.class,
                    GuiBookModelRenderer::new
                ),
                new PictureInPictureRendererRegistration<>(
                    GuiBannerResultRenderState.class,
                    () -> new GuiBannerResultRenderer(minecraft.getAtlasManager())
                ),
                new PictureInPictureRendererRegistration<>(
                    GuiProfilerChartRenderState.class,
                    GuiProfilerChartRenderer::new
                )
            ))
        );
    }

    void render() {
        this.renderer.render();
        this.renderer.endFrame();
    }

    @Override
    public void close() {
        this.renderer.close();
    }
}
