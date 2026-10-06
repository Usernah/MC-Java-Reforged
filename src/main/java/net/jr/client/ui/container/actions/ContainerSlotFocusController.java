package net.jr.client.ui.container.actions;

import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.ui.container.slots.SlotGrid;
import net.jr.client.ui.container.slots.SlotPos;
import net.jr.client.ui.container.slots.VanillaSlotLayer;
import net.jr.client.ui.container.slots.CustomSlotLayout;
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

public final class ContainerSlotFocusController {
    private ContainerSlotFocusController() {}

    public static boolean moveFocus(AbstractContainerScreen<?> screen, int keyCode) {
        if (screen instanceof CustomSlotLayout layout) {
            if (!layout.allowsSlotFocusNavigation()) {
                return false;
            }
            return moveFocus(screen, layout.slotGrid(), layout.slotLayer(), keyCode);
        }
        if (!SlotNavigationResolver.supportsKey(keyCode)) return false;
        List<Slot> slots = activeSlots(screen);
        if (slots.isEmpty()) return false;
        Slot current = focused(screen);
        if (current == null) {
            Slot initial = SlotNavigationResolver.initialSlot(slots, keyCode);
            setFocus(screen, initial);
            return true;
        }
        Slot origin = current;
        Slot next = SlotNavigationResolver.nextSlot(slots, origin, keyCode);
        if (next == null) {
            return screen instanceof CustomSlotLayout layout
                && layout.handleSlotNavigationBoundary(keyCode);
        }
        setFocus(screen, next);
        return true;
    }

    public static boolean moveFocus(
        AbstractContainerScreen<?> screen,
        SlotGrid grid,
        VanillaSlotLayer slotLayer,
        int keyCode
    ) {
        if (!SlotNavigationResolver.supportsKey(keyCode)) return false;

        List<Slot> slots = screen.getMenu().slots.stream()
            .filter(slot -> slot.isActive() && grid.has(slot.index))
            .toList();
        if (slots.isEmpty()) return false;

        Slot current = slotLayer.getFocusedSlot();
        Slot next = current == null
            ? SlotNavigationResolver.initialGridSlot(slots, grid, keyCode)
            : SlotNavigationResolver.nextGridSlot(slots, grid, current, keyCode);
        if (next == null) {
            return screen instanceof CustomSlotLayout layout
                && layout.handleSlotNavigationBoundary(keyCode);
        }

        slotLayer.setFocusedSlot(next);
        syncCursorToGridSlot(grid, next);
        return true;
    }

