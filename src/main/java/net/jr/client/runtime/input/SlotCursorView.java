package net.jr.client.runtime.input;

public final class SlotCursorView {
    private final int slotId;

    public SlotCursorView(int slotId) {
        this.slotId = slotId;
    }

    public boolean hidden() {
        return CursorVisibility.isHiddenForSlot(this.slotId);
    }

    public void setHidden(boolean hidden) {
        CursorVisibility.setHiddenForSlot(this.slotId, hidden);
    }

    public void setReplacementHidden(boolean hidden) {
        CursorVisibility.setReplacementHiddenForSlot(this.slotId, hidden);
    }

    public boolean joystickDriven() {
        return GamepadInputHandler.isJoystickCursorActive(this.slotId);
    }

    public boolean virtual() {
        return GamepadInputHandler.isVirtualCursorActive(this.slotId);
    }

    public double x() {
        return GamepadInputHandler.cursorX(this.slotId);
    }

    public double y() {
        return GamepadInputHandler.cursorY(this.slotId);
    }

    public double visualX() {
        return GamepadInputHandler.visualCursorX(this.slotId);
    }

    public double visualY() {
        return GamepadInputHandler.visualCursorY(this.slotId);
    }

    public void releaseFocusedSlot() {
        GamepadInputHandler.releaseFocusedSlotCursor(this.slotId);
    }
}
