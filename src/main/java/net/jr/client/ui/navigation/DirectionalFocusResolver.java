package net.jr.client.ui.navigation;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class DirectionalFocusResolver {
    private static final double CROSS_AXIS_WEIGHT = 2.0D;

    private DirectionalFocusResolver() {
    }

    public static boolean supportsKey(int keyCode) {
        return Direction.fromKey(keyCode) != null;
    }

    @Nullable
    public static <T> T initialTarget(
        List<T> targets,
        Function<T, ScreenRectangle> rectangle,
        int keyCode
    ) {
        Direction direction = Direction.fromKey(keyCode);
        if (direction == null || targets.isEmpty()) {
            return null;
        }
        return targets.stream().min(Comparator
            .comparingDouble((T target) -> direction.initialPrimary(rectangle.apply(target)))
            .thenComparingDouble(target -> direction.initialSecondary(rectangle.apply(target)))
        ).orElse(null);
    }

    @Nullable
    public static <T> T nextTarget(
        List<T> targets,
        T origin,
        Function<T, ScreenRectangle> rectangle,
        int keyCode
    ) {
        Direction direction = Direction.fromKey(keyCode);
        if (direction == null) {
            return null;
        }
        ScreenRectangle originRectangle = rectangle.apply(origin);
        List<T> directionalCandidates = targets.stream()
            .filter(target -> target != origin)
            .filter(target -> direction.accepts(rectangle.apply(target), originRectangle))
            .toList();
        boolean preferBeam = directionalCandidates.stream()
            .anyMatch(target -> direction.inBeam(rectangle.apply(target), originRectangle));
        Stream<T> candidates = directionalCandidates.stream();
        if (preferBeam) {
            candidates = candidates.filter(
                target -> direction.inBeam(rectangle.apply(target), originRectangle)
            );
        }
        Comparator<T> comparator = preferBeam
            ? Comparator
                .comparingDouble((T target) -> direction.primaryDistance(
                    rectangle.apply(target), originRectangle
                ))
                .thenComparingDouble(target -> direction.secondaryDistance(
                    rectangle.apply(target), originRectangle
                ))
            : Comparator
                .comparingDouble((T target) -> direction.score(
                    rectangle.apply(target), originRectangle
                ))
                .thenComparingDouble(target -> direction.crossAxisGap(
                    rectangle.apply(target), originRectangle
                ))
                .thenComparingDouble(target -> direction.secondaryDistance(
                    rectangle.apply(target), originRectangle
                ))
                .thenComparingDouble(target -> direction.primaryDistance(
                    rectangle.apply(target), originRectangle
                ));
        return candidates.min(comparator).orElse(null);
    }

    @Nullable
    public static <T> T wrapTarget(
        List<T> targets,
        T origin,
        Function<T, ScreenRectangle> rectangle,
        int keyCode
    ) {
        Direction direction = Direction.fromKey(keyCode);
        if (direction == null) {
            return null;
        }
        ScreenRectangle originRectangle = rectangle.apply(origin);
        List<T> candidates = targets.stream()
            .filter(target -> target != origin)
            .toList();
        boolean preferBeam = candidates.stream()
            .anyMatch(target -> direction.inBeam(rectangle.apply(target), originRectangle));
        Stream<T> wrapped = candidates.stream();
        if (preferBeam) {
            wrapped = wrapped.filter(
                target -> direction.inBeam(rectangle.apply(target), originRectangle)
            );
        }
        return wrapped.min(Comparator
            .comparingDouble((T target) -> direction.initialPrimary(rectangle.apply(target)))
            .thenComparingDouble(target -> direction.secondaryDistance(
                rectangle.apply(target), originRectangle
            ))
        ).orElse(null);
    }

    private enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT;

        @Nullable
        static Direction fromKey(int keyCode) {
            return switch (keyCode) {
                case GLFW.GLFW_KEY_UP -> UP;
                case GLFW.GLFW_KEY_DOWN -> DOWN;
                case GLFW.GLFW_KEY_LEFT -> LEFT;
                case GLFW.GLFW_KEY_RIGHT -> RIGHT;
                default -> null;
            };
        }

        boolean accepts(ScreenRectangle candidate, ScreenRectangle origin) {
            return switch (this) {
                case UP -> centerY(candidate) < centerY(origin);
                case DOWN -> centerY(candidate) > centerY(origin);
                case LEFT -> centerX(candidate) < centerX(origin);
                case RIGHT -> centerX(candidate) > centerX(origin);
            };
        }

        double initialPrimary(ScreenRectangle rectangle) {
            return switch (this) {
                case UP -> -centerY(rectangle);
                case DOWN -> centerY(rectangle);
                case LEFT -> -centerX(rectangle);
                case RIGHT -> centerX(rectangle);
            };
        }

        double initialSecondary(ScreenRectangle rectangle) {
            return isHorizontal() ? centerY(rectangle) : centerX(rectangle);
        }

        double score(ScreenRectangle candidate, ScreenRectangle origin) {
            double primary = primaryDistance(candidate, origin);
            double weightedCrossAxisGap = crossAxisGap(candidate, origin) * CROSS_AXIS_WEIGHT;
            return primary * primary + weightedCrossAxisGap * weightedCrossAxisGap;
        }

        double primaryDistance(ScreenRectangle candidate, ScreenRectangle origin) {
            return switch (this) {
                case UP -> centerY(origin) - centerY(candidate);
                case DOWN -> centerY(candidate) - centerY(origin);
                case LEFT -> centerX(origin) - centerX(candidate);
                case RIGHT -> centerX(candidate) - centerX(origin);
            };
        }

        double secondaryDistance(ScreenRectangle candidate, ScreenRectangle origin) {
            return isHorizontal()
                ? Math.abs(centerY(candidate) - centerY(origin))
                : Math.abs(centerX(candidate) - centerX(origin));
        }

        double crossAxisGap(ScreenRectangle candidate, ScreenRectangle origin) {
            if (isHorizontal()) {
                return intervalGap(
                    candidate.top(), candidate.top() + candidate.height(),
                    origin.top(), origin.top() + origin.height()
                );
            }
            return intervalGap(
                candidate.left(), candidate.left() + candidate.width(),
                origin.left(), origin.left() + origin.width()
            );
        }

        boolean inBeam(ScreenRectangle candidate, ScreenRectangle origin) {
            return isHorizontal()
                ? intervalsOverlap(
                    candidate.top(), candidate.top() + candidate.height(),
                    origin.top(), origin.top() + origin.height()
                )
                : intervalsOverlap(
                    candidate.left(), candidate.left() + candidate.width(),
                    origin.left(), origin.left() + origin.width()
                );
        }

        boolean isHorizontal() {
            return this == LEFT || this == RIGHT;
        }
    }

    private static double intervalGap(double aMin, double aMax, double bMin, double bMax) {
        return Math.max(0.0D, Math.max(aMin, bMin) - Math.min(aMax, bMax));
    }

    private static boolean intervalsOverlap(double aMin, double aMax, double bMin, double bMax) {
        return Math.max(aMin, bMin) < Math.min(aMax, bMax);
    }

    private static double centerX(ScreenRectangle rectangle) {
        return rectangle.getCenterInAxis(ScreenAxis.HORIZONTAL);
    }

    private static double centerY(ScreenRectangle rectangle) {
        return rectangle.getCenterInAxis(ScreenAxis.VERTICAL);
    }
}
