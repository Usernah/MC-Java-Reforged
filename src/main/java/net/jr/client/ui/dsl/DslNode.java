package net.jr.client.ui.dsl;

import net.jr.client.ui.dsl.UiCompiledDocument.DslBlockDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.DslNodeDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Runtime view over one Java-owned DSL declaration or one of its blocks. */
public final class DslNode {
    private final DslUiContext<?> context;
    private final String type;
    private final String id;
    private final Map<String, Expression<Object>> properties;
    private final Map<String, List<EntryDefinition>> renderBlocks;
    private final List<DslBlockDefinition> children;
    private final List<Expression<Object>> arguments;
    private final Set<String> consumedProperties = new HashSet<>();
    private final Set<String> consumedRenderBlocks = new HashSet<>();
    private final Set<String> consumedChildTypes = new HashSet<>();

    public DslNode(DslUiContext<?> context, DslNodeDefinition definition) {
        this(
            context,
            definition.type(),
            definition.id(),
            definition.properties(),
            definition.renderBlocks(),
            definition.children(),
            List.of()
        );
    }

    private DslNode(
        DslUiContext<?> context,
        String type,
        String id,
        Map<String, Expression<Object>> properties,
        Map<String, List<EntryDefinition>> renderBlocks,
        List<DslBlockDefinition> children,
        List<Expression<Object>> arguments
    ) {
        this.context = Objects.requireNonNull(context, "context");
        this.type = Objects.requireNonNull(type, "type");
        this.id = id;
        this.properties = properties;
        this.renderBlocks = renderBlocks;
        this.children = children;
        this.arguments = arguments;
    }

    public String id() {
        return this.id;
    }

    public String type() {
        return this.type;
    }

    public float number(String name) {
        Object value = this.resolveRequired(name);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        throw this.wrongType(name, "number", value);
    }

    public float numberOr(String name, float fallback) {
        return this.properties.containsKey(name) ? this.number(name) : fallback;
    }

    public int integer(String name) {
        return Math.round(this.number(name));
    }

    public int integerOr(String name, int fallback) {
        return this.properties.containsKey(name) ? this.integer(name) : fallback;
    }

    public String text(String name) {
        Object value = this.resolveRequired(name);
        if (value instanceof String text) {
            return text;
        }
        throw this.wrongType(name, "text", value);
    }

    public boolean booleanOr(String name, boolean fallback) {
        Expression<Object> expression = this.properties.get(name);
        if (expression == null) {
            return fallback;
        }
        this.consumedProperties.add(name);
        Object value = expression.resolve(this.context.data());
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw this.wrongType(name, "boolean", value);
    }

    public Object value(String name) {
        return this.resolveRequired(name);
    }

    public boolean hasProperty(String name) {
        return this.properties.containsKey(name);
    }

    public List<EntryDefinition> block(String name) {
        if (this.renderBlocks.containsKey(name)) {
            this.consumedRenderBlocks.add(name);
            return this.renderBlocks.get(name);
        }
        this.consumedChildTypes.add(name);
        return List.of();
    }

    public boolean hasBlock(String name) {
        return this.renderBlocks.containsKey(name)
            || this.children.stream().anyMatch(child -> child.type().equals(name));
    }

    public List<DslNode> children(String type) {
        this.consumedChildTypes.add(type);
        List<DslNode> result = new ArrayList<>();
        for (DslBlockDefinition child : this.children) {
            if (child.type().equals(type)) {
                result.add(new DslNode(
                    this.context,
                    child.type(),
                    child.id(),
                    child.properties(),
                    child.renderBlocks(),
                    child.children(),
                    child.arguments()
                ));
            }
        }
        return List.copyOf(result);
    }

    public List<DslNode> children() {
        List<DslNode> result = new ArrayList<>();
        for (DslBlockDefinition child : this.children) {
            this.consumedChildTypes.add(child.type());
            result.add(new DslNode(
                this.context,
                child.type(),
                child.id(),
                child.properties(),
                child.renderBlocks(),
                child.children(),
                child.arguments()
            ));
        }
        return List.copyOf(result);
    }

    public String argumentText(int index) {
        if (index < 0 || index >= this.arguments.size()) {
            throw new IllegalArgumentException("DSL block '" + this.type + "' is missing argument " + index);
        }
        Object value = this.arguments.get(index).resolve(this.context.data());
        if (value instanceof String text) {
            return text;
        }
        throw new IllegalArgumentException(
            "Argument " + index + " of DSL block '" + this.type + "' must be text"
        );
    }

    public void finish(UiDslKind<?> kind, DslElement element) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(element, "element");
        if (!this.type.equals(kind.name())) {
            throw new IllegalArgumentException(
                "DSL element '" + this.id + "' is '" + this.type + "', not '" + kind.name() + "'"
            );
        }
        if (!kind.javaType().isInstance(element)) {
            throw new IllegalArgumentException(
                "DSL element '" + this.id + "' requires " + kind.javaType().getName()
            );
        }
        this.validateConsumed("DSL element '" + this.id + "'");
        this.context.bind(this, element);
    }

    /** Ensures a Java-owned child block consumed every value it declared. */
    public void finishBlock() {
        this.validateConsumed("DSL block '" + this.type + "'");
    }

    public DslUiContext<?> context() {
        return this.context;
    }

    public UiCompiledDocument.DataContext data() {
        return this.context.data();
    }

    private void validateConsumed(String owner) {
        Set<String> unusedProperties = new HashSet<>(this.properties.keySet());
        unusedProperties.removeAll(this.consumedProperties);
        Set<String> unusedRenderBlocks = new HashSet<>(this.renderBlocks.keySet());
        unusedRenderBlocks.removeAll(this.consumedRenderBlocks);
        Set<String> childTypes = new HashSet<>();
        this.children.forEach(child -> childTypes.add(child.type()));
        childTypes.removeAll(this.consumedChildTypes);
        if (!unusedProperties.isEmpty() || !unusedRenderBlocks.isEmpty() || !childTypes.isEmpty()) {
            throw new IllegalArgumentException(
                owner + " contains values not consumed by Java: properties=" + unusedProperties
                    + ", blocks=" + unusedRenderBlocks + ", children=" + childTypes
            );
        }
    }

    private Object resolveRequired(String name) {
        Expression<Object> expression = this.properties.get(name);
        if (expression == null) {
            throw new IllegalArgumentException(
                "DSL declaration '" + this.id + "' is missing property '" + name + "'"
            );
        }
        this.consumedProperties.add(name);
        return expression.resolve(this.context.data());
    }

    private IllegalArgumentException wrongType(String name, String expected, Object value) {
        return new IllegalArgumentException(
            "DSL property '" + name + "' in '" + this.type + "' must be " + expected
                + ", received " + (value == null ? "null" : value.getClass().getSimpleName())
        );
    }
}
