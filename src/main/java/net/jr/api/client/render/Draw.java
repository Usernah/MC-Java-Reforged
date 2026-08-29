package net.jr.api.client.render;

import net.jr.api.client.render.font.FontHolder;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.video.VideoHolder;
import net.jr.client.render.UiRenderEngine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.Map;

public final class Draw {
    private Draw() {
    }

    public enum CenterMode {
        STRETCH,
        REPEAT
    }

    public enum TextAlign {
        LEFT,
        CENTER,
        RIGHT
    }

    public enum AnimUnit {
        MILLISECONDS(1),
        TICKS(50),
        SECONDS(1000),
        MINUTES(60_000);

        private final int milliseconds;

        AnimUnit(int milliseconds) {
            this.milliseconds = milliseconds;
        }

        public int toMillis(int value) {
            return Math.multiplyExact(value, this.milliseconds);
        }
    }

    public static ImageBuilder image(Asset texture, float x, float y, float width, float height) {
        return new ImageBuilder(UiRenderEngine.image(texture, x, y, width, height));
    }

    public static ImageBuilder imageFromMeta(Asset texture, float x, float y, float width, float height) {
        return new ImageBuilder(UiRenderEngine.imageFromMeta(texture, x, y, width, height));
    }

    public static TextBuilder text(Component text) {
        return new TextBuilder(UiRenderEngine.text(text));
    }

    public static TextBuilder text(Component text, int x, int y) {
        return new TextBuilder(UiRenderEngine.text(text, x, y));
    }

    public static TextBuilder text(FormattedCharSequence text) {
        return new TextBuilder(UiRenderEngine.text(text));
    }

    public static TextBuilder text(FormattedCharSequence text, int x, int y) {
        return new TextBuilder(UiRenderEngine.text(text, x, y));
    }

    public static VideoBuilder video(VideoHolder video) {
        return new VideoBuilder(UiRenderEngine.video(video));
    }

    public static void closeVideos() {
        UiRenderEngine.closeVideos();
    }

    public static class ImageBuilder {
        private final UiRenderEngine.ImageBuilder delegate;

        public ImageBuilder(Asset texture, float x, float y, float width, float height) {
            this(UiRenderEngine.image(texture, x, y, width, height));
        }

        private ImageBuilder(UiRenderEngine.ImageBuilder delegate) {
            this.delegate = delegate;
        }

        public ImageBuilder config(String configId) {
            this.delegate.config(configId);
            return this;
        }

        public ImageBuilder uvCord(int u, int v) {
            this.delegate.uvCord(u, v);
            return this;
        }

        public ImageBuilder uvSize(float uWidth, float vHeight) {
            this.delegate.uvSize(uWidth, vHeight);
            return this;
        }

        public ImageBuilder atlasSize(float atlasWidth, float atlasHeight) {
            this.delegate.atlasSize(atlasWidth, atlasHeight);
            return this;
        }

        public ImageBuilder color(int color) {
            this.delegate.color(color);
            return this;
        }

        public ImageBuilder alpha(float alpha) {
            this.delegate.alpha(alpha);
            return this;
        }

        public ImageBuilder rotation(float degrees) {
            this.delegate.rotation(degrees);
            return this;
        }

        public ImageBuilder nineSlice(int border) {
            this.delegate.nineSlice(border);
            return this;
        }

        public ImageBuilder nineSlice(int top, int bottom, int left, int right) {
            this.delegate.nineSlice(top, bottom, left, right);
            return this;
        }

        public ImageBuilder nineSlice(
                int top,
                int bottom,
                int left,
                int right,
                CenterMode centerMode
        ) {
            this.delegate.nineSlice(
                    top,
                    bottom,
                    left,
                    right,
                    centerMode == CenterMode.REPEAT
                            ? UiRenderEngine.CenterMode.REPEAT
                            : UiRenderEngine.CenterMode.STRETCH
            );
            return this;
        }

        public ImageBuilder animate(
                int frames,
                int frameWidth,
                int frameHeight,
                int duration,
                boolean interpolate
        ) {
            this.delegate.animate(frames, frameWidth, frameHeight, duration, interpolate);
            return this;
        }

        public ImageBuilder animate(
                int frames,
                int frameWidth,
                int frameHeight,
                int duration,
                AnimUnit unit,
                boolean interpolate
        ) {
            this.delegate.animate(
                    frames,
                    frameWidth,
                    frameHeight,
                    duration,
                    internal(unit),
                    interpolate
            );
            return this;
        }

        public ImageBuilder frameDuration(int frame, int duration) {
            this.delegate.frameDuration(frame, duration);
            return this;
        }

        public ImageBuilder frameDurations(Map<Integer, Integer> frameDurations) {
            this.delegate.frameDurations(frameDurations);
            return this;
        }

        public ImageBuilder blur(boolean blur) {
            this.delegate.blur(blur);
            return this;
        }

