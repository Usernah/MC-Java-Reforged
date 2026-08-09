package net.jr.client.components.elements;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.LinkedHashMap;
import java.util.Map;

public class ImageElement implements VisualElementsInterface {
    private final VisualState visualState;
    private Asset texture;
    private GuiGraphicsExtractor guiGraphics;

    private boolean useMeta;
    private String metaConfigId;

    private boolean hasUvCord;
    private int u;
    private int v;

    private boolean hasUvSize;
    private float uWidth;
    private float vHeight;

    private boolean hasAtlasSize;
    private float atlasWidth;
    private float atlasHeight;

    private boolean hasColor;
    private int color = 0xFFFFFFFF;

    private boolean hasNineSlice;
    private int nsTop;
    private int nsBottom;
    private int nsLeft;
    private int nsRight;
    private Draw.CenterMode nsCenterMode = Draw.CenterMode.STRETCH;

    private boolean hasAnimation;
    private int animFrames = 1;
    private int animFrameWidth;
    private int animFrameHeight;
    private int animDuration;
    private Draw.AnimUnit animUnit = Draw.AnimUnit.MILLISECONDS;
    private boolean animInterpolate;
    private final Map<Integer, Integer> animFrameDurations = new LinkedHashMap<>();

    private boolean hasBlur;
    private boolean blur;

    public ImageElement(Asset texture, float x, float y, float width, float height, GuiGraphicsExtractor guiGraphics) {
        this.visualState = new VisualState(x, y, width, height);
        this.texture = texture;
        this.guiGraphics = guiGraphics;
    }

    @Override
    public VisualState visualState() {
        return this.visualState;
    }

    public void setTexture(Asset texture) {
        this.texture = texture;
    }

    public Asset texture() {
        return this.texture;
    }

    public void setGuiGraphics(GuiGraphicsExtractor guiGraphics) {
        this.guiGraphics = guiGraphics;
    }

    public GuiGraphicsExtractor guiGraphics() {
        return this.guiGraphics;
    }

    public void setUseMeta(boolean useMeta) {
        this.useMeta = useMeta;
    }

    public void setConfig(String configId) {
        this.useMeta = true;
        this.metaConfigId = configId;
    }

    public void clearConfig() {
        this.metaConfigId = null;
    }

    public void setUvCord(int u, int v) {
        this.hasUvCord = true;
        this.u = u;
        this.v = v;
    }

    public void clearUvCord() {
        this.hasUvCord = false;
        this.u = 0;
        this.v = 0;
    }

    public void setUvSize(float uWidth, float vHeight) {
        this.hasUvSize = true;
        this.uWidth = uWidth;
        this.vHeight = vHeight;
    }

    public void clearUvSize() {
        this.hasUvSize = false;
        this.uWidth = 0.0F;
        this.vHeight = 0.0F;
    }

    public void setAtlasSize(float atlasWidth, float atlasHeight) {
        this.hasAtlasSize = true;
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
    }

    public void clearAtlasSize() {
        this.hasAtlasSize = false;
        this.atlasWidth = 0.0F;
        this.atlasHeight = 0.0F;
    }

    public void setColor(int color) {
        this.hasColor = true;
        this.color = color;
    }

    public void clearColor() {
        this.hasColor = false;
        this.color = 0xFFFFFFFF;
    }

    public void setNineSlice(int border) {
        this.setNineSlice(border, border, border, border, Draw.CenterMode.STRETCH);
    }

    public void setNineSlice(int top, int bottom, int left, int right) {
        this.setNineSlice(top, bottom, left, right, Draw.CenterMode.STRETCH);
    }

    public void setNineSlice(int top, int bottom, int left, int right, Draw.CenterMode centerMode) {
        this.hasNineSlice = true;
        this.nsTop = top;
        this.nsBottom = bottom;
        this.nsLeft = left;
        this.nsRight = right;
        this.nsCenterMode = centerMode == null ? Draw.CenterMode.STRETCH : centerMode;
    }

