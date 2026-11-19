package net.jr.client.ui.dsl;

import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiFileType;
import net.jr.client.ui.dsl.UiDocument.ArithmeticNegationValue;
import net.jr.client.ui.dsl.UiDocument.ArithmeticValue;
import net.jr.client.ui.dsl.UiDocument.AssignmentDirective;
import net.jr.client.ui.dsl.UiDocument.BlueprintDirective;
import net.jr.client.ui.dsl.UiDocument.BlueprintContent;
import net.jr.client.ui.dsl.UiDocument.BlueprintMemberUse;
import net.jr.client.ui.dsl.UiDocument.BlueprintParameterValue;
import net.jr.client.ui.dsl.UiDocument.BlueprintUse;
import net.jr.client.ui.dsl.UiDocument.CallValue;
import net.jr.client.ui.dsl.UiDocument.ConditionalValue;
import net.jr.client.ui.dsl.UiDocument.ConditionalStatement;
import net.jr.client.ui.dsl.UiDocument.ContentInsertion;
import net.jr.client.ui.dsl.UiDocument.ComparisonValue;
import net.jr.client.ui.dsl.UiDocument.ImportDirective;
import net.jr.client.ui.dsl.UiDocument.ListValue;
import net.jr.client.ui.dsl.UiDocument.LogicalValue;
import net.jr.client.ui.dsl.UiDocument.NegatedValue;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.ObjectValue;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.Statement;
import net.jr.client.ui.dsl.UiDocument.TupleValue;
import net.jr.client.ui.dsl.UiDocument.Value;
import net.jr.client.ui.dsl.UiDocument.VariableDirective;

