package net.jr.client.ui.hint;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.jr.client.input.InputApi;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.util.Util;

/** Keeps pressed hint glyphs visible for a fixed pulse instead of for the whole held input. */
final class ControlHintPressAnimation {
    private static final long DURATION_MS = 300L;
    private static final Map<GamepadDigitalInput, PressState> GAMEPAD_STATES =
        new EnumMap<>(GamepadDigitalInput.class);
    private static final Map<KeyMapping, PressState> KEY_STATES = new IdentityHashMap<>();

    private ControlHintPressAnimation() {
    }

    static boolean isAnimating(GamepadDigitalInput input) {
        return GAMEPAD_STATES
            .computeIfAbsent(input, ignored -> new PressState())
            .update(InputApi.isPressed(input));
    }

    static boolean isAnimating(KeyMapping mapping) {
        return KEY_STATES
            .computeIfAbsent(mapping, ignored -> new PressState())
            .update(mapping.isDown());
    }

    private static final class PressState {
        private boolean wasDown;
        private long animationEndsAt;

        private boolean update(boolean down) {
            long now = Util.getMillis();
            if (down && !this.wasDown) {
                this.animationEndsAt = now + DURATION_MS;
            }
            this.wasDown = down;
            return now < this.animationEndsAt;
        }
    }
}
