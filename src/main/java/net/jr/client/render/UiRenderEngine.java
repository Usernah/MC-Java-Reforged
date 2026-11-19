package net.jr.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.jr.api.client.render.font.FontHolder;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.video.VideoHolder;
import net.jr.client.meta.MetaManager;
import net.jr.client.meta.Meta;
import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.jr.client.render.state.AnimatedTextureRenderState;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.client.ui.render.composition.GuiCompositionScope;
import net.jr.client.video.VideoPlaybackEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.TiledBlitRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Util;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class UiRenderEngine {
    private static final Map<VideoCacheKey, VideoPlaybackEngine> VIDEO_RENDERER_CACHE = new ConcurrentHashMap<>();
    private static final Map<Asset, TextureSize> TEXTURE_SIZE_CACHE = new ConcurrentHashMap<>();
    private static final TextureSize UNKNOWN_TEXTURE_SIZE = new TextureSize(-1, -1);
    private UiRenderEngine() {}

    public enum CenterMode { STRETCH, REPEAT }
    public enum TextAlign { LEFT, CENTER, RIGHT }
    public enum AnimationMode {
        LOOP,
        PING_PONG;

        static AnimationMode parse(String mode) {
            return switch (mode.toLowerCase(Locale.ROOT).replace('-', '_')) {
                case "loop" -> LOOP;
                case "ping_pong" -> PING_PONG;
                default -> throw new IllegalArgumentException("Unsupported animation mode: " + mode);
            };
        }
    }
    public enum AnimUnit {
        MILLISECONDS(1), TICKS(50), SECONDS(1000), MINUTES(60_000);
        private final int milliseconds;
        AnimUnit(int milliseconds) { this.milliseconds = milliseconds; }
        public int toMillis(int value) { return Math.multiplyExact(value, milliseconds); }
        static AnimUnit parse(String unit) {
            return switch (unit.toLowerCase()) {
                case "ticks" -> TICKS; case "seconds" -> SECONDS; case "minutes" -> MINUTES;
                default -> MILLISECONDS;
            };
        }
    }

    public static ImageBuilder image(Asset texture, float x, float y, float width, float height) {
        return new ImageBuilder(texture, x, y, width, height);
    }
    public static ImageBuilder imageFromMeta(Asset texture, float x, float y, float width, float height) {
        return image(texture, x, y, width, height).fromMeta();
    }
    public static RectangleBuilder rectangle(float x, float y, float width, float height) {
        return new RectangleBuilder(x, y, width, height);
    }
    public static TextBuilder text(Component text) { return new TextBuilder(text, 0, 0); }
    public static TextBuilder text(Component text, int x, int y) { return new TextBuilder(text, x, y); }
    public static TextBuilder text(FormattedCharSequence text) { return new TextBuilder(text, 0, 0); }
    public static TextBuilder text(FormattedCharSequence text, int x, int y) {
        return new TextBuilder(text, x, y);
    }
    public static VideoBuilder video(VideoHolder video) { return new VideoBuilder(video); }
    public static void closeVideos() {
        VIDEO_RENDERER_CACHE.values().forEach(VideoPlaybackEngine::close);
        VIDEO_RENDERER_CACHE.clear();
    }

    public static class RectangleBuilder {
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        private int topColor = 0xFFFFFFFF;
        private int bottomColor = 0xFFFFFFFF;
        private boolean gradient;
        private float rotation;

        private RectangleBuilder(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public RectangleBuilder color(int color) {
            this.topColor = color;
            this.bottomColor = color;
            this.gradient = false;
            return this;
        }

        public RectangleBuilder color(int topColor, int bottomColor) {
            this.topColor = topColor;
            this.bottomColor = bottomColor;
            this.gradient = true;
            return this;
        }

        public RectangleBuilder rotation(float degrees) {
            this.rotation = degrees;
            return this;
        }

        public void draw(GuiGraphicsExtractor graphics) {
            Objects.requireNonNull(graphics, "graphics");
            if (this.width <= 0.0F || this.height <= 0.0F) {
                return;
            }

            int left = Math.round(this.x);
            int top = Math.round(this.y);
            int right = Math.round(this.x + this.width);
            int bottom = Math.round(this.y + this.height);
            if (right <= left || bottom <= top) {
                return;
            }

            if (this.rotation != 0.0F) {
                graphics.pose().pushMatrix();
                graphics.pose().rotateAbout(
                    (float)Math.toRadians(this.rotation),
                    this.x + this.width / 2.0F,
                    this.y + this.height / 2.0F
                );
            }
            try {
                if (this.gradient) {
                    graphics.fillGradient(
                        left,
                        top,
                        right,
                        bottom,
                        this.topColor,
                        this.bottomColor
                    );
                } else {
                    graphics.fill(left, top, right, bottom, this.topColor);
                }
            } finally {
                if (this.rotation != 0.0F) {
                    graphics.pose().popMatrix();
                }
            }
        }
    }

    public static class ImageBuilder {
        private final Asset texture;
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        private int u;
        private int v;
        private float uWidth = -1.0F;
        private float vHeight = -1.0F;
        private float atlasWidth = -1.0F;
        private float atlasHeight = -1.0F;

        private int tint = 0xFFFFFFFF;
        private float rotation;

        private boolean isNineSlice;
        private int nsTop;
        private int nsBottom;
        private int nsLeft;
        private int nsRight;
        private CenterMode nsCenterMode = CenterMode.STRETCH;

        private boolean isAnimated;
        private int animFrames = 1;
        private int animFrameWidth;
        private int animFrameHeight;
        private int animDuration;
        private AnimUnit animUnit = AnimUnit.MILLISECONDS;
        private AnimationMode animMode = AnimationMode.LOOP;
        private boolean explicitAnimMode;
        private boolean animInterpolate;
        private Map<Integer, Integer> animFrameDurations = Map.of();
        private long animStartMillis;

        private boolean useMeta;
        private Asset metaResource;
        private List<String> metaConfigIds = List.of();
        private boolean blur;
        private boolean antialiasing;
        private boolean activeLinearFiltering;
        private boolean pixelSnap;
        private boolean pixelCenterCorrection;

        public ImageBuilder(Asset texture, float x, float y, float width, float height) {
            this.texture = Objects.requireNonNull(texture, "texture");
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private ImageBuilder fromMeta() {
            this.useMeta = true;
            this.metaResource = null;
            return this;
        }

        public ImageBuilder meta(Asset metaResource) {
            this.useMeta = true;
            this.metaResource = Objects.requireNonNull(metaResource, "metaResource");
            return this;
        }

        public ImageBuilder config(String configId) {
            this.metaConfigIds = configId == null || configId.isBlank()
                ? List.of()
                : List.of(configId);
            this.useMeta = true;
            return this;
        }

        public ImageBuilder configs(List<String> configIds) {
            this.metaConfigIds = configIds == null
                ? List.of()
                : configIds.stream()
                    .filter(Objects::nonNull)
                    .filter(id -> !id.isBlank())
                    .toList();
            this.useMeta = true;
            return this;
        }

        public ImageBuilder uvCord(int u, int v) {
            this.u = u;
            this.v = v;
            return this;
        }

        public ImageBuilder uvSize(float uWidth, float vHeight) {
            this.uWidth = uWidth;
            this.vHeight = vHeight;
            return this;
        }

        public ImageBuilder atlasSize(float atlasWidth, float atlasHeight) {
            this.atlasWidth = atlasWidth;
            this.atlasHeight = atlasHeight;
            return this;
        }

        public ImageBuilder color(int color) {
            this.tint = color;
            return this;
        }

        public ImageBuilder alpha(float alpha) {
            int channel = Math.round(Math.clamp(alpha, 0.0F, 1.0F) * 255.0F);
            this.tint = channel << 24 | this.tint & 0xFFFFFF;
            return this;
        }

        public ImageBuilder rotation(float degrees) {
            this.rotation = degrees;
            return this;
        }

        public ImageBuilder nineSlice(int border) {
            return this.nineSlice(border, border, border, border, CenterMode.STRETCH);
        }

        public ImageBuilder nineSlice(int top, int bottom, int left, int right) {
            return this.nineSlice(top, bottom, left, right, CenterMode.STRETCH);
        }

        public ImageBuilder nineSlice(int top, int bottom, int left, int right, CenterMode centerMode) {
            this.isNineSlice = true;
            this.nsTop = top;
            this.nsBottom = bottom;
            this.nsLeft = left;
            this.nsRight = right;
            this.nsCenterMode = centerMode == null ? CenterMode.STRETCH : centerMode;
            return this;
        }

        public ImageBuilder animate(int frames, int frameWidth, int frameHeight, int duration, boolean interpolate) {
            return this.animate(frames, frameWidth, frameHeight, duration, AnimUnit.MILLISECONDS, interpolate);
        }

        public ImageBuilder animate(
            int frames,
            int frameWidth,
            int frameHeight,
            int duration,
            AnimUnit unit,
            boolean interpolate
        ) {
            this.isAnimated = true;
            this.animFrames = frames;
            this.animFrameWidth = frameWidth;
            this.animFrameHeight = frameHeight;
            this.animDuration = duration;
            this.animUnit = unit == null ? AnimUnit.MILLISECONDS : unit;
            this.animInterpolate = interpolate;
            return this;
        }

        public ImageBuilder frameDuration(int frame, int duration) {
            Map<Integer, Integer> copy = new LinkedHashMap<>(this.animFrameDurations);
            copy.put(frame, duration);
            this.animFrameDurations = Map.copyOf(copy);
            return this;
        }

        public ImageBuilder frameDurations(Map<Integer, Integer> frameDurations) {
            this.animFrameDurations = frameDurations == null ? Map.of() : Map.copyOf(frameDurations);
            return this;
        }

        public ImageBuilder animationStartMillis(long startMillis) {
            this.animStartMillis = startMillis;
            return this;
        }

        public ImageBuilder animationMode(AnimationMode mode) {
            this.animMode = mode == null ? AnimationMode.LOOP : mode;
            this.explicitAnimMode = true;
            return this;
        }

        public ImageBuilder blur(boolean blur) {
            this.blur = blur;
            return this;
        }

        public ImageBuilder antialiasing() {
            return this.antialiasing(true);
        }

        public ImageBuilder antialiasing(boolean enabled) {
            this.antialiasing = enabled;
            return this;
        }

        public ImageBuilder pixelSnap() {
            return this.pixelSnap(true);
        }

        public ImageBuilder pixelSnap(boolean enabled) {
            this.pixelSnap = enabled;
            return this;
        }

        public ImageBuilder pixelCenterCorrection() {
            return this.pixelCenterCorrection(true);
        }

        public ImageBuilder pixelCenterCorrection(boolean enabled) {
            this.pixelCenterCorrection = enabled;
            return this;
        }

        public void draw(GuiGraphicsExtractor graphics) {
            ResolvedImageState state = this.resolveState();

            float finalUWidth = state.uWidth == -1.0F ? state.atlasWidth : state.uWidth;
            float finalVHeight = state.vHeight == -1.0F ? state.atlasHeight : state.vHeight;
            float finalAtlasWidth = state.atlasWidth == -1.0F ? finalUWidth : state.atlasWidth;
            float finalAtlasHeight = state.atlasHeight == -1.0F ? finalVHeight : state.atlasHeight;

            boolean needsTextureDimensions = finalUWidth <= 0.0F
                || finalVHeight <= 0.0F
                || finalAtlasWidth <= 0.0F
                || finalAtlasHeight <= 0.0F;
            TextureSize textureSize = needsTextureDimensions
                ? UiRenderEngine.textureSize(this.texture)
                : UNKNOWN_TEXTURE_SIZE;
            if (finalUWidth <= 0.0F) {
                finalUWidth = textureSize.width();
            }
            if (finalVHeight <= 0.0F) {
                finalVHeight = textureSize.height();
            }
            if (finalAtlasWidth <= 0.0F) {
                finalAtlasWidth = textureSize.width();
            }
            if (finalAtlasHeight <= 0.0F) {
                finalAtlasHeight = textureSize.height();
            }
            if (finalUWidth <= 0.0F || finalVHeight <= 0.0F
                || finalAtlasWidth <= 0.0F || finalAtlasHeight <= 0.0F) {
                return;
            }

            float sourceWidth = state.isAnimated && state.animFrameWidth > 0
                ? state.animFrameWidth
                : finalUWidth;
            float sourceHeight = state.isAnimated && state.animFrameHeight > 0
                ? state.animFrameHeight
                : finalVHeight;
            this.activeLinearFiltering = state.blur
                || state.antialiasing && this.needsAdaptiveFiltering(state, sourceWidth, sourceHeight, textureSize);

            boolean pixelSnapApplied = this.applyPixelGeometry(graphics);

            if (this.rotation != 0.0F) {
                graphics.pose().pushMatrix();
                graphics.pose().rotateAbout(
                    (float) Math.toRadians(this.rotation),
                    this.x + this.width / 2.0F,
                    this.y + this.height / 2.0F
                );
            }

            try {
                if (state.isNineSlice) {
                    this.drawNineSlice(
                        graphics,
                        state,
                        finalUWidth,
                        finalVHeight,
                        finalAtlasWidth,
                        finalAtlasHeight
                    );
                } else if (state.isAnimated) {
                    this.drawAnimated(
                        graphics,
                        state,
                        finalAtlasWidth,
                        finalAtlasHeight
                    );
                } else {
                    this.blit(
                        graphics,
                        this.x,
                        this.y,
                        this.width,
                        this.height,
                        state.u,
                        state.v,
                        finalUWidth,
                        finalVHeight,
                        finalAtlasWidth,
                        finalAtlasHeight
                    );
                }
            } finally {
                if (this.rotation != 0.0F) {
                    graphics.pose().popMatrix();
                }
                if (pixelSnapApplied) {
                    graphics.pose().popMatrix();
                }
                this.activeLinearFiltering = false;
            }
        }

        private boolean applyPixelGeometry(GuiGraphicsExtractor graphics) {
            if ((!this.pixelSnap && !this.pixelCenterCorrection)
                || this.width == 0.0F || this.height == 0.0F) {
                return false;
            }

            double scaleX;
            double scaleY;
            ViewportGuiScale.Context viewportContext = ViewportGuiScale.activeOrNull();
            if (viewportContext != null) {
                scaleX = viewportContext.viewport().glWidth() / (double)viewportContext.logicalWidth();
                scaleY = viewportContext.viewport().glHeight() / (double)viewportContext.logicalHeight();
            } else {
                scaleX = Minecraft.getInstance().getWindow().getGuiScale();
                scaleY = scaleX;
            }
            if (!(scaleX > 0.0) || !(scaleY > 0.0)) {
                return false;
            }

            float sourceX = Math.round(this.x);
            float sourceY = Math.round(this.y);
            float sourceWidth = Math.round(this.width);
            float sourceHeight = Math.round(this.height);
            if (sourceWidth == 0.0F || sourceHeight == 0.0F) {
                return false;
            }

            float snappedX = this.pixelSnap ? snapToPhysicalPixel(this.x, scaleX) : sourceX;
            float snappedY = this.pixelSnap ? snapToPhysicalPixel(this.y, scaleY) : sourceY;
            float snappedWidth = this.pixelSnap ? snapToPhysicalPixel(this.width, scaleX) : sourceWidth;
            float snappedHeight = this.pixelSnap ? snapToPhysicalPixel(this.height, scaleY) : sourceHeight;
            if (this.pixelCenterCorrection) {
                snappedWidth -= 0.75F / (float)scaleX;
                snappedHeight -= 0.75F / (float)scaleY;
            }
            if (snappedWidth <= 0.0F || snappedHeight <= 0.0F) {
                return false;
            }
            if (nearlyEqual(sourceX, snappedX)
                && nearlyEqual(sourceY, snappedY)
                && nearlyEqual(sourceWidth, snappedWidth)
                && nearlyEqual(sourceHeight, snappedHeight)) {
                return false;
            }

            graphics.pose().pushMatrix();
            graphics.pose().translate(snappedX, snappedY);
            graphics.pose().scale(snappedWidth / sourceWidth, snappedHeight / sourceHeight);
            graphics.pose().translate(-sourceX, -sourceY);
            return true;
        }

        private static float snapToPhysicalPixel(float value, double physicalScale) {
            return (float)(Math.floor(value * physicalScale) / physicalScale);
        }

        private boolean needsAdaptiveFiltering(
            ResolvedImageState state,
            float sourceWidth,
            float sourceHeight,
            TextureSize textureSize
        ) {
            if (sourceWidth <= 0.0F || sourceHeight <= 0.0F) {
                return textureSize.isUnknown();
            }
            boolean scaled = !nearlyEqual(this.width, sourceWidth) || !nearlyEqual(this.height, sourceHeight);
            boolean fractionalGeometry = hasFractionalPart(this.x)
                || hasFractionalPart(this.y)
                || hasFractionalPart(this.width)
                || hasFractionalPart(this.height);
            boolean rotated = Math.abs(this.rotation) > 0.001F;
            boolean stretchedNineSlice = state.isNineSlice && scaled;
            return scaled || fractionalGeometry || rotated || stretchedNineSlice;
        }

        private ResolvedImageState resolveState() {
            ResolvedImageState state = new ResolvedImageState();
            state.u = this.u;
            state.v = this.v;
            state.uWidth = this.uWidth;
            state.vHeight = this.vHeight;
            state.atlasWidth = this.atlasWidth;
            state.atlasHeight = this.atlasHeight;
            state.isNineSlice = this.isNineSlice;
            state.nsTop = this.nsTop;
            state.nsBottom = this.nsBottom;
            state.nsLeft = this.nsLeft;
            state.nsRight = this.nsRight;
            state.nsCenterMode = this.nsCenterMode;
            state.isAnimated = this.isAnimated;
            state.animFrames = this.animFrames;
            state.animFrameWidth = this.animFrameWidth;
            state.animFrameHeight = this.animFrameHeight;
            state.animDuration = this.animDuration;
            state.animUnit = this.animUnit;
            state.animMode = this.animMode;
            state.animInterpolate = this.animInterpolate;
            state.animFrameDurations = new LinkedHashMap<>(this.animFrameDurations);
            state.animStartMillis = this.animStartMillis;
            state.blur = this.blur;
            state.antialiasing = this.antialiasing;
            state.metaScale = 1.0F;
            if (this.useMeta) {
                this.applyMeta(state);
            }
            return state;
        }

        private void applyMeta(ResolvedImageState state) {
            MetaManager manager = MetaManager.getInstance();
            Meta meta = (this.metaResource == null
                ? manager.getMeta(this.texture)
                : manager.getMetaResource(this.metaResource)).orElse(null);
            if (meta == null) {
                return;
            }

            Map<String, Object> data = this.metaConfigIds.isEmpty()
                ? meta.resolveData()
                : meta.resolveData(this.metaConfigIds);
            Meta.Atlas atlas = Meta.Atlas.from(data);
            Meta.ImageSize imageSize = Meta.ImageSize.from(data);
            state.metaScale = Meta.imageScale(data);

            state.u = atlas.x();
            state.v = atlas.y();
            if (atlas.width() > 0 && atlas.height() > 0) {
                state.uWidth = atlas.width();
                state.vHeight = atlas.height();
            }
            if (imageSize.width() > 0 && imageSize.height() > 0) {
                state.atlasWidth = imageSize.width();
                state.atlasHeight = imageSize.height();
            }

            if (data.containsKey("nine")) {
                Meta.NineSlice nine = Meta.NineSlice.from(data);
                state.isNineSlice = true;
                state.nsTop = nine.top();
                state.nsBottom = nine.bottom();
                state.nsLeft = nine.left();
                state.nsRight = nine.right();
                state.nsCenterMode = nine.repeatCenter() ? CenterMode.REPEAT : CenterMode.STRETCH;
            }

            if (data.containsKey("animation")) {
                Meta.Animation animation = Meta.Animation.from(data);
                state.isAnimated = true;
                state.animFrames = animation.frames();
                state.animFrameWidth = animation.frameWidth();
                state.animFrameHeight = animation.frameHeight();
                state.animDuration = animation.duration();
                state.animUnit = AnimUnit.parse(animation.unit());
                if (!this.explicitAnimMode) {
                    state.animMode = AnimationMode.parse(animation.mode());
                }
                state.animInterpolate = animation.interpolation();
                state.animFrameDurations = new LinkedHashMap<>(animation.frameDurations());
            }

            Object rawImage = data.get("image");
            if (rawImage instanceof Map<?, ?> image) {
                Object blurValue = image.get("blur");
                if (blurValue instanceof Boolean enabled) {
                    state.blur = enabled;
                }
                Object antialiasValue = image.get("antialiasing");
                if (!(antialiasValue instanceof Boolean)) {
                    antialiasValue = image.get("antialias");
                }
                if (antialiasValue instanceof Boolean enabled) {
                    state.antialiasing = enabled;
                }
            }
        }

        private void drawNineSlice(
            GuiGraphicsExtractor graphics,
            ResolvedImageState state,
            float sourceWidth,
            float sourceHeight,
            float atlasWidth,
            float atlasHeight
        ) {
            float drawTop = state.nsTop * state.metaScale;
            float drawBottom = state.nsBottom * state.metaScale;
            float drawLeft = state.nsLeft * state.metaScale;
            float drawRight = state.nsRight * state.metaScale;
            float centerSourceWidth = sourceWidth - state.nsLeft - state.nsRight;
            float centerSourceHeight = sourceHeight - state.nsTop - state.nsBottom;
            float centerDrawWidth = this.width - drawLeft - drawRight;
            float centerDrawHeight = this.height - drawTop - drawBottom;

            this.blit(graphics, this.x, this.y, drawLeft, drawTop,
                state.u, state.v, state.nsLeft, state.nsTop, atlasWidth, atlasHeight);
            this.blit(graphics, this.x + this.width - drawRight, this.y, drawRight, drawTop,
                state.u + sourceWidth - state.nsRight, state.v,
                state.nsRight, state.nsTop, atlasWidth, atlasHeight);
            this.blit(graphics, this.x, this.y + this.height - drawBottom, drawLeft, drawBottom,
                state.u, state.v + sourceHeight - state.nsBottom,
                state.nsLeft, state.nsBottom, atlasWidth, atlasHeight);
            this.blit(
                graphics,
                this.x + this.width - drawRight,
                this.y + this.height - drawBottom,
                drawRight,
                drawBottom,
                state.u + sourceWidth - state.nsRight,
                state.v + sourceHeight - state.nsBottom,
                state.nsRight,
                state.nsBottom,
                atlasWidth,
                atlasHeight
            );

            if (state.nsCenterMode == CenterMode.REPEAT) {
                this.repeatArea(graphics, this.x + drawLeft, this.y,
                    centerDrawWidth, drawTop,
                    centerSourceWidth * state.metaScale, drawTop,
                    state.u + state.nsLeft, state.v,
                    centerSourceWidth, state.nsTop, atlasWidth, atlasHeight);
                this.repeatArea(graphics, this.x + drawLeft, this.y + this.height - drawBottom,
                    centerDrawWidth, drawBottom,
                    centerSourceWidth * state.metaScale, drawBottom,
                    state.u + state.nsLeft, state.v + sourceHeight - state.nsBottom,
                    centerSourceWidth, state.nsBottom, atlasWidth, atlasHeight);
                this.repeatArea(graphics, this.x, this.y + drawTop,
                    drawLeft, centerDrawHeight,
                    drawLeft, centerSourceHeight * state.metaScale,
                    state.u, state.v + state.nsTop,
                    state.nsLeft, centerSourceHeight, atlasWidth, atlasHeight);
                this.repeatArea(graphics, this.x + this.width - drawRight, this.y + drawTop,
                    drawRight, centerDrawHeight,
                    drawRight, centerSourceHeight * state.metaScale,
                    state.u + sourceWidth - state.nsRight, state.v + state.nsTop,
                    state.nsRight, centerSourceHeight, atlasWidth, atlasHeight);
                this.repeatArea(graphics, this.x + drawLeft, this.y + drawTop,
                    centerDrawWidth, centerDrawHeight,
                    centerSourceWidth * state.metaScale, centerSourceHeight * state.metaScale,
                    state.u + state.nsLeft, state.v + state.nsTop,
                    centerSourceWidth, centerSourceHeight, atlasWidth, atlasHeight);
                return;
            }

            this.blit(graphics, this.x + drawLeft, this.y,
                centerDrawWidth, drawTop,
                state.u + state.nsLeft, state.v,
                centerSourceWidth, state.nsTop, atlasWidth, atlasHeight);
            this.blit(graphics, this.x + drawLeft, this.y + this.height - drawBottom,
                centerDrawWidth, drawBottom,
                state.u + state.nsLeft, state.v + sourceHeight - state.nsBottom,
                centerSourceWidth, state.nsBottom, atlasWidth, atlasHeight);
            this.blit(graphics, this.x, this.y + drawTop,
                drawLeft, centerDrawHeight,
                state.u, state.v + state.nsTop,
                state.nsLeft, centerSourceHeight, atlasWidth, atlasHeight);
            this.blit(graphics, this.x + this.width - drawRight, this.y + drawTop,
                drawRight, centerDrawHeight,
                state.u + sourceWidth - state.nsRight, state.v + state.nsTop,
                state.nsRight, centerSourceHeight, atlasWidth, atlasHeight);
            this.blit(graphics, this.x + drawLeft, this.y + drawTop,
                centerDrawWidth, centerDrawHeight,
                state.u + state.nsLeft, state.v + state.nsTop,
                centerSourceWidth, centerSourceHeight, atlasWidth, atlasHeight);
        }

        private void repeatArea(
            GuiGraphicsExtractor graphics,
            float x,
            float y,
            float width,
            float height,
            float tileDrawWidth,
            float tileDrawHeight,
            float u,
            float v,
            float tileSourceWidth,
            float tileSourceHeight,
            float atlasWidth,
            float atlasHeight
        ) {
            if (width <= 0.0F || height <= 0.0F || tileDrawWidth <= 0.0F || tileDrawHeight <= 0.0F
                || tileSourceWidth <= 0.0F || tileSourceHeight <= 0.0F
                || atlasWidth <= 0.0F || atlasHeight <= 0.0F) {
                return;
            }

            int drawX = Math.round(x);
            int drawY = Math.round(y);
            int drawWidth = Math.round(width);
            int drawHeight = Math.round(height);
            int resolvedTileWidth = Math.max(1, Math.round(tileDrawWidth));
            int resolvedTileHeight = Math.max(1, Math.round(tileDrawHeight));
            if (drawWidth <= 0 || drawHeight <= 0) {
                return;
            }

            var renderState = ((GuiGraphicsExtractorBridge) graphics).javaReforged$getGuiRenderState();
            var scissor = ((GuiGraphicsExtractorBridge) graphics).javaReforged$currentScissor();
            renderState.addGuiElement(new TiledBlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                this.textureSetup(),
                new Matrix3x2f(graphics.pose()),
                resolvedTileWidth,
                resolvedTileHeight,
                drawX,
                drawY,
                drawX + drawWidth,
                drawY + drawHeight,
                this.uvMin(u, tileSourceWidth, atlasWidth),
                this.uvMax(u, tileSourceWidth, atlasWidth),
                this.uvMin(v, tileSourceHeight, atlasHeight),
                this.uvMax(v, tileSourceHeight, atlasHeight),
                this.tint,
                scissor
            ));
        }

        private void drawAnimated(
            GuiGraphicsExtractor graphics,
            ResolvedImageState state,
            float atlasWidth,
            float atlasHeight
        ) {
            if (state.animFrames <= 0 || state.animFrameWidth <= 0 || state.animFrameHeight <= 0) {
                return;
            }
            AnimationFrame frame = this.animationFrame(state);
            float currentV = state.v + frame.index() * state.animFrameHeight;
            if (state.animInterpolate && state.animFrames > 1) {
                float nextV = state.v + frame.nextIndex() * state.animFrameHeight;
                this.blitInterpolated(
                    graphics,
                    this.x,
                    this.y,
                    this.width,
                    this.height,
                    state.u,
                    currentV,
                    state.u,
                    nextV,
                    state.animFrameWidth,
                    state.animFrameHeight,
                    atlasWidth,
                    atlasHeight,
                    frame.progress()
                );
                return;
            }
            this.blit(
                graphics,
                this.x,
                this.y,
                this.width,
                this.height,
                state.u,
                currentV,
                state.animFrameWidth,
                state.animFrameHeight,
                atlasWidth,
                atlasHeight
            );
        }

        private AnimationFrame animationFrame(ResolvedImageState state) {
            if (state.animFrames <= 1) {
                return new AnimationFrame(0, 0, 0.0F);
            }

            long cycle = 0L;
            for (int frame = 0; frame < state.animFrames; frame++) {
                cycle += this.frameDurationMillis(state, frame);
            }
            if (state.animMode == AnimationMode.PING_PONG) {
                for (int frame = state.animFrames - 2; frame > 0; frame--) {
                    cycle += this.frameDurationMillis(state, frame);
                }
            }
            if (cycle <= 0L) {
                return new AnimationFrame(0, 1, 0.0F);
            }

            long cursor = Math.floorMod(Util.getMillis() - state.animStartMillis, cycle);
            for (int frame = 0; frame < state.animFrames; frame++) {
                int duration = this.frameDurationMillis(state, frame);
                if (cursor < duration) {
                    return new AnimationFrame(
                        frame,
                        frame == state.animFrames - 1
                            ? (state.animMode == AnimationMode.PING_PONG ? frame - 1 : 0)
                            : frame + 1,
                        cursor / (float) duration
                    );
                }
                cursor -= duration;
            }
            if (state.animMode == AnimationMode.PING_PONG) {
                for (int frame = state.animFrames - 2; frame > 0; frame--) {
                    int duration = this.frameDurationMillis(state, frame);
                    if (cursor < duration) {
                        return new AnimationFrame(frame, frame - 1, cursor / (float) duration);
                    }
                    cursor -= duration;
                }
            }
            return new AnimationFrame(state.animFrames - 1, 0, 1.0F);
        }

        private int frameDurationMillis(ResolvedImageState state, int frame) {
            int duration = state.animFrameDurations.getOrDefault(frame, state.animDuration);
            return Math.max(1, state.animUnit.toMillis(duration));
        }

        private void blitInterpolated(
            GuiGraphicsExtractor graphics,
            float x,
            float y,
            float width,
            float height,
            float u,
            float v,
            float nextU,
            float nextV,
            float sourceWidth,
            float sourceHeight,
            float atlasWidth,
            float atlasHeight,
            float progress
        ) {
            if (width <= 0.0F || height <= 0.0F || sourceWidth <= 0.0F || sourceHeight <= 0.0F
                || atlasWidth <= 0.0F || atlasHeight <= 0.0F) {
                return;
            }

            int drawX = Math.round(x);
            int drawY = Math.round(y);
            int drawWidth = Math.round(width);
            int drawHeight = Math.round(height);
            if (drawWidth <= 0 || drawHeight <= 0) {
                return;
            }

            var renderState = ((GuiGraphicsExtractorBridge) graphics).javaReforged$getGuiRenderState();
            var scissor = ((GuiGraphicsExtractorBridge) graphics).javaReforged$currentScissor();
            renderState.addGuiElement(new AnimatedTextureRenderState(
                this.textureSetup(),
                new Matrix3x2f(graphics.pose()),
                drawX,
                drawY,
                drawX + drawWidth,
                drawY + drawHeight,
                this.uvMin(u, sourceWidth, atlasWidth),
                this.uvMax(u, sourceWidth, atlasWidth),
                this.uvMin(v, sourceHeight, atlasHeight),
                this.uvMax(v, sourceHeight, atlasHeight),
                Math.round(nextU),
                Math.round(nextU + sourceWidth),
                Math.round(nextV),
                Math.round(nextV + sourceHeight),
                Math.clamp(progress, 0.0F, 1.0F),
                this.tint,
                scissor
            ));
        }

        private void blit(
            GuiGraphicsExtractor graphics,
            float x,
            float y,
            float width,
            float height,
            float u,
            float v,
            float sourceWidth,
            float sourceHeight,
            float atlasWidth,
            float atlasHeight
        ) {
            if (width <= 0.0F || height <= 0.0F || sourceWidth <= 0.0F || sourceHeight <= 0.0F
                || atlasWidth <= 0.0F || atlasHeight <= 0.0F) {
                return;
            }

            int drawX = Math.round(x);
            int drawY = Math.round(y);
            int drawWidth = Math.round(width);
            int drawHeight = Math.round(height);
            if (drawWidth <= 0 || drawHeight <= 0) {
                return;
            }

            var renderState = ((GuiGraphicsExtractorBridge) graphics).javaReforged$getGuiRenderState();
            var scissor = ((GuiGraphicsExtractorBridge) graphics).javaReforged$currentScissor();
            renderState.addGuiElement(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                this.textureSetup(),
                new Matrix3x2f(graphics.pose()),
                drawX,
                drawY,
                drawX + drawWidth,
                drawY + drawHeight,
                this.uvMin(u, sourceWidth, atlasWidth),
                this.uvMax(u, sourceWidth, atlasWidth),
                this.uvMin(v, sourceHeight, atlasHeight),
                this.uvMax(v, sourceHeight, atlasHeight),
                this.tint,
                scissor
            ));
        }

        private TextureSetup textureSetup() {
            var nativeTexture = Minecraft.getInstance().getTextureManager().getTexture(this.texture.res());
            var sampler = this.activeLinearFiltering
                ? RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
                : nativeTexture.getSampler();
            return TextureSetup.singleTexture(nativeTexture.getTextureView(), sampler);
        }

        private float uvMin(float coordinate, float size, float atlasSize) {
            return (coordinate + this.edgeInset(size)) / atlasSize;
        }

        private float uvMax(float coordinate, float size, float atlasSize) {
            return (coordinate + size - this.edgeInset(size)) / atlasSize;
        }

        private float edgeInset(float size) {
            if (!this.activeLinearFiltering || size <= 0.0F) {
                return 0.0F;
            }
            return Math.min(0.5F, size * 0.5F);
        }

        private static boolean nearlyEqual(float first, float second) {
            return Math.abs(first - second) <= 0.001F;
        }

        private static boolean hasFractionalPart(float value) {
            return Math.abs(value - Math.round(value)) > 0.001F;
        }

        private static final class ResolvedImageState {
            private int u;
            private int v;
            private float uWidth;
            private float vHeight;
            private float atlasWidth;
            private float atlasHeight;
            private boolean isNineSlice;
            private int nsTop;
            private int nsBottom;
            private int nsLeft;
            private int nsRight;
            private CenterMode nsCenterMode;
            private boolean isAnimated;
            private int animFrames;
            private int animFrameWidth;
            private int animFrameHeight;
            private int animDuration;
            private AnimUnit animUnit;
            private AnimationMode animMode;
            private boolean animInterpolate;
            private Map<Integer, Integer> animFrameDurations = Map.of();
            private long animStartMillis;
            private boolean blur;
            private boolean antialiasing;
            private float metaScale = 1.0F;
        }

        private record AnimationFrame(int index, int nextIndex, float progress) {
        }
    }

    private static TextureSize textureSize(Asset texture) {
        return TEXTURE_SIZE_CACHE.computeIfAbsent(texture, UiRenderEngine::loadTextureSize);
    }

    private static TextureSize loadTextureSize(Asset texture) {
        Optional<InputStream> opened = texture.getAsStream();
        if (opened.isPresent()) {
            try (InputStream input = opened.get(); NativeImage image = NativeImage.read(input)) {
                return new TextureSize(image.getWidth(), image.getHeight());
            } catch (Exception ignored) {
            }
        }

        try {
            var nativeTexture = Minecraft.getInstance().getTextureManager().getTexture(texture.res());
            var gpuTexture = nativeTexture.getTexture();
            if (gpuTexture != null) {
                return new TextureSize(gpuTexture.getWidth(0), gpuTexture.getHeight(0));
            }
        } catch (Exception ignored) {
        }
        return UNKNOWN_TEXTURE_SIZE;
    }

    private record TextureSize(int width, int height) {
        private boolean isUnknown() {
            return this.width <= 0 || this.height <= 0;
        }
    }

    public static class TextBuilder {
        private final Component componentText;
        private final FormattedCharSequence charSequenceText;
        private int x;
        private int y;
        private float scale = 1.0F;
        private int color = 0xFFFFFFFF;
        private boolean shadow;
        private boolean legacyShadow;
        private Integer shadowColor;
        private Integer outlineColor;
        private TextAlign align = TextAlign.LEFT;
        private Asset fontLocation;
        private float wrapWidth = Float.POSITIVE_INFINITY;
        private float maxHeight = Float.POSITIVE_INFINITY;
        private int maxLines;
        private float lineSpacing;

        public TextBuilder(Component text, int x, int y) {
            this.componentText = Objects.requireNonNull(text, "text");
            this.charSequenceText = null;
            this.x = x;
            this.y = y;
        }

        public TextBuilder(FormattedCharSequence text, int x, int y) {
            this.componentText = null;
            this.charSequenceText = Objects.requireNonNull(text, "text");
            this.x = x;
            this.y = y;
        }

        public TextBuilder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public TextBuilder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public TextBuilder x(int x) {
            this.x = x;
            return this;
        }

        public TextBuilder y(int y) {
            this.y = y;
            return this;
        }

        public int x() {
            return this.x;
        }

        public int y() {
            return this.y;
        }

        public TextBuilder color(int a, int r, int g, int b) {
            this.color = (a & 0xFF) << 24
                | (r & 0xFF) << 16
                | (g & 0xFF) << 8
                | b & 0xFF;
            return this;
        }

        public TextBuilder color(int color) {
            this.color = color;
            return this;
        }

        public TextBuilder shadow(boolean shadow) {
            this.shadow = shadow;
            return this;
        }

        public TextBuilder legacyShadow(boolean legacyShadow) {
            this.legacyShadow = legacyShadow;
            return this;
        }

        public TextBuilder shadowColor(int a, int r, int g, int b) {
            return this.shadowColor((a & 0xFF) << 24
                | (r & 0xFF) << 16
                | (g & 0xFF) << 8
                | b & 0xFF);
        }

        public TextBuilder shadowColor(int shadowColor) {
            this.shadowColor = shadowColor;
            this.shadow = true;
            return this;
        }

        public TextBuilder outline(int outlineColor) {
            this.outlineColor = outlineColor;
            return this;
        }

        public TextBuilder align(TextAlign align) {
            this.align = align == null ? TextAlign.LEFT : align;
            return this;
        }

        public TextBuilder font(Asset font) {
            this.fontLocation = font;
            return this;
        }

        public TextBuilder font(FontHolder font) {
            this.fontLocation = font == null ? null : font.location();
            return this;
        }

        public TextBuilder wrap(float width) {
            this.wrapWidth = Math.max(0.0F, width);
            return this;
        }

        public TextBuilder maxHeight(float height) {
            this.maxHeight = Math.max(0.0F, height);
            return this;
        }

        public TextBuilder maxLines(int maxLines) {
            this.maxLines = Math.max(0, maxLines);
            return this;
        }

        public TextBuilder lineSpacing(float lineSpacing) {
            this.lineSpacing = Math.max(0.0F, lineSpacing);
            return this;
        }

        public int width() {
            if (this.usesInlineLayout()) {
                return Math.round(this.inlineLayout().width());
            }
            return Math.round(this.rawWidth() * this.scale);
        }

        public int lineHeight() {
            if (this.usesInlineLayout()) {
                return Math.round(this.inlineLayout().lineHeight());
            }
            return Math.round(Minecraft.getInstance().font.lineHeight * this.scale);
        }

        public int height() {
            if (this.usesInlineLayout()) {
                return Math.round(this.inlineLayout().height());
            }
            return Math.round(this.rawHeight() * this.scale);
        }

        public int lineCount() {
            if (this.usesInlineLayout()) {
                return this.inlineLayout().lineCount();
            }
            return this.componentText == null
                    ? 1
                    : Math.max(1, this.resolvedComponentLines().size());
        }

        public void draw(GuiGraphicsExtractor graphics) {
            if (this.usesInlineLayout()) {
                this.inlineLayout().draw(graphics, this.x, this.y, this.align);
                return;
            }

            Font font = Minecraft.getInstance().font;
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.x, this.y);
            graphics.pose().scale(this.scale, this.scale);

            try {
                if (this.componentText != null) {
                    List<Component> lines = this.resolvedComponentLines();
                    for (int index = 0; index < lines.size(); index++) {
                        Component line = lines.get(index);
                        int lineWidth = font.width(line);
                        int offsetX = Math.round(this.offsetX(lineWidth));
                        int offsetY = index * font.lineHeight;
                        if (this.outlineColor != null) {
                            this.drawOutlinedText(
                                graphics,
                                font,
                                line.getVisualOrderText(),
                                offsetX,
                                offsetY
                            );
                        } else {
                            this.drawText(
                                graphics,
                                font,
                                line,
                                offsetX,
                                offsetY
                            );
                        }
                    }
                } else {
                    FormattedCharSequence sequence = this.styledSequence();
                    int lineWidth = font.width(sequence);
                    int offsetX = Math.round(this.offsetX(lineWidth));
                    if (this.outlineColor != null) {
                        this.drawOutlinedText(graphics, font, sequence, offsetX, 0);
                    } else {
                        this.drawText(graphics, font, sequence, offsetX, 0);
                    }
                }
            } finally {
                graphics.pose().popMatrix();
            }
        }

        private int rawWidth() {
            Font font = Minecraft.getInstance().font;
            if (this.componentText != null) {
                int width = 0;
                for (Component line : this.resolvedComponentLines()) {
                    width = Math.max(width, font.width(line));
                }
                return width;
            }
            return font.width(this.styledSequence());
        }

        private int rawHeight() {
            Font font = Minecraft.getInstance().font;
            if (this.componentText != null) {
                return Math.max(1, this.resolvedComponentLines().size()) * font.lineHeight;
            }
            return font.lineHeight;
        }

        private List<Component> resolvedComponentLines() {
            return this.splitComponentByLines(this.styledComponent());
        }

        private Component styledComponent() {
            Component result = this.componentText;
            if (this.fontLocation != null) {
                result = result.copy().withStyle(style -> style.withFont(this.fontLocation.asFontDescription()));
            }
            if (this.shadowColor != null && !this.legacyShadow) {
                result = result.copy().withStyle(style -> style.withShadowColor(this.shadowColor));
            }
            return result;
        }

        private FormattedCharSequence styledSequence() {
            if (this.fontLocation == null && this.shadowColor == null && !this.legacyShadow) {
                return this.charSequenceText;
            }
            return sink -> this.charSequenceText.accept((index, style, codePoint) -> {
                Style resolvedStyle = style;
                if (this.fontLocation != null) {
                    resolvedStyle = resolvedStyle.withFont(this.fontLocation.asFontDescription());
                }
                if (this.legacyShadow) {
                    resolvedStyle = resolvedStyle.withoutShadow();
                } else if (this.shadowColor != null) {
                    resolvedStyle = resolvedStyle.withShadowColor(this.shadowColor);
                }
                return sink.accept(index, resolvedStyle, codePoint);
            });
        }

        private void drawOutlinedText(
            GuiGraphicsExtractor graphics,
            Font font,
            FormattedCharSequence text,
            int x,
            int y
        ) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    if (offsetX != 0 || offsetY != 0) {
                        graphics.text(font, text, x + offsetX, y + offsetY, this.outlineColor, false);
                    }
                }
            }
            graphics.text(font, text, x, y, this.color, false);
        }

        private void drawText(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int x,
            int y
        ) {
            if (this.legacyShadow) {
                FormattedCharSequence textWithoutShadow = this.withoutShadow(text.getVisualOrderText());
                this.drawLegacyShadow(graphics, font, textWithoutShadow, x, y);
                graphics.text(font, textWithoutShadow, x, y, this.color, false);
                return;
            }
            graphics.text(font, text, x, y, this.color, this.shadow);
        }

        private void drawText(
            GuiGraphicsExtractor graphics,
            Font font,
            FormattedCharSequence text,
            int x,
            int y
        ) {
            if (this.legacyShadow) {
                this.drawLegacyShadow(graphics, font, text, x, y);
            }
            graphics.text(font, text, x, y, this.color, this.shadow && !this.legacyShadow);
        }

        private void drawLegacyShadow(
            GuiGraphicsExtractor graphics,
            Font font,
            FormattedCharSequence text,
            int x,
            int y
        ) {
            float offset = this.resolvedLegacyShadowOffset();
            graphics.pose().pushMatrix();
            graphics.pose().translate(offset, offset);
            graphics.text(font, text, x, y, this.resolvedShadowColor(), false);
            graphics.pose().popMatrix();
        }

        private float resolvedLegacyShadowOffset() {
            double pixelDensity;
            GuiCompositionScope.Extraction extraction =
                GuiCompositionScope.activeExtractionOrNull();
            if (extraction != null) {
                pixelDensity = extraction.rasterScale();
            } else {
                ViewportGuiScale.Context viewportContext =
                    ViewportGuiScale.activeOrNull();
                pixelDensity = viewportContext != null
                    ? viewportContext.viewport().glWidth()
                        / (double)viewportContext.logicalWidth()
                    : Minecraft.getInstance().getWindow().getGuiScale();
            }

            double combinedScale = Math.abs(this.scale) * pixelDensity;
            if (!Double.isFinite(combinedScale) || combinedScale <= 0.0D) {
                return 0.5F;
            }
            int physicalOffset = Math.max(
                1,
                (int)Math.round(0.5D * combinedScale)
            );
            return (float)(physicalOffset / combinedScale);
        }

        private FormattedCharSequence withoutShadow(FormattedCharSequence text) {
            return sink -> text.accept((index, style, codePoint) ->
                sink.accept(index, style.withoutShadow(), codePoint)
            );
        }

        private int resolvedShadowColor() {
            if (this.shadowColor != null) {
                return this.shadowColor;
            }
            return this.color & 0xFF000000 | (this.color & 0x00FCFCFC) >> 2;
        }

        private boolean hasInlineMarkers() {
            return this.componentText != null
                    && InlineTextRenderer.containsMarker(this.componentText);
        }

        private boolean usesInlineLayout() {
            return this.componentText != null
                    && (this.hasInlineMarkers() || Float.isFinite(this.wrapWidth));
        }

        private InlineTextRenderer.Layout inlineLayout() {
            return InlineTextRenderer.layout(
                    this.componentText,
                    new InlineTextRenderer.TextStyle(
                            this.scale,
                            this.color,
                            this.shadow,
                            this.legacyShadow,
                            this.shadowColor,
                            this.outlineColor,
                            this.fontLocation,
                            this.componentText.getStyle()
                    ),
                    this.wrapWidth,
                    this.maxHeight,
                    this.maxLines,
                    this.lineSpacing
            );
        }

        private float offsetX(int lineWidth) {
            return switch (this.align) {
                case CENTER -> -lineWidth / 2.0F;
                case RIGHT -> -lineWidth;
                case LEFT -> 0.0F;
            };
        }

        private List<Component> splitComponentByLines(Component text) {
            List<Component> lines = new ArrayList<>();
            if (!text.getString().contains("\n")) {
                lines.add(text);
                return lines;
            }
            String[] split = text.getString().split("\\n", -1);
            for (String line : split) {
                lines.add(Component.literal(line).setStyle(text.getStyle()));
            }
            return lines;
        }
    }

    public static class VideoBuilder {
        private final VideoHolder video;
        private float x, y, width, height;
        private boolean fullscreen = true;
        private boolean loop = true;
        private int loopFadeMillis;
        public VideoBuilder(VideoHolder video) { this.video = Objects.requireNonNull(video, "video"); }
        public VideoBuilder rect(float x, float y, float width, float height) {
            this.x=x; this.y=y; this.width=width; this.height=height; fullscreen=false; return this;
        }
        public VideoBuilder fullscreen() { fullscreen = true; return this; }
        public VideoBuilder loop() { return loop(true); }
        public VideoBuilder loop(boolean value) { loop = value; return this; }
        public VideoBuilder loopFade(int milliseconds) {
            loopFadeMillis = Math.max(0, milliseconds);
            if (loopFadeMillis > 0) loop = true;
            return this;
        }
        public void draw(GuiGraphicsExtractor graphics) {
            VideoPlaybackEngine renderer = VIDEO_RENDERER_CACHE.computeIfAbsent(
                new VideoCacheKey(video, loop, loopFadeMillis),
                key -> {
                    VideoPlaybackEngine.Builder builder = VideoPlaybackEngine.builder(video).loop(loop);
                    if (loopFadeMillis > 0) builder.loopFade(loopFadeMillis);
                    return builder.build();
                }
            );
            if (fullscreen) {
                var window = Minecraft.getInstance().getWindow();
                renderer.renderFullscreen(graphics, window.getGuiScaledWidth(), window.getGuiScaledHeight());
            } else renderer.renderRect(graphics, x, y, width, height);
        }
    }
    private record VideoCacheKey(VideoHolder video, boolean loop, int loopFadeMillis) {}
}
