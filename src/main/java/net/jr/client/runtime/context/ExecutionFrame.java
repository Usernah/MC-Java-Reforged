package net.jr.client.runtime.context;

import javax.annotation.Nullable;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.render.world.WorldRenderView;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.viewport.ViewportArea;

public record ExecutionFrame(
    Slots slot,
    @Nullable ClientInstance client,
    @Nullable ViewportArea viewport,
    @Nullable WorldRenderView worldView,
    ExecutionPhase phase
) {
}
