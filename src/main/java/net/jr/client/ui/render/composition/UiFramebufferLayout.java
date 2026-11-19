package net.jr.client.ui.render.composition;

import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.client.ui.presentation.UiResolution;


public record UiFramebufferLayout(
    UiResolution resolution,
    int canvasWidth,
    int canvasHeight,
    int rasterScale,
    float scale,
    float sampleFootprint
) {
    public static UiFramebufferLayout resolve(ViewportArea viewport) {
        return resolve(viewport, UiPresentation.framebufferScale().value());
    }

    public static UiFramebufferLayout resolve(
        ViewportArea viewport,
        float baseScale
    ) {
        if (!Float.isFinite(baseScale) || baseScale <= 0.0F) {
            throw new IllegalArgumentException(
                "Framebuffer base scale must be positive and finite"
            );
        }
        UiResolution resolution = UiFramebufferPresentation.resolution();
        float scale = UiFramebufferPresentation.scale(viewport, baseScale);
        int width = Math.max(
            1,
            (int)Math.ceil(ViewportGuiScale.logicalWidth(viewport) / scale)
        );
        int height = Math.max(
            1,
            (int)Math.ceil(ViewportGuiScale.logicalHeight(viewport) / scale)
        );
        int rasterScale = UiFramebufferPresentation.rasterScale(viewport, scale);
        return new UiFramebufferLayout(
            resolution,
            width,
            height,
            rasterScale,
            scale,
            UiFramebufferPresentation.sampleFootprint(viewport, scale, rasterScale)
        );
    }

    public double toCanvasX(double localGuiX) {
        return localGuiX / this.scale;
    }

    public double toCanvasY(double localGuiY) {
        return localGuiY / this.scale;
    }
}
