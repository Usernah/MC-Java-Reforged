package net.jr.ClientRuntime.runtime;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public final class LocalPlayerRuntime {
    private LocalPlayerRuntime() {
    }

    public static boolean isControlledCamera(LocalPlayer player) {
        Entity cameraEntity = Client.cameraEntity();
        Entity expectedCamera = cameraEntity != null ? cameraEntity : Client.player();
        return expectedCamera == player;
    }

    public static boolean shouldRetainDeadClientPlayer(LocalPlayer player) {
        LocalClient client = Client.currentOrNull();
        if (client == null || client.player() != player) {
            return false;
        }
        Entity cameraEntity = client.cameraEntity();
        return cameraEntity == null || cameraEntity == player;
    }
}
