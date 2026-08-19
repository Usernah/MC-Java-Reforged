package net.jr.client.runtime.player;

import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientScope;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public final class LocalPlayerPolicy {
    private LocalPlayerPolicy() {
    }

    public static boolean isControlledCamera(LocalPlayer player) {
        LocalClient client = LocalClientScope.currentClient();
        Entity cameraEntity = client.cameraEntity();
        Entity expectedCamera = cameraEntity != null ? cameraEntity : client.player();
        return expectedCamera == player;
    }

    public static boolean shouldRetainDeadClientPlayer(LocalPlayer player) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        if (client == null || client.player() != player) {
            return false;
        }
        Entity cameraEntity = client.cameraEntity();
        return cameraEntity == null || cameraEntity == player;
    }
}
