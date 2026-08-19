package net.jr.client.ui;

import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.dsl.UiCompiledDocument;
import net.jr.api.client.ui.dsl.UiCompiledDocument.AnimationDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.ConditionalRenderableDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.ImageDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.NineDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.RenderableDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.TextDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.Vector2Definition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.VideoDefinition;
import net.jr.api.client.ui.dsl.UiRenderLayer;
import net.jr.api.client.video.VideoHolder;
import net.jr.api.client.video.VideoRegister;
import net.jr.client.components.elements.ImageElement;
import net.jr.client.components.elements.TextElement;
import net.jr.client.components.elements.VideoElement;
import net.jr.client.ui.layout.UILayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class UiElement {
    private UiElement() {
    }

    public static List<ElementRenderable> renderables(List<RenderableDefinition> definitions) {
        return definitions.stream().map(UiElement::renderable).toList();
    }

    private static ElementRenderable renderable(RenderableDefinition definition) {
        if (definition instanceof ConditionalRenderableDefinition conditional) {
            return new ConditionalElement(conditional);
        }
        if (definition instanceof ImageDefinition image) {
            return new ElementImage(image);
        }
        if (definition instanceof VideoDefinition video) {
            return new ElementVideo(video);
        }
        if (definition instanceof TextDefinition text) {
            return new ElementText(text);
        }
        throw new IllegalArgumentException("Unsupported compiled renderable: " + definition.getClass().getName());
    }

    private static final class ConditionalElement implements ElementRenderable {
        private final ConditionalRenderableDefinition definition;
        private final ElementRenderable renderable;

        private ConditionalElement(ConditionalRenderableDefinition definition) {
            this.definition = definition;
            this.renderable = UiElement.renderable(definition.renderable());
        }

        @Override
        public String id() {
            return this.definition.id();
        }

        @Override
        public void render(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            if (this.definition.condition().resolve(context)) {
                this.renderable.render(graphics, context, originX, originY);
            }
        }

        @Override
        public void close() {
            this.renderable.close();
        }
    }

    public interface ElementRenderable extends AutoCloseable {
        String id();

        void render(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        );

        @Override
        default void close() {
        }
    }

    public static final class Layer {
        private final UiRenderLayer type;
        private final UILayout host;
        private final List<ElementRenderable> renderables;

        public Layer(UiRenderLayer type, UILayout currentLayout) {
            this.type = Objects.requireNonNull(type, "type");
            this.host = Objects.requireNonNull(currentLayout, "currentLayout");
            this.renderables = UiElement.renderables(this.host.uiDocument().renderables(this.type));
            this.host.attachUiLayer(this);
        }

        public UiRenderLayer type() {
            return this.type;
        }

        public void render(GuiGraphicsExtractor graphics) {
            for (ElementRenderable renderable : this.renderables) {
                renderable.render(graphics, this.host.uiData(), 0.0F, 0.0F);
            }
        }

        public void close() {
            for (ElementRenderable renderable : this.renderables) {
                renderable.close();
            }
        }
    }

    public static final class ElementImage implements ElementRenderable {
        private final ImageDefinition definition;
        private final ImageElement image;

        private ElementImage(ImageDefinition definition) {
            this.definition = definition;
            this.image = new ImageElement(Asset.MOD("textures/missing.png"), 0, 0, 0, 0, null);
        }

        @Override
        public String id() {
            return this.definition.id();
        }

        @Override
        public void render(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            this.image.setTexture(this.definition.texture().resolve(context));
            this.image.setBounds(
                originX + this.definition.x().resolve(context),
                originY + this.definition.y().resolve(context),
                this.definition.width().resolve(context),
                this.definition.height().resolve(context)
            );

            this.applyTextureRegion(context);
            this.applyMeta(context);
            this.applyNine(context);
            this.applyAnimation(context);
            this.image.draw(graphics);
        }

        private void applyTextureRegion(UiCompiledDocument.DataContext context) {
            Vector2Definition uv = this.definition.uv();
            if (uv == null) {
                this.image.clearUvCord();
            } else {
                this.image.setUvCord(
                    Math.round(uv.first().resolve(context)),
                    Math.round(uv.second().resolve(context))
                );
            }

            Vector2Definition uvSize = this.definition.uvSize();
            if (uvSize == null) {
                this.image.clearUvSize();
            } else {
                this.image.setUvSize(
                    uvSize.first().resolve(context),
                    uvSize.second().resolve(context)
                );
            }

            Vector2Definition atlasSize = this.definition.atlasSize();
            if (atlasSize == null) {
                this.image.clearAtlasSize();
            } else {
                this.image.setAtlasSize(
                    atlasSize.first().resolve(context),
                    atlasSize.second().resolve(context)
                );
            }
        }

        private void applyMeta(UiCompiledDocument.DataContext context) {
            boolean useMeta = this.definition.useMeta().resolve(context);
            this.image.setUseMeta(useMeta);
            if (this.definition.metaConfig() == null) {
                this.image.clearConfig();
                return;
            }
            String config = this.definition.metaConfig().resolve(context);
            if (config == null || config.isBlank()) {
                this.image.clearConfig();
            } else {
                this.image.setConfig(config);
            }
        }

        private void applyNine(UiCompiledDocument.DataContext context) {
            NineDefinition nine = this.definition.nine();
            if (nine == null) {
                this.image.clearNineSlice();
                return;
            }
            this.image.setNineSlice(
                nine.top().resolve(context),
                nine.bottom().resolve(context),
                nine.left().resolve(context),
                nine.right().resolve(context),
                nine.centerMode().resolve(context)
            );
        }

        private void applyAnimation(UiCompiledDocument.DataContext context) {
            AnimationDefinition animation = this.definition.animation();
            if (animation == null) {
                this.image.clearAnimation();
                return;
            }
            int frameWidth = Math.round(animation.frameSize().first().resolve(context));
            int frameHeight = Math.round(animation.frameSize().second().resolve(context));
            this.image.setAnimation(
                animation.frames().resolve(context),
                frameWidth,
                frameHeight,
                animation.duration().resolve(context),
                animation.unit().resolve(context),
                animation.interpolation().resolve(context)
            );
            Map<Integer, Integer> durations = new LinkedHashMap<>();
            for (int frame = 0; frame < animation.frameDurations().size(); frame++) {
                durations.put(frame, animation.frameDurations().get(frame).resolve(context));
            }
            this.image.setFrameDurations(durations);
        }
    }

    public static final class ElementVideo implements ElementRenderable {
        private final VideoDefinition definition;
        private VideoElement video;

        private ElementVideo(VideoDefinition definition) {
            this.definition = definition;
        }

        @Override
        public String id() {
            return this.definition.id();
        }

        @Override
        public void render(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            VideoHolder holder = VideoRegister.require(this.definition.source().resolve(context));
            if (this.video == null) {
                this.video = new VideoElement(holder, 0.0F, 0.0F, 0.0F, 0.0F, null);
            } else {
                this.video.setVideo(holder);
            }
            this.video.setBounds(
                originX + this.definition.x().resolve(context),
                originY + this.definition.y().resolve(context),
                this.definition.width().resolve(context),
                this.definition.height().resolve(context)
            );
            this.video.setFullscreen(this.definition.fullscreen().resolve(context));
            this.video.setPlayback(
                this.definition.loop().resolve(context),
                this.definition.loopFadeMillis().resolve(context)
            );
            this.video.draw(graphics);
        }

        @Override
        public void close() {
            if (this.video != null) {
                this.video.close();
                this.video = null;
            }
        }
    }

    public static final class ElementText implements ElementRenderable {
        private final TextDefinition definition;
        private final TextElement text;

        private ElementText(TextDefinition definition) {
            this.definition = definition;
            this.text = new TextElement(Component.empty(), 0.0F, 0.0F, null);
        }

        @Override
        public String id() {
            return this.definition.id();
        }

        @Override
        public void render(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            Component content = this.definition.literal() != null
                ? Component.literal(this.definition.literal().resolve(context))
                : Component.translatable(this.definition.translatable().resolve(context));
            this.text.setText(content);
            this.text.setPosition(
                originX + this.definition.x().resolve(context),
                originY + this.definition.y().resolve(context)
            );
            this.text.setScale(this.definition.scale().resolve(context));
            this.text.setColor(this.definition.color().resolve(context));
            this.text.setShadow(this.definition.shadow().resolve(context));
            if (this.definition.shadowColor() == null) {
                this.text.clearShadowColor();
            } else {
                this.text.setShadowColor(this.definition.shadowColor().resolve(context));
            }
            if (this.definition.outline() == null) {
                this.text.clearOutline();
            } else {
                this.text.setOutline(this.definition.outline().resolve(context));
            }
            this.text.setAlign(this.definition.align().resolve(context));
            if (this.definition.font() == null) {
                this.text.clearFont();
            } else {
                this.text.setFont(this.definition.font().resolve(context));
            }
            this.text.draw(graphics);
        }
    }
}
