package net.jr.client.ui.render.composition;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.function.Consumer;
import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.jr.client.render.state.PixelArtCompositeRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix3x2f;

public final class GuiCompositionTarget implements AutoCloseable {
    private final GuiRenderState compositionState = new GuiRenderState();
    private GuiCompositionRenderer compositionRenderer;
    private TextureTarget renderTarget;
    private int textureWidth;
    private int textureHeight;

    public void extractCompositionAndSubmit(
        GuiGraphicsExtractor parentGraphics,
        int logicalWidth,
        int logicalHeight,
        int rasterScale,
        float destinationX,
        float destinationY,
        float destinationScaleX,
        float destinationScaleY,
        FilterMode finalFilter,
        float pixelBias,
        float sampleFootprintX,
        float sampleFootprintY,
        Consumer<GuiGraphicsExtractor> extractor
    ) {
        this.extractCompositionAndSubmit(
            parentGraphics,
            logicalWidth,
            logicalHeight,
            rasterScale,
            destinationX,
            destinationY,
            destinationScaleX,
            destinationScaleY,
            finalFilter,
            pixelBias,
            sampleFootprintX,
            sampleFootprintY,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            extractor
        );
    }

    public void extractCompositionAndSubmit(
        GuiGraphicsExtractor parentGraphics,
        int logicalWidth,
        int logicalHeight,
        int rasterScale,
        float destinationX,
        float destinationY,
        float destinationScaleX,
        float destinationScaleY,
        FilterMode finalFilter,
        float pixelBias,
        float sampleFootprintX,
        float sampleFootprintY,
        int mouseX,
        int mouseY,
        Consumer<GuiGraphicsExtractor> extractor
    ) {
        this.extractCompositionAndSubmit(
            parentGraphics,
            logicalWidth,
            logicalHeight,
            rasterScale,
            destinationX,
            destinationY,
            destinationScaleX,
            destinationScaleY,
            finalFilter,
            pixelBias,
            sampleFootprintX,
            sampleFootprintY,
            mouseX,
            mouseY,
            0xFFFFFFFF,
            extractor
        );
    }

    public void extractCompositionAndSubmit(
        GuiGraphicsExtractor parentGraphics,
        int logicalWidth,
        int logicalHeight,
        int rasterScale,
        float destinationX,
        float destinationY,
        float destinationScaleX,
        float destinationScaleY,
        FilterMode finalFilter,
        float pixelBias,
        float sampleFootprintX,
        float sampleFootprintY,
        int mouseX,
        int mouseY,
        int compositeColor,
        Consumer<GuiGraphicsExtractor> extractor
    ) {
        this.extractCompositionAndSubmit(
            parentGraphics,
            logicalWidth,
            logicalHeight,
            rasterScale,
            destinationX,
            destinationY,
            destinationScaleX,
            destinationScaleY,
            finalFilter,
            pixelBias,
            sampleFootprintX,
            sampleFootprintY,
            mouseX,
            mouseY,
            compositeColor,
            true,
            extractor
        );
    }

    public void extractCompositionAndSubmitBlit(
        GuiGraphicsExtractor parentGraphics,
        int logicalWidth,
        int logicalHeight,
        int rasterScale,
        float destinationX,
        float destinationY,
        float destinationScaleX,
        float destinationScaleY,
        FilterMode finalFilter,
        int mouseX,
        int mouseY,
        int compositeColor,
        Consumer<GuiGraphicsExtractor> extractor
    ) {
        this.extractCompositionAndSubmit(
            parentGraphics,
            logicalWidth,
            logicalHeight,
            rasterScale,
            destinationX,
            destinationY,
            destinationScaleX,
            destinationScaleY,
            finalFilter,
            0.0F,
            1.0F,
            1.0F,
            mouseX,
            mouseY,
            compositeColor,
            false,
            extractor
        );
    }

