package net.jr.client.compat.create;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.context.InstanceExecution;

public final class CreateClientEventDispatch {
    private CreateClientEventDispatch() {
    }

    public static void runEveryClient(Runnable action) {
        boolean ran = false;
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.player() == null || client.level() == null) {
                continue;
            }
            InstanceExecution.runForClient(client.slotId(), action);
            ran = true;
        }
        if (!ran) {
            InstanceExecution.runPrimary(action);
        }
    }
}
