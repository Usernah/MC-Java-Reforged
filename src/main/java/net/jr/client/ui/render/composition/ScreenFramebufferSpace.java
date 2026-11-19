package net.jr.client.ui.render.composition;

import net.jr.api.client.ui.FramebufferScreen;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.minecraft.client.gui.screens.Screen;


public final class ScreenFramebufferSpace {
    private ScreenFramebufferSpace() {
    }

    public static boolean isActive(Screen screen) {
        return screen instanceof FramebufferScreen;
    }

    public static UiFramebufferLayout layout(ViewportArea viewport) {
        return UiFramebufferLayout.resolve(viewport);
    }

    public static int width(Screen screen, ViewportArea viewport, int fallback) {
        return isActive(screen) ? layout(viewport).canvasWidth() : fallback;
    }

    public static int height(Screen screen, ViewportArea viewport, int fallback) {
        return isActive(screen) ? layout(viewport).canvasHeight() : fallback;
    }

    public static double windowMouseX(Screen screen, ViewportArea viewport, double windowX) {
        double localGui = viewport.windowMouseToLocalGuiX(windowX)
            * ViewportGuiScale.logicalWidth(viewport) / viewport.guiWidth();
        return isActive(screen)
            ? layout(viewport).toCanvasX(localGui)
            : localGui / ViewportGuiScale.screenScale(viewport);
    }

    public static double windowMouseY(Screen screen, ViewportArea viewport, double windowY) {
        double localGui = viewport.windowMouseToLocalGuiY(windowY)
            * ViewportGuiScale.logicalHeight(viewport) / viewport.guiHeight();
        return isActive(screen)
            ? layout(viewport).toCanvasY(localGui)
            : localGui / ViewportGuiScale.screenScale(viewport);
    }

    public static double windowDeltaX(Screen screen, ViewportArea viewport, double delta) {
        int width = width(screen, viewport, ViewportGuiScale.screenLogicalWidth(viewport));
        return delta * width / viewport.windowWidth();
    }

    public static double windowDeltaY(Screen screen, ViewportArea viewport, double delta) {
        int height = height(screen, viewport, ViewportGuiScale.screenLogicalHeight(viewport));
        return delta * height / viewport.windowHeight();
    }

    public static double pointerMovementScale(Screen screen, ViewportArea viewport) {
        return isActive(screen) ? 1.0D / layout(viewport).scale() : 1.0D;
    }
}
