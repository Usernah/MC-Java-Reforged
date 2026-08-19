package net.jr.api.client.ui.dsl;

import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiFileType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class UiDocument {
    private final Asset source;
    private final UiFileType type;
    private final List<Statement> statements;

    public UiDocument(Asset source, UiFileType type, List<Statement> statements) {
        this.source = Objects.requireNonNull(source, "source");
        this.type = Objects.requireNonNull(type, "type");
        this.statements = List.copyOf(statements);
    }

    public Asset source() {
        return this.source;
    }

    public UiFileType type() {
        return this.type;
    }

    public List<Statement> statements() {
        return this.statements;
    }

    public List<ImportDirective> imports() {
        return this.statements.stream()
            .filter(ImportDirective.class::isInstance)
            .map(ImportDirective.class::cast)
            .toList();
    }

    public Map<String, Value> definitions() {
        Map<String, Value> definitions = new LinkedHashMap<>();
        for (Statement statement : this.statements) {
            if (statement instanceof DefinitionDirective definition) {
                definitions.put(definition.name(), definition.value());
            }
        }
        return Collections.unmodifiableMap(definitions);
    }

    public Optional<Value> definition(String name) {
        return Optional.ofNullable(this.definitions().get(name));
    }

    public Map<String, BlueprintDirective> blueprints() {
        Map<String, BlueprintDirective> blueprints = new LinkedHashMap<>();
        for (Statement statement : this.statements) {
            if (statement instanceof BlueprintDirective blueprint) {
                blueprints.put(blueprint.name(), blueprint);
            }
        }
        return Collections.unmodifiableMap(blueprints);
    }

    public Optional<BlueprintDirective> blueprint(String name) {
        return Optional.ofNullable(this.blueprints().get(name));
    }

    public record Position(int line, int column) {
        public Position {
            if (line < 1 || column < 1) {
                throw new IllegalArgumentException("Line and column must be positive");
            }
        }
    }

    public sealed interface Statement permits ImportDirective, DefinitionDirective, BlueprintDirective,
        BlueprintUse, ConditionalStatement, Node, Property {
        Position position();
    }

    public record ImportDirective(ImportTarget target, String alias, Position position) implements Statement {
        public ImportDirective {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(alias, "alias");
            Objects.requireNonNull(position, "position");
        }
    }

    public record ImportTarget(boolean local, String namespace, String path) {
        public ImportTarget {
            Objects.requireNonNull(path, "path");
            if (!local) {
                Objects.requireNonNull(namespace, "namespace");
            }
        }
    }

    public record DefinitionDirective(String name, Value value, Position position) implements Statement {
        public DefinitionDirective {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(position, "position");
        }
    }

    public record BlueprintDirective(
        String name,
        List<String> parameters,
        List<Statement> body,
        Position position
    ) implements Statement {
        public BlueprintDirective {
            Objects.requireNonNull(name, "name");
            parameters = List.copyOf(parameters);
            body = List.copyOf(body);
            Objects.requireNonNull(position, "position");
        }
    }

    public record BlueprintUse(
        List<String> path,
        String instanceId,
        List<Value> arguments,
        Position position
    ) implements Statement {
        public BlueprintUse {
            path = immutablePath(path);
            Objects.requireNonNull(instanceId, "instanceId");
            arguments = List.copyOf(arguments);
            Objects.requireNonNull(position, "position");
        }
    }

    public record ConditionalStatement(Value condition, List<Statement> body, Position position)
        implements Statement {
        public ConditionalStatement {
            Objects.requireNonNull(condition, "condition");
            body = List.copyOf(body);
            Objects.requireNonNull(position, "position");
        }
    }

    public record Node(String name, List<Value> arguments, List<Statement> body, Position position)
        implements Statement {
        public Node {
            Objects.requireNonNull(name, "name");
            arguments = List.copyOf(arguments);
            body = List.copyOf(body);
            Objects.requireNonNull(position, "position");
        }
    }

    public record Property(String name, Value value, Position position) implements Statement {
        public Property {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(position, "position");
        }
    }

    public sealed interface Value permits StringValue, NumberValue, BooleanValue, NullValue,
        ColorValue, BareValue, ReferenceValue, BlueprintParameterValue, ExposedDataValue, ListValue, TupleValue,
        ObjectValue, CallValue, ConditionalValue, LogicalValue, NegatedValue, ComparisonValue,
        ArithmeticValue, ArithmeticNegationValue {
        Position position();
    }

    public record StringValue(String value, Position position) implements Value {
        public StringValue {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(position, "position");
        }
    }

    public record NumberValue(String literal, Position position) implements Value {
        public NumberValue {
            Objects.requireNonNull(literal, "literal");
            Objects.requireNonNull(position, "position");
        }
    }

    public record BooleanValue(boolean value, Position position) implements Value {
        public BooleanValue {
            Objects.requireNonNull(position, "position");
        }
    }

    public record NullValue(Position position) implements Value {
        public NullValue {
            Objects.requireNonNull(position, "position");
        }
    }

    public record ColorValue(String literal, Position position) implements Value {
        public ColorValue {
            Objects.requireNonNull(literal, "literal");
            Objects.requireNonNull(position, "position");
        }
    }

    public record BareValue(String value, Position position) implements Value {
        public BareValue {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(position, "position");
        }
    }

    public record ReferenceValue(List<String> path, Position position) implements Value {
        public ReferenceValue {
            path = immutablePath(path);
            Objects.requireNonNull(position, "position");
        }
    }

    public record BlueprintParameterValue(List<String> path, Position position) implements Value {
        public BlueprintParameterValue {
            path = immutablePath(path);
            Objects.requireNonNull(position, "position");
        }
    }

    public record ExposedDataValue(List<String> path, Position position) implements Value {
        public ExposedDataValue {
            path = immutablePath(path);
            Objects.requireNonNull(position, "position");
        }
    }

    public record ListValue(List<Value> values, Position position) implements Value {
        public ListValue {
            values = List.copyOf(values);
            Objects.requireNonNull(position, "position");
        }
    }

    public record TupleValue(List<Value> values, Position position) implements Value {
        public TupleValue {
            values = List.copyOf(values);
            Objects.requireNonNull(position, "position");
        }
    }

    public record ObjectValue(Map<String, Value> values, Position position) implements Value {
        public ObjectValue {
            values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
            Objects.requireNonNull(position, "position");
        }
    }

    public record CallValue(String name, List<Value> arguments, Position position) implements Value {
        public CallValue {
            Objects.requireNonNull(name, "name");
            arguments = List.copyOf(arguments);
            Objects.requireNonNull(position, "position");
        }
    }

    public record ConditionalValue(Value condition, Value whenTrue, Value whenFalse, Position position)
        implements Value {
        public ConditionalValue {
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(whenTrue, "whenTrue");
            Objects.requireNonNull(whenFalse, "whenFalse");
            Objects.requireNonNull(position, "position");
        }
    }

    public enum LogicalOperator {
        AND,
        OR
    }

    public record LogicalValue(
        Value left,
        LogicalOperator operator,
        Value right,
        Position position
    ) implements Value {
        public LogicalValue {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(position, "position");
        }
    }

    public record NegatedValue(Value value, Position position) implements Value {
        public NegatedValue {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(position, "position");
        }
    }

    public enum ComparisonOperator {
        EQUAL,
        NOT_EQUAL,
        LESS,
        LESS_OR_EQUAL,
        GREATER,
        GREATER_OR_EQUAL
    }

    public record ComparisonValue(
        Value left,
        ComparisonOperator operator,
        Value right,
        Position position
    ) implements Value {
        public ComparisonValue {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(position, "position");
        }
    }

    public enum ArithmeticOperator {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        MODULO
    }

    public record ArithmeticValue(
        Value left,
        ArithmeticOperator operator,
        Value right,
        Position position
    ) implements Value {
        public ArithmeticValue {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(position, "position");
        }
    }

    public record ArithmeticNegationValue(Value value, Position position) implements Value {
        public ArithmeticNegationValue {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(position, "position");
        }
    }

    private static List<String> immutablePath(List<String> path) {
        Objects.requireNonNull(path, "path");
        if (path.isEmpty()) {
            throw new IllegalArgumentException("A symbol path cannot be empty");
        }
        return Collections.unmodifiableList(new ArrayList<>(path));
    }
}
