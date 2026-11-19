package net.jr.client.ui.dsl;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiFileType;
import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.components.widgets.GridDirection;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class UiCompiledDocument {
    private final Asset source;
    private final UiFileType type;
    private final boolean usable;
    private final Map<String, Expression<Object>> properties;
    private final BoundsDefinition bounds;
    private final Map<String, DslNodeDefinition> elements;
    private final Map<String, GridDefinition> grids;
    private final Map<String, CanvasDefinition> canvases;
    private final Map<String, LoadingBarDefinition> loadingBars;
    private final Map<String, RenderableDefinition> renderables;
    private final Set<String> layoutManagedElements;
    private final Map<UiRenderLayer, List<EntryDefinition>> layers;
    private final Map<String, SourceLocation> locations;
    private final Map<String, Map<String, SourceLocation>> propertyLocations;

    public UiCompiledDocument(
        Asset source,
        UiFileType type,
        Map<String, Expression<Object>> properties,
        BoundsDefinition bounds,
        Map<UiRenderLayer, List<EntryDefinition>> layers,
        Map<String, SourceLocation> locations,
        Map<String, Map<String, SourceLocation>> propertyLocations
    ) {
        this(source, type, properties, bounds, layers, locations, propertyLocations, true);
    }

    private UiCompiledDocument(
        Asset source,
        UiFileType type,
        Map<String, Expression<Object>> properties,
        BoundsDefinition bounds,
        Map<UiRenderLayer, List<EntryDefinition>> layers,
        Map<String, SourceLocation> locations,
        Map<String, Map<String, SourceLocation>> propertyLocations,
        boolean usable
    ) {
        this.source = Objects.requireNonNull(source, "source");
        this.type = Objects.requireNonNull(type, "type");
        this.usable = usable;
        this.properties = Map.copyOf(properties);
        this.bounds = bounds;
        Map<String, DslNodeDefinition> indexed = new LinkedHashMap<>();
        Map<String, GridDefinition> indexedGrids = new LinkedHashMap<>();
        Map<String, CanvasDefinition> indexedCanvases = new LinkedHashMap<>();
        Map<String, LoadingBarDefinition> indexedLoadingBars = new LinkedHashMap<>();
        Map<String, RenderableDefinition> indexedRenderables = new LinkedHashMap<>();
        Set<String> layoutManaged = new HashSet<>();
        Map<UiRenderLayer, List<EntryDefinition>> copiedLayers = new LinkedHashMap<>();
        for (UiRenderLayer layer : UiRenderLayer.values()) {
            List<EntryDefinition> entries = List.copyOf(layers.getOrDefault(layer, List.of()));
            copiedLayers.put(layer, entries);
            indexElements(
                entries, indexed, indexedGrids, indexedCanvases, indexedLoadingBars,
                indexedRenderables,
                layoutManaged, false
            );
        }
        this.elements = Map.copyOf(indexed);
        this.grids = Map.copyOf(indexedGrids);
        this.canvases = Map.copyOf(indexedCanvases);
        this.loadingBars = Map.copyOf(indexedLoadingBars);
        this.renderables = Map.copyOf(indexedRenderables);
        this.layoutManagedElements = Set.copyOf(layoutManaged);
        this.layers = Map.copyOf(copiedLayers);
        this.locations = Map.copyOf(locations);
        Map<String, Map<String, SourceLocation>> copiedPropertyLocations = new LinkedHashMap<>();
        propertyLocations.forEach((id, values) ->
            copiedPropertyLocations.put(id, Map.copyOf(values))
        );
        this.propertyLocations = Map.copyOf(copiedPropertyLocations);
    }

    public static UiCompiledDocument empty(Asset source, UiFileType type) {
        return new UiCompiledDocument(
            source,
            type,
            Map.of(),
            null,
            Map.of(),
            Map.of(),
            Map.of(),
            false
        );
    }

    public boolean isUsable() {
        return this.usable;
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

    public Map<String, GridDefinition> grids() {
        return this.grids;
    }

    public Map<String, CanvasDefinition> canvases() {
        return this.canvases;
    }

    public Map<String, LoadingBarDefinition> loadingBars() {
        return this.loadingBars;
    }

    public Map<String, RenderableDefinition> renderables() {
        return this.renderables;
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

    public boolean isLayoutManaged(String id) {
        return this.layoutManagedElements.contains(id);
    }

    public SourceLocation location(String id) {
        return this.locations.getOrDefault(id, new SourceLocation(this.source, 1, 1));
    }

    public SourceLocation propertyLocation(String id, String property) {
        return this.propertyLocations.getOrDefault(id, Map.of())
            .getOrDefault(property, this.location(id));
    }

    public record SourceLocation(Asset source, int line, int column, boolean editable,
                                 boolean branchesEditable) {
        public SourceLocation(Asset source, int line, int column) {
            this(source, line, column, false, false);
        }

        public SourceLocation(Asset source, int line, int column, boolean editable) {
            this(source, line, column, editable, editable);
        }

        public SourceLocation {
            Objects.requireNonNull(source, "source");
        }
    }

    @FunctionalInterface
    public interface Expression<T> {
        T resolve(DataContext context);
    }

    @FunctionalInterface
    public interface DataContext {
        Object resolve(List<String> path);

        default boolean contains(List<String> path) {
            return false;
        }

        default Object resolveElement(String id, String property) {
            throw new IllegalArgumentException(
                "UI element references are not available in this data context: element(\""
                    + id + "\")." + property
            );
        }
    }

    public interface EntryDefinition {
        String id();
    }

    public record ConditionalEntryDefinition(
        String id,
        Expression<Boolean> condition,
        EntryDefinition entry
    ) implements EntryDefinition {
        public ConditionalEntryDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(entry, "entry");
        }
    }

    public record GridDefinition(
        String id,
        Expression<VisualAnchor> anchor,
        Expression<Float> x,
        Expression<Float> y,
        Expression<Integer> columns,
        Expression<Integer> horizontalGap,
        Expression<Integer> verticalGap,
        Expression<GridDirection> direction,
        List<EntryDefinition> entries
    ) implements EntryDefinition {
        public GridDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(columns, "columns");
            Objects.requireNonNull(horizontalGap, "horizontalGap");
            Objects.requireNonNull(verticalGap, "verticalGap");
            Objects.requireNonNull(direction, "direction");
            entries = List.copyOf(entries);
        }
    }

    public record CanvasDefinition(
        String id,
        Expression<VisualAnchor> anchor,
        Expression<Float> x,
        Expression<Float> y,
        Expression<Float> width,
        Expression<Float> height,
        List<EntryDefinition> entries
    ) implements EntryDefinition {
        public CanvasDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            entries = List.copyOf(entries);
        }
    }

    public record LoadingBarDefinition(
        String id,
        Expression<VisualAnchor> anchor,
        Expression<Float> x,
        Expression<Float> y,
        Expression<Float> width,
        Expression<Float> height,
        Expression<Float> fillX,
        Expression<Float> fillY,
        Expression<Float> fillWidth,
        Expression<Float> fillHeight,
        Expression<Float> progress,
        List<EntryDefinition> background,
        List<EntryDefinition> fill
    ) implements EntryDefinition {
        public LoadingBarDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            Objects.requireNonNull(fillX, "fillX");
            Objects.requireNonNull(fillY, "fillY");
            Objects.requireNonNull(fillWidth, "fillWidth");
            Objects.requireNonNull(fillHeight, "fillHeight");
            Objects.requireNonNull(progress, "progress");
            background = List.copyOf(background);
            fill = List.copyOf(fill);
        }
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

    private static void indexElements(
        List<EntryDefinition> entries,
        Map<String, DslNodeDefinition> indexed,
        Map<String, GridDefinition> indexedGrids,
        Map<String, CanvasDefinition> indexedCanvases,
        Map<String, LoadingBarDefinition> indexedLoadingBars,
        Map<String, RenderableDefinition> indexedRenderables,
        Set<String> layoutManaged,
        boolean insideLayout
    ) {
        for (EntryDefinition entry : entries) {
            if (entry instanceof ConditionalEntryDefinition conditional) {
                indexElements(
                    List.of(conditional.entry()), indexed, indexedGrids,
                    indexedCanvases, indexedLoadingBars, indexedRenderables,
                    layoutManaged, insideLayout
                );
                continue;
            }
            if (entry instanceof DslNodeDefinition element) {
                if (indexed.putIfAbsent(element.id(), element) != null) {
                    throw new IllegalArgumentException("Duplicate DSL element id '" + element.id() + "'");
                }
                if (insideLayout) {
                    layoutManaged.add(element.id());
                }
            }
            if (entry instanceof GridDefinition grid) {
                if (indexedGrids.putIfAbsent(grid.id(), grid) != null) {
                    throw new IllegalArgumentException("Duplicate grid id '" + grid.id() + "'");
                }
                indexElements(
                    grid.entries(), indexed, indexedGrids, indexedCanvases,
                    indexedLoadingBars, indexedRenderables,
                    layoutManaged, true
                );
            }
            if (entry instanceof CanvasDefinition canvas) {
                if (indexedCanvases.putIfAbsent(canvas.id(), canvas) != null) {
                    throw new IllegalArgumentException("Duplicate canvas id '" + canvas.id() + "'");
                }
                indexElements(
                    canvas.entries(), indexed, indexedGrids, indexedCanvases,
                    indexedLoadingBars,
                    indexedRenderables, layoutManaged, insideLayout
                );
            }
            if (entry instanceof LoadingBarDefinition loadingBar) {
                if (indexedLoadingBars.putIfAbsent(loadingBar.id(), loadingBar) != null) {
                    throw new IllegalArgumentException(
                        "Duplicate loading bar id '" + loadingBar.id() + "'"
                    );
                }
                indexElements(
                    loadingBar.background(), indexed, indexedGrids, indexedCanvases,
                    indexedLoadingBars, indexedRenderables, layoutManaged, insideLayout
                );
                indexElements(
                    loadingBar.fill(), indexed, indexedGrids, indexedCanvases,
                    indexedLoadingBars, indexedRenderables, layoutManaged, insideLayout
                );
            }
            if (entry instanceof RenderableDefinition renderable) {
                if (indexedRenderables.putIfAbsent(renderable.id(), renderable) != null) {
                    throw new IllegalArgumentException(
                        "Duplicate renderable id '" + renderable.id() + "'"
                    );
                }
            }
        }
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
        Expression<VisualAnchor> anchor,
        Expression<Float> x,
        Expression<Float> y,
        Expression<Float> width,
        Expression<Float> height,
        Expression<Float> alpha,
        Vector2Definition uv,
        Vector2Definition uvSize,
        Vector2Definition atlasSize,
        Expression<Object> meta,
        List<Expression<String>> metaConfigs,
        NineDefinition nine,
        AnimationDefinition animation
    ) implements RenderableDefinition {
        public ImageDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(texture, "texture");
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            Objects.requireNonNull(alpha, "alpha");
            Objects.requireNonNull(meta, "meta");
            metaConfigs = List.copyOf(metaConfigs);
        }
    }

    public record VideoDefinition(
        String id,
        Expression<Asset> source,
        Expression<VisualAnchor> anchor,
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
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            Objects.requireNonNull(fullscreen, "fullscreen");
            Objects.requireNonNull(loop, "loop");
            Objects.requireNonNull(loopFadeMillis, "loopFadeMillis");
        }
    }

    public record RectangleDefinition(
        String id,
        Expression<VisualAnchor> anchor,
        Expression<Float> x,
        Expression<Float> y,
        Expression<Float> width,
        Expression<Float> height,
        Expression<Integer> topColor,
        Expression<Integer> bottomColor
    ) implements RenderableDefinition {
        public RectangleDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            Objects.requireNonNull(topColor, "topColor");
        }

        public boolean gradient() {
            return this.bottomColor != null;
        }
    }

    public record TextDefinition(
        String id,
        Expression<String> literal,
        Expression<String> translatable,
        Expression<VisualAnchor> anchor,
        Expression<Float> x,
        Expression<Float> y,
        Expression<Float> scale,
        Expression<Integer> color,
        Expression<Boolean> bold,
        Expression<Boolean> italic,
        Expression<Boolean> underlined,
        Expression<Boolean> strikethrough,
        Expression<Boolean> obfuscated,
        Expression<Boolean> shadow,
        Expression<Integer> shadowColor,
        Expression<Integer> outline,
        Expression<Draw.TextAlign> align,
        Expression<Asset> font,
        Expression<Boolean> legacyShadow
    ) implements RenderableDefinition {
        public TextDefinition {
            Objects.requireNonNull(id, "id");
            if ((literal == null) == (translatable == null)) {
                throw new IllegalArgumentException("Text requires exactly one content source");
            }
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(scale, "scale");
            Objects.requireNonNull(color, "color");
            Objects.requireNonNull(bold, "bold");
            Objects.requireNonNull(italic, "italic");
            Objects.requireNonNull(underlined, "underlined");
            Objects.requireNonNull(strikethrough, "strikethrough");
            Objects.requireNonNull(obfuscated, "obfuscated");
            Objects.requireNonNull(shadow, "shadow");
            Objects.requireNonNull(align, "align");
            Objects.requireNonNull(legacyShadow, "legacyShadow");
        }
    }

    public record TextBoxDefinition(
        String id,
        Expression<VisualAnchor> anchor,
        Expression<Float> x,
        Expression<Float> y,
        Expression<Float> width,
        Expression<Float> height,
        Expression<Integer> maxLines,
        Expression<Float> lineSpacing,
        TextDefinition text
    ) implements RenderableDefinition {
        public TextBoxDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(height, "height");
            Objects.requireNonNull(maxLines, "maxLines");
            Objects.requireNonNull(lineSpacing, "lineSpacing");
            Objects.requireNonNull(text, "text");
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
