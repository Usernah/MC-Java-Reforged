package net.jr.client.ui.container.actions;

import net.jr.client.input.runtime.GamepadInputProcessor;
import net.jr.client.ui.container.slots.SlotGrid;
import net.jr.client.ui.container.slots.SlotPos;
import net.jr.client.ui.container.slots.VanillaSlotLayer;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.mixin.controlhints.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Controller focus for vanilla container slots and custom JR slot grids. */
public final class ContainerSlotFocusController {
    private static final Map<AbstractContainerScreen<?>, Integer> FOCUS = new WeakHashMap<>();
    private ContainerSlotFocusController() {}

    public static boolean moveFocus(AbstractContainerScreen<?> screen, int keyCode) {
        Direction direction = Direction.fromKey(keyCode);
        if (direction == null) return false;
        List<Slot> slots = activeSlots(screen);
        if (slots.isEmpty()) return false;
        Slot current = focused(screen);
        if (current == null) {
            Slot initial = initialSlot(slots, direction);
            setFocus(screen, initial);
            return true;
        }
        Slot origin = current;
        Slot next = nextSlot(slots, origin, direction);
        if (next == null) return false;
        setFocus(screen, next);
        return true;
    }

    public static boolean moveFocus(
        AbstractContainerScreen<?> screen,
        SlotGrid grid,
        VanillaSlotLayer slotLayer,
        int keyCode
    ) {
        Direction direction = Direction.fromKey(keyCode);
        if (direction == null) return false;

        List<Slot> slots = screen.getMenu().slots.stream()
            .filter(slot -> slot.isActive() && grid.has(slot.index))
            .toList();
        if (slots.isEmpty()) return false;

        Slot current = slotLayer.getFocusedSlot();
        Slot next = current == null
            ? initialGridSlot(slots, grid, direction)
            : nextGridSlot(slots, grid, current, direction);
        if (next == null) return false;

        slotLayer.setFocusedSlot(next);
        syncCursorToGridSlot(grid, next);
        return true;
    }

