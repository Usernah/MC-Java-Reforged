package net.jr.client.ui.dsl;

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
    private final Map<String, Expression<Object>> properties;
    private final BoundsDefinition bounds;
    private final Map<String, DslNodeDefinition> elements;
    private final Map<UiRenderLayer, List<EntryDefinition>> layers;

    public UiCompiledDocument(
        Asset source,
        UiFileType type,
        Map<String, Expression<Object>> properties,
        BoundsDefinition bounds,
        Map<UiRenderLayer, List<EntryDefinition>> layers
    ) {
        this.source = Objects.requireNonNull(source, "source");
        this.type = Objects.requireNonNull(type, "type");
        this.properties = Map.copyOf(properties);
        this.bounds = bounds;
        Map<String, DslNodeDefinition> indexed = new LinkedHashMap<>();
        Map<UiRenderLayer, List<EntryDefinition>> copiedLayers = new LinkedHashMap<>();
        for (UiRenderLayer layer : UiRenderLayer.values()) {
            List<EntryDefinition> entries = List.copyOf(layers.getOrDefault(layer, List.of()));
            copiedLayers.put(layer, entries);
            for (EntryDefinition entry : entries) {
                if (entry instanceof DslNodeDefinition element
                    && indexed.putIfAbsent(element.id(), element) != null) {
                    throw new IllegalArgumentException("Duplicate DSL element id '" + element.id() + "'");
                }
            }
        }
        this.elements = Map.copyOf(indexed);
        this.layers = Map.copyOf(copiedLayers);
    }

    public Asset source() {
        return this.source;
    }

    public UiFileType type() {
        return this.type;
    }

    public boolean hasProperty(String name) {
        return this.properties.containsKey(name);
    }

    public Object property(String name, DataContext context) {
        Expression<Object> expression = this.properties.get(name);
        if (expression == null) {
            throw new IllegalArgumentException(
                "UI DSL document does not declare property '" + name + "' in " + this.source
            );
        }
        return expression.resolve(context);
    }

    public boolean hasBounds() {
        return this.bounds != null;
    }

    public int boundsX(DataContext context) {
        if (this.bounds == null) {
            throw new IllegalStateException("UI DSL document does not declare bounds: " + this.source);
        }
        return this.bounds.x().resolve(context);
    }

    public int boundsY(DataContext context) {
        if (this.bounds == null) {
            throw new IllegalStateException("UI DSL document does not declare bounds: " + this.source);
        }
        return this.bounds.y().resolve(context);
    }

    public int boundsWidth(DataContext context) {
        if (this.bounds == null) {
            throw new IllegalStateException("UI DSL document does not declare bounds: " + this.source);
        }
        return this.bounds.width().resolve(context);
    }

    public int boundsHeight(DataContext context) {
        if (this.bounds == null) {
            throw new IllegalStateException("UI DSL document does not declare bounds: " + this.source);
        }
        return this.bounds.height().resolve(context);
    }

    public Map<String, DslNodeDefinition> elements() {
        return this.elements;
    }

    public List<EntryDefinition> entries(UiRenderLayer layer) {
        return this.layers.getOrDefault(Objects.requireNonNull(layer, "layer"), List.of());
    }

    public DslNodeDefinition requireElement(String id) {
        DslNodeDefinition definition = this.elements.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("UI DSL element '" + id + "' does not exist in " + this.source);
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

    public interface EntryDefinition {
        String id();
    }

    public record BoundsDefinition(
        Expression<Integer> x,
        Expression<Integer> y,
        Expression<Integer> width,
        Expression<Integer> height
    ) {
        public BoundsDefinition {
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
        }
    }

    public record DslNodeDefinition(
        String type,
        String id,
        Map<String, Expression<Object>> properties,
        Map<String, List<EntryDefinition>> renderBlocks,
        List<DslBlockDefinition> children
    ) implements EntryDefinition {
        public DslNodeDefinition {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(id, "id");
            properties = Map.copyOf(properties);
            Map<String, List<EntryDefinition>> copied = new LinkedHashMap<>();
            renderBlocks.forEach((name, entries) -> copied.put(name, List.copyOf(entries)));
            renderBlocks = Map.copyOf(copied);
            children = List.copyOf(children);
        }
    }

    public record DslBlockDefinition(
        String type,
        List<Expression<Object>> arguments,
        String id,
        Map<String, Expression<Object>> properties,
        Map<String, List<EntryDefinition>> renderBlocks,
        List<DslBlockDefinition> children
    ) {
        public DslBlockDefinition {
            Objects.requireNonNull(type, "type");
            arguments = List.copyOf(arguments);
            properties = Map.copyOf(properties);
            Map<String, List<EntryDefinition>> copied = new LinkedHashMap<>();
            renderBlocks.forEach((name, entries) -> copied.put(name, List.copyOf(entries)));
            renderBlocks = Map.copyOf(copied);
            children = List.copyOf(children);
        }
    }

    public interface RenderableDefinition extends EntryDefinition {
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
        Expression<Draw.AnimationMode> mode,
        Expression<Boolean> interpolation,
        List<Expression<Integer>> frameDurations
    ) {
        public AnimationDefinition {
            Objects.requireNonNull(frames, "frames");
            Objects.requireNonNull(frameSize, "frameSize");
            Objects.requireNonNull(duration, "duration");
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(interpolation, "interpolation");
            frameDurations = List.copyOf(frameDurations);
        }
    }
}
