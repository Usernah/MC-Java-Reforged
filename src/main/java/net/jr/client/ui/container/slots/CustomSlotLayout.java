package net.jr.client.ui.container.slots;

import net.jr.client.ui.cursor.CursorMagnetTarget;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public interface CustomSlotLayout {
    SlotGrid slotGrid();

    VanillaSlotLayer slotLayer();

    default boolean allowsSlotFocusNavigation() {
        return true;
    }

    default boolean seedFocusFromPointer(double guiX, double guiY) {
        return false;
    }

    default void clearFocusForPointer() {
    }

    default boolean handleSlotNavigationBoundary(int keyCode) {
        return false;
    }

    default @Nullable CursorMagnetTarget cursorMagnetTargetAt(double guiX, double guiY) {
        Slot slot = this.slotLayer().getSlotAt(this.slotGrid(), guiX, guiY);
        if (slot == null) {
            return null;
        }
        SlotPos position = this.slotGrid().get(slot.index);
        return position == null ? null : new CursorMagnetTarget(
                position.x() + position.width() / 2.0D,
                position.y() + position.height() / 2.0D
        );
    }
}
