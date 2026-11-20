package net.jr.client.ui.dsl;

import net.jr.api.client.ui.UiFile;
import net.jr.client.components.widgets.JavaBaseWidget;
import net.jr.client.components.widgets.GridLayout;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.ItemSlotPos;
import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.components.elements.CanvasElement;
import net.jr.client.components.elements.LoadingBarElement;
import net.jr.client.components.elements.VisualElementsInterface;
import net.jr.client.ui.UiElement;
import net.jr.client.ui.dsl.UiCompiledDocument.DslNodeDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.CanvasDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.ConditionalEntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.GridDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.LoadingBarDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.RenderableDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;


public abstract class DslUiContext<F extends UiFile> implements AutoCloseable {
    private final Screen host;
    private final F file;
    private final UiCompiledDocument document;
    private final UiDataContext data;
    private final Map<String, DslElement> elements = new LinkedHashMap<>();
    private final Map<RenderableDefinition, UiElement.ElementRenderable> renderables =
        new IdentityHashMap<>();
    private final Map<String, ReferencePhase> renderablePhases = new LinkedHashMap<>();
    private final Map<String, GridLayout> grids = new LinkedHashMap<>();
    private final Map<String, CanvasElement> canvases = new LinkedHashMap<>();
    private final Map<String, LoadingBarRuntime> loadingBars = new LinkedHashMap<>();
    private final Map<String, CanvasDefinition> canvasParents = new LinkedHashMap<>();
    private final Map<String, ReferencePhase> canvasPhases = new LinkedHashMap<>();
    private final Map<String, ReferencePhase> loadingBarPhases = new LinkedHashMap<>();
    private final Map<String, GridPhase> gridPhases = new LinkedHashMap<>();
    private final Set<String> invalidRenderables = new HashSet<>();
    private final Set<String> invalidGrids = new HashSet<>();
    private boolean extractingScreen;
    private boolean layoutsPrepared;

    protected DslUiContext(Screen host, F file) {
        this.host = Objects.requireNonNull(host, "host");
        this.file = Objects.requireNonNull(file, "file");
        this.document = UiDocumentManager.getInstance().requireCompiled(file);
        this.data = UiDataContext.withGlobals();
        this.data.bindElementResolver(this::resolveElementValue);
        for (UiRenderLayer layer : UiRenderLayer.values()) {
            this.indexCanvasParents(this.document.entries(layer), null);
        }
    }

    public final Screen host() {
        return this.host;
    }

    public final F file() {
        return this.file;
    }

    public final UiCompiledDocument compiledDocument() {
        return this.document;
    }

    /** Host-managed Java widgets and DSL elements, exposed without editor dependencies. */
    public final List<DslElement> boundElements() {
        return List.copyOf(this.elements.values());
    }

    /** Geometry from the actual layout, not a second interpretation of the DSL. */
    public final List<ResolvedVisual> resolvedVisuals() {
        List<ResolvedVisual> result = new java.util.ArrayList<>();
        for (var entry : this.renderables.entrySet()) {
            var visual = entry.getValue();
            try {
                float x = numeric(visual.dslValue("resolved_x"));
                float y = numeric(visual.dslValue("resolved_y"));
                float w = numeric(visual.dslValue("w"));
                float h = numeric(visual.dslValue("h"));
                if (w > 0 && h > 0) result.add(new ResolvedVisual(entry.getKey().id(), x, y, w, h));
            } catch (RuntimeException ignored) { }
        }
        for (var entry : this.canvases.entrySet()) {
            addVisual(result, entry.getKey(), entry.getValue());
        }
        for (var entry : this.grids.entrySet()) {
            addVisual(result, entry.getKey(), entry.getValue());
        }
        for (var entry : this.elements.entrySet()) {
            if (entry.getValue() instanceof VisualElementsInterface visual) {
                addVisual(result, entry.getKey(), visual);
            } else if (entry.getValue() instanceof ItemSlotGrid slots) {
                float left = Float.POSITIVE_INFINITY, top = Float.POSITIVE_INFINITY;
                float right = Float.NEGATIVE_INFINITY, bottom = Float.NEGATIVE_INFINITY;
                for (var layout : slots.layoutBounds().entrySet()) {
                    ItemSlotPos b = layout.getValue();
                    result.add(new ResolvedVisual(layout.getKey(), b.x(), b.y(), b.width(), b.height()));
                }
                for (int id : slots.slotIds()) {
                    ItemSlotPos pos = slots.get(id);
                    left = Math.min(left, pos.x()); top = Math.min(top, pos.y());
                    right = Math.max(right, pos.x() + pos.width());
                    bottom = Math.max(bottom, pos.y() + pos.height());
                    result.add(new ResolvedVisual(entry.getKey() + "/slot/" + id,
                        pos.x(), pos.y(), pos.width(), pos.height()));
                }
                if (left < right && top < bottom) {
                    result.add(new ResolvedVisual(entry.getKey(), left, top, right - left, bottom - top));
                }
            }
        }
        return List.copyOf(result);
    }

