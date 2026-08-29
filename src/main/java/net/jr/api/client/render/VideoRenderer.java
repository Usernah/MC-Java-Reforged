package net.jr.api.client.render;

import net.jr.api.client.video.VideoHolder;
import net.jr.client.video.VideoPlaybackEngine;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class VideoRenderer implements AutoCloseable {
    private final VideoPlaybackEngine engine;

    public VideoRenderer(VideoHolder video) {
        this(new VideoPlaybackEngine(video));
    }

    private VideoRenderer(VideoPlaybackEngine engine) {
        this.engine = engine;
    }

    public static Builder builder(VideoHolder video) {
        return new Builder(video);
    }

    public void render(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        this.engine.render(graphics, screenWidth, screenHeight);
    }

    public void renderFullscreen(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        this.engine.renderFullscreen(graphics, screenWidth, screenHeight);
    }

    public void renderRect(
            GuiGraphicsExtractor graphics,
            float x,
            float y,
            float width,
            float height
    ) {
        this.engine.renderRect(graphics, x, y, width, height);
    }

    public boolean hasFrame() {
        return this.engine.hasFrame();
    }

    @Override
    public void close() {
        this.engine.close();
    }

    public static final class Builder {
        private final VideoPlaybackEngine.Builder delegate;

        public Builder(VideoHolder video) {
            this.delegate = new VideoPlaybackEngine.Builder(video);
        }

        public Builder loop(boolean enabled) {
            this.delegate.loop(enabled);
            return this;
        }

        public Builder loopFade() {
            this.delegate.loopFade();
            return this;
        }

        public Builder loopFade(int milliseconds) {
            this.delegate.loopFade(milliseconds);
            return this;
        }

        public Builder forceExtract(boolean forceExtractOnCreate) {
            this.delegate.forceExtract(forceExtractOnCreate);
            return this;
        }

        public VideoRenderer build() {
            return new VideoRenderer(this.delegate.build());
        }
    }
}
