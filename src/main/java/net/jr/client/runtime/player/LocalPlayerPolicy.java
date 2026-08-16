package net.jr.client.runtime.player;

import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public final class LocalPlayerPolicy {
    private LocalPlayerPolicy() {
    }

    public static boolean isControlledCamera(LocalPlayer player) {
        Entity cameraEntity = LocalClientAcces.cameraEntity();
        Entity expectedCamera = cameraEntity != null ? cameraEntity : LocalClientAcces.player();
        return expectedCamera == player;
    }

    public static boolean shouldRetainDeadClientPlayer(LocalPlayer player) {
        LocalClient client = LocalClientAcces.currentOrNull();
        if (client == null || client.player() != player) {
            return false;
        }
        Entity cameraEntity = client.cameraEntity();
        return cameraEntity == null || cameraEntity == player;
    }
}
