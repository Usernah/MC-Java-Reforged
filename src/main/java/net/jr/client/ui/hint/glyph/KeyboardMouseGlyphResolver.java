package net.jr.client.ui.hint.glyph;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyModifier;

public final class KeyboardMouseGlyphResolver {
    private static volatile KeyboardMouseGlyphTheme currentTheme = KeyboardMouseGlyphTheme.PC;

    private KeyboardMouseGlyphResolver() {
    }

    public static KeyboardMouseGlyphTheme currentTheme() {
        return currentTheme;
    }

    public static void setCurrentTheme(KeyboardMouseGlyphTheme theme) {
        currentTheme = Objects.requireNonNull(theme, "theme");
    }

    public static List<KeyboardMouseGlyph> glyphs(KeyMapping mapping) {
        if (mapping == null || mapping.isUnbound()) {
            return List.of();
        }

        List<KeyboardMouseGlyph> glyphs = new ArrayList<>(2);
        KeyboardMouseGlyph modifierGlyph = modifierGlyph(mapping.getKeyModifier());
        if (modifierGlyph != null) {
            glyphs.add(modifierGlyph);
        }
        glyphs.add(KeyboardMouseGlyph.fromKey(mapping.getKey()));
        return List.copyOf(glyphs);
    }

    private static KeyboardMouseGlyph modifierGlyph(KeyModifier modifier) {
        return switch (modifier) {
            case SHIFT -> KeyboardMouseGlyph.SPECIAL_SHIFT_LEFT;
            case CONTROL -> KeyboardMouseGlyph.SPECIAL_CTRL_LEFT;
            case ALT -> KeyboardMouseGlyph.SPECIAL_ALT_LEFT;
            case NONE -> null;
            default -> null;
        };
    }
}
