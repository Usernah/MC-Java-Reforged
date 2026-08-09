package net.jr.client.ui.hint.glyph;

import net.jr.api.client.resource.Asset;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Java-side registry for controller glyph themes.
 *
 * Theme identity, supported glyphs and pressed variants remain code-defined.
 * Only visual heights are loaded from each theme's definitions.json.
 */
public enum ControllerGlyphTheme {
    XBOX(
        "controller/xbox",
        standardGlyphs(ControllerGlyph.BUTTON_GUIDE, ControllerGlyph.MISC_1),
        faceButtonPressGlyphs()
    ),
    XBOX_OLD(
        "controller/xbox_o",
        standardGlyphs(ControllerGlyph.BUTTON_GUIDE, ControllerGlyph.MISC_1),
        faceButtonPressGlyphs()
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
        ),
        faceButtonPressGlyphs()
    ),
    PS(
        "controller/ps",
        standardGlyphs(
                ControllerGlyph.BUTTON_GUIDE,
            ControllerGlyph.TOUCHPAD,
            ControllerGlyph.TOUCHPAD_BUTTON,
            ControllerGlyph.TOUCHPAD_LEFT_BUTTON,
            ControllerGlyph.TOUCHPAD_RIGHT_BUTTON
        ),
        faceButtonPressGlyphs()
    ),
    PS_OLD(
        "controller/ps_o",
        standardGlyphs(
                ControllerGlyph.BUTTON_GUIDE,
            ControllerGlyph.TOUCHPAD,
            ControllerGlyph.TOUCHPAD_BUTTON,
            ControllerGlyph.TOUCHPAD_LEFT_BUTTON,
            ControllerGlyph.TOUCHPAD_RIGHT_BUTTON
        ),
        faceButtonPressGlyphs()
    ),
    GENERIC_A("controller/generic_a", standardGlyphs()),
    SWITCH("controller/switch", standardGlyphs());

    private final Asset font;
    private final Asset textureFolder;
    private final Asset definitions;
    private final Set<ControllerGlyph> supportedGlyphs;
    private final Map<ControllerGlyph, Asset> textures = new EnumMap<>(ControllerGlyph.class);
    private final Map<ControllerGlyph, Asset> pressedTextures = new EnumMap<>(ControllerGlyph.class);
    private final EnumSet<ControllerGlyph> pressGlyphs = EnumSet.noneOf(ControllerGlyph.class);

    ControllerGlyphTheme(String themePath, Set<ControllerGlyph> supportedGlyphs) {
        this(themePath, supportedGlyphs, Set.of());
    }

    ControllerGlyphTheme(
        String themePath,
        Set<ControllerGlyph> supportedGlyphs,
        Set<ControllerGlyph> pressGlyphs
    ) {
        this.font = Asset.MOD(themePath);
        this.textureFolder = Asset.MOD("textures/" + themePath);
        this.definitions = textureFolder.child("definitions.json");
        this.supportedGlyphs = Set.copyOf(supportedGlyphs);

        if (!this.supportedGlyphs.containsAll(pressGlyphs)) {
            throw new IllegalArgumentException(
                "Pressed glyphs must also be supported by theme " + name()
            );
        }
        this.pressGlyphs.addAll(pressGlyphs);

        for (ControllerGlyph glyph : this.supportedGlyphs) {
            textures.put(glyph, textureFolder.child(glyph.fileName() + ".png"));
            if (this.pressGlyphs.contains(glyph)) {
                pressedTextures.put(glyph, textureFolder.child(glyph.fileName() + "_press.png"));
            }
        }
    }

    public Asset font() {
        return font;
    }

    Asset definitions() {
        return definitions;
    }

    public boolean supports(ControllerGlyph glyph) {
        return supportedGlyphs.contains(glyph);
    }

    public Set<ControllerGlyph> supportedGlyphs() {
        return supportedGlyphs;
    }

    @Nullable
    public Asset texture(ControllerGlyph glyph) {
        return textures.get(glyph);
    }

    public float hintHeight(ControllerGlyph glyph) {
        return ControllerGlyphDefinitions.height(this, glyph);
    }

    @Nullable
    public Asset pressedTexture(ControllerGlyph glyph) {
        return pressedTextures.get(glyph);
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

    private static Set<ControllerGlyph> faceButtonPressGlyphs() {
        return EnumSet.of(
            ControllerGlyph.BUTTON_UP,
            ControllerGlyph.BUTTON_DOWN,
            ControllerGlyph.BUTTON_LEFT,
            ControllerGlyph.BUTTON_RIGHT,
                ControllerGlyph.BUMPER_LEFT,
                ControllerGlyph.BUMPER_RIGHT,
                ControllerGlyph.TRIGGER_LEFT,
                ControllerGlyph.TRIGGER_RIGHT
        );
    }
}
