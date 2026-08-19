package net.jr.client.ui.container.slots;

import java.util.Arrays;

public final class SlotSlice {
    private final SlotGroup group;
    private final int[] slots;

    public SlotSlice(SlotGroup group, int[] slots) {
        this.group = group;
        this.slots = Arrays.copyOf(slots, slots.length);
    }

    public SlotGroup group() {
        return group;
    }

    public int size() {
        return slots.length;
    }

    public int slot(int index) {
        return slots[index];
    }

    public int[] slots() {
        return Arrays.copyOf(slots, slots.length);
    }

    public boolean contains(int slotId) {
        for (int slot : slots) {
            if (slot == slotId) {
                return true;
            }
        }
        return false;
    }
}

