package net.jr.client.input.binding;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jr.client.input.InputApi;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.minecraft.client.KeyMapping;

final class GamepadBindingEvaluator {
    private final Map<String, Boolean> appliedStates = new HashMap<>();
    private final EnumSet<GamepadDigitalInput> suppressedInputs = EnumSet.noneOf(GamepadDigitalInput.class);

    void apply(GamepadBindingRegistry bindings, BindingContext context) {
        Map<String, Boolean> nextStates = new HashMap<>();
        this.suppressedInputs.removeIf(input -> !InputApi.isPressed(input));
        List<BindingSample> samples = new ArrayList<>();

        for (Map.Entry<String, GamepadInputChord> entry : bindings.bindings().entrySet()) {
            KeyMapping keyMapping = bindings.keyMapping(entry.getKey());
            if (keyMapping == null || !bindings.isActive(keyMapping, context)) {
                continue;
            }

            GamepadInputChord chord = entry.getValue();
            boolean physicallyDown = isChordPressed(chord);
            if (physicallyDown) {
                InputApi.markGamepadInput();
            }
            samples.add(new BindingSample(entry.getKey(), keyMapping, chord, physicallyDown));
        }

        for (BindingSample sample : samples) {
            boolean down = sample.physicallyDown()
                && !isSuppressed(sample.chord())
                && !isShadowedByPressedSuperset(sample, samples);
            boolean previous = this.appliedStates.getOrDefault(sample.keyName(), false);
            if (down && !previous) {
                KeyMappingClickBridge.increment(sample.keyMapping());
            }
            if (down != previous) {
                sample.keyMapping().setDown(down);
            }
            nextStates.put(sample.keyName(), down);
        }

        for (String keyName : this.appliedStates.keySet()) {
            if (!nextStates.containsKey(keyName)) {
                KeyMapping keyMapping = bindings.keyMapping(keyName);
                if (keyMapping != null) {
                    keyMapping.setDown(false);
                }
            }
        }

        this.appliedStates.clear();
        this.appliedStates.putAll(nextStates);
    }

    void release(GamepadBindingRegistry bindings) {
        for (String keyName : this.appliedStates.keySet()) {
            KeyMapping keyMapping = bindings.keyMapping(keyName);
            if (keyMapping != null) {
                keyMapping.setDown(false);
            }
        }
        this.appliedStates.clear();
    }

    void suppressHeldInputs() {
        for (GamepadDigitalInput input : GamepadDigitalInput.values()) {
            if (InputApi.isPressed(input)) {
                this.suppressedInputs.add(input);
            }
        }
    }

    List<GamepadDigitalInput> currentlyPressedInputs() {
        List<GamepadDigitalInput> pressed = java.util.Arrays.stream(GamepadDigitalInput.values())
            .filter(InputApi::isPressed)
            .toList();
        if (!pressed.isEmpty()) {
            InputApi.markGamepadInput();
        }
        return pressed;
    }

    private boolean isSuppressed(GamepadInputChord chord) {
        return chord.inputs().stream().anyMatch(this.suppressedInputs::contains);
    }

    private static boolean isChordPressed(GamepadInputChord chord) {
        return chord.inputs().stream().allMatch(InputApi::isPressed);
    }

    private static boolean isShadowedByPressedSuperset(BindingSample candidate, List<BindingSample> samples) {
        if (!candidate.physicallyDown()) {
            return false;
        }
        return samples.stream().anyMatch(other -> other.physicallyDown()
            && other.chord().inputs().size() > candidate.chord().inputs().size()
            && other.chord().inputs().containsAll(candidate.chord().inputs()));
    }

    private record BindingSample(
        String keyName,
        KeyMapping keyMapping,
        GamepadInputChord chord,
        boolean physicallyDown
    ) {
    }
}

