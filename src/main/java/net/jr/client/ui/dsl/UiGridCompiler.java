package net.jr.client.ui.dsl;

import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.components.widgets.GridDirection;
import net.jr.client.ui.dsl.UiCompiledDocument.GridDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.Statement;
import net.jr.client.ui.dsl.UiDocument.Value;
import net.jr.client.ui.dsl.UiBlueprintCompiler.Key;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class UiGridCompiler implements UiLayoutCompiler {
    @Override
    public String name() {
        return "grid";
    }

    @Override
    public GridDefinition compile(
        UiLayoutContext context,
        UiDocument document,
        Node node,
        Deque<Key> expansionStack,
        boolean allowThis
    ) throws UiCompileException {
        if (allowThis) {
            throw context.elements().error(document, node.position(),
                "A visual-only block cannot contain a grid");
        }
        Map<String, Value> properties = new LinkedHashMap<>();
        List<Statement> children = new ArrayList<>();
        List<String> allowed = List.of(
            "id", "anchor", "x", "y", "columns", "horizontal_gap", "vertical_gap", "direction"
        );
        for (Statement statement : node.body()) {
            if (statement instanceof Property property) {
                if (!allowed.contains(property.name())) {
                    context.report(context.elements().error(document, property.position(),
                        "Unsupported property '" + property.name() + "' in 'grid'"));
                    continue;
                }
                if (properties.putIfAbsent(property.name(), property.value()) != null) {
                    context.report(context.elements().error(document, property.position(),
                        "Duplicate property '" + property.name() + "'"));
                }
            } else {
                children.add(statement);
            }
        }

        Value anchor = properties.get("anchor");
        Value horizontalGap = properties.get("horizontal_gap");
        Value verticalGap = properties.get("vertical_gap");
        return new GridDefinition(
            context.elements().id(document, node),
            anchor == null
                ? context.elements().expressions().constant(VisualAnchor.NONE)
                : context.elements().expressions().visualAnchor(document, anchor, false, 0),
            context.elements().expressions().number(document, requireGridProperty(document, node, properties, "x"), false, 0),
            context.elements().expressions().number(document, requireGridProperty(document, node, properties, "y"), false, 0),
            context.elements().expressions().integer(document, requireGridProperty(document, node, properties, "columns"), false, 0),
            horizontalGap == null ? context.elements().expressions().constant(0) : context.elements().expressions().integer(document, horizontalGap, false, 0),
            verticalGap == null ? context.elements().expressions().constant(0) : context.elements().expressions().integer(document, verticalGap, false, 0),
            context.elements().expressions().gridDirection(
                document,
                requireGridProperty(document, node, properties, "direction"),
                false,
                0
            ),
            context.compile(document, children, false, Map.of(), expansionStack)
        );
    }

    private static Value requireGridProperty(
        UiDocument document,
        Node node,
        Map<String, Value> properties,
        String name
    ) throws UiCompileException {
        Value value = properties.get(name);
        if (value == null) {
            throw new UiCompileException(
                document.source(), node.position(), "Missing required property '" + name + "' in 'grid'"
            );
        }
        return value;
    }

}

