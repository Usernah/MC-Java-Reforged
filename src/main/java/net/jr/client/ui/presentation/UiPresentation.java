package net.jr.client.ui.presentation;

import net.jr.api.client.render.font.FontHolder;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class UiPresentation {
    public static final String HD_SUFFIX = "-720p";
    public static final String FULL_HD_SUFFIX = "";
    public static final String SMALL_SUFFIX = "-small";

    private UiPresentation() {
    }

    public static UiResolution resolution() {
        var window = Minecraft.getInstance().getWindow();
        return window.getWidth() < UiResolution.FULL_HD.referenceWidth()
            || window.getHeight() < UiResolution.FULL_HD.referenceHeight()
            ? UiResolution.HD
            : UiResolution.FULL_HD;
    }

    public static boolean usesHdDensity() {
        return resolution() == UiResolution.HD;
    }

    public static FontHolder currentFont() {
        return glyphResolution().font();
    }

    public static UiSizeMode sizeMode() {
        return UiPresentationPolicy.sizeMode();
    }

    public static boolean isSmall() {
        return UiPresentationPolicy.isSmall();
    }

    public static UiResolution glyphResolution() {
        return isSmall()
            ? UiResolution.HD
            : UiResolution.FULL_HD;
    }


    public static UiResolution framebufferResolution() {
        return UiResolution.HD;
    }


    public static UiFramebufferScale framebufferScale() {
        return isSmall()
            ? UiFramebufferScale.SMALL
            : UiFramebufferScale.forResolution(framebufferResolution());
    }


    public static List<String> documentVariantSuffixes() {
        UiResolution resolution = resolution();
        String currentResolution = resolutionSuffix(resolution);
        String alternateResolution = resolution == UiResolution.HD
            ? FULL_HD_SUFFIX
            : HD_SUFFIX;
        if (isSmall()) {
            return List.of(
                currentResolution + SMALL_SUFFIX,
                alternateResolution + SMALL_SUFFIX,
                currentResolution,
                alternateResolution
            );
        }
        return List.of(currentResolution, alternateResolution);
    }

    private static String resolutionSuffix(UiResolution resolution) {
        return resolution == UiResolution.HD ? HD_SUFFIX : FULL_HD_SUFFIX;
    }
}
