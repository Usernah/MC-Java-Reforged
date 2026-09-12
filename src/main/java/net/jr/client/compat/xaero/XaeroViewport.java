package net.jr.client.compat.xaero;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientScope;
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
        LocalClient client = LocalClientScope.currentClientOrNull();
        if (client == null || !ClientRuntime.INSTANCE.viewports().hasViewport(client.slotId())) {
            return null;
        }
        return ClientRuntime.INSTANCE.viewports().viewport(client.slotId());
    }
}
