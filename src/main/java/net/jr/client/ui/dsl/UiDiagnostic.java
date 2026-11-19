package net.jr.client.ui.dsl;

import net.jr.api.client.resource.Asset;

import java.util.Objects;

public record UiDiagnostic(
    Severity severity,
    Asset source,
    int line,
    int column,
    String message,
    String code,
    String solution
) {
    public UiDiagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(message, "message");
        code = code == null ? "" : code.stripTrailing();
        solution = solution == null ? "" : solution.strip();
    }

    public UiDiagnostic(Severity severity, Asset source, int line, int column, String message) {
        this(severity, source, line, column, message, "", suggest(message));
    }

    public enum Severity {
        WARNING,
        ERROR
    }

    public String formatted() {
        String position = this.line > 0 && this.column > 0
            ? ":" + this.line + ":" + this.column
            : "";
        StringBuilder result = new StringBuilder()
            .append(this.source).append(position).append(" - ").append(this.message);
        if (!this.code.isBlank()) {
            result.append(System.lineSeparator())
                .append("  code: ").append(this.line).append(" | ").append(this.code);
            if (this.column > 0) {
                int prefixLength = "  code: ".length()
                    + Integer.toString(this.line).length() + " | ".length();
                result.append(System.lineSeparator())
                    .append(" ".repeat(prefixLength + Math.max(0, this.column - 1)))
                    .append('^');
            }
        }
        if (!this.solution.isBlank()) {
            result.append(System.lineSeparator()).append("  solution: ").append(this.solution);
        }
        return result.toString();
    }

    public static String suggest(String message) {
        if (message == null || message.isBlank()) {
            return "Review the indicated DSL declaration and reload resources; the UI instance was kept alive.";
        }
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("unsupported visual anchor")) {
            return "Use one of: none, top_left, top_center, top_right, center_left, center, "
                + "center_right, bottom_left, bottom_center or bottom_right.";
        }
        if (normalized.contains("does not exist") || normalized.contains("not found")) {
            return "Check the declared id/path and the selected HD/FHD/small variant.";
        }
        if (normalized.contains("missing property")) {
            return "Add the required property at this declaration.";
        }
        if (normalized.contains("not consumed") || normalized.contains("unsupported property")) {
            return "Remove the unsupported value or expose it from the Java element contract.";
        }
        if (normalized.contains("expected")) {
            return "Correct the syntax or value at the indicated line and reload resources.";
        }
        return "Correct the indicated DSL declaration and reload resources; the UI instance was kept alive.";
    }
}
