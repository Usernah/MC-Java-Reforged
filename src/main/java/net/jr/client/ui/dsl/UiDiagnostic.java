package net.jr.client.ui.dsl;

import net.jr.api.client.resource.Asset;

import java.util.Objects;

public record UiDiagnostic(Severity severity, Asset source, int line, int column, String message) {
    public UiDiagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(message, "message");
    }

    public enum Severity {
        WARNING,
        ERROR
    }

    public String formatted() {
        String position = this.line > 0 && this.column > 0
            ? ":" + this.line + ":" + this.column
            : "";
        return this.source + position + " - " + this.message;
    }
}
