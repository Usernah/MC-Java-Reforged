package net.jr.client.ui.dsl;

import java.util.LinkedHashMap;
import java.util.Map;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiFileType;
import net.jr.client.ui.dsl.UiCompiledDocument.Expression;
import net.jr.client.ui.dsl.UiDocument.DefinitionDirective;
import net.jr.client.ui.dsl.UiDocument.ImportDirective;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.Statement;
import net.jr.client.ui.dsl.UiDocument.Value;

final class UiDataCompiler {
    private final UiCompilationContext context;
    private final UiExpressionCompiler expressions;
    private final UiReferenceResolver references;

    UiDataCompiler(Map<Asset, UiDocument> documents, Map<String, UiDocument> styles) {
        this.context = new UiCompilationContext(documents, styles);
        this.references = new UiReferenceResolver(this.context);
        this.expressions = new UiExpressionCompiler(this.context, this.references);
    }

    UiDataCompilationResult compile(UiDocument document) {
        this.context.begin(document);
        Map<String, Expression<Object>> compiled = new LinkedHashMap<>();
        Map<String, UiCompiledDocument.SourceLocation> locations = new LinkedHashMap<>();
        try {
            if (document.type() != UiFileType.DATA) {
                throw this.context.error(
                    document, new UiDocument.Position(1, 1), "Expected a UI data document"
                );
            }
            for (Statement statement : document.statements()) {
                if (statement instanceof DefinitionDirective) {
                    continue;
                }
                if (statement instanceof ImportDirective imported) {
                    this.validateImport(document, imported);
                    continue;
                }
                if (statement instanceof Property property) {
                    if (compiled.containsKey(property.name())) {
                        throw this.context.error(
                            document, property.position(),
                            "Duplicate UI data ID '" + property.name() + "'"
                        );
                    }
                    compiled.put(
                        property.name(),
                        this.expressions.any(document, property.value(), false, 0)
                    );
                    SourceValue inspected=this.resolveSourceValue(document,property.value());
                    boolean direct = isDirectLiteral(inspected.value());
                    locations.put(property.name(), new UiCompiledDocument.SourceLocation(
                        inspected.document().source(), inspected.value().position().line(),
                        inspected.value().position().column(), direct,
                        direct || hasEditableConditionalBranches(inspected.value())
                    ));
                    continue;
                }
                throw this.context.error(
                    document,
                    statement.position(),
                    "UI data files only support values, definitions and Asset imports"
                );
            }
        } catch (UiCompileException exception) {
            this.context.diagnostics().add(exception);
        } catch (RuntimeException exception) {
            this.context.diagnostics().add(this.context.error(
                document,
                new UiDocument.Position(1, 1),
                "Internal UI data compilation error: "
                    + (exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage())
            ));
        }

        if (!this.context.diagnostics().isEmpty()) {
            return new UiDataCompilationResult(
                UiCompiledData.empty(document.source()), this.context.diagnostics()
            );
        }
        return new UiDataCompilationResult(
            new UiCompiledData(document.source(), compiled, locations),
            this.context.diagnostics()
        );
    }

    private static boolean isDirectLiteral(Value value) {
        return value instanceof UiDocument.StringValue
            || value instanceof UiDocument.NumberValue
            || value instanceof UiDocument.BooleanValue
            || value instanceof UiDocument.NullValue
            || value instanceof UiDocument.ColorValue
            || value instanceof UiDocument.BareValue;
    }

    private static boolean hasEditableConditionalBranches(Value value) {
        if (!(value instanceof UiDocument.ConditionalValue conditional)) return false;
        return isDirectLiteral(conditional.whenTrue()) || isDirectLiteral(conditional.whenFalse())
            || hasEditableConditionalBranches(conditional.whenTrue())
            || hasEditableConditionalBranches(conditional.whenFalse());
    }

    private SourceValue resolveSourceValue(UiDocument document,Value value) throws UiCompileException {
        UiDocument currentDocument=document;
        Value current=value;
        for (int depth=0;depth<64;depth++) {
            UiReferenceResolver.Resolved resolved=this.references.resolve(currentDocument,current,depth);
            if (resolved==null || resolved.value()==null) return new SourceValue(currentDocument,current);
            currentDocument=resolved.document();
            current=resolved.value();
        }
        throw this.context.error(currentDocument,current.position(),"Definition/reference chain is too deep");
    }

    private record SourceValue(UiDocument document,Value value) { }

    private void validateImport(UiDocument document, ImportDirective imported)
        throws UiCompileException {
        UiFileType type = UiFileType.fromImportPath(imported.target().path()).orElse(null);
        this.context.profile().require(
            type == null
                ? UiLanguageCapability.ASSET_IMPORTS
                : UiLanguageCapability.DOCUMENT_IMPORTS,
            document,
            imported.position()
        );
    }
}
