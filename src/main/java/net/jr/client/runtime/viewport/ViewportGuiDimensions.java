package net.jr.client.runtime.viewport;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;

public final class ViewportGuiDimensions {
    private ViewportGuiDimensions() {
    }

    @Nullable
    public static Integer activeWidthOrNull() {
        Integer screenWidth = ViewportGuiScale.activeGuiWidthOrNull();
        if (screenWidth != null) {
            return screenWidth;
        }
        ViewportArea viewport = ViewportRenderScope.activeViewportOrNull();
        return viewport != null ? viewport.guiWidth() : null;
    }

    @Nullable
    public static Integer activeHeightOrNull() {
        Integer screenHeight = ViewportGuiScale.activeGuiHeightOrNull();
        if (screenHeight != null) {
            return screenHeight;
        }
        ViewportArea viewport = ViewportRenderScope.activeViewportOrNull();
        return viewport != null ? viewport.guiHeight() : null;
    }

    public static boolean suppressFrameGlobalBlur() {
        return ClientRuntime.INSTANCE.viewports().presentedCount() > 1;
    }
}
