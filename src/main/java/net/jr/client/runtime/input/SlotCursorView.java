package net.jr.client.runtime.input;

import net.jr.client.runtime.input.LocalGamepadInputHandler;

public final class SlotCursorView {
    private final int slotId;

    public SlotCursorView(int slotId) {
        this.slotId = slotId;
    }

    public boolean hidden() {
        return LocalCursorVisibility.isHiddenForSlot(this.slotId);
    }

    public void setHidden(boolean hidden) {
        LocalCursorVisibility.setHiddenForSlot(this.slotId, hidden);
    }

    public void setReplacementHidden(boolean hidden) {
        LocalCursorVisibility.setReplacementHiddenForSlot(this.slotId, hidden);
    }

    public boolean joystickDriven() {
        return LocalGamepadInputHandler.isJoystickCursorActive(this.slotId);
    }

    public boolean virtual() {
        return LocalGamepadInputHandler.isVirtualCursorActive(this.slotId);
    }

    public double x() {
        return LocalGamepadInputHandler.cursorX(this.slotId);
    }

    public double y() {
        return LocalGamepadInputHandler.cursorY(this.slotId);
    }

    public double visualX() {
        return LocalGamepadInputHandler.visualCursorX(this.slotId);
    }

    public double visualY() {
        return LocalGamepadInputHandler.visualCursorY(this.slotId);
    }

    public void releaseFocusedSlot() {
        LocalGamepadInputHandler.releaseFocusedSlotCursor(this.slotId);
    }
}
