package net.jr.client.input;

import net.jr.client.input.cursor.CursorHider;
import net.jr.client.input.runtime.GamepadInputProcessor;

public final class SlotCursorView {
    private final int slotId;

    public SlotCursorView(int slotId) {
        this.slotId = slotId;
    }

    public boolean hidden() {
        return CursorHider.isHiddenForSlot(this.slotId);
    }

    public void setHidden(boolean hidden) {
        CursorHider.setHiddenForSlot(this.slotId, hidden);
    }

    public void setReplacementHidden(boolean hidden) {
        CursorHider.setReplacementHiddenForSlot(this.slotId, hidden);
    }

    public boolean joystickDriven() {
        return GamepadInputProcessor.isJoystickCursorActive(this.slotId);
    }

    public boolean virtual() {
        return GamepadInputProcessor.isVirtualCursorActive(this.slotId);
    }

    public double x() {
        return GamepadInputProcessor.cursorX(this.slotId);
    }

    public double y() {
        return GamepadInputProcessor.cursorY(this.slotId);
    }

    public double visualX() {
        return GamepadInputProcessor.visualCursorX(this.slotId);
    }

    public double visualY() {
        return GamepadInputProcessor.visualCursorY(this.slotId);
    }

    public void releaseFocusedSlot() {
        GamepadInputProcessor.releaseFocusedSlotCursor(this.slotId);
    }
}
