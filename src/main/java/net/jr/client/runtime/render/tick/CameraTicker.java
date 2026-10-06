package net.jr.client.runtime.render.tick;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.context.InstanceExecution;
import net.minecraft.world.entity.Entity;

public final class CameraTicker {
    private CameraTicker() {
    }

    public static void tickConnectedClients() {
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (!canTick(client)) {
                continue;
            }
            InstanceExecution.runForClient(client.slotId(), () -> client.camera().tick());
        }
    }

    private static boolean canTick(ClientInstance client) {
        Entity entity = client.cameraEntity();
        return client.level() != null && (entity != null || client.player() != null);
    }
}