    public static boolean initializeFocusFromPointer(
        AbstractContainerScreen<?> screen, double mouseGuiX, double mouseGuiY
    ) {
        List<Slot> slots = activeSlots(screen);
        if (slots.isEmpty()) return false;
        int left = accessor(screen).javareforged$getLeftPos();
        int top = accessor(screen).javareforged$getTopPos();
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
                double deltaX = position.x() + 8.0D - mouseGuiX;
                double deltaY = position.y() + 8.0D - mouseGuiY;
                return deltaX * deltaX + deltaY * deltaY;
            }))
            .orElse(null);
        if (target == null) return false;

        slotLayer.setFocusedSlot(target);
        syncCursorToGridSlot(grid, target);
        return true;
    }

    public static boolean hasFocusedSlot(AbstractContainerScreen<?> screen) {
        return focused(screen) != null;
    }

    public static boolean isVanillaSlotFocusActive(AbstractContainerScreen<?> screen) {
        return hasFocusedSlot(screen);
    }

    public static Integer getFocusedSlotIndex(AbstractContainerScreen<?> screen) {
        Slot slot = focused(screen);
        return slot == null ? null : slot.index;
    }

    public static FocusedSlotCenter getFocusedSlotCenter(AbstractContainerScreen<?> screen) {
        Slot slot = focused(screen);
        if (slot == null) return null;
        AbstractContainerScreenAccessor access = accessor(screen);
        return new FocusedSlotCenter(
            access.javareforged$getLeftPos() + slot.x + 8,
            access.javareforged$getTopPos() + slot.y + 8
        );
    }

    public static void clearFocusedSlot(AbstractContainerScreen<?> screen) {
        FOCUS.remove(screen);
    }

    public static boolean pickupFocusedOrHoveredSlot(AbstractContainerScreen<?> screen, int mouseButton) {
        return click(screen, actionSlot(screen, false), mouseButton, ContainerInput.PICKUP);
    }

    public static boolean quickMoveFocusedOrHoveredSlot(AbstractContainerScreen<?> screen) {
        if (!hasLocalInventoryAccess() || !screen.getMenu().getCarried().isEmpty()) return false;
        Slot target = actionSlot(screen, true);
        if (target == null) return false;
        if (screen instanceof CreativeModeInventoryScreen) {
            return CreativeContainerActions.quickMoveCreativeSlot(screen, target);
        }
        return click(screen, target, 0, ContainerInput.QUICK_MOVE);
    }

    public static boolean takeAllFromContainer(AbstractContainerScreen<?> screen) {
        if (!hasLocalInventoryAccess() || !screen.getMenu().getCarried().isEmpty()) return false;
        int containerSlotCount = containerSlotCount(screen);
        if (containerSlotCount < 0) return false;

        int playerSlotEnd = screen.getMenu().slots.size();
        int hotbarStart = Math.max(containerSlotCount, playerSlotEnd - 9);
        List<Integer> hotbarSlots = java.util.stream.IntStream
            .range(hotbarStart, playerSlotEnd)
            .boxed()
            .toList();
        List<Integer> inventorySlots = java.util.stream.IntStream
            .range(containerSlotCount, hotbarStart)
            .boxed()
            .toList();
        boolean movedAny = false;
        for (int sourceIndex = 0; sourceIndex < containerSlotCount; sourceIndex++) {
            Slot source = screen.getMenu().slots.get(sourceIndex);
            if (source.isActive() && source.hasItem()) {
                movedAny |= moveSlotToPlayerInOrder(screen, sourceIndex, hotbarSlots, inventorySlots);
            }
        }
        return movedAny;
    }

    public static boolean storeAllInContainer(AbstractContainerScreen<?> screen) {
        if (!hasLocalInventoryAccess() || !screen.getMenu().getCarried().isEmpty()) return false;
        int containerSlotCount = containerSlotCount(screen);
        if (containerSlotCount < 0) return false;

        List<Integer> containerSlots = java.util.stream.IntStream
            .range(0, containerSlotCount)
            .boxed()
            .toList();
        boolean movedAny = false;
        for (int sourceIndex = containerSlotCount; sourceIndex < screen.getMenu().slots.size(); sourceIndex++) {
            Slot source = screen.getMenu().slots.get(sourceIndex);
            if (source.isActive() && source.hasItem()) {
                movedAny |= moveSlotToContainerInOrder(screen, sourceIndex, containerSlots);
            }
        }
        return movedAny;
    }

    private static int containerSlotCount(AbstractContainerScreen<?> screen) {
        if (screen.getMenu() instanceof ChestMenu chest) {
            return chest.getRowCount() * 9;
        }
        if (screen.getMenu() instanceof ShulkerBoxMenu) {
            return 27;
        }
        return -1;
    }

    private static boolean click(AbstractContainerScreen<?> screen, Slot slot, int button, ContainerInput input) {
        if (!hasLocalInventoryAccess() || slot == null || !slot.isActive()) return false;
        accessor(screen).javareforged$invokeSlotClicked(slot, slot.index, button, input);
        return true;
    }

    private static Slot actionSlot(AbstractContainerScreen<?> screen, boolean requireItem) {
        Slot focusedSlot = UiInputModeController.isFocusNavigationActive() ? focused(screen) : null;
        if (isUsableActionSlot(focusedSlot, requireItem)) {
            return focusedSlot;
        }

        Slot hoveredSlot = hovered(screen);
        if (isUsableActionSlot(hoveredSlot, requireItem)) {
            return hoveredSlot;
        }

        if (!UiInputModeController.isFocusNavigationActive()) {
            focusedSlot = focused(screen);
            if (isUsableActionSlot(focusedSlot, requireItem)) {
                return focusedSlot;
            }
        }
        return null;
    }

    private static boolean isUsableActionSlot(Slot slot, boolean requireItem) {
        return slot != null && slot.isActive() && (!requireItem || slot.hasItem());
    }

    private static Slot focused(AbstractContainerScreen<?> screen) {
        Integer index = FOCUS.get(screen);
        if (index == null || index < 0 || index >= screen.getMenu().slots.size()) return null;
        Slot slot = screen.getMenu().slots.get(index);
        if (!activeSlots(screen).contains(slot)) {
            FOCUS.remove(screen);
            return null;
        }
        return slot;
    }

    private static Slot hovered(AbstractContainerScreen<?> screen) {
        return accessor(screen).javareforged$getHoveredSlot();
    }

    private static List<Slot> activeSlots(AbstractContainerScreen<?> screen) {
        AbstractContainerScreenAccessor access = accessor(screen);
        int width = access.javareforged$getImageWidth();
        int height = access.javareforged$getImageHeight();
        return screen.getMenu().slots.stream()
            .filter(Slot::isActive)
            .filter(slot -> slot.x >= 0 && slot.y >= 0 && slot.x <= width - 16 && slot.y <= height - 16)
            .toList();
    }

    private static Slot initialSlot(List<Slot> slots, Direction direction) {
        Comparator<Slot> comparator = switch (direction) {
            case UP -> Comparator.comparingInt((Slot slot) -> slot.y).reversed().thenComparingInt(slot -> slot.x);
            case DOWN -> Comparator.comparingInt((Slot slot) -> slot.y).thenComparingInt(slot -> slot.x);
            case LEFT -> Comparator.comparingInt((Slot slot) -> slot.x).reversed().thenComparingInt(slot -> slot.y);
            case RIGHT -> Comparator.comparingInt((Slot slot) -> slot.x).thenComparingInt(slot -> slot.y);
        };
        return slots.stream().min(comparator).orElse(slots.getFirst());
    }

    private static void setFocus(AbstractContainerScreen<?> screen, Slot slot) {
        FOCUS.put(screen, slot.index);
        FocusedSlotCenter center = getFocusedSlotCenter(screen);
        if (center != null) GamepadInputProcessor.moveVirtualCursorToFocusedSlot(center.guiX, center.guiY);
    }

    private static AbstractContainerScreenAccessor accessor(AbstractContainerScreen<?> screen) {
        return (AbstractContainerScreenAccessor) screen;
    }

    private static boolean moveSlotToPlayerInOrder(
        AbstractContainerScreen<?> screen,
        int sourceIndex,
        List<Integer> hotbarSlotOrder,
        List<Integer> inventorySlotOrder
    ) {
        Slot source = screen.getMenu().slots.get(sourceIndex);
        if (!source.hasItem()) return false;
        ItemStack original = source.getItem().copy();
        click(screen, source, 0, ContainerInput.PICKUP);
        if (screen.getMenu().getCarried().isEmpty()) {
            return !ItemStack.matches(original, source.getItem());
        }
        moveCarriedIntoMatchingSlots(screen, hotbarSlotOrder);
        moveCarriedIntoEmptySlots(screen, hotbarSlotOrder);
        moveCarriedIntoMatchingSlots(screen, inventorySlotOrder);
        moveCarriedIntoEmptySlots(screen, inventorySlotOrder);
        if (!screen.getMenu().getCarried().isEmpty()) {
            click(screen, source, 0, ContainerInput.PICKUP);
        }
        return !ItemStack.matches(original, source.getItem());
    }

    private static boolean moveSlotToContainerInOrder(
        AbstractContainerScreen<?> screen, int sourceIndex, List<Integer> containerSlotOrder
    ) {
        Slot source = screen.getMenu().slots.get(sourceIndex);
        if (!source.hasItem()) return false;
        ItemStack original = source.getItem().copy();
        click(screen, source, 0, ContainerInput.PICKUP);
        if (screen.getMenu().getCarried().isEmpty()) {
            return !ItemStack.matches(original, source.getItem());
        }
        moveCarriedIntoMatchingSlots(screen, containerSlotOrder);
        moveCarriedIntoEmptySlots(screen, containerSlotOrder);
        if (!screen.getMenu().getCarried().isEmpty()) {
            click(screen, source, 0, ContainerInput.PICKUP);
        }
        return !ItemStack.matches(original, source.getItem());
    }

    private static void moveCarriedIntoMatchingSlots(AbstractContainerScreen<?> screen, List<Integer> order) {
        for (int index : order) {
            ItemStack carried = screen.getMenu().getCarried();
            if (carried.isEmpty()) return;
            Slot slot = screen.getMenu().slots.get(index);
            ItemStack existing = slot.getItem();
            if (!slot.isActive() || !slot.mayPlace(carried) || existing.isEmpty()
                || !ItemStack.isSameItemSameComponents(existing, carried)) continue;
            int maximum = Math.min(slot.getMaxStackSize(carried), carried.getMaxStackSize());
            if (existing.getCount() < maximum) click(screen, slot, 0, ContainerInput.PICKUP);
        }
    }

    private static void moveCarriedIntoEmptySlots(AbstractContainerScreen<?> screen, List<Integer> order) {
        for (int index : order) {
            ItemStack carried = screen.getMenu().getCarried();
            if (carried.isEmpty()) return;
            Slot slot = screen.getMenu().slots.get(index);
            if (slot.isActive() && slot.mayPlace(carried) && slot.getItem().isEmpty()) {
                click(screen, slot, 0, ContainerInput.PICKUP);
            }
        }
    }

    private static boolean hasLocalInventoryAccess() {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        return minecraft.player != null && minecraft.gameMode != null;
    }

    private static Slot initialGridSlot(List<Slot> slots, SlotGrid grid, Direction direction) {
        Comparator<Slot> comparator = switch (direction) {
            case UP -> Comparator
                .comparingDouble((Slot slot) -> gridY(grid, slot))
                .thenComparingDouble(slot -> -gridX(grid, slot));
            case DOWN -> Comparator
                .comparingDouble((Slot slot) -> gridY(grid, slot))
                .thenComparingDouble(slot -> gridX(grid, slot));
            case LEFT -> Comparator
                .comparingDouble((Slot slot) -> gridX(grid, slot))
                .thenComparingDouble(slot -> -gridY(grid, slot));
            case RIGHT -> Comparator
                .comparingDouble((Slot slot) -> gridX(grid, slot))
                .thenComparingDouble(slot -> gridY(grid, slot));
        };
        return switch (direction) {
            case UP, LEFT -> slots.stream().max(comparator).orElse(null);
            case DOWN, RIGHT -> slots.stream().min(comparator).orElse(null);
        };
    }

    private static Slot nextGridSlot(
        List<Slot> slots,
        SlotGrid grid,
        Slot current,
        Direction direction
    ) {
        Comparator<Slot> straightComparator = Comparator
            .comparingDouble((Slot slot) -> gridPrimaryDistance(grid, slot, current, direction))
            .thenComparingDouble(slot -> gridSecondaryDistance(grid, slot, current, direction))
            .thenComparingDouble(slot -> direction.isHorizontal()
                ? gridY(grid, slot)
                : gridX(grid, slot));

        Slot straight = slots.stream()
            .filter(slot -> slot != current
                && gridCandidateInDirection(grid, slot, current, direction)
                && gridOverlapsDirectionAxis(grid, slot, current, direction))
            .min(straightComparator)
            .orElse(null);
        if (straight != null) {
            return straight;
        }

        return slots.stream()
            .filter(slot -> slot != current
                && gridCandidateInDirection(grid, slot, current, direction)
                && gridCandidateInsideNavigationCone(grid, slot, current, direction))
            .min(Comparator
                .comparingDouble((Slot slot) -> gridDistanceSquared(grid, slot, current, direction))
                .thenComparingDouble(slot -> gridAngularDeviation(grid, slot, current, direction))
                .thenComparing(straightComparator))
            .orElse(null);
    }

    private static Slot nextSlot(List<Slot> slots, Slot origin, Direction direction) {
        Comparator<Slot> straightComparator = Comparator
            .comparingDouble((Slot slot) -> direction.primary(slot, origin))
            .thenComparingDouble(slot -> direction.secondary(slot, origin))
            .thenComparingDouble(slot -> direction.isHorizontal() ? slot.y : slot.x);

        Slot straight = slots.stream()
            .filter(slot -> slot != origin
                && direction.accepts(slot, origin)
                && direction.overlapsDirectionAxis(slot, origin))
            .min(straightComparator)
            .orElse(null);
        if (straight != null) {
            return straight;
        }

        return slots.stream()
            .filter(slot -> slot != origin
                && direction.accepts(slot, origin)
                && direction.isInsideNavigationCone(slot, origin))
            .min(Comparator
                .comparingDouble((Slot slot) -> direction.distanceSquared(slot, origin))
                .thenComparingDouble(slot -> direction.angularDeviation(slot, origin))
                .thenComparing(straightComparator))
            .orElse(null);
    }

    private static boolean gridCandidateInDirection(
        SlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        return switch (direction) {
            case UP -> gridY(grid, slot) < gridY(grid, current);
            case DOWN -> gridY(grid, slot) > gridY(grid, current);
            case LEFT -> gridX(grid, slot) < gridX(grid, current);
            case RIGHT -> gridX(grid, slot) > gridX(grid, current);
        };
    }

    private static boolean gridCandidateInsideNavigationCone(
        SlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        return gridSecondaryDistance(grid, slot, current, direction)
            <= gridPrimaryDistance(grid, slot, current, direction);
    }

    private static boolean gridOverlapsDirectionAxis(
        SlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        double candidate = direction.isHorizontal() ? gridY(grid, slot) : gridX(grid, slot);
        double origin = direction.isHorizontal() ? gridY(grid, current) : gridX(grid, current);
        return Direction.rangesOverlap(
            candidate - 8.0D,
            candidate + 8.0D,
            origin - 8.0D,
            origin + 8.0D
        );
    }

    private static double gridAngularDeviation(
        SlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        return gridSecondaryDistance(grid, slot, current, direction)
            / gridPrimaryDistance(grid, slot, current, direction);
    }

    private static double gridDistanceSquared(
        SlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        double primary = gridPrimaryDistance(grid, slot, current, direction);
        double secondary = gridSecondaryDistance(grid, slot, current, direction);
        return primary * primary + secondary * secondary;
    }

    private static double gridPrimaryDistance(
        SlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        return switch (direction) {
            case UP -> gridY(grid, current) - gridY(grid, slot);
            case DOWN -> gridY(grid, slot) - gridY(grid, current);
            case LEFT -> gridX(grid, current) - gridX(grid, slot);
            case RIGHT -> gridX(grid, slot) - gridX(grid, current);
        };
    }

    private static double gridSecondaryDistance(
        SlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        return direction.isHorizontal()
            ? Math.abs(gridY(grid, slot) - gridY(grid, current))
            : Math.abs(gridX(grid, slot) - gridX(grid, current));
    }

    private static double gridX(SlotGrid grid, Slot slot) {
        return grid.get(slot.index).x() + 8.0D;
    }

    private static double gridY(SlotGrid grid, Slot slot) {
        return grid.get(slot.index).y() + 8.0D;
    }

    private static void syncCursorToGridSlot(SlotGrid grid, Slot slot) {
        GamepadInputProcessor.moveVirtualCursorToFocusedSlot(gridX(grid, slot), gridY(grid, slot));
    }

    public record FocusedSlotCenter(double guiX, double guiY) {}

    private enum Direction {
        UP, DOWN, LEFT, RIGHT;
        static Direction fromKey(int key) {
            return switch (key) {
                case GLFW.GLFW_KEY_UP -> UP; case GLFW.GLFW_KEY_DOWN -> DOWN;
                case GLFW.GLFW_KEY_LEFT -> LEFT; case GLFW.GLFW_KEY_RIGHT -> RIGHT;
                default -> null;
            };
        }
        boolean accepts(Slot candidate, Slot origin) {
            return switch (this) {
                case UP -> candidate.y < origin.y; case DOWN -> candidate.y > origin.y;
                case LEFT -> candidate.x < origin.x; case RIGHT -> candidate.x > origin.x;
            };
        }
        double primary(Slot candidate, Slot origin) {
            return switch (this) {
                case UP -> origin.y - candidate.y; case DOWN -> candidate.y - origin.y;
                case LEFT -> origin.x - candidate.x; case RIGHT -> candidate.x - origin.x;
            };
        }
        double secondary(Slot candidate, Slot origin) {
            return this == UP || this == DOWN
                ? Math.abs(candidate.x - origin.x) : Math.abs(candidate.y - origin.y);
        }
        boolean isInsideNavigationCone(Slot candidate, Slot origin) {
            return secondary(candidate, origin) <= primary(candidate, origin);
        }
        boolean overlapsDirectionAxis(Slot candidate, Slot origin) {
            if (this == LEFT || this == RIGHT) {
                return rangesOverlap(candidate.y, candidate.y + 16, origin.y, origin.y + 16);
            }
            return rangesOverlap(candidate.x, candidate.x + 16, origin.x, origin.x + 16);
        }
        double angularDeviation(Slot candidate, Slot origin) {
            return secondary(candidate, origin) / primary(candidate, origin);
        }
        double distanceSquared(Slot candidate, Slot origin) {
            double primary = primary(candidate, origin);
            double secondary = secondary(candidate, origin);
            return primary * primary + secondary * secondary;
        }
        private static boolean rangesOverlap(double aMin, double aMax, double bMin, double bMax) {
            return aMax > bMin && bMax > aMin;
        }
        private boolean isHorizontal() {
            return this == LEFT || this == RIGHT;
        }
    }
}
