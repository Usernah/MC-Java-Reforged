package net.jr.client.runtime.options;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.state.OptionsState;

public final class OptionsStateResolver {
    private OptionsStateResolver() {
    }

    public static OptionsState current() {
        var client = Client.currentOrNull();
        return (client != null ? client : Client.get(0)).options();
    }
}
