package net.jr.client.ui.render.composition;

import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.client.ui.presentation.UiResolution;


public record HintFramebufferLayout(
    UiResolution resolution,
    int canvasWidth,
    int canvasHeight,
    int rasterScale,
    float destinationY,
    float scale,
    float sampleFootprint
) {
    public static HintFramebufferLayout resolve(
        ViewportArea viewport,
        int parentHeight,
        float parentPresentationScale,
        int maximumCanvasHeight
    ) {
        if (!UiPresentation.isSmall()) {
            return resolveFullHd(
                viewport,
                parentHeight,
                parentPresentationScale,
                maximumCanvasHeight
            );
        }

        float baseScale = UiPresentation.framebufferScale().value();
        UiFramebufferLayout base = UiFramebufferLayout.resolve(
            viewport,
            baseScale
        );
        int canvasHeight = Math.min(
            base.canvasHeight(),
            Math.max(1, maximumCanvasHeight)
        );
        int rasterScale = Math.max(2, base.rasterScale());
        float scale = base.scale()
            / Math.max(parentPresentationScale, Float.MIN_NORMAL);
        return new HintFramebufferLayout(
            base.resolution(),
            base.canvasWidth(),
            canvasHeight,
            rasterScale,
            parentHeight - canvasHeight * scale,
            scale,
            UiFramebufferPresentation.sampleFootprint(
                viewport,
                base.scale(),
                rasterScale
            )
        );
    }

    private static HintFramebufferLayout resolveFullHd(
        ViewportArea viewport,
        int parentHeight,
        float parentPresentationScale,
        int maximumCanvasHeight
    ) {
        UiResolution resolution = UiResolution.FULL_HD;
        int canvasWidth = resolution.referenceWidth();
        int canvasHeight = Math.min(
            resolution.referenceHeight(),
            Math.max(1, maximumCanvasHeight)
        );
        float absoluteScale = ViewportGuiScale.logicalWidth(viewport)
            / (float)canvasWidth;
        float relativeScale = absoluteScale
            / Math.max(parentPresentationScale, Float.MIN_NORMAL);
        int rasterScale = 2;
        return new HintFramebufferLayout(
            resolution,
            canvasWidth,
            canvasHeight,
            rasterScale,
            parentHeight - canvasHeight * relativeScale,
            relativeScale,
            UiFramebufferPresentation.sampleFootprint(
                viewport,
                absoluteScale,
                rasterScale
            )
        );
    }
}
