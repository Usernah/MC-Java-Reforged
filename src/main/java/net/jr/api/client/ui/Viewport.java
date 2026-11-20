package net.jr.api.client.ui;

import javax.annotation.Nullable;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.WindowMetrics;

public final class Viewport {
    private Viewport() {
    }

    @Nullable
    public static ViewportArea currentOrNull() {
        ViewportArea viewport = SlotExecution.currentViewportOrNull();
        if (viewport != null) {
            return viewport;
        }
        Integer slotId = SlotExecution.currentSlotIdOrNull();

        return slotId != null
                ? ClientRuntime.INSTANCE.viewports().viewportOrNull(slotId)
                : null;
    }

    public static ViewportArea current() {
        ViewportArea viewport = SlotExecution.currentViewportOrNull();
        if (viewport != null) {
            return viewport;
        }
        return get(SlotExecution.currentSlotId());
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

    @Nullable
    public static WindowMetrics windowOrNull() {
        var viewports = ClientRuntime.INSTANCE.viewports();
        return viewports.hasWindowMetrics()
                ? viewports.windowMetrics()
                : null;
    }

    public static int count() {
        return ClientRuntime.INSTANCE.viewports().viewportCount();
    }
}
