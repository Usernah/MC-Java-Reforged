package net.jr.client.ui.container;

import net.jr.api.client.ui.UiContainerFile;

public interface DslContainerBridge {
    DslContainerContext javaReforged$dslContainerContext(UiContainerFile file);

    void javaReforged$closeDslContainerContext();

    /** Peek without instantiating a container context. */
    DslContainerContext javaReforged$peekDslContainerContext();
}
