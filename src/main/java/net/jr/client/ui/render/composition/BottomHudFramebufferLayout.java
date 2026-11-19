package net.jr.client.ui.render.composition;

import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.ui.presentation.UiResolution;


public record BottomHudFramebufferLayout(
    UiResolution resolution,
    int canvasWidth,
    int canvasHeight,
    int rasterScale,
    float scale
) {
    public static BottomHudFramebufferLayout resolve(
        ViewportArea viewport,
        int parentWidth,
        int parentHeight,
        float parentPresentationScale,
        float hudScaleMultiplier
    ) {
        UiResolution resolution = UiFramebufferPresentation.resolution();
        float baseScale = UiFramebufferPresentation.scale(viewport);
        float absoluteScale =
            baseScale * Math.max(hudScaleMultiplier, Float.MIN_NORMAL);
        float scale = absoluteScale
            / Math.max(parentPresentationScale, Float.MIN_NORMAL);
        int width = Math.max(
            1,
            (int)Math.ceil(Math.max(1, parentWidth) / scale)
        );
        int height = Math.max(
            1,
            (int)Math.ceil(Math.max(1, parentHeight) / scale)
        );
        int rasterScale =
            UiFramebufferPresentation.rasterScale(viewport, absoluteScale);
        return new BottomHudFramebufferLayout(
            resolution,
            width,
            height,
            rasterScale,
            scale
        );
    }
}
