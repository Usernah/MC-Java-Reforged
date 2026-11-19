package net.jr.client.ui.dsl;

import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiCompiledDocument.CanvasDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiBlueprintCompiler.Key;

import java.util.Deque;
import java.util.List;
import java.util.Map;

final class UiCanvasCompiler implements UiLayoutCompiler {
    @Override
    public String name() {
        return "canvas";
    }

    @Override
    public CanvasDefinition compile(
        UiLayoutContext context,
        UiDocument document,
        Node node,
        Deque<Key> expansionStack,
        boolean allowThis
    ) throws UiCompileException {
        UiNodeBody body = context.elements().body(
            document,
            node,
            List.of("id", "anchor", "x", "y", "w", "h"),
            List.of("content")
        );
        Node content = body.node("content");
        if (content == null) {
            throw context.elements().error(document, node.position(), "Canvas requires a 'content' block");
        }
        Expression<VisualAnchor> anchor = body.property("anchor") == null
            ? context.elements().expressions().constant(VisualAnchor.NONE)
            : context.elements().expressions().visualAnchor(document, body.property("anchor"), allowThis, 0);
        return new CanvasDefinition(
            context.elements().id(document, node),
            anchor,
            context.elements().expressions().number(document, body.require("x"), allowThis, 0),
            context.elements().expressions().number(document, body.require("y"), allowThis, 0),
            context.elements().expressions().number(document, body.require("w"), allowThis, 0),
            context.elements().expressions().number(document, body.require("h"), allowThis, 0),
            context.compile(
                document, content.body(), allowThis, Map.of(), expansionStack
            )
        );
    }

}

