package net.jr.client.ui.dsl;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiFileType;
import net.jr.api.client.ui.dsl.UiCompiledDocument;
import net.jr.api.client.ui.dsl.UiCompiledDocument.AnimationDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.ButtonDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.ConditionalRenderableDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.api.client.ui.dsl.UiCompiledDocument.ImageDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.NineDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.RenderableDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.TextDefinition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.Vector2Definition;
import net.jr.api.client.ui.dsl.UiCompiledDocument.VideoDefinition;
import net.jr.api.client.ui.dsl.UiDocument;
import net.jr.api.client.ui.dsl.UiDocument.ArithmeticNegationValue;
import net.jr.api.client.ui.dsl.UiDocument.ArithmeticValue;
import net.jr.api.client.ui.dsl.UiDocument.BooleanValue;
import net.jr.api.client.ui.dsl.UiDocument.BlueprintDirective;
import net.jr.api.client.ui.dsl.UiDocument.BlueprintParameterValue;
import net.jr.api.client.ui.dsl.UiDocument.BlueprintUse;
import net.jr.api.client.ui.dsl.UiDocument.CallValue;
import net.jr.api.client.ui.dsl.UiDocument.ConditionalValue;
import net.jr.api.client.ui.dsl.UiDocument.ConditionalStatement;
import net.jr.api.client.ui.dsl.UiDocument.ComparisonValue;
import net.jr.api.client.ui.dsl.UiDocument.ExposedDataValue;
import net.jr.api.client.ui.dsl.UiDocument.ImportDirective;
import net.jr.api.client.ui.dsl.UiDocument.ListValue;
import net.jr.api.client.ui.dsl.UiDocument.LogicalValue;
import net.jr.api.client.ui.dsl.UiDocument.NegatedValue;
import net.jr.api.client.ui.dsl.UiDocument.NullValue;
import net.jr.api.client.ui.dsl.UiDocument.Node;
import net.jr.api.client.ui.dsl.UiDocument.NumberValue;
import net.jr.api.client.ui.dsl.UiDocument.ObjectValue;
import net.jr.api.client.ui.dsl.UiDocument.Property;
import net.jr.api.client.ui.dsl.UiDocument.ReferenceValue;
import net.jr.api.client.ui.dsl.UiDocument.Statement;
import net.jr.api.client.ui.dsl.UiDocument.StringValue;
import net.jr.api.client.ui.dsl.UiDocument.TupleValue;
import net.jr.api.client.ui.dsl.UiDocument.Value;
import net.jr.api.client.ui.dsl.UiRenderLayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class UiCompiler {
    private static final int MAX_REFERENCE_DEPTH = 64;

    private final Map<Asset, UiDocument> documents;

    UiCompiler(Map<Asset, UiDocument> documents) {
        this.documents = documents;
    }

    UiCompiledDocument compile(UiDocument document) throws UiCompileException {
        String expectedRoot = switch (document.type()) {
            case SCREEN -> "screen";
            case CONTAINER -> "container";
            case OVERLAY -> "overlay";
            case STYLE -> throw this.error(document, new UiDocument.Position(1, 1),
                "Style files are imported definitions and cannot be used as a layout");
        };

        Node root = null;
        for (Statement statement : document.statements()) {
            if (statement instanceof Node node) {
                if (!node.name().equals(expectedRoot)) {
                    throw this.error(document, node.position(),
                        "Expected root block '" + expectedRoot + "', found '" + node.name() + "'");
                }
                if (root != null) {
                    throw this.error(document, node.position(), "A UI document can only have one root block");
                }
                root = node;
            } else if (statement instanceof BlueprintUse use) {
                throw this.error(document, use.position(),
                    "A blueprint instance must be placed inside a renderables block");
            }
        }
        if (root == null) {
            throw this.error(document, new UiDocument.Position(1, 1),
                "Missing root block '" + expectedRoot + "'");
        }

        Node elements = null;
        Node renderables = null;
        for (Statement statement : root.body()) {
            if (!(statement instanceof Node node)
                || (!node.name().equals("elements") && !node.name().equals("renderables"))) {
                throw this.error(document, statement.position(),
                    "Only 'elements' and 'renderables' blocks are supported inside '"
                        + expectedRoot + "'");
            }
            if (node.name().equals("elements")) {
                if (elements != null) {
                    throw this.error(document, node.position(), "Duplicate 'elements' block");
                }
                elements = node;
            } else {
                if (renderables != null) {
                    throw this.error(document, node.position(), "Duplicate 'renderables' block");
                }
                renderables = node;
            }
        }

        List<ButtonDefinition> buttons = new ArrayList<>();
        if (elements != null) {
            for (Statement statement : elements.body()) {
                if (!(statement instanceof Node node)) {
                    throw this.error(document, statement.position(), "Expected an element declaration");
                }
                switch (node.name()) {
                    case "button" -> buttons.add(this.compileButton(document, node));
                    default -> throw this.error(document, node.position(),
                        "Unsupported element type '" + node.name() + "'");
                }
            }
        }

        Map<UiRenderLayer, List<RenderableDefinition>> layers = this.compileRenderables(document, renderables);

        ensureUniqueIds(document, buttons.stream().map(ButtonDefinition::id).toList(), "button");
        List<String> renderableIds = layers.values().stream()
            .flatMap(List::stream)
            .map(RenderableDefinition::id)
            .toList();
        ensureUniqueIds(document, renderableIds, "layer renderable");
        return new UiCompiledDocument(document.source(), document.type(), buttons, layers);
    }

    private Map<UiRenderLayer, List<RenderableDefinition>> compileRenderables(
        UiDocument document,
        Node renderables
    ) throws UiCompileException {
        Map<UiRenderLayer, List<RenderableDefinition>> layers = new LinkedHashMap<>();
        if (renderables == null) {
            return layers;
        }
        for (Statement statement : renderables.body()) {
            if (!(statement instanceof Node layerNode)) {
                throw this.error(document, statement.position(), "Expected a render layer block");
            }
            UiRenderLayer layer = switch (layerNode.name()) {
                case "background" -> UiRenderLayer.BACKGROUND;
                case "content" -> UiRenderLayer.CONTENT;
                case "foreground" -> UiRenderLayer.FOREGROUND;
                default -> throw this.error(document, layerNode.position(),
                    "Unknown render layer '" + layerNode.name() + "'");
            };
            if (layers.containsKey(layer)) {
                throw this.error(document, layerNode.position(),
                    "Duplicate render layer '" + layerNode.name() + "'");
            }

            List<RenderableDefinition> elements = new ArrayList<>();
            elements.addAll(this.compileRenderableStatements(
                document, layerNode.body(), false, null, Map.of(), new ArrayDeque<>()
            ));
            layers.put(layer, List.copyOf(elements));
        }
        return layers;
    }

    private ButtonDefinition compileButton(UiDocument document, Node node) throws UiCompileException {
        String id = this.nodeId(document, node);
        Body body = this.body(
            document,
            node,
            List.of("x", "y", "w", "h", "scissor"),
            List.of("renderables")
        );
        Expression<Float> x = this.number(document, body.require("x"), false, 0);
        Expression<Float> y = this.number(document, body.require("y"), false, 0);
        Expression<Float> width = this.number(document, body.require("w"), false, 0);
        Expression<Float> height = this.number(document, body.require("h"), false, 0);
        Expression<Boolean> scissor = body.property("scissor") == null
            ? constant(false)
            : this.bool(document, body.property("scissor"), false, 0);

        List<RenderableDefinition> renderables = new ArrayList<>();
        Node renderablesNode = body.node("renderables");
        if (renderablesNode != null) {
            renderables.addAll(this.compileRenderableStatements(
                document, renderablesNode.body(), true, null, Map.of(), new ArrayDeque<>()
            ));
        }
        ensureUniqueIds(document, renderables.stream().map(RenderableDefinition::id).toList(),
            "renderable element in button '" + id + "'");
        return new ButtonDefinition(id, x, y, width, height, scissor, renderables);
    }

    private List<RenderableDefinition> compileRenderableStatements(
        UiDocument document,
        List<Statement> statements,
        boolean allowThis,
        String idPrefix,
        Map<String, Value> parameters,
        Deque<BlueprintKey> expansionStack
    ) throws UiCompileException {
        List<RenderableDefinition> renderables = new ArrayList<>();
        for (Statement statement : statements) {
            if (statement instanceof Node element) {
                Node expanded = this.substituteNode(document, element, parameters, idPrefix);
                renderables.add(this.compileRenderable(document, expanded, allowThis));
            } else if (statement instanceof BlueprintUse use) {
                renderables.addAll(this.expandBlueprint(
                    document, use, allowThis, idPrefix, parameters, expansionStack
                ));
            } else if (statement instanceof ConditionalStatement conditional) {
                Value conditionValue = this.substituteValue(document, conditional.condition(), parameters);
                Expression<Boolean> condition = this.bool(document, conditionValue, allowThis, 0);
                List<RenderableDefinition> conditionalRenderables = this.compileRenderableStatements(
                    document,
                    conditional.body(),
                    allowThis,
                    idPrefix,
                    parameters,
                    expansionStack
                );
                for (RenderableDefinition renderable : conditionalRenderables) {
                    renderables.add(new ConditionalRenderableDefinition(
                        renderable.id(),
                        condition,
                        renderable
                    ));
                }
            } else {
                throw this.error(document, statement.position(),
                    "Expected a renderable element, conditional block or blueprint instance");
            }
        }
        return renderables;
    }

    private List<RenderableDefinition> expandBlueprint(
        UiDocument caller,
        BlueprintUse use,
        boolean allowThis,
        String parentPrefix,
        Map<String, Value> callerParameters,
        Deque<BlueprintKey> expansionStack
    ) throws UiCompileException {
        BlueprintTarget target = this.resolveBlueprint(caller, use);
        BlueprintDirective blueprint = target.blueprint();
        BlueprintKey key = new BlueprintKey(target.document().source(), blueprint.name());
        if (expansionStack.contains(key)) {
            throw this.error(caller, use.position(),
                "Circular blueprint composition detected for '" + String.join(".", use.path()) + "'");
        }
        if (expansionStack.size() >= MAX_REFERENCE_DEPTH) {
            throw this.error(caller, use.position(), "Blueprint composition is too deep");
        }

        if (use.arguments().size() != blueprint.parameters().size()) {
            throw this.error(caller, use.position(),
                "Blueprint '" + blueprint.name() + "' expects " + blueprint.parameters().size()
                    + " arguments, but received " + use.arguments().size());
        }
        Map<String, Value> bindings = new LinkedHashMap<>();
        for (int index = 0; index < blueprint.parameters().size(); index++) {
            bindings.put(
                blueprint.parameters().get(index),
                this.substituteValue(caller, use.arguments().get(index), callerParameters)
            );
        }

        Node renderables = null;
        for (Statement statement : blueprint.body()) {
            if (!(statement instanceof Node node) || !node.name().equals("renderables")) {
                throw this.error(target.document(), statement.position(),
                    "A visual blueprint may only contain one 'renderables' block");
            }
            if (renderables != null) {
                throw this.error(target.document(), node.position(),
                    "A blueprint can only contain one 'renderables' block");
            }
            renderables = node;
        }
        if (renderables == null) {
            throw this.error(target.document(), blueprint.position(),
                "Blueprint '" + blueprint.name() + "' is missing its 'renderables' block");
        }

        String prefix = parentPrefix == null
            ? use.instanceId()
            : parentPrefix + "." + use.instanceId();
        expansionStack.addLast(key);
        try {
            return this.compileRenderableStatements(
                target.document(), renderables.body(), allowThis, prefix, bindings, expansionStack
            );
        } finally {
            expansionStack.removeLast();
        }
    }

    private BlueprintTarget resolveBlueprint(UiDocument caller, BlueprintUse use)
        throws UiCompileException {
        List<String> path = use.path();
        if (path.size() == 1) {
            BlueprintDirective blueprint = caller.blueprint(path.getFirst()).orElseThrow(() ->
                this.error(caller, use.position(), "Unknown blueprint '@" + path.getFirst() + "'")
            );
            return new BlueprintTarget(caller, blueprint);
        }
        if (path.size() != 2) {
            throw this.error(caller, use.position(),
                "A blueprint reference must be '@name' or '@alias.name'");
        }

        ImportDirective imported = caller.imports().stream()
            .filter(candidate -> candidate.alias().equals(path.getFirst()))
            .findFirst()
            .orElseThrow(() -> this.error(caller, use.position(),
                "Unknown import alias '@" + path.getFirst() + "'"));
        UiFileType importedType = UiFileType.fromImportPath(imported.target().path()).orElse(null);
        if (importedType == null) {
            throw this.error(caller, use.position(),
                "Raw Assets do not contain blueprints");
        }
        Asset importedAsset = importAsset(caller.source(), imported);
        UiDocument importedDocument = this.documents.get(importedAsset);
        if (importedDocument == null) {
            throw this.error(caller, use.position(),
                "Imported UI document is not loaded: " + importedAsset);
        }
        BlueprintDirective blueprint = importedDocument.blueprint(path.get(1)).orElseThrow(() ->
            this.error(caller, use.position(),
                "Imported blueprint '" + path.get(1) + "' does not exist in " + importedAsset)
        );
        return new BlueprintTarget(importedDocument, blueprint);
    }

    private Node substituteNode(
        UiDocument document,
        Node node,
        Map<String, Value> parameters,
        String idPrefix
    ) throws UiCompileException {
        List<Value> arguments = new ArrayList<>();
        for (Value argument : node.arguments()) {
            arguments.add(this.substituteValue(document, argument, parameters));
        }
        if (idPrefix != null && (node.name().equals("image") || node.name().equals("video")
            || node.name().equals("text"))) {
            if (arguments.size() != 1 || !(arguments.getFirst() instanceof StringValue id)
                || id.value().isBlank()) {
                throw this.error(document, node.position(),
                    "A renderable inside a blueprint requires one non-empty quoted id");
            }
            arguments.set(0, new StringValue(idPrefix + "." + id.value(), id.position()));
        }

        List<Statement> body = new ArrayList<>();
        for (Statement statement : node.body()) {
            if (statement instanceof Property property) {
                body.add(new Property(
                    property.name(),
                    this.substituteValue(document, property.value(), parameters),
                    property.position()
                ));
            } else if (statement instanceof Node child) {
                body.add(this.substituteNode(document, child, parameters, null));
            } else {
                throw this.error(document, statement.position(),
                    "Blueprint parameters cannot be substituted in this statement");
            }
        }
        return new Node(node.name(), arguments, body, node.position());
    }

    private Value substituteValue(UiDocument document, Value value, Map<String, Value> parameters)
        throws UiCompileException {
        if (value instanceof BlueprintParameterValue parameter) {
            Value bound = parameters.get(parameter.path().getFirst());
            if (bound == null) {
                throw this.error(document, parameter.position(),
                    "Unknown blueprint parameter '&" + parameter.path().getFirst() + "'");
            }
            return this.descend(document, bound, parameter.path().subList(1, parameter.path().size()));
        }
        if (value instanceof ListValue list) {
            return new ListValue(this.substituteValues(document, list.values(), parameters), list.position());
        }
        if (value instanceof TupleValue tuple) {
            return new TupleValue(this.substituteValues(document, tuple.values(), parameters), tuple.position());
        }
        if (value instanceof ObjectValue object) {
            Map<String, Value> values = new LinkedHashMap<>();
            for (Map.Entry<String, Value> entry : object.values().entrySet()) {
                values.put(entry.getKey(), this.substituteValue(document, entry.getValue(), parameters));
            }
            return new ObjectValue(values, object.position());
        }
        if (value instanceof CallValue call) {
            return new CallValue(
                call.name(), this.substituteValues(document, call.arguments(), parameters), call.position()
            );
        }
        if (value instanceof ConditionalValue conditional) {
            return new ConditionalValue(
                this.substituteValue(document, conditional.condition(), parameters),
                this.substituteValue(document, conditional.whenTrue(), parameters),
                this.substituteValue(document, conditional.whenFalse(), parameters),
                conditional.position()
            );
        }
        if (value instanceof LogicalValue logical) {
            return new LogicalValue(
                this.substituteValue(document, logical.left(), parameters),
                logical.operator(),
                this.substituteValue(document, logical.right(), parameters),
                logical.position()
            );
        }
        if (value instanceof NegatedValue negated) {
            return new NegatedValue(
                this.substituteValue(document, negated.value(), parameters),
                negated.position()
            );
        }
        if (value instanceof ComparisonValue comparison) {
            return new ComparisonValue(
                this.substituteValue(document, comparison.left(), parameters),
                comparison.operator(),
                this.substituteValue(document, comparison.right(), parameters),
                comparison.position()
            );
        }
        if (value instanceof ArithmeticValue arithmetic) {
            return new ArithmeticValue(
                this.substituteValue(document, arithmetic.left(), parameters),
                arithmetic.operator(),
                this.substituteValue(document, arithmetic.right(), parameters),
                arithmetic.position()
            );
        }
        if (value instanceof ArithmeticNegationValue negated) {
            return new ArithmeticNegationValue(
                this.substituteValue(document, negated.value(), parameters),
                negated.position()
            );
        }
        return value;
    }

    private List<Value> substituteValues(
        UiDocument document,
        List<Value> values,
        Map<String, Value> parameters
    ) throws UiCompileException {
        List<Value> result = new ArrayList<>(values.size());
        for (Value value : values) {
            result.add(this.substituteValue(document, value, parameters));
        }
        return List.copyOf(result);
    }

    private RenderableDefinition compileRenderable(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException {
        return switch (node.name()) {
            case "image" -> this.compileImage(document, node, allowThis);
            case "video" -> this.compileVideo(document, node, allowThis);
            case "text" -> this.compileText(document, node, allowThis);
            default -> throw this.error(document, node.position(),
                "Unsupported renderable element type '" + node.name() + "'");
        };
    }

    private ImageDefinition compileImage(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException {
        String id = this.nodeId(document, node);
        Body body = this.body(
            document,
            node,
            List.of("texture", "x", "y", "w", "h", "uv", "uv_size", "atlas_size", "meta", "config"),
            List.of("nine", "animation")
        );
        Expression<Asset> texture = this.asset(document, body.require("texture"), allowThis, 0);
        Expression<Float> x = this.number(document, body.require("x"), allowThis, 0);
        Expression<Float> y = this.number(document, body.require("y"), allowThis, 0);
        Expression<Float> width = this.number(document, body.require("w"), allowThis, 0);
        Expression<Float> height = this.number(document, body.require("h"), allowThis, 0);

        Vector2Definition uv = this.optionalVector(document, body.property("uv"), allowThis);
        Vector2Definition uvSize = this.optionalVector(document, body.property("uv_size"), allowThis);
        Vector2Definition atlasSize = this.optionalVector(document, body.property("atlas_size"), allowThis);
        Expression<Boolean> useMeta = body.property("meta") == null
            ? constant(body.property("config") != null)
            : this.bool(document, body.property("meta"), allowThis, 0);
        Expression<String> config = body.property("config") == null
            ? null
            : this.string(document, body.property("config"), allowThis, 0);
        NineDefinition nine = this.compileNine(document, body.node("nine"), allowThis);
        AnimationDefinition animation = this.compileAnimation(document, body.node("animation"), allowThis);

        if ((nine != null || animation != null) && body.property("meta") == null
            && body.property("config") == null && (uv == null || uvSize == null || atlasSize == null)) {
            throw this.error(document, node.position(),
                "An image with Nine or Animation and no Meta requires uv, uv_size and atlas_size");
        }

        return new ImageDefinition(
            id, texture, x, y, width, height, uv, uvSize, atlasSize,
            useMeta, config, nine, animation
        );
    }

    private VideoDefinition compileVideo(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException {
        String id = this.nodeId(document, node);
        Body body = this.body(
            document,
            node,
            List.of("source", "x", "y", "w", "h", "fullscreen", "loop", "loop_fade"),
            List.of()
        );
        Expression<Asset> source = this.asset(document, body.require("source"), allowThis, 0);
        Expression<Float> x = body.property("x") == null
            ? constant(0.0F)
            : this.number(document, body.property("x"), allowThis, 0);
        Expression<Float> y = body.property("y") == null
            ? constant(0.0F)
            : this.number(document, body.property("y"), allowThis, 0);
        Expression<Float> width = body.property("w") == null
            ? constant(0.0F)
            : this.number(document, body.property("w"), allowThis, 0);
        Expression<Float> height = body.property("h") == null
            ? constant(0.0F)
            : this.number(document, body.property("h"), allowThis, 0);
        Expression<Boolean> fullscreen = body.property("fullscreen") == null
            ? constant(true)
            : this.bool(document, body.property("fullscreen"), allowThis, 0);
        Expression<Boolean> loop = body.property("loop") == null
            ? constant(true)
            : this.bool(document, body.property("loop"), allowThis, 0);
        Expression<Integer> loopFadeMillis = body.property("loop_fade") == null
            ? constant(0)
            : this.integer(document, body.property("loop_fade"), allowThis, 0);
        return new VideoDefinition(
            id, source, x, y, width, height, fullscreen, loop, loopFadeMillis
        );
    }

    private TextDefinition compileText(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException {
        String id = this.nodeId(document, node);
        Body body = this.body(
            document,
            node,
            List.of(
                "literal", "translatable", "x", "y", "scale", "color", "shadow",
                "shadow_color", "outline", "align", "font"
            ),
            List.of()
        );
        Value literalValue = body.property("literal");
        Value translatableValue = body.property("translatable");
        if ((literalValue == null) == (translatableValue == null)) {
            throw this.error(document, node.position(),
                "Text requires exactly one of 'literal' or 'translatable'");
        }

        Expression<String> literal = literalValue == null
            ? null
            : this.string(document, literalValue, allowThis, 0);
        Expression<String> translatable = translatableValue == null
            ? null
            : this.string(document, translatableValue, allowThis, 0);
        Expression<Float> x = this.number(document, body.require("x"), allowThis, 0);
        Expression<Float> y = this.number(document, body.require("y"), allowThis, 0);
        Expression<Float> scale = body.property("scale") == null
            ? constant(1.0F)
            : this.number(document, body.property("scale"), allowThis, 0);
        Expression<Integer> color = body.property("color") == null
            ? constant(0xFFFFFFFF)
            : this.color(document, body.property("color"), allowThis, 0);
        Expression<Boolean> shadow = body.property("shadow") == null
            ? constant(false)
            : this.bool(document, body.property("shadow"), allowThis, 0);
        Expression<Integer> shadowColor = body.property("shadow_color") == null
            ? null
            : this.color(document, body.property("shadow_color"), allowThis, 0);
        Expression<Integer> outline = body.property("outline") == null
            ? null
            : this.color(document, body.property("outline"), allowThis, 0);
        Expression<Draw.TextAlign> align = body.property("align") == null
            ? constant(Draw.TextAlign.LEFT)
            : this.textAlign(document, body.property("align"), allowThis, 0);
        Expression<Asset> font = body.property("font") == null
            ? null
            : this.asset(document, body.property("font"), allowThis, 0);

        return new TextDefinition(
            id, literal, translatable, x, y, scale, color, shadow,
            shadowColor, outline, align, font
        );
    }

    private NineDefinition compileNine(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException {
        if (node == null) {
            return null;
        }
        Body body = this.body(document, node, List.of("borders", "mode"), List.of());
        Value borders = body.require("borders");
        List<Value> values = tupleValues(borders);
        Expression<Integer> top;
        Expression<Integer> bottom;
        Expression<Integer> left;
        Expression<Integer> right;
        if (values == null) {
            Expression<Integer> border = this.integer(document, borders, allowThis, 0);
            top = bottom = left = right = border;
        } else {
            if (values.size() != 4) {
                throw this.error(document, borders.position(), "Nine borders require one or four values");
            }
            top = this.integer(document, values.get(0), allowThis, 0);
            bottom = this.integer(document, values.get(1), allowThis, 0);
            left = this.integer(document, values.get(2), allowThis, 0);
            right = this.integer(document, values.get(3), allowThis, 0);
        }
        Expression<Draw.CenterMode> mode = body.property("mode") == null
            ? constant(Draw.CenterMode.STRETCH)
            : this.centerMode(document, body.property("mode"), allowThis, 0);
        return new NineDefinition(top, bottom, left, right, mode);
    }

    private AnimationDefinition compileAnimation(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException {
        if (node == null) {
            return null;
        }
        Body body = this.body(
            document,
            node,
            List.of("frames", "frame_size", "duration", "unit", "interpolation", "frame_durations"),
            List.of()
        );
        Expression<Integer> frames = this.integer(document, body.require("frames"), allowThis, 0);
        Vector2Definition frameSize = this.vector(document, body.require("frame_size"), allowThis);
        Expression<Integer> duration = this.integer(document, body.require("duration"), allowThis, 0);
        Expression<Draw.AnimUnit> unit = body.property("unit") == null
            ? constant(Draw.AnimUnit.MILLISECONDS)
            : this.animationUnit(document, body.property("unit"), allowThis, 0);
        Expression<Boolean> interpolation = body.property("interpolation") == null
            ? constant(false)
            : this.bool(document, body.property("interpolation"), allowThis, 0);

        List<Expression<Integer>> frameDurations = new ArrayList<>();
        Value rawDurations = body.property("frame_durations");
        if (rawDurations != null) {
            List<Value> values = tupleValues(rawDurations);
            if (values == null) {
                throw this.error(document, rawDurations.position(),
                    "frame_durations must be a list or tuple");
            }
            for (Value value : values) {
                frameDurations.add(this.integer(document, value, allowThis, 0));
            }
        }
        return new AnimationDefinition(frames, frameSize, duration, unit, interpolation, frameDurations);
    }

    private Vector2Definition optionalVector(UiDocument document, Value value, boolean allowThis)
        throws UiCompileException {
        return value == null ? null : this.vector(document, value, allowThis);
    }

    private Vector2Definition vector(UiDocument document, Value value, boolean allowThis)
        throws UiCompileException {
        List<Value> values = tupleValues(value);
        if (values == null || values.size() != 2) {
            throw this.error(document, value.position(), "Expected exactly two values");
        }
        return new Vector2Definition(
            this.number(document, values.get(0), allowThis, 0),
            this.number(document, values.get(1), allowThis, 0)
        );
    }

    private Expression<Float> number(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        this.checkDepth(document, value, depth);
        if (value instanceof NumberValue number) {
            try {
                float parsed = Float.parseFloat(number.literal());
                return constant(parsed);
            } catch (NumberFormatException exception) {
                throw this.error(document, value.position(), "Invalid number '" + number.literal() + "'");
            }
        }
        if (value instanceof ExposedDataValue data) {
            this.validateDataContext(document, data, allowThis);
            return context -> asNumber(context.resolve(data.path()), document.source(), data.position()).floatValue();
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
        if (value instanceof CallValue call && call.name().equals("mat")) {
            if (call.arguments().size() != 1) {
                throw this.error(document, call.position(), "mat() requires one mathematical expression");
            }
            return this.number(document, call.arguments().getFirst(), allowThis, depth + 1);
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(document, conditional.condition(), allowThis, depth + 1);
            Expression<Float> whenTrue = this.number(document, conditional.whenTrue(), allowThis, depth + 1);
            Expression<Float> whenFalse = this.number(document, conditional.whenFalse(), allowThis, depth + 1);
            return context -> condition.resolve(context) ? whenTrue.resolve(context) : whenFalse.resolve(context);
        }
        Resolved resolved = this.resolve(document, value, depth + 1);
        if (resolved != null) {
            return this.number(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.error(document, value.position(), "Expected a number");
    }

    private Expression<Integer> integer(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        Expression<Float> number = this.number(document, value, allowThis, depth);
        return context -> Math.round(number.resolve(context));
    }

    private Expression<Boolean> bool(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        this.checkDepth(document, value, depth);
        if (value instanceof BooleanValue bool) {
            return constant(bool.value());
        }
        if (value instanceof ExposedDataValue data) {
            this.validateDataContext(document, data, allowThis);
            return context -> asBoolean(context.resolve(data.path()), document.source(), data.position());
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
        Resolved resolved = this.resolve(document, value, depth + 1);
        if (resolved != null) {
            return this.bool(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.error(document, value.position(), "Expected a boolean");
    }

    private Expression<Object> any(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        this.checkDepth(document, value, depth);
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
                throw this.error(document, number.position(), "Invalid number '" + number.literal() + "'");
            }
        }
        if (value instanceof BooleanValue bool) {
            return constant(bool.value());
        }
        if (value instanceof NullValue) {
            return constant(null);
        }
        if (value instanceof UiDocument.ColorValue color) {
            return constant(color.literal());
        }
        if (value instanceof ExposedDataValue data) {
            this.validateDataContext(document, data, allowThis);
            return context -> context.resolve(data.path());
        }
        if (value instanceof LogicalValue || value instanceof NegatedValue || value instanceof ComparisonValue) {
            Expression<Boolean> expression = this.bool(document, value, allowThis, depth + 1);
            return context -> expression.resolve(context);
        }
        if (value instanceof ArithmeticValue || value instanceof ArithmeticNegationValue
            || value instanceof CallValue call && call.name().equals("mat")) {
            Expression<Float> expression = this.number(document, value, allowThis, depth + 1);
            return context -> expression.resolve(context);
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(document, conditional.condition(), allowThis, depth + 1);
            Expression<Object> whenTrue = this.any(document, conditional.whenTrue(), allowThis, depth + 1);
            Expression<Object> whenFalse = this.any(document, conditional.whenFalse(), allowThis, depth + 1);
            return context -> condition.resolve(context) ? whenTrue.resolve(context) : whenFalse.resolve(context);
        }
        Resolved resolved = this.resolve(document, value, depth + 1);
        if (resolved != null) {
            if (resolved.asset() != null) {
                return constant(resolved.asset());
            }
            return this.any(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.error(document, value.position(), "Expected a comparable value");
    }

    private Expression<String> string(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        this.checkDepth(document, value, depth);
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
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(document, conditional.condition(), allowThis, depth + 1);
            Expression<String> whenTrue = this.string(document, conditional.whenTrue(), allowThis, depth + 1);
            Expression<String> whenFalse = this.string(document, conditional.whenFalse(), allowThis, depth + 1);
            return context -> condition.resolve(context) ? whenTrue.resolve(context) : whenFalse.resolve(context);
        }
        Resolved resolved = this.resolve(document, value, depth + 1);
        if (resolved != null) {
            return this.string(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.error(document, value.position(), "Expected text");
    }

    private Expression<Asset> asset(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        this.checkDepth(document, value, depth);
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
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(document, conditional.condition(), allowThis, depth + 1);
            Expression<Asset> whenTrue = this.asset(document, conditional.whenTrue(), allowThis, depth + 1);
            Expression<Asset> whenFalse = this.asset(document, conditional.whenFalse(), allowThis, depth + 1);
            return context -> condition.resolve(context) ? whenTrue.resolve(context) : whenFalse.resolve(context);
        }
        Resolved resolved = this.resolve(document, value, depth + 1);
        if (resolved != null) {
            if (resolved.asset() != null) {
                return constant(resolved.asset());
            }
            return this.asset(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.error(document, value.position(), "Expected an imported Asset reference");
    }

    private Expression<Integer> color(UiDocument document, Value value, boolean allowThis, int depth)
        throws UiCompileException {
        this.checkDepth(document, value, depth);
        if (value instanceof UiDocument.ColorValue color) {
            return constant(parseColor(document, color));
        }
        if (value instanceof ExposedDataValue data) {
            this.validateDataContext(document, data, allowThis);
            return context -> asColor(context.resolve(data.path()), document.source(), data.position());
        }
        if (value instanceof ConditionalValue conditional) {
            Expression<Boolean> condition = this.bool(document, conditional.condition(), allowThis, depth + 1);
            Expression<Integer> whenTrue = this.color(document, conditional.whenTrue(), allowThis, depth + 1);
            Expression<Integer> whenFalse = this.color(document, conditional.whenFalse(), allowThis, depth + 1);
            return context -> condition.resolve(context) ? whenTrue.resolve(context) : whenFalse.resolve(context);
        }
        Resolved resolved = this.resolve(document, value, depth + 1);
        if (resolved != null) {
            return this.color(resolved.document(), resolved.value(), allowThis, depth + 1);
        }
        throw this.error(document, value.position(), "Expected a color");
    }

    private Expression<Draw.TextAlign> textAlign(
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

    private Expression<Draw.CenterMode> centerMode(
        UiDocument document, Value value, boolean allowThis, int depth
    ) throws UiCompileException {
        Expression<String> text = this.string(document, value, allowThis, depth);
        return context -> switch (text.resolve(context).toLowerCase(Locale.ROOT)) {
            case "stretch" -> Draw.CenterMode.STRETCH;
            case "repeat" -> Draw.CenterMode.REPEAT;
            default -> throw new IllegalArgumentException("Unsupported Nine center mode");
        };
    }

    private Expression<Draw.AnimUnit> animationUnit(
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

    private Resolved resolve(UiDocument document, Value value, int depth) throws UiCompileException {
        if (value instanceof BlueprintParameterValue parameter) {
            throw this.error(document, parameter.position(),
                "Blueprint parameter references are only valid inside a blueprint");
        }
        if (!(value instanceof ReferenceValue reference)) {
            return null;
        }
        this.checkDepth(document, value, depth);
        List<String> path = reference.path();
        String root = path.getFirst();

        Value definition = document.definition(root).orElse(null);
        if (definition != null) {
            return new Resolved(document, this.descend(document, definition, path.subList(1, path.size())), null);
        }

        ImportDirective imported = document.imports().stream()
            .filter(candidate -> candidate.alias().equals(root))
            .findFirst()
            .orElseThrow(() -> this.error(document, reference.position(), "Unknown symbol '@" + root + "'"));
        Asset importedAsset = importAsset(document.source(), imported);
        UiFileType importedType = UiFileType.fromImportPath(imported.target().path()).orElse(null);
        if (importedType == null) {
            if (path.size() != 1) {
                throw this.error(document, reference.position(),
                    "A raw Asset reference cannot have nested properties");
            }
            return new Resolved(document, null, importedAsset);
        }

        UiDocument importedDocument = this.documents.get(importedAsset);
        if (importedDocument == null) {
            throw this.error(document, reference.position(), "Imported UI document is not loaded: " + importedAsset);
        }
        if (path.size() < 2) {
            throw this.error(document, reference.position(),
                "A UI document alias must reference one of its definitions");
        }
        Value importedDefinition = importedDocument.definition(path.get(1)).orElseThrow(() ->
            this.error(document, reference.position(),
                "Imported definition '" + path.get(1) + "' does not exist in " + importedAsset)
        );
        return new Resolved(
            importedDocument,
            this.descend(importedDocument, importedDefinition, path.subList(2, path.size())),
            null
        );
    }

    private Value descend(UiDocument document, Value value, List<String> path) throws UiCompileException {
        Value current = value;
        for (String segment : path) {
            if (!(current instanceof ObjectValue object)) {
                throw this.error(document, current.position(),
                    "Cannot read property '" + segment + "' from this value");
            }
            current = object.values().get(segment);
            if (current == null) {
                throw this.error(document, object.position(), "Unknown property '" + segment + "'");
            }
        }
        return current;
    }

    private Body body(
        UiDocument document,
        Node node,
        List<String> allowedProperties,
        List<String> allowedNodes
    ) throws UiCompileException {
        Map<String, Value> properties = new LinkedHashMap<>();
        Map<String, Node> nodes = new LinkedHashMap<>();
        for (Statement statement : node.body()) {
            if (statement instanceof Property property) {
                if (!allowedProperties.contains(property.name())) {
                    throw this.error(document, property.position(),
                        "Unsupported property '" + property.name() + "' in '" + node.name() + "'");
                }
                if (properties.putIfAbsent(property.name(), property.value()) != null) {
                    throw this.error(document, property.position(), "Duplicate property '" + property.name() + "'");
                }
            } else if (statement instanceof Node child) {
                if (!allowedNodes.contains(child.name())) {
                    throw this.error(document, child.position(), "Unsupported block '" + child.name() + "'");
                }
                if (nodes.putIfAbsent(child.name(), child) != null) {
                    throw this.error(document, child.position(), "Duplicate block '" + child.name() + "'");
                }
            } else {
                throw this.error(document, statement.position(), "Directives are not valid inside this block");
            }
        }
        return new Body(document, node, properties, nodes);
    }

    private String nodeId(UiDocument document, Node node) throws UiCompileException {
        if (node.arguments().size() != 1 || !(node.arguments().getFirst() instanceof StringValue id)
            || id.value().isBlank()) {
            throw this.error(document, node.position(),
                "The '" + node.name() + "' declaration requires one non-empty quoted id");
        }
        return id.value();
    }

    private void validateDataContext(UiDocument document, ExposedDataValue data, boolean allowThis)
        throws UiCompileException {
        if (!allowThis && data.path().getFirst().equals("this")) {
            throw this.error(document, data.position(),
                "${this.*} is only valid inside a widget's renderables");
        }
    }

    private void checkDepth(UiDocument document, Value value, int depth) throws UiCompileException {
        if (depth > MAX_REFERENCE_DEPTH) {
            throw this.error(document, value.position(), "Definition/reference chain is too deep or circular");
        }
    }

    private UiCompileException error(UiDocument document, UiDocument.Position position, String message) {
        return new UiCompileException(document.source(), position, message);
    }

    private static List<Value> tupleValues(Value value) {
        if (value instanceof TupleValue tuple) {
            return tuple.values();
        }
        if (value instanceof ListValue list) {
            return list.values();
        }
        return null;
    }

    private static Asset importAsset(Asset owner, ImportDirective imported) {
        String namespace = imported.target().local() ? owner.namespace() : imported.target().namespace();
        String path = imported.target().path();
        UiFileType type = UiFileType.fromImportPath(path).orElse(null);
        String resourcePath = type == null ? path : type.resourcePath(type.stripDirectory(path));
        return Asset.NamespaceAndPatch(namespace, resourcePath);
    }

    private static Number asNumber(Object value, Asset source, UiDocument.Position position) {
        if (value instanceof Number number) {
            return number;
        }
        throw runtimeType(source, position, "Number", value);
    }

    private static boolean asBoolean(Object value, Asset source, UiDocument.Position position) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw runtimeType(source, position, "Boolean", value);
    }

    private static int asColor(Object value, Asset source, UiDocument.Position position) {
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

    private static int parseColor(UiDocument document, UiDocument.ColorValue color)
        throws UiCompileException {
        try {
            return parseColorLiteral(color.literal());
        } catch (IllegalArgumentException exception) {
            throw new UiCompileException(document.source(), color.position(), exception.getMessage());
        }
    }

    private static int parseColorLiteral(String literal) {
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

    private static boolean compare(
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

    private static float calculate(
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

    private static IllegalArgumentException arithmeticError(
        Asset source,
        ArithmeticValue arithmetic,
        String message
    ) {
        return new IllegalArgumentException(
            source + ":" + arithmetic.position().line() + ":" + arithmetic.position().column()
                + ": " + message + " in mat()"
        );
    }

    private static boolean valuesEqual(Object left, Object right) {
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            return Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue()) == 0;
        }
        return Objects.equals(left, right);
    }

    private static int compareOrdered(
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

    private static IllegalArgumentException runtimeType(
        Asset source, UiDocument.Position position, String expected, Object value
    ) {
        String actual = value == null ? "null" : value.getClass().getSimpleName();
        return new IllegalArgumentException(
            source + ":" + position.line() + ":" + position.column()
                + ": expected " + expected + ", found " + actual
        );
    }

    private static <T> Expression<T> constant(T value) {
        return ignored -> value;
    }

    private static void ensureUniqueIds(UiDocument document, List<String> ids, String kind)
        throws UiCompileException {
        Map<String, Boolean> found = new LinkedHashMap<>();
        for (String id : ids) {
            if (found.putIfAbsent(id, Boolean.TRUE) != null) {
                throw new UiCompileException(document.source(), new UiDocument.Position(1, 1),
                    "Duplicate " + kind + " id '" + id + "'");
            }
        }
    }

    private record Resolved(UiDocument document, Value value, Asset asset) {
    }

    private record BlueprintTarget(UiDocument document, BlueprintDirective blueprint) {
    }

    private record BlueprintKey(Asset source, String name) {
    }

    private final class Body {
        private final UiDocument document;
        private final Node owner;
        private final Map<String, Value> properties;
        private final Map<String, Node> nodes;

        private Body(UiDocument document, Node owner, Map<String, Value> properties, Map<String, Node> nodes) {
            this.document = document;
            this.owner = owner;
            this.properties = properties;
            this.nodes = nodes;
        }

        Value property(String name) {
            return this.properties.get(name);
        }

        Value require(String name) throws UiCompileException {
            Value value = this.property(name);
            if (value == null) {
                throw UiCompiler.this.error(this.document, this.owner.position(),
                    "Missing required property '" + name + "'");
            }
            return value;
        }

        Node node(String name) {
            return this.nodes.get(name);
        }
    }
}
