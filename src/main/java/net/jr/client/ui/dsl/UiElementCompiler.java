package net.jr.client.ui.dsl;

import net.jr.client.ui.dsl.UiCompiledDocument.RenderableDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;

interface UiElementCompiler {
    String name();

    RenderableDefinition compile(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException;
}
