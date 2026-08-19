package net.jr.client.runtime.slot;

import java.util.List;

public final class LocalClientSlotRegistry {
    public static final int MAX_SLOTS = 4;

    private final LocalClientSlot[] slots = new LocalClientSlot[MAX_SLOTS];

    public LocalClientSlotRegistry() {
        for (int slotId = 0; slotId < MAX_SLOTS; slotId++) {
            this.slots[slotId] = new LocalClientSlot(slotId);
        }
    }

    public LocalClientSlot slot(int slotId) {
        requireSlotId(slotId);
        return this.slots[slotId];
    }

    public LocalClientSlot primary() {
        return this.slots[0];
    }

    public List<LocalClientSlot> all() {
        return List.of(this.slots);
    }

    public static void requireSlotId(int slotId) {
        if (slotId < 0 || slotId >= MAX_SLOTS) {
            throw new IndexOutOfBoundsException("Invalid local client slot id " + slotId);
        }
    }
}
