package net.jr.client.runtime.input;

import java.util.Set;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.IInputDeviceRegistry;

/** Internal lifecycle operations absent from the read-only public registry. */
interface ManagedInputDeviceRegistry extends IInputDeviceRegistry {
    void evictDisconnectedExcept(Set<IGamepad> retained);
}
