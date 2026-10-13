package net.jr.client.ui.container.actions;

import java.util.Comparator;
import java.util.List;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.ItemSlotPos;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

final class ItemSlotNavigationResolver {
    private ItemSlotNavigationResolver() {
    }

    static boolean supportsKey(int keyCode) {
        return Direction.fromKey(keyCode) != null;
    }

    static Slot initialSlot(List<Slot> slots, int keyCode) {
        Direction direction = Direction.fromKey(keyCode);
        if (direction == null) {
            return null;
        }
        Comparator<Slot> comparator = switch (direction) {
            case UP -> Comparator.comparingInt((Slot slot) -> slot.y)
                .reversed()
                .thenComparingInt(slot -> slot.x);
            case DOWN -> Comparator.comparingInt((Slot slot) -> slot.y)
                .thenComparingInt(slot -> slot.x);
            case LEFT -> Comparator.comparingInt((Slot slot) -> slot.x)
                .reversed()
                .thenComparingInt(slot -> slot.y);
            case RIGHT -> Comparator.comparingInt((Slot slot) -> slot.x)
                .thenComparingInt(slot -> slot.y);
        };
        return slots.stream().min(comparator).orElse(slots.getFirst());
    }

    static Slot nextSlot(List<Slot> slots, Slot origin, int keyCode) {
        Direction direction = Direction.fromKey(keyCode);
        if (direction == null) {
            return null;
        }
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

    static Slot initialGridSlot(List<Slot> slots, ItemSlotGrid grid, int keyCode) {
        Direction direction = Direction.fromKey(keyCode);
        if (direction == null) {
            return null;
        }
        Comparator<Slot> comparator = switch (direction) {
            case UP -> Comparator
                .comparingDouble((Slot slot) -> centerY(grid, slot))
                .thenComparingDouble(slot -> -centerX(grid, slot));
            case DOWN -> Comparator
                .comparingDouble((Slot slot) -> centerY(grid, slot))
                .thenComparingDouble(slot -> centerX(grid, slot));
            case LEFT -> Comparator
                .comparingDouble((Slot slot) -> centerX(grid, slot))
                .thenComparingDouble(slot -> -centerY(grid, slot));
            case RIGHT -> Comparator
                .comparingDouble((Slot slot) -> centerX(grid, slot))
                .thenComparingDouble(slot -> centerY(grid, slot));
        };
        return switch (direction) {
            case UP, LEFT -> slots.stream().max(comparator).orElse(null);
            case DOWN, RIGHT -> slots.stream().min(comparator).orElse(null);
        };
    }

    static Slot nextGridSlot(
        List<Slot> slots,
        ItemSlotGrid grid,
        Slot current,
        int keyCode
    ) {
        Direction direction = Direction.fromKey(keyCode);
        if (direction == null) {
            return null;
        }
        Comparator<Slot> straightComparator = Comparator
            .comparingDouble((Slot slot) -> primaryDistance(grid, slot, current, direction))
            .thenComparingDouble(slot -> secondaryDistance(grid, slot, current, direction))
            .thenComparingDouble(slot -> direction.isHorizontal()
                ? centerY(grid, slot)
                : centerX(grid, slot));

        Slot straight = slots.stream()
            .filter(slot -> slot != current
                && isCandidateInDirection(grid, slot, current, direction)
                && overlapsDirectionAxis(grid, slot, current, direction))
            .min(straightComparator)
            .orElse(null);
        if (straight != null) {
            return straight;
        }

        return slots.stream()
            .filter(slot -> slot != current
                && isCandidateInDirection(grid, slot, current, direction)
                && isCandidateInsideNavigationCone(grid, slot, current, direction))
            .min(Comparator
                .comparingDouble((Slot slot) -> distanceSquared(grid, slot, current, direction))
                .thenComparingDouble(slot -> angularDeviation(grid, slot, current, direction))
                .thenComparing(straightComparator))
            .orElse(null);
    }

    static double centerX(ItemSlotGrid grid, Slot slot) {
        ItemSlotPos position = grid.get(slot.index);
        return position.x() + position.width() / 2.0D;
    }

    static double centerY(ItemSlotGrid grid, Slot slot) {
        ItemSlotPos position = grid.get(slot.index);
        return position.y() + position.height() / 2.0D;
    }

    private static boolean isCandidateInDirection(
        ItemSlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        return switch (direction) {
            case UP -> centerY(grid, slot) < centerY(grid, current);
            case DOWN -> centerY(grid, slot) > centerY(grid, current);
            case LEFT -> centerX(grid, slot) < centerX(grid, current);
            case RIGHT -> centerX(grid, slot) > centerX(grid, current);
        };
    }

    private static boolean isCandidateInsideNavigationCone(
        ItemSlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        return secondaryDistance(grid, slot, current, direction)
            <= primaryDistance(grid, slot, current, direction);
    }

    private static boolean overlapsDirectionAxis(
        ItemSlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        double candidate = direction.isHorizontal()
            ? centerY(grid, slot)
            : centerX(grid, slot);
        double origin = direction.isHorizontal()
            ? centerY(grid, current)
            : centerX(grid, current);
        return rangesOverlap(
            candidate - 8.0D,
            candidate + 8.0D,
            origin - 8.0D,
            origin + 8.0D
        );
    }

    private static double angularDeviation(
        ItemSlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        return secondaryDistance(grid, slot, current, direction)
            / primaryDistance(grid, slot, current, direction);
    }

    private static double distanceSquared(
        ItemSlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        double primary = primaryDistance(grid, slot, current, direction);
        double secondary = secondaryDistance(grid, slot, current, direction);
        return primary * primary + secondary * secondary;
    }

    private static double primaryDistance(
        ItemSlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        return switch (direction) {
            case UP -> centerY(grid, current) - centerY(grid, slot);
            case DOWN -> centerY(grid, slot) - centerY(grid, current);
            case LEFT -> centerX(grid, current) - centerX(grid, slot);
            case RIGHT -> centerX(grid, slot) - centerX(grid, current);
        };
    }

    private static double secondaryDistance(
        ItemSlotGrid grid,
        Slot slot,
        Slot current,
        Direction direction
    ) {
        return direction.isHorizontal()
            ? Math.abs(centerY(grid, slot) - centerY(grid, current))
            : Math.abs(centerX(grid, slot) - centerX(grid, current));
    }

    private static boolean rangesOverlap(double aMin, double aMax, double bMin, double bMax) {
        return aMax > bMin && bMax > aMin;
    }

    private enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT;

        static Direction fromKey(int keyCode) {
            return switch (keyCode) {
                case GLFW.GLFW_KEY_UP -> UP;
                case GLFW.GLFW_KEY_DOWN -> DOWN;
                case GLFW.GLFW_KEY_LEFT -> LEFT;
                case GLFW.GLFW_KEY_RIGHT -> RIGHT;
                default -> null;
            };
        }

        boolean accepts(Slot candidate, Slot origin) {
            return switch (this) {
                case UP -> candidate.y < origin.y;
                case DOWN -> candidate.y > origin.y;
                case LEFT -> candidate.x < origin.x;
                case RIGHT -> candidate.x > origin.x;
            };
        }

        double primary(Slot candidate, Slot origin) {
            return switch (this) {
                case UP -> origin.y - candidate.y;
                case DOWN -> candidate.y - origin.y;
                case LEFT -> origin.x - candidate.x;
                case RIGHT -> candidate.x - origin.x;
            };
        }

        double secondary(Slot candidate, Slot origin) {
            return isHorizontal()
                ? Math.abs(candidate.y - origin.y)
                : Math.abs(candidate.x - origin.x);
        }

        boolean isInsideNavigationCone(Slot candidate, Slot origin) {
            return secondary(candidate, origin) <= primary(candidate, origin);
        }

        boolean overlapsDirectionAxis(Slot candidate, Slot origin) {
            if (isHorizontal()) {
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

        boolean isHorizontal() {
            return this == LEFT || this == RIGHT;
        }
    }
}
