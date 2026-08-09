package net.jr.client.ui.dsl;

import net.jr.api.client.resource.Asset;

public final class UiParseException extends Exception {
    private final Asset source;
    private final int line;
    private final int column;
    private final String detail;

    public UiParseException(Asset source, int line, int column, String message) {
        super(message + " at " + source + ":" + line + ":" + column);
        this.source = source;
        this.line = line;
        this.column = column;
        this.detail = message;
    }

    public Asset source() {
        return this.source;
    }

    public int line() {
        return this.line;
    }

    public int column() {
        return this.column;
    }

    public String detail() {
        return this.detail;
    }
}
