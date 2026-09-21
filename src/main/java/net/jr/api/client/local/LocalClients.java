package net.jr.api.client.local;

import net.jr.client.runtime.ClientRuntime;
import net.jr.api.client.input.ISlotInput;
import net.minecraft.client.player.LocalPlayer;

public final class LocalClients {
    private LocalClients() {
    }

    public static int count() {
        return ClientRuntime.INSTANCE.clients().count();
    }

    public static ISlotInput input(int slotId) {
        return ClientRuntime.INSTANCE.slots().slot(slotId).input();
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
