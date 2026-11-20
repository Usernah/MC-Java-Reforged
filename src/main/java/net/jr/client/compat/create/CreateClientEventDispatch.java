package net.jr.client.compat.create;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;

public final class CreateClientEventDispatch {
    private CreateClientEventDispatch() {
    }

    public static void runEveryClient(Runnable action) {
        boolean ran = false;
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.player() == null || client.level() == null) {
                continue;
            }
            client.slot().runForSlot(action);
            ran = true;
        }
        if (!ran) {
            ClientRuntime.INSTANCE.slots().primary().runForSlot(action);
        }
    }
}
