package net.jr.client.ui.hint;

import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClient;
import net.jr.client.runtime.state.ControlHintAnimationState;
import net.jr.client.input.InputApi;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.minecraft.client.KeyMapping;

/** Keeps pressed hint glyphs visible for a fixed pulse instead of for the whole held input. */
final class ControlHintPressAnimation {
    private static final long DURATION_MS = 300L;

    private ControlHintPressAnimation() {
    }

    static boolean isAnimating(GamepadDigitalInput input) {
        return state().update(input, InputApi.isPressed(input), DURATION_MS);
    }

    static boolean isAnimating(KeyMapping mapping) {
        return state().update(mapping, InputApi.isDown(mapping), DURATION_MS);
    }

    private static ControlHintAnimationState state() {
        LocalClient current = LocalClientAcces.currentOrNull();
        return (current != null ? current.input() : LocalClientAcces.input(0)).controlHintAnimations();
    }
}
