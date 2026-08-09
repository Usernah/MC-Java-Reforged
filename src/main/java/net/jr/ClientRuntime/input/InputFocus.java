package net.jr.ClientRuntime.input;

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
        if (focusedSlotId < 0 || focusedSlotId >= this.slotCount) {
            throw new IndexOutOfBoundsException("Focused slot id " + focusedSlotId + " is not valid");
        }
        this.focusedSlotId = focusedSlotId;
    }

    public void focusNext() {
        this.focusNext(this.slotCount);
    }

    public void focusNext(int activeSlotCount) {
        int count = this.normalizedSlotCount(activeSlotCount);
        this.focusedSlotId = (this.focusedSlotId + 1) % count;
    }

    public void focusPrevious() {
        this.focusPrevious(this.slotCount);
    }

    public void focusPrevious(int activeSlotCount) {
        int count = this.normalizedSlotCount(activeSlotCount);
        this.focusedSlotId = (this.focusedSlotId + count - 1) % count;
    }

    public void clampToSlotCount(int activeSlotCount) {
        int count = this.normalizedSlotCount(activeSlotCount);
        if (this.focusedSlotId >= count) {
            this.focusedSlotId = count - 1;
        }
        if (this.focusedSlotId < 0) {
            this.focusedSlotId = 0;
        }
    }

    private int normalizedSlotCount(int activeSlotCount) {
        if (activeSlotCount <= 0) {
            return 1;
        }
        return Math.min(activeSlotCount, this.slotCount);
    }
}
