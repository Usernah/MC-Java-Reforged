package net.jr.client.ui.hint;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.jr.client.input.InputApi;
import net.jr.client.input.binding.GamepadInputChord;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.ui.hint.glyph.ControllerGlyph;
import net.jr.client.ui.hint.glyph.ControllerGlyphTheme;
import net.jr.client.ui.hint.glyph.GamepadGlyphThemeState;
import net.jr.client.ui.hint.glyph.KeyboardMouseGlyph;
import net.jr.client.ui.hint.glyph.KeyboardMouseGlyphResolver;
import net.jr.client.ui.hint.glyph.KeyboardMouseGlyphTheme;
import net.jr.client.ui.hint.model.ControlHintRequest;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.client.ui.hint.model.ResolvedControlHintIcon;
import net.jr.client.ui.hint.render.GlyphTextureBounds;
import net.jr.client.ui.hint.render.GlyphTextureBoundsCache;
import net.jr.api.client.resource.Asset;

public final class ControlHintResolver {
    private ControlHintResolver() {
    }

    public static List<ResolvedControlHint> resolve(ControlHintContext context, List<ControlHintRequest> requests) {
        InputApi.ensureBindingsLoaded(context.minecraft());
        List<ResolvedControlHint> resolved = new ArrayList<>(requests.size());

        for (ControlHintRequest request : requests) {
            ResolvedControlHint hint = context.isGamepadMode()
                ? resolveGamepadHint(context, request)
                : resolveKeyboardMouseHint(request);
            if (hint != null) {
                resolved.add(hint);
            }
        }

        return List.copyOf(resolved);
    }

    @Nullable
    private static ResolvedControlHint resolveGamepadHint(ControlHintContext context, ControlHintRequest request) {
        GamepadInputChord chord = InputApi.binding(request.binding());
        if (chord == null) {
            return null;
        }

        ControllerGlyphTheme theme = GamepadGlyphThemeState.themeForCurrentClient();
        List<ResolvedControlHintIcon> icons = new ArrayList<>(chord.inputs().size());
        for (GamepadDigitalInput input : chord.inputs()) {
            ResolvedControlHintIcon icon = resolveGamepadIcon(theme, input);
            if (icon == null) {
                // An incomplete chord would communicate an action the player cannot perform.
                return null;
            }
            icons.add(icon);
        }

        return icons.isEmpty()
            ? null
            : new ResolvedControlHint(request.placement(), request.label(), icons);
    }

    @Nullable
    private static ResolvedControlHint resolveKeyboardMouseHint(ControlHintRequest request) {
        List<KeyboardMouseGlyph> glyphs = KeyboardMouseGlyphResolver.glyphs(request.binding());
        if (glyphs.isEmpty()) {
            return null;
        }

        KeyboardMouseGlyphTheme theme = KeyboardMouseGlyphResolver.currentTheme();
        boolean pressed = ControlHintPressAnimation.isAnimating(request.binding());
        List<ResolvedControlHintIcon> icons = new ArrayList<>(glyphs.size());
        for (KeyboardMouseGlyph glyph : glyphs) {
            ResolvedControlHintIcon icon = resolveKeyboardMouseIcon(theme, glyph, pressed);
            if (icon == null) {
                return null;
            }
            icons.add(icon);
        }

        return new ResolvedControlHint(request.placement(), request.label(), icons);
    }

    @Nullable
    private static ResolvedControlHintIcon resolveGamepadIcon(ControllerGlyphTheme theme, GamepadDigitalInput input) {
        ControllerGlyph glyph = ControllerGlyph.forInput(input);
        if (glyph == null) {
            return null;
        }
        Asset baseTexture = theme.texture(glyph);
        if (!theme.supports(glyph) || baseTexture == null) {
            return null;
        }

        Asset pressedTexture = theme.pressedTexture(glyph);
        boolean pressed = ControlHintPressAnimation.isAnimating(input);
        Asset activeTexture = pressed && pressedTexture != null ? pressedTexture : baseTexture;

        return new ResolvedControlHintIcon(
            activeTexture,
            GlyphTextureBoundsCache.get(activeTexture),
            theme.hintHeight(glyph)
        );
    }

    @Nullable
    private static ResolvedControlHintIcon resolveKeyboardMouseIcon(KeyboardMouseGlyphTheme theme, KeyboardMouseGlyph glyph, boolean pressed) {
        Asset baseTexture = theme.texture(glyph);
        if (baseTexture == null) {
            return null;
        }

        Asset pressedTexture = theme.pressedTexture(glyph);
        Asset activeTexture = pressed && pressedTexture != null ? pressedTexture : baseTexture;

        return new ResolvedControlHintIcon(
            activeTexture,
            GlyphTextureBoundsCache.get(activeTexture),
            theme.hintHeight(glyph)
        );
    }
}
