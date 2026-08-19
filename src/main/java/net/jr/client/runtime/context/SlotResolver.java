package net.jr.client.runtime.context;

import net.jr.client.input.InputApi;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.minecraft.client.gui.screens.Screen;

public final class SlotResolver {
    private SlotResolver() {
    }

    public static LocalClientSlot requireSlot(LocalClientSlot slot) {
        if (slot == null) {
            throw new IllegalArgumentException("slot cannot be null");
        }
        return ClientRuntime.INSTANCE.slots().slot(slot.id());
    }

    public static LocalClientSlot requireSlot(int slotId) {
        return ClientRuntime.INSTANCE.slots().slot(slotId);
    }

    public static LocalClientSlot requirePrimary() {
        return ClientRuntime.INSTANCE.slots().primary();
    }

    public static LocalClientSlot requireActive() {
        return ClientRuntime.INSTANCE.slots().slot(SlotScope.requireId());
    }

    public static LocalClientSlot requireScreen(Screen screen) {
        if (screen == null) {
            throw new IllegalArgumentException("screen cannot be null");
        }

        LocalClientSlot match = null;
        for (int slotId = 0; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            LocalClientSlot candidate = ClientRuntime.INSTANCE.slots().slot(slotId);
            if (candidate.screenState().screen() != screen) {
                continue;
            }
            if (match != null) {
                throw new IllegalStateException("Screen is bound to more than one local slot: " + screen);
            }
            match = candidate;
        }

        if (match == null) {
            throw new IllegalStateException("Screen is not bound to any local slot: " + screen);
        }
        return match;
    }

    public static LocalClientSlot requireKeyboardMouseOwner() {
        return ClientRuntime.INSTANCE.slots().slot(InputApi.keyboardMouseSlotId());
    }
}
