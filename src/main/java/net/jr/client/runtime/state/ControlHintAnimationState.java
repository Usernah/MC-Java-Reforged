package net.jr.client.runtime.state;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.util.Util;

/** Per-player timing state for pressed control-hint glyphs. */
public final class ControlHintAnimationState {
    private final Map<GamepadDigitalInput, PressState> gamepadStates =
        new EnumMap<>(GamepadDigitalInput.class);
    private final Map<KeyMapping, PressState> keyStates = new IdentityHashMap<>();

    public boolean update(GamepadDigitalInput input, boolean down, long durationMs) {
        return this.gamepadStates
            .computeIfAbsent(input, ignored -> new PressState())
            .update(down, durationMs);
    }

    public boolean update(KeyMapping mapping, boolean down, long durationMs) {
        return this.keyStates
            .computeIfAbsent(mapping, ignored -> new PressState())
            .update(down, durationMs);
    }

    public void clear() {
        this.gamepadStates.clear();
        this.keyStates.clear();
    }

    private static final class PressState {
        private boolean wasDown;
        private long animationEndsAt;

        private boolean update(boolean down, long durationMs) {
            long now = Util.getMillis();
            if (down && !this.wasDown) {
                this.animationEndsAt = now + durationMs;
            }
            this.wasDown = down;
            return now < this.animationEndsAt;
        }
    }
}
