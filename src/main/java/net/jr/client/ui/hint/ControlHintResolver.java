package net.jr.client.ui.hint;

import net.jr.Java_reforged;
import net.jr.api.client.input.GamepadDigitalInput;
import net.jr.api.client.render.glyph.Glyphs;
import net.jr.client.input.InputApi;
import net.jr.client.input.binding.GamepadInputChord;
import net.jr.client.ui.hint.glyph.ControllerGlyph;
import net.jr.client.ui.hint.glyph.GamepadGlyphThemeState;
import net.jr.client.ui.hint.glyph.KeyboardMouseGlyph;
import net.jr.client.ui.hint.glyph.KeyboardMouseGlyphResolver;
import net.jr.client.ui.hint.model.ControlHintRequest;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.client.ui.hint.render.ControlHintMetrics;
import net.jr.client.ui.navigation.UiInputBinding;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ControlHintResolver {
    private static final String PROVIDER = "controls";

    private ControlHintResolver() {
    }

    public static List<ResolvedControlHint> resolve(
        ControlHintContext context,
        List<ControlHintRequest> requests
    ) {
        InputApi.ensureBindingsLoaded(context.minecraft());
        List<ResolvedControlHint> resolved = new ArrayList<>(requests.size());

        for (ControlHintRequest request : requests) {
            MutableComponent controls = resolveControls(context, request.inputs());
            if (controls == null) {
                continue;
            }
            MutableComponent content = Component.empty()
                .append(controls)
                .append(request.label());
            resolved.add(new ResolvedControlHint(request.placement(), content));
        }
        return List.copyOf(resolved);
    }

    @Nullable
    private static MutableComponent resolveControls(
        ControlHintContext context,
        List<UiInputBinding> inputs
    ) {
        MutableComponent result = Component.empty();
        boolean first = true;
        for (UiInputBinding input : inputs) {
            MutableComponent resolved = context.isGamepadMode()
                ? resolveGamepadInput(input)
                : resolveKeyboardInput(input);
            if (resolved == null) {
                return null;
            }
            if (!first) {
                result.append(Glyphs.horizontalSpace(
                    ControlHintMetrics.current().interInputSpacing()
                ));
            }
            result.append(resolved);
            first = false;
        }
        return first ? null : result;
    }

    @Nullable
    private static MutableComponent resolveGamepadInput(UiInputBinding input) {
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

        MutableComponent result = Component.empty();
        boolean first = true;
        for (GamepadDigitalInput gamepadInput : gamepadInputs) {
            ControllerGlyph glyph = ControllerGlyph.forInput(gamepadInput);
            if (glyph == null
                || !GamepadGlyphThemeState.themeForCurrentClient().supports(glyph)) {
                return null;
            }
            if (!first) {
                result.append(" + ");
            }
            result.append(Glyphs.component(Java_reforged.MODID, PROVIDER, glyph.name()));
            first = false;
        }
        return first ? null : result;
    }

    @Nullable
    private static MutableComponent resolveKeyboardInput(UiInputBinding input) {
        if (!(input instanceof UiInputBinding.KeyMappingInput keyInput)) {
            return null;
        }
        List<KeyboardMouseGlyph> glyphs =
            KeyboardMouseGlyphResolver.glyphs(keyInput.mapping());
        if (glyphs.isEmpty()) {
            return null;
        }

        MutableComponent result = Component.empty();
        boolean first = true;
        for (KeyboardMouseGlyph glyph : glyphs) {
            if (!first) {
                result.append(" + ");
            }
            result.append(Glyphs.component(Java_reforged.MODID, PROVIDER, glyph.name()));
            first = false;
        }
        return result;
    }
}

