package net.jr.api.client.render.world;

import javax.annotation.Nullable;
import net.jr.client.runtime.context.ExecutionFrame;
import net.jr.client.runtime.context.SlotExecution;

public final class WorldRenderContexts {
    private WorldRenderContexts() {
    }

    public static WorldRenderContext current() {
        WorldRenderContext context = currentOrNull();
        if (context == null) {
            throw new IllegalStateException("No world render context is active");
        }
        return context;
    }

    public static @Nullable WorldRenderContext currentOrNull() {
        ExecutionFrame frame = SlotExecution.currentFrameOrNull();
        return frame != null ? frame.worldView() : null;
    }
}
