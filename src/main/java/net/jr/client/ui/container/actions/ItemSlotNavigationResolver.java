package net.jr.client.ui.container.actions;

import java.util.List;
import net.jr.client.ui.navigation.DirectionalFocusResolver;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.ItemSlotGroup;
import net.jr.client.ui.container.slots.ItemSlotPos;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.inventory.Slot;

final class ItemSlotNavigationResolver {
    private ItemSlotNavigationResolver() {
    }

    static boolean supportsKey(int keyCode) {
        return DirectionalFocusResolver.supportsKey(keyCode);
    }

    static Slot initialSlot(List<Slot> slots, int keyCode) {
        return DirectionalFocusResolver.initialTarget(
            slots,
            ItemSlotNavigationResolver::rectangle,
            keyCode
        );
    }

    static Slot nextSlot(List<Slot> slots, Slot origin, int keyCode) {
        return DirectionalFocusResolver.nextTarget(
            slots,
            origin,
            ItemSlotNavigationResolver::rectangle,
            keyCode
        );
    }

    static Slot initialGridSlot(List<Slot> slots, ItemSlotGrid grid, int keyCode) {
        return DirectionalFocusResolver.initialTarget(
            slots,
            slot -> rectangle(grid, slot),
            keyCode
        );
    }

    static Slot nextGridSlot(
        List<Slot> slots,
        ItemSlotGrid grid,
        Slot current,
        int keyCode
    ) {
        ItemSlotGroup currentGroup = grid.groupOf(current.index);
        if (currentGroup != null) {
            List<Slot> groupSlots = slots.stream()
                .filter(slot -> grid.groupOf(slot.index) == currentGroup)
                .toList();
            Slot target = DirectionalFocusResolver.nextTarget(
                groupSlots,
                current,
                slot -> rectangle(grid, slot),
                keyCode
            );
            if (target != null) {
                return target;
            }
        }
        return DirectionalFocusResolver.nextTarget(
            slots,
            current,
            slot -> rectangle(grid, slot),
            keyCode
        );
    }

    static double centerX(ItemSlotGrid grid, Slot slot) {
        ItemSlotPos position = grid.get(slot.index);
        return position.x() + position.width() / 2.0D;
    }

    static double centerY(ItemSlotGrid grid, Slot slot) {
        ItemSlotPos position = grid.get(slot.index);
        return position.y() + position.height() / 2.0D;
    }

    private static ScreenRectangle rectangle(Slot slot) {
        return new ScreenRectangle(slot.x, slot.y, 16, 16);
    }

    private static ScreenRectangle rectangle(ItemSlotGrid grid, Slot slot) {
        ItemSlotPos position = grid.get(slot.index);
        return new ScreenRectangle(
            position.x(), position.y(), position.width(), position.height()
        );
    }
}
