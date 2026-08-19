package net.jr.client.ui.dsl;

import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.dsl.UiDocument;

final class UiCompileException extends Exception {
    private final Asset source;
    private final UiDocument.Position position;

    UiCompileException(Asset source, UiDocument.Position position, String message) {
        super(message);
        this.source = source;
        this.position = position;
    }

    Asset source() {
        return this.source;
    }

    UiDocument.Position position() {
        return this.position;
    }
}
