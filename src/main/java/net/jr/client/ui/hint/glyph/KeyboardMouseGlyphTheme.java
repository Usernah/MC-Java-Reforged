package net.jr.client.ui.hint.glyph;

import net.jr.api.client.resource.Asset;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum KeyboardMouseGlyphTheme {
    PC("controller/pc");

    private final Asset textureFolder;
    private final Asset definitions;
    private final Set<KeyboardMouseGlyph> supportedGlyphs;
    private final Map<KeyboardMouseGlyph, Asset> textures = new EnumMap<>(KeyboardMouseGlyph.class);
    private final Map<KeyboardMouseGlyph, Asset> pressedTextures = new EnumMap<>(KeyboardMouseGlyph.class);
    private final EnumSet<KeyboardMouseGlyph> pressGlyphs = EnumSet.noneOf(KeyboardMouseGlyph.class);

    KeyboardMouseGlyphTheme(String themePath, KeyboardMouseGlyph... pressGlyphs) {
        this.textureFolder = Asset.MOD("textures/" + themePath);
        this.definitions = this.textureFolder.child("definitions.json");
        this.supportedGlyphs = Set.copyOf(EnumSet.allOf(KeyboardMouseGlyph.class));
        this.pressGlyphs.addAll(Set.of(pressGlyphs));

        for (KeyboardMouseGlyph glyph : this.supportedGlyphs) {
            this.textures.put(glyph, textureFolder.child(glyph.fileName() + ".png"));

            if (this.pressGlyphs.contains(glyph)) {
                this.pressedTextures.put(
                    glyph,
                    textureFolder.child(glyph.fileName() + "_press.png")
                );
            }
        }
    }

    Asset definitions() {
        return this.definitions;
    }

    public boolean supports(KeyboardMouseGlyph glyph) {
        return this.supportedGlyphs.contains(glyph);
    }

    public Set<KeyboardMouseGlyph> supportedGlyphs() {
        return this.supportedGlyphs;
    }

    @Nullable
    public Asset texture(KeyboardMouseGlyph glyph) {
        return this.textures.get(glyph);
    }

    public float hintHeight(KeyboardMouseGlyph glyph) {
        return KeyboardMouseGlyphDefinitions.height(this, glyph);
    }

    @Nullable
    public Asset pressedTexture(KeyboardMouseGlyph glyph) {
        return this.pressedTextures.get(glyph);
    }
}
