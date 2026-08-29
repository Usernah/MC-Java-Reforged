package net.jr.api.client.local;

import net.jr.client.runtime.ClientRuntime;
import net.minecraft.client.player.LocalPlayer;

public final class LocalClients {
    private LocalClients() {
    }

    public static int count() {
        return ClientRuntime.INSTANCE.clients().count();
    }

    public static int slotId(LocalPlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        return ClientRuntime.INSTANCE.clients().all().stream()
            .filter(client -> client.player() == player)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Local player is not bound to a slot"))
            .slotId();
    }
}
