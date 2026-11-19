package net.jr.client.ui.dsl;

import net.jr.client.ui.dsl.UiCompiledDocument.DslBlockDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.DslNodeDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiDocument.BlueprintUse;
import net.jr.client.ui.dsl.UiDocument.ContentInsertion;
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

final class UiJavaElementCompiler {
    private final UiJavaElementContext context;

    UiJavaElementCompiler(UiJavaElementContext context) {
        this.context = context;
    }

    DslNodeDefinition compile(
        UiDocument document,
        Node node,
        Map<String, Value> parameters,
        Deque<Key> expansionStack,
        boolean fromExpansion
    ) throws UiCompileException {
        String id = this.context.elements().id(document, node);
        Map<String, Expression<Object>> properties = new LinkedHashMap<>();
        Map<String, List<EntryDefinition>> renderBlocks = new LinkedHashMap<>();
        List<DslBlockDefinition> children = new ArrayList<>();
        this.compileDslStatements(
            document,
            node.body(),
            "DSL element '" + id + "'", id, fromExpansion,
            parameters,
            expansionStack,
            properties,
            renderBlocks,
            children
        );
        return new DslNodeDefinition(node.name(), id, properties, renderBlocks, children);
    }

    private DslBlockDefinition compileDslBlock(
        UiDocument document,
        Node node,
        Map<String, Value> parameters,
        Deque<Key> expansionStack,
        boolean fromExpansion
    ) throws UiCompileException {
        List<Value> expandedArguments = new ArrayList<>();
        for (Value argument : node.arguments()) {
            expandedArguments.add(this.context.blueprints().substituteValue(document, argument, parameters));
        }
        List<Expression<Object>> arguments = new ArrayList<>();
        String id = this.context.elements().blockId(document, node);
        for (int index = 0; index < expandedArguments.size(); index++) {
            Value argument = expandedArguments.get(index);
            arguments.add(this.context.elements().expressions().any(document, argument, false, 0));
            var source = this.context.blueprints().sourceOf(document, argument);
            boolean editable = source.equals(this.context.compilation().document().source())
                && (argument instanceof UiDocument.NumberValue
                    || argument instanceof UiDocument.StringValue
                    || argument instanceof UiDocument.BooleanValue)
                && (!fromExpansion || this.context.blueprints().isInstanceArgument(argument));
            boolean branchesEditable = source.equals(this.context.compilation().document().source())
                && argument instanceof UiDocument.ConditionalValue
                && (!fromExpansion || this.context.blueprints().isInstanceArgument(argument));
            this.context.compilation().state().locateProperty(
                source, argument.position(), id, "arg" + index, editable, branchesEditable
            );
        }
        Map<String, Expression<Object>> properties = new LinkedHashMap<>();
        Map<String, List<EntryDefinition>> renderBlocks = new LinkedHashMap<>();
        List<DslBlockDefinition> children = new ArrayList<>();
        this.compileDslStatements(
            document,
            node.body(),
            "DSL block '" + node.name() + "'", id, fromExpansion,
            parameters,
            expansionStack,
            properties,
            renderBlocks,
            children
        );
        return new DslBlockDefinition(node.name(), arguments, id, properties, renderBlocks, children);
    }

    private void compileDslStatements(
        UiDocument document,
        List<Statement> statements,
        String owner,
        String ownerId,
        boolean fromExpansion,
        Map<String, Value> parameters,
        Deque<Key> expansionStack,
        Map<String, Expression<Object>> properties,
        Map<String, List<EntryDefinition>> renderBlocks,
        List<DslBlockDefinition> children
    ) throws UiCompileException {
        for (Statement statement : statements) {
            UiCompilationContext.Checkpoint checkpoint =
                this.context.compilation().state().checkpoint();
            Map<String, Expression<Object>> propertySnapshot = new LinkedHashMap<>(properties);
            Map<String, List<EntryDefinition>> renderBlockSnapshot =
                new LinkedHashMap<>(renderBlocks);
            int childCount = children.size();
            try {
                if (statement instanceof Property property) {
                    if (property.name().equals("id")) {
                        continue;
                    }
                    if (properties.containsKey(property.name())) {
                        throw this.context.elements().error(document, property.position(),
                            "Duplicate property '" + property.name() + "'");
                    }
                    Value resolved = this.context.blueprints().substituteValue(
                        document, property.value(), parameters);
                    properties.put(property.name(), this.context.elements().expressions().any(
                        document, resolved, false, 0
                    ));
                    var originalSource = this.context.blueprints().sourceOf(document, resolved);
                    boolean editable = originalSource.equals(this.context.compilation().document().source())
                        && (resolved instanceof UiDocument.NumberValue
                            || resolved instanceof UiDocument.StringValue
                            || resolved instanceof UiDocument.BooleanValue)
                        && (!fromExpansion || this.context.blueprints().isInstanceArgument(resolved));
                    boolean branchesEditable = originalSource.equals(this.context.compilation().document().source())
                        && resolved instanceof UiDocument.ConditionalValue
                        && (!fromExpansion || this.context.blueprints().isInstanceArgument(resolved));
                    this.context.compilation().state().locateProperty(
                        originalSource, resolved.position(), ownerId, property.name(), editable, branchesEditable
                    );
                } else if (statement instanceof Node block) {
                    if (isRenderableBlock(block.name())) {
                        if (renderBlocks.containsKey(block.name())) {
                            throw this.context.elements().error(
                                document, block.position(), "Duplicate block '" + block.name() + "'"
                            );
                        }
                        renderBlocks.put(
                            block.name(),
                            this.context.compileVisual(
                                document, block, parameters, expansionStack
                            )
                        );
                    } else {
                        children.add(this.compileDslBlock(
                            document, block, parameters, expansionStack, fromExpansion
                        ));
                    }
                } else if (statement instanceof BlueprintUse use) {
                    this.context.blueprints().expand(
                        document,
                        use,
                        parameters,
                        expansionStack,
                        (blueprintDocument, body, bindings, stack) -> {
                            this.compileDslStatements(
                                blueprintDocument,
                                body,
                                owner,
                                ownerId,
                                true,
                                bindings,
                                stack,
                                properties,
                                renderBlocks,
                                children
                            );
                            return null;
                        }
                    );
                } else if (statement instanceof ContentInsertion insertion) {
                    this.compileDslStatements(
                        this.context.blueprints().insertionDocument(document, insertion),
                        insertion.body(),
                        owner,
                        ownerId,
                        fromExpansion,
                        Map.of(),
                        expansionStack,
                        properties,
                        renderBlocks,
                        children
                    );
                } else {
                    throw this.context.elements().error(document, statement.position(),
                        "Only properties, child blocks and blueprint instances are valid inside " + owner);
                }
            } catch (UiCompileException exception) {
                this.context.compilation().state().rollback(checkpoint);
                properties.clear();
                properties.putAll(propertySnapshot);
                renderBlocks.clear();
                renderBlocks.putAll(renderBlockSnapshot);
                while (children.size() > childCount) {
                    children.removeLast();
                }
                this.context.compilation().diagnostics().add(exception);
            } catch (RuntimeException exception) {
                this.context.compilation().state().rollback(checkpoint);
                properties.clear();
                properties.putAll(propertySnapshot);
                renderBlocks.clear();
                renderBlocks.putAll(renderBlockSnapshot);
                while (children.size() > childCount) {
                    children.removeLast();
                }
                this.context.compilation().diagnostics().add(
                    this.context.unexpected(document, statement.position(), exception)
                );
            }
        }
    }

    private static boolean isRenderableBlock(String name) {
        return name.equals("renderables") || name.equals("background");
    }

}

