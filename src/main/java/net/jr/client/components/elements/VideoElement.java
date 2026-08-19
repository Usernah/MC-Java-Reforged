package net.jr.client.components.elements;

import net.jr.api.client.render.VideoRenderer;
import net.jr.api.client.video.VideoHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Objects;

public class VideoElement implements VisualElementsInterface, AutoCloseable {
    private final VisualState visualState;
    private VideoHolder video;
    private GuiGraphicsExtractor guiGraphics;
    private boolean fullscreen = true;
    private boolean loop = true;
    private int loopFadeMillis;

    private VideoRenderer renderer;
    private VideoHolder rendererVideo;
    private boolean rendererLoop;
    private int rendererLoopFadeMillis;

    public VideoElement(
        VideoHolder video,
        float x,
        float y,
        float width,
        float height,
        GuiGraphicsExtractor guiGraphics
    ) {
        this.visualState = new VisualState(x, y, width, height);
        this.video = Objects.requireNonNull(video, "video");
        this.guiGraphics = guiGraphics;
    }

    @Override
    public VisualState visualState() {
        return this.visualState;
    }

    public VideoHolder video() {
        return this.video;
    }

    public void setVideo(VideoHolder video) {
        VideoHolder resolved = Objects.requireNonNull(video, "video");
        if (!resolved.equals(this.video)) {
            this.video = resolved;
            this.closeRenderer();
        }
    }

    public GuiGraphicsExtractor guiGraphics() {
        return this.guiGraphics;
    }

    public void setGuiGraphics(GuiGraphicsExtractor guiGraphics) {
        this.guiGraphics = guiGraphics;
    }

    public boolean isFullscreen() {
        return this.fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public void fullscreen() {
        this.fullscreen = true;
    }

    public void rect(float x, float y, float width, float height) {
        this.setBounds(x, y, width, height);
        this.fullscreen = false;
    }

    public boolean loopEnabled() {
        return this.loop;
    }

    public void setLoop(boolean loop) {
        if (this.loop != loop) {
            this.loop = loop;
            if (!loop) {
                this.loopFadeMillis = 0;
            }
            this.closeRenderer();
        }
    }

    public void loop() {
        this.setLoop(true);
    }

    public void loop(boolean enabled) {
        this.setLoop(enabled);
    }

    public int loopFadeMillis() {
        return this.loopFadeMillis;
    }

    public void setLoopFade(int milliseconds) {
        int resolved = Math.max(0, milliseconds);
        boolean changed = this.loopFadeMillis != resolved || (resolved > 0 && !this.loop);
        this.loopFadeMillis = resolved;
        if (resolved > 0) {
            this.loop = true;
        }
        if (changed) {
            this.closeRenderer();
        }
    }

    public void loopFade(int milliseconds) {
        this.setLoopFade(milliseconds);
    }

    public void setPlayback(boolean loop, int loopFadeMillis) {
        int resolvedFade = Math.max(0, loopFadeMillis);
        boolean resolvedLoop = loop || resolvedFade > 0;
        if (this.loop != resolvedLoop || this.loopFadeMillis != resolvedFade) {
            this.loop = resolvedLoop;
            this.loopFadeMillis = resolvedFade;
            this.closeRenderer();
        }
    }

    public void draw() {
        this.draw(this.guiGraphics);
    }

    public void draw(GuiGraphicsExtractor guiGraphics) {
        if (!this.visible() || guiGraphics == null) {
            return;
        }

        VideoRenderer activeRenderer = this.renderer();
        if (this.fullscreen) {
            var window = Minecraft.getInstance().getWindow();
            activeRenderer.renderFullscreen(
                guiGraphics,
                window.getGuiScaledWidth(),
                window.getGuiScaledHeight()
            );
        } else {
            activeRenderer.renderRect(guiGraphics, this.x(), this.y(), this.width(), this.height());
        }
    }

    private VideoRenderer renderer() {
        if (this.renderer == null
            || !this.video.equals(this.rendererVideo)
            || this.loop != this.rendererLoop
            || this.loopFadeMillis != this.rendererLoopFadeMillis) {
            this.closeRenderer();
            VideoRenderer.Builder builder = VideoRenderer.builder(this.video).loop(this.loop);
            if (this.loopFadeMillis > 0) {
                builder.loopFade(this.loopFadeMillis);
            }
            this.renderer = builder.build();
            this.rendererVideo = this.video;
            this.rendererLoop = this.loop;
            this.rendererLoopFadeMillis = this.loopFadeMillis;
        }
        return this.renderer;
    }

    @Override
    public void close() {
        this.closeRenderer();
    }

    private void closeRenderer() {
        if (this.renderer != null) {
            this.renderer.close();
            this.renderer = null;
        }
        this.rendererVideo = null;
    }
}
