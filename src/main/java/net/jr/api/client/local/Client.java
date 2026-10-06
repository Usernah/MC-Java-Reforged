package net.jr.api.client.local;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.slot.Slots;
import net.minecraft.client.player.LocalPlayer;

import javax.annotation.Nullable;

public final class Client {
    private Client() {
    }

    @Nullable
    public static ClientInstance currentOrNull() {
        Slots slot = SlotExecution.currentSlotOrNull();
        return slot == null
                ? null
                : ClientRuntime.INSTANCE.clients().clientOrNull(slot.id());
    }


    public static ClientInstance current() {
        return get(SlotExecution.currentSlot().id());
    }

    public static ClientInstance get(int slotId) {
        return ClientRuntime.INSTANCE.clients().client(slotId);
    }

    public static int count() {
        return ClientRuntime.INSTANCE.clients().count();
    }

    public static int slotId() {
        return current().slotId();
    }

    public static int slotId(LocalPlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }

        return ClientRuntime.INSTANCE.clients().all().stream()
                .filter(client -> client.player() == player)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Local player is not bound to a slot")
                )
                .slotId();
    }
}
