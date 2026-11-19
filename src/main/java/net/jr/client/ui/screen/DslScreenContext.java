package net.jr.client.ui.screen;

import net.jr.api.client.ui.UiScreenFile;
import net.jr.client.ui.dsl.DslUiContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;


public final class DslScreenContext extends DslUiContext<UiScreenFile> {
    public DslScreenContext(Screen screen, UiScreenFile file) {
        super(screen, file);
        if (screen instanceof AbstractContainerScreen<?>) {
            throw new IllegalArgumentException("A container cannot own a DslScreenContext");
        }
    }
}