    private void extractCompositionAndSubmit(
        GuiGraphicsExtractor parentGraphics,
        int logicalWidth,
        int logicalHeight,
        int rasterScale,
        float destinationX,
        float destinationY,
        float destinationScaleX,
        float destinationScaleY,
        FilterMode finalFilter,
        float pixelBias,
        float sampleFootprintX,
        float sampleFootprintY,
        int mouseX,
        int mouseY,
        int compositeColor,
        boolean pixelArtFilter,
        Consumer<GuiGraphicsExtractor> extractor
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        int safeWidth = Math.max(1, logicalWidth);
        int safeHeight = Math.max(1, logicalHeight);
        int safeScale = Math.max(1, rasterScale);
        this.ensureTextures(safeWidth * safeScale, safeHeight * safeScale);

        try (GuiCompositionScope.Scope ignored =
                 GuiCompositionScope.enterExtraction(safeWidth, safeHeight, safeScale)) {
            GuiGraphicsExtractor compositionGraphics = new GuiGraphicsExtractor(
                minecraft,
                this.compositionState,
                mouseX,
                mouseY
            );
            extractor.accept(compositionGraphics);
        }

        GuiCompositionQueue.enqueue(this, safeWidth, safeHeight, safeScale);

        Matrix3x2f pose = new Matrix3x2f(parentGraphics.pose())
            .translate(destinationX, destinationY)
            .scale(destinationScaleX, destinationScaleY);
        GuiRenderState parentState = ((GuiGraphicsExtractorBridge) parentGraphics).javaReforged$getGuiRenderState();
        TextureSetup textureSetup = TextureSetup.singleTexture(
            this.colorView(),
            RenderSystem.getSamplerCache().getClampToEdge(finalFilter)
        );
        if (pixelArtFilter) {
            parentState.addGuiElement(new PixelArtCompositeRenderState(
                textureSetup,
                pose,
                0,
                0,
                safeWidth,
                safeHeight,
                0.0F,
                1.0F,
                1.0F,
                0.0F,
                Math.clamp(pixelBias, 0.0F, 1.0F),
                sampleFootprintX,
                sampleFootprintY,
                compositeColor,
                ((GuiGraphicsExtractorBridge) parentGraphics).javaReforged$currentScissor()
            ));
        } else {
            parentState.addGuiElement(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                textureSetup,
                pose,
                0,
                0,
                safeWidth,
                safeHeight,
                0.0F,
                1.0F,
                1.0F,
                0.0F,
                compositeColor,
                ((GuiGraphicsExtractorBridge) parentGraphics).javaReforged$currentScissor()
            ));
        }
    }

    GpuTextureView colorView() {
        return this.renderTarget.getColorTextureView();
    }

    GpuTextureView depthView() {
        return this.renderTarget.getDepthTextureView();
    }

    int textureWidth() {
        return this.textureWidth;
    }

    int textureHeight() {
        return this.textureHeight;
    }

    private void ensureTextures(int width, int height) {
        if (this.renderTarget != null && this.textureWidth == width && this.textureHeight == height) {
            return;
        }
        this.textureWidth = width;
        this.textureHeight = height;
        if (this.renderTarget == null) {
            this.renderTarget = new TextureTarget(
                "Java Reforged UI composition",
                width,
                height,
                true,
                GpuFormat.RGBA8_UNORM
            );
            this.compositionRenderer = new GuiCompositionRenderer(this.compositionState);
        } else {
            this.renderTarget.resize(width, height);
        }
    }

    void renderQueued(int guiWidth, int guiHeight, int guiScale) {
        if (this.renderTarget == null || this.compositionRenderer == null) {
            this.compositionState.reset();
            return;
        }
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            this.renderTarget.getColorTexture(),
            GuiRenderer.CLEAR_COLOR,
            this.renderTarget.getDepthTexture(),
            0.0D
        );
        try (GuiCompositionScope.Scope ignored = GuiCompositionScope.enterRendering(
            this.renderTarget,
            this.textureWidth,
            this.textureHeight,
            guiScale
        )) {
            this.compositionRenderer.render();
        }
    }

    void discardQueuedState() {
        this.compositionState.reset();
    }

    @Override
    public void close() {
        GuiCompositionQueue.discard(this);
        if (this.compositionRenderer != null) {
            this.compositionRenderer.close();
            this.compositionRenderer = null;
        }
        if (this.renderTarget != null) {
            this.renderTarget.destroyBuffers();
            this.renderTarget = null;
        }
        this.textureWidth = 0;
        this.textureHeight = 0;
    }
}
