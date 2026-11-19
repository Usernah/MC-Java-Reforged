package net.jr.client.compat;

import com.mojang.blaze3d.platform.Window;
import net.jr.api.client.ui.Viewport;
import net.jr.client.ui.render.composition.GuiCompositionScope;

/** Resolves HUD metrics from the active composition canvas, not the physical window. */
public final class CompatHudDimensions {
    private CompatHudDimensions() {
    }

    public static int width(Window window) {
        GuiCompositionScope.Extraction extraction = GuiCompositionScope.activeExtractionOrNull();
        return extraction != null ? extraction.width() : Viewport.current().logicalGuiWidth();
    }

    public static int height(Window window) {
        GuiCompositionScope.Extraction extraction = GuiCompositionScope.activeExtractionOrNull();
        return extraction != null ? extraction.height() : Viewport.current().logicalGuiHeight();
    }

    public static int rasterScale(Window window) {
        GuiCompositionScope.Extraction extraction = GuiCompositionScope.activeExtractionOrNull();
        return extraction != null ? extraction.rasterScale() : Viewport.current().guiScale();
    }

    public static int framebufferWidth(Window window) {
        GuiCompositionScope.Extraction extraction = GuiCompositionScope.activeExtractionOrNull();
        return extraction != null
            ? extraction.width() * extraction.rasterScale()
            : Viewport.current().width();
    }

    public static int framebufferHeight(Window window) {
        GuiCompositionScope.Extraction extraction = GuiCompositionScope.activeExtractionOrNull();
        return extraction != null
            ? extraction.height() * extraction.rasterScale()
            : Viewport.current().height();
    }
}
