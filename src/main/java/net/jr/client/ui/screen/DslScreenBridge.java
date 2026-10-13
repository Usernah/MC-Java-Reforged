package net.jr.client.ui.screen;

import net.jr.api.client.ui.UiScreenFile;

public interface DslScreenBridge {
    DslScreenContext javaReforged$dslContext(UiScreenFile file);

    void javaReforged$closeDslContext();
}
