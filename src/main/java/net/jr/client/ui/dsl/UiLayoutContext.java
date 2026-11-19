package net.jr.client.ui.dsl;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import net.jr.client.ui.dsl.UiBlueprintCompiler.Key;
import net.jr.client.ui.dsl.UiCompiledDocument.CanvasDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.ConditionalEntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.LoadingBarDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.RenderableDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.Statement;
import net.jr.client.ui.dsl.UiDocument.Value;

final class UiLayoutContext {
    private final UiElementContext elements;
    private final Children children;

    UiLayoutContext(UiElementContext elements, Children children) {
        this.elements = elements;
        this.children = children;
    }

    UiElementContext elements() {
        return this.elements;
    }

    List<EntryDefinition> compile(
        UiDocument document,
        List<Statement> statements,
        boolean allowThis,
        Map<String, Value> parameters,
        Deque<Key> stack
    ) throws UiCompileException {
        return this.children.compile(document, statements, allowThis, parameters, stack);
    }

    void requireVisual(UiDocument document, Node block, List<EntryDefinition> entries)
        throws UiCompileException {
        for (EntryDefinition entry : entries) {
            if (!(entry instanceof RenderableDefinition)
                && !(entry instanceof CanvasDefinition)
                && !(entry instanceof LoadingBarDefinition)
                && !(entry instanceof ConditionalEntryDefinition)) {
                throw this.elements.error(document, block.position(),
                    "Block '" + block.name() + "' can only contain visual elements");
            }
        }
    }

    void report(UiCompileException diagnostic) {
        this.elements.report(diagnostic);
    }

    @FunctionalInterface
    interface Children {
        List<EntryDefinition> compile(
            UiDocument document,
            List<Statement> statements,
            boolean allowThis,
            Map<String, Value> parameters,
            Deque<Key> stack
        ) throws UiCompileException;
    }
}
