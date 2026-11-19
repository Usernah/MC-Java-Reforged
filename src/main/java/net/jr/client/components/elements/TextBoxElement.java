package net.jr.client.components.elements;

import java.util.Objects;
import net.jr.api.client.render.Draw;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A fixed visual box that lays out a configured {@link TextElement} inside it.
 * The box owns position and size; the supplied text owns content and style.
 */
public class TextBoxElement implements VisualElementsInterface {
    private final TextElement text;
    private final VisualState visualState;
    private int maxLines;
    private float lineSpacing;

    public TextBoxElement(TextElement text, float width, float height) {
        this.text = Objects.requireNonNull(text, "text");
        if (text.componentText() == null) {
            throw new IllegalArgumentException(
                    "TextBoxElement requires a Component-backed TextElement"
            );
        }

        this.visualState = new VisualState(
                text.x(),
                text.y(),
                Math.max(0.0F, width),
                Math.max(0.0F, height)
        );
        this.visualState.setAnchor(text.anchor());
        this.visualState.captureBaseState();
    }

    @Override
    public VisualState visualState() {
        return this.visualState;
    }

    public TextElement text() {
        return this.text;
    }

    public int maxLines() {
        return this.maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = Math.max(0, maxLines);
    }

    public float lineSpacing() {
        return this.lineSpacing;
    }

    public void setLineSpacing(float lineSpacing) {
        this.lineSpacing = Math.max(0.0F, lineSpacing);
    }

    public int lineCount() {
        return this.visibleBuilder().lineCount();
    }

    public float contentWidth() {
        return this.contentBuilder().width();
    }

    public float contentHeight() {
        return this.contentBuilder().height();
    }

    public boolean isOverflowing() {
        Draw.TextBuilder content = this.contentBuilder();
        Draw.TextBuilder visible = this.visibleBuilder();
        return content.width() > this.width()
                || content.lineCount() > visible.lineCount()
                || content.height() > visible.height();
    }

    public void draw() {
        this.draw(this.text.guiGraphics());
    }

    public void draw(GuiGraphicsExtractor guiGraphics) {
        if (!this.visible() || !this.text.visible() || guiGraphics == null) {
            return;
        }
        this.visibleBuilder().draw(guiGraphics);
    }

    @Override
    public Object dslValue(String property) {
        return switch (property) {
            case "content_width" -> this.contentWidth();
            case "content_height" -> this.contentHeight();
            case "line_count" -> this.lineCount();
            case "overflowing" -> this.isOverflowing();
            default -> VisualElementsInterface.super.dslValue(property);
        };
    }

    private Draw.TextBuilder visibleBuilder() {
        return this.positionedBuilder()
                .wrap(this.width())
                .maxHeight(this.height())
                .maxLines(this.maxLines)
                .lineSpacing(this.lineSpacing);
    }

    private Draw.TextBuilder contentBuilder() {
        return this.positionedBuilder()
                .wrap(this.width())
                .lineSpacing(this.lineSpacing);
    }

    private Draw.TextBuilder positionedBuilder() {
        float boxLeft = this.resolvedX();
        float textX = switch (this.text.align()) {
            case LEFT -> boxLeft;
            case CENTER -> boxLeft + this.width() / 2.0F;
            case RIGHT -> boxLeft + this.width();
        };
        return this.text.styledBuilder(textX, this.resolvedY(), this.alpha());
    }
}
