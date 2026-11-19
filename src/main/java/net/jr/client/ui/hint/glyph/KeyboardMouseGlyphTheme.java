package net.jr.client.ui.hint.glyph;

import net.jr.api.client.resource.Asset;

import java.util.EnumSet;
import java.util.Set;

public enum KeyboardMouseGlyphTheme {
    PC("controller/pc");

    private final Asset textureFolder;
    private final String metricId;
    private final Set<KeyboardMouseGlyph> supportedGlyphs;

    KeyboardMouseGlyphTheme(String themePath) {
        this.textureFolder = Asset.MOD("textures/" + themePath);
        this.metricId = themePath.substring(themePath.lastIndexOf('/') + 1);
        this.supportedGlyphs = Set.copyOf(EnumSet.allOf(KeyboardMouseGlyph.class));
    }

    public Asset textureFolder() {
        return this.textureFolder;
    }

    public String metricId() {
        return this.metricId;
    }

    public boolean supports(KeyboardMouseGlyph glyph) {
        return this.supportedGlyphs.contains(glyph);
    }

    public Set<KeyboardMouseGlyph> supportedGlyphs() {
        return this.supportedGlyphs;
    }

}
