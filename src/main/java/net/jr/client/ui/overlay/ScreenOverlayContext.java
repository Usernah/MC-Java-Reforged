package net.jr.client.ui.overlay;

import net.minecraft.client.gui.screens.Screen;

public final class ScreenOverlayContext {
    private final Screen ownerScreen;
    private final ScreenOverlayManager manager;

    ScreenOverlayContext(Screen ownerScreen, ScreenOverlayManager manager) {
        this.ownerScreen = ownerScreen;
        this.manager = manager;
    }

    public Screen ownerScreen() {
        return this.ownerScreen;
    }

    public void close(ScreenOverlayCloseReason reason) {
        this.manager.close(reason);
    }
}
