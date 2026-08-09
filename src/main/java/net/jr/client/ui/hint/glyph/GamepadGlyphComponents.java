package net.jr.client.ui.hint.glyph;

import java.util.Objects;
import java.util.function.Supplier;

import net.jr.api.client.resource.Asset;
import net.jr.client.input.binding.GamepadInputChord;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class GamepadGlyphComponents {
    private static final HintSet CURRENT_HINTS = new HintSet(
            GamepadGlyphThemeState::currentFont,
            GamepadGlyphThemeState::currentTheme
    );
    private static final HintSet XBOX_HINTS = new HintSet(
            ControllerGlyphTheme.XBOX::font,
            () -> ControllerGlyphTheme.XBOX
    );
    private static final HintSet PS_HINTS = new HintSet(
            ControllerGlyphTheme.PS::font,
            () -> ControllerGlyphTheme.PS
    );

    private static final HintSet GENERIC_A_HINTS = new HintSet(
            ControllerGlyphTheme.GENERIC_A::font,
            () -> ControllerGlyphTheme.GENERIC_A
    );

    private GamepadGlyphComponents() {
    }

    public static HintSet current() {
        return CURRENT_HINTS;
    }

    public static HintSet xbox() {
        return XBOX_HINTS;
    }

    public static HintSet ps() {
        return PS_HINTS;
    }

    public static HintSet generic_a() {
        return GENERIC_A_HINTS;
    }

    public static final class HintSet {
        private final Supplier<Asset> fontSupplier;
        private final Supplier<ControllerGlyphTheme> themeSupplier;

        private HintSet(Supplier<Asset> fontSupplier, Supplier<ControllerGlyphTheme> themeSupplier) {
            this.fontSupplier = Objects.requireNonNull(fontSupplier, "fontSupplier");
            this.themeSupplier = Objects.requireNonNull(themeSupplier, "themeSupplier");
        }

        public ControllerGlyphTheme theme() {
            return themeSupplier.get();
        }

        public Asset font() {
            return fontSupplier.get();
        }

        public MutableComponent glyph(ControllerGlyph glyph) {
            Objects.requireNonNull(glyph, "glyph");
            if (!theme().supports(glyph)) {
                return Component.empty();
            }
            return Component.literal(glyph.text())
                    .withStyle(style -> style.withFont(font().asFontDescription()));
        }

        public MutableComponent icon(ControllerGlyph glyph) {
            return glyph(glyph);
        }

        public MutableComponent chord(GamepadInputChord chord) {
            Objects.requireNonNull(chord, "chord");
            MutableComponent message = Component.empty();
            boolean first = true;
            for (GamepadDigitalInput input : chord.inputs()) {
                ControllerGlyph glyph = ControllerGlyph.forInput(input);
                if (!first) {
                    message.append(" + ");
                }
                if (glyph == null || !theme().supports(glyph)) {
                    message.append(input.serializedName());
                } else {
                    message.append(icon(glyph));
                }
                first = false;
            }
            return message;
        }

        public MutableComponent buttonDown() {
            return glyph(ControllerGlyph.BUTTON_DOWN);
        }

        public MutableComponent buttonRight() {
            return glyph(ControllerGlyph.BUTTON_RIGHT);
        }

        public MutableComponent buttonLeft() {
            return glyph(ControllerGlyph.BUTTON_LEFT);
        }

        public MutableComponent buttonUp() {
            return glyph(ControllerGlyph.BUTTON_UP);
        }

        public MutableComponent bumperLeft() {
            return glyph(ControllerGlyph.BUMPER_LEFT);
        }

        public MutableComponent bumperRight() {
            return glyph(ControllerGlyph.BUMPER_RIGHT);
        }

        public MutableComponent triggerLeft() {
            return glyph(ControllerGlyph.TRIGGER_LEFT);
        }

        public MutableComponent triggerRight() {
            return glyph(ControllerGlyph.TRIGGER_RIGHT);
        }

        public MutableComponent buttonStart() {
            return glyph(ControllerGlyph.BUTTON_START);
        }

        public MutableComponent buttonSelect() {
            return glyph(ControllerGlyph.BUTTON_SELECT);
        }

        public MutableComponent dpadUp() {
            return glyph(ControllerGlyph.DPAD_UP);
        }

        public MutableComponent dpadDown() {
            return glyph(ControllerGlyph.DPAD_DOWN);
        }

        public MutableComponent dpadLeft() {
            return glyph(ControllerGlyph.DPAD_LEFT);
        }

        public MutableComponent dpadRight() {
            return glyph(ControllerGlyph.DPAD_RIGHT);
        }

        public MutableComponent touchpad() {
            return glyph(ControllerGlyph.TOUCHPAD);
        }

        public MutableComponent touchpadButton() {
            return glyph(ControllerGlyph.TOUCHPAD_BUTTON);
        }

        public MutableComponent touchpadLeftButton() {
            return glyph(ControllerGlyph.TOUCHPAD_LEFT_BUTTON);
        }

        public MutableComponent touchpadRightButton() {
            return glyph(ControllerGlyph.TOUCHPAD_RIGHT_BUTTON);
        }

        public MutableComponent stickLeft() {
            return glyph(ControllerGlyph.STICK_LEFT);
        }

        public MutableComponent stickRight() {
            return glyph(ControllerGlyph.STICK_RIGHT);
        }

        public MutableComponent stickLeftButton() {
            return glyph(ControllerGlyph.STICK_LEFT_BUTTON);
        }

        public MutableComponent stickRightButton() {
            return glyph(ControllerGlyph.STICK_RIGHT_BUTTON);
        }
    }
}
