package net.jr.client.runtime.render.tick;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.jr.client.runtime.context.InstanceExecution;
import net.minecraft.client.renderer.ItemInHandRenderer;

public final class HandRendererTicker {
    private HandRendererTicker() {
    }

    public static void tickClients(ItemInHandRenderer renderer) {
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (!ReadinessPolicy.worldBound(client)) {
                continue;
            }
            InstanceExecution.runForClient(client.slotId(), renderer::tick);
        }
    }
}
