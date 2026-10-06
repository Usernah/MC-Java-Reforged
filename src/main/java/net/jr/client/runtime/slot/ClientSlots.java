package net.jr.client.runtime.slot;

import java.util.List;

public final class ClientSlots {
    public static final int MAX_SLOTS = 4;

    private final Slots[] slots = new Slots[MAX_SLOTS];

    public ClientSlots() {
        for (int slotId = 0; slotId < MAX_SLOTS; slotId++) {
            this.slots[slotId] = new Slots(slotId);
        }
    }

    public Slots slot(int slotId) {
        requireSlotId(slotId);
        return this.slots[slotId];
    }

    public static final int PRIMARY_SLOT_ID = 0;

    public static boolean isPrimary(int slotId) {
        requireSlotId(slotId);
        return slotId == PRIMARY_SLOT_ID;
    }

    public static boolean isSecondary(int slotId) {
        return !isPrimary(slotId);
    }

    public List<Slots> all() {
        return List.of(this.slots);
    }

    public static void requireSlotId(int slotId) {
        if (slotId < 0 || slotId >= MAX_SLOTS) {
            throw new IndexOutOfBoundsException("Invalid local client slot id " + slotId);
        }
    }
    public Slots primary() {
        return this.slot(PRIMARY_SLOT_ID);
    }
}
