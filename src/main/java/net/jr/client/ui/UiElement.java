package net.jr.client.ui;

import net.jr.api.client.resource.Asset;
import net.jr.client.ui.dsl.UiCompiledDocument;
import net.jr.client.ui.dsl.UiCompiledDocument.AnimationDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.ConditionalRenderableDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.ImageDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.NineDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.RenderableDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.RectangleDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.TextDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.TextBoxDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.Vector2Definition;
import net.jr.client.ui.dsl.UiCompiledDocument.VideoDefinition;
import net.jr.api.client.video.VideoHolder;
import net.jr.api.client.video.VideoRegister;
import net.jr.client.components.elements.ImageElement;
import net.jr.client.components.elements.FillColorElement;
import net.jr.client.components.elements.TextElement;
import net.jr.client.components.elements.TextBoxElement;
import net.jr.client.components.elements.VideoElement;
import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.components.elements.VisualElementsInterface;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UiElement {
    private UiElement() {
    }

    public static List<ElementRenderable> renderables(List<RenderableDefinition> definitions) {
        return definitions.stream().map(UiElement::renderable).toList();
    }

    public static ElementRenderable renderable(RenderableDefinition definition) {
        if (definition instanceof ConditionalRenderableDefinition conditional) {
            return new ConditionalElement(conditional);
        }
        if (definition instanceof ImageDefinition image) {
            return new ElementImage(image);
        }
        if (definition instanceof VideoDefinition video) {
            return new ElementVideo(video);
        }
        if (definition instanceof RectangleDefinition rectangle) {
            return new ElementRectangle(rectangle);
        }
        if (definition instanceof TextDefinition text) {
            return new ElementText(text);
        }
        if (definition instanceof TextBoxDefinition textBox) {
            return new ElementTextBox(textBox);
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
        public void prepare(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            this.renderable.prepare(graphics, context, originX, originY);
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
        public Object dslValue(String property) {
            return this.renderable.dslValue(property);
        }

        @Override
        public void setLayoutParent(VisualElementsInterface parent) {
            this.renderable.setLayoutParent(parent);
        }

        @Override
        public void close() {
            this.renderable.close();
        }
    }

    public interface ElementRenderable extends AutoCloseable {
        String id();

        void prepare(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        );

        void render(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        );

        Object dslValue(String property);

        default void setLayoutParent(VisualElementsInterface parent) {
        }

        @Override
        default void close() {
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
        public void prepare(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            this.image.setTexture(this.definition.texture().resolve(context));
            VisualAnchor anchor = this.definition.anchor().resolve(context);
            this.image.setAnchor(anchor);
            this.image.setAlpha(this.definition.alpha().resolve(context));
            this.image.setBounds(
                position(anchor, originX, this.definition.x().resolve(context)),
                position(anchor, originY, this.definition.y().resolve(context)),
                this.definition.width().resolve(context),
                this.definition.height().resolve(context)
            );

            this.applyTextureRegion(context);
            this.applyMeta(context);
            this.applyNine(context);
            this.applyAnimation(context);
        }

        @Override
        public void render(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            this.prepare(graphics, context, originX, originY);
            this.image.draw(graphics);
        }

        @Override
        public Object dslValue(String property) {
            return this.image.dslValue(property);
        }

        @Override
        public void setLayoutParent(VisualElementsInterface parent) {
            this.image.setLayoutParent(parent);
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
            Object meta = this.definition.meta().resolve(context);
            if (meta instanceof Asset metaResource) {
                this.image.setMeta(metaResource);
            } else if (meta instanceof Boolean useMeta) {
                this.image.setMeta(useMeta);
            } else {
                throw new IllegalArgumentException(
                    "Image meta must resolve to a boolean or Asset, got "
                        + (meta == null ? "null" : meta.getClass().getSimpleName())
                );
            }
            if (this.definition.metaConfigs().isEmpty()) {
                this.image.clearConfig();
                return;
            }
            List<String> configs = this.definition.metaConfigs().stream()
                .map(config -> config.resolve(context))
                .filter(java.util.Objects::nonNull)
                .filter(config -> !config.isBlank())
                .toList();
            if (configs.isEmpty()) {
                this.image.clearConfig();
            } else {
                this.image.setConfigs(configs);
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
            this.image.setAnimationMode(animation.mode().resolve(context));
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
        private VisualElementsInterface layoutParent;

        private ElementVideo(VideoDefinition definition) {
            this.definition = definition;
        }

        @Override
        public String id() {
            return this.definition.id();
        }

        @Override
        public void prepare(
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
            this.video.setLayoutParent(this.layoutParent);
            VisualAnchor anchor = this.definition.anchor().resolve(context);
            this.video.setAnchor(anchor);
            this.video.setBounds(
                position(anchor, originX, this.definition.x().resolve(context)),
                position(anchor, originY, this.definition.y().resolve(context)),
                this.definition.width().resolve(context),
                this.definition.height().resolve(context)
            );
            this.video.setFullscreen(this.definition.fullscreen().resolve(context));
            this.video.setPlayback(
                this.definition.loop().resolve(context),
                this.definition.loopFadeMillis().resolve(context)
            );
        }

        @Override
        public void render(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            this.prepare(graphics, context, originX, originY);
            this.video.draw(graphics);
        }

        @Override
        public Object dslValue(String property) {
            if (this.video == null) {
                throw new IllegalStateException("Video element has not been prepared");
            }
            return this.video.dslValue(property);
        }

        @Override
        public void setLayoutParent(VisualElementsInterface parent) {
            this.layoutParent = parent;
            if (this.video != null) {
                this.video.setLayoutParent(parent);
            }
        }

        @Override
        public void close() {
            if (this.video != null) {
                this.video.close();
                this.video = null;
            }
        }
    }

    public static final class ElementRectangle implements ElementRenderable {
        private final RectangleDefinition definition;
        private final FillColorElement rectangle;

        private ElementRectangle(RectangleDefinition definition) {
            this.definition = definition;
            this.rectangle = new FillColorElement(0xFFFFFFFF, 0, 0, 0, 0, null);
        }

        @Override
        public String id() {
            return this.definition.id();
        }

        @Override
        public void prepare(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            VisualAnchor anchor = this.definition.anchor().resolve(context);
            this.rectangle.setAnchor(anchor);
            this.rectangle.setBounds(
                position(anchor, originX, this.definition.x().resolve(context)),
                position(anchor, originY, this.definition.y().resolve(context)),
                this.definition.width().resolve(context),
                this.definition.height().resolve(context)
            );
            if (this.definition.gradient()) {
                this.rectangle.setColor(
                    this.definition.topColor().resolve(context),
                    this.definition.bottomColor().resolve(context)
                );
            } else {
                this.rectangle.setColor(this.definition.topColor().resolve(context));
            }
        }

        @Override
        public void render(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            this.prepare(graphics, context, originX, originY);
            this.rectangle.draw(graphics);
        }

        @Override
        public Object dslValue(String property) {
            return this.rectangle.dslValue(property);
        }

        @Override
        public void setLayoutParent(VisualElementsInterface parent) {
            this.rectangle.setLayoutParent(parent);
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
        public void prepare(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            Component content = this.definition.literal() != null
                ? Component.literal(this.definition.literal().resolve(context))
                : Component.translatable(this.definition.translatable().resolve(context));
            content = content.copy().withStyle(style -> style
                .withBold(this.definition.bold().resolve(context))
                .withItalic(this.definition.italic().resolve(context))
                .withUnderlined(this.definition.underlined().resolve(context))
                .withStrikethrough(this.definition.strikethrough().resolve(context))
                .withObfuscated(this.definition.obfuscated().resolve(context))
            );
            this.text.setText(content);
            VisualAnchor anchor = this.definition.anchor().resolve(context);
            this.text.setAnchor(anchor);
            this.text.setPosition(
                position(anchor, originX, this.definition.x().resolve(context)),
                position(anchor, originY, this.definition.y().resolve(context))
            );
            this.text.setScale(this.definition.scale().resolve(context));
            this.text.setColor(this.definition.color().resolve(context));
            this.text.setShadow(this.definition.shadow().resolve(context));
            this.text.setLegacyShadow(this.definition.legacyShadow().resolve(context));
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
        }

        @Override
        public void render(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            this.prepare(graphics, context, originX, originY);
            this.text.draw(graphics);
        }

        @Override
        public Object dslValue(String property) {
            return this.text.dslValue(property);
        }

        @Override
        public void setLayoutParent(VisualElementsInterface parent) {
            this.text.setLayoutParent(parent);
        }
    }

    public static final class ElementTextBox implements ElementRenderable {
        private final TextBoxDefinition definition;
        private final ElementText content;
        private final TextBoxElement textBox;

        private ElementTextBox(TextBoxDefinition definition) {
            this.definition = definition;
            this.content = new ElementText(definition.text());
            this.textBox = new TextBoxElement(this.content.text, 0.0F, 0.0F);
        }

        @Override
        public String id() {
            return this.definition.id();
        }

        @Override
        public void prepare(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            this.content.prepare(graphics, context, 0.0F, 0.0F);
            VisualAnchor anchor = this.definition.anchor().resolve(context);
            this.textBox.setAnchor(anchor);
            this.textBox.setPosition(
                position(anchor, originX, this.definition.x().resolve(context)),
                position(anchor, originY, this.definition.y().resolve(context))
            );
            this.textBox.setSize(
                Math.max(0.0F, this.definition.width().resolve(context)),
                Math.max(0.0F, this.definition.height().resolve(context))
            );
            this.textBox.setMaxLines(this.definition.maxLines().resolve(context));
            this.textBox.setLineSpacing(this.definition.lineSpacing().resolve(context));
        }

        @Override
        public void render(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            float originX,
            float originY
        ) {
            this.prepare(graphics, context, originX, originY);
            this.textBox.draw(graphics);
        }

        @Override
        public Object dslValue(String property) {
            return this.textBox.dslValue(property);
        }

        @Override
        public void setLayoutParent(VisualElementsInterface parent) {
            this.textBox.setLayoutParent(parent);
        }
    }

    private static float position(VisualAnchor anchor, float origin, float offset) {
        return anchor.isExplicit() ? offset : origin + offset;
    }
}
