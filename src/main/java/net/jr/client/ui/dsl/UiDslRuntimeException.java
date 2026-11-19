package net.jr.client.ui.dsl;

import net.jr.client.ui.dsl.UiCompiledDocument.SourceLocation;


public final class UiDslRuntimeException extends RuntimeException {
    private final SourceLocation location;

    public UiDslRuntimeException(SourceLocation location, String message, Throwable cause) {
        super(message == null || message.isBlank() ? fallbackMessage(cause) : message, cause);
        this.location = location;
    }

    public SourceLocation location() {
        return this.location;
    }

    private static String fallbackMessage(Throwable cause) {
        return cause == null ? "Unknown DSL runtime error" : cause.getClass().getSimpleName();
    }
}
