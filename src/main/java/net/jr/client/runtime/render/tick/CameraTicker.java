package net.jr.client.runtime.render.tick;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientExecution;
import net.minecraft.world.entity.Entity;

public final class CameraTicker {
    private CameraTicker() {
    }

    public static void tickConnectedClients() {
        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            if (!canTick(client)) {
                continue;
            }
            LocalClientExecution.runForClient(client.slotId(), () -> client.camera().tick());
        }
    }

    private static boolean canTick(LocalClient client) {
        Entity entity = client.cameraEntity();
        return client.level() != null && (entity != null || client.player() != null);
    }
}
