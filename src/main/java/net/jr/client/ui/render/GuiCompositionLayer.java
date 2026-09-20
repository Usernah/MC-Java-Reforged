package net.jr.client.ui.render;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.function.Consumer;
import net.jr.client.ui.layout.render.UILayoutRenderTarget;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Renders a fixed canvas at a fixed presentation scale. */
public final class GuiCompositionLayer implements AutoCloseable {
    private final UILayoutRenderTarget target = new UILayoutRenderTarget();
    private final int canvasWidth;
    private final int canvasHeight;
    private final float presentationScale;

    public GuiCompositionLayer(int canvasWidth, int canvasHeight, float presentationScale) {
        if (canvasWidth <= 0 || canvasHeight <= 0) {
            throw new IllegalArgumentException("Canvas size must be positive");
        }
        if (!Float.isFinite(presentationScale) || presentationScale <= 0.0F) {
            throw new IllegalArgumentException("Presentation scale must be finite and positive");
        }
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.presentationScale = presentationScale;
    }

    public void submit(
        GuiGraphicsExtractor parentGraphics,
        FilterMode filter,
        float pixelBias,
        Consumer<Canvas> extractor
    ) {
        float destinationWidth = this.canvasWidth * this.presentationScale;
        float destinationHeight = this.canvasHeight * this.presentationScale;
        float destinationX = (parentGraphics.guiWidth() - destinationWidth) / 2.0F;
        float destinationY = (parentGraphics.guiHeight() - destinationHeight) / 2.0F;
        this.target.extractCompositionAndSubmit(
            parentGraphics,
            this.canvasWidth,
            this.canvasHeight,
            1,
            destinationX,
            destinationY,
            this.presentationScale,
            this.presentationScale,
            filter,
            pixelBias,
            canvasGraphics -> extractor.accept(new Canvas(
                canvasGraphics,
                destinationX,
                destinationY,
                this.presentationScale
            ))
        );
    }

    public static final class Canvas {
        private final GuiGraphicsExtractor graphics;
        private final float destinationX;
        private final float destinationY;
        private final float presentationScale;

        private Canvas(
            GuiGraphicsExtractor graphics,
            float destinationX,
            float destinationY,
            float presentationScale
        ) {
            this.graphics = graphics;
            this.destinationX = destinationX;
            this.destinationY = destinationY;
            this.presentationScale = presentationScale;
        }

        public GuiGraphicsExtractor graphics() {
            return this.graphics;
        }

        public float toCanvasX(float viewportX) {
            return (viewportX - this.destinationX) / this.presentationScale;
        }

        public float toCanvasY(float viewportY) {
            return (viewportY - this.destinationY) / this.presentationScale;
        }
    }

    @Override
    public void close() {
        this.target.close();
    }
}
