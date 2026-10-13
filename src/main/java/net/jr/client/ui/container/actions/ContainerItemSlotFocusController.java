package net.jr.client.ui.container.actions;

import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.ItemSlotPos;
import net.jr.client.ui.container.slots.VanillaItemSlotLayer;
import net.jr.client.ui.container.slots.CustomItemSlotLayout;
import net.jr.client.runtime.state.UiNavigationState;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.mixin.accessors.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.util.Comparator;
import java.util.List;

public final class ContainerItemSlotFocusController {
    private ContainerItemSlotFocusController() {}

    public static boolean moveFocus(AbstractContainerScreen<?> screen, int keyCode) {
        CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
        if (layout != null) {
            if (!layout.allowsItemSlotFocusNavigation()) {
                return false;
            }
            return moveFocus(screen, layout.itemSlotGrid(), layout.itemSlotLayer(), keyCode);
        }
        if (!ItemSlotNavigationResolver.supportsKey(keyCode)) return false;
        List<Slot> slots = activeSlots(screen);
        if (slots.isEmpty()) return false;
        Slot current = focused(screen);
        if (current == null) {
            Slot initial = ItemSlotNavigationResolver.initialSlot(slots, keyCode);
            setFocus(screen, initial);
            return true;
        }
        Slot origin = current;
        Slot next = ItemSlotNavigationResolver.nextSlot(slots, origin, keyCode);
        if (next == null) {
            return layout != null && layout.handleItemSlotNavigationBoundary(keyCode);
        }
        setFocus(screen, next);
        return true;
    }

    public static boolean moveFocus(
        AbstractContainerScreen<?> screen,
        ItemSlotGrid grid,
        VanillaItemSlotLayer itemSlotLayer,
        int keyCode
    ) {
        if (!ItemSlotNavigationResolver.supportsKey(keyCode)) return false;

        List<Slot> slots = screen.getMenu().slots.stream()
            .filter(slot -> slot.isActive() && grid.has(slot.index))
            .toList();
        if (slots.isEmpty()) return false;

        Slot current = itemSlotLayer.getFocusedItemSlot();
        Slot next = current == null
            ? ItemSlotNavigationResolver.initialGridSlot(slots, grid, keyCode)
            : ItemSlotNavigationResolver.nextGridSlot(slots, grid, current, keyCode);
        if (next == null) {
            CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
            return layout != null && layout.handleItemSlotNavigationBoundary(keyCode);
        }

        itemSlotLayer.setFocusedItemSlot(next);
        syncCursorToItemSlot(grid, next);
        return true;
    }

    public static boolean initializeFocusFromPointer(
        AbstractContainerScreen<?> screen, double mouseGuiX, double mouseGuiY
    ) {
        CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
        if (layout != null) {
            if (layout.seedFocusFromPointer(mouseGuiX, mouseGuiY)) {
                return true;
            }
            if (!layout.allowsItemSlotFocusNavigation()) {
                return false;
            }
            return initializeFocusFromPointer(
                screen, layout.itemSlotGrid(), layout.itemSlotLayer(), mouseGuiX, mouseGuiY
            );
        }
        List<Slot> slots = activeSlots(screen);
        if (slots.isEmpty()) return false;
        int left = accessor(screen).javaReforged$getLeftPos();
        int top = accessor(screen).javaReforged$getTopPos();
        Slot nearest = slots.stream().min(Comparator.comparingDouble(slot -> {
            double dx = left + slot.x + 8 - mouseGuiX;
            double dy = top + slot.y + 8 - mouseGuiY;
            return dx * dx + dy * dy;
        })).orElse(null);
        if (nearest == null) return false;
        setFocus(screen, nearest);
        return true;
    }

    public static boolean initializeFocusFromPointer(
        AbstractContainerScreen<?> screen,
        ItemSlotGrid grid,
        VanillaItemSlotLayer itemSlotLayer,
        double mouseGuiX,
        double mouseGuiY
    ) {
        Slot target = screen.getMenu().slots.stream()
            .filter(slot -> slot.isActive() && grid.has(slot.index))
            .min(Comparator.comparingDouble(slot -> {
                ItemSlotPos position = grid.get(slot.index);
                double deltaX = position.x() + position.width() / 2.0D - mouseGuiX;
                double deltaY = position.y() + position.height() / 2.0D - mouseGuiY;
                return deltaX * deltaX + deltaY * deltaY;
            }))
            .orElse(null);
        if (target == null) return false;

        itemSlotLayer.setFocusedItemSlot(target);
        syncCursorToItemSlot(grid, target);
        return true;
    }

    public static boolean hasFocusedItemSlot(AbstractContainerScreen<?> screen) {
        CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
        if (layout != null) {
            return layout.itemSlotLayer().hasFocusedItemSlot();
        }
        return focused(screen) != null;
    }

    public static boolean isVanillaItemSlotFocusActive(AbstractContainerScreen<?> screen) {
        return hasFocusedItemSlot(screen);
    }

    public static Integer getFocusedItemSlotIndex(AbstractContainerScreen<?> screen) {
        CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
        if (layout != null) {
            return layout.itemSlotLayer().getFocusedItemSlotId();
        }
        Slot slot = focused(screen);
        return slot == null ? null : slot.index;
    }

