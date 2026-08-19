package net.jr.client.ui.hint.model;

import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public record ControlHintRequest(KeyMapping binding, Component label, HintPlacement placement) {
    public ControlHintRequest {
        Objects.requireNonNull(binding, "binding");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(placement, "placement");
    }

    public ControlHintRequest(KeyMapping binding, Component label) {
        this(binding, label, HintPlacement.RIGHT);
    }
}
