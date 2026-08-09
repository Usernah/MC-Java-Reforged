package net.jr.client.input.binding;

import net.jr.client.input.InputApi;
import net.minecraft.client.KeyMapping;

public final class KeyMappingClickBridge {
    private KeyMappingClickBridge() {
    }

    public static void increment(KeyMapping keyMapping) {
        InputApi.click(keyMapping);
    }
}