    public static boolean initializeFocusFromPointer(
        AbstractContainerScreen<?> screen, double mouseGuiX, double mouseGuiY
    ) {
        if (screen instanceof CustomSlotLayout layout) {
            if (layout.seedFocusFromPointer(mouseGuiX, mouseGuiY)) {
                return true;
            }
            if (!layout.allowsSlotFocusNavigation()) {
                return false;
            }
            return initializeFocusFromPointer(
                screen, layout.slotGrid(), layout.slotLayer(), mouseGuiX, mouseGuiY
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
        SlotGrid grid,
        VanillaSlotLayer slotLayer,
        double mouseGuiX,
        double mouseGuiY
    ) {
        Slot target = screen.getMenu().slots.stream()
            .filter(slot -> slot.isActive() && grid.has(slot.index))
            .min(Comparator.comparingDouble(slot -> {
                SlotPos position = grid.get(slot.index);
                double deltaX = position.x() + position.width() / 2.0D - mouseGuiX;
                double deltaY = position.y() + position.height() / 2.0D - mouseGuiY;
                return deltaX * deltaX + deltaY * deltaY;
            }))
            .orElse(null);
        if (target == null) return false;

        slotLayer.setFocusedSlot(target);
        syncCursorToGridSlot(grid, target);
        return true;
    }

    public static boolean hasFocusedSlot(AbstractContainerScreen<?> screen) {
        if (screen instanceof CustomSlotLayout layout) {
            return layout.slotLayer().hasFocusedSlot();
        }
        return focused(screen) != null;
    }

    public static boolean isVanillaSlotFocusActive(AbstractContainerScreen<?> screen) {
        return hasFocusedSlot(screen);
    }

    public static Integer getFocusedSlotIndex(AbstractContainerScreen<?> screen) {
        if (screen instanceof CustomSlotLayout layout) {
            return layout.slotLayer().getFocusedSlotId();
        }
        Slot slot = focused(screen);
        return slot == null ? null : slot.index;
    }

    public static FocusedSlotCenter getFocusedSlotCenter(AbstractContainerScreen<?> screen) {
        if (screen instanceof CustomSlotLayout layout) {
            Slot slot = layout.slotLayer().getFocusedSlot();
            SlotPos position = slot == null ? null : layout.slotGrid().get(slot.index);
            return position == null ? null : new FocusedSlotCenter(
                position.x() + position.width() / 2.0D,
                position.y() + position.height() / 2.0D
            );
        }
        Slot slot = focused(screen);
        if (slot == null) return null;
        AbstractContainerScreenAccessor access = accessor(screen);
        return new FocusedSlotCenter(
            access.javaReforged$getLeftPos() + slot.x + 8,
            access.javaReforged$getTopPos() + slot.y + 8
        );
    }

    public static void clearFocusedSlot(AbstractContainerScreen<?> screen) {
        if (screen instanceof CustomSlotLayout layout) {
            layout.slotLayer().clearFocusedSlot();
            return;
        }
        state().containerSlotFocus().remove(screen);
    }

    public static List<SlotFocusTarget> focusTargets(AbstractContainerScreen<?> screen) {
        if (screen instanceof CustomSlotLayout layout) {
            if (!layout.allowsSlotFocusNavigation()) {
                return List.of();
            }
            return screen.getMenu().slots.stream()
                .filter(slot -> slot.isActive() && layout.slotGrid().has(slot.index))
                .map(slot -> {
                    SlotPos position = layout.slotGrid().get(slot.index);
                    return new SlotFocusTarget(slot, new ScreenRectangle(
                        position.x(), position.y(), position.width(), position.height()
                    ));
                })
                .toList();
        }

        AbstractContainerScreenAccessor access = accessor(screen);
        int left = access.javaReforged$getLeftPos();
        int top = access.javaReforged$getTopPos();
        return activeSlots(screen).stream()
            .map(slot -> new SlotFocusTarget(
                slot,
                new ScreenRectangle(left + slot.x, top + slot.y, 16, 16)
            ))
            .toList();
    }

    public static boolean focusSlot(AbstractContainerScreen<?> screen, Slot slot) {
        if (screen instanceof CustomSlotLayout layout) {
            if (!layout.allowsSlotFocusNavigation() || !layout.slotGrid().has(slot.index)) {
                return false;
            }
            screen.clearFocus();
            layout.slotLayer().setFocusedSlot(slot);
            syncCursorToGridSlot(layout.slotGrid(), slot);
            return true;
        }
        if (!activeSlots(screen).contains(slot)) {
            return false;
        }
        setFocus(screen, slot);
        return true;
    }

    private static Slot focused(AbstractContainerScreen<?> screen) {
        Integer index = state().containerSlotFocus().get(screen);
        if (index == null || index < 0 || index >= screen.getMenu().slots.size()) return null;
        Slot slot = screen.getMenu().slots.get(index);
        if (!activeSlots(screen).contains(slot)) {
            state().containerSlotFocus().remove(screen);
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
        state().containerSlotFocus().put(screen, slot.index);
        FocusedSlotCenter center = getFocusedSlotCenter(screen);
        if (center != null) syncCursorToFocusedSlot(center.guiX, center.guiY);
    }

    private static AbstractContainerScreenAccessor accessor(AbstractContainerScreen<?> screen) {
        return (AbstractContainerScreenAccessor) screen;
    }

    private static UiNavigationState state() {
        Integer slotId = SlotScope.idOrNull();
        return ClientRuntime.INSTANCE.slots().slot(slotId != null ? slotId : 0).inputState().uiNavigation();
    }

    private static void syncCursorToGridSlot(SlotGrid grid, Slot slot) {
        syncCursorToFocusedSlot(
            SlotNavigationResolver.centerX(grid, slot),
            SlotNavigationResolver.centerY(grid, slot)
        );
    }

    private static void syncCursorToFocusedSlot(double guiX, double guiY) {
        if (UiInputModeController.isFocusNavigationActive()
            || GamepadInputHandler.isControllerCursorActive()) {
            GamepadInputHandler.moveVirtualCursorToFocusedSlot(guiX, guiY);
        }
    }

    public record FocusedSlotCenter(double guiX, double guiY) {}

    public record SlotFocusTarget(Slot slot, ScreenRectangle rectangle) {}

}
