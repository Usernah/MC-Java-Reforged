package net.jr.client.input.binding;

import net.jr.api.client.input.GamepadDigitalInput;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public record GamepadInputChord(List<GamepadDigitalInput> inputs) {
    public GamepadInputChord {
        Objects.requireNonNull(inputs, "inputs");
        EnumSet<GamepadDigitalInput> uniqueInputs = EnumSet.noneOf(GamepadDigitalInput.class);
        uniqueInputs.addAll(inputs);
        if (uniqueInputs.isEmpty()) {
            throw new IllegalArgumentException("inputs must not be empty");
        }
        inputs = List.copyOf(uniqueInputs);
    }

    public static GamepadInputChord of(GamepadDigitalInput input) {
        return new GamepadInputChord(List.of(Objects.requireNonNull(input, "input")));
    }

    public static GamepadInputChord of(List<GamepadDigitalInput> inputs) {
        return new GamepadInputChord(inputs);
    }

}

