package net.jr.client.ui.dsl;

import java.util.List;

record UiCompilationResult(
    UiCompiledDocument document,
    List<UiCompileException> diagnostics
) {
    UiCompilationResult {
        diagnostics = List.copyOf(diagnostics);
    }
}
