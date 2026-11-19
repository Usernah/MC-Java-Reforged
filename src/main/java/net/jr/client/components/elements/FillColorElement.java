package net.jr.client.components.elements;

import net.jr.api.client.render.Draw;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class FillColorElement implements VisualElementsInterface {
    private final VisualState visualState;
    private GuiGraphicsExtractor guiGraphics;
    private int topColor;
    private int bottomColor;
    private boolean gradient;

    public FillColorElement(
        int color,
        float x,
        float y,
        float width,
        float height,
        GuiGraphicsExtractor guiGraphics
    ) {
        this.visualState = new VisualState(x, y, width, height);
        this.guiGraphics = guiGraphics;
        this.setColor(color);
    }

    public FillColorElement(
        int topColor,
        int bottomColor,
        float x,
        float y,
        float width,
        float height,
        GuiGraphicsExtractor guiGraphics
    ) {
        this.visualState = new VisualState(x, y, width, height);
        this.guiGraphics = guiGraphics;
        this.setColor(topColor, bottomColor);
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

    public void setColor(int color) {
        this.topColor = color;
        this.bottomColor = color;
        this.gradient = false;
    }

    public void setColor(int topColor, int bottomColor) {
        this.topColor = topColor;
        this.bottomColor = bottomColor;
        this.gradient = true;
    }

    public int topColor() {
        return this.topColor;
    }

    public int bottomColor() {
        return this.bottomColor;
    }

    public boolean isGradient() {
        return this.gradient;
    }

    public void draw() {
        this.draw(this.guiGraphics);
    }

    public void draw(GuiGraphicsExtractor guiGraphics) {
        if (!this.visible() || guiGraphics == null) {
            return;
        }

        Draw.RectangleBuilder builder = Draw.rectangle(
            this.resolvedX(),
            this.resolvedY(),
            this.width(),
            this.height()
        ).rotation(this.rotation());

        if (this.gradient) {
            builder.color(
                colorWithElementAlpha(this.topColor),
                colorWithElementAlpha(this.bottomColor)
            );
        } else {
            builder.color(colorWithElementAlpha(this.topColor));
        }
        builder.draw(guiGraphics);
    }

    private int colorWithElementAlpha(int color) {
        int colorAlpha = color >>> 24;
        int finalAlpha = Math.round(colorAlpha * this.alpha());
        return finalAlpha << 24 | color & 0xFFFFFF;
    }
}