    private static void addVisual(List<ResolvedVisual> result, String id, VisualElementsInterface visual) {
        try {
            float w = visual.width(), h = visual.height();
            if (w > 0 && h > 0) result.add(new ResolvedVisual(id, visual.resolvedX(), visual.resolvedY(), w, h));
        } catch (RuntimeException ignored) { }
    }

    private static float numeric(Object value) {
        return value instanceof Number number ? number.floatValue() : 0.0F;
    }

    public record ResolvedVisual(String id, float x, float y, float width, float height) { }

    public final UiDataContext data() {
        return this.data;
    }

    public final boolean hasProperty(String name) {
        return this.document.hasProperty(name);
    }

    public final Object property(String name) {
        return this.recover(
            "$document", null, () -> this.document.property(name, this.data)
        );
    }

    public final boolean hasBounds() {
        return this.document.hasBounds();
    }

    public final int boundsX() {
        return this.recover("$bounds", 0, () -> this.document.boundsX(this.data));
    }

    public final int boundsY() {
        return this.recover("$bounds", 0, () -> this.document.boundsY(this.data));
    }

    public final int boundsWidth() {
        return this.recover("$bounds", 0, () -> this.document.boundsWidth(this.data));
    }

    public final int boundsHeight() {
        return this.recover("$bounds", 0, () -> this.document.boundsHeight(this.data));
    }

    public float originX() {
        return 0.0F;
    }

    public float originY() {
        return 0.0F;
    }

    public final DslNode element(String id) {
        DslElement previous = this.elements.get(id);
        if (previous instanceof JavaBaseWidget widget && !this.host.children().contains(widget)) {
            this.elements.remove(id);
            previous.closeDsl();
        }
        DslNodeDefinition definition = this.document.elements().get(id);
        if (definition == null) {
            UiDslRuntimeException exception = this.failure(
                id,
                "UI DSL element '" + id + "' does not exist in " + this.document.source(),
                null
            );
            UiDocumentManager.getInstance().reportRuntime(exception);
            return DslNode.missing(this, id);
        }
        return new DslNode(this, definition);
    }

    final boolean isLayoutManaged(String id) {
        return this.document.isLayoutManaged(id);
    }

    final UiDslRuntimeException failure(String id, String message, Throwable cause) {
        return new UiDslRuntimeException(this.document.location(id), message, cause);
    }

    public final void report(String id, String message, Throwable cause) {
        UiDocumentManager.getInstance().reportRuntime(this.failure(
            id,
            message == null ? (cause == null ? "Unknown DSL runtime error" : cause.getClass().getSimpleName()) : message,
            cause
        ));
    }

    public final <T> T recover(String id, T fallback, Supplier<T> operation) {
        return UiDocumentManager.getInstance().recoverRuntime(
            this.document.location(id), fallback, operation
        );
    }

