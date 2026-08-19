package net.jr.client.runtime.input;

import java.util.List;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;

public final class InputFocus {
    private final int slotCount;
    private int focusedSlotId;

    public InputFocus(int slotCount) {
        if (slotCount <= 0) {
            throw new IllegalArgumentException("slotCount must be positive");
        }
        this.slotCount = slotCount;
    }

    public int focusedSlotId() {
        return this.focusedSlotId;
    }

    public void setFocusedSlotId(int focusedSlotId) {
        this.requireSlot(focusedSlotId);
        this.focusedSlotId = focusedSlotId;
    }

    public void focusNext(List<Integer> slotIds) {
        List<Integer> ids = this.requireSlotIds(slotIds);
        int index = ids.indexOf(this.focusedSlotId);
        this.focusedSlotId = index < 0 ? ids.getFirst() : ids.get((index + 1) % ids.size());
    }

    public void focusPrevious(List<Integer> slotIds) {
        List<Integer> ids = this.requireSlotIds(slotIds);
        int index = ids.indexOf(this.focusedSlotId);
        this.focusedSlotId = index < 0
            ? ids.getFirst()
            : ids.get((index + ids.size() - 1) % ids.size());
    }

    public void clampToSlots(List<Integer> slotIds) {
        List<Integer> ids = this.requireSlotIds(slotIds);
        if (!ids.contains(this.focusedSlotId)) {
            this.focusedSlotId = ids.getFirst();
        }
    }

    private List<Integer> requireSlotIds(List<Integer> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) {
            return List.of(0);
        }
        List<Integer> ids = List.copyOf(slotIds);
        for (int slotId : ids) {
            this.requireSlot(slotId);
        }
        return ids;
    }

    private void requireSlot(int slotId) {
        if (slotId < 0 || slotId >= this.slotCount || slotId >= LocalClientSlotRegistry.MAX_SLOTS) {
            throw new IndexOutOfBoundsException("Focused slot id " + slotId + " is not valid");
        }
    }
}
