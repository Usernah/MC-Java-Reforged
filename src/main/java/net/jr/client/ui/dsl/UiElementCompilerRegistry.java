package net.jr.client.ui.dsl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Deque;
import net.jr.client.ui.dsl.UiBlueprintCompiler.Key;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.RenderableDefinition;
import net.jr.client.ui.dsl.UiDocument.Node;

final class UiElementCompilerRegistry {
    private final UiElementContext context;
    private final Map<String, UiElementCompiler> compilers = new LinkedHashMap<>();
    private final Map<String, UiLayoutCompiler> layoutCompilers = new LinkedHashMap<>();

    UiElementCompilerRegistry(UiElementContext context) {
        this.context = context;
        UiTextCompiler text = new UiTextCompiler(context);
        UiTextBoxCompiler textBox = new UiTextBoxCompiler(context, text);
        this.register(new UiImageCompiler(context));
        this.register(new UiVideoCompiler(context));
        this.register(new UiRectangleCompiler(context));
        this.register(text);
        this.register(textBox);
        this.registerLayout(new UiGridCompiler());
        this.registerLayout(new UiCanvasCompiler());
        this.registerLayout(new UiLoadingBarCompiler());
    }

    boolean hasLayout(String name) {
        return this.layoutCompilers.containsKey(name);
    }

    EntryDefinition compileLayout(
        UiLayoutContext layout,
        UiDocument document,
        Node node,
        Deque<Key> stack,
        boolean allowThis
    ) throws UiCompileException {
        UiLayoutCompiler compiler = this.layoutCompilers.get(node.name());
        if (compiler == null) {
            throw this.context.error(document, node.position(),
                "Unsupported layout element type '" + node.name() + "'");
        }
        return compiler.compile(layout, document, node, stack, allowThis);
    }

    RenderableDefinition compile(UiDocument document, Node node, boolean allowThis)
        throws UiCompileException {
        UiElementCompiler compiler = this.compilers.get(node.name());
        if (compiler == null) {
            throw this.context.error(document, node.position(),
                "Unsupported renderable element type '" + node.name() + "'");
        }
        return compiler.compile(document, node, allowThis);
    }

    private void register(UiElementCompiler compiler) {
        UiElementCompiler previous = this.compilers.putIfAbsent(compiler.name(), compiler);
        if (previous != null) {
            throw new IllegalArgumentException(
                "DSL element compiler '" + compiler.name() + "' is already registered"
            );
        }
    }

    private void registerLayout(UiLayoutCompiler compiler) {
        UiLayoutCompiler previous = this.layoutCompilers.putIfAbsent(compiler.name(), compiler);
        if (previous != null) {
            throw new IllegalArgumentException(
                "DSL layout compiler '" + compiler.name() + "' is already registered"
            );
        }
    }

}