    public final void bind(DslNode node, DslElement element) {
        DslElement previous = this.elements.putIfAbsent(node.id(), element);
        if (previous != null && previous != element) {
            throw this.failure(
                node.id(), "DSL element '" + node.id() + "' is already bound", null
            );
        }
        if (element instanceof JavaBaseWidget widget) {
            ((DslHostBridge)this.host).javaReforged$addDslWidget(widget);
        }
    }

    public final void extractScreen(Runnable extraction) {
        boolean previousExtraction = this.extractingScreen;
        boolean previousPreparation = this.layoutsPrepared;
        this.extractingScreen = true;
        this.layoutsPrepared = false;
        try {
            extraction.run();
        } finally {
            this.extractingScreen = previousExtraction;
            this.layoutsPrepared = previousPreparation;
        }
    }

    public final void extract(
        UiRenderLayer layer,
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        if (!this.extractingScreen || !this.layoutsPrepared) {
            this.prepareLayouts(this.data, this.originX(), this.originY());
            this.layoutsPrepared = this.extractingScreen;
        }
        this.extractEntries(
            this.document.entries(layer), graphics, this.data, mouseX, mouseY, partialTick,
            this.originX(), this.originY(), null
        );
    }

    public final void extractBlock(
        List<EntryDefinition> entries,
        GuiGraphicsExtractor graphics,
        UiCompiledDocument.DataContext context,
        int mouseX,
        int mouseY,
        float partialTick,
        float originX,
        float originY
    ) {
        this.extractBlock(
            entries, graphics, context, mouseX, mouseY, partialTick, originX, originY, null
        );
    }

    public final void extractBlock(
        List<EntryDefinition> entries,
        GuiGraphicsExtractor graphics,
        UiCompiledDocument.DataContext context,
        int mouseX,
        int mouseY,
        float partialTick,
        float originX,
        float originY,
        VisualElementsInterface layoutParent
    ) {
        this.extractEntries(
            entries, graphics, context, mouseX, mouseY, partialTick,
            originX, originY, layoutParent
        );
    }

    private void extractEntries(
        List<EntryDefinition> entries,
        GuiGraphicsExtractor graphics,
        UiCompiledDocument.DataContext context,
        int mouseX,
        int mouseY,
        float partialTick,
        float originX,
        float originY,
        VisualElementsInterface layoutParent
    ) {
        for (EntryDefinition entry : entries) {
            try {
                this.extractEntry(
                    entry, graphics, context, mouseX, mouseY, partialTick,
                    originX, originY, layoutParent
                );
            } catch (UiDslRuntimeException exception) {
                UiDocumentManager.getInstance().reportRuntime(exception);
            } catch (RuntimeException exception) {
                UiDocumentManager.getInstance().reportRuntime(
                    this.failure(entry.id(), exception.getMessage(), exception)
                );
            }
        }
    }

