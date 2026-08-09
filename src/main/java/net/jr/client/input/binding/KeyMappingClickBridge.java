package net.jr.client.input.binding;

import net.jr.mixin.controls.KeyMappingClickAccessor;
import net.minecraft.client.KeyMapping;

public final class KeyMappingClickBridge {
    private KeyMappingClickBridge() {
    }

    public static void increment(KeyMapping keyMapping) {
        KeyMappingClickAccessor accessor = (KeyMappingClickAccessor) (Object) keyMapping;
        accessor.javareforged$setClickCount(accessor.javareforged$getClickCount() + 1);
    }
}