        public ImageBuilder antialiasing() {
            this.delegate.antialiasing();
            return this;
        }

        public ImageBuilder antialiasing(boolean enabled) {
            this.delegate.antialiasing(enabled);
            return this;
        }

        public ImageBuilder pixelSnap() {
            this.delegate.pixelSnap();
            return this;
        }

        public ImageBuilder pixelSnap(boolean enabled) {
            this.delegate.pixelSnap(enabled);
            return this;
        }

        public ImageBuilder pixelCenterCorrection() {
            this.delegate.pixelCenterCorrection();
            return this;
        }

        public ImageBuilder pixelCenterCorrection(boolean enabled) {
            this.delegate.pixelCenterCorrection(enabled);
            return this;
        }

        public void draw(GuiGraphicsExtractor graphics) {
            this.delegate.draw(graphics);
        }

        private static UiRenderEngine.AnimUnit internal(AnimUnit unit) {
            if (unit == null) {
                return UiRenderEngine.AnimUnit.MILLISECONDS;
            }
            return switch (unit) {
                case MILLISECONDS -> UiRenderEngine.AnimUnit.MILLISECONDS;
                case TICKS -> UiRenderEngine.AnimUnit.TICKS;
                case SECONDS -> UiRenderEngine.AnimUnit.SECONDS;
                case MINUTES -> UiRenderEngine.AnimUnit.MINUTES;
            };
        }
    }

    public static class TextBuilder {
        private final UiRenderEngine.TextBuilder delegate;

        public TextBuilder(Component text, int x, int y) {
            this(UiRenderEngine.text(text, x, y));
        }

        public TextBuilder(FormattedCharSequence text, int x, int y) {
            this(UiRenderEngine.text(text, x, y));
        }

        private TextBuilder(UiRenderEngine.TextBuilder delegate) {
            this.delegate = delegate;
        }

        public TextBuilder scale(float scale) {
            this.delegate.scale(scale);
            return this;
        }

        public TextBuilder position(int x, int y) {
            this.delegate.position(x, y);
            return this;
        }

        public TextBuilder x(int x) {
            this.delegate.x(x);
            return this;
        }

        public TextBuilder y(int y) {
            this.delegate.y(y);
            return this;
        }

        public int x() {
            return this.delegate.x();
        }

        public int y() {
            return this.delegate.y();
        }

        public TextBuilder color(int a, int r, int g, int b) {
            this.delegate.color(a, r, g, b);
            return this;
        }

        public TextBuilder color(int color) {
            this.delegate.color(color);
            return this;
        }

        public TextBuilder shadow(boolean shadow) {
            this.delegate.shadow(shadow);
            return this;
        }

        public TextBuilder shadowColor(int a, int r, int g, int b) {
            this.delegate.shadowColor(a, r, g, b);
            return this;
        }

        public TextBuilder shadowColor(int shadowColor) {
            this.delegate.shadowColor(shadowColor);
            return this;
        }

        public TextBuilder outline(int outlineColor) {
            this.delegate.outline(outlineColor);
            return this;
        }

        public TextBuilder align(TextAlign align) {
            this.delegate.align(switch (align == null ? TextAlign.LEFT : align) {
                case LEFT -> UiRenderEngine.TextAlign.LEFT;
                case CENTER -> UiRenderEngine.TextAlign.CENTER;
                case RIGHT -> UiRenderEngine.TextAlign.RIGHT;
            });
            return this;
        }

        public TextBuilder font(Asset font) {
            this.delegate.font(font);
            return this;
        }

        public TextBuilder font(FontHolder font) {
            this.delegate.font(font);
            return this;
        }

        public int width() {
            return this.delegate.width();
        }

        public int lineHeight() {
            return this.delegate.lineHeight();
        }

        public int height() {
            return this.delegate.height();
        }

        public void draw(GuiGraphicsExtractor graphics) {
            this.delegate.draw(graphics);
        }
    }

    public static class VideoBuilder {
        private final UiRenderEngine.VideoBuilder delegate;

        public VideoBuilder(VideoHolder video) {
            this(UiRenderEngine.video(video));
        }

        private VideoBuilder(UiRenderEngine.VideoBuilder delegate) {
            this.delegate = delegate;
        }

        public VideoBuilder rect(float x, float y, float width, float height) {
            this.delegate.rect(x, y, width, height);
            return this;
        }

        public VideoBuilder fullscreen() {
            this.delegate.fullscreen();
            return this;
        }

        public VideoBuilder loop() {
            this.delegate.loop();
            return this;
        }

        public VideoBuilder loop(boolean value) {
            this.delegate.loop(value);
            return this;
        }

        public VideoBuilder loopFade(int milliseconds) {
            this.delegate.loopFade(milliseconds);
            return this;
        }

        public void draw(GuiGraphicsExtractor graphics) {
            this.delegate.draw(graphics);
        }
    }
}
