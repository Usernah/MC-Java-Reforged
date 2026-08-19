package net.jr.api.client.local;

import net.jr.client.runtime.ClientRuntime;

public final class LocalClients {
    private LocalClients() {
    }

    public static int count() {
        return ClientRuntime.INSTANCE.clients().count();
    }
}