import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class UiBlueprintCompiler {
    private static final int MAX_DEPTH = 64;
    private final UiCompilationContext context;
    private final UiReferenceResolver references;
    private final Map<Value, ValueOrigin> valueSources = new IdentityHashMap<>();

    void resetSources() {
        this.valueSources.clear();
    }

    boolean isInstanceArgument(Value value) {
        ValueOrigin origin = this.valueSources.get(value);
        return origin != null && origin.instanceSpecific();
    }

    private void trackArgument(UiDocument caller, Value original, Value argument, boolean directCall) {
        this.valueSources.putIfAbsent(
            argument,
            this.valueSources.getOrDefault(
                original,
                new ValueOrigin(
                    caller.source(),
                    directCall && caller.source().equals(this.context.document().source())
                )
            )
        );
    }

    UiBlueprintCompiler(UiCompilationContext context, UiReferenceResolver references) {
        this.context = context;
        this.references = references;
    }

    <T> T expand(
        UiDocument caller,
        BlueprintUse use,
        Map<String, Value> callerParameters,
        Deque<Key> expansionStack,
        Consumer<T> consumer
    ) throws UiCompileException {
        this.context.profile().require(
            UiLanguageCapability.BLUEPRINT_USAGE, caller, use.position()
        );
        Target target = this.resolveBlueprint(caller, use);
        BlueprintDirective blueprint = target.blueprint();
        Key key = new Key(
            target.document().source(), blueprint.name(), blueprint.position()
        );
        if (expansionStack.contains(key)) {
            throw this.context.error(caller, use.position(),
                "Circular blueprint composition detected for '" + String.join(".", use.path()) + "'");
        }
        if (expansionStack.size() >= MAX_DEPTH) {
            throw this.context.error(caller, use.position(), "Blueprint composition is too deep");
        }

        if (use.arguments().size() != blueprint.parameters().size()) {
            throw this.context.error(caller, use.position(),
                "Blueprint '" + blueprint.name() + "' expects " + blueprint.parameters().size()
                    + " arguments, but received " + use.arguments().size());
        }
        Map<String, Value> bindings = new LinkedHashMap<>();
        for (int index = 0; index < blueprint.parameters().size(); index++) {
            Value argument = this.substituteValue(
                caller, use.arguments().get(index), callerParameters
            );
            this.trackArgument(
                caller, use.arguments().get(index), argument, expansionStack.isEmpty()
            );
            bindings.put(blueprint.parameters().get(index), argument);
        }

        expansionStack.addLast(key);
        try {
            List<BlueprintMemberUse> resolvedMembers = new ArrayList<>();
            for (BlueprintMemberUse member : use.members()) {
                List<Value> resolved = this.substituteValues(
                    caller, member.arguments(), callerParameters
                );
                for (int index = 0; index < resolved.size(); index++) {
                    this.trackArgument(
                        caller, member.arguments().get(index), resolved.get(index),
                        expansionStack.size() == 1
                    );
                }
                resolvedMembers.add(new BlueprintMemberUse(
                    member.name(), resolved, member.position()
                ));
            }
            List<Statement> suppliedContent = this.substituteStatements(
                caller,
                use.content(),
                callerParameters,
                null,
                new ContentTracker()
            );
            this.validateBlueprintContent(caller, suppliedContent);
            CallContent content = new CallContent(caller, suppliedContent);
            ContentTracker contentTracker = new ContentTracker();
            BlueprintScope scope = new BlueprintScope(null, bindings);
            List<Statement> expanded = this.materializeBlueprint(
                caller,
                target.document(),
                blueprint,
                resolvedMembers,
                scope,
                content,
                contentTracker
            );
            if (use.contentProvided() && contentTracker.uses == 0) {
                throw this.context.error(caller, use.position(),
                    "Blueprint '" + blueprint.name() + "' does not declare @content");
            }
            return consumer.consume(
                target.document(), expanded, Map.of(), expansionStack
            );
        } finally {
            expansionStack.removeLast();
        }
    }

    List<Statement> materializeBlueprint(
        UiDocument caller,
        UiDocument blueprintDocument,
        BlueprintDirective blueprint,
        List<BlueprintMemberUse> requestedMembers,
        BlueprintScope scope,
        CallContent content,
        ContentTracker contentTracker
    ) throws UiCompileException {
        Map<String, BlueprintDirective> members = new LinkedHashMap<>();
        for (Statement statement : blueprint.body()) {
            if (statement instanceof BlueprintDirective nested
                && members.putIfAbsent(nested.name(), nested) != null) {
                throw this.context.error(blueprintDocument, nested.position(),
                    "Duplicate nested blueprint '" + nested.name() + "'");
            }
        }

        Map<String, BlueprintMemberUse> requested = new LinkedHashMap<>();
        for (BlueprintMemberUse memberUse : requestedMembers) {
            if (!members.containsKey(memberUse.name())) {
                throw this.context.error(caller, memberUse.position(),
                    "Blueprint '" + blueprint.name() + "' has no builder member '."
                        + memberUse.name() + "(...)'");
            }
            if (requested.putIfAbsent(memberUse.name(), memberUse) != null) {
                throw this.context.error(caller, memberUse.position(),
                    "Builder member '." + memberUse.name() + "(...)' was used more than once");
            }
        }

        List<Statement> result = new ArrayList<>();
        for (Statement statement : blueprint.body()) {
            if (statement instanceof VariableDirective variable) {
                this.context.profile().require(
                    UiLanguageCapability.VARIABLES,
                    blueprintDocument,
                    variable.position()
                );
                Value initialValue = this.substituteValue(
                    blueprintDocument, variable.value(), scope.snapshot()
                );
                scope.declareVariable(blueprintDocument, variable, initialValue);
            } else if (statement instanceof AssignmentDirective assignment) {
                this.context.profile().require(
                    UiLanguageCapability.VARIABLES,
                    blueprintDocument,
                    assignment.position()
                );
                Value value = this.substituteValue(
                    blueprintDocument, assignment.value(), scope.snapshot()
                );
                scope.assign(blueprintDocument, assignment, value);
            } else if (statement instanceof BlueprintDirective nested) {
                BlueprintMemberUse memberUse = requested.get(nested.name());
                if (memberUse == null) {
                    continue;
                }
                if (memberUse.arguments().size() != nested.parameters().size()) {
                    throw this.context.error(caller, memberUse.position(),
                        "Builder member '." + nested.name() + "(...)' expects "
                            + nested.parameters().size() + " arguments, but received "
                            + memberUse.arguments().size());
                }
                Map<String, Value> nestedBindings = new LinkedHashMap<>();
                for (int index = 0; index < nested.parameters().size(); index++) {
                    nestedBindings.put(
                        nested.parameters().get(index),
                        memberUse.arguments().get(index)
                    );
                }
                BlueprintScope nestedScope = new BlueprintScope(scope, nestedBindings);
                result.addAll(this.materializeBlueprint(
                    caller,
                    blueprintDocument,
                    nested,
                    List.of(),
                    nestedScope,
                    content,
                    contentTracker
                ));
            } else {
                result.addAll(this.substituteStatements(
                    blueprintDocument,
                    List.of(statement),
                    scope.snapshot(),
                    content,
                    contentTracker
                ));
            }
        }
        return List.copyOf(result);
    }

    Target resolveBlueprint(UiDocument caller, BlueprintUse use)
        throws UiCompileException {
        List<String> path = use.path();
        if (use.registeredStyle()) {
            this.context.profile().require(
                UiLanguageCapability.STYLE_REFERENCES, caller, use.position()
            );
            if (path.size() != 2) {
                throw this.context.error(caller, use.position(),
                    "A registered style blueprint reference must be '::style_id.blueprint'");
            }
            UiDocument style = this.context.styles().get(path.getFirst());
            if (style == null) {
                throw this.context.error(caller, use.position(),
                    "Unknown registered style '::" + path.getFirst() + "'");
            }
            BlueprintDirective blueprint = style.blueprint(path.get(1)).orElseThrow(() ->
                this.context.error(caller, use.position(),
                    "Registered style '::" + path.getFirst() + "' has no blueprint '"
                        + path.get(1) + "'")
            );
            return new Target(style, blueprint);
        }
        if (path.size() == 1) {
            BlueprintDirective blueprint = caller.blueprint(path.getFirst()).orElseThrow(() ->
                this.context.error(caller, use.position(), "Unknown blueprint '@" + path.getFirst() + "'")
            );
            return new Target(caller, blueprint);
        }
        if (path.size() != 2) {
            throw this.context.error(caller, use.position(),
                "A blueprint reference must be '@name' or '@alias.name'");
        }

        ImportDirective imported = caller.imports().stream()
            .filter(candidate -> candidate.alias().equals(path.getFirst()))
            .findFirst()
            .orElseThrow(() -> this.context.error(caller, use.position(),
                "Unknown import alias '@" + path.getFirst() + "'"));
        UiFileType importedType = UiFileType.fromImportPath(imported.target().path()).orElse(null);
        if (importedType == null) {
            throw this.context.error(caller, use.position(),
                "Raw Assets do not contain blueprints");
        }
        if (importedType == UiFileType.STYLE) {
            throw this.context.error(caller, use.position(),
                "Style documents cannot be imported; register the style and use '::style_id.blueprint'");
        }
        Asset importedAsset = this.references.importedAsset(caller.source(), imported);
        UiDocument importedDocument = this.context.documents().get(importedAsset);
        if (importedDocument == null) {
            throw this.context.error(caller, use.position(),
                "Imported UI document is not loaded: " + importedAsset);
        }
        BlueprintDirective blueprint = importedDocument.blueprint(path.get(1)).orElseThrow(() ->
            this.context.error(caller, use.position(),
                "Imported blueprint '" + path.get(1) + "' does not exist in " + importedAsset)
        );
        return new Target(importedDocument, blueprint);
    }

    UiDocument insertionDocument(
        UiDocument current,
        ContentInsertion insertion
    ) throws UiCompileException {
        if (current.source().equals(insertion.source())) {
            return current;
        }
        UiDocument sourceDocument = this.context.documents().get(insertion.source());
        if (sourceDocument == null) {
            throw this.context.error(current, insertion.position(),
                "Blueprint content source is not loaded: " + insertion.source());
        }
        return sourceDocument;
    }

    void validateBlueprintContent(
        UiDocument document,
        List<Statement> statements
    ) throws UiCompileException {
        for (Statement statement : statements) {
            if (statement instanceof Node || statement instanceof BlueprintUse
                || statement instanceof ContentInsertion) {
                continue;
            }
            if (statement instanceof ConditionalStatement conditional) {
                this.validateBlueprintContent(document, conditional.body());
                continue;
            }
            throw this.context.error(document, statement.position(),
                "Blueprint content only accepts elements, conditionals and blueprint instances");
        }
    }

    Node substituteNode(
        UiDocument document,
        Node node,
        Map<String, Value> parameters
    ) throws UiCompileException {
        List<Value> arguments = new ArrayList<>();
        for (Value argument : node.arguments()) {
            arguments.add(this.substituteValue(document, argument, parameters));
        }
        List<Statement> body = this.substituteStatements(
            document, node.body(), parameters, null, new ContentTracker()
        );
        return new Node(node.name(), arguments, body, node.position());
    }

    List<Statement> substituteStatements(
        UiDocument document,
        List<Statement> statements,
        Map<String, Value> parameters,
        CallContent content,
        ContentTracker contentTracker
    ) throws UiCompileException {
        List<Statement> result = new ArrayList<>();
        for (Statement statement : statements) {
            result.addAll(this.substituteStatement(
                document, statement, parameters, content, contentTracker
            ));
        }
        return List.copyOf(result);
    }

    List<Statement> substituteStatement(
        UiDocument document,
        Statement statement,
        Map<String, Value> parameters,
        CallContent content,
        ContentTracker contentTracker
    ) throws UiCompileException {
        if (statement instanceof BlueprintContent insertionPoint) {
            this.context.profile().require(
                UiLanguageCapability.CONTENT_INSERTION,
                document,
                insertionPoint.position()
            );
            if (content == null) {
                throw this.context.error(document, insertionPoint.position(),
                    "@content can only be expanded by a blueprint call");
            }
            contentTracker.uses++;
            return List.of(new ContentInsertion(
                content.document().source(), content.statements(), insertionPoint.position()
            ));
        }
        if (statement instanceof ContentInsertion insertion) {
            return List.of(insertion);
        }
        if (statement instanceof Property property) {
            return List.of(new Property(
                property.name(),
                this.substituteValue(document, property.value(), parameters),
                property.position()
            ));
        }
        if (statement instanceof Node node) {
            List<Value> arguments = this.substituteValues(
                document, node.arguments(), parameters
            );
            List<Statement> body = this.substituteStatements(
                document, node.body(), parameters, content, contentTracker
            );
            return List.of(new Node(node.name(), arguments, body, node.position()));
        }
        if (statement instanceof ConditionalStatement conditional) {
            List<Statement> body = this.substituteStatements(
                document, conditional.body(), parameters, content, contentTracker
            );
            return List.of(new ConditionalStatement(
                this.substituteValue(document, conditional.condition(), parameters),
                body,
                conditional.position()
            ));
        }
        if (statement instanceof BlueprintUse use) {
            List<BlueprintMemberUse> members = new ArrayList<>();
            for (BlueprintMemberUse member : use.members()) {
                members.add(new BlueprintMemberUse(
                    member.name(),
                    this.substituteValues(document, member.arguments(), parameters),
                    member.position()
                ));
            }
            List<Statement> nestedContent = this.substituteStatements(
                document, use.content(), parameters, content, contentTracker
            );
            return List.of(new BlueprintUse(
                use.path(),
                this.substituteValues(document, use.arguments(), parameters),
                members,
                nestedContent,
                use.contentProvided(),
                use.registeredStyle(),
                use.position()
            ));
        }
        throw this.context.error(document, statement.position(),
            "This directive cannot be emitted as blueprint content");
    }

    Value substituteValue(UiDocument document, Value value, Map<String, Value> parameters)
        throws UiCompileException {
        if (value instanceof BlueprintParameterValue parameter) {
            Value bound = parameters.get(parameter.path().getFirst());
            if (bound == null) {
                throw this.context.error(document, parameter.position(),
                    "Unknown blueprint parameter '&" + parameter.path().getFirst() + "'");
            }
            return this.references.descend(document, bound, parameter.path().subList(1, parameter.path().size()));
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

    Asset sourceOf(UiDocument fallback, Value value) {
        ValueOrigin origin = this.valueSources.get(value);
        return origin == null ? fallback.source() : origin.source();
    }

    private record ValueOrigin(Asset source, boolean instanceSpecific) {
    }

    List<Value> substituteValues(
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

    record Target(UiDocument document, BlueprintDirective blueprint) {
    }

    record Key(Asset source, String name, UiDocument.Position position) {
    }

    record CallContent(UiDocument document, List<Statement> statements) {
        CallContent {
            statements = List.copyOf(statements);
        }
    }

    static final class ContentTracker {
        private int uses;
    }

    final class BlueprintScope {
        private final BlueprintScope parent;
        private final Map<String, Value> parameters;
        private final Map<String, Value> variables = new LinkedHashMap<>();

        private BlueprintScope(BlueprintScope parent, Map<String, Value> parameters) {
            this.parent = parent;
            this.parameters = Map.copyOf(parameters);
        }

        private Map<String, Value> snapshot() {
            Map<String, Value> result = this.parent == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(this.parent.snapshot());
            result.putAll(this.parameters);
            result.putAll(this.variables);
            return result;
        }

        private void declareVariable(
            UiDocument document,
            VariableDirective declaration,
            Value value
        ) throws UiCompileException {
            if (this.parameters.containsKey(declaration.name())
                || this.variables.putIfAbsent(declaration.name(), value) != null) {
                throw UiBlueprintCompiler.this.context.error(document, declaration.position(),
                    "Blueprint-local symbol '" + declaration.name() + "' already exists");
            }
        }

        private void assign(
            UiDocument document,
            AssignmentDirective assignment,
            Value value
        ) throws UiCompileException {
            if (this.variables.containsKey(assignment.name())) {
                this.variables.put(assignment.name(), value);
                return;
            }
            if (this.parameters.containsKey(assignment.name())) {
                throw UiBlueprintCompiler.this.context.error(document, assignment.position(),
                    "Blueprint parameter '" + assignment.name() + "' is immutable");
            }
            if (this.parent != null) {
                this.parent.assign(document, assignment, value);
                return;
            }
            throw UiBlueprintCompiler.this.context.error(document, assignment.position(),
                "Unknown mutable blueprint variable '" + assignment.name() + "'");
        }
    }

    @FunctionalInterface
    interface Consumer<T> {
        T consume(
            UiDocument document,
            List<Statement> statements,
            Map<String, Value> parameters,
            Deque<Key> expansionStack
        ) throws UiCompileException;
    }

}

