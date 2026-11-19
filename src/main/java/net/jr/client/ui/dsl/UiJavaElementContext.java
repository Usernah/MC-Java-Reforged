package net.jr.client.ui.dsl;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import net.jr.client.ui.dsl.UiBlueprintCompiler.Key;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Value;

final class UiJavaElementContext {
    private final UiElementContext elements;
    private final UiCompilationContext compilation;
    private final UiBlueprintCompiler blueprints;
    private final VisualEntries visualEntries;

    UiJavaElementContext(
        UiCompilationContext compilation,
        UiElementContext elements,
        UiBlueprintCompiler blueprints,
        VisualEntries visualEntries
    ) {
        this.compilation = compilation;
        this.elements = elements;
        this.blueprints = blueprints;
        this.visualEntries = visualEntries;
    }

    UiElementContext elements() {
        return this.elements;
    }

    UiBlueprintCompiler blueprints() {
        return this.blueprints;
    }

    UiCompilationContext compilation() {
        return this.compilation;
    }

    UiCompileException unexpected(
        UiDocument document,
        UiDocument.Position position,
        RuntimeException exception
    ) {
        String detail = exception.getMessage();
        return this.compilation.error(
            document,
            position,
            "Could not compile this declaration: "
                + (detail == null ? exception.getClass().getSimpleName() : detail)
        );
    }

    List<EntryDefinition> compileVisual(
        UiDocument document,
        Node block,
        Map<String, Value> parameters,
        Deque<Key> stack
    ) throws UiCompileException {
        return this.visualEntries.compile(document, block, parameters, stack);
    }

    @FunctionalInterface
    interface VisualEntries {
        List<EntryDefinition> compile(
            UiDocument document,
            Node block,
            Map<String, Value> parameters,
            Deque<Key> stack
        ) throws UiCompileException;
    }
}
