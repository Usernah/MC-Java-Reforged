package net.jr.api.client.ui;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;

public final class Viewport {
    private Viewport() {}

    public static int width() {
        return width(LocalClientScope.currentClient().slotId());
    }

    public static int height() {
        return height(LocalClientScope.currentClient().slotId());
    }

    public static int width(int slotId) {
        ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewport(slotId);
        return ViewportGuiScale.logicalWidth(viewport);
    }

    public static int height(int slotId) {
        ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewport(slotId);
        return ViewportGuiScale.logicalHeight(viewport);
    }

    public static int windowWidth() {
        return ClientRuntime.INSTANCE.viewports().windowMetrics().guiWidth();
    }

    public static int windowHeight() {
        return ClientRuntime.INSTANCE.viewports().windowMetrics().guiHeight();
    }

    public static int WindowWidth() {
        return ClientRuntime.INSTANCE.viewports().windowMetrics().framebufferWidth();
    }

    public static int WindowHeight() {
        return ClientRuntime.INSTANCE.viewports().windowMetrics().framebufferHeight();
    }

    public static int pixelWidth() {
        return pixelWidth(LocalClientScope.currentClient().slotId());
    }

    public static int pixelHeight() {
        return pixelHeight(LocalClientScope.currentClient().slotId());
    }

    public static int pixelWidth(int slotId) {
        return ClientRuntime.INSTANCE.viewports().viewport(slotId).width();
    }

    public static int pixelHeight(int slotId) {
        return ClientRuntime.INSTANCE.viewports().viewport(slotId).height();
    }

    public static int windowPixelWidth() {
        return ClientRuntime.INSTANCE.viewports().windowMetrics().framebufferWidth();
    }

    public static int windowPixelHeight() {
        return ClientRuntime.INSTANCE.viewports().windowMetrics().framebufferHeight();
    }
}
