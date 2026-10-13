package net.jr.client.ui.render.composition;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.function.Consumer;
import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.jr.client.render.state.PixelArtCompositeRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix3x2f;

public final class GuiCompositionTarget implements AutoCloseable {
    private GpuTexture colorTexture;
    private GpuTextureView colorView;
    private GpuTexture depthTexture;
    private GpuTextureView depthView;
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
        Minecraft minecraft = Minecraft.getInstance();
        int safeWidth = Math.max(1, logicalWidth);
        int safeHeight = Math.max(1, logicalHeight);
        int safeScale = Math.max(1, rasterScale);
        this.ensureTextures(safeWidth * safeScale, safeHeight * safeScale);

        GuiRenderState compositionState = new GuiRenderState();
        GuiGraphicsExtractor compositionGraphics = new GuiGraphicsExtractor(
            minecraft,
            compositionState,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE
        );
        extractor.accept(compositionGraphics);

        GuiCompositionQueue.enqueue(this, compositionState, safeWidth, safeHeight, safeScale);

        Matrix3x2f pose = new Matrix3x2f(parentGraphics.pose())
            .translate(destinationX, destinationY)
            .scale(destinationScaleX, destinationScaleY);
        GuiRenderState parentState = ((GuiGraphicsExtractorBridge) parentGraphics).javaReforged$getGuiRenderState();
        parentState.addGuiElement(
            new PixelArtCompositeRenderState(
                TextureSetup.singleTexture(
                    this.colorView,
                    RenderSystem.getSamplerCache().getClampToEdge(finalFilter)
                ),
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
                0xFFFFFFFF,
                ((GuiGraphicsExtractorBridge) parentGraphics).javaReforged$currentScissor()
            )
        );
    }

    GpuTextureView colorView() {
        return this.colorView;
    }

    GpuTextureView depthView() {
        return this.depthView;
    }

    int textureWidth() {
        return this.textureWidth;
    }

    int textureHeight() {
        return this.textureHeight;
    }

    private void ensureTextures(int width, int height) {
        if (this.colorTexture != null && this.textureWidth == width && this.textureHeight == height) {
            return;
        }
        this.close();

        this.textureWidth = width;
        this.textureHeight = height;
        this.colorTexture = RenderSystem.getDevice().createTexture(
            () -> "Java Reforged UI layout color",
            13,
            GpuFormat.RGBA8_UNORM,
            width,
            height,
            1,
            1
        );
        this.colorView = RenderSystem.getDevice().createTextureView(this.colorTexture);
        this.depthTexture = RenderSystem.getDevice().createTexture(
            () -> "Java Reforged UI layout depth",
            9,
            GpuFormat.D32_FLOAT,
            width,
            height,
            1,
            1
        );
        this.depthView = RenderSystem.getDevice().createTextureView(this.depthTexture);
    }

    @Override
    public void close() {
        GuiCompositionQueue.discard(this);
        if (this.colorView != null) {
            this.colorView.close();
            this.colorView = null;
        }
        if (this.colorTexture != null) {
            this.colorTexture.close();
            this.colorTexture = null;
        }
        if (this.depthView != null) {
            this.depthView.close();
            this.depthView = null;
        }
        if (this.depthTexture != null) {
            this.depthTexture.close();
            this.depthTexture = null;
        }
        this.textureWidth = 0;
        this.textureHeight = 0;
    }
}
