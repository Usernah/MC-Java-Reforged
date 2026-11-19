package net.jr.client.ui.dsl;

import java.util.List;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiDocument.ElementValue;
import net.jr.client.ui.dsl.UiDocument.ExposedDataValue;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.Statement;
import net.jr.client.ui.dsl.UiDocument.StringValue;

final class UiElementContext {
    private final UiCompilationContext compilation;
    private final UiReferenceResolver references;
    private final UiExpressionCompiler expressions;

    UiElementContext(
        UiCompilationContext compilation,
        UiReferenceResolver references,
        UiExpressionCompiler expressions
    ) {
        this.compilation = compilation;
        this.references = references;
        this.expressions = expressions;
    }

    UiNodeBody body(
        UiDocument document,
        Node node,
        List<String> properties,
        List<String> children
    ) {
        return UiNodeBody.read(this.compilation, document, node, properties, children);
    }

    String id(UiDocument document, Node node) throws UiCompileException {
        if (!node.arguments().isEmpty()) {
            throw this.error(document, node.position(),
                "The '" + node.name() + "' declaration does not accept positional ids or arguments; "
                    + "use id: \"...\" when Java needs an id");
        }
        Property property = this.idProperty(document, node);
        String id = property == null
            ? this.compilation.state().nextId(node.name())
            : this.explicitId(document, property);
        this.compilation.state().register(document, node.position(), id);
        return id;
    }

    String blockId(UiDocument document, Node node) throws UiCompileException {
        Property property = this.idProperty(document, node);
        String id = property == null
            ? this.compilation.state().nextId(node.name())
            : this.explicitId(document, property);
        this.compilation.state().locate(document, node.position(), id);
        return id;
    }

    UiExpressionCompiler expressions() {
        return this.expressions;
    }

    UiReferenceResolver references() {
        return this.references;
    }

    Expression<Object> element(UiDocument document, ElementValue element)
        throws UiCompileException {
        this.compilation.profile().require(
            UiLanguageCapability.ELEMENT_REFERENCES, document, element.position()
        );
        this.compilation.state().reference(document, element);
        return context -> context.resolveElement(element.id(), element.property());
    }

    void validateData(UiDocument document, ExposedDataValue data, boolean allowThis)
        throws UiCompileException {
        this.compilation.profile().require(
            UiLanguageCapability.EXPOSED_DATA, document, data.position()
        );
        if (!allowThis && data.path().getFirst().equals("this")) {
            throw this.error(document, data.position(),
                "${this.*} is only valid inside a widget's renderables");
        }
    }

    <T> T optional(UiDocument document, Node node, Section<T> section) {
        if (node == null) {
            return null;
        }
        UiCompilationContext.Checkpoint checkpoint = this.compilation.state().checkpoint();
        try {
            return section.compile();
        } catch (UiCompileException exception) {
            this.compilation.state().rollback(checkpoint);
            this.compilation.diagnostics().add(exception);
        } catch (RuntimeException exception) {
            this.compilation.state().rollback(checkpoint);
            String detail = exception.getMessage();
            this.compilation.diagnostics().add(this.error(
                document,
                node.position(),
                "Could not compile this declaration: "
                    + (detail == null ? exception.getClass().getSimpleName() : detail)
            ));
        }
        return null;
    }

    UiCompileException error(UiDocument document, UiDocument.Position position, String message) {
        return this.compilation.error(document, position, message);
    }

    void report(UiCompileException diagnostic) {
        this.compilation.diagnostics().add(diagnostic);
    }

    private Property idProperty(UiDocument document, Node node) {
        Property found = null;
        for (Statement statement : node.body()) {
            if (statement instanceof Property property && property.name().equals("id")) {
                if (found != null) {
                    this.compilation.diagnostics().add(this.error(
                        document, property.position(), "Duplicate property 'id'"
                    ));
                    continue;
                }
                found = property;
            }
        }
        return found;
    }

    private String explicitId(UiDocument document, Property property) throws UiCompileException {
        if (!(property.value() instanceof StringValue id) || id.value().isBlank()) {
            throw this.error(document, property.position(),
                "Property 'id' must be a non-empty quoted string");
        }
        return id.value();
    }

    @FunctionalInterface
    interface Section<T> {
        T compile() throws UiCompileException;
    }
}
