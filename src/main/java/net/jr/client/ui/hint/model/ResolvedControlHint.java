package net.jr.client.ui.hint.model;

import net.minecraft.network.chat.Component;

import java.util.Objects;

public record ResolvedControlHint(
    HintPlacement placement,
    Component content
) {
    public ResolvedControlHint {
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(content, "content");
    }
}
