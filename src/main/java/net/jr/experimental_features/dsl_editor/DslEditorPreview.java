package net.jr.experimental_features.dsl_editor;

import net.jr.api.client.resource.Asset;
import net.jr.client.components.elements.CanvasElement;
import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.components.elements.VisualElementsInterface;
import net.jr.client.ui.UiElement;
import net.jr.client.ui.dsl.UiCompiledDocument;
import net.jr.client.ui.dsl.UiCompiledDocument.CanvasDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.ConditionalEntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.ConditionalRenderableDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.LoadingBarDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.RenderableDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.TextDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.TextBoxDefinition;
import net.jr.client.ui.dsl.UiDataContext;
import net.jr.client.ui.dsl.UiDocumentManager;
import net.jr.client.ui.dsl.UiRenderLayer;
import net.jr.client.ui.presentation.UiResolution;
import net.jr.client.ui.presentation.UiPresentationPolicy;
import net.jr.client.ui.presentation.UiSizeMode;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class DslEditorPreview implements AutoCloseable {
    private final Map<RenderableDefinition, UiElement.ElementRenderable> renderables =
        new IdentityHashMap<>();
    private final Map<CanvasDefinition, CanvasElement> canvases = new IdentityHashMap<>();
    private final List<ElementBox> boxes = new ArrayList<>();
    private final UiDataContext data = UiDataContext.withGlobals();
    private UiCompiledDocument document;

    void begin(UiCompiledDocument document) {
        if (this.document != document) {
            this.closeRenderables();
            this.document = document;
        }
        this.boxes.clear();
    }

    void render(
        GuiGraphicsExtractor graphics,
        UiCompiledDocument document,
        boolean smallPresentation,
        int viewportWidth,
        int viewportHeight,
        int fullWidth,
        int fullHeight,
        boolean captureBoxes
    ) {
        this.data.exposeBoolean("is.small", () -> smallPresentation);
        this.data.exposeNumber("viewport.width", () -> viewportWidth);
        this.data.exposeNumber("viewport.height", () -> viewportHeight);
        this.data.exposeNumber("viewport.full_width", () -> fullWidth);
        this.data.exposeNumber("viewport.full_height", () -> fullHeight);
        this.data.exposeBoolean("global.full_hd", () -> !smallPresentation);
        this.data.exposeAsset(
            "global.font",
            () -> (smallPresentation ? UiResolution.HD : UiResolution.FULL_HD).font().location()
        );
        try (UiPresentationPolicy.Scope ignored = UiPresentationPolicy.push(
                smallPresentation ? UiSizeMode.SMALL : UiSizeMode.NORMAL
        )) {
            for (UiRenderLayer layer : UiRenderLayer.values()) {
                this.renderEntries(
                    graphics, document.entries(layer), null, 0.0F, 0.0F,
                    document, captureBoxes
                );
            }
        }
    }

    List<ElementBox> boxes() {
        return List.copyOf(this.boxes);
    }

    private void renderEntries(
        GuiGraphicsExtractor graphics,
        List<EntryDefinition> entries,
        VisualElementsInterface parent,
        float originX,
        float originY,
        UiCompiledDocument document,
        boolean captureBoxes
    ) {
        for (EntryDefinition entry : entries) {
            try {
                if (entry instanceof ConditionalEntryDefinition conditional) {
                    if (conditional.condition().resolve(this.data)) {
                        this.renderEntries(
                            graphics, List.of(conditional.entry()), parent,
                            originX, originY, document, captureBoxes
                        );
                    }
                } else if (entry instanceof CanvasDefinition canvasDefinition) {
                    CanvasElement canvas = this.canvases.computeIfAbsent(
                        canvasDefinition, ignored -> new CanvasElement()
                    );
                    canvas.setLayoutParent(parent);
                    VisualAnchor anchor = canvasDefinition.anchor().resolve(this.data);
                    canvas.setAnchor(anchor);
                    canvas.setBounds(
                        anchor.isExplicit() ? canvasDefinition.x().resolve(this.data)
                            : originX + canvasDefinition.x().resolve(this.data),
                        anchor.isExplicit() ? canvasDefinition.y().resolve(this.data)
                            : originY + canvasDefinition.y().resolve(this.data),
                        canvasDefinition.width().resolve(this.data),
                        canvasDefinition.height().resolve(this.data)
                    );
                    VisualElementsInterface childParent = anchor.isExplicit() ? canvas : parent;
                    this.renderEntries(
                        graphics,
                        canvasDefinition.entries(),
                        childParent,
                        anchor.isExplicit() ? 0.0F : originX,
                        anchor.isExplicit() ? 0.0F : originY,
                        document,
                        captureBoxes
                    );
                } else if (entry instanceof LoadingBarDefinition loading) {
                    this.renderEntries(
                        graphics, loading.background(), parent, originX, originY,
                        document, captureBoxes
                    );
                    this.renderEntries(
                        graphics, loading.fill(), parent, originX, originY,
                        document, captureBoxes
                    );
                } else if (entry instanceof RenderableDefinition renderableDefinition) {
                    RenderableDefinition actual = renderableDefinition instanceof ConditionalRenderableDefinition conditional
                        ? conditional.renderable() : renderableDefinition;
                    if (renderableDefinition instanceof ConditionalRenderableDefinition conditional
                        && !conditional.condition().resolve(this.data)) {
                        continue;
                    }
                    UiElement.ElementRenderable renderable = this.renderables.computeIfAbsent(
                        actual, UiElement::renderable
                    );
                    renderable.setLayoutParent(parent);
                    renderable.render(graphics, this.data, originX, originY);
                    if (captureBoxes) {
                        this.captureBox(document, actual, renderable);
                    }
                }
            } catch (RuntimeException ignored) {
                // El editor sigue mostrando los elementos que sí pueden resolverse sin el host Java.
            }
        }
    }

    private void captureBox(
        UiCompiledDocument document,
        RenderableDefinition definition,
        UiElement.ElementRenderable renderable
    ) {
        float rawX = number(renderable.dslValue("x"));
        float rawY = number(renderable.dslValue("y"));
        float x = number(renderable.dslValue("resolved_x"));
        float y = number(renderable.dslValue("resolved_y"));
        float width = number(renderable.dslValue("w"));
        float height = number(renderable.dslValue("h"));
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        UiCompiledDocument.SourceLocation definitionLocation = document.location(definition.id());
        UiCompiledDocument.SourceLocation xLocation = document.propertyLocation(definition.id(), "x");
        UiCompiledDocument.SourceLocation yLocation = document.propertyLocation(definition.id(), "y");
        UiCompiledDocument.SourceLocation widthLocation = document.propertyLocation(definition.id(), "w");
        UiCompiledDocument.SourceLocation heightLocation = document.propertyLocation(definition.id(), "h");
        boolean text = definition instanceof TextDefinition || definition instanceof TextBoxDefinition;
        boolean blueprint = !definitionLocation.source().equals(document.source())
            || !definitionLocation.source().equals(xLocation.source());
        String condition = UiDocumentManager.getInstance().sourceText(xLocation.source())
            .map(source -> DslSourceEditor.conditionNear(source, xLocation.line()))
            .orElse("");
        this.boxes.add(new ElementBox(
            definition.id(), definition.getClass().getSimpleName(),
            xLocation.source(), xLocation.line(),
            xLocation, yLocation, widthLocation, heightLocation,
            rawX, rawY, x, y, width, height, text, blueprint, condition
        ));
    }

    private static float number(Object value) {
        return value instanceof Number number ? number.floatValue() : 0.0F;
    }

    @Override
    public void close() {
        this.closeRenderables();
        this.document = null;
    }

    private void closeRenderables() {
        this.renderables.values().forEach(UiElement.ElementRenderable::close);
        this.renderables.clear();
        this.canvases.clear();
        this.boxes.clear();
    }

    record ElementBox(
        String id,
        String type,
        Asset source,
        int line,
        UiCompiledDocument.SourceLocation xLocation,
        UiCompiledDocument.SourceLocation yLocation,
        UiCompiledDocument.SourceLocation widthLocation,
        UiCompiledDocument.SourceLocation heightLocation,
        float rawX,
        float rawY,
        float x,
        float y,
        float width,
        float height,
        boolean text,
        boolean blueprint,
        String condition
    ) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }
}
