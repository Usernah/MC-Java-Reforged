package net.jr.client.ui.dsl;

import net.jr.api.client.ui.UiFile;
import net.jr.client.components.widgets.JavaBaseWidget;
import net.jr.client.ui.UiElement;
import net.jr.client.ui.dsl.UiCompiledDocument.DslNodeDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.RenderableDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Shared runtime for a compiled DSL document hosted by a Minecraft UI. */
public abstract class DslUiContext<F extends UiFile> implements AutoCloseable {
    private final Screen host;
    private final F file;
    private final UiCompiledDocument document;
    private final UiDataContext data;
    private final Map<String, DslElement> elements = new LinkedHashMap<>();
    private final Map<RenderableDefinition, UiElement.ElementRenderable> renderables =
        new IdentityHashMap<>();

    protected DslUiContext(Screen host, F file) {
        this.host = Objects.requireNonNull(host, "host");
        this.file = Objects.requireNonNull(file, "file");
        this.document = UiDocumentManager.getInstance().requireCompiled(file);
        this.data = UiDataContext.withGlobals();
    }

    public final Screen host() {
        return this.host;
    }

    public final F file() {
        return this.file;
    }

    public final UiDataContext data() {
        return this.data;
    }

    public final boolean hasProperty(String name) {
        return this.document.hasProperty(name);
    }

    public final Object property(String name) {
        return this.document.property(name, this.data);
    }

    public final boolean hasBounds() {
        return this.document.hasBounds();
    }

    public final int boundsX() {
        return this.document.boundsX(this.data);
    }

    public final int boundsY() {
        return this.document.boundsY(this.data);
    }

    public final int boundsWidth() {
        return this.document.boundsWidth(this.data);
    }

    public final int boundsHeight() {
        return this.document.boundsHeight(this.data);
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
        return new DslNode(this, this.document.requireElement(id));
    }

    public final void bind(DslNode node, DslElement element) {
        DslElement previous = this.elements.putIfAbsent(node.id(), element);
        if (previous != null && previous != element) {
            throw new IllegalStateException("DSL element '" + node.id() + "' is already bound");
        }
        if (element instanceof JavaBaseWidget widget) {
            ((DslHostBridge)this.host).javaReforged$addDslWidget(widget);
        }
    }

    public final void extract(
        UiRenderLayer layer,
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        this.extractEntries(
            this.document.entries(layer), graphics, this.data, mouseX, mouseY, partialTick,
            this.originX(), this.originY()
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
        this.extractEntries(entries, graphics, context, mouseX, mouseY, partialTick, originX, originY);
    }

    private void extractEntries(
        List<EntryDefinition> entries,
        GuiGraphicsExtractor graphics,
        UiCompiledDocument.DataContext context,
        int mouseX,
        int mouseY,
        float partialTick,
        float originX,
        float originY
    ) {
        for (EntryDefinition entry : entries) {
            if (entry instanceof RenderableDefinition definition) {
                this.renderables
                    .computeIfAbsent(definition, UiElement::renderable)
                    .render(graphics, context, originX, originY);
                continue;
            }
            if (!(entry instanceof DslNodeDefinition)) {
                throw new IllegalStateException("Unsupported UI entry: " + entry.getClass().getName());
            }
            DslElement element = this.elements.get(entry.id());
            if (element == null) {
                throw new IllegalStateException(
                    "DSL element '" + entry.id() + "' was declared but no Java instance was created"
                );
            }
            element.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public final void close() {
        for (UiElement.ElementRenderable renderable : this.renderables.values()) {
            renderable.close();
        }
        for (DslElement element : this.elements.values()) {
            element.closeDsl();
        }
        this.renderables.clear();
        this.elements.clear();
    }
}
