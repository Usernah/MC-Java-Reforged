package net.jr.client.ui.hint.glyph;

import net.jr.api.client.resource.Asset;

import java.util.EnumSet;
import java.util.Set;

public enum ControllerGlyphTheme {
    XBOX(
        "controller/xbox",
        standardGlyphs(ControllerGlyph.BUTTON_GUIDE)
    ),
    XBOX_OLD(
        "controller/xbox_o",
        standardGlyphs(ControllerGlyph.BUTTON_GUIDE)
    ),
    STEAM(
        "controller/steam",
        standardGlyphs(
            ControllerGlyph.BUTTON_GUIDE,
            ControllerGlyph.MISC_1,
            ControllerGlyph.PADDLE_LEFT_1,
            ControllerGlyph.PADDLE_LEFT_2,
            ControllerGlyph.PADDLE_RIGHT_1,
            ControllerGlyph.PADDLE_RIGHT_2
        )
    ),
    PS(
        "controller/ps",
        standardGlyphs(
                ControllerGlyph.BUTTON_GUIDE,
            ControllerGlyph.TOUCHPAD,
            ControllerGlyph.TOUCHPAD_BUTTON,
            ControllerGlyph.TOUCHPAD_LEFT_BUTTON,
            ControllerGlyph.TOUCHPAD_RIGHT_BUTTON
        )
    ),
    PS_OLD(
        "controller/ps_o",
        standardGlyphs(
                ControllerGlyph.BUTTON_GUIDE,
            ControllerGlyph.TOUCHPAD,
            ControllerGlyph.TOUCHPAD_BUTTON,
            ControllerGlyph.TOUCHPAD_LEFT_BUTTON,
            ControllerGlyph.TOUCHPAD_RIGHT_BUTTON
        )
    ),
    GENERIC_A("controller/generic_a", standardGlyphs()),
    SWITCH("controller/switch", standardGlyphs());

    private final Asset textureFolder;
    private final Asset controlTexture;
    private final String metricId;
    private final Set<ControllerGlyph> supportedGlyphs;

    ControllerGlyphTheme(String themePath, Set<ControllerGlyph> supportedGlyphs) {
        this.textureFolder = Asset.MOD("textures/" + themePath);
        this.controlTexture = textureFolder.child("control.png");
        this.metricId = themePath.substring(themePath.lastIndexOf('/') + 1);
        this.supportedGlyphs = Set.copyOf(supportedGlyphs);

    }

    public Asset textureFolder() {
        return this.textureFolder;
    }

    public String metricId() {
        return this.metricId;
    }

    public boolean supports(ControllerGlyph glyph) {
        return supportedGlyphs.contains(glyph);
    }

    public Set<ControllerGlyph> supportedGlyphs() {
        return supportedGlyphs;
    }

    public Asset controlTexture() {
        return this.controlTexture;
    }

    private static Set<ControllerGlyph> standardGlyphs(ControllerGlyph... extraGlyphs) {
        EnumSet<ControllerGlyph> glyphs = EnumSet.of(
            ControllerGlyph.BUTTON_DOWN,
            ControllerGlyph.BUTTON_RIGHT,
            ControllerGlyph.BUTTON_LEFT,
            ControllerGlyph.BUTTON_UP,
            ControllerGlyph.BUMPER_LEFT,
            ControllerGlyph.BUMPER_RIGHT,
            ControllerGlyph.TRIGGER_LEFT,
            ControllerGlyph.TRIGGER_RIGHT,
            ControllerGlyph.BUTTON_SELECT,
            ControllerGlyph.BUTTON_START,
            ControllerGlyph.DPAD,
            ControllerGlyph.DPAD_UP,
            ControllerGlyph.DPAD_DOWN,
            ControllerGlyph.DPAD_LEFT,
            ControllerGlyph.DPAD_RIGHT,
            ControllerGlyph.STICK_LEFT,
            ControllerGlyph.STICK_RIGHT,
            ControllerGlyph.STICK_LEFT_BUTTON,
            ControllerGlyph.STICK_RIGHT_BUTTON,
            ControllerGlyph.STICK_LEFT_MOVE_UP,
            ControllerGlyph.STICK_LEFT_MOVE_DOWN,
            ControllerGlyph.STICK_LEFT_MOVE_LEFT,
            ControllerGlyph.STICK_LEFT_MOVE_RIGHT,
            ControllerGlyph.STICK_RIGHT_MOVE_UP,
            ControllerGlyph.STICK_RIGHT_MOVE_DOWN,
            ControllerGlyph.STICK_RIGHT_MOVE_LEFT,
            ControllerGlyph.STICK_RIGHT_MOVE_RIGHT
        );
        for (ControllerGlyph glyph : extraGlyphs) {
            glyphs.add(glyph);
        }
        return glyphs;
    }

}
