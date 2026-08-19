package net.jr.client.runtime.render.tick;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientExecution;
import net.minecraft.client.renderer.ItemInHandRenderer;

public final class HandRendererTicker {
    private HandRendererTicker() {
    }

    public static void tickClients(ItemInHandRenderer renderer) {
        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            if (!LocalClientReadinessPolicy.worldBound(client)) {
                continue;
            }
            LocalClientExecution.runForClient(client.slotId(), renderer::tick);
        }
    }
}
