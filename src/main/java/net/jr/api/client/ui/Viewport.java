package net.jr.api.client.ui;

import javax.annotation.Nullable;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.WindowMetrics;

public final class Viewport {
    private Viewport() {
    }

    @Nullable
    public static ViewportArea currentOrNull() {
        Integer slotId = SlotScope.idOrNull();

        return slotId != null
                ? ClientRuntime.INSTANCE.viewports().viewportOrNull(slotId)
                : null;
    }

    public static ViewportArea current() {
        return get(SlotScope.requireId());
    }

    @Nullable
    public static ViewportArea getOrNull(int slotId) {
        return ClientRuntime.INSTANCE
                .viewports()
                .viewportOrNull(slotId);
    }

    public static ViewportArea get(int slotId) {
        return ClientRuntime.INSTANCE.viewports().viewport(slotId);
    }

    public static WindowMetrics window() {
        return ClientRuntime.INSTANCE.viewports().windowMetrics();

    }
}