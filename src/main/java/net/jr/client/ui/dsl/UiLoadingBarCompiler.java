package net.jr.client.ui.dsl;

import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiCompiledDocument.LoadingBarDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiBlueprintCompiler.Key;

import java.util.Deque;
import java.util.List;
import java.util.Map;

final class UiLoadingBarCompiler implements UiLayoutCompiler {
    @Override
    public String name() {
        return "loading_bar";
    }

    @Override
    public LoadingBarDefinition compile(
        UiLayoutContext context,
        UiDocument document,
        Node node,
        Deque<Key> expansionStack,
        boolean allowThis
    ) throws UiCompileException {
        UiNodeBody body = context.elements().body(
            document,
            node,
            List.of(
                "id", "anchor", "x", "y", "w", "h",
                "fill_x", "fill_y", "fill_w", "fill_h", "progress"
            ),
            List.of("background", "fill")
        );
        Node background = body.node("background");
        if (background == null) {
            throw context.elements().error(document, node.position(),
                "Loading bar requires a 'background' block");
        }
        Node fill = body.node("fill");
        if (fill == null) {
            throw context.elements().error(document, node.position(),
                "Loading bar requires a 'fill' block");
        }

        Expression<VisualAnchor> anchor = body.property("anchor") == null
            ? context.elements().expressions().constant(VisualAnchor.NONE)
            : context.elements().expressions().visualAnchor(document, body.property("anchor"), allowThis, 0);
        Expression<Float> width = context.elements().expressions().number(document, body.require("w"), allowThis, 0);
        Expression<Float> height = context.elements().expressions().number(document, body.require("h"), allowThis, 0);
        List<EntryDefinition> backgroundEntries = context.compile(
            document, background.body(), allowThis, Map.of(), expansionStack
        );
        List<EntryDefinition> fillEntries = context.compile(
            document, fill.body(), allowThis, Map.of(), expansionStack
        );
        context.requireVisual(document, background, backgroundEntries);
        context.requireVisual(document, fill, fillEntries);
        return new LoadingBarDefinition(
            context.elements().id(document, node),
            anchor,
            context.elements().expressions().number(document, body.require("x"), allowThis, 0),
            context.elements().expressions().number(document, body.require("y"), allowThis, 0),
            width,
            height,
            body.property("fill_x") == null
                ? context.elements().expressions().constant(0.0F)
                : context.elements().expressions().number(document, body.property("fill_x"), allowThis, 0),
            body.property("fill_y") == null
                ? context.elements().expressions().constant(0.0F)
                : context.elements().expressions().number(document, body.property("fill_y"), allowThis, 0),
            body.property("fill_w") == null
                ? width
                : context.elements().expressions().number(document, body.property("fill_w"), allowThis, 0),
            body.property("fill_h") == null
                ? height
                : context.elements().expressions().number(document, body.property("fill_h"), allowThis, 0),
            context.elements().expressions().number(document, body.require("progress"), allowThis, 0),
            backgroundEntries,
            fillEntries
        );
    }

}

