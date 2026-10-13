package net.jr.client.ui.container.slots;

import net.jr.client.ui.cursor.CursorMagnetTarget;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public interface CustomItemSlotLayout {
    static @Nullable CustomItemSlotLayout active(Object candidate) {
        return candidate instanceof CustomItemSlotLayout layout && layout.isItemSlotLayoutActive()
            ? layout
            : null;
    }

    ItemSlotGrid itemSlotGrid();

    VanillaItemSlotLayer itemSlotLayer();

    default boolean isItemSlotLayoutActive() {
        return true;
    }

    default boolean allowsItemSlotFocusNavigation() {
        return true;
    }

    default boolean seedFocusFromPointer(double guiX, double guiY) {
        return false;
    }

    default void clearFocusForPointer() {
    }

    default boolean handleItemSlotNavigationBoundary(int keyCode) {
        return false;
    }

    default @Nullable CursorMagnetTarget cursorMagnetTargetAt(double guiX, double guiY) {
        Slot slot = this.itemSlotLayer().getItemSlotAt(this.itemSlotGrid(), guiX, guiY);
        if (slot == null) {
            return null;
        }
        ItemSlotPos position = this.itemSlotGrid().get(slot.index);
        return position == null ? null : new CursorMagnetTarget(
                position.x() + position.width() / 2.0D,
                position.y() + position.height() / 2.0D
        );
    }
}
