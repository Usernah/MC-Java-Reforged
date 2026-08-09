package net.jr.client.components.elements;

import net.jr.api.client.render.Draw;
import net.jr.api.client.render.font.FontHolder;
import net.jr.api.client.resource.Asset;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.Objects;

public class TextElement implements VisualElementsInterface {
    private final VisualState visualState;
    private Component componentText;
    private FormattedCharSequence charSequenceText;
    private GuiGraphicsExtractor guiGraphics;

    private float scale = 1.0F;
    private int color = 0xFFFFFFFF;
    private boolean shadow;
    private Integer shadowColor;
    private Integer outlineColor;
    private Draw.TextAlign align = Draw.TextAlign.LEFT;
    private Asset font;

    public TextElement(Component text, float x, float y, GuiGraphicsExtractor guiGraphics) {
        this.visualState = new VisualState(x, y, 0.0F, 0.0F);
        this.componentText = Objects.requireNonNull(text, "text");
        this.guiGraphics = guiGraphics;
        this.updateMeasurements();
    }

    public TextElement(FormattedCharSequence text, float x, float y, GuiGraphicsExtractor guiGraphics) {
        this.visualState = new VisualState(x, y, 0.0F, 0.0F);
        this.charSequenceText = Objects.requireNonNull(text, "text");
        this.guiGraphics = guiGraphics;
        this.updateMeasurements();
    }

    @Override
    public VisualState visualState() {
        return this.visualState;
    }

    public Component componentText() {
        return this.componentText;
    }

    public FormattedCharSequence charSequenceText() {
        return this.charSequenceText;
    }

    public void setText(Component text) {
        this.componentText = Objects.requireNonNull(text, "text");
        this.charSequenceText = null;
        this.updateMeasurements();
    }

    public void setText(FormattedCharSequence text) {
        this.componentText = null;
        this.charSequenceText = Objects.requireNonNull(text, "text");
        this.updateMeasurements();
    }

    public GuiGraphicsExtractor guiGraphics() {
        return this.guiGraphics;
    }

    public void setGuiGraphics(GuiGraphicsExtractor guiGraphics) {
        this.guiGraphics = guiGraphics;
    }

    public float scale() {
        return this.scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        this.updateMeasurements();
    }

    public int color() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setColor(int alpha, int red, int green, int blue) {
        this.setColor((alpha & 0xFF) << 24
            | (red & 0xFF) << 16
            | (green & 0xFF) << 8
            | blue & 0xFF);
    }

    public boolean shadow() {
        return this.shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    public Integer shadowColor() {
        return this.shadowColor;
    }

    public void setShadowColor(int shadowColor) {
        this.shadowColor = shadowColor;
    }

    public void setShadowColor(int alpha, int red, int green, int blue) {
        this.setShadowColor((alpha & 0xFF) << 24
            | (red & 0xFF) << 16
            | (green & 0xFF) << 8
            | blue & 0xFF);
    }

    public void clearShadowColor() {
        this.shadowColor = null;
    }

    public Integer outlineColor() {
        return this.outlineColor;
    }

    public void setOutline(int outlineColor) {
        this.outlineColor = outlineColor;
    }

    public void clearOutline() {
        this.outlineColor = null;
    }

    public Draw.TextAlign align() {
        return this.align;
    }

    public void setAlign(Draw.TextAlign align) {
        this.align = align == null ? Draw.TextAlign.LEFT : align;
    }

    public Asset font() {
        return this.font;
    }

    public void setFont(Asset font) {
        this.font = font;
        this.updateMeasurements();
    }

    public void setFont(FontHolder font) {
        this.setFont(font == null ? null : font.location());
    }

    public void clearFont() {
        this.font = null;
        this.updateMeasurements();
    }

    public void draw() {
        this.draw(this.guiGraphics);
    }

    public void draw(GuiGraphicsExtractor guiGraphics) {
        if (!this.visible() || guiGraphics == null) {
            return;
        }

        Draw.TextBuilder builder = this.builder();
        this.applyStyle(builder);
        this.updateMeasurements(builder);
        builder.draw(guiGraphics);
    }

    private Draw.TextBuilder builder() {
        int drawX = Math.round(this.x());
        int drawY = Math.round(this.y());
        return this.componentText != null
            ? Draw.text(this.componentText, drawX, drawY)
            : Draw.text(this.charSequenceText, drawX, drawY);
    }

    private void applyStyle(Draw.TextBuilder builder) {
        builder.scale(this.scale)
            .color(this.colorWithElementAlpha())
            .shadow(this.shadow)
            .align(this.align);
        if (this.shadowColor != null) {
            builder.shadowColor(this.shadowColor);
        }
        if (this.outlineColor != null) {
            builder.outline(this.outlineColor);
        }
        if (this.font != null) {
            builder.font(this.font);
        }
    }

    private void updateMeasurements() {
        Draw.TextBuilder builder = this.builder();
        this.applyStyle(builder);
        this.updateMeasurements(builder);
    }

    private void updateMeasurements(Draw.TextBuilder builder) {
        float measuredWidth = builder.width();
        float measuredHeight = builder.height();
        this.setSize(measuredWidth, measuredHeight);
        this.setBaseSize(measuredWidth, measuredHeight);
    }

    private int colorWithElementAlpha() {
        int colorAlpha = this.color >>> 24;
        int finalAlpha = Math.round(colorAlpha * this.alpha());
        return (finalAlpha << 24) | (this.color & 0xFFFFFF);
    }
}
