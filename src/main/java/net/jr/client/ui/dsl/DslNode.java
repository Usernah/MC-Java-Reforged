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
import java.util.function.Supplier;


public final class DslNode {
    private final DslUiContext<?> context;
    private final boolean present;
    private final String type;
    private final String id;
    private final Map<String, Expression<Object>> properties;
    private final Map<String, List<EntryDefinition>> renderBlocks;
    private final List<DslBlockDefinition> children;
    private final List<Expression<Object>> arguments;
    private boolean usable;
    private final Set<String> consumedProperties = new HashSet<>();
    private final Set<String> consumedRenderBlocks = new HashSet<>();
    private final Set<String> consumedChildTypes = new HashSet<>();

    public DslNode(DslUiContext<?> context, DslNodeDefinition definition) {
        this(
            context,
            true,
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
        boolean present,
        String type,
        String id,
        Map<String, Expression<Object>> properties,
        Map<String, List<EntryDefinition>> renderBlocks,
        List<DslBlockDefinition> children,
        List<Expression<Object>> arguments
    ) {
        this.context = Objects.requireNonNull(context, "context");
        this.present = present;
        this.type = Objects.requireNonNull(type, "type");
        this.id = id;
        this.properties = properties;
        this.renderBlocks = renderBlocks;
        this.children = children;
        this.arguments = arguments;
        this.usable = present;
    }

    static DslNode missing(DslUiContext<?> context, String id) {
        return new DslNode(
            context,
            false,
            "$missing",
            id,
            Map.of(),
            Map.of(),
            List.of(),
            List.of()
        );
    }


    public boolean isPresent() {
        return this.present;
    }

    public boolean isUsable() {
        return this.present && this.usable;
    }

    private void disable(UiDslRuntimeException exception) {
        if (!this.usable) {
            return;
        }
        this.usable = false;
        UiDocumentManager.getInstance().reportRuntime(exception);
    }

    public String id() {
        return this.id;
    }

    public String type() {
        return this.type;
    }

    public float number(String name) {
        return this.recover(0.0F, () -> {
            Object value = this.resolveRequired(name);
            if (value instanceof Number number) {
                return number.floatValue();
            }
            throw this.wrongType(name, "number", value);
        });
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
        return this.recover("", () -> {
            Object value = this.resolveRequired(name);
            if (value instanceof String text) {
                return text;
            }
            throw this.wrongType(name, "text", value);
        });
    }

    public String textOr(String name, String fallback) {
        return this.properties.containsKey(name) ? this.text(name) : fallback;
    }

    public boolean booleanOr(String name, boolean fallback) {
        Expression<Object> expression = this.properties.get(name);
        if (expression == null) {
            return fallback;
        }
        return this.recover(fallback, () -> {
            this.consumedProperties.add(name);
            Object value = expression.resolve(this.context.data());
            if (value instanceof Boolean bool) {
                return bool;
            }
            throw this.wrongType(name, "boolean", value);
        });
    }

    public Object value(String name) {
        return this.recover(null, () -> this.resolveRequired(name));
    }

    public boolean hasProperty(String name) {
        return this.properties.containsKey(name);
    }


    public void ignoreProperty(String name) {
        if (this.properties.containsKey(name)) {
            this.consumedProperties.add(name);
        }
    }

    public List<EntryDefinition> block(String name) {
        if (!this.isUsable()) {
            return List.of();
        }
        if (this.renderBlocks.containsKey(name)) {
            this.consumedRenderBlocks.add(name);
            return this.renderBlocks.get(name);
        }
        this.consumedChildTypes.add(name);
        return List.of();
    }

    public boolean hasBlock(String name) {
        return this.isUsable() && (this.renderBlocks.containsKey(name)
            || this.children.stream().anyMatch(child -> child.type().equals(name)));
    }

    public List<DslNode> children(String type) {
        if (!this.isUsable()) {
            return List.of();
        }
        this.consumedChildTypes.add(type);
        List<DslNode> result = new ArrayList<>();
        for (DslBlockDefinition child : this.children) {
            if (child.type().equals(type)) {
                result.add(new DslNode(
                    this.context,
                    true,
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
        if (!this.isUsable()) {
            return List.of();
        }
        List<DslNode> result = new ArrayList<>();
        for (DslBlockDefinition child : this.children) {
            this.consumedChildTypes.add(child.type());
            result.add(new DslNode(
                this.context,
                true,
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
        return this.recover("", () -> {
            if (index < 0 || index >= this.arguments.size()) {
                throw this.error("DSL block '" + this.type + "' is missing argument " + index);
            }
            Object value = this.arguments.get(index).resolve(this.context.data());
            if (value instanceof String text) {
                return text;
            }
            throw this.error(
                "Argument " + index + " of DSL block '" + this.type + "' must be text"
            );
        });
    }

    public void finish(UiDslKind<?> kind, DslElement element) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(element, "element");
        if (!this.isUsable()) {
            return;
        }
        if (!this.type.equals(kind.name())) {
            this.disable(this.error(
                "DSL element '" + this.id + "' is '" + this.type + "', not '" + kind.name() + "'"
            ));
            return;
        }
        if (!kind.javaType().isInstance(element)) {
            this.disable(this.error(
                "DSL element '" + this.id + "' requires " + kind.javaType().getName()
            ));
            return;
        }
        this.recover(() -> {
            this.validateConsumed("DSL element '" + this.id + "'");
            this.context.bind(this, element);
        });
    }


    public void finishBlock() {
        if (!this.isUsable()) {
            return;
        }
        this.recover(() -> this.validateConsumed("DSL block '" + this.type + "'"));
    }

    public DslUiContext<?> context() {
        return this.context;
    }

    public UiCompiledDocument.DataContext data() {
        return this.context.data();
    }

    public boolean isLayoutManaged() {
        return this.present && this.context.isLayoutManaged(this.id);
    }

    public UiDslRuntimeException error(String message) {
        return this.context.failure(this.id, normalizeMessage(message, null), null);
    }

    public UiDslRuntimeException error(String message, Throwable cause) {
        return this.context.failure(this.id, normalizeMessage(message, cause), cause);
    }

    public <T> T recover(T fallback, Supplier<T> operation) {
        if (!this.isUsable()) {
            return fallback;
        }
        try {
            T value = operation.get();
            return this.isUsable() ? value : fallback;
        } catch (UiDslRuntimeException exception) {
            this.disable(exception);
        } catch (RuntimeException exception) {
            this.disable(this.error(exception.getMessage(), exception));
        }
        return fallback;
    }

    public void recover(Runnable operation) {
        this.recover(null, () -> {
            operation.run();
            return null;
        });
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
            throw this.error(
                owner + " contains values not consumed by Java: properties=" + unusedProperties
                    + ", blocks=" + unusedRenderBlocks + ", children=" + childTypes
            );
        }
    }

    private Object resolveRequired(String name) {
        Expression<Object> expression = this.properties.get(name);
        if (expression == null) {
            throw this.error(
                "DSL declaration '" + this.id + "' is missing property '" + name + "'"
            );
        }
        this.consumedProperties.add(name);
        try {
            return expression.resolve(this.context.data());
        } catch (UiDslRuntimeException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw this.error(
                "Could not resolve property '" + name + "' of '" + this.id + "': "
                    + exception.getMessage(),
                exception
            );
        }
    }

    private UiDslRuntimeException wrongType(String name, String expected, Object value) {
        return this.error(
            "DSL property '" + name + "' in '" + this.type + "' must be " + expected
                + ", received " + (value == null ? "null" : value.getClass().getSimpleName())
        );
    }

    private static String normalizeMessage(String message, Throwable cause) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return cause == null ? "Unknown DSL runtime error" : cause.getClass().getSimpleName();
    }
}
