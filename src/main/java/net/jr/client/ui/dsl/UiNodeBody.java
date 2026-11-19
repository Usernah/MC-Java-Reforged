package net.jr.client.ui.dsl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.Statement;
import net.jr.client.ui.dsl.UiDocument.Value;

final class UiNodeBody {
    private final UiDocument document;
    private final Node owner;
    private final Map<String, Value> properties;
    private final Map<String, Node> nodes;

    private UiNodeBody(
        UiDocument document,
        Node owner,
        Map<String, Value> properties,
        Map<String, Node> nodes
    ) {
        this.document = document;
        this.owner = owner;
        this.properties = Map.copyOf(properties);
        this.nodes = Map.copyOf(nodes);
    }

    static UiNodeBody read(
        UiCompilationContext context,
        UiDocument document,
        Node node,
        List<String> allowedProperties,
        List<String> allowedNodes
    ) {
        Map<String, Value> properties = new LinkedHashMap<>();
        Map<String, Node> nodes = new LinkedHashMap<>();
        for (Statement statement : node.body()) {
            if (statement instanceof Property property) {
                if (property.name().equals("id")) {
                    continue;
                }
                if (!allowedProperties.contains(property.name())) {
                    context.diagnostics().add(context.error(
                        document,
                        property.position(),
                        "Unsupported property '" + property.name() + "' in '" + node.name() + "'"
                    ));
                    continue;
                }
                if (properties.putIfAbsent(property.name(), property.value()) != null) {
                    context.diagnostics().add(context.error(
                        document, property.position(), "Duplicate property '" + property.name() + "'"
                    ));
                }
            } else if (statement instanceof Node child) {
                if (!allowedNodes.contains(child.name())) {
                    context.diagnostics().add(context.error(
                        document, child.position(), "Unsupported block '" + child.name() + "'"
                    ));
                    continue;
                }
                if (nodes.putIfAbsent(child.name(), child) != null) {
                    context.diagnostics().add(context.error(
                        document, child.position(), "Duplicate block '" + child.name() + "'"
                    ));
                }
            } else {
                context.diagnostics().add(context.error(
                    document, statement.position(), "Directives are not valid inside this block"
                ));
            }
        }
        return new UiNodeBody(document, node, properties, nodes);
    }

    Value property(String name) {
        return this.properties.get(name);
    }

    Value require(String name) throws UiCompileException {
        Value value = this.property(name);
        if (value == null) {
            throw new UiCompileException(
                this.document.source(),
                this.owner.position(),
                "Missing required property '" + name + "'"
            );
        }
        return value;
    }

    Node node(String name) {
        return this.nodes.get(name);
    }
}
