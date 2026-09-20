package net.jr.experimental_features.copper_golem;

import net.minecraft.core.GlobalPos;

public record CopperGolemContainerEntry(
        GlobalPos pos,
        CopperGolemFamily family,
        float confidence,
        long lastSeenGameTime
) {
}
