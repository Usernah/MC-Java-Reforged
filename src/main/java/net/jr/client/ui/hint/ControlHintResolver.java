package net.jr.client.ui.hint;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.jr.client.input.InputApi;
import net.jr.client.input.binding.GamepadInputChord;
import net.jr.client.input.binding.UiInputBinding;
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
import net.jr.client.ui.hint.model.ResolvedControlHintInput;
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
                ? resolveGamepadHint(request)
                : resolveKeyboardMouseHint(request);
            if (hint != null) {
                resolved.add(hint);
            }
        }

        return List.copyOf(resolved);
    }

    @Nullable
    private static ResolvedControlHint resolveGamepadHint(ControlHintRequest request) {
        ControllerGlyphTheme theme = GamepadGlyphThemeState.themeForCurrentClient();
        List<ResolvedControlHintInput> resolvedInputs = new ArrayList<>(request.inputs().size());
        for (UiInputBinding input : request.inputs()) {
            ResolvedControlHintInput resolved = resolveGamepadInput(theme, input);
            if (resolved == null) {
                return null;
            }
            resolvedInputs.add(resolved);
        }

        return resolvedInputs.isEmpty()
            ? null
            : new ResolvedControlHint(request.placement(), request.label(), resolvedInputs);
    }

    @Nullable
    private static ResolvedControlHint resolveKeyboardMouseHint(ControlHintRequest request) {
        KeyboardMouseGlyphTheme theme = KeyboardMouseGlyphResolver.currentTheme();
        List<ResolvedControlHintInput> resolvedInputs = new ArrayList<>(request.inputs().size());
        for (UiInputBinding input : request.inputs()) {
            ResolvedControlHintInput resolved = resolveKeyboardMouseInput(theme, input);
            if (resolved == null) {
                return null;
            }
            resolvedInputs.add(resolved);
        }

        return new ResolvedControlHint(request.placement(), request.label(), resolvedInputs);
    }

    @Nullable
    private static ResolvedControlHintInput resolveGamepadInput(
            ControllerGlyphTheme theme,
            UiInputBinding input
    ) {
        List<GamepadDigitalInput> gamepadInputs;
        if (input instanceof UiInputBinding.KeyMappingInput keyInput) {
            GamepadInputChord chord = InputApi.binding(keyInput.mapping());
            if (chord == null) {
                return null;
            }
            gamepadInputs = chord.inputs();
        } else if (input instanceof UiInputBinding.GamepadInput gamepadInput) {
            gamepadInputs = List.of(gamepadInput.input());
        } else {
            return null;
        }

        List<ResolvedControlHintIcon> icons = new ArrayList<>(gamepadInputs.size());
        for (GamepadDigitalInput gamepadInput : gamepadInputs) {
            ResolvedControlHintIcon icon = resolveGamepadIcon(theme, gamepadInput);
            if (icon == null) {

                return null;
            }
            icons.add(icon);
        }
        return icons.isEmpty() ? null : new ResolvedControlHintInput(icons);
    }

    @Nullable
    private static ResolvedControlHintInput resolveKeyboardMouseInput(
            KeyboardMouseGlyphTheme theme,
            UiInputBinding input
    ) {
        if (!(input instanceof UiInputBinding.KeyMappingInput keyInput)) {
            return null;
        }

        List<KeyboardMouseGlyph> glyphs = KeyboardMouseGlyphResolver.glyphs(keyInput.mapping());
        if (glyphs.isEmpty()) {
            return null;
        }

        boolean pressed = ControlHintPressAnimation.isAnimating(keyInput.mapping());
        List<ResolvedControlHintIcon> icons = new ArrayList<>(glyphs.size());
        for (KeyboardMouseGlyph glyph : glyphs) {
            ResolvedControlHintIcon icon = resolveKeyboardMouseIcon(theme, glyph, pressed);
            if (icon == null) {
                return null;
            }
            icons.add(icon);
        }
        return new ResolvedControlHintInput(icons);
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
