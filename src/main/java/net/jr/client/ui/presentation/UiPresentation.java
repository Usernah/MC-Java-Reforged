package net.jr.client.ui.presentation;

import net.jr.api.client.render.font.FontHolder;
import net.jr.api.client.split.SplitScreen;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class UiPresentation {
    public static final String HD_SUFFIX = "-720p";
    public static final String FULL_HD_SUFFIX = "-1080p";
    public static final String SMALL_SUFFIX = "-small";

    private UiPresentation() {
    }

    public static boolean isSplitScreen() {
        return SplitScreen.isActive();
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

    public static String glyphVariantFolder() {
        return glyphResolution().glyphVariantFolder();
    }

    /** Split-screen always uses the compact 720p glyphs and their metrics. */
    public static UiResolution glyphResolution() {
        return isSplitScreen() ? UiResolution.HD : resolution();
    }

    /**
     * Document variants in lookup order. Split-screen is an optional layer on
     * the current resolution; if it is absent, lookup falls back to the normal
     * resolution variants and finally to the base document.
     */
    public static List<String> documentVariantSuffixes() {
        UiResolution resolution = resolution();
        String currentResolution = resolutionSuffix(resolution);
        String alternateResolution = resolution == UiResolution.HD
            ? FULL_HD_SUFFIX
            : HD_SUFFIX;
        if (isSplitScreen()) {
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
