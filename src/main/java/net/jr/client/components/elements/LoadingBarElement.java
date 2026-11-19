package net.jr.client.components.elements;

import java.util.Objects;
import java.util.function.Consumer;
import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class LoadingBarElement implements VisualElementsInterface {
    private final VisualState visualState;
    private final Consumer<GuiGraphicsExtractor> background;
    private final Consumer<GuiGraphicsExtractor> fill;
    private GuiGraphicsExtractor guiGraphics;
    private float fillX;
    private float fillY;
    private float fillWidth;
    private float fillHeight;
    private float progress;

    public LoadingBarElement(
        float x,
        float y,
        float backgroundWidth,
        float backgroundHeight,
        GuiGraphicsExtractor guiGraphics,
        Consumer<GuiGraphicsExtractor> background,
        Consumer<GuiGraphicsExtractor> fill
    ) {
        this.visualState = new VisualState(
            x,
            y,
            Math.max(0.0F, backgroundWidth),
            Math.max(0.0F, backgroundHeight)
        );
        this.guiGraphics = guiGraphics;
        this.background = Objects.requireNonNull(background, "background");
        this.fill = Objects.requireNonNull(fill, "fill");
        this.setFillBounds(0.0F, 0.0F, backgroundWidth, backgroundHeight);
    }

    @Override
    public VisualState visualState() {
        return this.visualState;
    }

    public GuiGraphicsExtractor guiGraphics() {
        return this.guiGraphics;
    }

    public void setGuiGraphics(GuiGraphicsExtractor guiGraphics) {
        this.guiGraphics = guiGraphics;
    }

    public void setBackgroundBounds(float x, float y, float width, float height) {
        this.setBounds(x, y, Math.max(0.0F, width), Math.max(0.0F, height));
    }

    public float fillX() {
        return this.fillX;
    }

    public float fillY() {
        return this.fillY;
    }

    public float fillWidth() {
        return this.fillWidth;
    }

    public float fillHeight() {
        return this.fillHeight;
    }

    public float resolvedFillX() {
        return this.resolvedX() + this.fillX;
    }

    public float resolvedFillY() {
        return this.resolvedY() + this.fillY;
    }

    public void setFillBounds(float x, float y, float width, float height) {
        this.fillX = x;
        this.fillY = y;
        this.fillWidth = Math.max(0.0F, width);
        this.fillHeight = Math.max(0.0F, height);
    }

    public void setFillPosition(float x, float y) {
        this.fillX = x;
        this.fillY = y;
    }

    public void setFillSize(float width, float height) {
        this.fillWidth = Math.max(0.0F, width);
        this.fillHeight = Math.max(0.0F, height);
    }

    public float progress() {
        return this.progress;
    }

    public void setProgress(float progress) {
        this.progress = Float.isFinite(progress)
            ? Math.max(0.0F, Math.min(progress, 1.0F))
            : 0.0F;
    }

    public void draw() {
        this.draw(this.guiGraphics);
    }

    public void draw(GuiGraphicsExtractor guiGraphics) {
        if (!this.visible() || guiGraphics == null) {
            return;
        }

        this.background.accept(guiGraphics);
        if (this.progress <= 0.0F || this.fillWidth <= 0.0F || this.fillHeight <= 0.0F) {
            return;
        }

        guiGraphics.nextStratum();
        float left = this.resolvedFillX();
        float top = this.resolvedFillY();
        float right = left + this.fillWidth * this.progress;
        float bottom = top + this.fillHeight;
        ((GuiGraphicsExtractorBridge)guiGraphics).javaReforged$enablePreciseScissor(
            left,
            top,
            right,
            bottom
        );
        try {
            this.fill.accept(guiGraphics);
        } finally {
            guiGraphics.disableScissor();
        }
    }

    @Override
    public Object dslValue(String property) {
        return switch (property) {
            case "progress" -> this.progress();
            case "fill_x" -> this.fillX();
            case "fill_y" -> this.fillY();
            case "fill_w", "fill_width" -> this.fillWidth();
            case "fill_h", "fill_height" -> this.fillHeight();
            case "resolved_fill_x" -> this.resolvedFillX();
            case "resolved_fill_y" -> this.resolvedFillY();
            default -> VisualElementsInterface.super.dslValue(property);
        };
    }
}
