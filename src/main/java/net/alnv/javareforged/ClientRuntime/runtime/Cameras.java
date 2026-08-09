package net.alnv.javareforged.ClientRuntime.runtime;

import net.minecraft.world.entity.Entity;

/** Updates each Client-owned camera while keeping GameRenderer as one shared engine. */
public final class Cameras {
    private Cameras() {
    }

    public static void tickConnectedClients() {
        for (int clientId = 0; clientId < Client.MAX_CLIENTS; clientId++) {
            if (!canTick(clientId)) {
                continue;
            }
            ClientBoundary.runForClient(clientId, () -> Client.camera().tick());
        }
    }

    private static boolean canTick(int clientId) {
        Entity entity = Client.cameraEntity(clientId);
        return Client.connected(clientId)
            && Client.level(clientId) != null
            && (entity != null || Client.player(clientId) != null);
    }
}
