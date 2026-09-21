package net.jr.client.ui.render;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.function.Consumer;
import net.jr.client.ui.layout.render.UILayoutRenderTarget;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Renders a fixed canvas into a GUI viewport. */
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
        this.submitAt(parentGraphics, filter, pixelBias, extractor,
            destinationX, destinationY, this.presentationScale, this.presentationScale);
    }

    /** Scales the canvas uniformly to the GUI height and anchors its top-right corner. */
    public void submitTopRight(
        GuiGraphicsExtractor parentGraphics,
        FilterMode filter,
        float pixelBias,
        Consumer<Canvas> extractor
    ) {
        float scale = parentGraphics.guiHeight() / (float)this.canvasHeight;
        if (scale <= 0.0F) {
            return;
        }
        float destinationX = parentGraphics.guiWidth() - this.canvasWidth * scale;
        this.submitAt(parentGraphics, filter, pixelBias, extractor, destinationX, 0.0F, scale, scale);
    }

    private void submitAt(
        GuiGraphicsExtractor parentGraphics,
        FilterMode filter,
        float pixelBias,
        Consumer<Canvas> extractor,
        float destinationX,
        float destinationY,
        float scaleX,
        float scaleY
    ) {
        this.target.extractCompositionAndSubmit(
            parentGraphics,
            this.canvasWidth,
            this.canvasHeight,
            1,
            destinationX,
            destinationY,
            scaleX,
            scaleY,
            filter,
            pixelBias,
            canvasGraphics -> extractor.accept(new Canvas(
                canvasGraphics,
                destinationX,
                destinationY,
                scaleX,
                scaleY
            ))
        );
    }

    public static final class Canvas {
        private final GuiGraphicsExtractor graphics;
        private final float destinationX;
        private final float destinationY;
        private final float scaleX;
        private final float scaleY;

        private Canvas(
            GuiGraphicsExtractor graphics,
            float destinationX,
            float destinationY,
            float scaleX,
            float scaleY
        ) {
            this.graphics = graphics;
            this.destinationX = destinationX;
            this.destinationY = destinationY;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }

        public GuiGraphicsExtractor graphics() {
            return this.graphics;
        }

        public float toCanvasX(float viewportX) {
            return (viewportX - this.destinationX) / this.scaleX;
        }

        public float toCanvasY(float viewportY) {
            return (viewportY - this.destinationY) / this.scaleY;
        }
    }

    @Override
    public void close() {
        this.target.close();
    }
}
