package net.jr.client.runtime.render.tick;

import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClientExecution;
import net.minecraft.world.entity.Entity;

/** Updates each Client-owned camera while keeping GameRenderer as one shared engine. */
public final class CameraTicker {
    private CameraTicker() {
    }

    public static void tickConnectedClients() {
        for (int clientId = 0; clientId < LocalClientAcces.MAX_CLIENTS; clientId++) {
            if (!canTick(clientId)) {
                continue;
            }
            LocalClientExecution.runForClient(clientId, () -> LocalClientAcces.camera().tick());
        }
    }

    private static boolean canTick(int clientId) {
        Entity entity = LocalClientAcces.cameraEntity(clientId);
        return LocalClientAcces.connected(clientId)
            && LocalClientAcces.level(clientId) != null
            && (entity != null || LocalClientAcces.player(clientId) != null);
    }
}