    private void extractEntry(
        EntryDefinition entry,
        GuiGraphicsExtractor graphics,
        UiCompiledDocument.DataContext context,
        int mouseX,
        int mouseY,
        float partialTick,
        float originX,
        float originY,
        VisualElementsInterface layoutParent
    ) {
            if (entry instanceof ConditionalEntryDefinition conditional) {
                if (conditional.condition().resolve(context)) {
                    this.extractEntry(
                        conditional.entry(), graphics, context, mouseX, mouseY,
                        partialTick, originX, originY, layoutParent
                    );
                }
                return;
            }
            if (entry instanceof LoadingBarDefinition definition) {
                LoadingBarRuntime loadingBar = this.prepareLoadingBar(
                    definition, context, originX, originY, layoutParent
                );
                loadingBar.extract(graphics, context, mouseX, mouseY, partialTick);
                return;
            }
            if (entry instanceof CanvasDefinition definition) {
                CanvasElement canvas = this.prepareCanvas(
                    definition, context, originX, originY, layoutParent
                );
                VisualAnchor anchor = canvas.anchor();
                VisualElementsInterface childParent = anchor.isExplicit()
                    ? canvas
                    : layoutParent;
                float childOriginX = anchor.isExplicit() ? 0.0F : originX;
                float childOriginY = anchor.isExplicit() ? 0.0F : originY;
                this.extractEntries(
                    definition.entries(), graphics, context, mouseX, mouseY, partialTick,
                    childOriginX, childOriginY, childParent
                );
                return;
            }
            if (entry instanceof GridDefinition definition) {
                GridLayout grid = this.prepareGrid(
                    definition, context, originX, originY, layoutParent
                );
                if (grid == null) {
                    return;
                }
                this.extractEntries(
                    definition.entries(), graphics, context, mouseX, mouseY, partialTick,
                    0.0F, 0.0F, grid
                );
                return;
            }
            if (entry instanceof RenderableDefinition definition) {
                if (this.invalidRenderables.contains(definition.id())) {
                    return;
                }
                UiElement.ElementRenderable renderable = this.renderables.computeIfAbsent(
                    definition, UiElement::renderable
                );
                renderable.setLayoutParent(layoutParent);
                renderable.render(graphics, context, originX, originY);
                return;
            }
            if (!(entry instanceof DslNodeDefinition)) {
                throw new IllegalStateException("Unsupported UI entry: " + entry.getClass().getName());
            }
            DslElement element = this.elements.get(entry.id());
            if (element == null) {
                return;
            }
            if (element instanceof VisualElementsInterface visual) {
                this.assignLayoutParent(visual, layoutParent);
            }
            element.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private GridLayout prepareGrid(
        GridDefinition definition,
        UiCompiledDocument.DataContext context,
        float originX,
        float originY,
        VisualElementsInterface layoutParent
    ) {
        if (this.invalidGrids.contains(definition.id())) {
            return null;
        }
        try {
            return this.prepareGridStrict(
                definition, context, originX, originY, layoutParent
            );
        } catch (UiDslRuntimeException exception) {
            this.invalidGrids.add(definition.id());
            this.gridPhases.remove(definition.id());
            UiDocumentManager.getInstance().reportRuntime(exception);
        } catch (RuntimeException exception) {
            this.invalidGrids.add(definition.id());
            this.gridPhases.remove(definition.id());
            this.report(definition.id(), exception.getMessage(), exception);
        }
        return null;
    }

    private GridLayout prepareGridStrict(
        GridDefinition definition,
        UiCompiledDocument.DataContext context,
        float originX,
        float originY,
        VisualElementsInterface layoutParent
    ) {
        GridLayout grid = this.grids.computeIfAbsent(
            definition.id(), ignored -> new GridLayout(0.0F, 0.0F)
        );
        GridPhase phase = this.gridPhases.get(definition.id());
        if (phase == GridPhase.PREPARED || phase == GridPhase.MEASURED) {
            grid.setLayoutParent(layoutParent);
            return grid;
        }
        if (phase == GridPhase.CONFIGURING) {
            throw this.failure(definition.id(),
                "Circular element layout reference involving '" + definition.id() + "'", null);
        }

        this.gridPhases.put(definition.id(), GridPhase.CONFIGURING);
        try {
            grid.clearChildren();
            grid.setLayoutParent(layoutParent);
            grid.setAnchor(VisualAnchor.NONE);
            grid.setPosition(0.0F, 0.0F);
            grid.setColumns(definition.columns().resolve(context));
            grid.setHorizontalGap(definition.horizontalGap().resolve(context));
            grid.setVerticalGap(definition.verticalGap().resolve(context));
            grid.setDirection(definition.direction().resolve(context));

            for (EntryDefinition child : definition.entries()) {
                if (child instanceof GridDefinition nested) {
                    GridLayout nestedGrid = this.prepareGrid(
                        nested, context, originX, originY, grid
                    );
                    if (nestedGrid != null) {
                        grid.addChild(nestedGrid);
                    }
                    continue;
                }
                if (!(child instanceof DslNodeDefinition)) {
                    this.report(
                        child.id(),
                        "A grid can only arrange Java-owned visual elements or another grid",
                        null
                    );
                    continue;
                }
                DslElement element = this.elements.get(child.id());
                if (element == null) {
                    continue;
                }
                if (!(element instanceof VisualElementsInterface visual)) {
                    this.report(
                        child.id(),
                        "DSL element '" + child.id() + "' cannot be arranged by a grid",
                        null
                    );
                    continue;
                }
                grid.addChild(visual);
            }
            grid.layoutChildren();
            this.gridPhases.put(definition.id(), GridPhase.MEASURED);

            VisualAnchor anchor = definition.anchor().resolve(context);
            float x = definition.x().resolve(context);
            float y = definition.y().resolve(context);
            if (!anchor.isExplicit()) {
                x += originX;
                y += originY;
            }
            grid.setAnchor(anchor);
            grid.setPosition(x, y);
            grid.layoutChildren();
            this.gridPhases.put(definition.id(), GridPhase.PREPARED);
            return grid;
        } catch (RuntimeException exception) {
            this.gridPhases.remove(definition.id());
            throw exception;
        }
    }

    private void prepareLayouts(
        UiCompiledDocument.DataContext context,
        float originX,
        float originY
    ) {
        this.gridPhases.clear();
        this.renderablePhases.clear();
        this.canvasPhases.clear();
        this.loadingBarPhases.clear();
        for (UiRenderLayer layer : UiRenderLayer.values()) {
            this.prepareEntries(
                this.document.entries(layer), context, originX, originY, null
            );
        }
    }

    private void prepareEntries(
        List<EntryDefinition> entries,
        UiCompiledDocument.DataContext context,
        float originX,
        float originY,
        VisualElementsInterface layoutParent
    ) {
        for (EntryDefinition entry : entries) {
            if (entry instanceof ConditionalEntryDefinition conditional) {
                if (conditional.condition().resolve(context)) {
                    this.prepareEntries(
                        List.of(conditional.entry()), context,
                        originX, originY, layoutParent
                    );
                }
            } else if (entry instanceof LoadingBarDefinition definition) {
                LoadingBarRuntime loadingBar = this.prepareLoadingBar(
                    definition, context, originX, originY, layoutParent
                );
                this.prepareEntries(
                    definition.background(), context, 0.0F, 0.0F, loadingBar.element
                );
                this.prepareEntries(
                    definition.fill(), context, 0.0F, 0.0F, loadingBar.fillParent
                );
            } else if (entry instanceof CanvasDefinition definition) {
                CanvasElement canvas = this.prepareCanvas(
                    definition, context, originX, originY, layoutParent
                );
                boolean establishesParent = canvas.anchor().isExplicit();
                this.prepareEntries(
                    definition.entries(), context,
                    establishesParent ? 0.0F : originX,
                    establishesParent ? 0.0F : originY,
                    establishesParent ? canvas : layoutParent
                );
            } else if (entry instanceof GridDefinition definition) {
                this.prepareGrid(definition, context, originX, originY, layoutParent);
            } else if (entry instanceof RenderableDefinition definition) {
                this.prepareRenderable(definition, context, originX, originY, layoutParent);
            } else if (entry instanceof DslNodeDefinition) {
                DslElement element = this.elements.get(entry.id());
                if (element instanceof VisualElementsInterface visual) {
                    this.assignLayoutParent(visual, layoutParent);
                }
            }
        }
    }

    private void assignLayoutParent(
        VisualElementsInterface visual,
        VisualElementsInterface layoutParent
    ) {
        VisualElementsInterface previous = visual.layoutParent();
        if (previous == layoutParent) {
            return;
        }
        if (previous == null && layoutParent != null && !visual.anchor().isExplicit()) {
            visual.setPosition(
                visual.x() - this.originX(),
                visual.y() - this.originY()
            );
        }
        visual.setLayoutParent(layoutParent);
    }

    private CanvasElement prepareCanvas(
        CanvasDefinition definition,
        UiCompiledDocument.DataContext context,
        float originX,
        float originY,
        VisualElementsInterface layoutParent
    ) {
        CanvasElement canvas = this.canvases.computeIfAbsent(
            definition.id(), ignored -> new CanvasElement()
        );
        if (this.canvasPhases.get(definition.id()) == ReferencePhase.PREPARING) {
            throw this.failure(
                definition.id(),
                "Circular canvas reference involving '" + definition.id() + "'",
                null
            );
        }
        this.canvasPhases.put(definition.id(), ReferencePhase.PREPARING);
        try {
            VisualAnchor anchor = definition.anchor().resolve(context);
            canvas.setLayoutParent(layoutParent);
            canvas.setAnchor(anchor);
            canvas.setBounds(
                anchor.isExplicit() ? definition.x().resolve(context)
                    : originX + definition.x().resolve(context),
                anchor.isExplicit() ? definition.y().resolve(context)
                    : originY + definition.y().resolve(context),
                definition.width().resolve(context),
                definition.height().resolve(context)
            );
            this.canvasPhases.put(definition.id(), ReferencePhase.READY);
            return canvas;
        } catch (RuntimeException exception) {
            this.canvasPhases.remove(definition.id());
            throw exception;
        }
    }

    private LoadingBarRuntime prepareLoadingBar(
        LoadingBarDefinition definition,
        UiCompiledDocument.DataContext context,
        float originX,
        float originY,
        VisualElementsInterface layoutParent
    ) {
        LoadingBarRuntime loadingBar = this.loadingBars.computeIfAbsent(
            definition.id(), ignored -> new LoadingBarRuntime(definition)
        );
        if (this.loadingBarPhases.get(definition.id()) == ReferencePhase.PREPARING) {
            throw this.failure(
                definition.id(),
                "Circular loading bar reference involving '" + definition.id() + "'",
                null
            );
        }
        this.loadingBarPhases.put(definition.id(), ReferencePhase.PREPARING);
        try {
            VisualAnchor anchor = definition.anchor().resolve(context);
            loadingBar.element.setLayoutParent(layoutParent);
            loadingBar.element.setAnchor(anchor);
            loadingBar.element.setBackgroundBounds(
                anchor.isExplicit() ? definition.x().resolve(context)
                    : originX + definition.x().resolve(context),
                anchor.isExplicit() ? definition.y().resolve(context)
                    : originY + definition.y().resolve(context),
                definition.width().resolve(context),
                definition.height().resolve(context)
            );
            loadingBar.element.setFillBounds(
                definition.fillX().resolve(context),
                definition.fillY().resolve(context),
                definition.fillWidth().resolve(context),
                definition.fillHeight().resolve(context)
            );
            loadingBar.element.setProgress(definition.progress().resolve(context));
            loadingBar.fillParent.setLayoutParent(loadingBar.element);
            loadingBar.fillParent.setAnchor(VisualAnchor.NONE);
            loadingBar.fillParent.setBounds(
                loadingBar.element.fillX(),
                loadingBar.element.fillY(),
                loadingBar.element.fillWidth(),
                loadingBar.element.fillHeight()
            );
            this.loadingBarPhases.put(definition.id(), ReferencePhase.READY);
            return loadingBar;
        } catch (RuntimeException exception) {
            this.loadingBarPhases.remove(definition.id());
            throw exception;
        }
    }

    private void indexCanvasParents(
        List<EntryDefinition> entries,
        CanvasDefinition parent
    ) {
        for (EntryDefinition entry : entries) {
            if (entry instanceof ConditionalEntryDefinition conditional) {
                this.indexCanvasParents(List.of(conditional.entry()), parent);
            } else if (entry instanceof CanvasDefinition canvas) {
                if (parent != null) {
                    this.canvasParents.put(canvas.id(), parent);
                }
                this.indexCanvasParents(canvas.entries(), canvas);
            } else if (entry instanceof LoadingBarDefinition loadingBar) {
                this.indexCanvasParents(loadingBar.background(), parent);
                this.indexCanvasParents(loadingBar.fill(), parent);
            } else if (entry instanceof GridDefinition grid) {
                this.indexCanvasParents(grid.entries(), parent);
            }
        }
    }

    private PreparedCanvas prepareCanvasHierarchy(CanvasDefinition definition) {
        CanvasDefinition parentDefinition = this.canvasParents.get(definition.id());
        PreparedCanvas parent = parentDefinition == null
            ? new PreparedCanvas(null, null, this.originX(), this.originY())
            : this.prepareCanvasHierarchy(parentDefinition);
        CanvasElement canvas = this.prepareCanvas(
            definition,
            this.data,
            parent.childOriginX(),
            parent.childOriginY(),
            parent.childParent()
        );
        if (!canvas.anchor().isExplicit()) {
            return new PreparedCanvas(
                canvas, parent.childParent(), parent.childOriginX(), parent.childOriginY()
            );
        }
        return new PreparedCanvas(canvas, canvas, 0.0F, 0.0F);
    }

    private UiElement.ElementRenderable prepareRenderable(
        RenderableDefinition definition,
        UiCompiledDocument.DataContext context,
        float originX,
        float originY,
        VisualElementsInterface layoutParent
    ) {
        if (this.invalidRenderables.contains(definition.id())) {
            return null;
        }
        try {
            return this.prepareRenderableStrict(
                definition, context, originX, originY, layoutParent
            );
        } catch (UiDslRuntimeException exception) {
            this.invalidRenderables.add(definition.id());
            this.renderablePhases.remove(definition.id());
            UiDocumentManager.getInstance().reportRuntime(exception);
        } catch (RuntimeException exception) {
            this.invalidRenderables.add(definition.id());
            this.renderablePhases.remove(definition.id());
            this.report(definition.id(), exception.getMessage(), exception);
        }
        return null;
    }

    private UiElement.ElementRenderable prepareRenderableStrict(
        RenderableDefinition definition,
        UiCompiledDocument.DataContext context,
        float originX,
        float originY,
        VisualElementsInterface layoutParent
    ) {
        UiElement.ElementRenderable renderable = this.renderables.computeIfAbsent(
            definition, UiElement::renderable
        );
        renderable.setLayoutParent(layoutParent);
        ReferencePhase phase = this.renderablePhases.get(definition.id());
        if (phase == ReferencePhase.READY) {
            return renderable;
        }
        if (phase == ReferencePhase.PREPARING) {
            throw this.failure(definition.id(),
                "Circular element reference involving '" + definition.id() + "'", null);
        }
        this.renderablePhases.put(definition.id(), ReferencePhase.PREPARING);
        try {
            renderable.prepare(null, context, originX, originY);
            this.renderablePhases.put(definition.id(), ReferencePhase.READY);
            return renderable;
        } catch (RuntimeException exception) {
            this.renderablePhases.remove(definition.id());
            throw exception;
        }
    }

    private Object resolveElementValue(String id, String property) {
        LoadingBarDefinition loadingBarDefinition = this.document.loadingBars().get(id);
        if (loadingBarDefinition != null) {
            if (this.loadingBarPhases.get(id) == ReferencePhase.PREPARING) {
                throw this.failure(
                    id, "Circular loading bar reference: element(\"" + id + "\")." + property,
                    null
                );
            }
            LoadingBarRuntime loadingBar = this.prepareLoadingBar(
                loadingBarDefinition, this.data, this.originX(), this.originY(), null
            );
            return loadingBar.element.dslValue(property);
        }

        CanvasDefinition canvasDefinition = this.document.canvases().get(id);
        if (canvasDefinition != null) {
            if (this.canvasPhases.get(id) == ReferencePhase.PREPARING) {
                throw this.failure(
                    id, "Circular canvas reference: element(\"" + id + "\")." + property,
                    null
                );
            }
            CanvasElement canvas = this.canvases.get(id);
            if (canvas == null) {
                canvas = this.prepareCanvasHierarchy(canvasDefinition).canvas();
            }
            return canvas.dslValue(property);
        }

        GridDefinition gridDefinition = this.document.grids().get(id);
        if (gridDefinition != null) {
            GridPhase phase = this.gridPhases.get(id);
            if (phase == GridPhase.CONFIGURING
                || phase == GridPhase.MEASURED && !isMeasuredGridProperty(property)) {
                throw this.failure(id,
                    "Circular element layout reference: element(\"" + id + "\")." + property,
                    null);
            }
            GridLayout grid = phase == GridPhase.MEASURED || phase == GridPhase.PREPARED
                ? this.grids.get(id)
                : this.prepareGrid(
                    gridDefinition, this.data, this.originX(), this.originY(), null
                );
            if (grid == null) {
                throw this.failure(id, "UI grid '" + id + "' could not be prepared", null);
            }
            return grid.dslValue(property);
        }

        RenderableDefinition renderableDefinition = this.document.renderables().get(id);
        if (renderableDefinition != null) {
            if (this.renderablePhases.get(id) == ReferencePhase.PREPARING) {
                throw this.failure(id,
                    "Circular element reference: element(\"" + id + "\")." + property,
                    null);
            }
            UiElement.ElementRenderable renderable = this.prepareRenderable(
                renderableDefinition, this.data, this.originX(), this.originY(), null
            );
            if (renderable == null) {
                throw this.failure(id, "UI element '" + id + "' could not be prepared", null);
            }
            return renderable.dslValue(property);
        }

        DslElement element = this.elements.get(id);
        if (element == null) {
            throw this.failure(id,
                "UI element '" + id + "' is not bound to a Java instance yet", null);
        }
        return element.dslValue(property);
    }

    private static boolean isMeasuredGridProperty(String property) {
        return switch (property) {
            case "w", "width", "h", "height", "size", "count", "visible_size",
                 "visible_count", "columns", "horizontal_gap", "vertical_gap" -> true;
            default -> false;
        };
    }

    @Override
    public final void close() {
        for (UiElement.ElementRenderable renderable : this.renderables.values()) {
            this.recover(renderable.id(), null, () -> {
                renderable.close();
                return null;
            });
        }
        this.elements.forEach((id, element) ->
            this.recover(id, null, () -> {
                element.closeDsl();
                return null;
            })
        );
        this.renderables.clear();
        this.grids.clear();
        this.canvases.clear();
        this.loadingBars.clear();
        this.canvasParents.clear();
        this.canvasPhases.clear();
        this.loadingBarPhases.clear();
        this.gridPhases.clear();
        this.renderablePhases.clear();
        this.invalidGrids.clear();
        this.invalidRenderables.clear();
        this.elements.clear();
    }

    private enum GridPhase {
        CONFIGURING,
        MEASURED,
        PREPARED
    }

    private enum ReferencePhase {
        PREPARING,
        READY
    }

    private record PreparedCanvas(
        CanvasElement canvas,
        VisualElementsInterface childParent,
        float childOriginX,
        float childOriginY
    ) {
    }

    private final class LoadingBarRuntime {
        private final LoadingBarDefinition definition;
        private final CanvasElement fillParent = new CanvasElement();
        private final LoadingBarElement element;
        private UiCompiledDocument.DataContext context;
        private int mouseX;
        private int mouseY;
        private float partialTick;

        private LoadingBarRuntime(LoadingBarDefinition definition) {
            this.definition = definition;
            this.element = new LoadingBarElement(
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                null,
                this::extractBackground,
                this::extractFill
            );
        }

        private void extract(
            GuiGraphicsExtractor graphics,
            UiCompiledDocument.DataContext context,
            int mouseX,
            int mouseY,
            float partialTick
        ) {
            this.context = context;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.partialTick = partialTick;
            this.element.draw(graphics);
        }

        private void extractBackground(GuiGraphicsExtractor graphics) {
            DslUiContext.this.extractEntries(
                this.definition.background(), graphics, this.context,
                this.mouseX, this.mouseY, this.partialTick,
                0.0F, 0.0F, this.element
            );
        }

        private void extractFill(GuiGraphicsExtractor graphics) {
            DslUiContext.this.extractEntries(
                this.definition.fill(), graphics, this.context,
                this.mouseX, this.mouseY, this.partialTick,
                0.0F, 0.0F, this.fillParent
            );
        }
    }
}
