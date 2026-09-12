package net.jr.client.runtime.viewport;

import javax.annotation.Nullable;

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
}
