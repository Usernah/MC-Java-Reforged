package net.jr.client.ui.presentation;

import net.jr.api.client.render.font.FontHolder;
import net.jr.api.client.split.SplitScreen;
import net.jr.registry.ModFonts;

/** Visual resolution tier used by UI assets, typography and compositions. */
public enum UiResolution {
    FULL_HD(1920, 1080, 0.25F, "", ModFonts.MC_SEVEN, true),
    HD(1280, 720, 0.50F, "split_screen", ModFonts.MINECRAFT_MIN, false);

    private final int referenceWidth;
    private final int referenceHeight;
    private final float framebufferScale;
    private final String glyphVariantFolder;
    private final FontHolder font;
    private final boolean legacyTextShadow;

    UiResolution(
        int referenceWidth,
        int referenceHeight,
        float framebufferScale,
        String glyphVariantFolder,
        FontHolder font,
        boolean legacyTextShadow
    ) {
        this.referenceWidth = referenceWidth;
        this.referenceHeight = referenceHeight;
        this.framebufferScale = framebufferScale;
        this.glyphVariantFolder = glyphVariantFolder;
        this.font = font;
        this.legacyTextShadow = legacyTextShadow;
    }

    public int referenceWidth() {
        return this.referenceWidth;
    }

    public int referenceHeight() {
        return this.referenceHeight;
    }

    public float framebufferScale() {
        return this.framebufferScale;
    }

    public String glyphVariantFolder() {
        return this.glyphVariantFolder;
    }

    public FontHolder font() {
        return this.font;
    }

    public boolean legacyTextShadow() {
        return this.legacyTextShadow;
    }
}
