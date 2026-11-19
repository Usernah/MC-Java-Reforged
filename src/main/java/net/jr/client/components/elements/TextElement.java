package net.jr.client.components.elements;

import net.jr.api.client.render.Draw;
import net.jr.api.client.render.font.FontHolder;
import net.jr.api.client.resource.Asset;
import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.util.FormattedCharSequence;

import java.util.Objects;

public class TextElement implements VisualElementsInterface {
    public enum ScrollType {
        LOOP,
        PING_PONG
    }

    private static final float DEFAULT_SCROLL_VELOCITY = 0.055F;
    private static final long DEFAULT_SCROLL_DELAY_MS = 350L;

    private final VisualState visualState;
    private Component componentText;
    private FormattedCharSequence charSequenceText;
    private GuiGraphicsExtractor guiGraphics;

    private float scale = 1.0F;
    private int color = 0xFFFFFFFF;
    private boolean shadow;
    private boolean legacyShadow;
    private Integer shadowColor;
    private Integer outlineColor;
    private Draw.TextAlign align = Draw.TextAlign.LEFT;
    private Asset font;
    private float scrollWidth;
    private ScrollType scrollType = ScrollType.PING_PONG;
    private float scrollVelocity = DEFAULT_SCROLL_VELOCITY;
    private long scrollDelayMs = DEFAULT_SCROLL_DELAY_MS;
    private long scrollStartedAtMs = -1L;

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
        if (this.charSequenceText == null && Objects.equals(this.componentText, text)) {
            return;
        }
        this.componentText = Objects.requireNonNull(text, "text");
        this.charSequenceText = null;
        this.updateMeasurements();
        this.resetScrollClock();
    }

    public void setText(FormattedCharSequence text) {
        if (this.componentText == null && Objects.equals(this.charSequenceText, text)) {
            return;
        }
        this.componentText = null;
        this.charSequenceText = Objects.requireNonNull(text, "text");
        this.updateMeasurements();
        this.resetScrollClock();
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
        this.resetScrollClock();
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

    public boolean legacyShadow() {
        return this.legacyShadow;
    }

    public void setLegacyShadow(boolean legacyShadow) {
        this.legacyShadow = legacyShadow;
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
        this.resetScrollClock();
    }

    public void setFont(FontHolder font) {
        this.setFont(font == null ? null : font.location());
    }

    public void clearFont() {
        this.font = null;
        this.updateMeasurements();
        this.resetScrollClock();
    }

    public void setScrolling(float width, ScrollType type) {
        this.scrollWidth = Math.max(0.0F, width);
        this.scrollType = type == null ? ScrollType.PING_PONG : type;
        this.resetScrollClock();
    }

    public void clearScrolling() {
        this.scrollWidth = 0.0F;
        this.resetScrollClock();
    }

    public boolean isScrolling() {
        return this.scrollWidth > 0.0F;
    }

    public float scrollWidth() {
        return this.scrollWidth;
    }

    public ScrollType scrollType() {
        return this.scrollType;
    }

    public float scrollVelocity() {
        return this.scrollVelocity;
    }

    public void setScrollVelocity(float velocity) {
        this.scrollVelocity = Math.max(0.0F, velocity);
        this.resetScrollClock();
    }

    public long scrollDelayMs() {
        return this.scrollDelayMs;
    }

    public void setScrollDelayMs(long delayMs) {
        this.scrollDelayMs = Math.max(0L, delayMs);
        this.resetScrollClock();
    }

    public void draw() {
        this.draw(this.guiGraphics);
    }

    public void draw(GuiGraphicsExtractor guiGraphics) {
        if (!this.visible() || guiGraphics == null) {
            return;
        }

        Draw.TextBuilder builder = this.drawBuilder();
        this.applyStyle(builder);
        this.updateMeasurements(builder);

        float overflow = this.width() - this.scrollWidth;
        if (!this.isScrolling() || overflow <= 0.0F || this.scrollVelocity <= 0.0F) {
            builder.draw(guiGraphics);
            return;
        }

        float clipX = this.scrollClipX();
        float offset = this.scrollOffset(Util.getMillis(), overflow);
        float drawY = this.resolvedY();
        Draw.TextBuilder scrollingBuilder = this.builder(clipX - offset, drawY);
        this.applyStyle(scrollingBuilder, Draw.TextAlign.LEFT);

        ((GuiGraphicsExtractorBridge)(Object)guiGraphics)
            .javaReforged$enablePreciseScissor(
                clipX,
                drawY - 1.0F,
                clipX + this.scrollWidth,
                drawY + this.height() + 1.0F
            );
        try {
            scrollingBuilder.draw(guiGraphics);
        } finally {
            guiGraphics.disableScissor();
        }
    }

    private Draw.TextBuilder builder() {
        return this.builder(this.x(), this.y());
    }

    private Draw.TextBuilder drawBuilder() {
        float drawX = this.resolvedX();
        if (this.anchor().isExplicit()) {
            drawX += switch (this.align) {
                case LEFT -> 0.0F;
                case CENTER -> this.width() / 2.0F;
                case RIGHT -> this.width();
            };
        }
        return this.builder(drawX, this.resolvedY());
    }

    private Draw.TextBuilder builder(float x, float y) {
        int drawX = Math.round(x);
        int drawY = Math.round(y);
        return this.componentText != null
            ? Draw.text(this.componentText, drawX, drawY)
            : Draw.text(this.charSequenceText, drawX, drawY);
    }

    private void applyStyle(Draw.TextBuilder builder) {
        this.applyStyle(builder, this.align, 1.0F);
    }

    private void applyStyle(Draw.TextBuilder builder, Draw.TextAlign alignment) {
        this.applyStyle(builder, alignment, 1.0F);
    }

    private void applyStyle(
            Draw.TextBuilder builder,
            Draw.TextAlign alignment,
            float parentAlpha
    ) {
        builder.scale(this.scale)
            .color(this.colorWithElementAlpha(parentAlpha))
            .shadow(this.shadow)
            .legacyShadow(this.legacyShadow)
            .align(alignment);
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

    Draw.TextBuilder styledBuilder(float x, float y, float parentAlpha) {
        Draw.TextBuilder builder = this.builder(x, y);
        this.applyStyle(builder, this.align, parentAlpha);
        return builder;
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
        return this.colorWithElementAlpha(1.0F);
    }

    private int colorWithElementAlpha(float parentAlpha) {
        int colorAlpha = this.color >>> 24;
        float effectiveAlpha = Math.max(0.0F, Math.min(1.0F, parentAlpha));
        int finalAlpha = Math.round(colorAlpha * this.alpha() * effectiveAlpha);
        return (finalAlpha << 24) | (this.color & 0xFFFFFF);
    }

    private float scrollClipX() {
        float drawX = this.drawX();
        return switch (this.align) {
            case LEFT -> drawX;
            case CENTER -> drawX - this.scrollWidth / 2.0F;
            case RIGHT -> drawX - this.scrollWidth;
        };
    }

    private float drawX() {
        float drawX = this.resolvedX();
        if (!this.anchor().isExplicit()) {
            return drawX;
        }
        return drawX + switch (this.align) {
            case LEFT -> 0.0F;
            case CENTER -> this.width() / 2.0F;
            case RIGHT -> this.width();
        };
    }

    private float scrollOffset(long nowMs, float overflow) {
        if (this.scrollStartedAtMs < 0L) {
            this.scrollStartedAtMs = nowMs;
        }

        double travelMs = overflow / this.scrollVelocity;
        if (travelMs <= 0.0D) {
            return 0.0F;
        }

        double elapsed = Math.max(0L, nowMs - this.scrollStartedAtMs);
        return switch (this.scrollType) {
            case LOOP -> this.loopOffset(elapsed, travelMs, overflow);
            case PING_PONG -> this.pingPongOffset(elapsed, travelMs, overflow);
        };
    }

    private float loopOffset(double elapsed, double travelMs, float overflow) {
        double cycleMs = this.scrollDelayMs + travelMs;
        double cycleElapsed = cycleMs <= 0.0D ? 0.0D : elapsed % cycleMs;
        if (cycleElapsed <= this.scrollDelayMs) {
            return 0.0F;
        }
        return Math.min(
            overflow,
            (float)((cycleElapsed - this.scrollDelayMs) * this.scrollVelocity)
        );
    }

    private float pingPongOffset(double elapsed, double travelMs, float overflow) {
        double cycleMs = this.scrollDelayMs * 2.0D + travelMs * 2.0D;
        double cycleElapsed = cycleMs <= 0.0D ? 0.0D : elapsed % cycleMs;
        if (cycleElapsed <= this.scrollDelayMs) {
            return 0.0F;
        }

        cycleElapsed -= this.scrollDelayMs;
        if (cycleElapsed <= travelMs) {
            return Math.min(overflow, (float)(cycleElapsed * this.scrollVelocity));
        }

        cycleElapsed -= travelMs;
        if (cycleElapsed <= this.scrollDelayMs) {
            return overflow;
        }

        cycleElapsed -= this.scrollDelayMs;
        return Math.max(0.0F, overflow - (float)(cycleElapsed * this.scrollVelocity));
    }

    private void resetScrollClock() {
        this.scrollStartedAtMs = -1L;
    }
}
