package net.jr.api.client.ui.dsl;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiFileType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class UiCompiledDocument {
    private final Asset source;
    private final UiFileType type;
    private final Map<String, ButtonDefinition> buttons;
    private final Map<UiRenderLayer, List<RenderableDefinition>> renderables;

    public UiCompiledDocument(
        Asset source,
        UiFileType type,
        List<ButtonDefinition> buttons,
        Map<UiRenderLayer, List<RenderableDefinition>> renderables
    ) {
        this.source = Objects.requireNonNull(source, "source");
        this.type = Objects.requireNonNull(type, "type");
        Map<String, ButtonDefinition> indexed = new LinkedHashMap<>();
        for (ButtonDefinition button : buttons) {
            if (indexed.putIfAbsent(button.id(), button) != null) {
                throw new IllegalArgumentException("Duplicate button id '" + button.id() + "'");
            }
        }
        this.buttons = Map.copyOf(indexed);
        Map<UiRenderLayer, List<RenderableDefinition>> copiedLayers = new LinkedHashMap<>();
        for (UiRenderLayer layer : UiRenderLayer.values()) {
            copiedLayers.put(layer, List.copyOf(renderables.getOrDefault(layer, List.of())));
        }
        this.renderables = Map.copyOf(copiedLayers);
    }

    public Asset source() {
        return this.source;
    }

    public UiFileType type() {
        return this.type;
    }

    public Map<String, ButtonDefinition> buttons() {
        return this.buttons;
    }

    public List<RenderableDefinition> renderables(UiRenderLayer layer) {
        return this.renderables.getOrDefault(Objects.requireNonNull(layer, "layer"), List.of());
    }

    public ButtonDefinition requireButton(String id) {
        ButtonDefinition definition = this.buttons.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("UI button '" + id + "' does not exist in " + this.source);
        }
        return definition;
    }

    @FunctionalInterface
    public interface Expression<T> {
        T resolve(DataContext context);
    }

    @FunctionalInterface
    public interface DataContext {
        Object resolve(List<String> path);
    }

    public record ButtonDefinition(
        String id,
        Expression<Float> x,
        Expression<Float> y,
        Expression<Float> width,
        Expression<Float> height,
        Expression<Boolean> scissor,
        List<RenderableDefinition> renderables
    ) {
        public ButtonDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            Objects.requireNonNull(scissor, "scissor");
            renderables = List.copyOf(renderables);
        }
    }

    public interface RenderableDefinition {
        String id();
    }

    public record ConditionalRenderableDefinition(
        String id,
        Expression<Boolean> condition,
        RenderableDefinition renderable
    ) implements RenderableDefinition {
        public ConditionalRenderableDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(renderable, "renderable");
        }
    }

    public record ImageDefinition(
        String id,
        Expression<Asset> texture,
        Expression<Float> x,
        Expression<Float> y,
        Expression<Float> width,
        Expression<Float> height,
        Vector2Definition uv,
        Vector2Definition uvSize,
        Vector2Definition atlasSize,
        Expression<Boolean> useMeta,
        Expression<String> metaConfig,
        NineDefinition nine,
        AnimationDefinition animation
    ) implements RenderableDefinition {
        public ImageDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(texture, "texture");
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            Objects.requireNonNull(useMeta, "useMeta");
        }
    }

    public record VideoDefinition(
        String id,
        Expression<Asset> source,
        Expression<Float> x,
        Expression<Float> y,
        Expression<Float> width,
        Expression<Float> height,
        Expression<Boolean> fullscreen,
        Expression<Boolean> loop,
        Expression<Integer> loopFadeMillis
    ) implements RenderableDefinition {
        public VideoDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            Objects.requireNonNull(fullscreen, "fullscreen");
            Objects.requireNonNull(loop, "loop");
            Objects.requireNonNull(loopFadeMillis, "loopFadeMillis");
        }
    }

    public record TextDefinition(
        String id,
        Expression<String> literal,
        Expression<String> translatable,
        Expression<Float> x,
        Expression<Float> y,
        Expression<Float> scale,
        Expression<Integer> color,
        Expression<Boolean> shadow,
        Expression<Integer> shadowColor,
        Expression<Integer> outline,
        Expression<Draw.TextAlign> align,
        Expression<Asset> font
    ) implements RenderableDefinition {
        public TextDefinition {
            Objects.requireNonNull(id, "id");
            if ((literal == null) == (translatable == null)) {
                throw new IllegalArgumentException("Text requires exactly one content source");
            }
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(scale, "scale");
            Objects.requireNonNull(color, "color");
            Objects.requireNonNull(shadow, "shadow");
            Objects.requireNonNull(align, "align");
        }
    }

    public record Vector2Definition(Expression<Float> first, Expression<Float> second) {
        public Vector2Definition {
            Objects.requireNonNull(first, "first");
            Objects.requireNonNull(second, "second");
        }
    }

    public record NineDefinition(
        Expression<Integer> top,
        Expression<Integer> bottom,
        Expression<Integer> left,
        Expression<Integer> right,
        Expression<Draw.CenterMode> centerMode
    ) {
        public NineDefinition {
            Objects.requireNonNull(top, "top");
            Objects.requireNonNull(bottom, "bottom");
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(centerMode, "centerMode");
        }
    }

    public record AnimationDefinition(
        Expression<Integer> frames,
        Vector2Definition frameSize,
        Expression<Integer> duration,
        Expression<Draw.AnimUnit> unit,
        Expression<Boolean> interpolation,
        List<Expression<Integer>> frameDurations
    ) {
        public AnimationDefinition {
            Objects.requireNonNull(frames, "frames");
            Objects.requireNonNull(frameSize, "frameSize");
            Objects.requireNonNull(duration, "duration");
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(interpolation, "interpolation");
            frameDurations = List.copyOf(frameDurations);
        }
    }
}
