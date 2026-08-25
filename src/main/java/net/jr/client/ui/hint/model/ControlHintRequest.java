package net.jr.client.ui.hint.model;

import java.util.List;
import java.util.Objects;
import net.jr.client.input.binding.UiInputBinding;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public record ControlHintRequest(
        List<UiInputBinding> inputs,
        Component label,
        HintPlacement placement
) {
    public ControlHintRequest {
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("inputs must not be empty");
        }
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(placement, "placement");
    }

    public ControlHintRequest(KeyMapping binding, Component label) {
        this(binding, label, HintPlacement.RIGHT);
    }

    public ControlHintRequest(KeyMapping binding, Component label, HintPlacement placement) {
        this(List.of(UiInputBinding.key(binding)), label, placement);
    }
}