    public static FocusedItemSlotCenter getFocusedItemSlotCenter(AbstractContainerScreen<?> screen) {
        CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
        if (layout != null) {
            Slot slot = layout.itemSlotLayer().getFocusedItemSlot();
            ItemSlotPos position = slot == null ? null : layout.itemSlotGrid().get(slot.index);
            return position == null ? null : new FocusedItemSlotCenter(
                position.x() + position.width() / 2.0D,
                position.y() + position.height() / 2.0D
            );
        }
        Slot slot = focused(screen);
        if (slot == null) return null;
        AbstractContainerScreenAccessor access = accessor(screen);
        return new FocusedItemSlotCenter(
            access.javaReforged$getLeftPos() + slot.x + 8,
            access.javaReforged$getTopPos() + slot.y + 8
        );
    }

    public static void clearFocusedItemSlot(AbstractContainerScreen<?> screen) {
        CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
        if (layout != null) {
            layout.itemSlotLayer().clearFocusedItemSlot();
            return;
        }
        state().containerItemSlotFocus().remove(screen);
    }

    public static List<ItemSlotFocusTarget> focusTargets(AbstractContainerScreen<?> screen) {
        CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
        if (layout != null) {
            if (!layout.allowsItemSlotFocusNavigation()) {
                return List.of();
            }
            return screen.getMenu().slots.stream()
                .filter(slot -> slot.isActive() && layout.itemSlotGrid().has(slot.index))
                .map(slot -> {
                    ItemSlotPos position = layout.itemSlotGrid().get(slot.index);
                    return new ItemSlotFocusTarget(slot, new ScreenRectangle(
                        position.x(), position.y(), position.width(), position.height()
                    ));
                })
                .toList();
        }

        AbstractContainerScreenAccessor access = accessor(screen);
        int left = access.javaReforged$getLeftPos();
        int top = access.javaReforged$getTopPos();
        return activeSlots(screen).stream()
            .map(slot -> new ItemSlotFocusTarget(
                slot,
                new ScreenRectangle(left + slot.x, top + slot.y, 16, 16)
            ))
            .toList();
    }

    public static boolean focusItemSlot(AbstractContainerScreen<?> screen, Slot slot) {
        CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
        if (layout != null) {
            if (!layout.allowsItemSlotFocusNavigation() || !layout.itemSlotGrid().has(slot.index)) {
                return false;
            }
            screen.clearFocus();
            layout.itemSlotLayer().setFocusedItemSlot(slot);
            syncCursorToItemSlot(layout.itemSlotGrid(), slot);
            return true;
        }
        if (!activeSlots(screen).contains(slot)) {
            return false;
        }
        setFocus(screen, slot);
        return true;
    }

    private static Slot focused(AbstractContainerScreen<?> screen) {
        Integer index = state().containerItemSlotFocus().get(screen);
        if (index == null || index < 0 || index >= screen.getMenu().slots.size()) return null;
        Slot slot = screen.getMenu().slots.get(index);
        if (!activeSlots(screen).contains(slot)) {
            state().containerItemSlotFocus().remove(screen);
            return null;
        }
        return slot;
    }

    private static List<Slot> activeSlots(AbstractContainerScreen<?> screen) {
        AbstractContainerScreenAccessor access = accessor(screen);
        int width = access.javaReforged$getImageWidth();
        int height = access.javaReforged$getImageHeight();
        return screen.getMenu().slots.stream()
            .filter(Slot::isActive)
            .filter(slot -> slot.x >= 0 && slot.y >= 0 && slot.x <= width - 16 && slot.y <= height - 16)
            .toList();
    }

    private static void setFocus(AbstractContainerScreen<?> screen, Slot slot) {
        // A vanilla container slot and a screen widget must not both own controller focus.
        screen.clearFocus();
        state().containerItemSlotFocus().put(screen, slot.index);
        FocusedItemSlotCenter center = getFocusedItemSlotCenter(screen);
        if (center != null) syncCursorToFocusedItemSlot(center.guiX, center.guiY);
    }

    private static AbstractContainerScreenAccessor accessor(AbstractContainerScreen<?> screen) {
        return (AbstractContainerScreenAccessor) screen;
    }

    private static UiNavigationState state() {
        Integer slotId = SlotScope.idOrNull();
        return ClientRuntime.INSTANCE.slots().slot(slotId != null ? slotId : 0).inputState().uiNavigation();
    }

    private static void syncCursorToItemSlot(ItemSlotGrid grid, Slot slot) {
        syncCursorToFocusedItemSlot(
            ItemSlotNavigationResolver.centerX(grid, slot),
            ItemSlotNavigationResolver.centerY(grid, slot)
        );
    }

    private static void syncCursorToFocusedItemSlot(double guiX, double guiY) {
        if (UiInputModeController.isFocusNavigationActive()
            || GamepadInputHandler.isControllerCursorActive()) {
            GamepadInputHandler.moveVirtualCursorToFocusedItemSlot(guiX, guiY);
        }
    }

    public record FocusedItemSlotCenter(double guiX, double guiY) {}

    public record ItemSlotFocusTarget(Slot slot, ScreenRectangle rectangle) {}

}
