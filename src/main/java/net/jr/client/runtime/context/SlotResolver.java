package net.jr.client.runtime.context;

import net.jr.client.input.InputApi;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.slot.ClientSlots;
import net.minecraft.client.gui.screens.Screen;

public final class SlotResolver {
    private SlotResolver() {
    }

    public static Slots requireSlot(Slots slot) {
        if (slot == null) {
            throw new IllegalArgumentException("slot cannot be null");
        }
        return ClientRuntime.INSTANCE.slots().slot(slot.id());
    }

    public static Slots requireSlot(int slotId) {
        return ClientRuntime.INSTANCE.slots().slot(slotId);
    }

    public static Slots requirePrimary() {
        return ClientRuntime.INSTANCE.slots().primary();
    }

    public static Slots requireActive() {
        return SlotExecution.currentSlot();
    }

    public static Slots requireScreen(Screen screen) {
        if (screen == null) {
            throw new IllegalArgumentException("screen cannot be null");
        }

        Slots match = null;
        for (int slotId = 0; slotId < ClientSlots.MAX_SLOTS; slotId++) {
            Slots candidate = ClientRuntime.INSTANCE.slots().slot(slotId);
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

    public static Slots requireKeyboardMouseOwner() {
        return ClientRuntime.INSTANCE.slots().slot(InputApi.keyboardMouseSlotId());
    }
}
