package net.jr.ClientRuntime.runtime;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;

/** Per-Client FOV smoothing driven from the single high-level GameRenderer tick. */
public final class Fovs {
    private Fovs() {
    }

    public static void tickConnectedClients() {
        for (int clientId = 0; clientId < Client.MAX_CLIENTS; clientId++) {
            if (!Client.connected(clientId) || Client.level(clientId) == null || Client.player(clientId) == null) {
                continue;
            }
            ClientBoundary.runForClient(clientId, Fovs::tickCurrentClient);
        }
    }

    private static void tickCurrentClient() {
        float targetModifier = 1.0F;
        Entity cameraEntity = Client.cameraEntity();
        if (cameraEntity == null) {
            cameraEntity = Client.player();
        }
        if (cameraEntity instanceof AbstractClientPlayer player) {
            Minecraft minecraft = Minecraft.getInstance();
            targetModifier = player.getFieldOfViewModifier(
                minecraft.options.getCameraType().isFirstPerson(),
                minecraft.options.fovEffectScale().get().floatValue()
            );
        }
        Client.render().fovState().tick(targetModifier);
    }
}
