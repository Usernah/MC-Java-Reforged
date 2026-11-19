package net.jr.client.ui.dsl;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.client.components.elements.VisualAnchor;
import net.jr.client.components.widgets.GridDirection;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiCompiledDocument.Vector2Definition;
import net.jr.client.ui.dsl.UiDocument.ArithmeticNegationValue;
import net.jr.client.ui.dsl.UiDocument.ArithmeticValue;
import net.jr.client.ui.dsl.UiDocument.BooleanValue;
import net.jr.client.ui.dsl.UiDocument.CallValue;
import net.jr.client.ui.dsl.UiDocument.ConditionalValue;
import net.jr.client.ui.dsl.UiDocument.ComparisonValue;
import net.jr.client.ui.dsl.UiDocument.ExposedDataValue;
import net.jr.client.ui.dsl.UiDocument.GlyphValue;
import net.jr.api.client.render.glyph.Glyphs;
import net.jr.client.ui.dsl.UiDocument.ElementValue;
import net.jr.client.ui.dsl.UiDocument.ListValue;
import net.jr.client.ui.dsl.UiDocument.LogicalValue;
import net.jr.client.ui.dsl.UiDocument.NegatedValue;
import net.jr.client.ui.dsl.UiDocument.NullValue;
import net.jr.client.ui.dsl.UiDocument.NumberValue;
import net.jr.client.ui.dsl.UiDocument.ObjectValue;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.StringValue;
import net.jr.client.ui.dsl.UiDocument.TupleValue;
import net.jr.client.ui.dsl.UiDocument.Value;
import net.jr.client.ui.dsl.UiReferenceResolver.Resolved;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class UiExpressionCompiler {
    private final UiCompilationContext context;
    private final UiReferenceResolver references;

    UiExpressionCompiler(UiCompilationContext context, UiReferenceResolver references) {
        this.context = context;
        this.references = references;
    }

    Vector2Definition optionalVector(UiDocument document, Value value, boolean allowThis)
        throws UiCompileException {
        return value == null ? null : this.vector(document, value, allowThis);
    }

    Vector2Definition vector(UiDocument document, Value value, boolean allowThis)
        throws UiCompileException {
        List<Value> values = tupleValues(value);
        if (values == null || values.size() != 2) {
            throw this.context.error(document, value.position(), "Expected exactly two values");
        }
        return new Vector2Definition(
            this.number(document, values.get(0), allowThis, 0),
            this.number(document, values.get(1), allowThis, 0)
        );
    }

    Expression<Float> number(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        this.references.checkDepth(document, value, depth);
        if (value instanceof NumberValue number) {
            try {
                float parsed = Float.parseFloat(number.literal());
                return constant(parsed);
            } catch (NumberFormatException exception) {
                throw this.context.error(document, value.position(), "Invalid number '" + number.literal() + "'");
            }
        }
        if (value instanceof ExposedDataValue data) {
            this.validateDataContext(document, data, allowThis);
            return context -> asNumber(context.resolve(data.path()), document.source(), data.position()).floatValue();
        }
        if (value instanceof ElementValue element) {
            Expression<Object> expression = this.element(document, element);
            return context -> asNumber(
                expression.resolve(context), document.source(), element.position()
            ).floatValue();
        }
        if (value instanceof ArithmeticValue arithmetic) {
            Expression<Float> left = this.number(document, arithmetic.left(), allowThis, depth + 1);
            Expression<Float> right = this.number(document, arithmetic.right(), allowThis, depth + 1);
            return context -> calculate(
                left.resolve(context),
                right.resolve(context),
                arithmetic,
                document.source()
            );
        }
        if (value instanceof ArithmeticNegationValue negated) {
            Expression<Float> expression = this.number(document, negated.value(), allowThis, depth + 1);
            return context -> -expression.resolve(context);
        }
        if (value instanceof CallValue call) {
            if (call.name().equals("mat")) {
                if (call.arguments().size() != 1) {
                    throw this.context.error(document, call.position(), "mat() requires one mathematical expression");
                }
                return this.number(document, call.arguments().getFirst(), allowThis, depth + 1);
            }
            if (call.name().equals("interpolate")) {
                if (call.arguments().size() != 3) {
                    throw this.context.error(
                        document,
                        call.position(),
                        "interpolate() requires value, time and unit"
                    );
                }
                Expression<Float> target = this.number(
                    document, call.arguments().get(0), allowThis, depth + 1
                );
                Expression<Float> duration = this.number(
                    document, call.arguments().get(1), allowThis, depth + 1
                );
                Expression<Draw.AnimUnit> unit = this.animationUnit(
                    document, call.arguments().get(2), allowThis, depth + 1
                );
                return context -> UiInterpolation.pingPong(
                    target.resolve(context),
                    duration.resolve(context),
                    unit.resolve(context),
                    System.nanoTime() / 1_000_000L,
                    document.source(),
                    call.position()
                );
            }
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(document, conditional.condition(), allowThis, depth + 1);
            Expression<Float> whenTrue = this.number(document, conditional.whenTrue(), allowThis, depth + 1);
            Expression<Float> whenFalse = this.number(document, conditional.whenFalse(), allowThis, depth + 1);
            return context -> condition.resolve(context) ? whenTrue.resolve(context) : whenFalse.resolve(context);
        }
        Resolved resolved = this.references.resolve(document, value, depth + 1);
        if (resolved != null) {
            return this.number(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.context.error(document, value.position(), "Expected a number");
    }

    Expression<Integer> integer(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        Expression<Float> number = this.number(document, value, allowThis, depth);
        return context -> Math.round(number.resolve(context));
    }

    Expression<Boolean> bool(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        this.references.checkDepth(document, value, depth);
        if (value instanceof BooleanValue bool) {
            return constant(bool.value());
        }
        if (value instanceof ExposedDataValue data) {
            this.validateDataContext(document, data, allowThis);
            return context -> asBoolean(context.resolve(data.path()), document.source(), data.position());
        }
        if (value instanceof ElementValue element) {
            Expression<Object> expression = this.element(document, element);
            return context -> asBoolean(
                expression.resolve(context), document.source(), element.position()
            );
        }
        if (value instanceof LogicalValue logical) {
            Expression<Boolean> left = this.bool(document, logical.left(), allowThis, depth + 1);
            Expression<Boolean> right = this.bool(document, logical.right(), allowThis, depth + 1);
            return switch (logical.operator()) {
                case AND -> context -> left.resolve(context) && right.resolve(context);
                case OR -> context -> left.resolve(context) || right.resolve(context);
            };
        }
        if (value instanceof NegatedValue negated) {
            Expression<Boolean> expression = this.bool(document, negated.value(), allowThis, depth + 1);
            return context -> !expression.resolve(context);
        }
        if (value instanceof ComparisonValue comparison) {
            Expression<Object> left = this.any(document, comparison.left(), allowThis, depth + 1);
            Expression<Object> right = this.any(document, comparison.right(), allowThis, depth + 1);
            return context -> compare(
                left.resolve(context),
                right.resolve(context),
                comparison,
                document.source()
            );
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(document, conditional.condition(), allowThis, depth + 1);
            Expression<Boolean> whenTrue = this.bool(document, conditional.whenTrue(), allowThis, depth + 1);
            Expression<Boolean> whenFalse = this.bool(document, conditional.whenFalse(), allowThis, depth + 1);
            return context -> condition.resolve(context) ? whenTrue.resolve(context) : whenFalse.resolve(context);
        }
        Resolved resolved = this.references.resolve(document, value, depth + 1);
        if (resolved != null) {
            return this.bool(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.context.error(document, value.position(), "Expected a boolean");
    }

    Expression<Object> any(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        this.references.checkDepth(document, value, depth);
        if (value instanceof StringValue string) {
            return constant(string.value());
        }
        if (value instanceof UiDocument.BareValue bare) {
            return constant(bare.value());
        }
        if (value instanceof NumberValue number) {
            try {
                return constant(Double.parseDouble(number.literal()));
            } catch (NumberFormatException exception) {
                throw this.context.error(document, number.position(), "Invalid number '" + number.literal() + "'");
            }
        }
        if (value instanceof BooleanValue bool) {
            return constant(bool.value());
        }
        if (value instanceof NullValue) {
            return constant(null);
        }
        if (value instanceof UiDocument.ColorValue color) {
            return constant(parseColor(document, color));
        }
        if (value instanceof ListValue list) {
            List<Expression<Object>> values = new ArrayList<>();
            for (Value item : list.values()) {
                values.add(this.any(document, item, allowThis, depth + 1));
            }
            return context -> values.stream().map(item -> item.resolve(context)).toList();
        }
        if (value instanceof TupleValue tuple) {
            List<Expression<Object>> values = new ArrayList<>();
            for (Value item : tuple.values()) {
                values.add(this.any(document, item, allowThis, depth + 1));
            }
            return context -> values.stream().map(item -> item.resolve(context)).toList();
        }
        if (value instanceof ObjectValue object) {
            Map<String, Expression<Object>> values = new LinkedHashMap<>();
            for (Map.Entry<String, Value> entry : object.values().entrySet()) {
                values.put(entry.getKey(), this.any(document, entry.getValue(), allowThis, depth + 1));
            }
            return context -> {
                Map<String, Object> resolvedValues = new LinkedHashMap<>();
                values.forEach((name, expression) -> resolvedValues.put(name, expression.resolve(context)));
                return java.util.Collections.unmodifiableMap(resolvedValues);
            };
        }
        if (value instanceof ExposedDataValue data) {
            this.validateDataContext(document, data, allowThis);
            return context -> context.resolve(data.path());
        }
        if (value instanceof ElementValue element) {
            return this.element(document, element);
        }
        if (value instanceof LogicalValue || value instanceof NegatedValue || value instanceof ComparisonValue) {
            Expression<Boolean> expression = this.bool(document, value, allowThis, depth + 1);
            return context -> expression.resolve(context);
        }
        if (value instanceof ArithmeticValue || value instanceof ArithmeticNegationValue
            || value instanceof CallValue call
                && (call.name().equals("mat") || call.name().equals("interpolate"))) {
            Expression<Float> expression = this.number(document, value, allowThis, depth + 1);
            return context -> expression.resolve(context);
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(document, conditional.condition(), allowThis, depth + 1);
            Expression<Object> whenTrue = this.any(document, conditional.whenTrue(), allowThis, depth + 1);
            Expression<Object> whenFalse = this.any(document, conditional.whenFalse(), allowThis, depth + 1);
            return context -> condition.resolve(context) ? whenTrue.resolve(context) : whenFalse.resolve(context);
        }
        Resolved resolved = this.references.resolve(document, value, depth + 1);
        if (resolved != null) {
            if (resolved.asset() != null) {
                return constant(resolved.asset());
            }
            return this.any(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.context.error(document, value.position(), "Expected a comparable value");
    }

    Expression<String> string(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        this.references.checkDepth(document, value, depth);
        if (value instanceof StringValue string) {
            return constant(string.value());
        }
        if (value instanceof UiDocument.BareValue bare) {
            return constant(bare.value());
        }
        if (value instanceof ExposedDataValue data) {
            this.validateDataContext(document, data, allowThis);
            return context -> Objects.toString(context.resolve(data.path()), "");
        }
        if (value instanceof ElementValue element) {
            Expression<Object> expression = this.element(document, element);
            return context -> Objects.toString(expression.resolve(context), "");
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(document, conditional.condition(), allowThis, depth + 1);
            Expression<String> whenTrue = this.string(document, conditional.whenTrue(), allowThis, depth + 1);
            Expression<String> whenFalse = this.string(document, conditional.whenFalse(), allowThis, depth + 1);
            return context -> condition.resolve(context) ? whenTrue.resolve(context) : whenFalse.resolve(context);
        }
        Resolved resolved = this.references.resolve(document, value, depth + 1);
        if (resolved != null) {
            return this.string(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.context.error(document, value.position(), "Expected text");
    }

    Expression<String> textString(
        UiDocument document,
        Value value,
        boolean allowThis,
        int depth
    ) throws UiCompileException {
        this.references.checkDepth(document, value, depth);
        if (value instanceof GlyphValue glyph) {
            return constant(Glyphs.marker(
                glyph.namespace(), glyph.provider(), glyph.id()
            ));
        }
        if (value instanceof ExposedDataValue data && data.path().size() == 1) {
            this.validateDataContext(document, data, allowThis);
            String id = data.path().getFirst();
            return context -> context.contains(data.path())
                ? Objects.toString(context.resolve(data.path()), "")
                : Glyphs.marker(null, null, id);
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(
                document, conditional.condition(), allowThis, depth + 1
            );
            Expression<String> whenTrue = this.textString(
                document, conditional.whenTrue(), allowThis, depth + 1
            );
            Expression<String> whenFalse = this.textString(
                document, conditional.whenFalse(), allowThis, depth + 1
            );
            return context -> condition.resolve(context)
                ? whenTrue.resolve(context)
                : whenFalse.resolve(context);
        }
        return this.string(document, value, allowThis, depth);
    }

    Expression<Asset> asset(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        this.references.checkDepth(document, value, depth);
        if (value instanceof ExposedDataValue data) {
            this.validateDataContext(document, data, allowThis);
            return context -> {
                Object resolved = context.resolve(data.path());
                if (resolved instanceof Asset asset) {
                    return asset;
                }
                throw runtimeType(document.source(), data.position(), "Asset", resolved);
            };
        }
        if (value instanceof ElementValue element) {
            Expression<Object> expression = this.element(document, element);
            return context -> {
                Object resolved = expression.resolve(context);
                if (resolved instanceof Asset asset) {
                    return asset;
                }
                throw runtimeType(document.source(), element.position(), "Asset", resolved);
            };
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(document, conditional.condition(), allowThis, depth + 1);
            Expression<Asset> whenTrue = this.asset(document, conditional.whenTrue(), allowThis, depth + 1);
            Expression<Asset> whenFalse = this.asset(document, conditional.whenFalse(), allowThis, depth + 1);
            return context -> condition.resolve(context) ? whenTrue.resolve(context) : whenFalse.resolve(context);
        }
        Resolved resolved = this.references.resolve(document, value, depth + 1);
        if (resolved != null) {
            if (resolved.asset() != null) {
                return constant(resolved.asset());
            }
            return this.asset(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.context.error(document, value.position(), "Expected an imported Asset reference");
    }

    Expression<Integer> color(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        this.references.checkDepth(document, value, depth);
        if (value instanceof UiDocument.ColorValue color) {
            return constant(parseColor(document, color));
        }
        if (value instanceof ExposedDataValue data) {
            this.validateDataContext(document, data, allowThis);
            return context -> asColor(context.resolve(data.path()), document.source(), data.position());
        }
        if (value instanceof ElementValue element) {
            Expression<Object> expression = this.element(document, element);
            return context -> asColor(
                expression.resolve(context), document.source(), element.position()
            );
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(document, conditional.condition(), allowThis, depth + 1);
            Expression<Integer> whenTrue = this.color(document, conditional.whenTrue(), allowThis, depth + 1);
            Expression<Integer> whenFalse = this.color(document, conditional.whenFalse(), allowThis, depth + 1);
            return context -> condition.resolve(context) ? whenTrue.resolve(context) : whenFalse.resolve(context);
        }
        Resolved resolved = this.references.resolve(document, value, depth + 1);
        if (resolved != null) {
            return this.color(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.context.error(document, value.position(), "Expected a color");
    }

    Expression<Draw.TextAlign> textAlign(
        UiDocument document, Value value, boolean allowThis, int depth
    ) throws UiCompileException {
        Expression<String> text = this.string(document, value, allowThis, depth);
        return context -> switch (text.resolve(context).toLowerCase(Locale.ROOT)) {
            case "left" -> Draw.TextAlign.LEFT;
            case "center" -> Draw.TextAlign.CENTER;
            case "right" -> Draw.TextAlign.RIGHT;
            default -> throw new IllegalArgumentException("Unsupported text alignment");
        };
    }

    Expression<VisualAnchor> visualAnchor(
        UiDocument document, Value value, boolean allowThis, int depth
    ) throws UiCompileException {
        if (value instanceof StringValue string) {
            return constant(this.parseVisualAnchor(document, value, string.value()));
        }
        if (value instanceof UiDocument.BareValue bare) {
            return constant(this.parseVisualAnchor(document, value, bare.value()));
        }
        Expression<String> text = this.string(document, value, allowThis, depth);
        return context -> VisualAnchor.parse(text.resolve(context));
    }

    VisualAnchor parseVisualAnchor(UiDocument document, Value value, String text)
        throws UiCompileException {
        try {
            return VisualAnchor.parse(text);
        } catch (IllegalArgumentException exception) {
            throw this.context.error(document, value.position(), exception.getMessage());
        }
    }

    Expression<GridDirection> gridDirection(
        UiDocument document, Value value, boolean allowThis, int depth
    ) throws UiCompileException {
        this.references.checkDepth(document, value, depth);
        if (value instanceof CallValue call) {
            if (call.arguments().size() != 1) {
                throw this.context.error(document, call.position(),
                    call.name() + "() requires exactly one alignment");
            }
            Expression<String> alignment = this.string(
                document, call.arguments().getFirst(), allowThis, depth + 1
            );
            String direction = call.name().trim().toLowerCase(Locale.ROOT);
            if (!direction.equals("horizontal") && !direction.equals("vertical")) {
                throw this.context.error(document, call.position(),
                    "Grid direction must be horizontal(top|center|bottom) or "
                        + "vertical(left|center|right)");
            }
            return context -> parseGridDirection(
                direction,
                alignment.resolve(context),
                document,
                call.position()
            );
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(
                document, conditional.condition(), allowThis, depth + 1
            );
            Expression<GridDirection> whenTrue = this.gridDirection(
                document, conditional.whenTrue(), allowThis, depth + 1
            );
            Expression<GridDirection> whenFalse = this.gridDirection(
                document, conditional.whenFalse(), allowThis, depth + 1
            );
            return context -> condition.resolve(context)
                ? whenTrue.resolve(context)
                : whenFalse.resolve(context);
        }
        Resolved resolved = this.references.resolve(document, value, depth + 1);
        if (resolved != null) {
            return this.gridDirection(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.context.error(document, value.position(),
            "Expected horizontal(top|center|bottom) or vertical(left|center|right)");
    }

    static GridDirection parseGridDirection(
        String direction,
        String alignment,
        UiDocument document,
        UiDocument.Position position
    ) {
        String normalized = alignment.trim().toUpperCase(Locale.ROOT);
        try {
            return direction.equals("horizontal")
                ? GridDirection.horizontal(GridDirection.VerticalAlignment.valueOf(normalized))
                : GridDirection.vertical(GridDirection.HorizontalAlignment.valueOf(normalized));
        } catch (IllegalArgumentException exception) {
            String accepted = direction.equals("horizontal")
                ? "top, center or bottom"
                : "left, center or right";
            throw new IllegalArgumentException(
                document.source() + ":" + position.line() + ":" + position.column()
                    + ": invalid alignment '" + alignment + "' for " + direction
                    + "(); expected " + accepted
            );
        }
    }

    Expression<Draw.CenterMode> centerMode(
        UiDocument document, Value value, boolean allowThis, int depth
    ) throws UiCompileException {
        Expression<String> text = this.string(document, value, allowThis, depth);
        return context -> switch (text.resolve(context).toLowerCase(Locale.ROOT)) {
            case "stretch" -> Draw.CenterMode.STRETCH;
            case "repeat" -> Draw.CenterMode.REPEAT;
            default -> throw new IllegalArgumentException("Unsupported Nine center mode");
        };
    }

    Expression<Draw.AnimUnit> animationUnit(
        UiDocument document, Value value, boolean allowThis, int depth
    ) throws UiCompileException {
        Expression<String> text = this.string(document, value, allowThis, depth);
        return context -> switch (text.resolve(context).toLowerCase(Locale.ROOT)) {
            case "ms", "millisecond", "milliseconds" -> Draw.AnimUnit.MILLISECONDS;
            case "tick", "ticks" -> Draw.AnimUnit.TICKS;
            case "second", "seconds" -> Draw.AnimUnit.SECONDS;
            case "minute", "minutes" -> Draw.AnimUnit.MINUTES;
            default -> throw new IllegalArgumentException("Unsupported animation unit");
        };
    }

    Expression<Draw.AnimationMode> animationMode(
        UiDocument document, Value value, boolean allowThis, int depth
    ) throws UiCompileException {
        Expression<String> text = this.string(document, value, allowThis, depth);
        return context -> switch (text.resolve(context).toLowerCase(Locale.ROOT).replace('-', '_')) {
            case "loop" -> Draw.AnimationMode.LOOP;
            case "ping_pong" -> Draw.AnimationMode.PING_PONG;
            default -> throw new IllegalArgumentException("Unsupported animation mode");
        };
    }

    static List<Value> tupleValues(Value value) {
        if (value instanceof TupleValue tuple) {
            return tuple.values();
        }
        if (value instanceof ListValue list) {
            return list.values();
        }
        return null;
    }

    static Number asNumber(Object value, Asset source, UiDocument.Position position) {
        if (value instanceof Number number) {
            return number;
        }
        throw runtimeType(source, position, "Number", value);
    }

    static boolean asBoolean(Object value, Asset source, UiDocument.Position position) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw runtimeType(source, position, "Boolean", value);
    }

    static Object requireImageMetaValue(
        Object value,
        Asset source,
        UiDocument.Position position
    ) {
        if (value instanceof Boolean || value instanceof Asset) {
            return value;
        }
        throw runtimeType(source, position, "Boolean or Asset", value);
    }

    static int asColor(Object value, Asset source, UiDocument.Position position) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return parseColorLiteral(text);
            } catch (IllegalArgumentException ignored) {
            }
        }
        throw runtimeType(source, position, "Color", value);
    }

    static int parseColor(UiDocument document, UiDocument.ColorValue color)
        throws UiCompileException {
        try {
            return parseColorLiteral(color.literal());
        } catch (IllegalArgumentException exception) {
            throw new UiCompileException(document.source(), color.position(), exception.getMessage());
        }
    }

    static int parseColorLiteral(String literal) {
        String hex = literal.startsWith("#") ? literal.substring(1) : literal;
        return switch (hex.length()) {
            case 3 -> 0xFF000000
                | Integer.parseInt(hex.substring(0, 1).repeat(2), 16) << 16
                | Integer.parseInt(hex.substring(1, 2).repeat(2), 16) << 8
                | Integer.parseInt(hex.substring(2, 3).repeat(2), 16);
            case 4 -> Integer.parseInt(hex.substring(0, 1).repeat(2), 16) << 24
                | Integer.parseInt(hex.substring(1, 2).repeat(2), 16) << 16
                | Integer.parseInt(hex.substring(2, 3).repeat(2), 16) << 8
                | Integer.parseInt(hex.substring(3, 4).repeat(2), 16);
            case 6 -> 0xFF000000 | Integer.parseInt(hex, 16);
            case 8 -> (int) Long.parseLong(hex, 16);
            default -> throw new IllegalArgumentException("Color must use #RGB, #ARGB, #RRGGBB or #AARRGGBB");
        };
    }

    static boolean compare(
        Object left,
        Object right,
        ComparisonValue comparison,
        Asset source
    ) {
        return switch (comparison.operator()) {
            case EQUAL -> valuesEqual(left, right);
            case NOT_EQUAL -> !valuesEqual(left, right);
            case LESS -> compareOrdered(left, right, comparison, source) < 0;
            case LESS_OR_EQUAL -> compareOrdered(left, right, comparison, source) <= 0;
            case GREATER -> compareOrdered(left, right, comparison, source) > 0;
            case GREATER_OR_EQUAL -> compareOrdered(left, right, comparison, source) >= 0;
        };
    }

    static float calculate(
        float left,
        float right,
        ArithmeticValue arithmetic,
        Asset source
    ) {
        return switch (arithmetic.operator()) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> {
                if (right == 0.0F) {
                    throw arithmeticError(source, arithmetic, "division by zero");
                }
                yield left / right;
            }
            case MODULO -> {
                if (right == 0.0F) {
                    throw arithmeticError(source, arithmetic, "modulo by zero");
                }
                yield left % right;
            }
        };
    }

    static IllegalArgumentException arithmeticError(
        Asset source,
        ArithmeticValue arithmetic,
        String message
    ) {
        return new IllegalArgumentException(
            source + ":" + arithmetic.position().line() + ":" + arithmetic.position().column()
                + ": " + message + " in mat()"
        );
    }

    static boolean valuesEqual(Object left, Object right) {
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            return Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue()) == 0;
        }
        return Objects.equals(left, right);
    }

    static int compareOrdered(
        Object left,
        Object right,
        ComparisonValue comparison,
        Asset source
    ) {
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            return Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue());
        }
        if (left instanceof String leftText && right instanceof String rightText) {
            return leftText.compareTo(rightText);
        }
        String leftType = left == null ? "null" : left.getClass().getSimpleName();
        String rightType = right == null ? "null" : right.getClass().getSimpleName();
        throw new IllegalArgumentException(
            source + ":" + comparison.position().line() + ":" + comparison.position().column()
                + ": ordered comparison requires two numbers or two strings, found "
                + leftType + " and " + rightType
        );
    }

    static IllegalArgumentException runtimeType(
        Asset source, UiDocument.Position position, String expected, Object value
    ) {
        String actual = value == null ? "null" : value.getClass().getSimpleName();
        return new IllegalArgumentException(
            source + ":" + position.line() + ":" + position.column()
                + ": expected " + expected + ", found " + actual
        );
    }

    static <T> Expression<T> constant(T value) {
        return ignored -> value;
    }

    private Expression<Object> element(UiDocument document, ElementValue element)
        throws UiCompileException {
        this.context.profile().require(
            UiLanguageCapability.ELEMENT_REFERENCES, document, element.position()
        );
        this.context.state().reference(document, element);
        return context -> context.resolveElement(element.id(), element.property());
    }

    private void validateDataContext(UiDocument document, ExposedDataValue data, boolean allowThis)
        throws UiCompileException {
        this.context.profile().require(
            UiLanguageCapability.EXPOSED_DATA, document, data.position()
        );
        if (!allowThis && data.path().getFirst().equals("this")) {
            throw this.context.error(document, data.position(),
                "${this.*} is only valid inside a widget's renderables");
        }
    }

}

