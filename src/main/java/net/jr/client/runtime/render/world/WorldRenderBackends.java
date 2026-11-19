package net.jr.client.runtime.render.world;

import net.jr.bootstrap.OptionalModPresence;
import net.jr.client.compat.sodium.SodiumWorldRenderBackend;

public final class WorldRenderBackends {
    private static final WorldRenderBackend ACTIVE = OptionalModPresence.sodiumLoaded()
        ? new SodiumWorldRenderBackend()
        : new NativeWorldRenderBackend();

    private WorldRenderBackends() {
    }

    public static WorldRenderBackend active() {
        return ACTIVE;
    }

    public static void resourcesReloaded() {
        ACTIVE.resourcesReloaded();
    }

    public static SodiumWorldRenderBackend sodium() {
        if (ACTIVE instanceof SodiumWorldRenderBackend sodium) {
            return sodium;
        }
        throw new IllegalStateException("The active world render backend is not Sodium");
    }
}
