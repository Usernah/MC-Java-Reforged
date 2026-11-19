package net.jr.client.ui.dsl;

import java.util.List;
record UiDataCompilationResult(
    UiCompiledData data,
    List<UiCompileException> diagnostics
) {
    UiDataCompilationResult {
        diagnostics = List.copyOf(diagnostics);
    }
}
