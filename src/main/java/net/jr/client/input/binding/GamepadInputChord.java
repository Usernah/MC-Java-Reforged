package net.jr.client.input.binding;

import net.jr.client.input.gamepad.GamepadDigitalInput;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public record GamepadInputChord(List<GamepadDigitalInput> inputs) {
    public GamepadInputChord {
        Objects.requireNonNull(inputs, "inputs");
        LinkedHashSet<GamepadDigitalInput> uniqueInputs = new LinkedHashSet<>();
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