    public void clearNineSlice() {
        this.hasNineSlice = false;
        this.nsTop = 0;
        this.nsBottom = 0;
        this.nsLeft = 0;
        this.nsRight = 0;
        this.nsCenterMode = Draw.CenterMode.STRETCH;
    }

    public void setAnimation(int frames, int frameWidth, int frameHeight, int duration, boolean interpolate) {
        this.setAnimation(frames, frameWidth, frameHeight, duration, Draw.AnimUnit.MILLISECONDS, interpolate);
    }

    public void setAnimation(
            int frames,
            int frameWidth,
            int frameHeight,
            int duration,
            Draw.AnimUnit unit,
            boolean interpolate
    ) {
        this.hasAnimation = true;
        this.animFrames = frames;
        this.animFrameWidth = frameWidth;
        this.animFrameHeight = frameHeight;
        this.animDuration = duration;
        this.animUnit = unit == null ? Draw.AnimUnit.MILLISECONDS : unit;
        this.animInterpolate = interpolate;
    }

    public void clearAnimation() {
        this.hasAnimation = false;
        this.animFrames = 1;
        this.animFrameWidth = 0;
        this.animFrameHeight = 0;
        this.animDuration = 0;
        this.animUnit = Draw.AnimUnit.MILLISECONDS;
        this.animInterpolate = false;
        this.animFrameDurations.clear();
    }

    public void setFrameDuration(int frame, int duration) {
        if (frame >= 0 && duration > 0) {
            this.animFrameDurations.put(frame, duration);
        }
    }

    public void setFrameDurations(Map<Integer, Integer> frameDurations) {
        this.animFrameDurations.clear();
        if (frameDurations == null) {
            return;
        }
        for (Map.Entry<Integer, Integer> entry : frameDurations.entrySet()) {
            this.setFrameDuration(entry.getKey(), entry.getValue());
        }
    }

    public void setBlur(boolean blur) {
        this.hasBlur = true;
        this.blur = blur;
    }

    public void clearBlur() {
        this.hasBlur = false;
        this.blur = false;
    }

    public void draw() {
        this.draw(this.guiGraphics);
    }

    public void draw(GuiGraphicsExtractor guiGraphics) {
        if (!this.visible() || this.texture == null || guiGraphics == null) {
            return;
        }

        Draw.ImageBuilder builder = this.useMeta
                ? Draw.imageFromMeta(this.texture, this.x(), this.y(), this.width(), this.height())
                : Draw.image(this.texture, this.x(), this.y(), this.width(), this.height());

        if (this.metaConfigId != null) {
            builder.config(this.metaConfigId);
        }
        if (this.hasUvCord) {
            builder.uvCord(this.u, this.v);
        }
        if (this.hasUvSize) {
            builder.uvSize(this.uWidth, this.vHeight);
        }
        if (this.hasAtlasSize) {
            builder.atlasSize(this.atlasWidth, this.atlasHeight);
        }
        if (this.hasColor) {
            builder.color(this.colorWithElementAlpha());
        } else {
            builder.alpha(this.alpha());
        }
        builder.rotation(this.rotation());
        if (this.hasNineSlice) {
            builder.nineSlice(this.nsTop, this.nsBottom, this.nsLeft, this.nsRight, this.nsCenterMode);
        }
        if (this.hasAnimation) {
            builder.animate(
                    this.animFrames,
                    this.animFrameWidth,
                    this.animFrameHeight,
                    this.animDuration,
                    this.animUnit,
                    this.animInterpolate
            );
            builder.frameDurations(this.animFrameDurations);
        }
        if (this.hasBlur) {
            builder.blur(this.blur);
        }

        builder.draw(guiGraphics);
    }

    private int colorWithElementAlpha() {
        int colorAlpha = this.color >>> 24;
        int finalAlpha = Math.round(colorAlpha * this.alpha());
        return (finalAlpha << 24) | (this.color & 0xFFFFFF);
    }
}
