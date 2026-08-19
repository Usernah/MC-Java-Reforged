package net.jr.api.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.jr.Java_reforged;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.video.VideoHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class VideoRenderer implements AutoCloseable {
    private static final boolean DISABLED = false;
    private static final int DEFAULT_LOOP_FADE_MS = 280;

    private final VideoHolder video;
    private final boolean loopEnabled;
    private final int loopFadeMillis;
    private final boolean forceExtractOnCreate;

    private DynamicTexture dynamicTexture;
    private Asset textureLoader;
    private final Asset textureLocation;

    private final ExecutorService executor;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean hasFrame = new AtomicBoolean(false);
    private final AtomicReference<NativeImage> frameToUpload = new AtomicReference<>();

    private int videoWidth;
    private int videoHeight;
    private File videoFile;
    private NativeImage firstFrameSnapshot;
    private NativeImage lastFrameSnapshot;

    static {
        if (!DISABLED) {
            try {
                Class.forName("org.bytedeco.ffmpeg.global.avutil");
                Class.forName("org.bytedeco.ffmpeg.global.avcodec");
                Class.forName("org.bytedeco.ffmpeg.global.avformat");
                Class.forName("org.bytedeco.ffmpeg.global.swscale");
                FFmpegFrameGrabber.tryLoad();
            } catch (Throwable error) {
                Java_reforged.LOGGER.error("FFmpeg native libraries could not be preloaded", error);
            }
        }
    }

    public VideoRenderer(VideoHolder video) {
        this(new Builder(video));
    }

    private VideoRenderer(Builder builder) {
        this.video = builder.video;
        this.loopEnabled = builder.loopEnabled;
        this.loopFadeMillis = builder.loopFadeMillis;
        this.forceExtractOnCreate = builder.forceExtractOnCreate;
        this.textureLocation = Asset.NamespaceAndPatch(
            Java_reforged.MODID,
            "dynamic/video_" + Integer.toUnsignedString(this.video.location().toString().hashCode())
                + "_" + System.nanoTime()
        );
        this.executor = Executors.newSingleThreadExecutor(runnable ->
            Thread.ofPlatform()
                .name("JR video decoder " + this.video.debugId())
                .daemon(true)
                .unstarted(runnable)
        );

        if (DISABLED) {
            this.isRunning.set(false);
            this.hasFrame.set(false);
            return;
        }

        this.isRunning.set(true);
        executor.submit(() -> {
            this.videoFile = this.video.resolveCachedFile(this.forceExtractOnCreate);
            if (this.videoFile != null && this.isRunning.get()) {
                initGrabber();
            } else if (this.videoFile == null && this.isRunning.get()) {
                Java_reforged.LOGGER.error("Could not load video {}", this.video.debugId());
                isRunning.set(false);
            }
        });
    }

    public static Builder builder(VideoHolder video) {
        return new Builder(video);
    }

    private void initGrabber() {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            try {
                grabber.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_RGBA);
                grabber.start();

                this.videoWidth = grabber.getImageWidth();
                this.videoHeight = grabber.getImageHeight();

                double frameRate = grabber.getFrameRate() > 0 ? grabber.getFrameRate() : 30;

                while (isRunning.get()) {
                    long startTime = System.currentTimeMillis();
                    Frame frame = grabber.grabImage();

                    if (frame == null) {
                        if (!loopEnabled) {
                            isRunning.set(false);
                            break;
                        }
                        playLoopFade(frameRate);
                        grabber.setTimestamp(0);
                        continue;
                    }

                    if (frame.image != null && frame.image.length > 0) {
                        ByteBuffer buffer = (ByteBuffer) frame.image[0];
                        buffer.rewind();

                        NativeImage decoded = decodeFrame(buffer, videoWidth, videoHeight);
                        cacheLoopFrames(decoded);
                        this.hasFrame.set(true);

                        NativeImage old = frameToUpload.getAndSet(decoded);
                        if (old != null) old.close();
                    }

                    long delay = (long) (1000 / frameRate);
                    long difference = System.currentTimeMillis() - startTime;
                    if (delay > difference) Thread.sleep(delay - difference);
                }

                grabber.stop();
            } catch (InterruptedException interrupted) {
                // close() stops the decoder with shutdownNow(). Interrupting
                // its frame pacing sleep is therefore the normal shutdown
                // path, not a video decoding failure.
                Thread.currentThread().interrupt();
                if (isRunning.get()) {
                    Java_reforged.LOGGER.error("Video decoder was interrupted for {}", video.debugId(), interrupted);
                }
            } catch (Exception exception) {
                Java_reforged.LOGGER.error("Video processing failed for {}", video.debugId(), exception);
                try {
                    grabber.stop();
                } catch (Exception stopException) {
                    Java_reforged.LOGGER.error("Could not stop video grabber for {}", video.debugId(), stopException);
                }
            }
        } catch (Throwable error) {
            Java_reforged.LOGGER.error("Could not initialize FFmpegFrameGrabber for {}", video.debugId(), error);
        } finally {
            isRunning.set(false);
        }
    }

    private NativeImage decodeFrame(ByteBuffer source, int width, int height) {
        ByteBuffer buffer = source.duplicate();
        buffer.rewind();
        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = buffer.get() & 0xFF;
                int green = buffer.get() & 0xFF;
                int blue = buffer.get() & 0xFF;
                int alpha = buffer.get() & 0xFF;
                image.setPixelABGR(x, y, (alpha << 24) | (blue << 16) | (green << 8) | red);
            }
        }
        return image;
    }

    private void cacheLoopFrames(NativeImage decodedFrame) {
        if (loopFadeMillis <= 0) {
            return;
        }

        if (firstFrameSnapshot == null) {
            firstFrameSnapshot = cloneImage(decodedFrame);
        }

        NativeImage newLast = cloneImage(decodedFrame);
        NativeImage oldLast = lastFrameSnapshot;
        lastFrameSnapshot = newLast;
        if (oldLast != null) {
            oldLast.close();
        }
    }

    private NativeImage cloneImage(NativeImage source) {
        NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), false);
        copy.copyFrom(source);
        return copy;
    }

    private void playLoopFade(double frameRate) throws InterruptedException {
        if (loopFadeMillis <= 0 || firstFrameSnapshot == null || lastFrameSnapshot == null) {
            return;
        }

        int steps = Math.max(2, (int) Math.round((loopFadeMillis / 1000.0D) * frameRate));
        long frameDelay = Math.max(1L, (long) (1000.0D / (frameRate > 0 ? frameRate : 30.0D)));

        for (int i = 1; i <= steps && isRunning.get(); i++) {
            float alpha = i / (float) steps;
            NativeImage blended = blendFrames(lastFrameSnapshot, firstFrameSnapshot, alpha);
            NativeImage old = frameToUpload.getAndSet(blended);
            if (old != null) {
                old.close();
            }
            Thread.sleep(frameDelay);
        }
    }

    private NativeImage blendFrames(NativeImage from, NativeImage to, float alpha) {
        int width = from.getWidth();
        int height = from.getHeight();
        NativeImage blended = new NativeImage(width, height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int fromColor = from.getPixel(x, y);
                int toColor = to.getPixel(x, y);

                int red = lerp((fromColor >> 16) & 0xFF, (toColor >> 16) & 0xFF, alpha);
                int green = lerp((fromColor >> 8) & 0xFF, (toColor >> 8) & 0xFF, alpha);
                int blue = lerp(fromColor & 0xFF, toColor & 0xFF, alpha);
                int colorAlpha = lerp((fromColor >>> 24) & 0xFF, (toColor >>> 24) & 0xFF, alpha);

                blended.setPixel(x, y, (colorAlpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }

        return blended;
    }

    private int lerp(int from, int to, float alpha) {
        return from + Math.round((to - from) * alpha);
    }

    public void render(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        renderFullscreen(graphics, screenWidth, screenHeight);
    }

    public void renderFullscreen(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        if (DISABLED) {
            return;
        }

        uploadPendingFrame();

        if (textureLoader != null && dynamicTexture != null && videoWidth > 0 && videoHeight > 0) {
            float scale = Math.max((float) screenWidth / videoWidth, (float) screenHeight / videoHeight);
            int renderedWidth = Math.round(videoWidth * scale);
            int renderedHeight = Math.round(videoHeight * scale);
            int x = (screenWidth - renderedWidth) / 2;
            int y = (screenHeight - renderedHeight) / 2;
            renderTexture(graphics, x, y, renderedWidth, renderedHeight);
        }
    }

    public void renderRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height) {
        if (DISABLED) {
            return;
        }

        uploadPendingFrame();

        if (textureLoader != null && dynamicTexture != null) {
            renderTexture(graphics, Math.round(x), Math.round(y), Math.round(width), Math.round(height));
        }
    }

    private void renderTexture(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            textureLoader.res(),
            x,
            y,
            0,
            0,
            width,
            height,
            videoWidth,
            videoHeight,
            videoWidth,
            videoHeight
        );
    }

    private void uploadPendingFrame() {
        NativeImage image = frameToUpload.getAndSet(null);
        if (image == null) {
            return;
        }

        try {
            if (dynamicTexture == null) {
                dynamicTexture = new DynamicTexture(() -> video.debugId(), image);
                Minecraft.getInstance().getTextureManager().register(textureLocation.res(), dynamicTexture);
                textureLoader = textureLocation;
            } else {
                NativeImage oldPixels = dynamicTexture.getPixels();
                dynamicTexture.setPixels(image);
                dynamicTexture.upload();
                if (oldPixels != null && oldPixels != image) {
                    oldPixels.close();
                }
            }
        } catch (Exception exception) {
            image.close();
            releaseTexture();
            Java_reforged.LOGGER.error("Could not upload video frame for {}", video.debugId(), exception);
        }
    }

    public boolean hasFrame() {
        if (DISABLED) {
            return false;
        }
        return this.hasFrame.get();
    }

    @Override
    public void close() {
        isRunning.set(false);
        this.hasFrame.set(false);
        executor.shutdownNow();
        try {
            executor.awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        NativeImage first = firstFrameSnapshot;
        firstFrameSnapshot = null;
        if (first != null) first.close();

        NativeImage last = lastFrameSnapshot;
        lastFrameSnapshot = null;
        if (last != null) last.close();

        Runnable release = () -> {
            releaseTexture();
            NativeImage pending = frameToUpload.getAndSet(null);
            if (pending != null) pending.close();
        };
        if (RenderSystem.isOnRenderThread()) {
            release.run();
        } else {
            Minecraft.getInstance().execute(release);
        }
    }

    private void releaseTexture() {
        if (dynamicTexture != null) {
            Minecraft.getInstance().getTextureManager().release(textureLocation.res());
            dynamicTexture = null;
            textureLoader = null;
        }
    }

    public static final class Builder {
        private final VideoHolder video;
        private boolean loopEnabled = true;
        private int loopFadeMillis = 0;
        private boolean forceExtractOnCreate = false;

        public Builder(VideoHolder video) {
            this.video = video;
        }

        public Builder loop(boolean enabled) {
            this.loopEnabled = enabled;
            return this;
        }

        public Builder loopFade() {
            return loopFade(DEFAULT_LOOP_FADE_MS);
        }

        public Builder loopFade(int milliseconds) {
            this.loopFadeMillis = Math.max(0, milliseconds);
            this.loopEnabled = true;
            return this;
        }

        public Builder forceExtract(boolean forceExtractOnCreate) {
            this.forceExtractOnCreate = forceExtractOnCreate;
            return this;
        }

        public VideoRenderer build() {
            return new VideoRenderer(this);
        }
    }
}
