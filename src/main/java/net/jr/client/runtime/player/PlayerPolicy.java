package net.jr.client.runtime.player;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.client.ClientInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public final class PlayerPolicy {
    private PlayerPolicy() {
    }

    public static boolean isControlledCamera(LocalPlayer player) {
        ClientInstance client = Client.current();
        Entity cameraEntity = client.cameraEntity();
        Entity expectedCamera = cameraEntity != null ? cameraEntity : client.player();
        return expectedCamera == player;
    }

    public static boolean shouldRetainDeadClientPlayer(LocalPlayer player) {
        ClientInstance client = Client.currentOrNull();
        if (client == null || client.player() != player) {
            return false;
        }
        Entity cameraEntity = client.cameraEntity();
        return cameraEntity == null || cameraEntity == player;
    }
}
