package net.jr.client.ui.presentation;

import net.jr.api.client.render.font.FontHolder;
import net.jr.registry.ModFonts;


public enum UiResolution {
    FULL_HD(1920, 1080, ModFonts.MC_SEVEN, true),
    HD(1280, 720, ModFonts.MINECRAFT_MIN, false);

    private final int referenceWidth;
    private final int referenceHeight;
    private final FontHolder font;
    private final boolean legacyTextShadow;

    UiResolution(
        int referenceWidth,
        int referenceHeight,
        FontHolder font,
        boolean legacyTextShadow
    ) {
        this.referenceWidth = referenceWidth;
        this.referenceHeight = referenceHeight;
        this.font = font;
        this.legacyTextShadow = legacyTextShadow;
    }

    public int referenceWidth() {
        return this.referenceWidth;
    }

    public int referenceHeight() {
        return this.referenceHeight;
    }

    public FontHolder font() {
        return this.font;
    }

    public boolean legacyTextShadow() {
        return this.legacyTextShadow;
    }
}
