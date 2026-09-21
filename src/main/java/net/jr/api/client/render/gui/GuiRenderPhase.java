package net.jr.api.client.render.gui;

/** Ordered stages of GUI extraction. A stage may have a local or window scope. */
public enum GuiRenderPhase {
    HUD,
    OVERLAY,
    VANILLA_OVERLAY,
    SCREEN,
    SCREEN_OVERLAY,
    TOAST,
    GLOBAL
}
