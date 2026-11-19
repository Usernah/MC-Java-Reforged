package net.jr.client.ui.render.composition;

import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportLayout;
import net.jr.client.ui.presentation.UiFramebufferScale;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.client.ui.presentation.UiResolution;
import net.minecraft.client.Minecraft;


public final class UiFramebufferPresentation {
    private static final float SMALL_GUI_SCALE_REFERENCE = 2.0F;
    private static final float FULL_HD_GUI_SCALE_REFERENCE = 3.0F;

    private UiFramebufferPresentation() {
    }

    public static UiResolution resolution() {
        return UiPresentation.framebufferResolution();
    }

    public static float scale(ViewportArea viewport) {
        return scale(viewport, UiPresentation.framebufferScale().value());
    }

    public static float scale(ViewportArea viewport, UiFramebufferScale scale) {
        return scale(viewport, scale.value());
    }

    public static float scale(ViewportArea viewport, float baseScale) {
        float scale = baseScale;
        if (!UiPresentation.isSmall()) {
            return scale;
        }

        double effectiveGuiScale = viewport.effectiveGuiScale();
        if (!Double.isFinite(effectiveGuiScale) || effectiveGuiScale <= 0.0D) {
            return scale;
        }

        var window = Minecraft.getInstance().getWindow();
        float referenceScale = window.getWidth() >= UiResolution.FULL_HD.referenceWidth()
                && window.getHeight() >= UiResolution.FULL_HD.referenceHeight()
            ? FULL_HD_GUI_SCALE_REFERENCE
            : SMALL_GUI_SCALE_REFERENCE;
        scale = scale * referenceScale / (float)effectiveGuiScale;
        if (viewport.layout() == ViewportLayout.SINGLE && !SplitScreen.isActive()) {
            scale *= 2.0F;
        }
        return scale;
    }

    public static float sampleFootprint(ViewportArea viewport, float scale, int rasterScale) {
        float physicalDestinationScale = physicalDestinationScale(viewport, scale);
        return physicalDestinationScale > 0.0F
            ? Math.max(1.0F, Math.max(1, rasterScale) / physicalDestinationScale)
            : 1.0F;
    }

    public static int rasterScale(ViewportArea viewport, float scale) {
        float physicalDestinationScale = physicalDestinationScale(viewport, scale);
        return physicalDestinationScale > 0.0F
            ? Math.max(1, (int)Math.ceil(physicalDestinationScale))
            : 1;
    }

    private static float physicalDestinationScale(ViewportArea viewport, float scale) {
        double effectiveGuiScale = viewport.effectiveGuiScale();
        if (!Double.isFinite(effectiveGuiScale) || effectiveGuiScale <= 0.0D) {
            effectiveGuiScale = Minecraft.getInstance().getWindow().getGuiScale();
        }
        return scale * (float)effectiveGuiScale;
    }
}
