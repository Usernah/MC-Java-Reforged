package net.jr.client.ui.dsl;

import net.jr.api.client.resource.Asset;

import java.util.Map;

final class UiCompiler {
    private final UiStructureCompiler structure;

    UiCompiler(Map<Asset, UiDocument> documents, Map<String, UiDocument> styles) {
        UiCompilationContext context = new UiCompilationContext(documents, styles);
        UiReferenceResolver references = new UiReferenceResolver(context);
        UiExpressionCompiler expressions = new UiExpressionCompiler(context, references);
        UiBlueprintCompiler blueprints = new UiBlueprintCompiler(context, references);
        UiElementContext elementContext = new UiElementContext(context, references, expressions);
        UiElementCompilerRegistry elements = new UiElementCompilerRegistry(elementContext);
        this.structure = new UiStructureCompiler(
            context, references, expressions, blueprints, elements, elementContext
        );
    }

    UiCompilationResult compile(UiDocument document) {
        return this.structure.compile(document);
    }
}
