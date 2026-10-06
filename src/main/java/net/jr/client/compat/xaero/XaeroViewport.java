package net.jr.client.compat.xaero;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportRenderScope;

public final class XaeroViewport {
    private XaeroViewport() {
    }

    public static ViewportArea currentOrNull() {
        ViewportArea viewport = ViewportRenderScope.activeViewportOrNull();
        if (viewport != null) {
            return viewport;
        }

        Integer slotId = SlotScope.idOrNull();
        if (slotId != null && ClientRuntime.INSTANCE.viewports().hasViewport(slotId)) {
            return ClientRuntime.INSTANCE.viewports().viewport(slotId);
        }

        ClientInstance client = Client.currentOrNull();
        if (client == null || !ClientRuntime.INSTANCE.viewports().hasViewport(client.slotId())) {
            return null;
        }
        return ClientRuntime.INSTANCE.viewports().viewport(client.slotId());
    }
}
