package net.jr.client.ui.dsl;

import java.util.Deque;
import net.jr.client.ui.dsl.UiBlueprintCompiler.Key;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;

interface UiLayoutCompiler {
    String name();

    EntryDefinition compile(
        UiLayoutContext context,
        UiDocument document,
        Node node,
        Deque<Key> stack,
        boolean allowThis
    ) throws UiCompileException;
}
