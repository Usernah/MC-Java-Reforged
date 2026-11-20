package net.jr.client.ui.hint;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.state.ControlHintAnimationState;
import net.jr.client.input.InputApi;
import net.jr.api.client.input.GamepadDigitalInput;
import net.minecraft.client.KeyMapping;

public final class ControlHintPressAnimation {
    private static final long DURATION_MS = 300L;

    private ControlHintPressAnimation() {
    }

    public static boolean isAnimating(GamepadDigitalInput input) {
        return state().update(input, InputApi.isPressed(input), DURATION_MS);
    }

    public static boolean isAnimating(KeyMapping mapping) {
        return state().update(mapping, InputApi.isDown(mapping), DURATION_MS);
    }

    private static ControlHintAnimationState state() {
        Integer slotId = SlotExecution.currentSlotIdOrNull();
        return ClientRuntime.INSTANCE.slots().slot(slotId != null ? slotId : 0).inputState().controlHintAnimations();
    }
}
