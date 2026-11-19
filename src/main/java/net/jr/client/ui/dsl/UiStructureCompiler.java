package net.jr.client.ui.dsl;

import net.jr.api.client.ui.UiFileType;
import net.jr.client.ui.dsl.UiCompiledDocument.BoundsDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.ConditionalRenderableDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.ConditionalEntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiCompiledDocument.LoadingBarDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.CanvasDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.RenderableDefinition;
import net.jr.client.ui.dsl.UiDocument.BlueprintDirective;
import net.jr.client.ui.dsl.UiDocument.BlueprintUse;
import net.jr.client.ui.dsl.UiDocument.ConditionalStatement;
import net.jr.client.ui.dsl.UiDocument.ContentInsertion;
import net.jr.client.ui.dsl.UiDocument.ImportDirective;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.Statement;
import net.jr.client.ui.dsl.UiDocument.Value;
import net.jr.client.ui.dsl.UiBlueprintCompiler.Key;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class UiStructureCompiler {
    private final UiCompilationContext context;
    private final UiReferenceResolver references;
    private final UiExpressionCompiler expressions;
    private final UiBlueprintCompiler blueprints;
    private final UiElementCompilerRegistry elements;
    private final UiLayoutContext layouts;
    private final UiJavaElementCompiler javaElements;

    UiStructureCompiler(
        UiCompilationContext context,
        UiReferenceResolver references,
        UiExpressionCompiler expressions,
        UiBlueprintCompiler blueprints,
        UiElementCompilerRegistry elements,
        UiElementContext elementContext
    ) {
        this.context = context;
        this.references = references;
        this.expressions = expressions;
        this.blueprints = blueprints;
        this.elements = elements;
        this.layouts = new UiLayoutContext(elementContext, this::compileChildEntries);
        this.javaElements = new UiJavaElementCompiler(
            new UiJavaElementContext(
                context, elementContext, blueprints, this::compileVisualEntries
            )
        );
    }

    UiCompilationResult compile(UiDocument document) {
        this.context.begin(document);
        this.blueprints.resetSources();
        try {
            UiCompiledDocument compiled = this.compileDocument(document);
            return new UiCompilationResult(compiled, this.context.diagnostics());
        } catch (UiCompileException exception) {
            this.context.diagnostics().add(exception);
        } catch (RuntimeException exception) {
            this.context.diagnostics().add(this.error(
                document,
                new UiDocument.Position(1, 1),
                "Internal DSL compilation error: "
                    + (exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage())
            ));
        }
        return new UiCompilationResult(
            UiCompiledDocument.empty(document.source(), document.type()),
            this.context.diagnostics()
        );
    }

    private UiCompiledDocument compileDocument(UiDocument document) throws UiCompileException {
        this.validateCapabilities(document);
        String expectedRoot = switch (document.type()) {
            case SCREEN -> "screen";
            case CONTAINER -> "container";
            case OVERLAY -> "overlay";
            case STYLE -> throw this.error(document, new UiDocument.Position(1, 1),
                "Registered style files are reusable definitions and cannot be used as a layout");
            case DATA -> throw this.error(document, new UiDocument.Position(1, 1),
                "Data files contain values and cannot be used as a layout");
        };

        Node root = null;
        for (Statement statement : document.statements()) {
            if (statement instanceof Node node) {
                if (!node.name().equals(expectedRoot)) {
                    this.context.diagnostics().add(this.error(document, node.position(),
                        "Expected root block '" + expectedRoot + "', found '" + node.name() + "'"));
                    continue;
                }
                if (root != null) {
                    this.context.diagnostics().add(this.error(
                        document, node.position(), "A UI document can only have one root block"
                    ));
                    continue;
                }
                root = node;
            } else if (statement instanceof BlueprintUse use) {
                this.context.diagnostics().add(this.error(document, use.position(),
                    "A blueprint instance must be placed inside a block that can consume its structure"));
            }
        }
        if (root == null) {
            this.context.diagnostics().add(this.error(document, new UiDocument.Position(1, 1),
                "Missing root block '" + expectedRoot + "'"));
            return UiCompiledDocument.empty(document.source(), document.type());
        }
        if (!root.arguments().isEmpty()) {
            this.context.diagnostics().add(this.error(document, root.position(),
                "The root '" + expectedRoot + "' block does not accept an id or arguments"));
        }

        Map<String, Expression<Object>> properties = new LinkedHashMap<>();
        Map<UiRenderLayer, List<EntryDefinition>> layers = new LinkedHashMap<>();
        BoundsAccumulator bounds = new BoundsAccumulator();
        this.compileRootStatements(
            document,
            root.body(),
            expectedRoot,
            Map.of(),
            new ArrayDeque<>(),
            properties,
            layers,
            bounds
        );
        this.context.state().validateReferences();

        return new UiCompiledDocument(
            document.source(), document.type(), properties, bounds.definition, layers,
            this.context.state().locations(),
            this.context.state().propertyLocations()
        );
    }

    private void compileRootStatements(
        UiDocument document,
        List<Statement> statements,
        String rootName,
        Map<String, Value> parameters,
        Deque<Key> expansionStack,
        Map<String, Expression<Object>> properties,
        Map<UiRenderLayer, List<EntryDefinition>> layers,
        BoundsAccumulator bounds
    ) throws UiCompileException {
        for (Statement statement : statements) {
            UiCompilationContext.Checkpoint checkpoint = this.context.state().checkpoint();
            Map<String, Expression<Object>> propertySnapshot = new LinkedHashMap<>(properties);
            Map<UiRenderLayer, List<EntryDefinition>> layerSnapshot = new LinkedHashMap<>(layers);
            BoundsDefinition boundsSnapshot = bounds.definition;
            try {
                if (statement instanceof Property property) {
                    if (properties.containsKey(property.name())) {
                        throw this.error(document, property.position(),
                            "Duplicate document property '" + property.name() + "'");
                    }
                    properties.put(
                        property.name(),
                        this.expressions.any(
                            document,
                            this.blueprints.substituteValue(document, property.value(), parameters),
                            false,
                            0
                        )
                    );
                } else if (statement instanceof Node node) {
                    UiRenderLayer layer = switch (node.name()) {
                        case "background" -> UiRenderLayer.BACKGROUND;
                        case "content" -> UiRenderLayer.CONTENT;
                        case "foreground" -> UiRenderLayer.FOREGROUND;
                        default -> throw this.error(document, node.position(),
                            "Unknown " + rootName + " block '" + node.name() + "'");
                    };
                    if (layers.containsKey(layer)) {
                        throw this.error(document, node.position(),
                            "Duplicate '" + node.name() + "' block");
                    }
                    layers.put(
                        layer,
                        this.compileEntries(
                            document,
                            node.body(),
                            false,
                            parameters,
                            expansionStack,
                            layer == UiRenderLayer.BACKGROUND ? bounds : null
                        )
                    );
                } else if (statement instanceof BlueprintUse use) {
                    this.blueprints.expand(
                        document,
                        use,
                        parameters,
                        expansionStack,
                        (blueprintDocument, body, bindings, stack) -> {
                            this.compileRootStatements(
                                blueprintDocument,
                                body,
                                rootName,
                                bindings,
                                stack,
                                properties,
                                layers,
                                bounds
                            );
                            return null;
                        }
                    );
                } else if (statement instanceof ContentInsertion insertion) {
                    this.compileRootStatements(
                        this.blueprints.insertionDocument(document, insertion),
                        insertion.body(),
                        rootName,
                        Map.of(),
                        expansionStack,
                        properties,
                        layers,
                        bounds
                    );
                } else {
                    throw this.error(document, statement.position(),
                        "Only properties, render blocks and blueprint instances are valid inside '"
                            + rootName + "'");
                }
            } catch (UiCompileException exception) {
                this.context.state().rollback(checkpoint);
                properties.clear();
                properties.putAll(propertySnapshot);
                layers.clear();
                layers.putAll(layerSnapshot);
                bounds.definition = boundsSnapshot;
                this.context.diagnostics().add(exception);
            } catch (RuntimeException exception) {
                this.context.state().rollback(checkpoint);
                properties.clear();
                properties.putAll(propertySnapshot);
                layers.clear();
                layers.putAll(layerSnapshot);
                bounds.definition = boundsSnapshot;
                this.context.diagnostics().add(this.unexpected(document, statement.position(), exception));
            }
        }
    }

    private List<EntryDefinition> compileChildEntries(
        UiDocument document,
        List<Statement> statements,
        boolean allowThis,
        Map<String, Value> parameters,
        Deque<Key> expansionStack
    ) throws UiCompileException {
        return this.compileEntries(
            document, statements, allowThis, parameters, expansionStack, null
        );
    }

    private List<EntryDefinition> compileEntries(
        UiDocument document,
        List<Statement> statements,
        boolean allowThis,
        Map<String, Value> parameters,
        Deque<Key> expansionStack,
        BoundsAccumulator bounds
    ) throws UiCompileException {
        List<EntryDefinition> entries = new ArrayList<>();
        for (Statement statement : statements) {
            UiCompilationContext.Checkpoint checkpoint = this.context.state().checkpoint();
            int entryCount = entries.size();
            try {
            if (statement instanceof Node node) {
                if (node.name().equals("bounds")) {
                    if (bounds == null) {
                        throw this.error(document, node.position(),
                            "Document bounds are only valid directly inside 'background'");
                    }
                    if (bounds.definition != null) {
                        throw this.error(document, node.position(),
                            "A UI document can only declare one bounds block");
                    }
                    bounds.definition = this.compileBounds(document, node, parameters);
                    continue;
                }
                if (this.elements.hasLayout(node.name())) {
                    Node expanded = parameters.isEmpty()
                        ? node
                        : this.blueprints.substituteNode(document, node, parameters);
                    EntryDefinition layout = this.elements.compileLayout(
                        this.layouts, document, expanded, expansionStack, allowThis
                    );
                    this.trackLayoutProperties(document, expanded, layout.id(), !expansionStack.isEmpty());
                    entries.add(layout);
                    continue;
                }
                if (UiDslRegistry.find(node.name()).isPresent()) {
                    if (allowThis) {
                        throw this.error(document, node.position(),
                            "A visual-only block cannot contain Java-owned DSL elements");
                    }
                    Node expanded = this.blueprints.substituteNode(document, node, parameters);
                    entries.add(this.javaElements.compile(
                        document, expanded, Map.of(), expansionStack, !expansionStack.isEmpty()
                    ));
                } else {
                    Node expanded = parameters.isEmpty()
                        ? node
                        : this.blueprints.substituteNode(document, node, parameters);
                    entries.add(this.compileRenderable(document, expanded, allowThis, !expansionStack.isEmpty()));
                }
            } else if (statement instanceof BlueprintUse use) {
                entries.addAll(this.blueprints.expand(
                    document,
                    use,
                    parameters,
                    expansionStack,
                    (blueprintDocument, body, bindings, stack) -> this.compileEntries(
                        blueprintDocument, body, allowThis, bindings, stack, bounds
                    )
                ));
            } else if (statement instanceof ContentInsertion insertion) {
                entries.addAll(this.compileEntries(
                    this.blueprints.insertionDocument(document, insertion),
                    insertion.body(),
                    allowThis,
                    Map.of(),
                    expansionStack,
                    bounds
                ));
            } else if (statement instanceof ConditionalStatement conditional) {
                Value conditionValue = this.blueprints.substituteValue(document, conditional.condition(), parameters);
                Expression<Boolean> condition = this.expressions.bool(document, conditionValue, allowThis, 0);
                List<EntryDefinition> nested = this.compileEntries(
                    document,
                    conditional.body(),
                    allowThis,
                    parameters,
                    expansionStack,
                    null
                );
                for (EntryDefinition entry : nested) {
                    if (entry instanceof RenderableDefinition renderable) {
                        entries.add(new ConditionalRenderableDefinition(
                            renderable.id(), condition, renderable
                        ));
                    } else if (entry instanceof CanvasDefinition
                        || entry instanceof LoadingBarDefinition
                        || entry instanceof ConditionalEntryDefinition) {
                        entries.add(new ConditionalEntryDefinition(
                            entry.id(), condition, entry
                        ));
                    } else {
                        throw this.error(document, conditional.position(),
                            "Conditional declarations only support visual elements");
                    }
                }
            } else {
                throw this.error(document, statement.position(),
                    "Expected a visual element, conditional block or blueprint instance");
            }
            } catch (UiCompileException exception) {
                this.context.state().rollback(checkpoint);
                while (entries.size() > entryCount) {
                    entries.removeLast();
                }
                this.context.diagnostics().add(exception);
            } catch (RuntimeException exception) {
                this.context.state().rollback(checkpoint);
                while (entries.size() > entryCount) {
                    entries.removeLast();
                }
                this.context.diagnostics().add(this.unexpected(document, statement.position(), exception));
            }
        }
        return List.copyOf(entries);
    }

    private BoundsDefinition compileBounds(
        UiDocument document,
        Node node,
        Map<String, Value> parameters
    ) throws UiCompileException {
        this.context.state().locate(document, node.position(), "$bounds");
        if (!node.arguments().isEmpty()) {
            throw this.error(document, node.position(), "bounds does not accept arguments");
        }
        Node expanded = parameters.isEmpty()
            ? node
            : this.blueprints.substituteNode(document, node, parameters);
        UiNodeBody body = this.body(document, expanded, List.of("x", "y", "w", "h"), List.of());
        this.trackLayoutProperties(document, expanded, "$bounds", !parameters.isEmpty());
        Value x = body.property("x");
        Value y = body.property("y");
        return new BoundsDefinition(
            x == null ? this.expressions.constant(0) : this.expressions.integer(document, x, false, 0),
            y == null ? this.expressions.constant(0) : this.expressions.integer(document, y, false, 0),
            this.expressions.integer(document, body.require("w"), false, 0),
            this.expressions.integer(document, body.require("h"), false, 0)
        );
    }

    private List<EntryDefinition> compileVisualEntries(
        UiDocument document,
        Node block,
        Map<String, Value> parameters,
        Deque<Key> expansionStack
    ) throws UiCompileException {
        List<EntryDefinition> entries = this.compileEntries(
            document, block.body(), true, parameters, expansionStack, null
        );
        this.layouts.requireVisual(document, block, entries);
        return entries;
    }

    private void trackLayoutProperties(UiDocument document, Node node, String id,
                                       boolean fromExpansion) {
        for (Statement statement : node.body()) {
            if (!(statement instanceof Property property)) continue;
            String name = switch (property.name()) {
                case "width" -> "w";
                case "height" -> "h";
                default -> property.name();
            };
            Value value = property.value();
            var source = this.blueprints.sourceOf(document, value);
            boolean editable = source.equals(this.context.document().source())
                && (value instanceof UiDocument.NumberValue
                    || value instanceof UiDocument.StringValue
                    || value instanceof UiDocument.BooleanValue)
                && (!fromExpansion || this.blueprints.isInstanceArgument(value));
            boolean branchesEditable = source.equals(this.context.document().source())
                && value instanceof UiDocument.ConditionalValue
                && (!fromExpansion || this.blueprints.isInstanceArgument(value));
            this.context.state().locateProperty(
                source, value.position(), id, name, editable, branchesEditable
            );
        }
    }

    private RenderableDefinition compileRenderable(
        UiDocument document, Node node, boolean allowThis, boolean fromExpansion
    )
        throws UiCompileException {
        this.context.profile().require(
            UiLanguageCapability.VISUAL_ELEMENTS, document, node.position()
        );
        RenderableDefinition definition = this.elements.compile(document, node, allowThis);
        // Trace the real argument tokens, including nested Nine and animation fields.
        // Editing an instance never targets the shared blueprint definition.
        for (Statement statement : node.body()) {
            if (statement instanceof Property property) {
                String name = switch (property.name()) {
                    case "width" -> "w";
                    case "height" -> "h";
                    default -> camelCase(property.name());
                };
                this.trackEditableValue(document, definition.id(), name, property.value(), fromExpansion);
            } else if (statement instanceof Node nested &&
                    (nested.name().equals("nine") || nested.name().equals("animation"))) {
                for (Statement nestedStatement : nested.body()) {
                    if (nestedStatement instanceof Property property) {
                        this.trackEditableValue(document, definition.id(),
                            nested.name() + "." + camelCase(property.name()), property.value(), fromExpansion);
                    }
                }
            }
        }
        return definition;
    }

    private static String camelCase(String name) {
        StringBuilder result = new StringBuilder();
        boolean upper = false;
        for (char ch : name.toCharArray()) {
            if (ch == '_') upper = true;
            else { result.append(upper ? Character.toUpperCase(ch) : ch); upper = false; }
        }
        return result.toString();
    }

    private void trackEditableValue(UiDocument document, String id, String key,
                                    Value value, boolean fromExpansion) {
        List<Value> tuple = value instanceof UiDocument.TupleValue tv ? tv.values()
            : value instanceof UiDocument.ListValue lv ? lv.values() : null;
        if (tuple != null) {
            String[] names;
            if (key.equals("nine.borders") && tuple.size() == 4) {
                names = new String[] {"nine.top", "nine.bottom", "nine.left", "nine.right"};
            } else if (key.equals("animation.frameSize") && tuple.size() == 2) {
                names = new String[] {"animation.frameSize.first", "animation.frameSize.second"};
            } else if ((key.equals("uv") || key.equals("uvSize") || key.equals("atlasSize")) && tuple.size() == 2) {
                names = new String[] {key + ".first", key + ".second"};
            } else {
                names = new String[tuple.size()];
                for (int i = 0; i < names.length; i++) names[i] = key + "." + i;
            }
            for (int i = 0; i < tuple.size(); i++) {
                this.trackEditableValue(document, id, names[i], tuple.get(i), fromExpansion);
            }
            return;
        }
        if (key.equals("nine.borders")) key = "nine.borders";
        var source = this.blueprints.sourceOf(document, value);
        boolean literal = value instanceof UiDocument.NumberValue
            || value instanceof UiDocument.StringValue
            || value instanceof UiDocument.BooleanValue
            || value instanceof UiDocument.BareValue
            || value instanceof UiDocument.ColorValue;
        boolean editable = source.equals(this.context.document().source()) && literal
            && (!fromExpansion || this.blueprints.isInstanceArgument(value));
        boolean branchesEditable = source.equals(this.context.document().source())
            && value instanceof UiDocument.ConditionalValue
            && (!fromExpansion || this.blueprints.isInstanceArgument(value));
        this.context.state().locateProperty(source, value.position(), id, key, editable, branchesEditable);
    }

    private void validateCapabilities(UiDocument document) throws UiCompileException {
        for (Statement statement : document.statements()) {
            if (statement instanceof ImportDirective imported) {
                UiFileType type = UiFileType.fromImportPath(imported.target().path()).orElse(null);
                this.context.profile().require(
                    type == null
                        ? UiLanguageCapability.ASSET_IMPORTS
                        : UiLanguageCapability.DOCUMENT_IMPORTS,
                    document,
                    imported.position()
                );
            } else if (statement instanceof BlueprintDirective blueprint) {
                this.context.profile().require(
                    UiLanguageCapability.BLUEPRINT_DECLARATIONS,
                    document,
                    blueprint.position()
                );
            } else if (statement instanceof Node node) {
                this.context.profile().require(
                    UiLanguageCapability.LAYOUT, document, node.position()
                );
            }
        }
    }

    private UiNodeBody body(
        UiDocument document,
        Node node,
        List<String> allowedProperties,
        List<String> allowedNodes
    ) throws UiCompileException {
        return UiNodeBody.read(this.context, document, node, allowedProperties, allowedNodes);
    }

    private UiCompileException error(UiDocument document, UiDocument.Position position, String message) {
        return new UiCompileException(document.source(), position, message);
    }

    private UiCompileException unexpected(
        UiDocument document,
        UiDocument.Position position,
        RuntimeException exception
    ) {
        String detail = exception.getMessage();
        return this.error(
            document,
            position,
            "Could not compile this declaration: "
                + (detail == null ? exception.getClass().getSimpleName() : detail)
        );
    }

    private static final class BoundsAccumulator {
        private BoundsDefinition definition;
    }

}


