package net.jr.client.runtime.options;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.state.OptionsState;

public final class OptionsStateResolver {
    private OptionsStateResolver() {
    }

    public static OptionsState current() {
        Integer slotId = SlotScope.idOrNull();
        return ClientRuntime.INSTANCE.slots().slot(slotId != null ? slotId : 0).optionsState();
    }
}
